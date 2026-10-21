package com.stride.form

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

val Ink = Color(0xFF252B22)
val Paper = Color(0xFFF6F5F0)
val Surface = Color(0xFFFFFEF9)
val Lime = Color(0xFFD1F078)
val Olive = Color(0xFF536B33)
val Muted = Color(0xFF72776A)
val Line = Color(0xFFE3E5DA)
val Coral = Color(0xFFD66C4E)
val PaleCoral = Color(0xFFF7E6DE)
val PaleGreen = Color(0xFFEDF2DF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Olive, onPrimary = Color.White,
                secondary = Olive, tertiary = Coral, background = Paper, surface = Surface,
                onBackground = Ink, onSurface = Ink, surfaceVariant = PaleGreen, onSurfaceVariant = Muted,
                outline = Line, error = Coral), typography = Typography(
                headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1.4).sp),
                headlineMedium = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 33.sp, letterSpacing = (-.8).sp),
                titleLarge = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 27.sp),
                bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
            )) { StrideApp() }
        }
    }
}

@Composable fun StrideApp(vm: StrideViewModel = viewModel()) {
    val main = vm.page in listOf(Page.HOME, Page.HISTORY, Page.LEARN)
    BackHandler(enabled = vm.page !in listOf(Page.HOME, Page.CAMERA, Page.PROCESSING)) {
        vm.page = if (vm.page == Page.REPORT) Page.HISTORY else Page.HOME
    }
    Scaffold(containerColor = Paper, bottomBar = {
        if (main) NavigationBar(containerColor = Paper, tonalElevation = 0.dp, modifier = Modifier.navigationBarsPadding()) {
            listOf(Triple(Page.HOME, Icons.Outlined.Home, "Home"), Triple(Page.SETUP, Icons.Outlined.CenterFocusStrong, "Analyze"),
                Triple(Page.HISTORY, Icons.Outlined.BarChart, "My runs"), Triple(Page.LEARN, Icons.Outlined.AutoStories, "Learn")).forEach { (page, icon, title) ->
                NavigationBarItem(selected = vm.page == page, onClick = { vm.page = page }, icon = { Icon(icon, title) }, label = { Text(title, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Ink, selectedTextColor = Ink, indicatorColor = Lime, unselectedIconColor = Muted, unselectedTextColor = Muted))
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(if (vm.page == Page.CAMERA) PaddingValues(0.dp) else padding)) {
            when (vm.page) {
                Page.HOME -> HomeScreen(vm)
                Page.SETUP -> SetupScreen(vm)
                Page.CAMERA -> CameraScreen(vm)
                Page.PROCESSING -> ProcessingScreen(vm)
                Page.REPORT -> vm.report?.let { ReportScreen(it, vm) }
                Page.HISTORY -> HistoryScreen(vm)
                Page.LEARN -> LearnScreen()
            }
        }
    }
    if (vm.loadingReport) androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
        Surface(shape = RoundedCornerShape(24.dp), color = Paper) {
            Row(Modifier.padding(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Olive, strokeWidth = 2.dp)
                Text("Opening your report…", fontSize = 14.sp)
            }
        }
    }
    vm.error?.let { message ->
        AlertDialog(onDismissRequest = { vm.error = null }, title = { Text("Let’s try that again") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = { vm.error = null }) { Text("Got it") } })
    }
}

@Composable fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Canvas(Modifier.size(27.dp)) {
            val p = Path().apply { moveTo(size.width * .08f, size.height * .8f); lineTo(size.width * .4f, size.height * .18f); lineTo(size.width * .67f, size.height * .18f); lineTo(size.width * .35f, size.height * .8f); close() }
            drawPath(p, Olive)
            val q = Path().apply { moveTo(size.width * .48f, size.height * .8f); lineTo(size.width * .8f, size.height * .18f); lineTo(size.width, size.height * .18f); lineTo(size.width * .68f, size.height * .8f); close() }
            drawPath(q, Olive)
        }
        Text("stride", fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.4).sp)
    }
}
@Composable fun Eyebrow(text: String, color: Color = Muted) {
    Text(text.uppercase(Locale.getDefault()), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
}
@Composable fun Panel(modifier: Modifier = Modifier, color: Color = Surface, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(color).border(1.dp, Line.copy(alpha = .65f), RoundedCornerShape(24.dp)).padding(20.dp), content = content)
}
@Composable fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = Icons.AutoMirrored.Outlined.ArrowForward, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Lime)) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp); Spacer(Modifier.weight(1f)); Icon(icon, null, Modifier.size(21.dp))
    }
}
@Composable fun TopBar(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Go back") }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}
@Composable fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action, color = Olive, fontSize = 12.sp) }
    }
}
@Composable fun HomeScreen(vm: StrideViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Brand(); Spacer(Modifier.weight(1f))
            Surface(shape = CircleShape, color = PaleGreen) { Icon(Icons.Outlined.DirectionsRun, null, Modifier.padding(12.dp).size(22.dp), tint = Olive) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow("A better run starts with you", Olive)
            Text("Every stride.\nA little better.", style = MaterialTheme.typography.headlineLarge)
            Text("See your movement. Understand your form.\nFind your next small improvement.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
        }
        Box(Modifier.fillMaxWidth().height(238.dp).clip(RoundedCornerShape(26.dp)).background(Ink)) {
            RunnerArtwork(Modifier.fillMaxSize())
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Surface(color = Lime, shape = RoundedCornerShape(6.dp)) { Text("MADE FOR YOUR MOVEMENT", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp)); Text("Your form, in focus.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Outlined.CenterFocusStrong, null, Modifier.align(Alignment.TopEnd).padding(20.dp), tint = Lime)
        }
        Panel(color = PaleGreen) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Meet your running form", fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(5.dp)); Text("A short video. A clearer picture.", fontSize = 13.sp, color = Muted) }
                Icon(Icons.Outlined.AutoAwesome, null, tint = Olive)
            }
            Spacer(Modifier.height(18.dp)); PrimaryButton("Analyze my run", { vm.page = Page.SETUP })
            Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(13.dp), tint = Muted); Text("On your device. Always in your control.", fontSize = 11.sp, color = Muted)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Your running journal", if (vm.sessions.isNotEmpty()) "View all" else null, { vm.page = Page.HISTORY })
            if (vm.sessions.isEmpty()) {
                Panel {
                    Icon(Icons.Outlined.Route, null, tint = Olive); Spacer(Modifier.height(12.dp))
                    Text("A fresh starting line", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text("Your first run report will appear here. Build a picture of how you move, one session at a time.", color = Muted, modifier = Modifier.padding(top = 6.dp))
                }
            } else SessionRow(vm.sessions.first(), { vm.openReport(vm.sessions.first()) })
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { vm.demo() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = PaleCoral) { Icon(Icons.Outlined.PlayArrow, null, Modifier.padding(12.dp), tint = Coral) }
            Column(Modifier.weight(1f).padding(start = 13.dp)) { Text("Take a look around", fontWeight = FontWeight.Medium); Text("Explore an illustrated example report", fontSize = 12.sp, color = Muted) }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(19.dp))
        }
        Spacer(Modifier.height(10.dp))
    }
}
@Composable fun RunnerArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawCircle(Color(0xFF3B4630), w * .37f, androidx.compose.ui.geometry.Offset(w * .76f, h * .42f))
        for (i in 0..4) drawArc(Color(0xFF637245).copy(alpha = .35f), 205f, 150f, false,
            topLeft = androidx.compose.ui.geometry.Offset(w * .35f + i * 18, -h * .4f + i * 6), size = androidx.compose.ui.geometry.Size(w, h * 2), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
        fun v(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(w * x, h * y)
        fun line(a: androidx.compose.ui.geometry.Offset, b: androidx.compose.ui.geometry.Offset, color: Color, width: Float) = drawLine(color, a, b, width, StrokeCap.Round)
        val body = Color(0xFFE5E9D4)
        line(v(.67f,.47f),v(.48f,.62f), Color(0xFF838F69),w*.055f)
        line(v(.48f,.62f),v(.34f,.51f), Color(0xFF838F69),w*.037f)
        line(v(.68f,.49f),v(.78f,.67f),body,w*.065f)
        line(v(.78f,.67f),v(.95f,.65f),body,w*.042f)
        line(v(.7f,.27f),v(.66f,.47f),Lime,w*.095f)
        line(v(.69f,.29f),v(.56f,.34f),body,w*.034f)
        line(v(.56f,.34f),v(.5f,.25f),body,w*.028f)
        line(v(.71f,.29f),v(.78f,.38f),body,w*.036f)
        line(v(.78f,.38f),v(.87f,.29f),body,w*.029f)
        drawCircle(body,w*.035f,v(.73f,.15f))
        listOf(v(.7f,.27f),v(.67f,.47f),v(.78f,.67f),v(.95f,.65f),v(.48f,.62f),v(.34f,.51f)).forEach { drawCircle(Ink,w*.013f,it);drawCircle(Lime,w*.008f,it) }
        val path = Path().apply { moveTo(w*.47f,h*.50f); cubicTo(w*.51f,h*.40f,w*.58f,h*.55f,w*.67f,h*.47f) }
        drawPath(path, Lime.copy(alpha=.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f,6f))))
    }
}
@Composable fun SetupScreen(vm: StrideViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importVideo) }
    Column(Modifier.fillMaxSize()) {
        TopBar("Analyze your run", { vm.page = Page.HOME })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Eyebrow("Your personal movement lab", Olive)
            Text("Let’s see you\nin motion.", style = MaterialTheme.typography.headlineLarge)
            Text("Record a steady 10–20 second clip, or choose a video from your gallery.", color = Muted)
            Row(Modifier.fillMaxWidth().background(Line, RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ViewAngle.entries.forEach { angle ->
                    val chosen = vm.view == angle
                    Surface(onClick = { vm.view = angle }, color = if (chosen) Ink else Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                        Text(angle.label, Modifier.padding(vertical = 13.dp), color = if (chosen) Lime else Muted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Videocam, null, tint = Olive); Spacer(Modifier.width(10.dp)); Text("Set yourself up for clarity", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                CaptureDiagram(vm.view, Modifier.fillMaxWidth().height(112.dp))
                Spacer(Modifier.height(16.dp))
                TipRow("01", if (vm.view == ViewAngle.SIDE) "Film directly from the side" else "Film directly from behind", "Use a fixed, level camera at hip height, 3–5 m away.")
                TipRow("02", "Keep your whole body visible", "Head, hips and feet in frame. One runner, bright light.")
                TipRow("03", "Run at a comfortable, steady pace", "A treadmill or a fixed-camera pass works best. Maximum 2 minutes.")
            }
            if (vm.view == ViewAngle.SIDE) {
                Text("Which way are you running in the video?", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Facing.entries.forEach { facing -> FilterChip(selected = vm.facing == facing, onClick = { vm.facing = facing }, label = { Text(facing.label) }) }
                }
            } else Text("Rear view measures hip-line tilt and knee alignment. Step rate is available in side-view clips only.", color = Muted, fontSize = 13.sp)
            Text("The score summarizes this app’s 2D coaching thresholds. Camera angle matters; it is not a clinical assessment.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PrimaryButton("Record a run", { vm.page = Page.CAMERA }, icon = Icons.Outlined.Videocam)
            TextButton(onClick = { picker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Icon(Icons.Outlined.FileUpload, null, Modifier.size(19.dp)); Spacer(Modifier.width(9.dp)); Text("Choose a video") }
        }
    }
}
@Composable fun TipRow(number: String, title: String, description: String) {
    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(number, color = Olive, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
        Column { Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp); Text(description, color = Muted, fontSize = 12.sp, lineHeight = 18.sp) }
    }
}
@Composable fun CaptureDiagram(view: ViewAngle, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(PaleGreen)) {
        val w = size.width; val h = size.height
        drawLine(Olive.copy(alpha = .2f), androidx.compose.ui.geometry.Offset(0f,h*.85f), androidx.compose.ui.geometry.Offset(w,h*.85f),2f)
        val cx = w*.68f
        drawRoundRect(Olive, androidx.compose.ui.geometry.Offset(w*.14f,h*.38f), androidx.compose.ui.geometry.Size(w*.09f,h*.22f), androidx.compose.ui.geometry.CornerRadius(8f))
        drawLine(Olive, androidx.compose.ui.geometry.Offset(w*.185f,h*.6f), androidx.compose.ui.geometry.Offset(w*.185f,h*.85f),3f)
        drawLine(Olive.copy(alpha=.5f), androidx.compose.ui.geometry.Offset(w*.29f,h*.48f), androidx.compose.ui.geometry.Offset(w*.51f,h*.48f),2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f,8f)))
        drawCircle(Olive,h*.065f,androidx.compose.ui.geometry.Offset(cx,h*.23f))
        fun l(x1:Float,y1:Float,x2:Float,y2:Float) = drawLine(Olive,androidx.compose.ui.geometry.Offset(cx+x1*w,y1*h),androidx.compose.ui.geometry.Offset(cx+x2*w,y2*h),6f,StrokeCap.Round)
        l(0f,.34f,0f,.57f)
        if(view==ViewAngle.REAR) { l(-.07f,.38f,.07f,.38f);l(-.07f,.38f,-.09f,.56f);l(.07f,.38f,.09f,.56f);l(0f,.57f,-.06f,.8f);l(0f,.57f,.06f,.8f) }
        else { l(0f,.39f,-.05f,.49f);l(-.05f,.49f,-.09f,.44f);l(0f,.57f,.07f,.68f);l(.07f,.68f,.14f,.78f);l(0f,.57f,-.08f,.71f);l(-.08f,.71f,-.13f,.64f) }
    }
}
@Composable fun ProcessingScreen(vm: StrideViewModel) {
    var confirm by remember { mutableStateOf(false) }
    BackHandler { confirm = true }
    Column(Modifier.fillMaxSize().padding(30.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { vm.progress }, modifier = Modifier.size(126.dp), color = Olive, trackColor = Line, strokeWidth = 5.dp)
            Icon(Icons.Outlined.DirectionsRun, null, Modifier.size(44.dp), tint = Olive)
        }
        Spacer(Modifier.height(36.dp)); Eyebrow("Movement into insight", Olive); Spacer(Modifier.height(12.dp))
        Text(vm.processingLabel, style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("${(vm.progress * 100).toInt()}%", Modifier.padding(16.dp), color = Olive, fontSize = 22.sp)
        Text("Tracing your joints, reviewing patterns and finding moments worth a closer look. Keep the app open.", color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(20.dp)); TextButton(onClick = { confirm = true }) { Text("Cancel analysis") }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Cancel this analysis?") }, text = { Text("This unsaved clip will be removed. You can choose it again later.") }, confirmButton = { TextButton(onClick = { confirm = false; vm.cancelProcessing() }) { Text("Cancel analysis") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep analyzing") } })
}
fun clockText(time: Long): String = "%d:%02d".format(Locale.US, time / 60000, (time / 1000) % 60)
fun dateText(time: Long): String = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(time))
@Composable fun SessionRow(r: RunReport, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Surface, border = BorderStroke(1.dp, Line)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(54.dp).background(PaleGreen, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Text(r.score?.toString() ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 23.sp, color = Olive) }
            Column(Modifier.weight(1f)) { Text(r.view.label + " session", fontWeight = FontWeight.Medium); Text(dateText(r.createdAt), color = Muted, fontSize = 12.sp); Text("${clockText(r.durationMs)} · ${r.coverage}% tracked", color = Muted, fontSize = 11.sp) }
            Icon(Icons.Outlined.ChevronRight, "Open report", tint = Muted)
        }
    }
}
@Composable fun HistoryScreen(vm: StrideViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Eyebrow("Your running journal", Olive); Text("Small steps.\nVisible progress.", style = MaterialTheme.typography.headlineLarge)
        Text("${vm.sessions.size} saved ${if (vm.sessions.size == 1) "session" else "sessions"} · stored on this device", color = Muted, fontSize = 13.sp)
        val chartView = vm.sessions.firstOrNull { it.score != null }?.view
        val scored = vm.sessions.filter { it.score != null && it.view == chartView }.take(8).reversed()
        if (scored.size >= 2) Panel { Text("Recent ${chartView?.label?.lowercase()} scores", fontWeight = FontWeight.Medium); Spacer(Modifier.height(18.dp)); ScoreChart(scored, Modifier.fillMaxWidth().height(100.dp)); Text("Compare the same view, pace and camera setup.", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 10.dp)) }
        if (vm.sessions.isEmpty()) {
            Spacer(Modifier.height(20.dp)); Panel {
                Icon(Icons.Outlined.Route, null, Modifier.size(40.dp), tint = Olive); Spacer(Modifier.height(16.dp))
                Text("Your story starts here", style = MaterialTheme.typography.titleLarge); Text("Record your first run to save a report, revisit key moments and follow your form over time.", color = Muted, modifier = Modifier.padding(vertical = 12.dp))
                PrimaryButton("Analyze my first run", { vm.page = Page.SETUP })
            }
        } else vm.sessions.forEach { r -> SessionRow(r) { vm.openReport(r) } }
        Text("Your videos and reports stay local. Deleting the app removes your journal; export reports you want to keep.", color = Muted, fontSize = 12.sp)
    }
}
@Composable fun ScoreChart(reports: List<RunReport>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        for(i in 0..2) drawLine(Line, androidx.compose.ui.geometry.Offset(0f,size.height*i/2), androidx.compose.ui.geometry.Offset(size.width,size.height*i/2),1f)
        val p = Path()
        reports.forEachIndexed { i,r ->
            val x = size.width * i / (reports.size-1); val y = size.height * (1-(r.score?:0)/100f)
            if(i==0) p.moveTo(x,y) else p.lineTo(x,y)
        }
        drawPath(p,Olive,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(),cap=StrokeCap.Round))
        reports.forEachIndexed { i,r -> drawCircle(Olive,5.dp.toPx(),androidx.compose.ui.geometry.Offset(size.width*i/(reports.size-1),size.height*(1-(r.score?:0)/100f))) }
    }
}
