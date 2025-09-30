// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "iosApp",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "iosApp",
            targets: ["iosApp"]
        )
    ],
    dependencies: [
        // SwiftSodium - libsodium wrapper for NaCl Box encryption
        .package(url: "https://github.com/jedisct1/swift-sodium.git", from: "0.9.1"),

        // CryptoSwift - Pure Swift crypto including SCrypt
        .package(url: "https://github.com/krzyzanowskim/CryptoSwift.git", from: "1.8.0")
    ],
    targets: [
        .target(
            name: "iosApp",
            dependencies: [
                .product(name: "Sodium", package: "swift-sodium"),
                .product(name: "CryptoSwift", package: "CryptoSwift")
            ],
            path: "iosApp"
        )
    ]
)