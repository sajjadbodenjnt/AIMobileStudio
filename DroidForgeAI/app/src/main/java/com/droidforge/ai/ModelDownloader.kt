package com.droidforge.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloader(private val context: Context) {
    companion object {
        const val MODEL_URL =
            "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/main/qwen2.5-coder-3b-instruct-q4_k_m.gguf"
        const val MODEL_NAME = "qwen2.5-coder-3b-instruct-q4_k_m.gguf"
    }

    suspend fun download(onProgress: (Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        val target = File(context.filesDir, MODEL_NAME)
        val temp = File(context.filesDir, "$MODEL_NAME.part")
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("Model download failed: HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        onProgress(downloaded, total)
                    }
                }
            }
            check(temp.length() > 0L) { "Downloaded model is empty" }
            check(temp.renameTo(target)) { "Could not finalize model file" }
            target
        } finally {
            connection.disconnect()
            if (temp.exists()) temp.delete()
        }
    }
}
