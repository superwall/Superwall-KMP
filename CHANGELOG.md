# CHANGELOG

The changelog for `Superwall-KMP`. Also see the [releases](https://github.com/superwall/Superwall-KMP/releases) on GitHub.

## 0.1.1

## Enhancements
- Adds threading improvements to reduce main thread load

## 0.1.0

Initial release of the Kotlin Multiplatform SDK for Superwall.

## Enhancements
- Adds `com.superwall.sdk:superwall-kmp`, a Kotlin Multiplatform wrapper over the native Superwall SDKs. The entire public API lives in `commonMain` — no platform types leak into it, and `Superwall.configure` has an identical signature on both platforms (no `Context` parameter on Android).
- Android support (`minSdk 26`) wrapping `com.superwall.sdk:superwall-android` 2.8.0. The dependency is bundled transitively and an `androidx.startup` initializer captures the `Application`, so integration is a single Gradle dependency.
- iOS support (iOS 14+, `iosArm64`/`iosSimulatorArm64`/`iosX64`) forwarding through **SuperwallKMPBridge**, a self-authored `@objc` Swift facade over SuperwallKit iOS 4.16.1 (pinned exactly). The bridge destructures Swift-only constructs — enum associated values, structs, `async` — into ObjC-visible envelopes consumed via cinterop.
- Adds `Superwall.register(placement:params:handler:feature:)` for gating features behind paywalls, with a `PaywallPresentationHandler` exposing `onPresent`, `onDismiss`, `onError` and `onSkip`.
- Adds `Superwall.subscriptionStatusFlow`, a `StateFlow` that is collectable before `configure` (seeded with `SubscriptionStatus.Unknown`) and emits on the main thread.
- Adds `PurchaseController` for apps that own their purchase logic, with separate `purchaseFromAppStore` and `purchaseFromGooglePlay` entry points.
- Adds `SuperwallDelegate` covering the paywall presentation lifecycle, subscription-status changes, deep links, URLs, custom paywall actions, logging, and link redemption.
- Adds `SuperwallOptions` (including `PaywallOptions` and `TestModeBehavior`), user identity (`identify`, `reset`, user attributes), and deep-link handling via `handleDeepLink`.
- Adds `configureAndAwait`, a suspending twin of `configure`, plus the `Superwall.isConfigured` flag for ordering calls.

## Notes
- There is no pre-configure call queue: most members throw `SuperwallError.NotConfigured` before `configure`. Guard-exempt members are `handleDeepLink`, the flows, `delegate`, and the introspection properties.
- iOS integration is two steps — the Gradle dependency plus the `SuperwallKMPBridge` Swift package — and the Kotlin framework must be exported as `isStatic = true`, because it does not embed the bridge binary. See the [README](README.md#ios) for details.
