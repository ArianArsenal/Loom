plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    // --- FIX: Removed the alias that was secretly forcing v1.2.4.
    // This now correctly inherits v1.3.3 from your root project! ---
    id("androidx.baselineprofile")
}

android {
    namespace = "com.example.baselineprofile"
    compileSdk = 35 // --- FIX: Synced to match your App module ---

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 35 // --- FIX: Synced to match your App module ---

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

// --- FIX: Modern Kotlin 2.0 compilerOptions format (removed deprecated kotlinOptions) ---
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on managed devices or connected devices.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
}

androidComponents {
    onVariants { v ->
        val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
        v.instrumentationRunnerArguments.put(
            "targetAppId",
            v.testedApks.map { artifactsLoader.load(it)?.applicationId }
        )
    }
}