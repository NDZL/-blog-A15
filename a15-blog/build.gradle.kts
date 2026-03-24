/**
 * A15-Blog — Android 15 (API 35) Blog Companion Samples
 *
 * This module accompanies the blog series at:
 *   https://github.com/NDZL/-blog-A15/wiki/A15%E2%80%90(0)%E2%80%90Agenda
 *
 * Blog (1) — Foundational App Compatibility & Build Updates:
 *   - compileSdk/targetSdk = 35 (Vanilla Ice Cream) is mandatory.
 *   - minSdk = 29 keeps us well above the new API-24 floor.
 *   - 16 KB page-size compliance: pure Kotlin/Java apps are already compliant;
 *     NDK components would need recompilation with `-Wl,-z,max-page-size=16384`.
 *
 * Blog (5) — Testing Strategy & Migration Timeline:
 *   - Phase 1 (Immediate): this build targets SDK 35, satisfying the first migration gate.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.ndzl.a15_blog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ndzl.a15_blog"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0 A15 blog samples"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.kotlinx.coroutines.android)
}
