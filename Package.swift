// swift-tools-version:5.9
import PackageDescription

let packageName = "IdosSDK"
let version = "0.0.7"

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
            checksum: "81e36e73947f2d0c5419dcb165f63a3714bb56a38ed357bc034ce0d671279690"
        )
    ]
)
