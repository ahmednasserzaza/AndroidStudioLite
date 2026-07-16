package com.worldcup.androidstudiolite.data.local.templates

import com.worldcup.androidstudiolite.entities.Project
import java.io.File
import java.util.Base64

object ProjectTemplates {

    const val META_FILE = ".aslproject.json"

    fun writeNewProject(project: Project) {
        val projectDir = File(project.path)
        val pkg = project.packageName
        val pkgPath = pkg.replace('.', '/')
        val appName = project.name

        val files = mapOf(
            "settings.gradle.kts" to settingsGradle(appName),
            "build.gradle.kts" to ROOT_BUILD_GRADLE,
            "gradle.properties" to GRADLE_PROPERTIES,
            ".gitignore" to GITIGNORE,
            ".github/workflows/build.yml" to WORKFLOW,
            "app/build.gradle.kts" to appBuildGradle(pkg),
            "app/src/main/AndroidManifest.xml" to manifest(),
            "app/src/main/java/$pkgPath/MainActivity.kt" to mainActivity(pkg, appName),
            "app/src/main/res/values/strings.xml" to strings(appName),
            "app/src/main/res/values/themes.xml" to THEMES,
            "app/src/main/res/values/colors.xml" to COLORS,
            "app/src/main/res/drawable/ic_launcher_foreground.xml" to IC_FOREGROUND,
            "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" to IC_LAUNCHER,
        )
        for ((path, content) in files) {
            val file = File(projectDir, path)
            file.parentFile?.mkdirs()
            file.writeText(content)
        }
        writeKeystore(projectDir)
    }

    fun writeKeystore(projectDir: File) {
        File(projectDir, "app/debug.keystore")
            .writeBytes(Base64.getMimeDecoder().decode(DEBUG_KEYSTORE_B64))
    }

    fun writeWorkflow(projectDir: File) {
        val file = File(projectDir, ".github/workflows/build.yml")
        file.parentFile?.mkdirs()
        file.writeText(WORKFLOW)
    }

    private fun settingsGradle(appName: String) = """
        pluginManagement {
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
            }
        }
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                google()
                mavenCentral()
            }
        }

        rootProject.name = "${appName.replace("\"", "")}"
        include(":app")
    """.trimIndent() + "\n"

    private val ROOT_BUILD_GRADLE = """
        plugins {
            id("com.android.application") version "8.7.3" apply false
            id("org.jetbrains.kotlin.android") version "2.0.21" apply false
            id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
        }
    """.trimIndent() + "\n"

    private val GRADLE_PROPERTIES = """
        org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
        android.useAndroidX=true
        kotlin.code.style=official
    """.trimIndent() + "\n"

    private val GITIGNORE = """
        .gradle/
        build/
        local.properties
        .idea/
        *.iml
    """.trimIndent() + "\n"

    private val WORKFLOW = """
        name: Build APK
        on: [push, workflow_dispatch]

        jobs:
          build:
            runs-on: ubuntu-latest
            steps:
              - uses: actions/checkout@v4
              - uses: actions/setup-java@v4
                with:
                  distribution: temurin
                  java-version: '17'
              - uses: gradle/actions/setup-gradle@v4
                with:
                  gradle-version: '8.9'
              - name: Build debug APK
                run: gradle :app:assembleDebug --no-daemon
              - uses: actions/upload-artifact@v4
                with:
                  name: app-debug
                  path: app/build/outputs/apk/debug/app-debug.apk
    """.trimIndent() + "\n"

    private fun appBuildGradle(pkg: String) = """
        plugins {
            id("com.android.application")
            id("org.jetbrains.kotlin.android")
            id("org.jetbrains.kotlin.plugin.compose")
        }

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

            signingConfigs {
                getByName("debug") {
                    storeFile = file("debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }

            buildTypes {
                debug {
                    signingConfig = signingConfigs.getByName("debug")
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            kotlinOptions {
                jvmTarget = "17"
            }
            buildFeatures {
                compose = true
            }
        }

        dependencies {
            implementation(platform("androidx.compose:compose-bom:2024.09.03"))
            implementation("androidx.core:core-ktx:1.13.1")
            implementation("androidx.activity:activity-compose:1.9.2")
            implementation("androidx.compose.ui:ui")
            implementation("androidx.compose.ui:ui-graphics")
            implementation("androidx.compose.material3:material3")
        }
    """.trimIndent() + "\n"

    private fun manifest() = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">

            <application
                android:allowBackup="true"
                android:icon="@mipmap/ic_launcher"
                android:label="@string/app_name"
                android:supportsRtl="true"
                android:theme="@style/Theme.App">
                <activity
                    android:name=".MainActivity"
                    android:exported="true">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
            </application>

        </manifest>
    """.trimIndent() + "\n"

    private fun mainActivity(pkg: String, appName: String) = """
        package $pkg

        import android.os.Bundle
        import androidx.activity.ComponentActivity
        import androidx.activity.compose.setContent
        import androidx.compose.foundation.layout.Arrangement
        import androidx.compose.foundation.layout.Column
        import androidx.compose.foundation.layout.Spacer
        import androidx.compose.foundation.layout.fillMaxSize
        import androidx.compose.foundation.layout.height
        import androidx.compose.foundation.layout.padding
        import androidx.compose.material3.Button
        import androidx.compose.material3.MaterialTheme
        import androidx.compose.material3.Scaffold
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.getValue
        import androidx.compose.runtime.mutableIntStateOf
        import androidx.compose.runtime.remember
        import androidx.compose.runtime.setValue
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.unit.dp

        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    MaterialTheme {
                        App()
                    }
                }
            }
        }

        @Composable
        fun App() {
            var count by remember { mutableIntStateOf(0) }
            Scaffold { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Hello from $appName!",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Built on your phone, compiled in the cloud.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Count: ${'$'}count",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { count++ }) {
                        Text("Tap me")
                    }
                }
            }
        }
    """.trimIndent() + "\n"

    private fun strings(appName: String) = """
        <resources>
            <string name="app_name">${appName.replace("&", "&amp;").replace("<", "&lt;")}</string>
        </resources>
    """.trimIndent() + "\n"

    private val THEMES = """
        <resources>
            <style name="Theme.App" parent="android:Theme.Material.Light.NoActionBar" />
        </resources>
    """.trimIndent() + "\n"

    private val COLORS = """
        <resources>
            <color name="ic_launcher_background">#1E88E5</color>
        </resources>
    """.trimIndent() + "\n"

    private val IC_FOREGROUND = """
        <vector xmlns:android="http://schemas.android.com/apk/res/android"
            android:width="108dp"
            android:height="108dp"
            android:viewportWidth="108"
            android:viewportHeight="108">
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M42,36 L42,72 L72,54 Z" />
        </vector>
    """.trimIndent() + "\n"

    private val IC_LAUNCHER = """
        <?xml version="1.0" encoding="utf-8"?>
        <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
            <background android:drawable="@color/ic_launcher_background" />
            <foreground android:drawable="@drawable/ic_launcher_foreground" />
        </adaptive-icon>
    """.trimIndent() + "\n"

    private const val DEBUG_KEYSTORE_B64 =
        "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"
}
