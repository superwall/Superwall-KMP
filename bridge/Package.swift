// swift-tools-version:5.9
// SuperwallKMPBridge — the self-authored @objc Swift bridge for the Superwall KMP SDK.
// See.agents/AGENTS.md (iOS bridge). The public surface of this package is 100% @objc
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
      // Dynamic so `xcodebuild archive` installs a real.framework into the
      // archive's Products/Library/Frameworks (static/automatic products
      // install nothing there, breaking the XCFramework build).
      type: .dynamic,
      targets: ["SuperwallKMPBridge"]
    )
  ],
  dependencies: [
    // Exact pin — consumers must not be able to drift SuperwallKit independently
    // of the bridge.
    .package(url: "https://github.com/superwall/Superwall-iOS", exact: "4.16.1")
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
