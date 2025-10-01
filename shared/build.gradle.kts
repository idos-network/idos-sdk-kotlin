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

    // Configure cinterop for iOS device (arm64) - XCFramework
    iosArm64().compilations.getByName("main") {
        cinterops {
            val libsodium by creating {
                defFile(project.file("src/iosMain/c_interop/libsodium.def"))
                compilerOpts("-I$projectDir/libs/libsodium.xcframework/ios-arm64/Headers")
            }
        }
        compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-include-binary")
            compilerOptions.freeCompilerArgs.add("$projectDir/libs/libsodium.xcframework/ios-arm64/libsodium.a")
        }
    }

    // Configure cinterop for iOS simulator (arm64) - XCFramework
    iosSimulatorArm64().compilations.getByName("main") {
        cinterops {
            val libsodium by creating {
                defFile(project.file("src/iosMain/c_interop/libsodium.def"))
                compilerOpts("-I$projectDir/libs/libsodium.xcframework/ios-arm64_x86_64-simulator/Headers")
            }
        }
        compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-include-binary")
            compilerOptions.freeCompilerArgs.add("$projectDir/libs/libsodium.xcframework/ios-arm64_x86_64-simulator/libsodium.a")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io)
            implementation(project.dependencies.platform(libs.kotlincrypto.hash.bom))
            implementation(libs.kotlincrypto.hash.sha2)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // NaCl for Android
            implementation("${libs.lazysodium.android.get()}@aar")
            implementation("${libs.jna.get()}@aar")
            // AndroidX Security with StrongBox support
            implementation(libs.security.crypto.ktx)
            // Bouncy Castle for SCrypt implementation
            implementation(libs.bcprov.jdk15to18)
        }

        jvmMain.dependencies {
            implementation(libs.tweetnacl.java)
            implementation(libs.bcprov.jdk15on)
            implementation(libs.kethereum)
        }

        iosMain.dependencies {
//            implementation(libs.multiplatform.crypto.libsodium.bindings)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest)
            implementation(libs.kotest.assert)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest)
            implementation(libs.kotest.runner)
            implementation(libs.kotest.assert)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.dotenv.kotlin)
        }

        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.runner)
            implementation(libs.kotest.assert)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.test.core)
            // Desktop libsodium for unit tests (unit tests run on host JVM, not Android)
            implementation(libs.lazysodium.java)
            implementation(libs.jna)
        }

        iosTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.assert)
            implementation(libs.kotlinx.coroutines.test)
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
