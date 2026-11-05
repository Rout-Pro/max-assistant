package com.rout.max.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared orchestrator used by both the chat UI and the floating orb. */
object MAXCore {

    fun batteryPercent(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val cap = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (cap in 1..100) cap else 0
    }

    fun isCharging(context: Context): Boolean {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val status = context.registerReceiver(null, ifilter)
            ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun clipboardText(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString() ?: ""
        } catch (_: Exception) { "" }
    }

    /**
     * Full pipeline: gather live context, try local keyless commands first when
     * there is no API key or the network fails, otherwise ask Gemini.
     */
    suspend fun ask(context: Context, userText: String, screenshotBase64: String? = null): BrainResponse {
        val battery = batteryPercent(context)
        val apiKey = KeyStore.getApiKey(context)
        val appCtx = context.applicationContext

        // Keyless local answers first: live weather and the morning briefing.
        // These never invent data - on any failure they fall through.
        val lower = userText.lowercase()
        if (isMorningBriefingQuery(lower)) {
            return morningBriefing(appCtx, battery)
        }
        if (LocalRouter.isWeatherQuery(lower)) {
            val w = Weather.fetchSummary(appCtx)
            if (w != null) {
                return BrainResponse("Sir, $w.", "happy", false, emptyList())
            }
            return BrainResponse(
                "Sir, mausam ke liye Control tab mein Location permission chahiye, aur internet on hona chahiye. Permission hai to ek baar fir poochhiye.",
                "alert", false, emptyList())
        }

        if (apiKey.isBlank()) {
            LocalRouter.route(userText, battery)?.let { return it }
            return BrainResponse(
                "Sir, smart jawab ke liye Settings mein apni free Gemini API key daal dijiye. Time, date, battery, torch, weather aur app kholna bina key ke bhi chalta hai.",
                "alert", false, emptyList())
        }

        val nodes = MAXAccessibilityService.instance?.dumpInteractiveScreenNodes() ?: emptyList()
        val notifications = synchronized(MAXNotificationService.liveNotifications) {
            MAXNotificationService.liveNotifications.toList()
        }
        val systemPrompt = PersonalityEngine.buildSystemPrompt(
            mode = KeyStore.getPersonality(appCtx),
            userName = KeyStore.getUserName(appCtx),
            memories = MemoryManager.activeMemories(appCtx)
        )
        val body = Brain.buildRequestBody(
            systemPrompt = systemPrompt,
            userText = userText,
            screenNodes = nodes,
            screenshotBase64 = screenshotBase64,
            battery = battery,
            isCharging = isCharging(appCtx),
            foregroundPackage = MAXAccessibilityService.foregroundPackage,
            clipboard = clipboardText(appCtx),
            notifications = notifications
        ).toString()

        var model = KeyStore.getModel(appCtx)
        var outcome = httpPost(Brain.apiUrl(model, apiKey), body)
        // If the configured model was renamed/retired, retry once with the
        // current flash alias and remember it when it works.
        if (outcome is ApiOutcome.HttpErr && Brain.isModelNotFound(outcome.code, outcome.body) &&
            model != Brain.FALLBACK_MODEL) {
            val retry = httpPost(Brain.apiUrl(Brain.FALLBACK_MODEL, apiKey), body)
            if (retry is ApiOutcome.Ok) {
                KeyStore.setModel(appCtx, Brain.FALLBACK_MODEL)
                outcome = retry
            }
        }
        if (outcome is ApiOutcome.Ok) {
            val text = Brain.extractTextFromApiResponse(outcome.body)
            if (text != null) {
                val parsed = Brain.parseResponse(text)
                Brain.appendHistory(userText, parsed.speech)
                return parsed
            }
            return BrainResponse("Sir, AI ka reply samajh nahi aaya, ek baar fir bolo?", "alert", false, emptyList())
        }

        // Failure: keyless commands still work; otherwise say exactly why.
        LocalRouter.route(userText, battery)?.let { return it }
        val msg = when (outcome) {
            is ApiOutcome.HttpErr -> Brain.diagnoseHttpError(outcome.code, outcome.body)
            is ApiOutcome.NetErr -> Brain.diagnoseNetError(outcome.reason)
            is ApiOutcome.Ok -> "Sir, AI ka reply samajh nahi aaya, ek baar fir bolo?"
        }
        return BrainResponse(msg, "alert", false, emptyList())
    }

    private fun isMorningBriefingQuery(lower: String): Boolean =
        lower.contains("good morning") || lower.contains("morning briefing") ||
            lower.contains("morning digest") || lower.contains("subah ki briefing") ||
            lower.contains("meri briefing") || lower.contains("briefing do")

    /** Spoken morning briefing: greeting, day, time, battery and live weather when available. */
    private suspend fun morningBriefing(context: Context, battery: Int): BrainResponse {
        val now = Date()
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        val day = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
        val w = Weather.fetchSummary(context)
        val weatherPart = w?.let { " Mausam: $it." }
            ?: " Mausam ke liye location permission ya internet nahi mila."
        val speech = "Good morning Sir. Aaj $day hai aur abhi $time baj rahe hain. " +
            "Battery $battery percent pe hai.$weatherPart Aapka din shubh ho!"
        return BrainResponse(speech, "happy", false, emptyList())
    }

    /** Result of a Gemini API call that keeps the real failure cause. */
    sealed interface ApiOutcome {
        class Ok(val body: String) : ApiOutcome
        class HttpErr(val code: Int, val body: String) : ApiOutcome
        class NetErr(val reason: String) : ApiOutcome
    }

    private suspend fun httpPost(url: String, body: String): ApiOutcome = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 45000
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) ApiOutcome.Ok(text) else ApiOutcome.HttpErr(code, text)
        } catch (e: javax.net.ssl.SSLException) {
            ApiOutcome.NetErr("ssl")
        } catch (e: java.net.SocketTimeoutException) {
            ApiOutcome.NetErr("timeout")
        } catch (e: java.net.UnknownHostException) {
            ApiOutcome.NetErr("net")
        } catch (e: java.net.ConnectException) {
            ApiOutcome.NetErr("net")
        } catch (e: Exception) {
            ApiOutcome.NetErr("other")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Minimal live ping of the saved key + model. Returns a user-readable
     * status line, never the key or the raw API body.
     */
    suspend fun testApiKey(context: Context): String {
        val appCtx = context.applicationContext
        val apiKey = KeyStore.getApiKey(appCtx)
        if (apiKey.isBlank()) return "Pehle API key daal kar Save key dabayein."
        val model = KeyStore.getModel(appCtx).ifBlank { Brain.FALLBACK_MODEL }
        val body = """{"contents":[{"role":"user","parts":[{"text":"Say OK"}]}]}"""
        return when (val r = httpPost(Brain.apiUrl(model, apiKey), body)) {
            is ApiOutcome.Ok -> "Key kaam kar rahi hai - model $model se jawab mil gaya."
            is ApiOutcome.HttpErr -> Brain.diagnoseHttpError(r.code, r.body)
            is ApiOutcome.NetErr -> Brain.diagnoseNetError(r.reason)
        }
    }
}
