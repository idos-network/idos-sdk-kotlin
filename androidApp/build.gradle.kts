import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

fun getEnvProperties(filePath: String): Properties {
    val properties = Properties()
    val envFile = File(filePath)
    if (envFile.exists()) {
        try {
            FileInputStream(envFile).use { properties.load(it) }
        } catch (e: Exception) {
            logger.warn("Could not load .env file: ${e.message}")
        }
    } else {
        logger.warn(".env file not found at $filePath")
    }
    return properties
}

// Load properties from .env file at the project root
// Adjust the path if your .env file is elsewhere (e.g., project.rootDir.resolve(".env"))
val envProps = getEnvProperties("${project.rootDir}/.env")

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "org.idos.app"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "org.idos.app"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        testOptions {
            // To use Android Test Orchestrator for better test isolation
            execution = "ANDROIDX_TEST_ORCHESTRATOR"
        }

        testApplicationId = "org.idos.app.test"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "MNEMONIC_WORDS", "\"\"")
            buildConfigField("String", "TEST_PASSWORD", "\"\"")
            buildConfigField("String", "REOWN_PROJECT_ID", "\"\"")
        }
        debug {
            val devMnemonic = envProps.getProperty("MNEMONIC_WORDS") ?: ""
            val devPassword = envProps.getProperty("PASSWORD") ?: ""
            val reownProjectId = envProps.getProperty("REOWN_PROJECT_ID") ?: ""
            buildConfigField("String", "MNEMONIC_WORDS", "\"$devMnemonic\"")
            buildConfigField("String", "TEST_PASSWORD", "\"$devPassword\"")
            buildConfigField("String", "REOWN_PROJECT_ID", "\"$reownProjectId\"")
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11) // Use typed enum instead of string
    }
}

// Force BouncyCastle resolution to avoid conflicts between Kethereum and Reown
configurations.all {
    resolutionStrategy {
        force("org.bouncycastle:bcprov-jdk18on:1.82")
    }
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.process)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.foundation)
    implementation(libs.coroutines.android)
    implementation(libs.timber)
    implementation(libs.androidx.foundation)

    // Security
    implementation(libs.security.crypto)
    implementation(libs.security.crypto.ktx)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.kethereum.model)
    implementation(libs.kethereum.bip32)
    implementation(libs.kethereum.bip39)
    implementation(libs.kethereum.sign) {
    }
    implementation(libs.kethereum.crypto)
    implementation(libs.kethereum.crypto.impl) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
    }

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.kotlinx.serialization.json)

    // Reown AppKit (WalletConnect)
    implementation(platform(libs.reown.bom))
    implementation(libs.reown.core) {
        exclude(group = "com.github.komputing.kethereum", module = "crypto_impl_spongycastle")
    }
    implementation(libs.reown.appkit) {
        exclude(group = "com.github.komputing.kethereum", module = "crypto_impl_spongycastle")
    }

    implementation(project(":shared"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.koin.android)
    androidTestImplementation(libs.koin.androidx.compose)
    androidTestImplementation(libs.androidx.runner)
    androidTestUtil(libs.androidx.orchestrator)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
