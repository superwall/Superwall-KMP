# Superwall KMP — sample app

A Compose Multiplatform port of the Flutter SDK's `test_app` (see `test_app/lib/` in the Superwall-Flutter repo), exercising the `:superwall-kmp` SDK on Android, iOS and the web from a single shared UI. A separate minimal web sample (`sample/webMinimal`) shows the bare Kotlin/JS setup without Compose.

## What the app shows

`sample/shared/src/commonMain/.../App.kt` — a Home screen navigating to one test screen per SDK area, mirroring the Flutter test_app's routes:

- **Configuration test** (`ConfigureTestScreen.kt`) — `Superwall.configure` with/without the mock purchase controller, showing the completion's real `Result<Unit>` in a dialog. Every configure call in the app disables paywall preloading (`PaywallOptions.shouldPreload = false`). The Flutter screen's RevenueCat variant has no port (no RevenueCat dependency here).
- **Subscription Status Test** (`SubscriptionStatusTestScreen.kt`) — sets `Superwall.subscriptionStatus` to Active (`pro` + `test_entitlement`) / Inactive / Unknown and shows the result in a dialog.
- **Purchase Controller Test** (`PurchaseControllerTestScreen.kt`) — configures with the shared mock `TestingPurchaseController`, triggers the `campaign_trigger` paywall, and toggles whether mock purchases/restores succeed. Its **"Configure with test mode"** button has no Flutter counterpart: it configures with `TestModeBehavior.ALWAYS`, which resolves products from Superwall's servers and simulates transactions in the SDK's own drawer — the only way to buy on a bare simulator/emulator. Test mode bypasses the purchase controller as well as the store, so it does not exercise `TestingPurchaseController`.

Every **other** configure path in the app passes `TestModeBehavior.NEVER` (not the SDK default `AUTOMATIC`), so real purchases always go to the real store. `AUTOMATIC` would silently fall back to simulated purchases on a bundle-ID mismatch — e.g. when the app is re-signed with a different team to run on a device — which would quietly invalidate exactly the thing a device run is meant to verify.
- **Delegate Test** (`DelegateTestScreen.kt`) — installs a `SuperwallDelegate` that records every callback, then inspects the recorded events (with/without the high-volume log and analytics callbacks).
- **Handler Test** (`HandlerTestScreen.kt`) — registers `non_gated_paywall` / `gated_paywall` / `skip_audience` / `error_placement` with a recording `PaywallPresentationHandler`, plus a results box showing feature-block execution and event count.

Screens that need a configured SDK surface `SuperwallError.NotConfigured` in a dialog when used before configuring — the SDK has no pre-configure call queue, and the guard is part of what the app demonstrates.

## Where the API key lives

`superwallApiKey` is an expect/actual in `sample/shared/src/{commonMain,androidMain,iosMain}/kotlin/com/superwall/sdk/kmp/sample/ApiKey*.kt`. These are the **public test-app keys** shared with the Flutter SDK's `test_app` — safe to commit. Each key belongs to a dashboard platform-app, so the sample's app ids match those apps: Android applicationId `com.superwall.superapp`, iOS bundle id `com.superwall.Advanced`. Replace them with your own dashboard's Public API Key (Settings → Keys) to run against your campaigns; note iOS and Android normally use **different** keys per platform-app.

The web key (`ApiKey.js.kt`, also in `sample/webMinimal`) is the public key of Superwall-Web's own example apps. Its campaign has `campaign_trigger`; the other placements the test screens use exist only in the mobile test apps and resolve as "placement not found" on web.

## Module layout

| Module | What | Build |
|---|---|---|
| `sample/shared` | KMP module: the Compose UI (commonMain), `setSampleAppContent()` activity host (androidMain), `MainViewController()` = `ComposeUIViewController { App() }` (iosMain). iOS targets export a **static** `SampleShared` framework (static is required — see the root README's iOS install notes). | Gradle (`:sample:shared`) |
| `sample/androidApp` | Android application (applicationId `com.superwall.superapp`, namespace `com.superwall.sdk.kmp.sample`, minSdk 26, compileSdk 36). Uses AGP 9 **built-in Kotlin** — no `org.jetbrains.kotlin.android`, no Compose compiler; all `@Composable` code stays in `sample/shared`. | Gradle (`:sample:androidApp`) |
| `sample/iosApp` | SwiftUI app embedding `MainViewController()` via `UIViewControllerRepresentable`. Not a Gradle module — an [xcodegen](https://github.com/yonaskolb/XcodeGen) project (`project.yml`). | xcodegen + Xcode (macOS) |
| `sample/webApp` | Browser host: `main()` calls `startSampleApp()` (`sample/shared` jsMain), which renders `App()` on a canvas with `ComposeViewport`. No `@Composable` code of its own. | Gradle (`:sample:webApp`) |
| `sample/webMinimal` | Standalone Kotlin/JS page — plain HTML buttons, an event log, no Compose. The smallest complete consumer setup. | Gradle (`:sample:webMinimal`) |

None of the sample modules are published.

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

### StoreKit products

`sample/iosApp/Products.storekit` (copied from the Flutter test_app: subscriptions `superwall_pro_3999` and `superwall_diamond_8999`) is attached to the SampleApp scheme's **run action**. Without it the paywall resolves no products — it presents with an empty price and its CONTINUE button does nothing.

It applies only when **Xcode** launches the app. A `simctl`-launched build (including anything Maestro starts) does not get it; `xcrun simctl` has no storekit option.

## Running — Web

```bash
./gradlew :sample:webApp:jsBrowserDevelopmentRun      # the Compose sample
./gradlew :sample:webMinimal:jsBrowserDevelopmentRun  # the minimal page
```

Each opens a webpack dev server in the browser. Both modules carry the consumer setup from the root README's "Web (Kotlin/JS)" section: an ES-module `js` target and `webpack.config.d/superwall.js`.

On web, the purchase controller is ignored (the SDK logs a warning): purchases go through Superwall's built-in checkout, so the **Mock PC Test** screen's purchase/restore toggles have no effect there. Web checkout also only completes on a host allowed for the dashboard app, so on `localhost` the paywall's Continue button fails with a 403 — the paywall itself, closing it, and the feature callback all work.

## UI tests (Maestro)

`sample/maestro/` holds the Maestro flows, ported from the Flutter SDK's
`test_app/maestro/`. The app id is a parameter, since it differs per platform:

```bash
maestro test -e APP_ID=com.superwall.Advanced sample/maestro/flow.yaml   # iOS
maestro test -e APP_ID=com.superwall.superapp sample/maestro/flow.yaml   # Android
```

See [`maestro/README.md`](maestro/README.md) for the per-flow status, the
deviations from the Flutter originals, and why the two purchase-controller flows
need a StoreKit product source that a Maestro run cannot supply.

## Troubleshooting

- **`ld: framework 'SampleShared' not found`** — the pre-build Gradle phase failed (check the build log's script-phase output; usually a missing JDK) or you built before it finished once.
- **Undefined ObjC symbols `_OBJC_CLASS_$_SWB…`** — the SuperwallKMPBridge package isn't linked; re-run `xcodegen generate` and confirm the package product is attached to the SampleApp target.
- **Android: paywall doesn't show** — the test key's campaign must have a `campaign_trigger` placement (it does on the shared test account); check Logcat for `Superwall` output.
