// swift-tools-version:5.9
// SuperwallKMPBridge — the self-authored @objc Swift bridge for the Superwall KMP SDK.
// See docs/IMPLEMENTATION_PLAN.md §5. The public surface of this package is 100% @objc
// (SWB-prefixed ObjC names); Kotlin/Native cinterops against the generated
// SuperwallKMPBridge-Swift.h from a prebuilt XCFramework.
import PackageDescription

let package = Package(
  name: "SuperwallKMPBridge",
  platforms: [
    .iOS(.v14)
  ],
  products: [
    .library(
      name: "SuperwallKMPBridge",
      targets: ["SuperwallKMPBridge"]
    )
  ],
  dependencies: [
    // Exact pin — consumers must not be able to drift SuperwallKit independently
    // of the bridge (plan §5.2, Open Question #7).
    .package(url: "https://github.com/superwall/Superwall-iOS", exact: "4.16.2")
  ],
  targets: [
    .target(
      name: "SuperwallKMPBridge",
      dependencies: [
        .product(name: "SuperwallKit", package: "Superwall-iOS")
      ],
      path: "Sources/SuperwallKMPBridge"
    ),
    .testTarget(
      name: "SuperwallKMPBridgeTests",
      dependencies: ["SuperwallKMPBridge"],
      path: "Tests/SuperwallKMPBridgeTests"
    )
  ]
)
