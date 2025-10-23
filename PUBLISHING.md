# Publishing Guide

Complete guide for publishing the idOS SDK for Kotlin Multiplatform to Maven Central, GitHub Releases, and Swift Package Manager.

## Table of Contents

- [Quick Start](#quick-start)
- [Distribution Overview](#distribution-overview)
- [Initial Setup](#initial-setup)
- [Publishing a Release](#publishing-a-release)
- [Build Tasks Reference](#build-tasks-reference)
- [Troubleshooting](#troubleshooting)
- [Advanced Configuration](#advanced-configuration)

---

## Quick Start

### For Your First Release

1. **Complete initial setup** (one-time, see [Initial Setup](#initial-setup))
2. **Tag and push** (version is automatically derived from tag):
   ```bash
   git tag v0.1.0
   git push origin main
   git push origin v0.1.0
   ```
3. **Monitor**: Check https://github.com/idos-network/idos-sdk-kotlin/actions
4. **Verify**: Check releases and Maven Central

**Note**: Version is now **automatically derived from Git tags** via the `gradle-git-versioning` plugin. No need to manually update `gradle.properties`!

---

## Distribution Overview

The SDK publishes to multiple channels automatically:

| Channel | Artifacts | Triggered By | Consumer Access |
|---------|-----------|--------------|-----------------|
| **Maven Central** | Android AAR, JVM JAR, iOS Klib | Version tag (`v*.*.*`) | `implementation("org.idos:idos-sdk-kotlin:VERSION")` |
| **GitHub Releases** | AAR, XCFramework.zip | Version tag (`v*.*.*`) | Manual download |
| **Swift Package Manager** | XCFramework (binary) | Version tag (`v*.*.*`) | Xcode or Package.swift |

### What Gets Published

```
Release v0.1.0
│
├─ Maven Central (org.idos:idos-sdk-kotlin:0.1.0)
│  ├─ Android AAR
│  ├─ JVM JAR
│  ├─ iOS Klibs (arm64, simulator)
│  └─ Multiplatform metadata
│
├─ GitHub Releases
│  ├─ shared-release.aar
│  ├─ idos_sdk.xcframework.zip
│  └─ idos_sdk.xcframework.zip.sha256
│
└─ Swift Package Manager
   └─ Binary target → XCFramework from GitHub Releases
```

---

## Initial Setup

Complete these steps once before your first release.

### Step 1: Maven Central Account

- [ ] Visit [Sonatype Central Portal](https://central.sonatype.com/)
- [ ] Create an account
- [ ] Verify ownership of namespace `org.idos`
  - Click "Add Namespace"
  - Enter `org.idos`
  - Follow verification instructions (usually DNS or GitHub verification)
- [ ] Wait for approval (usually < 24 hours)

### Step 2: Generate GPG Key

```bash
# 1. Generate new GPG key
gpg --gen-key
# Enter your name and email when prompted

# 2. List keys and note the KEY_ID
gpg --list-secret-keys --keyid-format LONG
# Output shows: sec   rsa3072/YOUR_KEY_ID 2024-01-01

# 3. Export private key in ASCII-armored format
gpg --armor --export-secret-keys YOUR_KEY_ID > key.asc

# 4. Publish key to keyserver
gpg --keyserver hkps://keys.openpgp.org --send-keys YOUR_KEY_ID

# 5. Note your key password - you'll need it for GitHub Secrets
```

### Step 3: Configure GitHub Secrets

Navigate to: **Settings → Secrets and variables → Actions → New repository secret**

Add these 4 secrets:

| Secret Name | Value | How to Get |
|-------------|-------|------------|
| `MAVEN_CENTRAL_USERNAME` | Your Sonatype username | From Step 1 |
| `MAVEN_CENTRAL_PASSWORD` | Your Sonatype password | From Step 1 |
| `SIGNING_KEY` | Full GPG private key (ASCII-armored) | Content of `key.asc` from Step 2 |
| `SIGNING_PASSWORD` | GPG key password | Password you set in Step 2 |

**Important**:
- Copy the entire `key.asc` content including `-----BEGIN PGP PRIVATE KEY BLOCK-----` and `-----END PGP PRIVATE KEY BLOCK-----`
- Preserve all newlines and formatting
- Test by pasting into a text editor first to verify format
- `GITHUB_TOKEN` is automatically provided by GitHub Actions

### Step 4: Verify Setup

Test locally before creating a release:

```bash
# Lint checks
./gradlew ktlintCheck detekt

# Run tests
./gradlew jvmTest

# Test Maven publishing (publishes to ~/.m2/repository)
./gradlew publishToMavenLocal

# Verify local publication
ls ~/.m2/repository/org/idos/idos-sdk-kotlin/

# Build Android AAR
./gradlew :shared:assembleRelease

# Build iOS XCFramework
./gradlew :shared:assembleIdos_sdkXCFramework

# Verify outputs
ls shared/build/outputs/aar/shared-release.aar
ls shared/build/XCFrameworks/release/idos_sdk.xcframework
```

✅ **Setup Complete** when all commands succeed without errors.

---

## Publishing a Release

### Preparation

1. **Ensure clean state**:
   ```bash
   git status  # Should be clean
   ./gradlew clean
   ```

2. **Update changelog** (recommended):
   ```bash
   # Document changes in CHANGELOG.md or release notes
   ```

3. **Check current version** (optional):
   ```bash
   ./gradlew properties | grep "version:"
   # Shows version based on latest tag
   ```

### Release Process

```bash
# 1. Create and push tag (version is automatically derived from tag)
git tag v0.1.0
git push origin main
git push origin v0.1.0

# That's it! No manual version updates needed.
```

### Automated Workflow

Pushing the tag triggers `.github/workflows/release.yml`:

1. **Validate** (5-10 min)
   - Runs linting (ktlint, detekt)
   - Runs all platform tests (JVM, Android, iOS)

2. **Build and Publish** (10-15 min)
   - **Android**: Builds AAR, uploads to GitHub Releases, publishes to Maven Central
   - **iOS**: KMMBridge builds XCFramework, uploads to GitHub Release, updates Package.swift, and moves tag to include the updated Package.swift

**Total time**: ~15-25 minutes

**Note**: iOS publishing uses KMMBridge which automatically handles Package.swift updates and tag management to ensure Swift Package Manager works correctly.

### Monitor Progress

```bash
# Via GitHub CLI
gh run list --workflow=release.yml
gh run watch

# Or open in browser
open https://github.com/idos-network/idos-sdk-kotlin/actions
```

### Verification

After workflow completes:

- [ ] **GitHub Releases**: Check artifacts at https://github.com/idos-network/idos-sdk-kotlin/releases
  - [ ] `shared-release.aar` present
  - [ ] `idos_sdk.xcframework.zip` present
  - [ ] `idos_sdk.xcframework.zip.sha256` present

- [ ] **Maven Central** (10-30 min delay): https://central.sonatype.com/artifact/org.idos/idos-sdk-kotlin
  - [ ] Version appears
  - [ ] POM metadata correct

- [ ] **Package.swift**: Verify commit updating checksum

- [ ] **Test Integration**:

  **Android/JVM**:
  ```kotlin
  // In a test project's build.gradle.kts
  dependencies {
      implementation("org.idos:idos-sdk-kotlin:0.1.0")
  }
  ```

  **iOS (Xcode)**:
  - File → Add Package Dependencies
  - Enter: `https://github.com/idos-network/idos-sdk-kotlin`
  - Select version `0.1.0`

### Post-Release

**No action needed!** Development versions are automatically SNAPSHOT-based:

```bash
# On main branch without new tags, version is automatically:
# <latest-tag>-SNAPSHOT

# Example: After v0.1.0 release, main branch automatically becomes 0.1.0-SNAPSHOT
# When you tag v0.2.0, it automatically becomes 0.2.0-SNAPSHOT after

# You can verify current version anytime:
./gradlew properties | grep "version:"
```

---

## Build Tasks Reference

### Local Testing

```bash
# Publish to local Maven (~/.m2/repository)
./gradlew publishToMavenLocal

# Check what will be published
./gradlew generatePomFileForKotlinMultiplatformPublication
cat shared/build/publications/kotlinMultiplatform/pom-default.xml
```

### Android Build

```bash
# Build release AAR
./gradlew :shared:assembleRelease

# Output: shared/build/outputs/aar/shared-release.aar

# Build debug AAR
./gradlew :shared:assembleDebug
```

### iOS Build

```bash
# Build complete XCFramework (all architectures, debug + release)
./gradlew :shared:assembleIdos_sdkXCFramework

# Build release only
./gradlew :shared:assembleIdos_sdkReleaseXCFramework

# Build debug only
./gradlew :shared:assembleIdos_sdkDebugXCFramework

# Output: shared/build/XCFrameworks/release/idos_sdk.xcframework

# Manually zip for distribution
cd shared/build/XCFrameworks/release
zip -r idos_sdk.xcframework.zip idos_sdk.xcframework
shasum -a 256 idos_sdk.xcframework.zip
```

### Platform-Specific

```bash
# iOS device framework
./gradlew :shared:linkReleaseFrameworkIosArm64

# iOS simulator framework
./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64

# JVM JAR
./gradlew :shared:jvmJar

# Run tests
./gradlew jvmTest                      # JVM
./gradlew :shared:testDebugUnitTest    # Android
./gradlew :shared:iosSimulatorArm64Test # iOS
```

### Publishing

```bash
# List all publishing tasks
./gradlew tasks --group=publishing

# Publish to Maven Central (requires secrets)
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache

# Publish specific target
./gradlew publishAndroidReleasePublicationToMavenCentralRepository
./gradlew publishJvmPublicationToMavenCentralRepository
```

---

## Troubleshooting

### Build Errors

#### "Invalid POM" error
**Problem**: Maven Central rejects POM file

**Solution**: Verify all required fields in `shared/build.gradle.kts`:
- name, description, url
- licenses, developers, scm

#### "Signing failed"
**Problem**: GPG signing error

**Solutions**:
```bash
# Verify key exists locally
gpg --list-secret-keys

# Verify secret format
echo $SIGNING_KEY | head -1  # Should show -----BEGIN PGP PRIVATE KEY BLOCK-----

# Re-export key
gpg --armor --export-secret-keys YOUR_KEY_ID > key.asc

# Copy entire content (including BEGIN/END lines) to GitHub Secret and retry
```

**Common signing issues:**
- Missing BEGIN/END headers → Ensure you copied the entire key.asc content
- Truncated key → Verify all lines are present in the GitHub Secret
- Wrong key ID → Verify with `gpg --list-secret-keys --keyid-format LONG`

#### "Namespace not verified"
**Problem**: Can't publish to `org.idos`

**Solution**: Complete namespace verification in Sonatype Central Portal

### XCFramework Errors

#### "Module not found" in Xcode
**Solutions**:
- Ensure framework `baseName` matches XCFramework name (`idos_sdk`)
- Clean Xcode: Cmd+Shift+K
- Delete derived data: `rm -rf ~/Library/Developer/Xcode/DerivedData`

#### Checksum mismatch
**Solutions**:
```bash
# Recalculate checksum
shasum -a 256 idos_sdk.xcframework.zip

# Update Package.swift with correct checksum
vim Package.swift
```

### GitHub Actions Errors

#### Workflow not triggering
**Solutions**:
- Verify tag format: `v*.*.*` (must start with `v`)
- Check tag is pushed: `git ls-remote --tags origin`
- Re-push tag: `git push origin v0.1.0 --force`

#### Secrets not available
**Solutions**:
- Verify secrets exist: Settings → Secrets → Actions
- Check secret names match exactly (case-sensitive)
- Re-create secrets if needed

### Maven Central Issues

#### "401 Unauthorized"
**Solution**: Verify `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` are correct

#### "403 Forbidden"
**Solution**: Ensure namespace `org.idos` is verified in your account

#### Publication not appearing
**Solution**: Wait 10-30 minutes, then check https://central.sonatype.com/

### Gradle Issues

```bash
# Clear all caches
./gradlew clean
./gradlew --stop
rm -rf ~/.gradle/caches/
./gradlew build --refresh-dependencies

# Clear configuration cache
rm -rf .gradle/configuration-cache/
./gradlew build --no-configuration-cache
```

---

## Advanced Configuration

### Custom Publishing

For manual control over the entire process:

```bash
# 1. Build artifacts
./gradlew :shared:assembleRelease
./gradlew :shared:assembleIdos_sdkReleaseXCFramework

# 2. Publish to Maven Central
export ORG_GRADLE_PROJECT_mavenCentralUsername="username"
export ORG_GRADLE_PROJECT_mavenCentralPassword="password"
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat key.asc)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="password"
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache

# 3. Create GitHub Release manually
gh release create v0.1.0 \
  shared/build/outputs/aar/shared-release.aar \
  shared/build/XCFrameworks/release/idos_sdk.xcframework.zip

# 4. Update Package.swift
# Calculate checksum and update manually
```

---

## Version Management

### Automatic Versioning from Git Tags

**The SDK uses automatic versioning** via the `gradle-git-versioning` plugin. Version is derived from Git tags and branches:

| Git State | Version Format | Example |
|-----------|----------------|---------|
| On tag `v1.2.3` | `1.2.3` | Exact version from tag |
| On `main` branch | `<latest-tag>-SNAPSHOT` | `0.1.0-SNAPSHOT` |
| On `develop` branch | `<latest-tag>-SNAPSHOT` | `0.1.0-SNAPSHOT` |
| On `feature/foo` branch | `<latest-tag>-feature/foo-SNAPSHOT` | `0.1.0-feature/foo-SNAPSHOT` |
| Detached HEAD (CI) | `<latest-tag>-<commit-short>` | `0.1.0-a1b2c3d` |

**Configuration**: See `build.gradle.kts` (root) for versioning rules.

### Semantic Versioning

Follow [semver.org](https://semver.org/) when creating tags:

- **MAJOR** (v1.0.0) - Breaking changes
- **MINOR** (v0.2.0) - New features, backward compatible
- **PATCH** (v0.1.1) - Bug fixes, backward compatible

### Release Types

**Stable Release**:
```bash
git tag v0.1.0  # Version automatically becomes 0.1.0
```

**Pre-release** (use tag naming):
```bash
git tag v1.0.0-alpha    # Version automatically becomes 1.0.0-alpha
git tag v1.0.0-beta.1   # Version automatically becomes 1.0.0-beta.1
git tag v1.0.0-rc.1     # Version automatically becomes 1.0.0-rc.1
```

**Check Current Version**:
```bash
./gradlew properties | grep "version:"
```

---

## Quick Reference

### Essential Commands

```bash
# Test locally
./gradlew publishToMavenLocal

# Build for release
./gradlew :shared:assembleRelease
./gradlew :shared:assembleIdos_sdkReleaseXCFramework

# Create release
git tag v0.1.0 && git push origin v0.1.0

# Monitor
gh run watch
```

### Configuration Files

- `build.gradle.kts` (root) - Automatic versioning configuration
- `gradle.properties` - Group and artifact metadata (no version)
- `shared/build.gradle.kts` - Build and publishing config
- `.github/workflows/release.yml` - Release automation
- `Package.swift` - Swift Package Manager manifest

### Important URLs

- **Releases**: https://github.com/idos-network/idos-sdk-kotlin/releases
- **Actions**: https://github.com/idos-network/idos-sdk-kotlin/actions
- **Maven Central**: https://central.sonatype.com/artifact/org.idos/idos-sdk-kotlin
- **Sonatype Portal**: https://central.sonatype.com/

### Support

- Full command reference: `.github/PUBLISHING_REFERENCE.md`
- Issues: https://github.com/idos-network/idos-sdk-kotlin/issues
- Vanniktech Plugin: https://vanniktech.github.io/gradle-maven-publish-plugin/
- KMMBridge: https://kmmbridge.touchlab.co/

---

## Success Checklist

Your publishing setup is complete when:

- ✅ Maven Central namespace verified
- ✅ GPG key generated and published
- ✅ GitHub Secrets configured
- ✅ Local builds succeed
- ✅ Version tag triggers workflow
- ✅ AAR publishes to Maven Central
- ✅ XCFramework publishes to GitHub Releases
- ✅ Package.swift updates automatically
- ✅ Gradle projects can fetch dependency
- ✅ Xcode can resolve SPM package
