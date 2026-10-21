package com.stride.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun LearnScreen() {
    val uri = LocalUriHandler.current
    var error by remember { mutableStateOf(false) }
    fun open(url: String) { runCatching { uri.openUri(url) }.onFailure { error = true } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Eyebrow("The thinking behind your stride", Olive)
        Text("Run curious.\nMove naturally.", style = MaterialTheme.typography.headlineLarge)
        Text("Understand what we can see, what research suggests and where a video has limits.", color = Muted)
        Panel(color = PaleGreen) {
            Icon(Icons.Outlined.Spa, null, tint = Olive); Spacer(Modifier.height(14.dp))
            Text("There’s no single perfect form.", style = MaterialTheme.typography.titleLarge)
            Text("Your movement depends on pace, anatomy, experience and fatigue. A visual pattern is a prompt for review. It does not automatically need correction.", color = Muted, modifier = Modifier.padding(top = 10.dp))
        }
        LearnCard("01", "Start with a clear view", "A fixed, level camera and a whole-body view matter more than any score. Use the same camera position and pace when comparing sessions. A moving or tilted phone can create misleading hip and trunk measurements.")
        LearnCard("02", "Let your cadence be personal", "Step-rate changes can alter lower-limb mechanics, but there is no universal 180-spm target. Research is less conclusive about injury prevention and performance. Make gradual, comfortable changes with a qualified professional when appropriate.")
        LearnCard("03", "Look for repeated patterns", "One unusual frame may be a tracking error. Stride flags a pattern only after several consecutive detections. Review highlighted video moments and repeat a clip before acting on a cue.")
        LearnCard("04", "Keep the whole story in mind", "Sleep, recovery, training load, strength and comfort all matter. Phone video cannot measure ground reaction force, tissue load, true 3D joint motion, running economy or injury risk.")
        SectionTitle("Built on open research")
        Panel {
            Eyebrow("Systematic review · 2022", Olive)
            Text("Changing running step rate", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            Text("Anderson et al. reviewed 37 studies of cadence, biomechanics, injury and performance. Evidence for biomechanical changes is stronger than for injury and performance outcomes.", fontSize = 12.sp, color = Muted)
            TextButton(onClick = { open("https://pmc.ncbi.nlm.nih.gov/articles/PMC9441414/") }, contentPadding = PaddingValues(0.dp)) { Text("Read the review", fontSize = 12.sp); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(14.dp)) }
        }
        Panel {
            Eyebrow("Systematic review · 2022", Olive)
            Text("What gait retraining changes", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            Text("Doyle et al. reviewed 19 trials. Gait retraining altered selected biomechanical measures; certainty varied, and pain and injury outcomes were sparsely reported.", fontSize = 12.sp, color = Muted)
            TextButton(onClick = { open("https://pubmed.ncbi.nlm.nih.gov/35128941/") }, contentPadding = PaddingValues(0.dp)) { Text("View on PubMed", fontSize = 12.sp); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(14.dp)) }
        }
        Panel {
            Eyebrow("On-device pose tracking", Olive)
            Text("Google MediaPipe", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            Text("The Pose Landmarker Lite model detects 33 landmarks. Stride uses 2D image coordinates and landmark visibility. Its rule-based coaching score is separate from the pose model and has not been clinically validated.", fontSize = 12.sp, color = Muted)
            TextButton(onClick = { open("https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker") }, contentPadding = PaddingValues(0.dp)) { Text("Model documentation", fontSize = 12.sp); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(14.dp)) }
            TextButton(onClick = { open("https://github.com/google-ai-edge/mediapipe-samples/tree/main/examples/pose_landmarker/android") }, contentPadding = PaddingValues(0.dp)) { Text("Android reference on GitHub", fontSize = 12.sp); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(14.dp)) }
        }
        Panel(color = Ink) {
            Eyebrow("Privacy by design", Lime)
            Text("Your movement stays yours.", style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
            Text("Camera frames are processed on this phone. Stride has no account, analytics SDK, internet permission or server upload. Videos and reports remain in app-private storage until you choose to share or delete them.", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .7f))
        }
        Text("Stride 1.0 · Android 8.0+\nCameraX · MediaPipe · Jetpack Compose\nNot a medical device. Stop and seek professional advice if running causes pain.", color = Muted, fontSize = 11.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
    }
    if(error) AlertDialog(onDismissRequest = { error = false }, title = { Text("No browser available") }, text = { Text("Install or enable a browser to open research links.") }, confirmButton = { TextButton(onClick = { error = false }) { Text("Got it") } })
}
@Composable private fun LearnCard(number: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(number, color = Olive, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        Column { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium); Spacer(Modifier.height(7.dp)); Text(body, fontSize = 13.sp, lineHeight = 21.sp, color = Muted) }
    }
}
