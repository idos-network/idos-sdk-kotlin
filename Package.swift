// swift-tools-version:5.9
import PackageDescription

let packageName = "IdosSDK"
let version = "0.0.5"

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
            checksum: "f2e50c7f130fd7a577a19300b76a42db8f2ce519a753bcb206be5ccbe3553f44"
        )
    ]
)
