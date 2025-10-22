// swift-tools-version:5.9
import PackageDescription

let packageName = "IdosSDK"
let version = "0.1.0"

let package = Package(
    name: packageName,
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: packageName,
            targets: [packageName]
        )
    ],
    targets: [
        .binaryTarget(
            name: packageName,
            url: "https://github.com/idos-network/idos-sdk-kotlin/releases/download/\(version)/idos_sdk.xcframework.zip",
            checksum: "CHECKSUM_PLACEHOLDER"
        )
    ]
)
