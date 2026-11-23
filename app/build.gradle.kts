@file:Suppress("GradleDependency") // --- FIX: Silences "Use version catalog instead" warnings ---

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.example.loom"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.loom"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Release-speed build for testing on the phone next to the real app (R8 + baseline profile,
        // not debuggable). Debug builds run Compose unoptimized and scroll noticeably worse.
        create("bench") {
            initWith(getByName("release"))
            applicationIdSuffix = ".bench"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// --- FIX: Modern Kotlin 2.0 compilerOptions format ---
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.profileinstaller)
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // --- FIX: Removed the duplicate hardcoded coil-compose dependency ---

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    "baselineProfile"(project(":baselineprofile"))

    // --- FIX: Deduplicated Compose BOM implementation ---
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Hilt for Dependency Injection
    implementation("com.google.dagger:hilt-android:2.53.1")
    ksp("com.google.dagger:hilt-compiler:2.53.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Media & UI Utils
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // DataStore & WorkManager
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Home-screen widgets and Health Connect sync
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")
}