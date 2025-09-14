plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("io.ktor.plugin") version "3.2.0"
}

group = "org.idos"
version = "1.0-SNAPSHOT"
val ktor_version: String by project

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-client-core:${ktor_version}")
    implementation("io.ktor:ktor-client-cio:${ktor_version}")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("com.github.komputing.kethereum:crypto_impl_bouncycastle:0.85.7")
    implementation("com.github.komputing.kethereum:bip39:0.86.0")
    implementation("com.github.komputing.kethereum:bip32:0.86.0")
    implementation("com.github.komputing.kethereum:eip191:0.86.0")
    implementation("com.github.komputing.kethereum:crypto:0.86.0")
    implementation("com.github.komputing.kethereum:model:0.86.0")
}

application {
    mainClass.set("org.idos.MainKt.main()")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(24)
}