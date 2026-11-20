package com.offlineide;

import java.io.*;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** SAF-based, user-confirmed handoff to Termux; it never crosses Android app sandboxes. */
public final class TermuxProjectBridge {
  private TermuxProjectBridge() {}
  private static final String BUILD_SCRIPT =
      "#!/data/data/com.termux/files/usr/bin/bash" +
      "set -Eeuo pipefail" +
      "ROOT=\"$(cd \"$(dirname \"$0\")\" && pwd)\"" +
      "cd \"$ROOT\"" +
      "[[ \"${PREFIX:-}\" == /data/data/com.termux/files/usr ]] || { echo \"Run inside Termux, not proot.\" >&2; exit 2; }" +
      "command -v java >/dev/null 2>&1 || { echo \"Install JDK: pkg install openjdk-17\"; exit 3; }" +
      "java -version 2>&1 | grep -Eq 'version \"(17|21)(\\.|\\+)' || { echo \"JDK 17 or 21 required.\"; exit 3; }" +
      "SDK=\"${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}\"" +
      "for p in \"$HOME/lib/android-sdk-9123335\" \"$HOME/lib/android-sdk\" \"$HOME/Android/Sdk\"; do" +
      "  if [[ -z \"$SDK\" && -d \"$p/platforms\" ]]; then SDK=\"$p\"; fi" +
      "done" +
      "[[ -n \"$SDK\" && -d \"$SDK/platforms\" && -d \"$SDK/build-tools\" ]] || { echo \"Set ANDROID_HOME to an installed Android SDK with platforms and build-tools.\"; exit 4; }" +
      "export ANDROID_HOME=\"$SDK\" ANDROID_SDK_ROOT=\"$SDK\"" +
      "[[ -f settings.gradle || -f settings.gradle.kts ]] || { echo \"settings.gradle(.kts) missing.\"; exit 5; }" +
      "grep -RqsE 'com\\.android\\.(application|library)|com.android.tools.build:gradle' --include='*.gradle' --include='*.gradle.kts' . || { echo \"Android Gradle Plugin declaration not found.\"; exit 5; }" +
      "if [[ -f gradlew ]]; then chmod +x gradlew; GRADLE=(./gradlew)" +
      "elif command -v gradle >/dev/null 2>&1; then GRADLE=(gradle)" +
      "else echo \"Install Gradle: pkg install gradle\"; exit 6; fi" +
      "BUILD_TOOLS=\"$(find \"$SDK/build-tools\" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)\"" +
      "[[ -n \"$BUILD_TOOLS\" ]] || { echo \"No Android Build Tools found.\"; exit 7; }" +
      "echo \"Java: $(java -version 2>&1 | head -n 1)\"" +
      "echo \"SDK: $SDK\"" +
      "echo \"Build Tools: $(basename \"$BUILD_TOOLS\")\"" +
      "\"${GRADLE[@]}\" --version | head -n 12" +
      "TYPE=\"${AIMS_BUILD_TYPE:-debug}\"" +
      "case \"$TYPE\" in debug) TASK=assembleDebug;; release) TASK=assembleRelease;; *) echo \"AIMS_BUILD_TYPE must be debug or release.\"; exit 8;; esac" +
      "echo \"Building all Android modules: $TASK\"" +
      "\"${GRADLE[@]}\" --no-daemon --stacktrace --console=plain \"$TASK\"" +
      "mapfile -t APKS < <(find . -type f -name '*.apk' | grep '/build/outputs/apk/' | sort)" +
      "((\"${#APKS[@]}\" > 0)) || { echo \"No APK produced. This project may produce only an AAB.\"; exit 9; }" +
      "OUT=\"$HOME/storage/downloads/AIMobileStudio-Builds/$(basename \"$ROOT\")\"" +
      "mkdir -p \"$OUT\"" +
      "for apk in \"${APKS[@]}\"; do" +
      "  name=\"$(echo \"$apk\" | sed 's#^./##; s#/#-#g; s#[^A-Za-z0-9._-]#_#g')\"" +
      "  dest=\"$OUT/$name\"" +
      "  cp \"$apk\" \"$dest\"" +
      "  test -s \"$dest\" || { echo \"Empty APK: $dest\"; exit 10; }" +
      "  if [[ -x \"$BUILD_TOOLS/apksigner\" ]]; then \"$BUILD_TOOLS/apksigner\" verify --verbose \"$dest\"; else echo \"WARNING: apksigner unavailable; signature not independently verified.\"; fi" +
      "  echo \"APK_READY=$dest\"" +
      "done" +
      "[[ \"$TYPE\" != release ]] || echo \"WARNING: Configure your own release signing key before distribution.\"" +
      "echo \"BUILD SUCCESS: copied ${#APKS[@]} APK(s) to $OUT\"" +
      "";

  public static void exportZip(File project, OutputStream destination) throws IOException {
    File root = project.getCanonicalFile();
    if (!root.isDirectory() ||
        !(new File(root, "settings.gradle").isFile() || new File(root, "settings.gradle.kts").isFile()) ||
        !(new File(root, "app/build.gradle").isFile() || new File(root, "app/build.gradle.kts").isFile())) {
      throw new IOException("Select a complete Android Gradle project with an app module.");
    }
    try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(destination))) {
      zip.putNextEntry(new ZipEntry(root.getName() + "/")); zip.closeEntry();
      addTree(zip, root, root.getName(), 0);
      zip.putNextEntry(new ZipEntry(root.getName() + "/AIMobileStudio-Termux-Build.sh"));
      zip.write(BUILD_SCRIPT.getBytes("UTF-8")); zip.closeEntry(); zip.finish();
    }
  }

  private static void addTree(ZipOutputStream zip, File dir, String prefix, int depth) throws IOException {
    if (depth > 30) throw new IOException("Project nesting is too deep.");
    File[] children = dir.listFiles(); if (children == null) return;
    for (File child : children) {
      String n = child.getName().toLowerCase(Locale.ROOT);
      if (n.equals("build") || n.equals(".gradle") || n.equals(".git") || n.equals(".idea") ||
          n.equals("local.properties") || n.equals(".env") || n.startsWith(".env.") ||
          n.endsWith(".jks") || n.endsWith(".keystore") || n.contains("secret") ||
          n.contains("credential") || n.equals("keystore.properties")) continue;
      if (child.isDirectory()) {
        zip.putNextEntry(new ZipEntry(prefix + "/" + child.getName() + "/")); zip.closeEntry();
        addTree(zip, child, prefix + "/" + child.getName(), depth + 1);
      } else if (child.isFile()) {
        zip.putNextEntry(new ZipEntry(prefix + "/" + child.getName()));
        try (InputStream in = new BufferedInputStream(new FileInputStream(child))) {
          byte[] b = new byte[16384]; int count; while ((count = in.read(b)) != -1) zip.write(b, 0, count);
        }
        zip.closeEntry();
      }
    }
  }
}
