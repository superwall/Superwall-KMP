# Superwall KMP — sample app

A Compose Multiplatform port of the Flutter SDK's `test_app` (see `test_app/lib/` in the Superwall-Flutter repo), exercising the `:superwall-kmp` SDK on Android and iOS from a single shared UI.

## What the app shows

`sample/shared/src/commonMain/.../App.kt` — a Home screen navigating to one test screen per SDK area, mirroring the Flutter test_app's routes:

- **Configuration test** (`ConfigureTestScreen.kt`) — `Superwall.configure` with/without the mock purchase controller, showing the completion's real `Result<Unit>` in a dialog. Every configure call in the app disables paywall preloading (`PaywallOptions.shouldPreload = false`). The Flutter screen's RevenueCat variant has no port (no RevenueCat dependency here).
- **Subscription Status Test** (`SubscriptionStatusTestScreen.kt`) — sets `Superwall.subscriptionStatus` to Active (`pro` + `test_entitlement`) / Inactive / Unknown and shows the result in a dialog.
- **Purchase Controller Test** (`PurchaseControllerTestScreen.kt`) — configures with the shared mock `TestingPurchaseController`, triggers the `campaign_trigger` paywall, and toggles whether mock purchases/restores succeed.
- **Delegate Test** (`DelegateTestScreen.kt`) — installs a `SuperwallDelegate` that records every callback, then inspects the recorded events (with/without the high-volume log and analytics callbacks).
- **Handler Test** (`HandlerTestScreen.kt`) — registers `non_gated_paywall` / `gated_paywall` / `skip_audience` / `error_placement` with a recording `PaywallPresentationHandler`, plus a results box showing feature-block execution and event count.

Screens that need a configured SDK surface `SuperwallError.NotConfigured` in a dialog when used before configuring — the SDK has no pre-configure call queue, and the guard is part of what the app demonstrates.

## Where the API key lives

`superwallApiKey` is an expect/actual in `sample/shared/src/{commonMain,androidMain,iosMain}/kotlin/com/superwall/sdk/kmp/sample/ApiKey*.kt`. These are the **public test-app keys** shared with the Flutter SDK's `test_app` (Android: `com.superwall.superapp`, iOS: `com.superwall.Advanced`) — safe to commit. Replace them with your own dashboard's Public API Key (Settings → Keys) to run against your campaigns; note iOS and Android normally use **different** keys per platform-app.

## Module layout

| Module | What | Build |
|---|---|---|
| `sample/shared` | KMP module: the Compose UI (commonMain), `setSampleAppContent()` activity host (androidMain), `MainViewController()` = `ComposeUIViewController { App() }` (iosMain). iOS targets export a **static** `SampleShared` framework (static is required — see the root README's iOS install notes). | Gradle (`:sample:shared`) |
| `sample/androidApp` | Android application (`com.superwall.sdk.kmp.sample`, minSdk 26, compileSdk 36). Uses AGP 9 **built-in Kotlin** — no `org.jetbrains.kotlin.android`, no Compose compiler; all `@Composable` code stays in `sample/shared`. | Gradle (`:sample:androidApp`) |
| `sample/iosApp` | SwiftUI app embedding `MainViewController()` via `UIViewControllerRepresentable`. Not a Gradle module — an [xcodegen](https://github.com/yonaskolb/XcodeGen) project (`project.yml`). | xcodegen + Xcode (macOS) |

Neither sample module is published.

## Running — Android

```bash
# from the repo root; needs an emulator/device connected
./gradlew :sample:androidApp:installDebug
```

Then launch "Superwall KMP Sample". (Plain `:sample:androidApp:assembleDebug` to just build.)

## Running — iOS (macOS + Xcode required)

```bash
brew install xcodegen   # once
cd sample/iosApp
xcodegen generate
open SuperwallKMPSample.xcodeproj
```

Select the SampleApp scheme + a simulator and Run. What happens on the first build:

1. A pre-build script phase runs `./gradlew :sample:shared:embedAndSignAppleFrameworkForXcode`, which compiles the Kotlin `SampleShared` framework for the active configuration/SDK. Building `:superwall-kmp`'s cinterop on macOS also auto-builds `bridge/build/SuperwallKMPBridge.xcframework` from `bridge/` if missing (needs network for SPM resolution; takes several minutes once). A JDK must be visible to Xcode's build environment (`JAVA_HOME` or on `PATH`).
2. SPM resolves the **local** `../../bridge` package (SuperwallKMPBridge built from source) and pins SuperwallKit `4.16.1` transitively. The sample uses the local source package rather than the repo-root `Package.swift` because that manifest's binary-target URL/checksum are placeholders until the first release publishes the XCFramework artifact. Do **not** add SuperwallKit separately.

This mirrors the documented consumer setup (root README → Installation → iOS): static Kotlin framework + the bridge SPM package supplying the binary the klib was compiled against.

## Troubleshooting

- **`ld: framework 'SampleShared' not found`** — the pre-build Gradle phase failed (check the build log's script-phase output; usually a missing JDK) or you built before it finished once.
- **Undefined ObjC symbols `_OBJC_CLASS_$_SWB…`** — the SuperwallKMPBridge package isn't linked; re-run `xcodegen generate` and confirm the package product is attached to the SampleApp target.
- **Android: paywall doesn't show** — the test key's campaign must have a `campaign_trigger` placement (it does on the shared test account); check Logcat for `Superwall` output.
