// swift-tools-version:5.9
import PackageDescription

let packageName = "IdosSDK"
let version = "0.0.8"

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
            url: "https://github.com/idos-network/idos-sdk-kotlin/releases/download/v0.0.8/idos_sdk.xcframework.zip",
            checksum: "e42f112b7a3b68aa9e4461e8bcd4ab3f4365d7664fb0999e8f60ffde7d8d46ec"
        )
    ]
)
