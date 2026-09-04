import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
    kotlin("plugin.serialization") version "1.9.0"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example"
    compileSdk { version = release(36) { minorApiLevel = 1 } }

    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.jks")
            storePassword = "012253"
            keyAlias = "my-key-alias"
            keyPassword = "012253"
        }
    }

    defaultConfig {
        applicationId = "com.youssef.meenfeena"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.google.firebase:firebase-database-ktx:20.3.1")
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("io.getstream:stream-webrtc-android:1.1.1")
}

abstract class ProvisionMolhimFontTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val destDir = outputDir.get().asFile
        destDir.mkdirs()

        val destFile = File(destDir, "molhim.ttf")
        if (!destFile.exists()) {
            val systemFonts = listOf(
                "/system/fonts/NotoSansArabic-Regular.ttf",
                "/system/fonts/DroidSansArabic.ttf",
                "/system/fonts/Roboto-Regular.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
            )
            for (path in systemFonts) {
                val f = File(path)
                if (f.exists()) {
                    f.copyTo(destFile, true)
                    break
                }
            }
        }

        val handjetFile = File(destDir, "handjet.ttf")
        if (!handjetFile.exists()) {
            var downloaded = false
            try {
                val uri = URI("https://raw.githubusercontent.com/google/fonts/main/ofl/handjet/Handjet%5BELGR%2CELSH%2Cwght%5D.ttf")
                val url: URL = uri.toURL()

                url.openStream().use { input ->
                    Files.copy(input, handjetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                downloaded = true
            } catch (e: Exception) {
                println("Failed to download Handjet font from github: ${e.message}")
            }

            if (!downloaded) {
                if (destFile.exists()) {
                    destFile.copyTo(handjetFile, true)
                } else {
                    val systemFonts = listOf(
                        "/system/fonts/Roboto-Regular.ttf",
                        "/system/fonts/NotoSansArabic-Regular.ttf",
                        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
                    )
                    for (path in systemFonts) {
                        val f = File(path)
                        if (f.exists()) {
                            f.copyTo(handjetFile, true)
                            break
                        }
                    }
                }
            }
        }
    }
}

val provisionMolhimFont = tasks.register<ProvisionMolhimFontTask>("provisionMolhimFont") {
    outputDir.set(layout.projectDirectory.dir("src/main/res/font"))
}

tasks.named("preBuild") {
    dependsOn(provisionMolhimFont)
}