// swift-tools-version:5.9
import PackageDescription

let packageName = "IdosSDK"
let version = "0.0.4-test"

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
            checksum: "2bac874c2c34045c7eeac3b370237d8a2a64b01e67710a2ff248294c06fd7454"
        )
    ]
)
