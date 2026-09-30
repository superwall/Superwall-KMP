# Superwall KMP — agent & contributor guide

Kotlin Multiplatform SDK wrapping the native Superwall SDKs behind one common API.

---

## Part 1 — Using the SDK

### Install

**Android** — one dependency. `superwall-android` comes in transitively and an
`androidx.startup` initializer captures the `Application`, so there is no
`Context` parameter anywhere in the API.

```kotlin
implementation("com.superwall.sdk:superwall-kmp:<version>")
```

**iOS** — two steps, and both are required:

1. The same Gradle dependency in your shared module, with the framework exported
   as **static**:

   ```kotlin
   listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
       it.binaries.framework {
           baseName = "Shared"
           isStatic = true // required — the Kotlin framework does not embed the bridge binary
       }
   }
   ```

2. The **SuperwallKMPBridge** Swift package added to the iOS app target
   (`https://github.com/superwall/Superwall-KMP`). It pins SuperwallKit
   transitively — do not add SuperwallKit separately.

   Kotlin compiles against the bridge's ObjC headers only (compile-only
   cinterop); the app supplies the binary. If it is missing you get a **link
   error at app build time**, not a runtime failure.

**Web** — the same Gradle dependency on a `js(IR) { useEsModules(); browser() }`
target; `@superwall/paywalls-js` comes in transitively through npm. The app
module also needs a `webpack.config.d/superwall.js` enabling
`experiments.asyncWebAssembly` and relaxing `fullySpecified` for
`@superwall/superscript` (see the README for the two lines). A
`PurchaseController` is ignored on web — Superwall's built-in web checkout
handles purchases.

### Usage

```kotlin
Superwall.configure(apiKey = "pk_...") { result ->
    result.onFailure { println("Superwall configuration failed: $it") }
}

Superwall.register(placement = "campaign_trigger") {
    launchTheFeature() // runs per the paywall's feature-gating behavior
}

scope.launch {
    Superwall.subscriptionStatusFlow.collect { status -> /* Active / Inactive / Unknown */ }
}
```

### Rules that surprise people

- **There is no pre-configure call queue.** Most members throw
  `SuperwallError.NotConfigured` before `configure`. Use `configureAndAwait`, or
  gate on `Superwall.isConfigured`. Guard-exempt: `handleDeepLink`, the flows,
  `delegate`, and the introspection properties.
- **Callbacks are delivered on the main thread** — delegate methods, presentation
  handler closures, and flow emissions.
- **`configure` runs once per process.** Repeat calls are no-ops whose completion
  fires with the first call's outcome; options and the purchase controller are
  installed only by the first one.

---

## Part 2 — Contributing

### Layout

| Path | What |
|---|---|
| `superwall-kmp/` | the published KMP module — the only thing consumers get |
| `bridge/` | `SuperwallKMPBridge`, the `@objc` Swift facade over SuperwallKit |
| `sample/shared` | Compose Multiplatform test app UI (commonMain) |
| `sample/androidApp` · `sample/iosApp` | platform hosts (`iosApp` is xcodegen, not Gradle) |
| `sample/webApp` | browser host for the shared Compose UI |
| `sample/webMinimal` | standalone Kotlin/JS page — the minimal web consumer setup |
| `sample/maestro/` | Maestro UI flows |
| `Package.swift` | **consumer-facing** SPM manifest — machine-owned, see Release |

### Architecture

The public `Superwall` facade (`superwall-kmp/src/commonMain/.../Superwall.kt`)
is a thin layer over an internal `SuperwallBridge` interface with one platform
`actual` each:

- **Android** (`androidMain`) wraps `com.superwall.sdk:superwall-android` directly.
- **iOS** (`iosMain`) forwards through the Swift bridge in `bridge/`, which
  destructures Swift-only constructs — enum associated values, structs, `async` —
  into ObjC-visible envelopes (`SWBEventEnvelope` and friends) consumed via
  cinterop. Kotlin never sees a Swift type.
- **Web** (`jsMain`) wraps the `@superwall/paywalls-js` npm package (Superwall-Web)
  through hand-written `external` declarations in `internal/interop/`. Payloads
  stay `dynamic` and are read field-by-field in `internal/mappers/`. Where web
  has no equivalent, the bridge member degrades (no-op / empty) and says why at
  the site.

Version pins live in two layers and must stay in lockstep: `bridge/Package.swift`
pins SuperwallKit exactly, and the root `Package.swift` pins the same version so
the prebuilt binary and the transitively resolved SuperwallKit cannot drift.

### Conventions

- `explicitApi()` is on. Every public declaration needs explicit visibility and
  an explicit return type.
- **The public API lives entirely in `commonMain`.** No platform types in public
  signatures, and `configure` has an identical signature on both platforms.
- Anything platform-specific goes behind the internal `SuperwallBridge`.
- Event/enum mappers are exhaustive by design — an unmapped native case falling
  through to a params-fallback is a bug, and there are tests that assert this.

### Build & test

```bash
./gradlew :superwall-kmp:build            # build the library
./gradlew :superwall-kmp:allTests         # common + Android host + JS (Node) tests, iOS simulator tests on macOS
./gradlew :superwall-kmp:jsNodeTest       # common + JS tests only — runs anywhere, no Android SDK needed
./gradlew :superwall-kmp:check            # lint
./bridge/scripts/build-xcframework.sh     # build the bridge XCFramework (macOS)
```

**Bridge tests do not run under `swift test`.** `bridge/Package.swift` declares
`platforms: [.iOS(.v14)]`, so `swift test` builds for the macOS host where the
iOS-only SuperwallKit (`import UIKit`) cannot compile. Use a simulator:

```bash
cd bridge && xcodebuild test -scheme SuperwallKMPBridge \
  -workspace . -destination 'platform=iOS Simulator,name=iPhone 17'
```

Building `:superwall-kmp`'s cinterop on macOS auto-builds the bridge XCFramework
if it is missing (network needed for SPM resolution; several minutes once).

### Sample app

```bash
./gradlew :sample:androidApp:installDebug          # Android
./gradlew :sample:webApp:jsBrowserDevelopmentRun   # Web (Compose); :sample:webMinimal for the plain page
cd sample/iosApp && xcodegen generate && open SuperwallKMPSample.xcodeproj
```

The `.xcodeproj` is **generated and gitignored** — edit `sample/iosApp/project.yml`
and regenerate; changes made in the Xcode UI (including the signing Team) are
lost on the next `xcodegen generate`.

App ids match the Superwall dashboard apps their API keys belong to: Android
`com.superwall.superapp`, iOS `com.superwall.Advanced`. Keys are the public
test-app keys shared with the Flutter SDK's `test_app` — safe to commit.

See `sample/maestro/README.md` for the UI flows, including which ones need a
purchase sandbox and why the StoreKit sheet has to be tapped by coordinates.

### Release

1. Bump `SUPERWALL_VERSION` in `version.env` (the only version source — the
   module reads it via `rootProject.extra["superwallVersion"]`).
2. Add a `## <version>` section to `CHANGELOG.md`. The workflow extracts it
   verbatim as the GitHub release body, so an empty section ships an empty release.
3. Merge to `main`.

`.github/workflows/release.yml` then: builds and tests the bridge → resolves the
version → skips everything if that version is already tagged → zips the
XCFramework and computes its checksum → **rewrites `Package.swift`'s binaryTarget
url + checksum and commits** → publishes to Maven Central → tags, cuts
`release/<version>`, creates the GitHub release with the XCFramework attached →
notifies Slack.

**Do not hand-edit `Package.swift`'s url/checksum.** SPM resolves the manifest at
the git tag, so the rewrite must be committed *before* tagging; that ordering is
the whole reason the step sits where it does.

Required repository secrets: the five `ORG_GRADLE_PROJECT_*` (Maven Central
credentials + in-memory PGP signing), `SLACK_WEBHOOK`, and `MAIN_REPO_PAT`.
