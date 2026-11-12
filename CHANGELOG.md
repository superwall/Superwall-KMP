# CHANGELOG

The changelog for `Superwall-KMP`. Also see the [releases](https://github.com/superwall/Superwall-KMP/releases) on GitHub.

## 0.2.0

## Breaking Changes
- iOS: the minimum deployment target is now iOS 15, and the iOS side must be built with Xcode 26 (Swift 6.2) or later. SuperwallKit 4.18.0 raised both, and the `SuperwallKMPBridge` Swift package now declares iOS 15 and swift-tools-version 6.2 to match. Set your app's iOS deployment target to 15.0 or higher.

## Enhancements
- Updates Android SDK to 2.10.2 [View Android SDK release notes](https://github.com/superwall/Superwall-Android/releases/tag/2.10.2).
- Updates iOS SDK to 4.19.1 [View iOS SDK release notes](https://github.com/superwall/Superwall-iOS/releases/tag/4.19.1).

## Fixes
- Fixes `attributionMatch`, `paywallPageView`, `testModeModalOpen` and `testModeModalClose`, the Android-only `reviewGranted` and `reviewDenied`, and the iOS-only `stripeCheckoutStart`, `stripeCheckoutSubmit`, `stripeCheckoutComplete` and `stripeCheckoutFail` events reaching `SuperwallDelegate.handleSuperwallEvent` as `EventType.CUSTOM_PLACEMENT` with a platform-specific name. Each now has its own `EventType`. The Customer Center events (`CUSTOMER_CENTER_OPEN`, `CUSTOMER_CENTER_CLOSE`, `CUSTOMER_CENTER_ACTION`, `CUSTOMER_CENTER_SURVEY_RESPONSE`, `CUSTOMER_CENTER_REFUND_REQUEST`) and the iOS-only `TELEPORT_OPEN` and `TELEPORT_RETURN` that the new native SDKs send get their own types too. If you matched any of these by name on `CUSTOM_PLACEMENT`, match the new `EventType` instead.
- Android: fixes event details being dropped. Permission events now have `permissionName` and `paywallIdentifier` in `SuperwallEventInfo.params`, paywall preload events `paywallCount`, `PAYWALL_PAGE_VIEW` the page details under `pageViewData`, `ATTRIBUTION_MATCH` the match under `attributionMatch`, `PAYWALL_RESOURCE_LOAD_FAIL` the resource `url`, and `SHIMMER_VIEW_COMPLETE` its `duration`, and `NON_RECURRING_PRODUCT_PURCHASE` the product's `transactionProductId`. The keys are the ones iOS already used.
- iOS: permission events now have the permission name in `SuperwallEventInfo.name`, as on Android.
- iOS: fixes `Superwall.customerInfoFlow` emitting every customer info change twice, plus the current value when the SDK finished configuring. It now emits once per change, as on Android. Use `getCustomerInfo()` for the current value.
- iOS: exceptions thrown by your `PurchaseController`, your `PaywallPresentationHandler` callbacks or the `register` feature block are now reported to `SuperwallDelegate.handleLog` at `LogLevel.ERROR`, as on Android. They were only printed to the console.
- Android: fixes the restore-failed alert always showing English text. `RestoreFailed` fields left at their defaults are no longer passed to the Android SDK, so it shows them in the device's language.
- Fixes requests from the native SDKs not saying they came through superwall-kmp. The SDK now reports itself as the `KMP` platform wrapper, with its version, when it configures, as the other Superwall wrapper SDKs do.
- Corrects the option docs: `PaywallOptions.transactionBackgroundView` also applies on Android, `shouldShowWebRestorationAlert` and `shouldShowWebPurchaseConfirmationAlert` are iOS only, and `SuperwallOptions.shouldObservePurchases` alone reports nothing on Android.
- An event newer than the SDK still arrives as `CUSTOM_PLACEMENT` with its native name in `name`; on Android that name is now also in `params["rawEventType"]`, as on iOS.

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
