#!/bin/bash

# iOS App Build Script
# This script builds the shared KMM framework for iOS

set -e

echo "🔨 Building shared KMM framework for iOS..."

cd ..
./gradlew :shared:embedAndSignAppleFrameworkForXcode

echo "✅ Build complete!"
echo "You can now open iosApp.xcodeproj in Xcode"