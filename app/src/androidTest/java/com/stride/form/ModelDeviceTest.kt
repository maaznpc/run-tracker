package com.stride.form

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ModelDeviceTest {
    @Test fun officialPoseImageProducesBodyAndHipLandmarks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val bitmap = instrumentation.context.assets.open("pose.jpg").use { requireNotNull(BitmapFactory.decodeStream(it)) }
        assertNotNull("Official pose fixture must decode", bitmap)
        val detector = PoseProcessor.create(context, RunningMode.IMAGE)
        val input = BitmapImageBuilder(bitmap).build()
        try {
            val result = detector.detect(input)
            val frame = PoseProcessor.frame(result, 0, bitmap.width, bitmap.height)
            assertEquals("Pose model must detect the known person", 33, frame.joints.size)
            assertTrue("Left hip must be visible", frame.joints[23].visibility >= .5f)
            assertTrue("Right hip must be visible", frame.joints[24].visibility >= .5f)
        } finally { input.close(); detector.close(); bitmap.recycle() }
    }

    @Test fun realVideoDecodeAndInferenceWithholdScoreWhenNoRunnerExists() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File(context.cacheDir, "verify-${UUID.randomUUID()}").apply { mkdirs() }
        val video = File(directory, "blank.mp4")
        try {
            instrumentation.context.assets.open("blank-run.mp4").use { source -> video.outputStream().use { destination -> source.copyTo(destination) } }
            val result = PoseProcessor.analyze(context, video, "blank-verification", ViewAngle.SIDE, Facing.RIGHT,
                File(directory,"cover.jpg")) { }
            assertTrue("All 5 seconds should be sampled", result.frames.size >= 45)
            assertNull("No person must never get a fabricated form score", result.score)
            assertEquals(0, result.coverage)
            assertTrue(result.moments.isEmpty())
        } finally { directory.deleteRecursively() }
    }
}
