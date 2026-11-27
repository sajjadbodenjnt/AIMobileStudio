package com.droidforge.ai

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.io.StringWriter
import java.util.UUID

/**
 * Source-of-truth model for the visual designer.
 * Every canvas operation changes this model; XML is serialized from the same model.
 */
enum class DesignKind(val xmlTag: String, val displayName: String) {
    TEXT("TextView", "Text"),
    BUTTON("Button", "Button"),
    INPUT("EditText", "Input"),
    CARD("LinearLayout", "Card"),
    TOGGLE("Switch", "Toggle")
}

data class DesignNode(
    val id: String = UUID.randomUUID().toString(),
    val kind: DesignKind,
    val text: String,
    val width: String = "match_parent",
    val height: String = "wrap_content",
    val marginDp: Int = 8,
    val paddingDp: Int = 12,
    val background: String = "#17243A"
)

data class DesignDocument(
    val packageName: String = "com.example.generated",
    val screenName: String = "My App",
    val nodes: List<DesignNode> = listOf(
        DesignNode(kind = DesignKind.TEXT, text = "Welcome"),
        DesignNode(kind = DesignKind.CARD, text = "Your first card"),
        DesignNode(kind = DesignKind.BUTTON, text = "Get Started")
    )
)

/** XML import/export for the supported Android View subset. Unsupported tags are ignored safely. */
object VisualDesignEngine {
    private const val ANDROID = "http://schemas.android.com/apk/res/android"

    fun add(document: DesignDocument, kind: DesignKind): DesignDocument {
        val defaultText = when (kind) {
            DesignKind.TEXT -> "Text"
            DesignKind.BUTTON -> "Button"
            DesignKind.INPUT -> "Enter text"
            DesignKind.CARD -> "Card content"
            DesignKind.TOGGLE -> "Toggle"
        }
        return document.copy(nodes = document.nodes + DesignNode(kind = kind, text = defaultText))
    }

    fun updateText(document: DesignDocument, id: String, text: String): DesignDocument =
        document.copy(nodes = document.nodes.map { if (it.id == id) it.copy(text = text) else it })

    fun remove(document: DesignDocument, id: String): DesignDocument =
        document.copy(nodes = document.nodes.filterNot { it.id == id })

    fun move(document: DesignDocument, id: String, targetIndex: Int): DesignDocument {
        val current = document.nodes.indexOfFirst { it.id == id }
        if (current < 0 || document.nodes.isEmpty()) return document
        val mutable = document.nodes.toMutableList()
        val item = mutable.removeAt(current)
        mutable.add(targetIndex.coerceIn(0, mutable.size), item)
        return document.copy(nodes = mutable)
    }

    fun toXml(document: DesignDocument): String {
        val out = StringWriter()
        val serializer = Xml.newSerializer()
        serializer.setOutput(out)
        serializer.startDocument("utf-8", true)
        serializer.setPrefix("android", ANDROID)
        serializer.startTag(null, "LinearLayout")
        serializer.attribute(ANDROID, "layout_width", "match_parent")
        serializer.attribute(ANDROID, "layout_height", "match_parent")
        serializer.attribute(ANDROID, "orientation", "vertical")
        serializer.attribute(ANDROID, "padding", "16dp")
        serializer.attribute(ANDROID, "background", "#0B1120")
        serializer.startTag(null, "TextView")
        serializer.attribute(ANDROID, "layout_width", "match_parent")
        serializer.attribute(ANDROID, "layout_height", "wrap_content")
        serializer.attribute(ANDROID, "text", document.screenName)
        serializer.attribute(ANDROID, "textColor", "#FFFFFF")
        serializer.attribute(ANDROID, "textSize", "22sp")
        serializer.attribute(ANDROID, "padding", "8dp")
        serializer.endTag(null, "TextView")

        document.nodes.forEach { node ->
            when (node.kind) {
                DesignKind.CARD -> {
                    serializer.startTag(null, "LinearLayout")
                    serializer.attribute(ANDROID, "layout_width", node.width)
                    serializer.attribute(ANDROID, "layout_height", node.height)
                    serializer.attribute(ANDROID, "orientation", "vertical")
                    serializer.attribute(ANDROID, "layout_margin", "${node.marginDp}dp")
                    serializer.attribute(ANDROID, "padding", "${node.paddingDp}dp")
                    serializer.attribute(ANDROID, "background", node.background)
                    serializer.startTag(null, "TextView")
                    serializer.attribute(ANDROID, "layout_width", "match_parent")
                    serializer.attribute(ANDROID, "layout_height", "wrap_content")
                    serializer.attribute(ANDROID, "text", node.text)
                    serializer.attribute(ANDROID, "textColor", "#FFFFFF")
                    serializer.endTag(null, "TextView")
                    serializer.endTag(null, "LinearLayout")
                }
                else -> {
                    serializer.startTag(null, node.kind.xmlTag)
                    serializer.attribute(ANDROID, "layout_width", node.width)
                    serializer.attribute(ANDROID, "layout_height", node.height)
                    serializer.attribute(ANDROID, "layout_margin", "${node.marginDp}dp")
                    serializer.attribute(ANDROID, "padding", "${node.paddingDp}dp")
                    serializer.attribute(ANDROID, "text", node.text)
                    if (node.kind == DesignKind.TEXT) {
                        serializer.attribute(ANDROID, "textColor", "#FFFFFF")
                        serializer.attribute(ANDROID, "textSize", "16sp")
                    }
                    if (node.kind == DesignKind.BUTTON) {
                        serializer.attribute(ANDROID, "allCaps", "false")
                    }
                    serializer.endTag(null, node.kind.xmlTag)
                }
            }
        }
        serializer.endTag(null, "LinearLayout")
        serializer.endDocument()
        return out.toString()
    }

    fun fromXml(xml: String, screenName: String = "Imported layout"): DesignDocument {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        val nodes = mutableListOf<DesignNode>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name
                val kind = when (tag) {
                    "TextView" -> DesignKind.TEXT
                    "Button" -> DesignKind.BUTTON
                    "EditText" -> DesignKind.INPUT
                    "Switch", "ToggleButton" -> DesignKind.TOGGLE
                    else -> null
                }
                if (kind != null) {
                    val text = parser.getAttributeValue(ANDROID, "text")
                        ?: parser.getAttributeValue(null, "text")
                        ?: when (kind) {
                            DesignKind.TEXT -> "Text"
                            DesignKind.BUTTON -> "Button"
                            DesignKind.INPUT -> "Enter text"
                            DesignKind.TOGGLE -> "Toggle"
                            else -> ""
                        }
                    nodes += DesignNode(
                        kind = kind,
                        text = text,
                        width = parser.getAttributeValue(ANDROID, "layout_width") ?: "match_parent",
                        height = parser.getAttributeValue(ANDROID, "layout_height") ?: "wrap_content"
                    )
                }
                if (tag == "LinearLayout") {
                    val childText = parser.getAttributeValue(ANDROID, "text")
                    if (!childText.isNullOrBlank()) nodes += DesignNode(kind = DesignKind.CARD, text = childText)
                }
            }
            event = parser.next()
        }
        return DesignDocument(screenName = screenName, nodes = nodes)
    }
}
