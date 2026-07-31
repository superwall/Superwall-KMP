import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.superwall.sdk"
version = "0.1.0"

// ---------------------------------------------------------------------------
// SuperwallKMPBridge XCFramework location (plan §5.2).
//
// iosMain cinterops against the prebuilt @objc Swift bridge. The XCFramework
// directory resolves from, in order:
//   1. the SUPERWALL_BRIDGE_XCFRAMEWORK Gradle property (-P) or environment
//      variable — points a build at a prebuilt/downloaded framework;
//   2. the local bridge build output, bridge/build/SuperwallKMPBridge.xcframework
//      (produced by the buildBridgeXCFramework task / bridge/scripts/build-xcframework.sh).
//
// Everything here must stay configuration-safe on non-macOS hosts: the iOS
// cinterop/compile tasks never execute on Linux, so a missing XCFramework must
// not break configuration, `./gradlew help`, or the Android/metadata builds.
// ---------------------------------------------------------------------------
val isMacOsHost: Boolean = org.gradle.internal.os.OperatingSystem.current().isMacOsX

val bridgeFrameworkDir: java.io.File =
    providers.gradleProperty("SUPERWALL_BRIDGE_XCFRAMEWORK")
        .orElse(providers.environmentVariable("SUPERWALL_BRIDGE_XCFRAMEWORK"))
        .orNull
        ?.let { rootProject.file(it) } // absolute paths pass through; relative resolve against the repo root
        ?: rootProject.file("bridge/build/SuperwallKMPBridge.xcframework")

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "com.superwall.sdk.kmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Publish the R8 keep rule for SuperwallInitializer into the AAR so
        // consumers' minified builds keep the reflectively-discovered
        // androidx.startup initializer.
        optimization {
            consumerKeepRules.file(layout.projectDirectory.file("src/androidMain/consumer-rules.pro"))
            consumerKeepRules.publish = true
        }

        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    // Cinterop against the prebuilt SuperwallKMPBridge.xcframework (plan §5.2):
    // the device target uses the ios-arm64 slice; both simulator targets share
    // the fat ios-arm64_x86_64-simulator slice. -F must point at the directory
    // that CONTAINS SuperwallKMPBridge.framework, i.e. the slice directory.
    listOf(
        iosArm64() to "ios-arm64",
        iosSimulatorArm64() to "ios-arm64_x86_64-simulator",
        iosX64() to "ios-arm64_x86_64-simulator",
    ).forEach { (target, slice) ->
        target.compilations.getByName("main").cinterops.create("SuperwallKMPBridge") {
            definitionFile.set(layout.projectDirectory.file("nativeInterop/cinterop/SuperwallKMPBridge.def"))
            compilerOpts(
                // `modules =` in the .def requires Clang modules enabled.
                "-fmodules",
                "-F${bridgeFrameworkDir.resolve(slice).absolutePath}",
                "-framework",
                "SuperwallKMPBridge",
            )
        }
    }

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.superwall.android)
            implementation(libs.androidx.startup)
            implementation(libs.kotlinx.coroutines.android)
            // superwall-android's POM declares these as runtime scope, so they are
            // on consumers' runtime classpath transitively but NOT on our compile
            // classpath. The native API surface we adapt against exposes them
            // (PurchaseController.purchase takes a billing ProductDetails; the
            // redemption models carry kotlinx.serialization JsonElement), so pin
            // them compileOnly at the exact versions superwall-android 2.8.0 uses.
            compileOnly(libs.android.billing)
            compileOnly(libs.kotlinx.serialization.json)
        }
    }
}

// ---------------------------------------------------------------------------
// Bridge XCFramework build task (plan §5.2).
//
// Builds bridge/build/SuperwallKMPBridge.xcframework from bridge/Package.swift
// via xcodebuild. Requires macOS + Xcode; onlyIf-skipped everywhere else so it
// stays registerable (and `./gradlew help` keeps working) on Linux CI.
// ---------------------------------------------------------------------------
val buildBridgeXCFramework: TaskProvider<Exec> = tasks.register<Exec>("buildBridgeXCFramework") {
    group = "bridge"
    description =
        "Builds bridge/build/SuperwallKMPBridge.xcframework via bridge/scripts/build-xcframework.sh (macOS + Xcode only)."
    workingDir(rootProject.layout.projectDirectory)
    commandLine(
        "bash",
        rootProject.layout.projectDirectory.file("bridge/scripts/build-xcframework.sh").asFile.absolutePath,
    )
    onlyIf("XCFrameworks can only be built on a macOS host") { isMacOsHost }
    outputs.dir(rootProject.layout.projectDirectory.dir("bridge/build/SuperwallKMPBridge.xcframework"))
}

// Auto-build the bridge before cinterop, but ONLY when (a) this is a macOS host
// (the only place cinterop for iOS targets can actually run) and (b) the resolved
// XCFramework directory is not already present — e.g. a cached CI download passed
// via SUPERWALL_BRIDGE_XCFRAMEWORK, or a previous local build. On Linux this block
// wires nothing, leaving the Android/metadata builds untouched.
// (Note: the exists() check runs at configuration time; with the configuration
// cache enabled, delete the cache entry or re-run after building the bridge if
// the framework appears out-of-band.)
if (isMacOsHost && !bridgeFrameworkDir.exists()) {
    tasks.matching { it.name.startsWith("cinteropSuperwallKMPBridge") }.configureEach {
        dependsOn(buildBridgeXCFramework)
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "superwall-kmp", version.toString())

    pom {
        name = "Superwall KMP"
        description = "Kotlin Multiplatform SDK for Superwall, wrapping the native Android and iOS SuperwallKit SDKs."
        inceptionYear = "2026"
        url = "https://github.com/superwall/Superwall-KMP"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "superwall"
                name = "Superwall"
                url = "https://superwall.com"
            }
        }
        scm {
            url = "https://github.com/superwall/Superwall-KMP"
            connection = "scm:git:git://github.com/superwall/Superwall-KMP.git"
            developerConnection = "scm:git:ssh://git@github.com/superwall/Superwall-KMP.git"
        }
    }
}
