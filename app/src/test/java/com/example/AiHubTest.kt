package com.example

import com.example.model.AiCatalog
import com.example.model.AiCategory
import com.example.util.AiServiceManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiHubTest {

    @Test
    fun testAll50EnginesPresent() {
        assertEquals("Catalog must contain exactly 50 AI engines", 50, AiCatalog.engines.size)
        
        val uniqueIds = AiCatalog.engines.map { it.id }.toSet()
        assertEquals("All 50 AI engine IDs must be unique", 50, uniqueIds.size)

        // Ensure key providers exist
        val ids = uniqueIds
        assertTrue(ids.contains("chatgpt"))
        assertTrue(ids.contains("gemini"))
        assertTrue(ids.contains("claude"))
        assertTrue(ids.contains("perplexity"))
        assertTrue(ids.contains("dall_e"))
        assertTrue(ids.contains("runway"))
        assertTrue(ids.contains("suno"))
        assertTrue(ids.contains("elevenlabs"))
    }

    @Test
    fun testAiCategoriesCoverage() {
        val categories = AiCatalog.engines.map { it.category }.toSet()
        assertTrue(categories.contains(AiCategory.TEXT))
        assertTrue(categories.contains(AiCategory.IMAGE))
        assertTrue(categories.contains(AiCategory.VIDEO))
        assertTrue(categories.contains(AiCategory.AUDIO))
    }

    @Test
    fun testGeminiIsConfigured() {
        val isConfigured = AiServiceManager.isGeminiConfigured()
        assertTrue("Gemini should be detected as configured via GEMINI_API_KEY", isConfigured)
    }

    @Test
    fun testUnconfiguredProviderNotice() = runBlocking {
        val runwayEngine = AiCatalog.engines.first { it.id == "runway" }
        val reply = AiServiceManager.sendQuery(runwayEngine, "Create a fast drone shot")
        
        assertNotNull(reply)
        assertEquals("runway", reply.aiEngineId)
        assertEquals("ai", reply.sender)
        assertTrue(reply.text.contains("API Integration Required: Runway"))
        assertTrue(reply.text.contains("RUNWAY_API_KEY"))
    }

    @Test
    fun testOpenAiMissingKeyHandling() = runBlocking {
        // If OPENAI_API_KEY is not configured, it should return a clear error card
        if (!AiServiceManager.isOpenAiConfigured()) {
            val chatgptEngine = AiCatalog.engines.first { it.id == "chatgpt" }
            val reply = AiServiceManager.sendQuery(chatgptEngine, "Write a viral script")
            assertNotNull(reply)
            assertTrue(reply.isError)
            assertEquals("MISSING_KEY", reply.errorType)
            assertTrue(reply.text.contains("OPENAI_API_KEY"))
        }
    }
}
