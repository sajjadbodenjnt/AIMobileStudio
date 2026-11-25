package com.droidforge.ai

class QwenCoderEngine {
    fun generate(userPrompt: String, maxTokens: Int = 1200): String {
        val system = """
            You are DroidForge AI, an expert Android developer.
            Return complete, compilable Kotlin Jetpack Compose Material 3 source.
            Include required imports and one MainActivity with a @Composable screen.
            Do not use markdown explanations. If the user writes Bangla, understand it
            and generate the app accordingly. Never claim a build succeeded.
        """.trimIndent()
        val chat = "<|im_start|>system\n$system<|im_end|>\n" +
            "<|im_start|>user\n$userPrompt<|im_end|>\n" +
            "<|im_start|>assistant\n"
        return extractCode(LlamaNative.generateCode(chat, maxTokens))
    }

    private fun extractCode(raw: String): String {
        val match = Regex("(?s)```(?:kotlin)?\\s*(.*?)```").find(raw)
        return (match?.groupValues?.get(1) ?: raw).trim()
    }
}
