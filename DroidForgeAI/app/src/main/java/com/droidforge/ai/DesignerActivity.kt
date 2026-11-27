package com.droidforge.ai

import android.os.Bundle
import android.util.Xml
import android.view.LayoutInflater
import android.widget.FrameLayout
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser

private val DfBg = Color(0xFF0B1120)
private val DfPanel = Color(0xFF111D30)
private val DfPanel2 = Color(0xFF17263D)
private val DfCyan = Color(0xFF19C7FF)
private val DfGreen = Color(0xFF00E676)
private val DfText = Color(0xFFEAF2FF)
private val DfMuted = Color(0xFF91A5BF)

class DesignerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = DfCyan, background = DfBg, surface = DfPanel)) {
                DroidForgePortraitScreen(
                    modelFile = File(filesDir, ModelDownloader.MODEL_NAME),
                    onBuild = {
                        // No fake success: the current app does not yet contain a verified on-device APK toolchain.
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
private fun DroidForgePortraitScreen(modelFile: File, onBuild: () -> Unit) {
    var document by remember { mutableStateOf(DesignDocument(screenName = "My Fitness")) }
    var xml by remember { mutableStateOf(VisualDesignEngine.toXml(document)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf("") }
    var modelReady by remember { mutableStateOf(false) }
    var modelBusy by remember { mutableStateOf(false) }
    var modelProgress by remember { mutableFloatStateOf(0f) }
    var modelStatus by remember { mutableStateOf(if (modelFile.exists()) "Model file found • load required" else "Qwen model not downloaded") }
    var chatStatus by remember { mutableStateOf("Ready. Load Qwen to generate UI offline.") }
    var buildStatus by remember { mutableStateOf("On-device compiler not installed") }
    var xmlError by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf("Chat") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    fun sync(next: DesignDocument) {
        document = next
        xml = VisualDesignEngine.toXml(next)
        xmlError = null
    }

    Column(Modifier.fillMaxSize().background(DfBg)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp)) {
            // Header inspired by the supplied phone screenshot.
            Row(
                Modifier.fillMaxWidth().background(DfPanel, RoundedCornerShape(15.dp)).border(1.dp, Color(0xFF263C5A), RoundedCornerShape(15.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("☰", color = DfText, fontSize = 23.sp)
                Spacer(Modifier.width(8.dp))
                Text("◈", color = DfGreen, fontSize = 28.sp)
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text("Offline AI Builder", color = DfText, fontSize = 16.sp)
                    Text(if (modelReady) "● Qwen 2.5 • Offline" else "○ Qwen 2.5 • Not loaded", color = if (modelReady) DfGreen else DfMuted, fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        buildStatus = "BLOCKED: local aapt2, D8, zipalign and apksig toolchain are not bundled."
                        chatStatus = "APK build cannot be marked ready until the on-device compiler is implemented and tested."
                        onBuild()
                    },
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4935D8))
                ) { Text("⚒ Build APK", fontSize = 11.sp, color = Color.White) }
            }

            Spacer(Modifier.height(10.dp))
            // Split workspace: actual Android View inflation from the current XML on the left.
            Row(Modifier.fillMaxWidth().height(355.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().background(DfPanel, RoundedCornerShape(14.dp)).border(1.dp, DfCyan.copy(alpha = .65f), RoundedCornerShape(14.dp)).padding(7.dp)) {
                    Text("LIVE XML PREVIEW", color = DfGreen, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                setBackgroundColor(android.graphics.Color.rgb(5, 12, 28))
                            }
                        },
                        modifier = Modifier.fillMaxSize().border(1.dp, DfCyan, RoundedCornerShape(10.dp)),
                        update = { container ->
                            try {
                                container.removeAllViews()
                                val parser = Xml.newPullParser()
                                parser.setInput(StringReader(xml))
                                var event = parser.eventType
                                while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) event = parser.next()
                                check(event == XmlPullParser.START_TAG) { "No XML root element" }
                                val view = LayoutInflater.from(container.context).inflate(parser, container, false)
                                container.addView(view)
                            } catch (e: Exception) {
                                container.removeAllViews()
                                val error = android.widget.TextView(container.context).apply {
                                    text = "Preview XML error\n${e.message}"
                                    setTextColor(android.graphics.Color.RED)
                                    textSize = 12f
                                }
                                container.addView(error)
                            }
                        }
                    )
                }

                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Column(Modifier.fillMaxWidth().background(DfPanel, RoundedCornerShape(14.dp)).padding(7.dp)) {
                        Text("COMPONENTS", color = DfGreen, fontSize = 10.sp)
                        val kinds = listOf(DesignKind.BUTTON, DesignKind.INPUT, DesignKind.CARD, DesignKind.TOGGLE)
                        kinds.chunked(2).forEach { rowKinds ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                rowKinds.forEach { kind ->
                                    OutlinedButton(
                                        onClick = { sync(VisualDesignEngine.add(document, kind)) },
                                        modifier = Modifier.weight(1f).height(39.dp),
                                        contentPadding = PaddingValues(2.dp)
                                    ) { Text(kind.displayName, fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().background(DfPanel, RoundedCornerShape(14.dp)).padding(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ANDROID XML", color = DfGreen, fontSize = 10.sp)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                try {
                                    sync(VisualDesignEngine.fromXml(xml, document.screenName))
                                    selectedId = null
                                } catch (e: Exception) { xmlError = e.message ?: "Invalid XML" }
                            }, contentPadding = PaddingValues(0.dp)) { Text("Apply", fontSize = 9.sp) }
                        }
                        BasicTextField(
                            value = xml,
                            onValueChange = { xml = it },
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            textStyle = TextStyle(color = Color(0xFFB8D8FF), fontFamily = FontFamily.Monospace, fontSize = 8.sp, lineHeight = 11.sp),
                            decorationBox = { inner -> inner() }
                        )
                        if (xmlError != null) Text("XML: $xmlError", color = Color(0xFFFF7777), fontSize = 8.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Selection/edit/reorder controls backed by the same document model.
            Column(Modifier.fillMaxWidth().background(DfPanel, RoundedCornerShape(13.dp)).padding(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DESIGN ELEMENTS", color = DfGreen, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${document.nodes.size} items", color = DfMuted, fontSize = 9.sp)
                }
                document.nodes.forEachIndexed { index, node ->
                    val active = selectedId == node.id
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp).background(if (active) Color(0xFF173958) else DfPanel2, RoundedCornerShape(8.dp)).border(1.dp, if (active) DfCyan else Color.Transparent, RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedId = node.id }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                            Text("${node.kind.displayName}: ${node.text}", color = DfText, fontSize = 9.sp, maxLines = 1)
                        }
                        TextButton(onClick = { sync(VisualDesignEngine.move(document, node.id, (index - 1).coerceAtLeast(0))) }, contentPadding = PaddingValues(0.dp)) { Text("↑", color = DfCyan) }
                        TextButton(onClick = { sync(VisualDesignEngine.move(document, node.id, (index + 1).coerceAtMost(document.nodes.lastIndex))) }, contentPadding = PaddingValues(0.dp)) { Text("↓", color = DfCyan) }
                        TextButton(onClick = { sync(VisualDesignEngine.remove(document, node.id)); selectedId = null }, contentPadding = PaddingValues(0.dp)) { Text("×", color = Color(0xFFFF7373)) }
                    }
                }
                val selected = document.nodes.firstOrNull { it.id == selectedId }
                if (selected != null) {
                    var editedText by remember(selected.id, selected.text) { mutableStateOf(selected.text) }
                    OutlinedTextField(value = editedText, onValueChange = { editedText = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Selected component text") })
                    Button(onClick = { sync(VisualDesignEngine.updateText(document, selected.id, editedText)) }) { Text("Apply component change") }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Honest toolchain status. Never show success ticks for tools not actually present.
            Column(Modifier.fillMaxWidth().background(DfPanel, RoundedCornerShape(13.dp)).padding(9.dp)) {
                Text("LOCAL BUILD TOOLCHAIN", color = DfGreen, fontSize = 10.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToolStatus("aapt2", false, Modifier.weight(1f))
                    ToolStatus("d8", false, Modifier.weight(1f))
                    ToolStatus("apksigner", false, Modifier.weight(1f))
                }
                Text(buildStatus, color = Color(0xFFFFC66D), fontSize = 9.sp)
            }

            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().background(DfPanel, RoundedCornerShape(13.dp)).padding(10.dp)) {
                Text("OFFLINE AI CHAT", color = DfGreen, fontSize = 10.sp)
                Text(chatStatus, color = DfMuted, fontSize = 9.sp)
                OutlinedTextField(
                    value = prompt, onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Build a fitness screen / একটি Quiz অ্যাপ তৈরি করো", fontSize = 10.sp) },
                    textStyle = TextStyle(color = DfText, fontSize = 11.sp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                modelBusy = true
                                modelProgress = 0f
                                modelStatus = "Preparing Qwen model…"
                                try {
                                    val file = if (modelFile.exists() && modelFile.length() > 0L) modelFile else {
                                        ModelDownloader(context).download { done, total ->
                                            modelProgress = if (total > 0) done.toFloat() / total else 0f
                                        }
                                    }
                                    modelStatus = "Loading model into memory…"
                                    val loaded = withContext(Dispatchers.IO) {
                                        LlamaNative.initModel(file.absolutePath, object : ModelLoadProgress {
                                            override fun onProgress(progress: Float) { modelProgress = progress.coerceIn(0f, 1f) }
                                        })
                                    }
                                    modelReady = loaded
                                    modelStatus = if (loaded) "Qwen 2.5 • Offline • Ready" else "Native model load failed"
                                } catch (e: Exception) {
                                    modelStatus = "Model error: ${e.message}"
                                } finally { modelBusy = false }
                            }
                        },
                        enabled = !modelBusy,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (modelBusy) "Loading…" else "Load Qwen", fontSize = 10.sp) }
                    Button(
                        onClick = {
                            if (!modelReady) {
                                chatStatus = "Load Qwen first. AI generation is disabled until the local model is loaded."
                            } else if (prompt.isBlank()) {
                                chatStatus = "Write a prompt first."
                            } else {
                                scope.launch {
                                    chatStatus = "Generating Android XML locally…"
                                    try {
                                        val generated = withContext(Dispatchers.IO) {
                                            val task = "Return ONLY valid Android XML layout. Root must be LinearLayout with android namespace. Allowed views: TextView, Button, EditText, Switch, nested LinearLayout. Use only standard android: attributes. User request: ${prompt}"
                                            LlamaNative.generateCode(task, 900)
                                        }.trim().removePrefix("```xml").removeSuffix("```").trim()
                                        val parsed = withContext(Dispatchers.Default) { VisualDesignEngine.fromXml(generated, document.screenName) }
                                        check(parsed.nodes.isNotEmpty()) { "AI did not return supported Android XML. Please retry with a simpler prompt." }
                                        document = parsed
                                        xml = VisualDesignEngine.toXml(parsed)
                                        selectedId = null
                                        chatStatus = "Generated UI from local Qwen output and parsed supported XML components."
                                    } catch (e: Exception) {
                                        chatStatus = "AI generation failed: ${e.message}"
                                    }
                                }
                            }
                        },
                        enabled = !modelBusy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Generate UI", fontSize = 10.sp) }
                }
                if (modelBusy) {
                    LinearProgressIndicator(progress = { modelProgress }, modifier = Modifier.fillMaxWidth(), color = DfGreen)
                    Text("${(modelProgress * 100).toInt()}% • ${modelStatus}", color = DfGreen, fontSize = 8.sp)
                } else Text(modelStatus, color = if (modelReady) DfGreen else DfMuted, fontSize = 8.sp)
            }
        }

        // Bottom navigation tabs switch the focused section rather than acting as decoration.
        Row(Modifier.fillMaxWidth().background(Color(0xFF050B14)).padding(horizontal = 6.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Chat", "Design", "Code", "Build").forEach { tab ->
                TextButton(onClick = {
                    selectedTab = tab
                    chatStatus = when (tab) {
                        "Build" -> "Build tab: local APK compiler is not yet available; no APK success is claimed."
                        "Code" -> "Code tab: edit the Android XML in the workspace above."
                        "Design" -> "Design tab: add, select, reorder and edit canvas components."
                        else -> "Chat tab: ask the loaded offline Qwen model to generate XML UI."
                    }
                }) {
                    Text((if (selectedTab == tab) "● " else "") + tab, color = if (selectedTab == tab) DfCyan else DfMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ToolStatus(label: String, available: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = if (available) Color(0xFF063B25) else Color(0xFF3B2B19), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (available) DfGreen else Color(0xFFFFC66D))) {
        Text((if (available) "✓ " else "○ ") + label, color = if (available) DfGreen else Color(0xFFFFC66D), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp))
    }
}
