package com.droidforge.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * Streams the model to disk and keeps a partial file for retry/resume.
 * Progress callbacks are dispatched to Main because the Compose UI updates
 * snapshot state from this callback.
 */
class ModelDownloader(private val context: Context) {
    companion object {
        const val MODEL_URL =
            "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/main/qwen2.5-coder-3b-instruct-q4_k_m.gguf"
        const val MODEL_NAME = "qwen2.5-coder-3b-instruct-q4_k_m.gguf"
        private const val BUFFER_SIZE = 64 * 1024
        private const val PROGRESS_INTERVAL_MS = 250L
        private const val MIN_FREE_SPACE_BYTES = 256L * 1024L * 1024L
    }

    suspend fun download(onProgress: (Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        val target = File(context.filesDir, MODEL_NAME)
        val temp = File(context.filesDir, "$MODEL_NAME.part")

        if (target.exists() && target.length() > 0L) {
            withContext(Dispatchers.Main.immediate) { onProgress(target.length(), target.length()) }
            return@withContext target
        }

        var activeConnection: HttpURLConnection? = null
        try {
            val existingBytes = if (temp.exists()) temp.length() else 0L
            val conn = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 90_000
                instanceFollowRedirects = true
                setRequestProperty("Accept-Encoding", "identity")
                if (existingBytes > 0L) setRequestProperty("Range", "bytes=$existingBytes-")
            }
            activeConnection = conn

            val response = conn.responseCode
            if (response == 416 && existingBytes > 0L) {
                temp.delete()
                throw IllegalStateException("Saved partial download was rejected. Tap download again to restart.")
            }
            if (response !in 200..299) {
                throw IllegalStateException("Model download failed: HTTP $response")
            }

            val append = existingBytes > 0L && response == HttpURLConnection.HTTP_PARTIAL
            if (existingBytes > 0L && !append) temp.delete()

            val startingBytes = if (append) existingBytes else 0L
            val responseLength = conn.contentLengthLong
            val totalBytes = if (responseLength > 0L) startingBytes + responseLength else -1L
            val requiredSpace = if (append && responseLength > 0L) responseLength else totalBytes
            if (requiredSpace > 0L && context.filesDir.usableSpace < requiredSpace + MIN_FREE_SPACE_BYTES) {
                throw IllegalStateException(
                    "Not enough storage. Need about %.2f GB plus 256 MB free space."
                        .format(requiredSpace / 1_073_741_824.0)
                )
            }

            var downloaded = startingBytes
            var lastProgressAt = 0L
            RandomAccessFile(temp, "rw").use { output ->
                if (append) output.seek(existingBytes) else output.setLength(0L)
                conn.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        val now = System.currentTimeMillis()
                        if (now - lastProgressAt >= PROGRESS_INTERVAL_MS) {
                            lastProgressAt = now
                            withContext(Dispatchers.Main.immediate) {
                                onProgress(downloaded, totalBytes)
                            }
                        }
                    }
                    output.fd.sync()
                }
            }

            check(temp.exists() && temp.length() > 0L) { "Downloaded model is empty." }
            if (totalBytes > 0L) {
                check(temp.length() == totalBytes) {
                    "Download incomplete: received ${temp.length()} of $totalBytes bytes. Partial file kept; retry to resume."
                }
            }

            if (target.exists() && !target.delete()) {
                throw IllegalStateException("Cannot replace existing model file.")
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                check(target.length() == temp.length()) { "Could not finalize model file." }
                temp.delete()
            }

            withContext(Dispatchers.Main.immediate) {
                onProgress(target.length(), if (totalBytes > 0L) totalBytes else target.length())
            }
            target
        } finally {
            activeConnection?.disconnect()
            // Preserve .part after network/storage errors so the next attempt can resume.
        }
    }
}
