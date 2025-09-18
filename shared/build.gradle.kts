import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "idos-sdk"
            freeCompilerArgs += listOf("-Xbinary=bundleId=org.idos")
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation("io.ktor:ktor-client-content-negotiation:3.2.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation(libs.kotlinx.io)
            implementation(project.dependencies.platform("org.kotlincrypto.hash:bom:0.7.1"))
            implementation("org.kotlincrypto.hash:sha2")
        }
        
        jvmMain.dependencies {
            implementation("com.github.InstantWebP2P:tweetnacl-java:1.1.2")
            implementation("org.bouncycastle:bcprov-jdk15on:1.70")
            implementation("com.github.komputing:kethereum:0.86.0")
        }
        
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest)
            implementation(libs.kotest.assert)
            implementation(libs.ktor.client.okhttp)
            implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
        }
    }
}

android {
    namespace = "org.idos"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

// ktlint configuration
ktlint {
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.PLAIN)
    }
}

// detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    config.setFrom(files("${rootDir}/detekt.yml"))
}
