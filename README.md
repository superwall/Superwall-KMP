# Superwall KMP

Kotlin Multiplatform SDK for [Superwall](https://superwall.com) — remotely configurable in-app paywall infrastructure.

This library wraps the native SuperwallKit SDKs (Android and iOS) behind a single Kotlin Multiplatform API. The public API lives entirely in `commonMain` — no platform types, and an identical `configure` signature on both platforms (no `Context` parameter on Android).

## Installation

### Android

One Gradle dependency — the native `superwall-android` SDK is bundled transitively, and an androidx.startup initializer captures the `Application` automatically:

```kotlin
// build.gradle.kts (commonMain or androidMain dependencies)
implementation("com.superwall.sdk:superwall-kmp:<version>")
```

### iOS

Two steps:

1. Add the same Gradle dependency (above) to your shared module, and export the framework as **static**:

   ```kotlin
   // shared module build.gradle.kts
   listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
       it.binaries.framework {
           baseName = "Shared"
           isStatic = true // required — the Kotlin framework does not embed the bridge binary
       }
   }
   ```

2. Add the **SuperwallKMPBridge** Swift package to your iOS app via SPM, using this repository's URL:

   ```
   https://github.com/superwall/Superwall-KMP
   ```

   In Xcode: File → Add Package Dependencies… → paste the URL → add the `SuperwallKMPBridge` product to your app target. The package pins SuperwallKit iOS (exact `4.16.1`) transitively — do not add SuperwallKit separately.

   The Kotlin side compiles against the bridge's ObjC headers only (compile-only cinterop); your app supplies the binary by linking the SPM package. If it is missing you'll get a link error at app build time, not at runtime.

## Usage

```kotlin
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import kotlinx.coroutines.launch

// 1. Configure as early as possible (suspend twin: Superwall.configureAndAwait(...))
Superwall.configure(apiKey = "pk_your_api_key") { result ->
    result.onFailure { println("Superwall configuration failed: $it") }
}

// 2. Register a placement to gate a feature behind a paywall
Superwall.register(placement = "campaign_trigger") {
    // Feature code — runs per the paywall's feature-gating behavior
    launchTheFeature()
}

// 3. Observe subscription status (collectable even before configure;
//    seeded with SubscriptionStatus.Unknown, emissions on the main thread)
scope.launch {
    Superwall.subscriptionStatusFlow.collect { status ->
        when (status) {
            is SubscriptionStatus.Active -> showPro(status.entitlements)
            is SubscriptionStatus.Inactive -> showFree()
            is SubscriptionStatus.Unknown -> showLoading()
        }
    }
}
```

There is no pre-configure call queue: most members throw `SuperwallError.NotConfigured` before `configure`. Use `configureAndAwait` or gate on `Superwall.isConfigured` to order calls. Guard-exempt: `handleDeepLink`, the flows, `delegate`, and the introspection properties.

## Targets

- Android (`minSdk 26`, matching the native superwall-android SDK)
- iOS 14+ (`iosArm64`, `iosSimulatorArm64`, `iosX64`)

## Development

```bash
# Build the library
./gradlew :superwall-kmp:build

# Run tests (common + Android host tests; iOS simulator tests on macOS)
./gradlew :superwall-kmp:allTests

# Lint
./gradlew :superwall-kmp:check

# Swift bridge tests (macOS only)
cd bridge && swift test

# Build the bridge XCFramework (macOS only)
./bridge/scripts/build-xcframework.sh
```

## Architecture

The public `Superwall` facade (`superwall-kmp/src/commonMain/.../Superwall.kt`) is a thin layer over an internal `SuperwallBridge` interface with one platform `actual` each:

- **Android** (`androidMain`) wraps `com.superwall.sdk:superwall-android` directly.
- **iOS** (`iosMain`) forwards through **SuperwallKMPBridge** (`bridge/`), a self-authored `@objc` Swift facade over SuperwallKit that destructures Swift-only constructs (enum associated values, structs, async) into ObjC-visible envelopes, consumed via cinterop.

See `docs/` for the details:

- [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md) — full architecture, API design, phased plan
- [`docs/bridge-surface.md`](docs/bridge-surface.md) — the iOS bridge API map: Kotlin bridge member → `@objc` bridge API → SuperwallKit call, the `SWBEventEnvelope` design, and the SuperwallKit 4.16.1 deltas

## License

Apache License 2.0 — see [LICENSE](LICENSE).
