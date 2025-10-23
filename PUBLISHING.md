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
2. **Trigger manual release** from GitHub UI:
   - Go to **Actions** → **Release** workflow
   - Click **Run workflow**
   - Select branch: `main`
   - Enter version: `0.0.9` (without `v` prefix)
   - Click **Run workflow**
3. **Monitor**: Check https://github.com/idos-network/idos-sdk-kotlin/actions
4. **Verify**: Check releases and Maven Central

**Note**: Version is managed in `gradle.properties` and updated automatically by the workflow.

---

## Distribution Overview

The SDK publishes to multiple channels automatically:

| Channel | Artifacts | Triggered By | Consumer Access |
|---------|-----------|--------------|-----------------|
| **Maven Central** | Android AAR, JVM JAR, iOS Klib | Manual workflow dispatch | `implementation("org.idos:idos-sdk-kotlin:VERSION")` |
| **GitHub Releases** | AAR, XCFramework.zip | Manual workflow dispatch | Manual download |
| **Swift Package Manager** | XCFramework (binary) | Manual workflow dispatch | Xcode or Package.swift |

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

1. **Ensure main branch is ready**:
   ```bash
   git checkout main
   git pull origin main
   git status  # Should be clean
   ```

2. **Update changelog** (recommended):
   ```bash
   # Document changes in CHANGELOG.md or release notes
   # Commit changes to main before releasing
   ```

3. **Check current version**:
   ```bash
   ./gradlew properties | grep "version:"
   # Shows current version from gradle.properties
   ```

### Release Process

1. **Go to GitHub Actions**:
   - Navigate to: https://github.com/idos-network/idos-sdk-kotlin/actions
   - Select **Release** workflow from the left sidebar

2. **Run workflow**:
   - Click **Run workflow** button
   - Ensure **Use workflow from** is set to `main` branch
   - Enter version in format: `0.0.9` or `0.0.9-rc1` (without `v` prefix)
   - Click **Run workflow**

3. **Version validation**:
   - Workflow validates semantic versioning format
   - Checks tag `v0.0.9` doesn't already exist
   - Fails fast if validation fails

### Automated Workflow

The manual release triggers `.github/workflows/release.yml`:

1. **Validate** (< 1 min)
   - Validates semver format
   - Checks tag availability
   - Ensures running on main branch

2. **Build and Test** (10-15 min)
   - Runs linting (ktlint, detekt)
   - Runs all platform tests (JVM, Android, iOS)
   - Builds AAR and XCFramework

3. **Release** (5-10 min)
   - Updates `gradle.properties` and `Package.swift`
   - Commits to main with version bump
   - Creates and pushes tag `v0.0.9`
   - Publishes to Maven Central
   - Creates GitHub Release with artifacts

**Total time**: ~15-25 minutes

**Note**: The workflow runs on the main branch for optimal cache performance. Version files are updated, committed, and tagged automatically.

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

**After a successful release**:

1. The version in `gradle.properties` is updated to the released version (e.g., `0.0.9`)
2. The tag `v0.0.9` is created and pushed
3. GitHub Release is created with artifacts

**For next release**:
- Simply run the workflow again with the next version (e.g., `0.1.0`)
- No manual file updates needed - workflow handles everything

**Check current version**:
```bash
./gradlew properties | grep "version:"
# Or:
grep "^VERSION=" gradle.properties
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

### Manual Versioning

**The SDK uses manual versioning** stored in `gradle.properties`. The release workflow automatically updates this file and creates the corresponding git tag.

**How it works**:
1. Version stored in `gradle.properties`: `VERSION=0.0.8`
2. Workflow input: `0.0.9`
3. Workflow updates `gradle.properties` → `VERSION=0.0.9`
4. Workflow commits and creates tag `v0.0.9`
5. Maven Central receives version `0.0.9`

**Configuration**: See `gradle.properties` for current version.

### Semantic Versioning

Follow [semver.org](https://semver.org/) when releasing:

- **MAJOR** (1.0.0) - Breaking changes
- **MINOR** (0.2.0) - New features, backward compatible
- **PATCH** (0.1.1) - Bug fixes, backward compatible

### Release Types

**Stable Release**:
```
Workflow input: 0.1.0
Creates tag: v0.1.0
Maven version: 0.1.0
```

**Pre-release**:
```
Workflow input: 1.0.0-alpha
Creates tag: v1.0.0-alpha
Maven version: 1.0.0-alpha

Workflow input: 1.0.0-rc.1
Creates tag: v1.0.0-rc.1
Maven version: 1.0.0-rc.1
```

**Check Current Version**:
```bash
./gradlew properties | grep "version:"
# Or directly:
grep "^VERSION=" gradle.properties
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

# Create release (via GitHub UI)
# Actions → Release → Run workflow → Enter version

# Monitor
gh run watch
```

### Configuration Files

- `build.gradle.kts` (root) - Version property configuration
- `gradle.properties` - VERSION, group, and artifact metadata
- `shared/build.gradle.kts` - Build and publishing config
- `.github/workflows/release.yml` - Manual release workflow
- `Package.swift` - Swift Package Manager manifest (auto-updated)

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
