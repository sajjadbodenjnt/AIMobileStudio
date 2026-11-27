package com.droidforge.ai

import java.io.File

class ProjectScaffolder(private val root: File) {
    fun writeComposeProject(mainActivitySource: String): File {
        val app = File(root, "app/src/main/java/com/example/generated").apply { mkdirs() }
        File(app, "MainActivity.kt").writeText(mainActivitySource)
        File(root, "settings.gradle.kts").writeText(
            """pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "GeneratedApp"
include(":app")
"""
        )
        File(root, "build.gradle.kts").writeText(
            """plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
"""
        )
        File(root, "app/build.gradle.kts").apply {
            parentFile?.mkdirs()
            writeText(
                """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.example.generated"
    compileSdk = 35
    defaultConfig { applicationId = "com.example.generated"; minSdk = 26; targetSdk = 35 }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
}
"""
            )
        }
        File(root, "app/src/main/AndroidManifest.xml").apply {
            parentFile?.mkdirs()
            writeText(
                """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="Generated App" android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
        </activity>
    </application>
</manifest>
"""
            )
        }
        return root
    }

    /** Writes a dependency-free Android Views project from the current designer XML.
     * This generates source files only; it does not claim to compile an APK on-device.
     */
    fun writeXmlAndroidProject(layoutXml: String, appLabel: String = "Generated App"): File {
        val pkg = "com.example.generated"
        val javaDir = File(root, "app/src/main/java/com/example/generated").apply { mkdirs() }
        val layoutDir = File(root, "app/src/main/res/layout").apply { mkdirs() }
        File(layoutDir, "activity_main.xml").writeText(layoutXml)
        File(javaDir, "MainActivity.java").writeText(
            """package com.example.generated;

import android.app.Activity;
import android.os.Bundle;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
"""
        )
        File(root, "settings.gradle.kts").writeText(
            """pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "DroidForgeGeneratedApp"
include(":app")
"""
        )
        File(root, "build.gradle.kts").writeText(
            """plugins {
    id("com.android.application") version "8.7.3" apply false
}
"""
        )
        File(root, "app/build.gradle.kts").apply {
            parentFile?.mkdirs()
            writeText(
                """plugins { id("com.android.application") }
android {
    namespace = "$pkg"
    compileSdk = 35
    defaultConfig {
        applicationId = "$pkg"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
"""
            )
        }
        File(root, "app/src/main/AndroidManifest.xml").apply {
            parentFile?.mkdirs()
            writeText(
                """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="$appLabel" android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""
            )
        }
        return root
    }

}
