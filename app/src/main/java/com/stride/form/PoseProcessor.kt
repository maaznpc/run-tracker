package com.stride.form

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

object PoseProcessor {
    const val MAX_DURATION_MS = 120000L
    fun create(context: Context, mode: RunningMode = RunningMode.VIDEO): PoseLandmarker {
        try { context.assets.open("pose_landmarker_lite.task").close() }
        catch (e: Exception) { throw IllegalStateException("The pose model is missing. Build the app with scripts/setup.sh to bundle the on-device model.", e) }
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build())
            .setRunningMode(mode).setNumPoses(1).setMinPoseDetectionConfidence(.6f)
            .setMinPosePresenceConfidence(.6f).setMinTrackingConfidence(.6f).build()
        return PoseLandmarker.createFromOptions(context, options)
    }
    fun frame(result: PoseLandmarkerResult, timeMs: Long, width: Int, height: Int): PoseFrame {
        val points = result.landmarks().firstOrNull().orEmpty().map { Joint(it.x(), it.y(), it.visibility().orElse(0f)) }
        return PoseFrame(timeMs, points, width, height)
    }
    suspend fun analyze(context: Context, file: File, id: String, view: ViewAngle, facing: Facing,
                        cover: File, progress: (Float) -> Unit): RunReport {
        val retriever = MediaMetadataRetriever()
        var detector: PoseLandmarker? = null
        try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: error("This video cannot be read. Choose a video recorded by your phone.")
            require(duration >= 4000) { "This clip is too short. Choose at least 5 seconds of continuous running." }
            require(duration <= MAX_DURATION_MS) { "Choose a clip of 2 minutes or less. Trim the video in your gallery first." }
            detector = create(context)
            val engine = GaitEngine(view, facing)
            var count = 0
            for (time in 0 until duration step 100L) {
                currentCoroutineContext().ensureActive()
                val bitmap = retriever.getScaledFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST, 720, 720)
                if (bitmap != null) {
                    var input = bitmap
                    if (bitmap.config != Bitmap.Config.ARGB_8888) input = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                    try {
                        if (count == 0) cover.outputStream().use { input.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                        val image = BitmapImageBuilder(input).build()
                        try { engine.add(frame(detector.detectForVideo(image, time), time, input.width, input.height)) }
                        finally { image.close() }
                        count++
                    } finally { if (input !== bitmap) input.recycle(); bitmap.recycle() }
                } else engine.add(PoseFrame(time, emptyList(), 1, 1))
                progress(time.toFloat() / duration)
            }
            require(count > 0) { "No frames could be decoded. Try a different video format." }
            progress(1f)
            return engine.finish(id, duration)
        } finally { detector?.close(); retriever.release() }
    }
}
