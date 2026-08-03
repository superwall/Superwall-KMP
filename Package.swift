// swift-tools-version:5.9
// =============================================================================
// Consumer-facing SPM manifest for the Superwall KMP SDK (plan §5.2, §5.4).
//
// iOS apps that depend on com.superwall.sdk:superwall-kmp add THIS package
// (the repo URL) to get the prebuilt SuperwallKMPBridge.xcframework the Kotlin
// klib was cinterop-compiled against, plus SuperwallKit at the exact version
// the bridge was built with.
//
// This is deliberately distinct from bridge/Package.swift, which builds the
// bridge FROM SOURCE and is used only by our own development and release
// pipeline (bridge/scripts/build-xcframework.sh, bridge XCTests).
//
// Structure — the RevenueCat (PurchasesHybridCommon) wrapper pattern:
// an SPM `binaryTarget` cannot declare dependencies of its own, so a bare
// binary target would NOT pull SuperwallKit transitively and consumers would
// hit missing-framework link errors. We therefore wrap the binary target in a
// regular source target (`SuperwallKMPBridgeSPM`, containing only a stub file)
// that depends on BOTH the binary target AND SuperwallKit; the library product
// points at the wrapper, so adding the product gives consumers the bridge
// binary and SuperwallKit (and its Superscript/libcel dependency) transitively.
// =============================================================================
import PackageDescription

let package = Package(
    name: "SuperwallKMPBridge",
    platforms: [
        .iOS(.v14)
    ],
    products: [
        .library(
            name: "SuperwallKMPBridge",
            targets: ["SuperwallKMPBridgeSPM"]
        )
    ],
    dependencies: [
        // Exact pin — MUST stay in lockstep with bridge/Package.swift so the
        // binary artifact and the transitively-resolved SuperwallKit can never
        // drift apart (plan §5.2 "Version pins, two layers").
        .package(url: "https://github.com/superwall/Superwall-iOS", exact: "4.16.1")
    ],
    targets: [
        // The prebuilt @objc bridge, attached to each GitHub release.
        // TODO(release): url and checksum below are PLACEHOLDERS. The release
        // job (.github/workflows/release.yml) zips the freshly built
        // SuperwallKMPBridge.xcframework, computes its checksum via
        // `swift package compute-checksum`, attaches the zip to the GitHub
        // release for the tag, and rewrites these two values to the real
        // release-asset URL + checksum before committing. The klib and this
        // binary ship from the same tag — they are one release unit.
        .binaryTarget(
            name: "SuperwallKMPBridge",
            url: "https://github.com/superwall/Superwall-KMP/releases/download/v0.0.0-PLACEHOLDER/SuperwallKMPBridge.xcframework.zip",
            checksum: "0000000000000000000000000000000000000000000000000000000000000000"
        ),
        // Wrapper target: exists ONLY to carry the SuperwallKit dependency
        // alongside the binary (see header comment). Its single stub source
        // file satisfies SPM's requirement that regular targets have sources.
        .target(
            name: "SuperwallKMPBridgeSPM",
            dependencies: [
                .target(name: "SuperwallKMPBridge"),
                .product(name: "SuperwallKit", package: "Superwall-iOS")
            ],
            path: "Sources/SuperwallKMPBridgeSPM"
        )
    ]
)
