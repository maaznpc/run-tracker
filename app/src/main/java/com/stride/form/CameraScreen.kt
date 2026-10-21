package com.stride.form

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

@Composable fun CameraScreen(vm: StrideViewModel) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    var capture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var recordingStarted by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var pose by remember { mutableStateOf<PoseFrame?>(null) }
    var metrics by remember { mutableStateOf<FrameMetrics?>(null) }
    var hipTrace by remember { mutableStateOf(emptyList<PoseFrame>()) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var finalizing by remember { mutableStateOf(false) }
    val discardRequested = remember { AtomicBoolean(false) }
    val isRecording = recording != null
    val currentRecording by rememberUpdatedState(recording)
    fun leave() {
        if (isRecording || finalizing) confirmLeave = true else vm.discardRecording()
    }
    BackHandler { leave() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) currentRecording?.stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); currentRecording?.stop() }
    }
    LaunchedEffect(recordingStarted) {
        while (recordingStarted) {
            elapsed = SystemClock.elapsedRealtime() - startedAt
            if (elapsed >= PoseProcessor.MAX_DURATION_MS - 1500L) { finalizing = true; recording?.stop(); break }
            delay(100)
        }
    }
    if (!permission) {
        Column(Modifier.fillMaxSize().background(Paper)) {
            TopBar("Your running camera", { vm.page = Page.SETUP })
            Column(Modifier.weight(1f).padding(28.dp), verticalArrangement = Arrangement.Center) {
                Surface(color = PaleGreen, shape = CircleShape) { Icon(Icons.Outlined.Videocam, null, Modifier.padding(24.dp).size(42.dp), tint = Olive) }
                Spacer(Modifier.height(24.dp)); Text("Put your form\nin the picture.", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(16.dp)); Text("Allow camera access to record and track your movement. No microphone access is needed. Your video stays on this device.", color = Muted)
                Spacer(Modifier.height(24.dp)); PrimaryButton("Allow camera access", { request.launch(Manifest.permission.CAMERA) }, icon = Icons.Outlined.Videocam)
                TextButton(onClick = { vm.page = Page.SETUP }, modifier = Modifier.fillMaxWidth()) { Text("Choose a video instead") }
                Text("If permission was permanently denied, enable Camera for Stride in Android Settings.", color = Muted, fontSize = 12.sp)
            }
        }
        return
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF13170F))) {
        CameraFeed(vm.view, vm.facing, Modifier.fillMaxSize(),
            onReady = { capture = it },
            onPose = { frame, m ->
                pose = frame; metrics = m
                hipTrace = if (m.valid) (hipTrace + frame).takeLast(40) else emptyList()
            }, onError = { cameraError = it })
        PoseOverlay(pose, metrics?.flags.orEmpty().flatMap { GaitEngine.findings[it]?.joints.orEmpty() }.toSet(), hipTrace,
            Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { leave() }, modifier = Modifier.background(Color.Black.copy(alpha = .35f), CircleShape)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                Surface(color = Color.Black.copy(alpha = .55f), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(7.dp).background(if (recordingStarted) Coral else Lime, CircleShape))
                        Text(if (recordingStarted) clockText(elapsed) else vm.view.label, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .75f)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)) {
                val valid = metrics?.valid == true
                val flags = metrics?.flags.orEmpty()
                Text(cameraError ?: if (finalizing) "Saving your recording…" else if (!valid) "Step back · keep head, hips and feet visible" else if (flags.isEmpty()) "Body tracked · keep running naturally" else GaitEngine.findings[flags.first()]?.title ?: "Review your form", color = if (cameraError != null || flags.isNotEmpty()) Color(0xFFF3AF93) else Lime, fontSize = 13.sp)
                if (cameraError == null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LiveStat("TRACKING", if (valid) "${((metrics?.confidence ?: 0f) * 100).toInt()}%" else "—")
                    if (vm.view == ViewAngle.SIDE) LiveStat("TRUNK", metrics?.lean?.let { "%.0f°".format(it) } ?: "—")
                    else LiveStat("HIP TILT", metrics?.pelvicTilt?.let { "%.0f°".format(it) } ?: "—")
                    LiveStat("VIEW", if (vm.view == ViewAngle.SIDE) "SIDE" else "REAR")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { confirmLeave = true }, enabled = isRecording && !finalizing) { Icon(Icons.Outlined.Close, "Discard recording", tint = if (isRecording) Color.White else Color.Transparent) }
                    Spacer(Modifier.width(30.dp))
                    FilledIconButton(onClick = {
                        if (isRecording) { finalizing = true; recording?.stop() }
                        else {
                            val vc = capture ?: return@FilledIconButton
                            val file = vm.allocateRecording(); discardRequested.set(false)
                            try {
                                recording = vc.output.prepareRecording(context, FileOutputOptions.Builder(file).build()).start(ContextCompat.getMainExecutor(context)) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> { recordingStarted = true; startedAt = SystemClock.elapsedRealtime() }
                                        is VideoRecordEvent.Finalize -> {
                                            recordingStarted = false; recording = null; finalizing = false
                                            if (discardRequested.get()) { vm.discardRecording() }
                                            else if (event.hasError()) {
                                                cameraError = "Recording failed (${event.error}). Try a shorter clip or import a video."
                                                vm.activeId?.let { vm.store.delete(it) }; vm.activeId = null
                                            } else vm.recorded(file)
                                        }
                                    }
                                }
                            } catch (e: Exception) { cameraError = e.message ?: "Could not start recording."; vm.activeId?.let { vm.store.delete(it) }; vm.activeId = null }
                        }
                    }, enabled = capture != null && cameraError == null && !finalizing,
                        modifier = Modifier.size(78.dp).border(2.dp, Color.White.copy(alpha = .7f), CircleShape).padding(7.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isRecording) Coral else Lime, contentColor = Ink)) {
                        Icon(if (isRecording) Icons.Outlined.Stop else Icons.Outlined.FiberManualRecord, if (isRecording) "Finish and analyze" else "Start recording", Modifier.size(33.dp))
                    }
                    Spacer(Modifier.width(30.dp)); Box(Modifier.size(48.dp))
                }
                Text(if (isRecording) "Tap stop for your full report · 2 min maximum" else "Tap to record · live cues are preliminary", color = Color.White.copy(alpha = .6f), fontSize = 11.sp)
                if (cameraError != null) TextButton(onClick = { vm.discardRecording() }) { Text("Back to video selection", color = Lime) }
            }
        }
    }
    if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false }, title = { Text("Discard this recording?") }, text = { Text("The current clip will be deleted. Finish recording to keep it and see a report.") }, confirmButton = { TextButton(onClick = {
        confirmLeave = false; discardRequested.set(true)
        if (recording != null) recording?.stop() else vm.discardRecording()
    }) { Text("Discard") } }, dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep recording") } })
}
@Composable private fun LiveStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        Text(label, color = Color.White.copy(alpha = .5f), fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable private fun CameraFeed(view: ViewAngle, facing: Facing, modifier: Modifier,
                                   onReady: (VideoCapture<Recorder>) -> Unit,
                                   onPose: (PoseFrame, FrameMetrics) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FIT_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    } }
    val latestPose by rememberUpdatedState(onPose)
    val latestError by rememberUpdatedState(onError)
    val latestReady by rememberUpdatedState(onReady)
    AndroidView(factory = { previewView }, modifier = modifier)
    DisposableEffect(owner, view, facing) {
        val closed = AtomicBoolean(false)
        val executor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        var detector: PoseLandmarker? = null
        var provider: ProcessCameraProvider? = null
        val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))).build()
        val capture = VideoCapture.withOutput(recorder)
        var lastFrame = 0L
        var failed = false
        analysis.setAnalyzer(executor) { image ->
            try {
                val now = SystemClock.uptimeMillis()
                if (!closed.get() && !failed && now - lastFrame >= 80) {
                    lastFrame = now
                    if (detector == null) detector = PoseProcessor.create(context, RunningMode.VIDEO)
                    val rotation = image.imageInfo.rotationDegrees
                    val bitmap = image.toBitmap()
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
                    try {
                        val input = BitmapImageBuilder(rotated).build()
                        try {
                            val result = detector!!.detectForVideo(input, now)
                            val frame = PoseProcessor.frame(result, now, rotated.width, rotated.height)
                            val metrics = GaitEngine.measure(frame, view, facing)
                            main.execute { if (!closed.get()) latestPose(frame, metrics) }
                        } finally { input.close() }
                    } finally {
                        if (rotated !== bitmap) rotated.recycle()
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                failed = true
                main.execute { if (!closed.get()) latestError(e.message ?: "Body tracking could not start.") }
            } finally { image.close() }
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!closed.get()) try {
                provider = future.get()
                // Preview and analysis request the same 4:3 sensor aspect; both render with FIT_CENTER.
                previewView.post {
                    if (!closed.get()) try {
                        val group = UseCaseGroup.Builder().addUseCase(preview).addUseCase(analysis).addUseCase(capture)
                        provider!!.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, group.build())
                        latestReady(capture)
                    } catch (e: Exception) { latestError("This camera cannot run recording and tracking together. Import a video instead. ${e.message.orEmpty()}") }
                }
            } catch (e: Exception) { latestError("Camera unavailable. Close other camera apps and try again.") }
        }, main)
        onDispose {
            closed.set(true); analysis.clearAnalyzer(); provider?.unbind(preview, analysis, capture)
            executor.execute { detector?.close() }; executor.shutdown()
        }
    }
}

@Composable fun PoseOverlay(frame: PoseFrame?, highlights: Set<Int>, trace: List<PoseFrame> = emptyList(), modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val f = frame ?: return@Canvas
        if (f.joints.size < 33 || f.width <= 0 || f.height <= 0) return@Canvas
        val scale = min(size.width / f.width, size.height / f.height)
        val offsetX = (size.width - f.width * scale) / 2
        val offsetY = (size.height - f.height * scale) / 2
        fun xy(j: Joint) = Offset(offsetX + j.x * f.width * scale, offsetY + j.y * f.height * scale)
        if (trace.size > 1) {
            val path = Path(); var started = false; var lastTime = 0L
            trace.forEach { old ->
                if (old.joints.size >= 25 && old.joints[23].visibility >= .55f && old.joints[24].visibility >= .55f) {
                    val h = Joint((old.joints[23].x + old.joints[24].x) / 2, (old.joints[23].y + old.joints[24].y) / 2)
                    val o = xy(h)
                    if (!started || old.timeMs - lastTime > 300) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                    started = true; lastTime = old.timeMs
                } else started = false
            }
            drawPath(path, Lime.copy(alpha = .7f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        GaitEngine.connections.forEach { (a, b) ->
            if (f.joints[a].visibility >= .55f && f.joints[b].visibility >= .55f)
                drawLine(if (a in highlights || b in highlights) Coral else Lime, xy(f.joints[a]), xy(f.joints[b]), 2.4.dp.toPx(), StrokeCap.Round)
        }
        (11..32).filter { f.joints[it].visibility >= .55f }.forEach { i ->
            val point = xy(f.joints[i]); val color = if (i in highlights) Coral else Lime
            if (i in highlights) drawCircle(color.copy(alpha = .18f), 12.dp.toPx(), point)
            drawCircle(Ink, 4.dp.toPx(), point); drawCircle(color, 2.7.dp.toPx(), point)
        }
    }
}
