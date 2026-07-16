plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.worldcup.androidstudiolite.designsystem"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register("checkNoMaterial") {
    val srcDir = layout.projectDirectory.dir("src/main/java")
    val rootPath = layout.projectDirectory.asFile
    inputs.dir(srcDir)
    doLast {
        val offenders = srcDir.asFileTree.matching { include("**/*.kt") }.filter { file ->
            file.readText().contains(Regex("import (androidx\\.compose\\.material|com\\.google\\.android\\.material|androidx\\.appcompat)"))
        }.files
        check(offenders.isEmpty()) {
            "Material/AppCompat imports are forbidden in :designsystem:\n" +
                offenders.joinToString("\n") { " - ${it.relativeTo(rootPath)}" }
        }
    }
}

tasks.matching { it.name.startsWith("compile") }.configureEach {
    dependsOn("checkNoMaterial")
}
