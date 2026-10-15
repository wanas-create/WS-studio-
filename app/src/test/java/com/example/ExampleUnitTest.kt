package com.example

import com.example.ads.AdManager
import com.example.model.AiCatalog
import com.example.model.StudioPresets
import com.example.model.VideoProject
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testRealAdMobConfiguration() {
        assertEquals("Real AdMob App ID must match", "ca-app-pub-5230351335453456~3162729190", AdManager.APP_ID)
        assertEquals("Real Banner Unit ID must match", "ca-app-pub-5230351335453456/1555053926", AdManager.BANNER_ID)
        assertEquals("Real Interstitial Unit ID must match", "ca-app-pub-5230351335453456/5520766398", AdManager.INTERSTITIAL_ID)
        assertEquals("Real Rewarded Unit ID must match", "ca-app-pub-5230351335453456/8693724641", AdManager.REWARDED_ID)
        assertEquals("Real Native Unit ID must match", "ca-app-pub-5230351335453456/2927401807", AdManager.NATIVE_ID)
        assertEquals("Real AppOpen Unit ID must match", "ca-app-pub-5230351335453456/6781962899", AdManager.APPOPEN_ID)
    }

    @Test
    fun testAiCatalogHasFiftyEngines() {
        assertEquals("AI Hub must contain exactly 50 AI models", 50, AiCatalog.engines.size)
    }

    @Test
    fun testAiHubInitialChatIsEmpty() {
        assertTrue("AI Hub chat must start empty", AiCatalog.getInitialMessages().isEmpty())
    }

    @Test
    fun testDefaultVideoProjectHasNoPreExistingClips() {
        val project = VideoProject(id = "test", title = "New Project")
        assertTrue("Project must start with no pre-existing video clips", project.clips.isEmpty())
        assertTrue("Project must start with no auto music", project.audioClips.isEmpty())
        assertNull("Project videoUri must be null initially", project.videoUri)
    }

    @Test
    fun testStudioPresetsMeetCapCutRequirements() {
        assertTrue("Filters count must exceed 100", StudioPresets.filters.size >= 100)
        assertTrue("Effects count must exceed 100", StudioPresets.effects.size >= 100)
        assertTrue("Transitions count must be 50 or more", StudioPresets.transitions.size >= 50)
        assertTrue("Fonts count must be 50 or more", StudioPresets.fonts.size >= 50)
    }

    @Test
    fun testVideoClipEffectAndFilterIntensity() {
        val clip = com.example.model.VideoClip(
            id = "clip_1",
            title = "Sample Video",
            durationSec = 35.0f,
            filterName = "Teal & Orange",
            filterIntensity = 0.75f,
            effectName = "Earthquake Shake",
            effectIntensity = 0.9f
        )
        assertEquals("Sample Video", clip.title)
        assertEquals(35.0f, clip.durationSec, 0.001f)
        assertEquals("Teal & Orange", clip.filterName)
        assertEquals(0.75f, clip.filterIntensity, 0.001f)
        assertEquals("Earthquake Shake", clip.effectName)
        assertEquals(0.9f, clip.effectIntensity, 0.001f)
        assertEquals(35000L, clip.endMs)
    }

    @Test
    fun testEffectOverlayCreationAndIntensity() {
        val fx = com.example.model.EffectOverlay(
            id = "fx_test",
            name = "Magnetic Distortion",
            category = "Glitch & VHS",
            intensity = 0.85f,
            startSec = 5.0f,
            durationSec = 2.0f
        )
        assertEquals("Magnetic Distortion", fx.name)
        assertEquals(0.85f, fx.intensity, 0.001f)
        assertEquals(5.0f, fx.startSec, 0.001f)
        assertEquals(2.0f, fx.durationSec, 0.001f)
    }
}
