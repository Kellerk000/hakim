import java.io.File
import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hakim.clinical"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.drali.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        // الموبايلات والتابات الحديثة كلها arm64
        ndk { abiFilters += listOf("arm64-v8a") }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fexceptions")
            }
        }
    }

    // مفتاح توقيع ثابت: هيك كل نسخة جديدة بتتثبّت فوق القديمة وبتضل بيانات المرضى
    signingConfigs {
        create("drali") {
            storeFile = file("drali-release.jks")
            storePassword = "oSbsVMFV5D69XjzQPysPtPyB"
            keyAlias = "drali"
            keyPassword = "oSbsVMFV5D69XjzQPysPtPyB"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("drali")
        }
        debug {
            signingConfig = signingConfigs.getByName("drali")
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

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
}

// خط IBM Plex Sans Arabic: بينزل تلقائياً وقت البناء لمجلد assets/fonts.
// إذا ما في نت وقت البناء، التطبيق بيشتغل بخط الجهاز.
val downloadFonts by tasks.registering {
    val dir = layout.projectDirectory.dir("src/main/assets/fonts").asFile
    doLast {
        dir.mkdirs()
        listOf("Regular", "Medium", "SemiBold", "Bold").forEach { w ->
            val f = File(dir, "IBMPlexSansArabic-$w.ttf")
            if (!f.exists() || f.length() < 10_000) {
                try {
                    val url = URI(
                        "https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexsansarabic/IBMPlexSansArabic-$w.ttf"
                    ).toURL()
                    url.openStream().use { input -> f.outputStream().use { out -> input.copyTo(out) } }
                    logger.lifecycle("Downloaded font ${f.name} (${f.length()} bytes)")
                } catch (e: Exception) {
                    logger.warn("Could not download font ${f.name}: ${e.message}")
                    f.delete()
                }
            }
        }
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(downloadFonts) }
