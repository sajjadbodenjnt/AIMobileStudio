package com.droidforge.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private val Bg = Color(0xFF0D1117)
private val Panel = Color(0xFF111820)
private val Panel2 = Color(0xFF161F29)
private val Green = Color(0xFF00E676)
private val Cyan = Color(0xFF20C8FF)
private val White = Color(0xFFE6EDF3)
private val Muted = Color(0xFF8B9AAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Bg, surface = Panel)) {
                DroidForgeScreen(File(filesDir, "generated-project")) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sajjadbodenjnt/AIMobileStudio/releases/latest")))
                }
            }
        }
    }
}

@Composable
private fun DroidForgeScreen(projectDir: File, openReleases: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("// Your generated Android source appears here\n\nclass MainActivity {\n    // Describe an app to generate with AI\n}") }
    var log by remember { mutableStateOf("SYSTEM READY  •  DroidForge workspace initialized\nDownload and load the Qwen GGUF model to enable offline code generation.") }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(-1L) }
    var progressStage by remember { mutableStateOf("Model not loaded") }
    var downloading by remember { mutableStateOf(false) }
    var loadingModel by remember { mutableStateOf(false) }
    var modelReady by remember { mutableStateOf(false) }
    var generating by remember { mutableStateOf(false) }
    var selectedNav by remember { mutableStateOf("App Builder") }
    var selectedTab by remember { mutableStateOf("MainActivity.kt") }
    var showPreview by remember { mutableStateOf(true) }
    val downloader = remember { ModelDownloader(context) }

    Column(Modifier.fillMaxSize().background(Bg).padding(8.dp)) {
        // Top application bar
        Row(
            Modifier.fillMaxWidth().height(48.dp).background(Panel, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("▣", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("DroidForge AI", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("AI-POWERED ANDROID APP BUILDER", color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
            }
            Surface(color = Color(0xFF07351F), shape = RoundedCornerShape(30.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Green)) {
                Text(if (modelReady) "● OFFLINE • READY" else "● OFFLINE • MODEL NEEDED", color = Green, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
            Spacer(Modifier.width(10.dp))
            TextButton(onClick = openReleases) { Text("APK RELEASES ↗", color = Cyan, fontSize = 10.sp) }
        }
        Spacer(Modifier.height(7.dp))

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            // Navigation rail
            Column(Modifier.width(112.dp).fillMaxHeight().background(Panel, RoundedCornerShape(12.dp)).padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("⌂  Home", "✦  AI Assistant", "‹/›  Code Editor", "🚀  App Builder", "▣  Projects", "▦  Templates", "⚙  Settings").forEach { item ->
                    val label = item.substringAfter("  ")
                    val active = selectedNav == label
                    Surface(
                        onClick = { selectedNav = label },
                        color = if (active) Color(0xFF063D25) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, Green) else null
                    ) {
                        Text(item, color = if (active) Green else White, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 10.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("v0.2 • DEV", color = Muted, fontSize = 8.sp, modifier = Modifier.padding(5.dp))
            }

            // Main editor and console
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionTile("✦  Create with AI", "Generate source", Green, Modifier.weight(1f)) {
                        selectedNav = "AI Assistant"
                        if (prompt.isBlank()) prompt = "Create a simple Android quiz app with a home screen, multiple-choice questions, score and restart button."
                    }
                    ActionTile("‹/›  Build APK", "Open build releases", Cyan, Modifier.weight(1f), openReleases)
                    ActionTile("▶  Run Preview", "Open visual designer", Green, Modifier.weight(1f)) { context.startActivity(Intent(context, DesignerActivity::class.java)) }
                }

                Column(Modifier.weight(1f).fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF214333), RoundedCornerShape(10.dp))) {
                    Row(Modifier.fillMaxWidth().height(36.dp).background(Panel2, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)), verticalAlignment = Alignment.CenterVertically) {
                        listOf("MainActivity.kt", "build.gradle.kts", "AndroidManifest.xml").forEach { tab ->
                            Surface(onClick = { selectedTab = tab }, color = if (selectedTab == tab) Bg else Color.Transparent) {
                                Text(tab, color = if (selectedTab == tab) Green else Muted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 11.dp))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("● EDITABLE", color = Green, fontSize = 8.sp, modifier = Modifier.padding(end = 10.dp))
                    }
                    Box(Modifier.fillMaxSize().padding(8.dp)) {
                        BasicTextField(
                            value = code,
                            onValueChange = { code = it },
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            textStyle = TextStyle(color = White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 17.sp),
                            decorationBox = { inner -> if (code.isEmpty()) Text("Generated source code…", color = Muted) else inner() }
                        )
                    }
                }

                Column(Modifier.fillMaxWidth().height(88.dp).background(Color(0xFF080E13), RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF17412D), RoundedCornerShape(10.dp)).padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("● BUILD OUTPUT / CONSOLE", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { log = "Console cleared." }, contentPadding = PaddingValues(0.dp)) { Text("CLEAR", color = Muted, fontSize = 8.sp) }
                    }
                    Text(log, color = White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.verticalScroll(rememberScrollState()))
                }
            }

            // AI model / prompt panel
            Column(Modifier.width(184.dp).fillMaxHeight().background(Panel, RoundedCornerShape(12.dp)).padding(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("AI MODEL MANAGER", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Qwen2.5-Coder • GGUF Q4", color = White, fontSize = 9.sp)
                Text(if (modelReady) "● MODEL READY" else "○ MODEL NOT LOADED", color = if (modelReady) Green else Color(0xFFFFC857), fontSize = 9.sp)
                Button(
                    onClick = {
                        scope.launch {
                            downloading = true; loadingModel = false; modelReady = false
                            progress = 0f; downloadedBytes = 0L; totalBytes = -1L
                            progressStage = "Downloading model"; log = "Downloading Qwen model..."
                            try {
                                val file = downloader.download { done, total ->
                                    downloadedBytes = done; totalBytes = total
                                    progress = if (total > 0L) done.toFloat() / total.toFloat() else 0f
                                }
                                downloading = false; loadingModel = true; progress = 0f
                                progressStage = "Loading model into RAM"
                                val callback = object : ModelLoadProgress {
                                    override fun onProgress(value: Float) { progress = value.coerceIn(0f, 1f) }
                                }
                                modelReady = withContextCompat { LlamaNative.initModel(file.absolutePath, callback) }
                                progress = if (modelReady) 1f else progress
                                progressStage = if (modelReady) "Model ready" else "Model load failed"
                                log = if (modelReady) "SUCCESS: Qwen model loaded. Offline inference ready." else "ERROR: Native model initialization failed."
                            } catch (e: Exception) {
                                log = "MODEL ERROR: ${e.message}"; progressStage = "Download/load error"
                            } finally { downloading = false; loadingModel = false }
                        }
                    },
                    enabled = !downloading && !loadingModel && !generating,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(4.dp)
                ) { Text(if (downloading) "Downloading…" else if (loadingModel) "Loading…" else "↓  Download / Load", fontSize = 9.sp) }

                if (downloading || loadingModel) {
                    val pct = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Green)
                    Text(progressStage, color = White, fontSize = 8.sp)
                    if (downloading) {
                        val doneMb = downloadedBytes / (1024f * 1024f)
                        val totalMb = if (totalBytes > 0L) totalBytes / (1024f * 1024f) else 0f
                        Text(if (totalBytes > 0L) "$pct% • %.1f / %.1f MB".format(doneMb, totalMb) else "$pct% • %.1f MB".format(doneMb), color = Green, fontSize = 9.sp)
                    } else Text("$pct% • Loading tensors", color = Green, fontSize = 9.sp)
                } else if (modelReady) Text("100% • READY", color = Green, fontSize = 9.sp)

                Divider(color = Color(0xFF293746))
                Text("APP PROMPT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = prompt, onValueChange = { prompt = it },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    placeholder = { Text("বাংলা বা English-এ অ্যাপের বর্ণনা লিখুন…", fontSize = 9.sp) },
                    textStyle = TextStyle(color = White, fontSize = 10.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Muted, cursorColor = Green)
                )
                Button(
                    onClick = {
                        if (!modelReady) { log = "Load the offline model first."; return@Button }
                        scope.launch {
                            generating = true; log = "Generating source on device…"
                            try {
                                code = withContextCompat { QwenCoderEngine().generate(prompt) }
                                ProjectScaffolder(projectDir).writeComposeProject(code)
                                log = "SUCCESS: AI source generated and project scaffolded at ${projectDir.absolutePath}\nNote: APK packaging is performed by the connected build workflow, not locally on this screen."
                                selectedTab = "MainActivity.kt"
                            } catch (e: Exception) { log = "GENERATION ERROR: ${e.message}" }
                            finally { generating = false }
                        }
                    },
                    enabled = !generating && !downloading && !loadingModel && prompt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(4.dp)
                ) { Text(if (generating) "Generating…" else "✦  Generate App", fontSize = 9.sp) }
                OutlinedButton(onClick = openReleases, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(4.dp)) {
                    Text("⇩  Get Built APK", fontSize = 9.sp)
                }
            }

            if (showPreview) {
                Column(Modifier.width(150.dp).fillMaxHeight().background(Panel, RoundedCornerShape(12.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("● LIVE PREVIEW", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { showPreview = false }, contentPadding = PaddingValues(0.dp)) { Text("×", color = Muted) }
                    }
                    Spacer(Modifier.height(5.dp))
                    Column(Modifier.weight(1f).fillMaxWidth().border(2.dp, Color(0xFF40515F), RoundedCornerShape(18.dp)).background(Color(0xFF050B12), RoundedCornerShape(18.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("🤖", fontSize = 30.sp)
                        Text("Your App", color = Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Build • Code • Create", color = White, fontSize = 8.sp)
                        Spacer(Modifier.height(16.dp))
                        PreviewPill("AI Assistant")
                        PreviewPill("Build APK")
                        PreviewPill("My Projects")
                        Spacer(Modifier.height(10.dp))
                        Text("Live UI preview\nupdates as you build", color = Muted, fontSize = 8.sp)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().height(24.dp).background(Panel, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("◆ PRIVATE WORKSPACE", color = Green, fontSize = 8.sp)
            Spacer(Modifier.weight(1f))
            Text("JAVA / KOTLIN • ANDROID • OFFLINE MODEL", color = Muted, fontSize = 8.sp)
            Spacer(Modifier.weight(1f))
            Text("DroidForge AI", color = Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(48.dp), color = Panel2, shape = RoundedCornerShape(9.dp), border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.55f)) ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalArrangement = Arrangement.Center) {
            Text(title, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun PreviewPill(label: String) {
    Surface(color = Color(0xFF102A35), shape = RoundedCornerShape(30.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = White, fontSize = 9.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp))
    }
}

private suspend fun <T> withContextCompat(block: () -> T): T =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }
