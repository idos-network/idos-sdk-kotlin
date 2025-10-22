plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
//    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.gitVersioning)
}

// Configure automatic versioning from Git tags
version = "0.0.0-SNAPSHOT" // Fallback version

gitVersioning.apply {
    refs {
        // On version tags (v1.2.3), use the version without 'v' prefix
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
        // On main/develop branches, use latest tag + SNAPSHOT
        branch("main|develop") {
            version = "\${describe.tag.version}-SNAPSHOT"
        }
        // On feature branches, include branch name
        branch(".+") {
            version = "\${describe.tag.version}-\${ref}-SNAPSHOT"
        }
    }
    // Fallback for detached HEAD (CI environments)
    rev {
        version = "\${describe.tag.version}-\${commit.short}"
    }
}
