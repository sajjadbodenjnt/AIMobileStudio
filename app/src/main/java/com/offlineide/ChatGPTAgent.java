package com.offlineide;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import android.net.Uri;
import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * ChatGPT plan-usage client for a locally running open-source app.
 * Uses the public Responses API and does not access ChatGPT conversations.
 */
public final class ChatGPTAgent {
  private static final String API = "https://api.openai.com/v1";
  private final ChatGPTAuth auth;
  private final Context context;

  public ChatGPTAgent(Context context) { this.context=context.getApplicationContext(); auth = new ChatGPTAuth(context); }

  private JSONObject credentials() throws Exception {
    JSONObject cred = auth.loadValid();
    if (cred == null) throw new SecurityException("Connect your ChatGPT account first.");
    if (!cred.optBoolean("plan_usage_enabled"))
      throw new SecurityException("ChatGPT plan usage permission is not enabled. Reconnect and approve it.");
    return cred;
  }

  public JSONArray models() throws Exception {
    JSONObject cred = credentials();
    HttpURLConnection c = (HttpURLConnection) new URL(API + "/models").openConnection();
    c.setRequestMethod("GET");
    c.setConnectTimeout(15000);
    c.setReadTimeout(30000);
    c.setRequestProperty("Authorization", "Bearer " + cred.getString("access_token"));
    int code = c.getResponseCode();
    InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
    String body = readAll(in);
    c.disconnect();
    if (code < 200 || code >= 300) throw apiError(code, body);
    JSONObject data = new JSONObject(body);
    JSONArray source = data.optJSONArray("models");
    JSONArray result = new JSONArray();
    if (source == null) throw new IOException("Unexpected model catalog: missing models array.");
    for (int i = 0; i < source.length(); i++) {
      JSONObject model = source.optJSONObject(i);
      if (model != null && "list".equals(model.optString("visibility"))) {
        String slug = model.optString("slug", "");
        if (!slug.isEmpty()) result.put(new JSONObject()
          .put("id", slug)
          .put("name", model.optString("display_name", slug)));
      }
    }
    return result;
  }

  public String generate(String instruction, String projectSummary) throws Exception { return generate(instruction, projectSummary, null); }

  public String generate(String instruction, String projectSummary, List<Uri> images) throws Exception {
    JSONObject cred = credentials();
    JSONArray available = models();
    if (available.length() == 0) throw new IOException("No models are available for this ChatGPT account.");
    String model = available.getJSONObject(0).getString("id");

    String prompt =
      "You are the senior Android engineer inside AIMobileStudio. Return ONLY one valid JSON object " +
      "with schema {\"summary\":string,\"files\":[{\"path\":string,\"content\":string}]}. " +
      "Every file content must be complete, not abbreviated. Never use markdown fences. Never include absolute paths, ../, " +
      "shell commands, secrets or API keys. For a new app, generate a complete Gradle Android project: settings.gradle, " +
      "root build.gradle, gradle.properties, app/build.gradle, app/src/main/AndroidManifest.xml, Java source under the " +
      "matching package path, and all required XML resources. Use Java 17, compileSdk 35, minSdk 26, targetSdk 35. " +
      "Prefer Android framework APIs and avoid AndroidX or external dependencies unless required. Ensure all referenced " +
      "classes, resources, package names and manifest entries exist and match. Implement requested screens and working " +
      "interactions, validation, loading/error/empty states and polished responsive UI. Do not output TODOs, pseudocode, " +
      "placeholder screens or instructions instead of code. For existing projects, preserve unrelated files and make only " +
      "necessary changes. File paths must be relative to the selected project root. User request: " + instruction +
      "\nExisting project files summary:\n" + projectSummary;

    JSONObject body = new JSONObject()
      .put("model", model)
      .put("store", false)
      .put("stream", true)
      .put("input", new JSONArray().put(new JSONObject()
        .put("role", "user")
        .put("content", buildContent(prompt, images))));

    HttpURLConnection c = (HttpURLConnection) new URL(API + "/responses").openConnection();
    c.setRequestMethod("POST");
    c.setConnectTimeout(20000);
    c.setReadTimeout(180000);
    c.setDoOutput(true);
    c.setRequestProperty("Authorization", "Bearer " + cred.getString("access_token"));
    c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    c.setRequestProperty("Accept", "text/event-stream");
    try (OutputStream out = c.getOutputStream()) {
      out.write(body.toString().getBytes(StandardCharsets.UTF_8));
    }

    int code = c.getResponseCode();
    if (code < 200 || code >= 300) {
      String error = readAll(c.getErrorStream());
      c.disconnect();
      throw apiError(code, error);
    }

    StringBuilder output = new StringBuilder();
    boolean completed = false;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.startsWith("data:")) continue;
        String data = line.substring(5).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) continue;
        JSONObject event;
        try { event = new JSONObject(data); }
        catch (JSONException malformed) { throw new IOException("Invalid Responses stream event.", malformed); }
        String type = event.optString("type", "");
        if ("response.output_text.delta".equals(type)) {
          output.append(event.optString("delta", ""));
          if (output.length() > 2_000_000) throw new IOException("AI response exceeded the 2 MB safety limit.");
        } else if ("response.failed".equals(type)) {
          JSONObject response = event.optJSONObject("response");
          JSONObject error = response == null ? null : response.optJSONObject("error");
          String message = error == null ? "The Responses API reported a failed generation." :
            error.optString("code", "unknown_error") + ": " + error.optString("message", "Generation failed.");
          throw new IOException(message);
        } else if ("response.incomplete".equals(type)) {
          throw new IOException("AI generation was incomplete: " + event.toString());
        } else if ("response.completed".equals(type)) {
          completed = true;
        }
      }
    } finally {
      c.disconnect();
    }
    if (!completed) throw new IOException("AI stream ended before response.completed; generated output was discarded.");
    String result = output.toString().trim();
    if (result.isEmpty()) throw new IOException("ChatGPT returned an empty response.");
    if (result.startsWith("```json")) result = result.substring(7).trim();
    else if (result.startsWith("```")) result = result.substring(3).trim();
    if (result.endsWith("```")) result = result.substring(0, result.length() - 3).trim();
    return result;
  }

  private JSONArray buildContent(String prompt, List<Uri> images) throws Exception {
    JSONArray content = new JSONArray();
    content.put(new JSONObject().put("type","input_text").put("text",prompt));
    if(images!=null) for(Uri uri:images) {
      // Decode and downscale one image at a time to avoid loading the entire queue into RAM.
      BitmapFactory.Options bounds = new BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      try(InputStream in=context.getContentResolver().openInputStream(uri)) {
        if(in==null)throw new IOException("Cannot open selected image: "+uri);
        BitmapFactory.decodeStream(in,null,bounds);
      }
      if(bounds.outWidth<=0||bounds.outHeight<=0)throw new IOException("Unsupported or corrupt image: "+uri);
      int sample=1;
      while(Math.max(bounds.outWidth/sample,bounds.outHeight/sample)>2048) sample*=2;
      BitmapFactory.Options options=new BitmapFactory.Options();
      options.inSampleSize=sample;
      Bitmap bitmap;
      try(InputStream in=context.getContentResolver().openInputStream(uri)) {
        if(in==null)throw new IOException("Cannot reopen selected image: "+uri);
        bitmap=BitmapFactory.decodeStream(in,null,options);
      }
      if(bitmap==null)throw new IOException("Could not decode selected image: "+uri);
      byte[] bytes;
      try(ByteArrayOutputStream out=new ByteArrayOutputStream()) {
        bitmap.compress(Bitmap.CompressFormat.JPEG,82,out);
        bytes=out.toByteArray();
      } finally { bitmap.recycle(); }
      String dataUrl="data:image/jpeg;base64,"+Base64.encodeToString(bytes,Base64.NO_WRAP);
      content.put(new JSONObject().put("type","input_image").put("image_url",dataUrl).put("detail","auto"));
    }
    return content;
  }

  private static String readAll(InputStream in) throws IOException {
    if (in == null) return "";
    try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int n;
      while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
      return out.toString("UTF-8");
    }
  }

  private static IOException apiError(int status, String body) {
    String detail = body;
    try {
      JSONObject json = new JSONObject(body);
      JSONObject error = json.optJSONObject("error");
      if (error != null) detail = error.optString("code", "") + ": " + error.optString("message", body);
      else if (json.has("detail")) detail = json.optString("detail");
    } catch (Exception ignored) { }
    return new IOException("OpenAI HTTP " + status + ": " + detail);
  }
}
