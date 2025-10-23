// swift-tools-version:5.9
import PackageDescription

let packageName = "IdosSDK"
let version = "0.0.10"

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
            url: "https://github.com/idos-network/idos-sdk-kotlin/releases/download/v0.0.10/idos_sdk.xcframework.zip",
            checksum: "43fc07e6945fb14a2b58ee7c036467f94de6c2cfc2726121697897ece76d0331"
        )
    ]
)
