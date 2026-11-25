package com.droidforge.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

private val Bg = Color(0xFF0D1117)
private val Panel = Color(0xFF161B22)
private val Green = Color(0xFF00E676)
private val TextColor = Color(0xFFE6EDF3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Bg, surface = Panel)) {
            DroidForgeScreen(File(filesDir, "generated-project"))
        } }
    }
}

@Composable
private fun DroidForgeScreen(projectDir: File) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("// Generated Kotlin source will appear here") }
    var log by remember { mutableStateOf("Ready. Download the GGUF model to enable offline generation.") }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloading by remember { mutableStateOf(false) }
    var modelReady by remember { mutableStateOf(false) }
    var generating by remember { mutableStateOf(false) }
    val downloader = remember { ModelDownloader(context) }

    Row(Modifier.fillMaxSize().background(Bg).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(2.2f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("◆ DroidForge AI IDE", color = Green, fontSize = 20.sp)
                Text(if (modelReady) "QWEN READY" else "MODEL NOT LOADED", color = if (modelReady) Green else Color.Yellow)
            }
            Spacer(Modifier.height(8.dp))
            Text("Kotlin Editor", color = TextColor)
            Box(Modifier.weight(1f).fillMaxWidth().background(Panel).padding(8.dp)) {
                Text(code, color = TextColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()))
            }
            Spacer(Modifier.height(8.dp))
            Text("Build Console", color = Green)
            Box(Modifier.height(92.dp).fillMaxWidth().background(Panel).padding(8.dp)) {
                Text(log, color = TextColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().background(Panel).padding(12.dp)) {
            Text("AI ASSISTANT", color = Green, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            Text("Qwen2.5-Coder-3B-Instruct Q4_K_M", color = TextColor, fontSize = 12.sp)
            Button(onClick = {
                scope.launch {
                    downloading = true
                    log = "Downloading model..."
                    try {
                        val f = downloader.download { done, total ->
                            progress = if (total > 0) done.toFloat() / total else 0f
                        }
                        modelReady = LlamaNative.initModel(f.absolutePath)
                        log = if (modelReady) "Model loaded. Offline inference ready." else "Model downloaded, but native initialization failed."
                    } catch (e: Exception) { log = "Download error: ${e.message}" }
                    downloading = false
                }
            }, enabled = !downloading, modifier = Modifier.fillMaxWidth()) {
                Text(if (downloading) "Downloading..." else "Download / Load Model")
            }
            if (downloading) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text("Prompt (বাংলা / English)", color = TextColor)
            OutlinedTextField(prompt, { prompt = it }, modifier = Modifier.weight(1f).fillMaxWidth(),
                placeholder = { Text("Describe the Android app you want...") })
            Button(onClick = {
                if (!modelReady) { log = "Download and load the model first."; return@Button }
                scope.launch {
                    generating = true
                    log = "Generating Kotlin source on device..."
                    try {
                        code = withContextCompat { QwenCoderEngine().generate(prompt) }
                        ProjectScaffolder(projectDir).writeComposeProject(code)
                        log = "Source generated and project scaffolded at:\n${projectDir.absolutePath}\nAPK compilation is not yet integrated."
                    } catch (e: Exception) { log = "Generation error: ${e.message}" }
                    generating = false
                }
            }, enabled = !generating && prompt.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(if (generating) "Generating..." else "Generate & Scaffold")
            }
            OutlinedButton(onClick = { log = "APK build is not implemented in this source drop. Build the generated project with Android Studio or Gradle on a host." },
                modifier = Modifier.fillMaxWidth()) { Text("Build APK (host toolchain required)") }
        }
    }
}

private suspend fun <T> withContextCompat(block: () -> T): T =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }
