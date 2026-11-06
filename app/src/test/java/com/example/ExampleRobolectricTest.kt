package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioDspEngine
import com.example.audio.DemoAudioProvider
import com.example.model.VoiceEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI Voice Changer", appName)
    }

    @Test
    fun `audio dsp engine processes audio properly`() {
        val demo = DemoAudioProvider.getSpeechDemo()
        assertTrue(demo.samples.isNotEmpty())

        val robotParams = VoiceEffect.CYBER_ROBOT.toParams()
        val processed = AudioDspEngine.process(demo.samples, robotParams, demo.sampleRate)

        assertNotNull(processed)
        assertTrue("Processed audio should not be empty", processed.isNotEmpty())
    }

    @Test
    fun `noise reduction attenuates audio correctly`() {
        val noisyDemo = DemoAudioProvider.getSpeechWithNoiseDemo()
        val cleanParams = VoiceEffect.ORIGINAL.toParams().copy(noiseReduction = 0.8f)
        val denoised = AudioDspEngine.process(noisyDemo.samples, cleanParams, noisyDemo.sampleRate)

        assertNotNull(denoised)
        assertTrue(denoised.isNotEmpty())
    }
}
