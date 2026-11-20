package com.offlineide;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/** Installs a completed APK through Android's package installer (never silently). */
public final class ApkInstaller {
  private ApkInstaller() {}

  public static void install(Activity activity, File apk) throws Exception {
    if (apk == null || !apk.isFile() || apk.length() <= 0)
      throw new IllegalArgumentException("APK file is missing or empty.");
    if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
      Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:" + activity.getPackageName()));
      activity.startActivity(settings);
      Toast.makeText(activity, "Allow installation for AI Mobile Studio, then tap Install APK again.", Toast.LENGTH_LONG).show();
      return;
    }

    PackageInstaller installer = activity.getPackageManager().getPackageInstaller();
    PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
    params.setSize(apk.length());
    int sessionId = installer.createSession(params);
    try (PackageInstaller.Session session = installer.openSession(sessionId);
         FileInputStream input = new FileInputStream(apk);
         OutputStream output = session.openWrite("base.apk", 0, apk.length())) {
      byte[] buffer = new byte[64 * 1024];
      int count;
      while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
      session.fsync(output);
      Intent callback = new Intent(activity, InstallResultReceiver.class)
        .setAction(InstallResultReceiver.ACTION)
        .putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId);
      int flags = PendingIntent.FLAG_UPDATE_CURRENT;
      if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
      PendingIntent pending = PendingIntent.getBroadcast(activity, sessionId, callback, flags);
      session.commit(pending.getIntentSender());
    } catch (Exception error) {
      try { installer.abandonSession(sessionId); } catch (Exception ignored) {}
      throw error;
    }
  }
}
