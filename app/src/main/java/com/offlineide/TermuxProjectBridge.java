package com.offlineide;

import java.io.*;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class TermuxProjectBridge {
  private TermuxProjectBridge() {}
  private static final String BUILD_SCRIPT =
      "#!/data/data/com.termux/files/usr/bin/bash\n" +
      "set -Eeuo pipefail\n" +
      "ROOT=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\n" +
      "cd \"$ROOT\"\n" +
      "if [[ \"${PREFIX:-}\" != /data/data/com.termux/files/usr ]]; then echo 'Run inside Termux, not proot.' >&2; exit 2; fi\n" +
      "command -v java >/dev/null || { echo 'Install JDK 17: pkg install openjdk-17'; exit 3; }\n" +
      "java -version 2>&1 | grep -q '17\\.' || { echo 'JDK 17 is required: pkg install openjdk-17'; exit 3; }\n" +
      "SDK=\"${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}\"\n" +
      "for candidate in \"$HOME/lib/android-sdk-9123335\" \"$HOME/lib/android-sdk\" \"$HOME/Android/Sdk\"; do if [[ -z \"$SDK\" && -d \"$candidate/platforms/android-35\" ]]; then SDK=\"$candidate\"; fi; done\n" +
      "if [[ -z \"$SDK\" || ! -f \"$SDK/platforms/android-35/android.jar\" ]]; then echo 'Android SDK Platform 35 not found. Set ANDROID_HOME.'; exit 4; fi\n" +
      "export ANDROID_HOME=\"$SDK\" ANDROID_SDK_ROOT=\"$SDK\"\n" +
      "if [[ ! -x \"$HOME/.aimobilestudio/gradle-8.9/bin/gradle\" ]]; then\n" +
      "  command -v curl >/dev/null || { echo 'Install curl: pkg install curl'; exit 5; }\n" +
      "  command -v unzip >/dev/null || { echo 'Install unzip: pkg install unzip'; exit 5; }\n" +
      "  mkdir -p \"$HOME/.aimobilestudio\"\n" +
      "  curl -fL --retry 3 https://downloads.gradle.org/distributions/gradle-8.9-bin.zip -o \"$HOME/.aimobilestudio/gradle-8.9-bin.zip\"\n" +
      "  unzip -q -o \"$HOME/.aimobilestudio/gradle-8.9-bin.zip\" -d \"$HOME/.aimobilestudio\"\n" +
      "fi\n" +
      "GRADLE=\"$HOME/.aimobilestudio/gradle-8.9/bin/gradle\"\n" +
      "printf 'sdk.dir=%s\\n' \"$SDK\" > local.properties\n" +
      "echo 'First build downloads required Android Gradle Plugin and Maven dependencies.'\n" +
      "\"$GRADLE\" --no-daemon --stacktrace clean assembleDebug\n" +
      "APK=app/build/outputs/apk/debug/app-debug.apk\n" +
      "test -s \"$APK\" || { echo 'APK missing or empty.'; exit 6; }\n" +
      "mkdir -p \"$HOME/storage/downloads/AIMobileStudio-Builds\"\n" +
      "OUT=\"$HOME/storage/downloads/AIMobileStudio-Builds/$(basename \"$ROOT\")-debug.apk\"\n" +
      "cp \"$APK\" \"$OUT\"\n" +
      "echo \"BUILD SUCCESS: $OUT\"\n";

  public static void exportZip(File project, OutputStream destination) throws IOException {
    File root = project.getCanonicalFile();
    if (!root.isDirectory() || !new File(root, "settings.gradle").isFile() ||
        !new File(root, "app/build.gradle").isFile()) throw new IOException("Not a complete Android Gradle project.");
    try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(destination))) {
      zip.putNextEntry(new ZipEntry(root.getName() + "/")); zip.closeEntry();
      addTree(zip, root, 0);
      zip.putNextEntry(new ZipEntry(root.getName() + "/AIMobileStudio-Termux-Build.sh"));
      zip.write(BUILD_SCRIPT.getBytes("UTF-8")); zip.closeEntry(); zip.finish();
    }
  }
  private static void addTree(ZipOutputStream zip, File dir, int depth) throws IOException {
    if (depth > 30) throw new IOException("Project nesting is too deep.");
    File[] children = dir.listFiles(); if (children == null) return;
    for (File child : children) {
      String n = child.getName().toLowerCase(Locale.ROOT);
      if (n.equals("build") || n.equals(".gradle") || n.equals(".git") || n.equals(".idea") ||
          n.equals("local.properties") || n.equals(".env") || n.endsWith(".jks") ||
          n.endsWith(".keystore") || n.contains("secret") || n.contains("credential") ||
          n.equals("keystore.properties")) continue;
      if (child.isDirectory()) {
        zip.putNextEntry(new ZipEntry(child.getName() + "/")); zip.closeEntry();
        addTree(zip, child, depth + 1);
      } else if (child.isFile()) {
        zip.putNextEntry(new ZipEntry(child.getName()));
        try (InputStream in = new BufferedInputStream(new FileInputStream(child))) {
          byte[] b = new byte[16384]; int count; while ((count = in.read(b)) != -1) zip.write(b, 0, count);
        }
        zip.closeEntry();
      }
    }
  }
}
