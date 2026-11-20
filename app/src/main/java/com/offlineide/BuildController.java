package com.offlineide;

import android.content.Context;
import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Executes the explicitly provisioned, app-private Gradle toolchain.
 * This is an on-device/offline builder; it does not pretend to be an online build service.
 */
public final class BuildController {
  private static final long BUILD_TIMEOUT_MINUTES = 20;
  private static final int MAX_LOG_BYTES = 2 * 1024 * 1024;
  private final Context context;

  public BuildController(Context c) { context = c.getApplicationContext(); }

  private File toolchainRoot() { return new File(context.getFilesDir(), "toolchain"); }
  private File gradle() { return new File(toolchainRoot(), "gradle/bin/gradle"); }
  private File sdk() { return new File(toolchainRoot(), "android-sdk"); }

  public String status() {
    File platform = new File(sdk(), "platforms/android-35/android.jar");
    File buildTools = new File(sdk(), "build-tools");
    boolean hasBuildTools = buildTools.isDirectory() && buildTools.list() != null && buildTools.list().length > 0;
    return "Build mode: ON-DEVICE / OFFLINE" +
      "\nGradle executable: " + (gradle().canExecute() ? "READY" : "MISSING") +
      "\nAndroid SDK: " + (sdk().isDirectory() ? "FOUND" : "MISSING") +
      "\nAndroid 35 platform: " + (platform.isFile() ? "READY" : "MISSING") +
      "\nAndroid Build Tools: " + (hasBuildTools ? "FOUND" : "MISSING") +
      "\nToolchain directory: " + toolchainRoot().getAbsolutePath() +
      "\nBuild task: clean assembleDebug";
  }

  public File expectedApk(File project) throws IOException {
    return new File(project.getCanonicalFile(), "app/build/outputs/apk/debug/app-debug.apk");
  }

  public String build(File project, boolean approved) throws Exception {
    if (!approved) throw new SecurityException("Build requires explicit user approval.");

    File canonicalWorkspace = new File(context.getFilesDir(), "workspace").getCanonicalFile();
    File canonicalProject = project.getCanonicalFile();
    String workspacePrefix = canonicalWorkspace.getPath() + File.separator;
    if (!canonicalProject.getPath().startsWith(workspacePrefix) ||
        !new File(canonicalProject, "settings.gradle").isFile() ||
        !new File(canonicalProject, "app/build.gradle").isFile()) {
      throw new SecurityException("Select a complete Gradle project inside AIMobileStudio workspace.");
    }

    File platform = new File(sdk(), "platforms/android-35/android.jar");
    if (!gradle().canExecute() || !platform.isFile()) {
      throw new FileNotFoundException("The app-private Gradle/Android SDK toolchain is incomplete.\n" + status());
    }

    File localProperties = new File(canonicalProject, "local.properties");
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(localProperties), "UTF-8")) {
      writer.write("sdk.dir=" + sdk().getAbsolutePath().replace("\\", "\\\\") + "\n");
    }

    ProcessBuilder pb = new ProcessBuilder(gradle().getAbsolutePath(),
      "--offline", "--no-daemon", "--stacktrace", "clean", "assembleDebug");
    pb.directory(canonicalProject);
    pb.redirectErrorStream(true);
    pb.environment().put("ANDROID_HOME", sdk().getAbsolutePath());
    pb.environment().put("ANDROID_SDK_ROOT", sdk().getAbsolutePath());
    pb.environment().put("JAVA_HOME", new File(toolchainRoot(), "jdk").getAbsolutePath());

    Process process = pb.start();
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    Thread drain = new Thread(() -> {
      try (InputStream in = process.getInputStream()) {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
          synchronized (captured) {
            int remaining = MAX_LOG_BYTES - captured.size();
            if (remaining > 0) captured.write(buffer, 0, Math.min(remaining, n));
          }
        }
      } catch (IOException ignored) { }
    }, "aims-gradle-log");
    drain.setDaemon(true);
    drain.start();

    boolean finished = process.waitFor(BUILD_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    if (!finished) {
      process.destroy();
      if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
      drain.join(3000);
      throw new IOException("Gradle build timed out after " + BUILD_TIMEOUT_MINUTES + " minutes.\n" + logText(captured));
    }
    drain.join(5000);
    String output = logText(captured);
    if (process.exitValue() != 0) {
      throw new IOException("Gradle build failed (exit " + process.exitValue() + ").\n" + output);
    }

    File apk = expectedApk(canonicalProject);
    if (!apk.isFile() || apk.length() == 0) {
      throw new IOException("Gradle reported success but the expected APK is missing or empty.\n" + output);
    }
    return "BUILD SUCCESS\nAPK size: " + apk.length() + " bytes\nAPK: " + apk.getAbsolutePath() + "\n\n" + output;
  }

  private static String logText(ByteArrayOutputStream out) {
    synchronized (out) {
      String value = out.toString();
      return value + (out.size() >= MAX_LOG_BYTES ? "\n[Build log truncated at 2 MB]" : "");
    }
  }
}
