package com.droidforge.ai

object LlamaNative {
    init { System.loadLibrary("droidforge_llama") }

    external fun initModel(modelPath: String): Boolean
    external fun generateCode(prompt: String, maxTokens: Int): String
    external fun release()
}
