# CHANGELOG

The changelog for `Superwall-KMP`. Also see the [releases](https://github.com/superwall/Superwall-KMP/releases) on GitHub.

## 0.1.2

## Enhancements
- Updates iOS SDK to 4.18.2 [View iOS SDK release notes](https://github.com/superwall/Superwall-iOS/releases/tag/4.18.2).
- Raises the minimum iOS version from iOS 14 to iOS 15, matching SuperwallKit 4.18.0. Building the iOS app now requires Xcode 26 (Swift 6.2) or later.
- Adds `Superwall.presentCustomerCenter(onDismiss)` and `Superwall.dismissCustomerCenter()` to show the Customer Center, a self-service screen where users can view, restore, manage, cancel, refund and change their purchases, and contact support. Works on both platforms. On Android, `onDismiss` is also called if the Customer Center couldn't open, for example when called while the app is in the background.
- Adds `SuperwallOptions.adConsent` and `Superwall.adConsent` to report a user's ad consent (`AdConsent` with `adUserData` and `adPersonalization`), which Superwall passes on with the conversions it uploads to Google Ads and Meta. Leave it unset to use an IAB TCF consent banner's stored answer. iOS only.
- Adds `Superwall.grantedEntitlements` to grant entitlements from your own backend, which the SDK merges with device and web entitlements. iOS only.
- Adds `userAttributes` to the `RedemptionInfo` passed to `didRedeemLink`, with the answers someone gave on your web paywall funnel. iOS only; `null` on Android.
- Adds `SuperwallOptions.devServer` (`DevServer.Default` on a simulator, `DevServer.Url` on a device) to render paywalls from a running `superwall dev` server in development builds, and `PaywallInfo.isLocal`. iOS only.
- Delivers the Customer Center (`customerCenterOpen`, `customerCenterClose`, `customerCenterAction`, `customerCenterSurveyResponse`, `customerCenterRefundRequest`, both platforms) and iOS teleport (`teleportOpen`, `teleportReturn`) events to `SuperwallDelegate.handleSuperwallEvent` as `EventType.CUSTOM_PLACEMENT`, with the event name in `name` and its details in `params`. Customer Center events use the same names and `params` keys on both platforms.
- Updates Android SDK to 2.8.8 [View Android SDK release notes](https://github.com/superwall/Superwall-Android/releases/tag/2.8.8).
- Adds `IntegrationAttribute.AIRBRIDGE_ID` for the Airbridge Device ID from `Airbridge.fetchDeviceUUID`. Android only; on iOS it is skipped with a logged warning.
- Adds `PaywallOptions.presentationHost`. Set it to `PresentationHost.IN_WINDOW` to show paywalls as an overlay in the presenting activity's own window instead of in a separate activity, for hosts such as game engines that stop running while another activity covers them. Experimental and Android only; the default stays `PresentationHost.ACTIVITY`.
- Adds `PaywallInfo.presentationId`, a unique id for each paywall presentation that matches the `presentation_id` on that presentation's paywall events. Android only; `null` on iOS.
- On Android, a paywall whose webview can't load is now dismissed as declined with `PaywallCloseReason.WEB_VIEW_FAILED_TO_LOAD`, matching iOS. Your `PaywallPresentationHandler` gets `onDismiss`, then the feature block for a non-gated placement or `onError` for a gated one.

## Fixes
- Fixes `null` values in iOS event and redemption payloads arriving as the string `"<null>"` instead of `null`.

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
