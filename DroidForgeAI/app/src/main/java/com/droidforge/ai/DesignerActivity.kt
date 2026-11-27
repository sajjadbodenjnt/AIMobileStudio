package com.droidforge.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DesignerBg = Color(0xFF0B1120)
private val DesignerPanel = Color(0xFF131F32)
private val DesignerAccent = Color(0xFF19C7FF)
private val DesignerGreen = Color(0xFF00E676)

/** A real XML-backed designer for the supported Android View component subset. */
class DesignerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = DesignerAccent, background = DesignerBg, surface = DesignerPanel)) {
                VisualDesignerScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun VisualDesignerScreen(onBack: () -> Unit) {
    var document by remember { mutableStateOf(DesignDocument()) }
    var xml by remember { mutableStateOf(VisualDesignEngine.toXml(document)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    fun syncXml(next: DesignDocument) {
        document = next
        xml = VisualDesignEngine.toXml(next)
        importError = null
    }

    Column(Modifier.fillMaxSize().background(DesignerBg).padding(10.dp)) {
        Row(Modifier.fillMaxWidth().background(DesignerPanel, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back", color = DesignerAccent) }
            Column(Modifier.weight(1f)) {
                Text("Visual App Designer", color = Color.White, fontSize = 18.sp)
                Text("XML is the source of truth • local editing", color = DesignerGreen, fontSize = 10.sp)
            }
            Text("${document.nodes.size} components", color = Color.LightGray, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().background(DesignerPanel, RoundedCornerShape(12.dp)).padding(10.dp)) {
                Text("LIVE CANVAS", color = DesignerGreen, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.fillMaxSize().background(DesignerBg, RoundedCornerShape(16.dp)).border(1.dp, DesignerAccent, RoundedCornerShape(16.dp)).padding(12.dp)) {
                    Text(document.screenName, color = Color.White, fontSize = 21.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        itemsIndexed(document.nodes, key = { _, node -> node.id }) { index, node ->
                            val active = selectedId == node.id
                            Surface(
                                onClick = { selectedId = node.id },
                                color = if (active) Color(0xFF153D5C) else Color(0xFF18283E),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, if (active) DesignerAccent else Color(0xFF31435C))
                            ) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(node.text, color = Color.White, fontSize = 14.sp)
                                        Text(node.kind.displayName, color = DesignerAccent, fontSize = 9.sp)
                                    }
                                    if (node.kind == DesignKind.BUTTON) {
                                        Text("●", color = DesignerGreen)
                                    }
                                    TextButton(onClick = {
                                        syncXml(VisualDesignEngine.move(document, node.id, (index - 1).coerceAtLeast(0)))
                                    }) { Text("↑", color = Color.White) }
                                    TextButton(onClick = {
                                        syncXml(VisualDesignEngine.move(document, node.id, (index + 1).coerceAtMost(document.nodes.lastIndex)))
                                    }) { Text("↓", color = Color.White) }
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth().background(DesignerPanel, RoundedCornerShape(12.dp)).padding(10.dp)) {
                    Text("ADD COMPONENT", color = DesignerGreen, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        DesignKind.values().forEach { kind ->
                            OutlinedButton(onClick = { syncXml(VisualDesignEngine.add(document, kind)) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)) {
                                Text(kind.displayName, fontSize = 10.sp)
                            }
                        }
                    }
                    val selected = document.nodes.firstOrNull { it.id == selectedId }
                    if (selected != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("EDIT SELECTED • ${selected.kind.displayName}", color = DesignerAccent, fontSize = 10.sp)
                        var textValue by remember(selected.id, selected.text) { mutableStateOf(selected.text) }
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Component text") }
                        )
                        Row {
                            Button(onClick = { syncXml(VisualDesignEngine.updateText(document, selected.id, textValue)) }) { Text("Apply") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                syncXml(VisualDesignEngine.remove(document, selected.id))
                                selectedId = null
                            }) { Text("Delete", color = Color(0xFFFF6B6B)) }
                        }
                    } else {
                        Text("Tap a canvas component to edit it.", color = Color.LightGray, fontSize = 10.sp)
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().background(DesignerPanel, RoundedCornerShape(12.dp)).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ANDROID XML", color = DesignerGreen, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { xml = VisualDesignEngine.toXml(document) }) { Text("Regenerate", fontSize = 10.sp) }
                        TextButton(onClick = {
                            try {
                                syncXml(VisualDesignEngine.fromXml(xml, document.screenName))
                                selectedId = null
                            } catch (e: Exception) {
                                importError = e.message ?: "Invalid XML"
                            }
                        }) { Text("Apply XML", fontSize = 10.sp) }
                    }
                    OutlinedTextField(
                        value = xml,
                        onValueChange = { xml = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFB8D8FF), fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignerAccent, unfocusedBorderColor = Color(0xFF34465E))
                    )
                    if (importError != null) Text("XML error: $importError", color = Color(0xFFFF7777), fontSize = 10.sp)
                }
            }
        }
    }
}
