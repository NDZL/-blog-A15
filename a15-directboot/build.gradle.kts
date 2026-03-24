/**
 * A15-DirectBoot — Android 15 Direct Boot & FGS-at-Boot Demo
 *
 * Demonstrates:
 *   1. A long-running Foreground Service that starts at boot via LOCKED_BOOT_COMPLETED
 *      (Direct Boot aware) and transitions through BOOT_COMPLETED (after first unlock).
 *   2. What resources are available BEFORE the user enters PIN/password
 *      (Device Encrypted storage vs Credential Encrypted storage).
 *
 * Android 15 considerations:
 *   - A15 restricts certain FGS types from starting at BOOT_COMPLETED.
 *     We use FGS type `specialUse` which is NOT in the restricted list,
 *     making it safe to start from a boot receiver.
 *   - Direct Boot mode: only Device Encrypted (DE) storage is available.
 *     Credential Encrypted (CE) storage is locked until the user unlocks.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.ndzl.a15_directboot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ndzl.a15_directboot"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0 A15 Direct Boot demo"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
