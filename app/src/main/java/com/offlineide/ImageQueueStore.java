package com.offlineide;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Durable, de-duplicated reference-image queue. No fixed app-level item count. */
public final class ImageQueueStore {
  private static final String PREFS = "reference_image_queue";
  private static final String KEY = "uris";
  private final Context context;
  private final SharedPreferences prefs;

  public ImageQueueStore(Context context) {
    this.context = context.getApplicationContext();
    this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  public synchronized List<Uri> all() {
    ArrayList<Uri> result = new ArrayList<>();
    try {
      JSONArray saved = new JSONArray(prefs.getString(KEY, "[]"));
      for (int i = 0; i < saved.length(); i++) {
        String value = saved.optString(i, "");
        if (!value.isEmpty()) result.add(Uri.parse(value));
      }
    } catch (Exception ignored) { }
    return result;
  }

  public synchronized int add(List<Uri> incoming) {
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    for (Uri uri : all()) unique.add(uri.toString());
    int before = unique.size();
    if (incoming != null) for (Uri uri : incoming) if (uri != null) unique.add(uri.toString());
    JSONArray saved = new JSONArray();
    for (String value : unique) saved.put(value);
    prefs.edit().putString(KEY, saved.toString()).apply();
    return unique.size() - before;
  }

  public synchronized void remove(Uri uri) {
    if (uri == null) return;
    JSONArray saved = new JSONArray();
    for (Uri item : all()) if (!item.equals(uri)) saved.put(item.toString());
    prefs.edit().putString(KEY, saved.toString()).apply();
  }

  public synchronized void clear() {
    prefs.edit().remove(KEY).apply();
  }

  /** Returns one bounded work batch; the full queue remains intact until explicitly cleared. */
  public synchronized List<Uri> batch(int offset, int batchSize) {
    List<Uri> all = all();
    ArrayList<Uri> result = new ArrayList<>();
    if (offset < 0 || batchSize < 1 || offset >= all.size()) return result;
    int end = Math.min(all.size(), offset + batchSize);
    for (int i = offset; i < end; i++) result.add(all.get(i));
    return result;
  }
}
