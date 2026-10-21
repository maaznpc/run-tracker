package com.stride.form

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlin.math.*

@Composable fun ReportScreen(report: RunReport, vm: StrideViewModel) {
    val replayRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf(false) }
    var requestedTime by remember(report.id) { mutableStateOf<Pair<Long, Long>?>(null) }
    var selectedKey by remember(report.id) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopBar(if (report.demo) "Example report" else "Your run report", { vm.page = if (report.demo) Page.HOME else Page.HISTORY }, action = {
            if (!report.demo) Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreHoriz, "Report options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Export analysis (JSON)") }, onClick = { menu = false; vm.share(report) }, leadingIcon = { Icon(Icons.Outlined.FileDownload, null) })
                    DropdownMenuItem(text = { Text("Share original video") }, onClick = { menu = false; vm.share(report, true) }, leadingIcon = { Icon(Icons.Outlined.Share, null) })
                    DropdownMenuItem(text = { Text("Delete session", color = Coral) }, onClick = { menu = false; delete = true }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = Coral) })
                }
            }
        })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow(if (report.demo) "Illustrated sample · synthetic movement" else "${report.view.label} · ${dateText(report.createdAt)}", Olive)
                Text("Get to know\nyour stride.", style = MaterialTheme.typography.headlineLarge)
                if (report.demo) Text("An interactive example with generated landmarks. These are not measurements of a real runner.", color = Coral, fontSize = 12.sp)
            }
            Panel(color = PaleGreen) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ScoreRing(report.score, Modifier.size(100.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Eyebrow("Form consistency", Olive)
                        Text(when { report.score == null -> "A clearer clip will help"; report.score >= 85 -> "A steady foundation"; report.score >= 65 -> "Good things to build on"; else -> "A few patterns to review" }, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
                        Text(if (report.score == null) "Score unavailable" else "Frames within coaching thresholds", color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
                Spacer(Modifier.height(16.dp)); Text(report.status, fontSize = 12.sp, color = Muted, lineHeight = 18.sp)
                TextButton(onClick = { method = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(35.dp)) { Icon(Icons.Outlined.Info, null, Modifier.size(14.dp)); Spacer(Modifier.width(5.dp)); Text("How is this calculated?", fontSize = 11.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QualityTile("DURATION", clockText(report.durationMs), Modifier.weight(1f))
                QualityTile("TRACKED", "${report.coverage}%", Modifier.weight(1f))
                QualityTile("VISIBILITY", "${report.confidence}%", Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Your movement, mapped")
                Box(Modifier.bringIntoViewRequester(replayRequester)) { ReportPlayer(report, vm, requestedTime) }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Legend(Lime, "Tracked joints"); Legend(Coral, "Review moment")
                }
                Text("Tap the timeline or a moment below. The thin trail follows the midpoint of your hips over the previous 3 seconds.", fontSize = 11.sp, color = Muted, lineHeight = 17.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("The details in your stride")
                report.summaries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { metric -> Panel(Modifier.weight(1f).heightIn(min = 166.dp)) {
                            Text(metric.label, color = Muted, fontSize = 12.sp)
                            Text(metric.value, fontSize = 27.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 8.dp), letterSpacing = (-1).sp)
                            Text(metric.note, color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                        } }
                    }
                }
            }
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ShowChart, null, tint = Olive, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(9.dp)); Text("Hip movement", fontWeight = FontWeight.Medium)
                }
                Text("Vertical position over time", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
                HipChart(report, Modifier.fillMaxWidth().height(100.dp))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("0:00", fontSize = 10.sp, color = Muted); Text(clockText(report.durationMs), fontSize = 10.sp, color = Muted) }
                Text("Screen-plane movement; uncalibrated and sensitive to camera motion. Gaps indicate lost tracking.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Moments to learn from")
                if (report.score == null) Panel(color = PaleCoral) {
                    Icon(Icons.Outlined.CenterFocusWeak, null, tint = Coral); Spacer(Modifier.height(10.dp)); Text("Let’s capture a clearer picture", fontWeight = FontWeight.SemiBold)
                    Text(report.status, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                } else if (report.moments.isEmpty()) Panel {
                    Icon(Icons.Outlined.CheckCircleOutline, null, tint = Olive); Spacer(Modifier.height(10.dp)); Text("No sustained flags in this clip", fontWeight = FontWeight.SemiBold)
                    Text("The measured frames stayed within this app’s thresholds. This does not assess every aspect of your running.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                } else report.moments.groupBy { it.key }.forEach { (key, moments) ->
                    val finding = GaitEngine.findings.getValue(key)
                    Panel(color = if (selectedKey == key) PaleCoral else Surface) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = PaleCoral, shape = CircleShape) { Icon(Icons.Outlined.NearMe, null, Modifier.padding(9.dp).size(18.dp), tint = Coral) }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(finding.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp); Text("${moments.size} ${if (moments.size == 1) "moment" else "moments"} to review", fontSize = 11.sp, color = Muted) }
                        }
                        Spacer(Modifier.height(13.dp)); Text(finding.explanation, fontSize = 12.sp, color = Muted, lineHeight = 19.sp)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            moments.forEach { m -> AssistChip(onClick = { selectedKey = key; requestedTime = m.timeMs to System.nanoTime(); scope.launch { replayRequester.bringIntoView() } }, label = { Text(clockText(m.timeMs), fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Outlined.PlayArrow, null, Modifier.size(16.dp), tint = Coral) }) }
                        }
                        HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 12.dp))
                        Eyebrow("Try this next", Olive); Spacer(Modifier.height(6.dp)); Text(finding.cue, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
            Panel(color = Ink) {
                Eyebrow("One small step forward", Lime)
                Text("Awareness before adjustment.", color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 9.dp, bottom = 8.dp))
                Text("Pick one cue, keep it comfortable and film again at the same pace. There is no single perfect way to run.", color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp)); Button(onClick = { vm.page = Page.SETUP }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink), shape = RoundedCornerShape(14.dp)) { Text("Record another run"); Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.Videocam, null, Modifier.size(18.dp)) }
            }
            Text("Coaching observations, not a diagnosis. This app cannot measure forces, injury risk, true 3D pelvic movement or running economy. If running hurts, consult a qualified clinician.", fontSize = 11.sp, color = Muted, lineHeight = 17.sp)
            Spacer(Modifier.height(20.dp))
        }
    }
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Delete this session?") }, text = { Text("The saved video, landmarks and report will be removed from this device. This cannot be undone.") }, confirmButton = { TextButton(onClick = { delete = false; vm.delete(report) }) { Text("Delete session", color = Coral) } }, dismissButton = { TextButton(onClick = { delete = false }) { Text("Keep it") } })
    if (method) AlertDialog(onDismissRequest = { method = false }, title = { Text("A transparent form score") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Score = 100 × usable frames without a sustained flag ÷ all usable frames. We sample video at 10 frames per second.")
            Text("A flag must persist across at least 3 samples, with no gap above 300 ms. Required joint visibility is at least 55%. A score needs 30 usable frames, 4 seconds of observations, at least 50% tracking coverage and 6 alternating leg events.")
            Text("Side view: trunk angle >20°; estimated contact reach >0.35 leg lengths with knee >150°. Rear view: hip-line tilt >8°; inward knee offset >0.20 hip widths.")
            Text("These are broad, unvalidated app thresholds chosen for review, not clinical standards. Foot contact is inferred from pose, not directly detected. Visibility is the model’s landmark confidence, not an accuracy guarantee. Step rate is an estimate; hip excursion is descriptive.")
        }
    }, confirmButton = { TextButton(onClick = { method = false }) { Text("Understood") } })
}
@Composable private fun QualityTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).background(Surface).padding(vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Medium); Spacer(Modifier.height(4.dp)); Text(label, fontSize = 8.sp, letterSpacing = 1.sp, color = Muted)
    }
}
@Composable fun ScoreRing(score: Int?, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            drawArc(Olive.copy(alpha = .14f), 135f, 270f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            score?.let { drawArc(Olive, 135f, 270 * it / 100f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round)) }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(score?.toString() ?: "—", fontSize = 36.sp, fontWeight = FontWeight.Medium, letterSpacing = (-2).sp)
            if (score != null) Text("%", fontSize = 12.sp, color = Olive, modifier = Modifier.padding(bottom = 7.dp, start = 2.dp))
        }
    }
}
@Composable private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(6.dp).background(color, CircleShape)); Text(label, fontSize = 10.sp, color = Muted)
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable fun ReportPlayer(report: RunReport, vm: StrideViewModel, requestedTime: Pair<Long, Long>?) {
    val context = LocalContext.current
    val video = remember(report.id) { if (report.demo) null else vm.store.video(report.id).takeIf { it.exists() } }
    val player = remember(report.id) { video?.let {
        ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(it))); prepare() }
    } }
    var position by remember(report.id) { mutableLongStateOf(0) }
    var playing by remember(report.id) { mutableStateOf(false) }
    var slow by remember(report.id) { mutableStateOf(false) }
    var playbackError by remember(report.id) { mutableStateOf<String?>(null) }
    var showSkeleton by remember { mutableStateOf(true) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { playbackError = "This video cannot be played on this device. The report is still available." }
        }
        player?.addListener(listener)
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) { player?.pause(); playing = false } }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); player?.removeListener(listener); player?.release() }
    }
    LaunchedEffect(player, playing, report.id) {
        while (true) {
            if (report.demo && playing) { position = (position + 50).coerceAtMost(report.durationMs - 100); if (position >= report.durationMs - 100) playing = false }
            else if (player != null) { position = player.currentPosition; playing = player.isPlaying }
            delay(50)
        }
    }
    fun seek(time: Long) { position = time.coerceIn(0, max(0, report.durationMs - 1)); player?.seekTo(position) }
    LaunchedEffect(requestedTime) { requestedTime?.let { seek(it.first); player?.pause(); playing = false } }
    val index = remember(position, report.id) {
        val at = report.frames.binarySearch { it.timeMs.compareTo(position) }
        if (at >= 0) at else (-at - 2).coerceAtLeast(0)
    }
    val frame = report.frames.getOrNull(index)?.takeIf { abs(it.timeMs - position) <= 180 }
    val activeMoments = report.moments.filter { position >= it.timeMs && position <= it.endMs + 100 }
    val highlights = activeMoments.flatMap { GaitEngine.findings[it.key]?.joints.orEmpty() }.toSet()
    val trace = remember(index, report.id) { report.frames.take(index + 1).takeLast(31) }
    Column(Modifier.clip(RoundedCornerShape(22.dp)).background(Ink)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.15f).background(Color(0xFF171D15))) {
            if (report.demo) Canvas(Modifier.fillMaxSize()) {
                for (i in 0..5) drawLine(Color(0xFF3B4630), Offset(0f,size.height*.65f+i*20),Offset(size.width,size.height*.65f+i*20),1f)
                drawCircle(Color(0xFF323C27),size.width*.36f,Offset(size.width*.5f,size.height*.4f),style=Stroke(1f))
            } else if (player != null && playbackError == null) AndroidView(factory = {
                PlayerView(it).apply { this.player = player; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; setShutterBackgroundColor(android.graphics.Color.rgb(23,29,21)) }
            }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
            else Text(playbackError ?: "Video unavailable on this device", Modifier.align(Alignment.Center).padding(24.dp), color = Color.White, fontSize = 12.sp)
            if (showSkeleton && playbackError == null) PoseOverlay(frame, highlights, trace, Modifier.fillMaxSize())
            Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color.Black.copy(alpha = .5f)) { Text(if (report.demo) "ILLUSTRATED EXAMPLE" else "POSE REPLAY", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 8.sp, color = Lime, letterSpacing = 1.sp) }
                IconToggleButton(checked = showSkeleton, onCheckedChange = { showSkeleton = it }, modifier = Modifier.size(34.dp).background(Color.Black.copy(alpha = .4f), CircleShape)) { Icon(Icons.Outlined.AccountTree, "Toggle body tracing", tint = if (showSkeleton) Lime else Color.White, modifier = Modifier.size(18.dp)) }
            }
            activeMoments.firstOrNull()?.let { moment ->
                Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp), shape = RoundedCornerShape(9.dp), color = Coral) { Text(GaitEngine.findings[moment.key]?.title.orEmpty(), Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontSize = 11.sp) }
            }
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (player != null) { if (player.isPlaying) player.pause() else { if (position >= report.durationMs - 200) seek(0); player.play() } }
                    else if (report.demo) { if (position >= report.durationMs - 200) seek(0); playing = !playing }
                }, enabled = player != null || report.demo, modifier = Modifier.size(36.dp)) { Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (playing) "Pause video" else "Play video", tint = Color.White) }
                Text("${clockText(position)} / ${clockText(report.durationMs)}", fontSize = 10.sp, color = Color.White.copy(alpha = .7f))
                Spacer(Modifier.weight(1f)); if (player != null) TextButton(onClick = { slow = !slow; player.setPlaybackSpeed(if (slow) .5f else 1f) }) { Text(if (slow) "0.5×" else "1×", fontSize = 11.sp, color = Lime) }
            }
            Timeline(report, position, { seek(it) }, Modifier.fillMaxWidth().height(30.dp))
            Slider(value = position.toFloat(), onValueChange = { seek(it.toLong()) }, valueRange = 0f..report.durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = Lime, activeTrackColor = Lime, inactiveTrackColor = Color(0xFF4A5140)), modifier = Modifier.height(28.dp).semantics { contentDescription = "Video position" })
        }
    }
}
@Composable private fun Timeline(report: RunReport, position: Long, onSeek: (Long) -> Unit, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "Form timeline. Green is within thresholds, coral is a sustained flag, gray is unscored." }.pointerInput(report.id) {
        detectTapGestures { onSeek((it.x / size.width * report.durationMs).toLong()) }
    }) {
        val count = report.metrics.size.coerceAtLeast(1)
        val width = size.width / count
        report.metrics.forEach { m ->
            val flagged = report.moments.any { m.timeMs in it.timeMs..it.endMs }
            val color = if (!m.valid || report.score == null) Color(0xFF68705E) else if (flagged) Coral else Lime.copy(alpha = .8f)
            drawRect(color, Offset(size.width * m.timeMs / report.durationMs.coerceAtLeast(1), size.height * .2f), androidx.compose.ui.geometry.Size(width + 1, size.height * .6f))
        }
        val x = size.width * position / report.durationMs.coerceAtLeast(1)
        drawLine(Color.White, Offset(x,0f),Offset(x,size.height),2.dp.toPx())
    }
}
@Composable private fun HipChart(report: RunReport, modifier: Modifier) {
    val points = report.metrics.filter { it.valid && it.hipY != null }
    Canvas(modifier) {
        for(i in 0..3) drawLine(Line, Offset(0f,size.height*i/3),Offset(size.width,size.height*i/3),1f)
        if (points.size < 2) return@Canvas
        val low = points.minOf { it.hipY!! }; val high = points.maxOf { it.hipY!! }
        val range = (high-low).coerceAtLeast(.02f)
        val path = Path(); var lastTime = -1000L
        points.forEach { m ->
            val x = size.width*m.timeMs/report.durationMs.coerceAtLeast(1)
            val y = size.height*(.1f+.8f*(m.hipY!!-low)/range)
            if(m.timeMs-lastTime>300) path.moveTo(x,y) else path.lineTo(x,y)
            lastTime = m.timeMs
        }
        drawPath(path,Olive,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
    }
}
