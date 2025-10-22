# Publishing Quick Reference

Quick reference for common publishing commands and workflows.

## 🚀 Quick Release Process

**Version is now automatically derived from Git tags!**

```bash
# 1. Create and push tag (version is derived automatically)
git tag v0.1.0
git push origin main
git push origin v0.1.0

# 2. Monitor release
open https://github.com/idos-network/idos-sdk-kotlin/actions

# That's it! No manual version updates needed.
# After release, branch automatically becomes <version>-SNAPSHOT
```

## 🔨 Build Commands

### Local Testing
```bash
# Lint and format check
./gradlew ktlintCheck detekt

# Run all tests
./gradlew jvmTest

# Test publishing locally
./gradlew publishToMavenLocal

# Check published artifacts
ls ~/.m2/repository/org/idos/idos-sdk-kotlin/
```

### Android Build
```bash
# Build release AAR
./gradlew :shared:assembleRelease

# Output location
ls shared/build/outputs/aar/shared-release.aar
```

### iOS Build
```bash
# Build XCFramework (release)
./gradlew :shared:assembleIdos_sdkReleaseXCFramework

# Build all variants (debug + release)
./gradlew :shared:assembleIdos_sdkXCFramework

# Output location
ls shared/build/XCFrameworks/release/idos_sdk.xcframework

# Zip for distribution
cd shared/build/XCFrameworks/release
zip -r idos_sdk.xcframework.zip idos_sdk.xcframework
shasum -a 256 idos_sdk.xcframework.zip
```

## 📦 Publishing Commands

### Maven Central
```bash
# Publish to Maven Central (requires secrets)
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache

# Publish specific target
./gradlew publishAndroidReleasePublicationToMavenCentralRepository
```

### Local Maven Repository
```bash
# Publish all to local Maven
./gradlew publishToMavenLocal

# Test consuming in another project
# In consumer's build.gradle.kts:
repositories {
    mavenLocal()
}
dependencies {
    implementation("org.idos:idos-sdk-kotlin:0.1.0-SNAPSHOT")
}
```

## 🔍 Verification Commands

### Check Available Tasks
```bash
# List publishing tasks
./gradlew tasks --group=publishing

# List build tasks
./gradlew tasks --group=build

# List all tasks
./gradlew tasks --all | grep -i "xcframework\|publish"
```

### Verify Build Configuration
```bash
# Check current version (derived from Git tags)
./gradlew properties | grep "^version:"

# Check group and artifact
./gradlew properties | grep -i "group"

# Validate POM generation
./gradlew generatePomFileForKotlinMultiplatformPublication

# Check generated POM
cat shared/build/publications/kotlinMultiplatform/pom-default.xml
```

### Check Maven Central Publication
```bash
# Search (after publishing - may take 10-30 min)
open "https://central.sonatype.com/search?q=idos-sdk-kotlin"

# Direct artifact link
open "https://central.sonatype.com/artifact/org.idos/idos-sdk-kotlin"
```

## 🔐 Environment Variables for CI/CD

### Required for Maven Central
```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername="your-username"
export ORG_GRADLE_PROJECT_mavenCentralPassword="your-password"

# Using base64 format (recommended):
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat key.base64)"
# OR using ASCII-armored format:
# export ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat key.asc)"

export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="key-password"
```

### GPG Key Export
```bash
# Base64 format (recommended - single line, no formatting issues):
gpg --export-secret-keys YOUR_KEY_ID | base64 | tr -d '\n' > key.base64

# ASCII-armored format (alternative - human-readable):
gpg --armor --export-secret-keys YOUR_KEY_ID > key.asc
```

### Required for GitHub Releases
```bash
export GITHUB_TOKEN="ghp_xxxxxxxxxxxx"
```

## 📝 Version Management

**Automatic versioning from Git tags** via `gradle-git-versioning` plugin.

### Check Current Version
```bash
# Version is automatically derived from Git state
./gradlew properties | grep "^version:"

# On tag v0.1.0 → version: 0.1.0
# On main branch → version: 0.1.0-SNAPSHOT
# On feature/foo → version: 0.1.0-feature/foo-SNAPSHOT
```

### Create Release Tag
```bash
# Release version (triggers workflow)
git tag v0.1.0      # Version automatically becomes: 0.1.0
git tag v1.2.3      # Version automatically becomes: 1.2.3

# Pre-release tags
git tag v0.1.0-alpha    # Version automatically becomes: 0.1.0-alpha
git tag v0.1.0-beta.1   # Version automatically becomes: 0.1.0-beta.1
git tag v0.1.0-rc.1     # Version automatically becomes: 0.1.0-rc.1
```

### No Manual Version Updates Needed!
- Version is **always** derived from Git
- SNAPSHOT automatically added to branches
- No `gradle.properties` edits required

## 🧪 Testing Package Distribution

### Test Gradle Dependency
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Or for testing snapshots:
        // maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

// build.gradle.kts
dependencies {
    implementation("org.idos:idos-sdk-kotlin:0.1.0")
}
```

### Test Swift Package Manager
```bash
# In Xcode: File → Add Package Dependencies
# URL: https://github.com/idos-network/idos-sdk-kotlin
# Version: 0.1.0

# Or via Package.swift
dependencies: [
    .package(url: "https://github.com/idos-network/idos-sdk-kotlin", from: "0.1.0")
]
```

### Test CocoaPods (if published)
```ruby
# Podfile
pod 'IdosSDK', '~> 0.1.0'

# Install
pod install

# Update
pod update IdosSDK
```

## 🐛 Troubleshooting

### Clear Gradle Caches
```bash
./gradlew clean
./gradlew --stop
rm -rf ~/.gradle/caches/
./gradlew build --refresh-dependencies
```

### Clear Configuration Cache
```bash
rm -rf .gradle/configuration-cache/
./gradlew build --no-configuration-cache
```

### Verify GPG Setup
```bash
# List keys
gpg --list-secret-keys --keyid-format LONG

# Export key for CI (choose format)
# Base64 (recommended):
gpg --export-secret-keys YOUR_KEY_ID | base64 | tr -d '\n' > key.base64
# ASCII-armored:
gpg --armor --export-secret-keys YOUR_KEY_ID > key.asc

# Test signing
echo "test" | gpg --armor --sign

# Verify key on server
gpg --keyserver keyserver.ubuntu.com --recv-keys YOUR_KEY_ID
```

### Check XCFramework Contents
```bash
# List architectures
lipo -info shared/build/XCFrameworks/release/idos_sdk.xcframework/ios-arm64/idos_sdk.framework/idos_sdk

# Verify framework structure
tree shared/build/XCFrameworks/release/idos_sdk.xcframework
```

## 📊 CI/CD Workflow Monitoring

### View Workflow Runs
```bash
# Via GitHub CLI
gh run list --workflow=release.yml
gh run view <RUN_ID>

# Or open in browser
open https://github.com/idos-network/idos-sdk-kotlin/actions
```

### Download Artifacts
```bash
# Via GitHub CLI
gh run download <RUN_ID>

# Or from releases page
open https://github.com/idos-network/idos-sdk-kotlin/releases
```

## 📱 Platform-Specific Commands

### Android
```bash
# Assemble debug
./gradlew :shared:assembleDebug

# Assemble release
./gradlew :shared:assembleRelease

# Run Android tests
./gradlew :shared:testDebugUnitTest
```

### iOS
```bash
# Build for device
./gradlew :shared:linkReleaseFrameworkIosArm64

# Build for simulator
./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64

# Run iOS tests
./gradlew :shared:iosSimulatorArm64Test
```

### JVM
```bash
# Build JVM
./gradlew :shared:jvmJar

# Run JVM tests
./gradlew :shared:jvmTest
```

## 🔗 Useful Links

- **Repository**: https://github.com/idos-network/idos-sdk-kotlin
- **Releases**: https://github.com/idos-network/idos-sdk-kotlin/releases
- **Actions**: https://github.com/idos-network/idos-sdk-kotlin/actions
- **Maven Central**: https://central.sonatype.com/artifact/org.idos/idos-sdk-kotlin
- **Sonatype Portal**: https://central.sonatype.com/
- **Issues**: https://github.com/idos-network/idos-sdk-kotlin/issues

## 📚 Documentation

- Publishing guide: [`PUBLISHING.md`](../PUBLISHING.md)
- README: [`README.md`](../README.md)
- Architecture: [`ARCHITECTURE.md`](../ARCHITECTURE.md)
