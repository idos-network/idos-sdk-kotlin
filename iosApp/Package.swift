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
        .package(url: "https://github.com/krzyzanowskim/CryptoSwift.git", from: "1.8.0"),

        // web3.swift - Ethereum and Web3 functionality (BIP39, BIP32, secp256k1)
        .package(url: "https://github.com/argentlabs/web3.swift", from: "1.6.0")
    ],
    targets: [
        .target(
            name: "iosApp",
            dependencies: [
                .product(name: "Sodium", package: "swift-sodium"),
                .product(name: "CryptoSwift", package: "CryptoSwift"),
                .product(name: "web3.swift", package: "web3.swift")
            ],
            path: "iosApp"
        )
    ]
)