package com.rout.max

import com.rout.max.core.Brain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainErrorTest {

    private val keyInvalidBody = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}"""

    @Test fun invalidKeyMapsToKeyMessage() {
        assertTrue(Brain.isKeyInvalid(400, keyInvalidBody))
        assertTrue(Brain.diagnoseHttpError(400, keyInvalidBody).contains("key invalid"))
    }

    @Test fun forbiddenMapsToPermissionMessage() {
        assertFalse(Brain.isKeyInvalid(403, "{}"))
        assertTrue(Brain.diagnoseHttpError(403, "{}").contains("access"))
        assertTrue(Brain.diagnoseHttpError(401, "{}").contains("401"))
    }

    @Test fun quotaMapsToQuotaMessage() {
        assertTrue(Brain.diagnoseHttpError(429, "{}").contains("quota"))
        assertTrue(Brain.diagnoseHttpError(400, """{"error":{"status":"RESOURCE_EXHAUSTED"}}""").contains("quota"))
    }

    @Test fun modelNotFoundMapsToModelMessage() {
        assertTrue(Brain.isModelNotFound(404, "{}"))
        assertTrue(Brain.isModelNotFound(400, """{"error":{"message":"models/gemini-x is not found"}}"""))
        assertTrue(Brain.diagnoseHttpError(404, "{}").contains("model"))
        assertFalse(Brain.isModelNotFound(400, keyInvalidBody))
    }

    @Test fun serverAndNetErrorsMap() {
        assertTrue(Brain.diagnoseHttpError(500, "").contains("server"))
        assertTrue(Brain.diagnoseHttpError(418, "").contains("418"))
        assertTrue(Brain.diagnoseNetError("timeout").contains("slow"))
        assertTrue(Brain.diagnoseNetError("ssl").contains("secure"))
        assertTrue(Brain.diagnoseNetError("net").contains("internet"))
        assertEquals(Brain.FALLBACK_MODEL, "gemini-flash-latest")
    }

    @Test fun modelInfoProbeUsesGetMetadataEndpoint() {
        val url = Brain.modelInfoUrl("gemini-2.5-flash", "KEY123")
        assertTrue(url.startsWith("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash?key="))
        assertFalse(url.contains("generateContent"))
    }
}
