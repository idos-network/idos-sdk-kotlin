#!/bin/bash

# iOS App Setup Script
# This script sets up the iOS development environment for the IDOS SDK

set -e

echo "🚀 IDOS iOS App Setup"
echo "====================="
echo ""

# Check if we're in the right directory
if [ ! -f "Podfile" ]; then
    echo "❌ Error: Podfile not found. Please run this script from the iosApp directory."
    exit 1
fi

# Check for required tools
echo "📋 Checking prerequisites..."

# Check for Xcode
if ! command -v xcodebuild &> /dev/null; then
    echo "❌ Xcode not found. Please install Xcode from the App Store."
    exit 1
fi
echo "✅ Xcode found: $(xcodebuild -version | head -n 1)"

# Check for CocoaPods
if ! command -v pod &> /dev/null; then
    echo "⚠️  CocoaPods not found. Installing..."
    sudo gem install cocoapods
fi
echo "✅ CocoaPods found: $(pod --version)"

# Check for Gradle
if [ ! -f "../gradlew" ]; then
    echo "❌ Gradle wrapper not found. Please run this script from the iosApp directory."
    exit 1
fi
echo "✅ Gradle wrapper found"

# Step 1: Install CocoaPods dependencies
echo ""
echo "📦 Step 1: Installing CocoaPods dependencies..."
echo "   This will install SwiftSodium and CryptoSwift..."
pod install
echo "✅ CocoaPods dependencies installed"

# Step 2: Build KMM shared framework
echo ""
echo "🔨 Step 2: Building KMM shared framework..."
cd ..
./gradlew :shared:embedAndSignAppleFrameworkForXcode
cd iosApp
echo "✅ Shared framework built"

# Step 3: Verify project structure
echo ""
echo "🔍 Step 3: Verifying project structure..."

if [ ! -d "iosApp.xcworkspace" ]; then
    echo "❌ Workspace not created. CocoaPods installation may have failed."
    exit 1
fi
echo "✅ Xcode workspace created"

if [ ! -d "../shared/build/xcode-frameworks" ]; then
    echo "⚠️  Warning: Shared framework not found. You may need to run the build again."
else
    echo "✅ Shared framework found"
fi

# Success message
echo ""
echo "✨ Setup complete!"
echo ""
echo "📝 Next steps:"
echo "   1. Open the workspace:"
echo "      open iosApp.xcworkspace"
echo ""
echo "   2. Review crypto integration guide:"
echo "      cat CRYPTO_INTEGRATION.md"
echo ""
echo "   3. Check implementation status:"
echo "      cat IMPLEMENTATION_STATUS.md"
echo ""
echo "   4. Complete crypto bridge integration:"
echo "      - Update shared/src/iosMain/kotlin/enclave/Encryption.ios.kt"
echo "      - Update shared/src/iosMain/kotlin/enclave/KeyDerivation.ios.kt"
echo "      - See CRYPTO_INTEGRATION.md for details"
echo ""
echo "   5. Build and run in Xcode:"
echo "      - Select iosApp scheme"
echo "      - Choose iOS simulator"
echo "      - Press Cmd+R"
echo ""
echo "🎉 Happy coding!"