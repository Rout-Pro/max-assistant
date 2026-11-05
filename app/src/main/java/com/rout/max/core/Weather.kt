package com.rout.max.core

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Keyless local weather via Open-Meteo (free, no API key, no account).
 * Needs the coarse-location permission; returns null when permission,
 * a location fix, or the network is unavailable, so callers can fall
 * back to an honest reply instead of inventing data.
 */
object Weather {

    private fun describe(code: Int): String = when (code) {
        0 -> "bilkul clear sky"
        1, 2 -> "halki clouds"
        3 -> "overcast sky"
        45, 48 -> "fog"
        51, 53, 55, 56, 57 -> "drizzle"
        61, 63, 65, 66, 67 -> "baarish"
        71, 73, 75, 77 -> "snowfall"
        80, 81, 82 -> "rain showers"
        95, 96, 99 -> "thunderstorm"
        else -> "mixed mausam"
    }

    /** Short Hinglish weather fragment like "abhi 28°C, halki clouds", or null. */
    suspend fun fetchSummary(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val granted = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return@withContext null
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var here: Location? = null
            for (prov in listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )) {
                try {
                    val l = lm.getLastKnownLocation(prov)
                    if (l != null) { here = l; break }
                } catch (_: SecurityException) {}
            }
            val loc = here ?: return@withContext null
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${loc.latitude}&longitude=${loc.longitude}" +
                "&current=temperature_2m,weather_code&timezone=auto"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
            }
            val text = try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else return@withContext null
            } finally {
                conn.disconnect()
            }
            val cur = JSONObject(text).getJSONObject("current")
            val temp = cur.getDouble("temperature_2m")
            val code = cur.getInt("weather_code")
            "abhi ${"%.0f".format(temp)} degree C, ${describe(code)}"
        } catch (_: Exception) {
            null
        }
    }
}
