// swift-tools-version:5.9
// =============================================================================
// Consumer-facing SPM manifest for the Superwall KMP SDK.
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
        // drift apart ("Version pins, two layers").
        .package(url: "https://github.com/superwall/Superwall-iOS", exact: "4.16.1")
    ],
    targets: [
        // The prebuilt @objc bridge, attached to each GitHub release.
        //
        // DO NOT EDIT the url/checksum below by hand. The release job
        // (.github/workflows/release.yml, "Point Package.swift at the release
        // artifact") zips the freshly built SuperwallKMPBridge.xcframework,
        // computes its checksum with `swift package compute-checksum`, rewrites
        // both values here, and commits BEFORE tagging — SPM resolves this
        // manifest at the tag, so the tag has to already carry them. The klib
        // and this binary ship from the same tag; they are one release unit.
        //
        // Until the first release runs, these are placeholder values and this
        // package cannot resolve — build the bridge from source (bridge/) in the
        // meantime, as sample/iosApp does.
        .binaryTarget(
            name: "SuperwallKMPBridge",
            url: "https://github.com/superwall/Superwall-KMP/releases/download/0.1.1/SuperwallKMPBridge.xcframework.zip",
            checksum: "eec4f35203d8348c7dbf98535d8a67a37bf4f5709f9c395bace2e38046613e2f"
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
