package com.sajjad.aimobilestudio;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class OpenAIClient {
    private final String apiKey;
    public OpenAIClient(String apiKey) { this.apiKey = apiKey; }
    public String generate(String prompt) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.openai.com/v1/responses").openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(20000); c.setReadTimeout(120000);
        c.setDoOutput(true); c.setRequestProperty("Authorization", "Bearer " + apiKey);
        c.setRequestProperty("Content-Type", "application/json");
        JSONObject body = new JSONObject();
        body.put("model", "gpt-4.1-mini");
        body.put("store", false);
        body.put("input", prompt + "\n\nIf code is requested, return complete files with clear relative paths. Do not claim a build was run.");
        try (OutputStream out = c.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder raw = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) raw.append(line);
        }
        c.disconnect();
        JSONObject response = new JSONObject(raw.toString());
        if (code < 200 || code >= 300) throw new Exception(response.optJSONObject("error") != null ?
            response.getJSONObject("error").optString("message", "HTTP " + code) : "HTTP " + code);
        JSONArray output = response.optJSONArray("output");
        if (output != null) for (int i=0; i<output.length(); i++) {
            JSONObject item = output.optJSONObject(i);
            JSONArray content = item == null ? null : item.optJSONArray("content");
            if (content != null) for (int j=0; j<content.length(); j++) {
                JSONObject part = content.optJSONObject(j);
                if (part != null && "output_text".equals(part.optString("type"))) return part.optString("text");
            }
        }
        return "The API returned no output_text. Raw response: " + raw;
    }
}
