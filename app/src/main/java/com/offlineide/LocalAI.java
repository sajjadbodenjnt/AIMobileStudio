package com.offlineide;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public final class LocalAI {
    private LocalAI() {}
    public static String generate(String model, String prompt) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL("http://127.0.0.1:11434/api/generate").openConnection();
            c.setRequestMethod("POST"); c.setConnectTimeout(5000); c.setReadTimeout(120000);
            c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json");
            JSONObject data = new JSONObject().put("model", model).put("prompt", prompt)
                .put("stream", false).put("options", new JSONObject().put("num_ctx", 4096).put("num_predict", 512));
            try (OutputStream out = c.getOutputStream()) { out.write(data.toString().getBytes(StandardCharsets.UTF_8)); }
            int code = c.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
            JSONObject result = new JSONObject(bytes.toString(StandardCharsets.UTF_8));
            if (code < 200 || code >= 300) return "ERROR: " + result.optString("error", "HTTP " + code);
            return result.optString("response", "(empty response)");
        } catch (Exception e) {
            return "ERROR: Local Ollama is unreachable. Start Ollama and verify the model is installed. Details: " + e.getMessage();
        } finally { if (c != null) c.disconnect(); }
    }
}
