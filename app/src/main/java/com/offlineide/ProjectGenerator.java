package com.offlineide;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Creates a minimal, real Android Gradle project in the IDE workspace. */
public final class ProjectGenerator {
    private ProjectGenerator() {}
    private static void put(File root, String path, String content) throws Exception {
        File file = new File(root, path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
            throw new IllegalStateException("Cannot create " + parent);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
    public static File create(File workspace, String rawName) throws Exception {
        String name = rawName.replaceAll("[^A-Za-z0-9_]", "");
        if (name.isEmpty()) name = "MyApp";
        String pkg = "com.generated." + name.toLowerCase();
        File root = new File(workspace, name);
        if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("Cannot create project folder");
        String src = "app/src/main/java/" + pkg.replace('.', '/');
        put(root, "settings.gradle",
            "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\n" +
            "dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\n" +
            "rootProject.name = \"" + name + "\"\ninclude ':app'\n");
        put(root, "build.gradle", "plugins { id 'com.android.application' version '8.7.3' apply false }\n");
        put(root, "gradle.properties",
            "org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8\nandroid.useAndroidX=true\norg.gradle.caching=true\n");
        put(root, "app/build.gradle",
            "plugins { id 'com.android.application' }\n" +
            "android {\n namespace '" + pkg + "'\n compileSdk 35\n" +
            " defaultConfig { applicationId '" + pkg + "'; minSdk 24; targetSdk 35; versionCode 1; versionName '1.0' }\n" +
            " compileOptions { sourceCompatibility JavaVersion.VERSION_17; targetCompatibility JavaVersion.VERSION_17 }\n}\n");
        put(root, "app/src/main/AndroidManifest.xml",
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
            " <application android:theme=\"@android:style/Theme.Material.Light.NoActionBar\" android:label=\"" + name + "\" android:allowBackup=\"false\">\n" +
            "  <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
            "   <intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>\n" +
            "  </activity>\n </application>\n</manifest>\n");
        put(root, src + "/MainActivity.java",
            "package " + pkg + ";\n\nimport android.app.Activity;\nimport android.os.Bundle;\n" +
            "import android.graphics.Color;\nimport android.view.Gravity;\nimport android.widget.TextView;\n\n" +
            "public class MainActivity extends Activity {\n" +
            " @Override public void onCreate(Bundle state) {\n  super.onCreate(state);\n" +
            "  TextView view = new TextView(this);\n  view.setText(\"Welcome to " + name + "!\\nYour Android app is ready to build.\");\n" +
            "  view.setTextSize(22); view.setTextColor(Color.rgb(25, 70, 48)); view.setGravity(Gravity.CENTER);\n" +
            "  setContentView(view);\n }\n}\n");
        put(root, "README.md",
            "# " + name + "\n\nGenerated native Android application (Java + Gradle).\n\n" +
            "Install JDK 17, Android SDK Platform 35 and Build Tools. Resolve dependencies once while online.\n\n" +
            "After all Gradle and Maven artifacts are cached, run gradle --offline assembleDebug from this folder.\n\n" +
            "APK output: app/build/outputs/apk/debug/app-debug.apk\n");
        return new File(root, src + "/MainActivity.java");
    }
}
