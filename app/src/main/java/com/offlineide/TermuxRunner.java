package com.offlineide;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

/**
 * Explicit, permission-gated bridge to the user's Termux installation.
 * Commands run only after the user taps the build action in the foreground app.
 */
public final class TermuxRunner {
  private static final String TERMUX_PACKAGE = "com.termux";
  private static final String RUN_PERMISSION = "com.termux.permission.RUN_COMMAND";
  private static final int REQUEST_RUN_PERMISSION = 7314;

  private TermuxRunner() {}

  public static boolean isInstalled(Activity activity) {
    try {
      activity.getPackageManager().getPackageInfo(TERMUX_PACKAGE, 0);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  public static void runBuild(Activity activity, String projectName, String buildType) {
    String safe = projectName == null ? "" : projectName.replaceAll("[^A-Za-z0-9._-]", "_");
    if (safe.isEmpty() || safe.equals(".") || safe.equals("..")) {
      Toast.makeText(activity, "Invalid project name.", Toast.LENGTH_LONG).show();
      return;
    }
    if (!isInstalled(activity)) {
      new AlertDialog.Builder(activity)
        .setTitle("Termux is not installed")
        .setMessage("Install the official Termux app first. Then return here and run the build again.")
        .setPositiveButton("OK", null).show();
      return;
    }
    if (activity.checkSelfPermission(RUN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
      new AlertDialog.Builder(activity)
        .setTitle("Allow Termux build access")
        .setMessage("AI Mobile Studio needs the Termux RUN_COMMAND permission. This permission lets this app run the selected build script in Termux. Grant it only if you trust this app.")
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Grant permission", (d, w) ->
          activity.requestPermissions(new String[]{RUN_PERMISSION}, REQUEST_RUN_PERMISSION))
        .show();
      return;
    }

    if (buildType == null || !buildType.matches("debug|release|aab|aab-release")) {
      Toast.makeText(activity, "Invalid build type.", Toast.LENGTH_LONG).show(); return;
    }\n    String home = "/data/data/com.termux/files/home";
    String zip = home + "/storage/downloads/" + safe + "-source.zip";
    String buildRoot = home + "/storage/downloads/aimobilestudio-builds";
    String project = buildRoot + "/" + safe;
    String command = "set -e; " +
      "test -f " + quote(zip) + " || { echo 'Source ZIP not found. Save it in Downloads as " + safe + "-source.zip'; exit 20; }; " +
      "mkdir -p " + quote(buildRoot) + "; " +
      "unzip -o " + quote(zip) + " -d " + quote(buildRoot) + "; " +
      "cd " + quote(project) + "; " +
      "AIMS_BUILD_TYPE=" + quote(buildType) + " bash AIMobileStudio-Termux-Build.sh";

    Intent intent = new Intent("com.termux.RUN_COMMAND");
    intent.setComponent(new ComponentName(TERMUX_PACKAGE, "com.termux.app.RunCommandService"));
    intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
    intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-lc", command});
    intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", home);
    intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
    intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
    intent.putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", "AI Mobile Studio APK Build");
    try {
      activity.startService(intent);
      Toast.makeText(activity, "Build command sent to Termux. Check the Termux session/notification.", Toast.LENGTH_LONG).show();
    } catch (SecurityException | IllegalStateException e) {
      new AlertDialog.Builder(activity)
        .setTitle("Termux did not accept the build command")
        .setMessage(e.getClass().getSimpleName() + ": " + e.getMessage() +
          "\\n\\nIn Termux, enable allow-external-apps=true in ~/.termux/termux.properties, then run termux-reload-settings. Also verify the RUN_COMMAND permission in Android Settings.")
        .setPositiveButton("Open app settings", (d, w) -> {
          Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + activity.getPackageName()));
          activity.startActivity(settings);
        })
        .setNegativeButton("Close", null).show();
    }
  }

  private static String quote(String value) {
    return "'" + value.replace("'", "'\\''") + "'";
  }
}
