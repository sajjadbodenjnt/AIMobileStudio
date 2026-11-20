package com.offlineide;

import java.io.*;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** SAF-based, user-confirmed handoff to Termux; it never crosses Android app sandboxes. */
public final class TermuxProjectBridge {
  private TermuxProjectBridge() {}
  private static final String BUILD_SCRIPT =
      "#!/data/data/com.termux/files/usr/bin/bash" + "\n" +
      "set -Eeuo pipefail" + "\n" +
      "ROOT=\"$(cd \"$(dirname \"$0\")\" && pwd)\"" + "\n" +
      "cd \"$ROOT\"" + "\n" +
      "[[ \"${PREFIX:-}\" == /data/data/com.termux/files/usr ]] || { echo \"Run inside Termux, not proot.\" >&2; exit 2; }" + "\n" +
      "command -v java >/dev/null 2>&1 || { echo \"Install JDK: pkg install openjdk-17\"; exit 3; }" + "\n" +
      "java -version 2>&1 | grep -Eq 'version \"(17|21)(\\.|\\+)' || { echo \"JDK 17 or 21 required.\"; exit 3; }" + "\n" +
      "SDK=\"${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}\"" + "\n" +
      "for p in \"$HOME/lib/android-sdk-9123335\" \"$HOME/lib/android-sdk\" \"$HOME/Android/Sdk\"; do" + "\n" +
      "  if [[ -z \"$SDK\" && -d \"$p/platforms\" ]]; then SDK=\"$p\"; fi" + "\n" +
      "done" + "\n" +
      "[[ -n \"$SDK\" && -d \"$SDK/platforms\" && -d \"$SDK/build-tools\" ]] || { echo \"Set ANDROID_HOME to an installed Android SDK with platforms and build-tools.\"; exit 4; }" + "\n" +
      "export ANDROID_HOME=\"$SDK\" ANDROID_SDK_ROOT=\"$SDK\"" + "\n" +
      "[[ -f settings.gradle || -f settings.gradle.kts ]] || { echo \"settings.gradle(.kts) missing.\"; exit 5; }" + "\n" +
      "grep -RqsE 'com\\.android\\.(application|library)|com.android.tools.build:gradle' --include='*.gradle' --include='*.gradle.kts' . || { echo \"Android Gradle Plugin declaration not found.\"; exit 5; }" + "\n" +
      "if [[ -f gradlew ]]; then chmod +x gradlew; GRADLE=(./gradlew)" + "\n" +
      "elif command -v gradle >/dev/null 2>&1; then GRADLE=(gradle)" + "\n" +
      "else echo \"Install Gradle: pkg install gradle\"; exit 6; fi" + "\n" +
      "BUILD_TOOLS=\"$(find \"$SDK/build-tools\" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)\"" + "\n" +
      "[[ -n \"$BUILD_TOOLS\" ]] || { echo \"No Android Build Tools found.\"; exit 7; }" + "\n" +
      "echo \"Java: $(java -version 2>&1 | head -n 1)\"" + "\n" +
      "echo \"SDK: $SDK\"" + "\n" +
      "echo \"Build Tools: $(basename \"$BUILD_TOOLS\")\"" + "\n" +
      "\"${GRADLE[@]}\" --version | head -n 12" + "\n" +
      "TYPE=\"${AIMS_BUILD_TYPE:-debug}\"" + "\n" +
      "case \"$TYPE\" in debug) TASK=assembleDebug; KIND=apk;; release) TASK=assembleRelease; KIND=apk;; aab) TASK=bundleDebug; KIND=aab;; aab-release) TASK=bundleRelease; KIND=aab;; *) echo \"AIMS_BUILD_TYPE must be debug, release, aab, or aab-release.\"; exit 8;; esac" + "\n" +
      "echo \"Building all Android modules: $TASK ($KIND)\"" + "\n" +
      "\"\${GRADLE[@]}\" --no-daemon --stacktrace --console=plain \"$TASK\"" + "\n" +
      "if [[ \"$KIND\" == apk ]]; then mapfile -t OUTPUTS < <(find . -type f -name '*.apk' | grep '/build/outputs/apk/' | sort); else mapfile -t OUTPUTS < <(find . -type f -name '*.aab' | grep '/build/outputs/bundle/' | sort); fi" + "\n" +
      "((\"\${#OUTPUTS[@]}\" > 0)) || { echo \"No $KIND artifact produced.\"; exit 9; }" + "\n" +
      "OUT=\"$HOME/storage/downloads/AIMobileStudio-Builds/$(basename \"$ROOT\")\"" + "\n" +
      "mkdir -p \"$OUT\"" + "\n" +
      "for artifact in \"\${OUTPUTS[@]}\"; do" + "\n" +
      "  name=\"$(echo \"$artifact\" | sed 's#^./##; s#/#-#g; s#[^A-Za-z0-9._-]#_#g')\"" + "\n" +
      "  dest=\"$OUT/$name\"" + "\n" +
      "  cp \"$artifact\" \"$dest\"" + "\n" +
      "  test -s \"$dest\" || { echo \"Empty artifact: $dest\"; exit 10; }" + "\n" +
      "  if [[ \"$KIND\" == apk && -x \"$BUILD_TOOLS/apksigner\" ]]; then \"$BUILD_TOOLS/apksigner\" verify --verbose \"$dest\"; fi" + "\n" +
      "  echo \"\${KIND^^}_READY=$dest\"" + "\n" +
      "done" + "\n" +
      "[[ \"$TYPE\" != release && \"$TYPE\" != aab-release ]] || echo \"WARNING: Configure your own release signing key before distribution.\"" + "\n" +
      "echo \"BUILD SUCCESS: copied \${#OUTPUTS[@]} $KIND artifact(s) to $OUT\"" + "\n" +
      "" + "\n";

  public static void exportZip(File project, OutputStream destination) throws IOException {
    File root = project.getCanonicalFile();
    if (!root.isDirectory() ||
        !(new File(root, "settings.gradle").isFile() || new File(root, "settings.gradle.kts").isFile()) ||
        !hasAndroidApplicationModule(root, 0)) {
      throw new IOException("Select a Gradle project containing an Android application module.");
    }
    try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(destination))) {
      zip.putNextEntry(new ZipEntry(root.getName() + "/")); zip.closeEntry();
      addTree(zip, root, root.getName(), 0);
      zip.putNextEntry(new ZipEntry(root.getName() + "/AIMobileStudio-Termux-Build.sh"));
      zip.write(BUILD_SCRIPT.getBytes("UTF-8")); zip.closeEntry(); zip.finish();
    }
  }

  private static boolean hasAndroidApplicationModule(File dir, int depth) throws IOException {
    if (depth > 12) return false;
    File[] children = dir.listFiles();
    if (children == null) return false;
    for (File child : children) {
      String name = child.getName();
      if (child.isDirectory()) {
        if (name.equals("build") || name.equals(".gradle") || name.equals(".git") || name.equals(".idea")) continue;
        if (hasAndroidApplicationModule(child, depth + 1)) return true;
      } else if (child.isFile() && (name.equals("build.gradle") || name.equals("build.gradle.kts"))) {
        if (child.length() > 1024 * 1024) continue;
        String source = new String(java.nio.file.Files.readAllBytes(child.toPath()), "UTF-8");
        if (source.contains("com.android.application") || source.contains("com.android.tools.build:gradle")) return true;
      }
    }
    return false;
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
