package com.droidforge.ai

interface ModelLoadProgress {
    fun onProgress(progress: Float)
}

object LlamaNative {
    init { System.loadLibrary("droidforge_llama") }

    external fun initModel(modelPath: String, progress: ModelLoadProgress): Boolean
    external fun generateCode(prompt: String, maxTokens: Int): String
    external fun release()
}
