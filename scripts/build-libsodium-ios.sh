#!/bin/bash

# Build libsodium XCFramework for iOS
# This script downloads libsodium source and builds an XCFramework
# that includes all Apple platforms (iOS device, iOS simulator, macOS, etc.)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LIBS_DIR="$PROJECT_ROOT/shared/libs"
LIBSODIUM_VERSION="${LIBSODIUM_VERSION:-1.0.20}"
WORK_DIR="$PROJECT_ROOT/build/libsodium-build"

echo "Building libsodium $LIBSODIUM_VERSION XCFramework..."
echo "Work directory: $WORK_DIR"

# Clean and create work directory
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

# Cleanup function - check for errors first
cleanup() {
    if [ -f "$WORK_DIR/libsodium-${LIBSODIUM_VERSION}-RELEASE/libsodium-apple/tmp/build_log" ]; then
        echo ""
        echo "=== Build log found, showing last 50 lines ==="
        tail -50 "$WORK_DIR/libsodium-${LIBSODIUM_VERSION}-RELEASE/libsodium-apple/tmp/build_log"
        echo "=== End of build log ==="
        echo ""
    fi
    echo "Cleaning up work directory..."
    rm -rf "$WORK_DIR"
}
trap cleanup EXIT

cd "$WORK_DIR"

# Download libsodium source
echo "Downloading libsodium source..."
curl -L "https://github.com/jedisct1/libsodium/archive/refs/tags/${LIBSODIUM_VERSION}-RELEASE.tar.gz" -o libsodium.tar.gz
tar xzf libsodium.tar.gz
cd "libsodium-${LIBSODIUM_VERSION}-RELEASE"

# Modify apple-xcframework.sh to only build iOS device and simulator
echo "Modifying apple-xcframework.sh to only build iOS targets..."
XCFRAMEWORK_SCRIPT="dist-build/apple-xcframework.sh"

# Match the actual pattern: build_platform >"$LOG_FILE" 2>&1 || exit 1
# Replace unwanted builds with a no-op that preserves the log redirection
# Also prevent I386_SIMULATOR_SUPPORTED and VISIONOS_SUPPORTED from being set to true
# Delete bundling blocks for unwanted platforms (from echo "Bundling..." to done)
# Modify loops to only process iOS targets
# Remove --enable-minimal flag to get full libsodium with scrypt support
gsed -i.bak \
    -e '/I386_SIMULATOR_SUPPORTED=true/d' \
    -e '/IOS32_SUPPORTED=true/d' \
    -e '/VISIONOS_SUPPORTED=true/d' \
    -e 's/build_macos >"$LOG_FILE" 2>&1 || exit 1/: # Skipped macOS/' \
    -e 's/build_watchos >"$LOG_FILE" 2>&1 || exit 1/: # Skipped watchOS/' \
    -e 's/build_watchos_simulator >"$LOG_FILE" 2>&1 || exit 1/: # Skipped watchOS/' \
    -e 's/build_tvos >"$LOG_FILE" 2>&1 || exit 1/: # Skipped tvOS/' \
    -e 's/build_tvos_simulator >"$LOG_FILE" 2>&1 || exit 1/: # Skipped tvOS/' \
    -e 's/build_visionos >"$LOG_FILE" 2>&1 || exit 1/: # Skipped visionOS/' \
    -e 's/build_visionos_simulator >"$LOG_FILE" 2>&1 || exit 1/: # Skipped visionOS/' \
    -e 's/build_catalyst >"$LOG_FILE" 2>&1 || exit 1/: # Skipped Catalyst/' \
    -e '/^[[:space:]]*echo "Bundling macOS.*"/,/^[[:space:]]*done$/c\  : # Skipped macOS bundling' \
    -e '/^[[:space:]]*echo "Bundling watchOS.*"/,/^[[:space:]]*done$/c\  : # Skipped watchOS bundling' \
    -e '/^[[:space:]]*echo "Bundling tvOS.*"/,/^[[:space:]]*done$/c\  : # Skipped tvOS bundling' \
    -e '/^[[:space:]]*echo "Bundling visionOS.*"/,/^[[:space:]]*done$/c\  : # Skipped visionOS bundling' \
    -e '/^[[:space:]]*echo "Bundling Catalyst.*"/,/^[[:space:]]*done$/c\  : # Skipped Catalyst bundling' \
    -e 's/for f in macos ios watchos tvos visionos catalyst; do/for f in ios; do/' \
    -e 's/for f in ios-simulators watchos-simulators tvos-simulators visionos-simulators; do/for f in ios-simulators; do/' \
    -e 's/--enable-minimal/--disable-minimal/g' \
    "$XCFRAMEWORK_SCRIPT"

echo "Modified script to build only:"
echo "  - iOS device (arm64)"
echo "  - iOS simulator (arm64)"

# Build XCFramework using modified script
echo "Building XCFramework (this may take several minutes)..."
if ! ./dist-build/apple-xcframework.sh; then
    echo ""
    echo "Error: XCFramework build failed!"
    exit 1
fi

# Verify the XCFramework was created
if [ ! -d "libsodium-apple/Clibsodium.xcframework" ]; then
    echo "Error: XCFramework not found at expected location"
    exit 1
fi

# Create libs directory if it doesn't exist
mkdir -p "$LIBS_DIR"

# Copy the built XCFramework
echo "Copying XCFramework to $LIBS_DIR..."
rm -rf "$LIBS_DIR/libsodium.xcframework"
cp -R "libsodium-apple/Clibsodium.xcframework" "$LIBS_DIR/libsodium.xcframework"

echo "✓ libsodium XCFramework built successfully!"
echo "Location: $LIBS_DIR/libsodium.xcframework"

# Show framework info
echo ""
echo "Framework contents:"
ls -la "$LIBS_DIR/libsodium.xcframework"
