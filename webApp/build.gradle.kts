plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
            webpackTask {
                output.libraryTarget = "umd"
            }
            runTask {
                mainOutputFileName.set("webApp.js")
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

                // Reown AppKit for WalletConnect
                implementation(npm("@reown/appkit", "1.6.0"))
                implementation(npm("@reown/appkit-adapter-ethers", "1.6.0"))

                // Ethers.js for provider and signer
                implementation(npm("ethers", "6.13.2"))

                // Peer dependencies for Reown AppKit
                implementation(npm("@coinbase/wallet-sdk", "4.0.3"))
                implementation(npm("@ethersproject/sha2", "5.7.0"))
            }
        }
    }
}
