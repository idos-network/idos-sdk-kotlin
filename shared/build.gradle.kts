import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

    // Configure macOS target for testing (uses macOS libsodium)
    macosArm64()

    // Configure iOS targets
    val iosTargets =
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        )

    // Common iOS configuration
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "idos-sdk"
            freeCompilerArgs += listOf("-Xbinary=bundleId=org.idos")
            isStatic = true
        }
    }

    // Configure cinterop for iOS and macOS
    val darwinTargets = iosTargets + listOf(macosArm64())
    darwinTargets.forEach { target ->
        target.compilations.getByName("main") {
            val libsodium by cinterops.creating {
                defFile(project.file("src/iosMain/c_interop/libsodium.def"))
                compilerOpts("-I/opt/homebrew/Cellar/libsodium/1.0.20/include")
                includeDirs("/opt/homebrew/Cellar/libsodium/1.0.20/include")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.io)
                implementation(project.dependencies.platform(libs.kotlincrypto.hash.bom))
                implementation(libs.kotlincrypto.hash.sha2)
            }
        }

        // Create shared Darwin source set for all Darwin platforms (iOS + macOS)
        val darwinMain by creating {
            dependsOn(commonMain)
        }

        // iOS source sets depend on darwinMain
        val iosMain by creating {
            dependsOn(darwinMain)
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        // macOS depends on darwinMain (can add macOS-specific code to macosArm64Main)
        val macosArm64Main by getting {
            dependsOn(darwinMain)
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                // NaCl for Android
                implementation("${libs.lazysodium.android.get()}@aar")
                implementation("${libs.jna.get()}@aar")
                // AndroidX Security with StrongBox support
                implementation(libs.security.crypto.ktx)
                // Bouncy Castle for SCrypt implementation
                implementation(libs.bcprov.jdk15to18)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.tweetnacl.java)
                implementation(libs.bcprov.jdk15on)
                implementation(libs.kethereum)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest)
                implementation(libs.kotest.assert)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // JVM test dependencies
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotest)
                implementation(libs.kotest.runner)
                implementation(libs.kotest.assert)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.dotenv.kotlin)
            }
        }

        // Android test dependencies
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.runner)
                implementation(libs.kotest.assert)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.androidx.test.core)
                // Desktop libsodium for unit tests (unit tests run on host JVM, not Android)
                implementation(libs.lazysodium.java)
                implementation(libs.jna)
            }
        }

        // macOS test dependencies (uses macOS libsodium for testing)
        val macosArm64Test by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assert)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "org.idos"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
}

// ktlint configuration (minimal to avoid classpath timing issues)
ktlint {
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

// detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    config.setFrom(files("$rootDir/detekt.yml"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports {
        junitXml.required.set(true)
    }
    systemProperty("gradle.build.dir", layout.buildDirectory.asFile.get())
}
