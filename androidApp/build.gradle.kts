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
            println("Warning: Could not load .env file: ${e.message}")
        }
    } else {
        println("Warning: .env file not found at $filePath")
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
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            val devMnemonic = envProps.getProperty("MNEMONIC_WORDS") ?: ""
            buildConfigField("String", "MNEMONIC_WORDS", "\"$devMnemonic\"")
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

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
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

    implementation(libs.kethereum.model)
    implementation(libs.kethereum.bip32)
    implementation(libs.kethereum.bip39)
    implementation(libs.kethereum.sign)
    implementation(libs.kethereum.crypto)
    implementation(libs.kethereum.crypto.impl)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(project(":shared"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
