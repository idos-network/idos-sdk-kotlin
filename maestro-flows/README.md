# Maestro Tests

This directory contains Maestro UI test flows for both Android and iOS sample apps.

## Test Files

- `user-flow.yaml` - Tests USER enclave functionality
- `mpc-flow.yaml` - Tests MPC enclave functionality

All `.yaml` files in this directory are automatically run by CI.

## Setup

```bash
# Install Maestro
brew install maestro

# Install Task
brew install go-task/tap/go-task
```

## Usage

```bash
# Create .env with test password
cat > .env <<EOF
PASSWORD="your test password"
EOF

# Run all tests on Android
task maestro:android

# Run all tests on iOS
task maestro:ios

# Run all tests on both platforms
task maestro:all
```

## Manual Execution

### Android

```bash
# Build app
./gradlew :androidApp:assembleDebug

# Start emulator (if needed)
# AVD will be auto-detected from installed emulators

# Run all flows
maestro test \
  --env APP_ID="org.idos.app" \
  --env PASSWORD="..." \
  maestro-flows/
```

### iOS

```bash
# Build app
cd iosApp && xcodebuild build \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  -derivedDataPath build

# Boot simulator and install app
xcrun simctl boot "iPhone 16"
xcrun simctl install booted iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app

# Run all flows
maestro test \
  --env APP_ID="org.idos.app" \
  --env PASSWORD="..." \
  maestro-flows/
```

## Environment Variables

Tests use these environment variables:

- `APP_ID` - Application package/bundle ID (set automatically by Task or manually)
- `PASSWORD` - Password for USER enclave (required in `.env`)

Each test file has its own hardcoded derivation path for the specific enclave type it tests.

## CI Integration

GitHub Actions automatically:
1. Builds debug apps
2. Runs all test flows in this directory
3. Reports results for both Android and iOS

No need to filter by enclave type - just add new `.yaml` files and they'll be picked up automatically.
