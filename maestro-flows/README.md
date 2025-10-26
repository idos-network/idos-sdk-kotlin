# Maestro Tests

## Setup

```bash
# Install Maestro
brew install maestro

# Install Task
brew install go-task/tap/go-task
```

## Usage

```bash
# Create .env with test credentials
cat > .env <<EOF
PASSWORD="your test password"
EOF

# Run Android tests (both USER and MPC enclave)
task maestro:android

# Run iOS tests (both USER and MPC enclave)
task maestro:ios

# Run both platforms
task maestro:all

# Run specific enclave type
task maestro:android:user
task maestro:android:mpc
task maestro:ios:user
task maestro:ios:mpc
```

## Environment Variables

The test flow uses these environment variables:

- `APP_ID` - Application package/bundle ID (set automatically by Taskfile)
- `DERIVATION_PATH` - Wallet derivation path (set automatically: `m/44'/60'/0'/0/4` for USER, `m/44'/60'/0'/0/3` for MPC)
- `PASSWORD` - Password for USER enclave (required in `.env`)

## Manual Execution

```bash
# Android - USER enclave
./gradlew :androidApp:assembleDebug
maestro test \
  --env APP_ID="org.idos.app" \
  --env DERIVATION_PATH="m/44'/60'/0'/0/4" \
  --env PASSWORD="..." \
  maestro-flows/

# Android - MPC enclave
maestro test \
  --env APP_ID="org.idos.app" \
  --env DERIVATION_PATH="m/44'/60'/0'/0/3" \
  --env PASSWORD="..." \
  maestro-flows/

# iOS - USER enclave
cd iosApp && xcodebuild build -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16'
xcrun simctl boot "iPhone 16"
xcrun simctl install booted iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app
maestro test \
  --env APP_ID="org.idos.app" \
  --env DERIVATION_PATH="m/44'/60'/0'/0/4" \
  --env PASSWORD="..." \
  maestro-flows/

# iOS - MPC enclave
maestro test \
  --env APP_ID="org.idos.app" \
  --env DERIVATION_PATH="m/44'/60'/0'/0/3" \
  --env PASSWORD="..." \
  maestro-flows/
```

## Test Flow

The single `decrypt-enclave-flow.yaml` handles both USER and MPC enclaves:

1. Imports wallet with derivation path from `DERIVATION_PATH` env var
2. Opens first credential
3. Taps "Decrypt Content" button
4. If "Generate Encryption Key" dialog appears → USER enclave (enters password)
5. If "Unlock MPC Enclave" dialog appears → MPC enclave (no password needed)
6. Verifies decrypted content contains `@context`
