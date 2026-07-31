# Superwall KMP SDK — Implementation Plan

**Repo:** `/home/user/Superwall-KMP` · **Coordinates:** `com.superwall.sdk:superwall-kmp` · **Package:** `com.superwall.sdk.kmp`
**Toolchain (verified in repo):** Kotlin 2.3.10, AGP 9.0.1 (`com.android.kotlin.multiplatform.library`), Gradle 9.1.0, vanniktech maven-publish 0.36.0 · **Targets:** androidLibrary (minSdk 24 **provisional — see §4 minSdk note and Open Question #11**, compileSdk 36), iosArm64, iosSimulatorArm64, iosX64
**Native SDK pins:** `com.superwall.sdk:superwall-android:2.8.0` (latest on Maven Central; the Flutter plugin pins 2.7.11 and `minSdkVersion 26` — see §4) · SuperwallKit iOS **4.16.1** (latest tagged release; 4.16.2 exists only unreleased on main), min iOS 14
**API contract source of truth:** `/home/user/Superwall-Flutter/pigeons/configure.dart` (P-prefix stripped) · **Behavioral porting specs:** `/home/user/Superwall-Flutter/android/src/main/kotlin/com/superwall/superwallkit_flutter/*` and `/home/user/Superwall-Flutter/ios/Classes/*`

> This document merges three independent architecture proposals. Where they diverged, the resolution and its reasoning are recorded inline in *Resolved:* callouts.
>
> **Revision (2026-07-31):** the iOS binding strategy changed from direct cinterop against SuperwallKit's `@objc(SWK…)` surface to a **self-authored `@objc` Swift bridge (`SuperwallKMPBridge`) shipped as a prebuilt XCFramework** on stable Kotlin — Kotlin's first-party SwiftPM import (Alpha, 2.4.20-Beta) was evaluated and deliberately deferred to v2. See §5.1 for the rationale.

---

## 1. Goals & non-goals

### Goals
- Ship a single KMP module exposing the **same external models and interfaces as the Flutter SDK's Pigeon definition**, without the `P` prefix, as hand-written commonMain Kotlin (no codegen). **Every deliberate deviation from the Pigeon contract is individually recorded and sign-off-gated** (governance in §3.4; ledger in `docs/MODELS.md`).
- Public API is **100% commonMain**: no cinterop type, no native SDK type, and no `ExperimentalForeignApi` ever appears in a public signature.
- Internally wrap **SuperwallKit Android** (androidMain) and **SuperwallKit iOS** (iosMain), playing exactly the role the Flutter plugin's native host classes play today.
- Idiomatic Kotlin async: `suspend` where the natives are async, synchronous properties where they are sync, `StateFlow`/`Flow` for the two event streams (replacing Pigeon EventChannels).
- **Fix, don't port, the known Flutter-layer bugs** (enumerated in §3.4).
- Identical `configure(apiKey, …)` signature on both platforms — no `Context` parameter (androidx.startup captures it).
- Publish to Maven Central via vanniktech; documented two-step iOS install (Gradle dep + `SuperwallKMPBridge` SPM/CocoaPods package, which pins SuperwallKit transitively) with a compatibility table.
- Own the Swift↔Kotlin boundary: a **self-authored `@objc` Swift bridge** (`SuperwallKMPBridge`) destructures Swift-only constructs (enum associated values, structs, async) into an ObjC-visible surface designed for exactly what `SuperwallBridge` needs — no dependency on upstream Superwall-iOS `@objc` additions.

### Non-goals (v1)
- **Static embedding of SuperwallKit/Superscript** into the Kotlin framework (RevenueCat-3.0 pattern) — explicit v2 milestone, not a v1 blocker.
- Swift-export ergonomics for Swift-first consumers (Kotlin Swift export is alpha; primary v1 consumers are Kotlin/Compose MP apps).
- A `SuperwallBuilder`-widget analog — `StateFlow` + Compose `collectAsState` covers it.
- Pre-configure call queuing (matches the Flutter SDK's deliberate choice; see §7).
- Exposing raw `ProductDetails` (Android) / `StoreProduct` (iOS) in the `PurchaseController` — candidate additive platform extension post-1.0.
- Porting Pigeon transport artifacts: the five `*Host` marker classes, hostId/messageChannelSuffix routing, `setDelegate(Boolean)`, `ignore: Boolean?` fields, `SubscriptionStatusType`, `TransactionProduct`, deprecated typedefs.
- **Kotlin's first-party SwiftPM import** (`swiftPMDependencies {}` / `localSwiftPackage(...)`) — it is Alpha and requires the 2.4.20-Beta Kotlin Gradle plugin; we stay on stable Kotlin and hand-wired cinterop against the prebuilt bridge XCFramework. Explicit migration candidate once the feature is stable (see v2, §9).

---

## 2. Architecture overview

### 2.1 Layer diagram

```mermaid
flowchart TB
    subgraph common["commonMain"]
        direction TB
        PUB["PUBLIC — com.superwall.sdk.kmp<br/>Superwall (singleton object)<br/>models.* (Pigeon catalog, no P prefix)<br/>SuperwallDelegate · PurchaseController ·<br/>PaywallPresentationHandler · SuperwallError"]
        INT["INTERNAL<br/>interface SuperwallBridge (≈ HostApi role)<br/>expect fun createSuperwallBridge()  ← the ONLY expect<br/>DelegateMultiplexer · StreamHolder · EntitlementPriority"]
        PUB --> INT
    end
    INT --> A["androidMain (actual)<br/>AndroidSuperwallBridge<br/>adapters + mappers<br/>(import-aliased com.superwall.sdk.*)"]
    INT --> I["iosMain (actual)<br/>IosSuperwallBridge<br/>adapters + mappers<br/>(cinterop bridge types, internal only)"]
    A --> NA["superwall-android 2.8.0<br/>(Maven, implementation — bundled transitively)"]
    I --> NI["SuperwallKMPBridge.xcframework<br/>(our @objc Swift bridge; cinterop compile-only;<br/>consumer app links it via SPM/CocoaPods)"]
    NI --> SWK["SuperwallKit iOS 4.16.x<br/>(SPM dependency of the bridge, exact-pinned)"]
```

The `SuperwallBridge` interface plays exactly the role the Pigeon `PSuperwallHostApi` + the two native `SuperwallHost` classes play in the Flutter plugin — but it is an ordinary in-process Kotlin interface with real object references instead of hostId routing. The public `Superwall` object is a thin façade over it: default arguments, the pre-configure guard (with its exemption list, §7), delegate multiplexing, and stream ownership live in common code **once**, not per platform. `StreamHolder` **owns** the public flows in common code (they exist and are collectable before configure); platform bridges *attach* native sources to them at configure time (§4 "Stream wiring") — this is deliberately not a straight port, because the Flutter hosts only register their EventChannels during `configure`.

> **Resolved (bridge seam):** Proposals 1 & 2 used an `internal interface SuperwallBridge` + a single `internal expect fun createSuperwallBridge()`; Proposal 3 used `internal expect object SuperwallHost`. We take the **interface + factory**: an interface is mockable (`FakeBridge` contract tests for all façade logic in `commonTest`), avoids `expect object` restrictions (no supertypes on expects, awkward state), and keeps exactly one `expect` declaration in the whole module.

### 2.2 Source-set layout

```
superwall-kmp/
  build.gradle.kts                       # exists — extend: explicitApi(), deps, cinterop
  src/
    commonMain/kotlin/com/superwall/sdk/kmp/
      Superwall.kt                       # public singleton façade
      SuperwallDelegate.kt               # interface — ALL 15 methods default no-op
      PurchaseController.kt              # interface — 3 suspend methods
      PaywallPresentationHandler.kt      # mutable closure holder (builder-style setters)
      SuperwallError.kt                  # sealed exception hierarchy
      models/
        options/    SuperwallOptions.kt, PaywallOptions.kt, Logging.kt, RestoreFailed.kt,
                    OptionEnums.kt       # TestModeBehavior, NetworkEnvironment, LogLevel, LogScope,
                                         # TransactionBackgroundView, ConfigurationStatus, DeviceTier
        identity/   IdentityOptions.kt
        paywall/    PaywallInfo.kt, Product.kt, LocalNotification.kt, ComputedPropertyRequest.kt,
                    Survey.kt, PaywallEnums.kt   # FeatureGatingBehavior, PaywallCloseReason, ...
        store/      StoreProduct.kt, StoreTransaction.kt
        entitlements/ Entitlement.kt, Entitlements.kt, SubscriptionStatus.kt, CustomerInfo.kt,
                    EntitlementEnums.kt  # EntitlementType, ProductStore, LatestSubscriptionState/OfferType
        results/    PurchaseResult.kt, RestorationResult.kt, RestoreType.kt, TriggerResult.kt,
                    PresentationResult.kt, PaywallResult.kt, PaywallSkippedReason.kt,
                    PaywallPresentationRequestStatus.kt
        triggers/   Experiment.kt, Variant.kt, ConfirmedAssignment.kt
        redemption/ RedemptionResult.kt, RedemptionInfo.kt, Ownership.kt, StoreIdentifiers.kt,
                    PurchaserInfo.kt, ErrorInfo.kt, ExpiredCodeInfo.kt, RedemptionPaywallInfo.kt
        events/     SuperwallEventInfo.kt, EventType.kt, IntegrationAttribute.kt
        callbacks/  CustomCallback.kt, CustomCallbackResult.kt
      internal/
        SuperwallBridge.kt               # internal interface + internal expect fun createSuperwallBridge()
        DelegateMultiplexer.kt           # always-installed native delegate → user delegate + StateFlows
        StreamHolder.kt                  # common-owned MutableStateFlow/SharedFlow (seeded pre-configure,
                                         # native source attached at configure) + bridge CoroutineScope
        EntitlementPriority.kt           # Entitlement.mergePrioritized port (pure common logic)
    commonTest/kotlin/…                  # model tests, sealed exhaustiveness, mergePrioritized,
                                         # FakeBridge contract tests for the façade
    androidMain/kotlin/com/superwall/sdk/kmp/internal/
      SuperwallBridge.android.kt         # actual fun createSuperwallBridge() = AndroidSuperwallBridge
      AndroidSuperwallBridge.kt          # port of Flutter SuperwallHost.kt (minus Pigeon)
      SuperwallInitializer.kt            # androidx.startup Initializer<Unit>
      CurrentActivityTracker.kt          # ActivityLifecycleCallbacks → native ActivityProvider
      AndroidSetup.kt                    # public fun Superwall.androidSetup(app) escape hatch
      adapters/  DelegateAdapter.kt, PurchaseControllerAdapter.kt,
                 PresentationHandlerAdapter.kt, OnBackPressedAdapter.kt
      mappers/   OptionsMapper.kt, PaywallInfoMapper.kt, EventMapper.kt,
                 SubscriptionStatusMapper.kt, EntitlementMapper.kt, StoreMapper.kt,
                 ResultMappers.kt, RedemptionMapper.kt, AnySanitizer.kt
    androidMain/AndroidManifest.xml      # startup provider metadata
    androidMain/consumer-rules.pro       # R8 keep rules: SuperwallInitializer (reflective discovery)
    androidHostTest/  androidDeviceTest/ # already scaffolded by the AGP KMP plugin
    iosMain/kotlin/com/superwall/sdk/kmp/internal/
      SuperwallBridge.ios.kt             # actual fun createSuperwallBridge() = IosSuperwallBridge
      IosSuperwallBridge.kt              # thin forwarder to the Swift bridge (heavy lifting lives in Swift)
      adapters/  DelegateAdapter.kt      # NSObject subclass : bridge delegate protocol
                 PurchaseControllerAdapter.kt   # : bridge purchase-controller protocol
                 PresentationHandlerAdapter.kt
      interop/   Continuations.kt        # completion-block → suspendCancellableCoroutine helpers
                 NSAnySanitizer.kt       # NSDictionary/NSArray/NSNumber ↔ Map<String, Any?>
      mappers/   (same file names as androidMain, mapping bridge cinterop types)
    iosTest/kotlin/…                     # simulator: link smoke + mapper tests vs the real bridge
    nativeInterop/cinterop/SuperwallKMPBridge.def
  bridge/                                # SuperwallKMPBridge — Swift package (SPM), our @objc facade
    Package.swift                        # depends on SuperwallKit iOS, exact-pinned
    Sources/SuperwallKMPBridge/          # full Swift internally; @objc-only public surface:
      SuperwallKMPBridge.swift           #   control-plane facade (configure/identify/register/…)
      BridgeDelegate.swift               #   @objc delegate protocol + typed event envelope classes
                                         #   (ports SuperwallDelegateHost.swift's associated-value
                                         #    destructuring switch — in Swift, where it belongs)
      BridgePurchaseController.swift     #   @objc purchase-controller protocol
      Envelopes.swift                    #   @objc classes for enum-with-associated-value payloads
    Tests/                               # XCTest: envelope destructuring vs real SuperwallKit types
  sample/  androidApp/  iosApp/          # Compose MP demo; iosApp SPM-links SuperwallKMPBridge
  docs/    IMPLEMENTATION_PLAN.md, MODELS.md (Pigeon → KMP cross-reference + ratified Deltas table),
           bridge-surface.md (bridge API map), COMPATIBILITY.md
```

**Build file additions:** `explicitApi()`; commonMain `api(kotlinx-coroutines-core)` (**`api`, not `implementation`** — `Flow`/`StateFlow` are in the public surface); androidMain deps per §4; per-target cinterop per §5. Enable kotlinx-binary-compatibility-validator before 1.0.

**Package/collision rule:** public code lives in `com.superwall.sdk.kmp` — deliberately disjoint from native Android's `com.superwall.sdk`. Only androidMain mapper/adapter files see both worlds, and they **must** import-alias every native type (`import com.superwall.sdk.models.entitlements.Entitlement as NativeEntitlement`). A Detekt/lint rule bans un-aliased `com.superwall.sdk.*` (non-`.kmp`) imports outside `internal/adapters` and `internal/mappers` — the collision hazard is chronic (nearly every stripped model name matches a native type), so it needs tooling, not just convention. iosMain interop names arrive from the bridge module with `SWB`-prefixed ObjC names (our convention in `SuperwallKMPBridge`); collisions are structurally impossible there.

---

## 3. Public API design

### 3.1 `Superwall` façade (representative signatures)

```kotlin
package com.superwall.sdk.kmp

public object Superwall {

    // ---- Configuration ---------------------------------------------------
    /** Identical signature on Android & iOS. No Context param: androidx.startup
     *  captures the Application on Android. Fire-and-forget; completion reports
     *  the configuration result. Android: superwall-android's configure completion
     *  carries Result<Unit> directly. iOS: native completion is a bare `(() -> Void)?`
     *  (verified: the Flutter host at SuperwallHost.swift:25-47 hardcodes success:true),
     *  so the bridge DERIVES the result — when the native completion fires, it reads
     *  Superwall.configurationStatus and maps .configured → success,
     *  .failed → failure(SuperwallError.ConfigurationFailed), .pending → treated as
     *  success-with-logged-warning (should not occur post-completion). An upstream
     *  request for a completion-with-result (or ObjC-visible failure reason) is on the
     *  Phase 1 upstream list (§5.3); until it lands, iOS failure detail is limited to
     *  the status enum. This asymmetry is documented in KDoc and COMPATIBILITY.md.
     *  Re-invocation: a second configure() call is a no-op that logs a warning via
     *  handleLog (matching both natives); the multiplexer install is idempotent (§7). */
    public fun configure(
        apiKey: String,
        purchaseController: PurchaseController? = null,
        options: SuperwallOptions? = null,
        completion: ((Result<Unit>) -> Unit)? = null,
    )
    /** Suspend twin — resumes when the native SDK reports configuration complete,
     *  with the same per-platform result derivation as configure's completion. */
    public suspend fun configureAndAwait(
        apiKey: String,
        purchaseController: PurchaseController? = null,
        options: SuperwallOptions? = null,
    )

    public var delegate: SuperwallDelegate?      // real object; null clears — no setDelegate(Boolean)
    public val configurationStatus: ConfigurationStatus   // guard-exempt (§7)
    public val isConfigured: Boolean                      // guard-exempt (§7)
    public val isInitialized: Boolean                     // guard-exempt (§7)
    public fun reset()

    // ---- Identity / attributes --------------------------------------------
    public val userId: String
    public val isLoggedIn: Boolean
    public fun identify(userId: String, options: IdentityOptions? = null)
    /** Snapshot getter. NOT symmetric with setUserAttributes — see below. */
    public val userAttributes: Map<String, Any?>
    /** MERGES into existing attributes (native semantics, both platforms): a null
     *  value REMOVES that key; absent keys are left untouched. This is deliberately
     *  a method, not a `var`, because set-then-get identity does not hold. */
    public fun setUserAttributes(attributes: Map<String, Any?>)
    public fun setIntegrationAttribute(attribute: IntegrationAttribute, value: String?)
    public fun setIntegrationAttributes(attributes: Map<IntegrationAttribute, String?>)
    public suspend fun getDeviceAttributes(): Map<String, Any?>
    public var localeIdentifier: String?
    public var logLevel: LogLevel                // enum end-to-end, not String

    // ---- Entitlements / subscription ---------------------------------------
    public val entitlements: Entitlements        // pure value snapshot — see §3.5 note
    /** Product-ID filtering is a façade call (it must reach the native SDK — the
     *  Flutter layer does this via a hidden nativeFilterCallback), NOT a method on
     *  the Entitlements value model; see §3.5 for the design decision. */
    public suspend fun getEntitlementsByProductIds(productIds: Set<String>): Set<Entitlement>
    public var subscriptionStatus: SubscriptionStatus            // synchronous get/set
    public val subscriptionStatusFlow: StateFlow<SubscriptionStatus>  // guard-exempt; seeded Unknown pre-configure (§4 stream wiring)
    public suspend fun getCustomerInfo(): CustomerInfo           // Android caveat: see §4
    public val customerInfoFlow: Flow<CustomerInfo>              // Android caveat: see §4
    public suspend fun confirmAllAssignments(): Set<ConfirmedAssignment>
    public suspend fun restorePurchases(): RestorationResult

    // ---- Presentation -------------------------------------------------------
    public fun register(                          // fire-and-forget, like both natives
        placement: String,
        params: Map<String, Any?>? = null,
        handler: PaywallPresentationHandler? = null,
        feature: (() -> Unit)? = null,
    )
    public suspend fun getPresentationResult(
        placement: String, params: Map<String, Any?>? = null,
    ): PresentationResult
    public suspend fun dismiss()
    public val isPaywallPresented: Boolean
    public val latestPaywallInfo: PaywallInfo?
    public fun preloadAllPaywalls()
    public fun preloadPaywalls(placementNames: Set<String>)
    public fun togglePaywallSpinner(isHidden: Boolean)
    /** Guard-EXEMPT (§7): both natives expose this as a static precisely so it can be
     *  called before/around configure — deep-link cold start is its primary use. */
    public fun handleDeepLink(url: String): Boolean
    public var overrideProductsByName: Map<String, String>?

    // ---- Platform-specific (documented no-op/echo on the other platform) ----
    public suspend fun consume(purchaseToken: String): String   // Play Billing; iOS echoes token
}
```

Ergonomic upgrade over Flutter: every native **sync** property (`userId`, `isLoggedIn`, `entitlements`, `logLevel`, `latestPaywallInfo`, …) is a direct property, not a `Future`; only genuinely-async natives are `suspend`.

> **Resolved (status naming):** `var subscriptionStatus` for sync get/set (matching both native SDKs) plus a distinctly-named `subscriptionStatusFlow: StateFlow` — avoids the property-vs-stream name collision Proposal 1 had, keeps Proposal 3's Compose-friendly shape.
> **Resolved (configure):** keep the fire-and-forget `configure` (Flutter/native parity) **and** add Proposal 1's `configureAndAwait` suspend twin — it is the sanctioned ordering tool given there is no pre-configure call queue.
> **Resolved (userAttributes):** getter property + `setUserAttributes` method with documented merge semantics — a symmetric `var` would misrepresent native behavior (set merges and null-removes; it does not replace), and the Pigeon contract itself models these as separate get/set methods.

### 3.2 Callback interfaces

```kotlin
/** ALL methods default no-op — users override selectively (fixes the Flutter
 *  inconsistency where only 4 of 15 had defaults).
 *  ANDROID AVAILABILITY CAVEAT: customerInfoDidChange and handleSuperwallDeepLink
 *  are declared in the common interface (Pigeon contract) but their Android wiring
 *  is UNVERIFIED — the Flutter Android host verifiably overrides neither (it
 *  implements only 13 of the 15 delegate methods; see §4 "Android delegate-hook
 *  audit"). Until the Phase 2 audit confirms superwall-android hooks exist, KDoc
 *  marks both as "@platform iOS (Android: pending upstream)". */
public interface SuperwallDelegate {
    public fun subscriptionStatusDidChange(from: SubscriptionStatus, to: SubscriptionStatus) {}
    public fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {}
    public fun handleCustomPaywallAction(name: String) {}
    public fun willPresentPaywall(paywallInfo: PaywallInfo) {}
    public fun didPresentPaywall(paywallInfo: PaywallInfo) {}
    public fun willDismissPaywall(paywallInfo: PaywallInfo) {}
    public fun didDismissPaywall(paywallInfo: PaywallInfo) {}
    public fun paywallWillOpenURL(url: String) {}
    public fun paywallWillOpenDeepLink(url: String) {}
    /** scope is the typed 22-value LogScope, not String (§3.4). Unmappable native
     *  level/scope strings fall back to LogLevel.DEBUG / LogScope.ALL with the raw
     *  string preserved under info["rawLevel"]/info["rawScope"] — degrade, never drop. */
    public fun handleLog(level: LogLevel, scope: LogScope, message: String?,
                         info: Map<String, Any?>?, error: String?) {}
    public fun willRedeemLink() {}
    public fun didRedeemLink(result: RedemptionResult) {}
    public fun handleSuperwallDeepLink(fullURL: String, pathComponents: List<String>,
                                       queryParameters: Map<String, String>) {}
    public fun customerInfoDidChange(from: CustomerInfo, to: CustomerInfo) {}
    public fun userAttributesDidChange(newAttributes: Map<String, Any?>) {}
}

/** One common interface covering both stores (mirrors the Flutter contract).
 *  Each platform bridge invokes only its store's method. */
public interface PurchaseController {
    public suspend fun purchaseFromAppStore(productId: String): PurchaseResult
    public suspend fun purchaseFromGooglePlay(
        productId: String, basePlanId: String?, offerId: String?,
    ): PurchaseResult
    public suspend fun restorePurchases(): RestorationResult
}

public class PaywallPresentationHandler {
    public fun onPresent(block: (PaywallInfo) -> Unit)
    public fun onDismiss(block: (PaywallInfo, PaywallResult) -> Unit)
    public fun onError(block: (String) -> Unit)
    public fun onSkip(block: (PaywallSkippedReason) -> Unit)
    /** The only value-returning callback; defaults to CustomCallbackResult.failure() when unset. */
    public fun onCustomCallback(block: suspend (CustomCallback) -> CustomCallbackResult)
}
```

### 3.3 Sealed-model conventions (pattern used everywhere)

```kotlin
public sealed interface SubscriptionStatus {
    public data class Active(val entitlements: Set<Entitlement>) : SubscriptionStatus
    public data object Inactive : SubscriptionStatus
    public data object Unknown  : SubscriptionStatus
    public val isActive: Boolean get() = this is Active
}

public sealed interface TriggerResult {
    public data object PlacementNotFound : TriggerResult
    public data object NoAudienceMatch   : TriggerResult
    public data class Paywall(val experiment: Experiment) : TriggerResult
    public data class Holdout(val experiment: Experiment) : TriggerResult
    public data class Error(val error: String) : TriggerResult  // nested → no kotlin.Error clash
}
```

Rules: `sealed interface` + nested subtypes; `data object` for empty cases (the Pigeon `ignore: Boolean?` hack disappears); generic names (`Active`, `Failed`, `Error`, `Unknown`, `Purchased`, `Device`, …) always nested inside their sealed parent (resolves stdlib collisions and matches native SDK style); Dart subtype-name prefixes dropped in favor of nesting (`RedemptionResult.Success`, `PaywallPresentationRequestStatusReason.Holdout`). Convenience factories match the Flutter public layer (`PurchaseResult.Failed`, `RestorationResult.Restored`, `CustomCallbackResult.success(data)`, `RestoreType.ViaPurchase(txn)` …). Document the Swift `.none` gotcha (`LogLevel.NONE`, `PaywallCloseReason.NONE`) in KDoc.

### 3.4 Deliberate deltas from the Flutter layer (all fidelity **fixes**, decided)

- `Experiment` carries its `variant: Variant` (Flutter's public type fakes an empty one).
- `TriggerResult` **and** `PaywallSkippedReason` are both sealed; `PaywallSkippedReason.Holdout(experiment)` keeps the payload Flutter drops.
- `SuperwallEventInfo` keeps the six silently-dropped fields (`userEnrichment`, `deviceEnrichment`, `message`, `integrationAttributes`, `reviewRequestedCount`, `missingProductIdentifiers`); `EventType` includes `paywallResourceLoadFail` (73 values); `attempt: Long?` stays numeric. **iOS fidelity is equal to Android's by construction:** the event payload's typed fields (`paywallInfo`, `transaction`, `product`, `result`, `survey`, `restoreType`, …) are Swift enum **associated values**, invisible across the ObjC boundary — so `SuperwallKMPBridge` destructures them **in Swift** (porting `SuperwallDelegateHost.swift`'s ~70-case switch) into typed `@objc` envelope classes that Kotlin maps 1:1 into `SuperwallEventInfo`. No upstream Superwall-iOS change is required; the envelope design is a Phase 1 deliverable (§5.3, §9).
- Not replicated: the `"${placement}handler"` feature-hostId suffix mismatch, `ConfigureCompletionProxy` dropping the success bool, `transactionRestore` mislabeled as `transactionComplete`, dropped `type`/`product` on transaction events, `LocalNotification.id` hardcoded `""`/default-`0` (now required `String`), the OnBackPressed return-value/comment disagreement (semantics defined and tested explicitly).
- `logLevel` is the `LogLevel` enum end-to-end (wire API was stringly-typed). `handleLog` is **fully** typed: `scope` is the 22-value `LogScope` enum, not `String` (the enum already exists in the catalog — leaving scope stringly-typed was an oversight). Unmappable native level/scope strings degrade to `LogLevel.DEBUG` / `LogScope.ALL` with the raw string preserved in `info["rawLevel"]`/`info["rawScope"]` (consistent with the §7 degrade-never-crash rule).
- `Logging`'s client-side helpers are **ported, not dropped**: the Flutter public `Logging` carries `handleLogRecord` + `debug/info/warn/error` convenience methods (verified at `/home/user/Superwall-Flutter/lib/src/public/SuperwallOptions.dart:138-163`); the KMP `Logging` class keeps equivalents (`handleLogRecord(level, message, error?)` + the four level shorthands) so app-side log routing ports one-to-one.
- `enableExperimentalDeviceVariables` is actually wired (dead in Flutter).
- **Dates:** `kotlin.time.Instant` everywhere (stable in Kotlin 2.3 — no kotlinx-datetime dependency). *Resolved:* over Proposal 3's epoch-ms-Long-plus-accessors — one typed convention; mappers convert the Flutter codebase's epoch-ms (CustomerInfo family) and ISO-8601-string (StoreTransaction/StoreProduct) conventions at the boundary.
- **Set semantics** where set-like: `Entitlements.active/inactive/all/web`, `SubscriptionStatus.Active.entitlements`, `confirmAllAssignments()`, `preloadPaywalls`, `getEntitlementsByProductIds`.
- Options classes are non-null-with-defaults data classes (Flutter public style); `preloadDeviceOverrides: Map<DeviceTier, Boolean>` with a typed `DeviceTier` enum; `PaywallOptions.onBackPressed: ((PaywallInfo?) -> Boolean)?` as a real closure (Android-only, documented; threading in §6).
- Platform-only members stay in the common surface with KDoc `@platform` notes and documented no-op/echo semantics: `shouldBypassAppTransactionCheck`/`maxConfigRetryCount` iOS-only; `useMockReviews`/`preloadDeviceOverrides`/`onBackPressed`/`consume` Android-only; `customerInfoFlow`/`customerInfoDidChange`/`handleSuperwallDeepLink` pending the Android delegate-hook audit (§4).
- Untyped payloads are `Map<String, Any?>` with a documented value contract (String/Boolean/Long/Double/List/Map/Set); each platform ships an Any-sanitizer ported from `mapParamsForDart` (unknown → `toString()`, never crash).
- Pure client logic ports to commonMain: `Entitlement.mergePrioritized(Set<Entitlement>)` + priority comparator, `SubscriptionStatus.isActive`.

**Contract-deviation governance.** The task mandate is "same external models and interfaces as the Pigeon definition, minus P" — and this section deliberately violates it item by item (renames like `registerPlacement`→`register` and `preloadPaywallsForPlacements`→`preloadPaywalls`; type changes like epoch-`Long`→`Instant`, `List`→`Set`, `String` log level→enum; added fields like `Experiment.variant`; dropped models like `SubscriptionStatusType`/`TransactionProduct`). Each deviation is therefore **individually ledgered**: `docs/MODELS.md` gains a mandatory **Deltas table** — one row per deviation with columns {Pigeon shape, KMP shape, category (rename/type/add/drop), rationale, sign-off}. The table is the Phase 2 API-review artifact (Open Question #10): every row must carry an explicit approve/revert decision before Phase 2 exits, and Phase 2's validation criteria include "Deltas table complete and ratified" (see §9). Nothing ships under the generic label "fidelity fix" without a row.

### 3.5 Full model catalog (Pigeon → KMP, P stripped, grouped by area)

Maintained as `docs/MODELS.md` with a per-field cross-reference **plus the ratified Deltas table (§3.4)**; summary:

| Area | Models (data classes / sealed / enums) |
|---|---|
| **Configuration** | `SuperwallOptions`, `PaywallOptions`, `RestoreFailed`, `Logging` (incl. ported `handleLogRecord`/`debug/info/warn/error` helpers); enums `TestModeBehavior`, `NetworkEnvironment`, `LogLevel`, `LogScope` (22), `TransactionBackgroundView`, `ConfigurationStatus`, `DeviceTier` (typed key for `preloadDeviceOverrides`) |
| **Identity** | `IdentityOptions` |
| **Paywall** | `PaywallInfo` (~30 fields, all nullable), `Product`, `LocalNotification` + `LocalNotificationType`, `ComputedPropertyRequest` + `ComputedPropertyRequestType` (10), `Survey`, `SurveyOption`, `SurveyShowCondition`; enums `FeatureGatingBehavior`, `PaywallCloseReason` |
| **Store** | `StoreTransaction` (13 fields, dates → `Instant`), `StoreProduct` (41 fields, dates → `Instant`) |
| **Entitlements / customer** | `Entitlement` (13 fields), `Entitlements` (**pure value snapshot** — see note below), `CustomerInfo`, `SubscriptionTransaction`, `NonSubscriptionTransaction`; sealed `SubscriptionStatus` {`Active(Set<Entitlement>)`, `Inactive`, `Unknown`}; enums `EntitlementType`, `ProductStore`, `LatestSubscriptionState`, `LatestSubscriptionOfferType` |
| **Purchase/restore results** | sealed `PurchaseResult` {`Purchased`, `Cancelled`, `Pending`, `Failed(error)`}; sealed `RestorationResult` {`Restored`, `Failed(error)`}; sealed `RestoreType` {`ViaPurchase(StoreTransaction?)`, `ViaRestore`} |
| **Trigger / presentation** | `Experiment` (with `variant`), `Variant` + `VariantType`, `ConfirmedAssignment`; sealed `TriggerResult` (5 cases incl. nested `Error`); sealed `PresentationResult` (5 cases incl. `PaywallNotAvailable`); sealed `PaywallResult` {`Purchased(productId)`, `Declined`, `Restored`}; sealed `PaywallSkippedReason` {`Holdout(experiment)`, `NoAudienceMatch`, `PlacementNotFound`}; enum `PaywallPresentationRequestStatusType`; sealed `PaywallPresentationRequestStatusReason` (9 cases, nested, prefix-free) |
| **Redemption** | sealed `RedemptionResult` {`Success`, `Error`, `ExpiredCode`, `InvalidCode`, `ExpiredSubscription`}; `RedemptionInfo`, `PurchaserInfo`, `ErrorInfo`, `ExpiredCodeInfo`, `RedemptionPaywallInfo`; sealed `Ownership` {`AppUser`, `Device`}; sealed `StoreIdentifiers` {`Stripe`, `Paddle`, `Unknown`} |
| **Custom callbacks** | `CustomCallback`, `CustomCallbackResult` + `CustomCallbackResultStatus` |
| **Events** | `SuperwallEventInfo` (flat envelope, ALL fields — iOS fidelity caveat in §3.4), `EventType` (73 values), `IntegrationAttribute` (21 values) |
| **Dropped entirely** | `OnBackPressedHost`, `PurchaseControllerHost`, `ConfigureCompletionHost`, `PaywallPresentationHandlerHost`, `FeatureHandlerHost`, `ignore` fields, `SubscriptionStatusType`, `SuperwallBuilder`, `TransactionProduct`, deprecated typedefs |

> **Resolved (`Entitlements` vs. `byProductIds`):** `Entitlements` is a **pure immutable snapshot data class** (`active`/`inactive`/`all`/`web`) with value equality — it never carries a live bridge reference (which would break equality, snapshotting, and testability). Product-ID filtering — which must reach the native SDK (the Flutter layer does it via a hidden `nativeFilterCallback` for exactly this reason) — lives on the façade as `suspend fun Superwall.getEntitlementsByProductIds(productIds: Set<String>): Set<Entitlement>`, routed through the bridge. On Android this local-filters with a warning until upstream support exists (§4).

### 3.6 Internal bridge

```kotlin
internal interface SuperwallBridge {
    fun configure(apiKey: String, purchaseController: PurchaseController?,
                  options: SuperwallOptions?, completion: (Result<Unit>) -> Unit)
    fun setDelegate(delegate: DelegateMultiplexer?)   // multiplexer, installed once (idempotent — §7)
    fun register(placement: String, params: Map<String, Any?>?,
                 handler: PaywallPresentationHandler?, feature: (() -> Unit)?)
    suspend fun getPresentationResult(placement: String, params: Map<String, Any?>?): PresentationResult
    fun setUserAttributes(attributes: Map<String, Any?>)   // merge semantics (§3.1)
    suspend fun getEntitlementsByProductIds(productIds: Set<String>): Set<Entitlement>
    fun attachStreams(holder: StreamHolder)   // called once at configure-complete (§4 stream wiring)
    // … one member per façade member (~40 total, mirroring the Pigeon HostApi),
    // taking/returning ONLY commonMain model types
}
internal expect fun createSuperwallBridge(): SuperwallBridge
```

The façade owns: the `DelegateMultiplexer` (installed into the native SDK exactly once at configure, idempotently on repeat configure calls; forwards to the user `delegate` if set **and** feeds the flows — fixing Flutter's `setDelegate(bool)` artifact and stream re-wrap quirk), the user `PurchaseController` reference, the `StreamHolder` (flows exist pre-configure; §4), and the pre-configure guard **with its exemption list** (§7).

---

## 4. Android implementation strategy

**Dependencies (androidMain):** `implementation("com.superwall.sdk:superwall-android:2.8.0")` — **`implementation`, never `api`**: native types stay hidden; the dependency still propagates through published module metadata so consumers add nothing. Plus `androidx.startup:startup-runtime`, `kotlinx-coroutines-android`.

**minSdk (Phase 0 gate).** The KMP scaffold sets `android-minSdk = 24` (`/home/user/Superwall-KMP/gradle/libs.versions.toml:4`), but the Flutter plugin builds against superwall-android 2.8.0 with `minSdkVersion 26` (`/home/user/Superwall-Flutter/android/build.gradle:46`; the Flutter repo's CLAUDE.md also states min SDK 26). If the superwall-android AAR declares `minSdk > 24`, every consumer at minSdk 24–25 hits a manifest-merger failure. Phase 0 therefore **inspects the AAR's `AndroidManifest.xml` `uses-sdk` declaration and builds the consumer fixture at minSdk 24 explicitly**; if the merge fails, the module moves to minSdk 26 (preferred over `tools:overrideLibrary`, which merely hides the incompatibility) — decision recorded as Open Question #11 and resolved before any other Android work.

**Context/Activity (replaces the Flutter plugin lifecycle):**
- `SuperwallInitializer : androidx.startup.Initializer<Unit>` (declared in the library manifest) captures the `Application` and calls `registerActivityLifecycleCallbacks(CurrentActivityTracker)` (WeakReference to the resumed Activity).
- `AndroidSuperwallBridge.configure` calls static `NativeSuperwall.configure(application, apiKey, mappedOptions, purchaseControllerAdapter, ActivityProvider { CurrentActivityTracker.current }, completion)` — the same shape as `SuperwallHost.kt`, minus Pigeon. So common `configure(apiKey, …)` needs no Context.
- Escape hatch for apps that strip startup initializers: `public fun Superwall.androidSetup(application: Application)` — the one intentional platform-specific public function (androidMain extension).
- **Failure mode when androidx.startup is stripped** (common with WorkManager `InitializationProvider` removal): `configure()` on Android checks whether an `Application` was captured; if not, it fails **immediately and actionably** with `SuperwallError.NotInitialized("Superwall's androidx.startup initializer did not run — call Superwall.androidSetup(application) from Application.onCreate()")` instead of proceeding with no `Application`/`ActivityProvider` and crashing later inside the native SDK. Covered by a Robolectric test that disables the provider (Phase 2 validation). The module ships `consumer-rules.pro` keeping `SuperwallInitializer` (androidx.startup discovers initializers reflectively; R8 full mode can strip them), and the README documents both the keep rule and the `tools:node="remove"` pitfall.

**Method mapping:** near-mechanical port of `/home/user/Superwall-Flutter/android/.../SuperwallHost.kt`:
- Sync property passthroughs stay synchronous (`logLevel`, `userId`, `isLoggedIn`, `localeIdentifier`, `overrideProductsByName`, `latestPaywallInfo`, `entitlements`, `configurationState`, extension-function APIs `identify`/`setUserAttributes`/`register`).
- Native suspend + `Result<T>` APIs (`confirmAllAssignments`, `getPresentationResult`, `restorePurchases`, `consume`, `dismiss`, `deviceAttributes`) are called directly from the KMP suspend functions — the caller's coroutine suspends; no IO-scope + callback dance. Wrap in `withContext(Dispatchers.IO)` only where the native call is not documented main-safe (default: keep the Flutter host's IO hop as the conservative default and remove per-call once verified). Failures → thrown `SuperwallError.Native(cause)` except where the domain type already models failure.

**Stream wiring (deliberately NOT a straight port — the one façade feature the Flutter host cannot supply a pattern for, since it only registers EventChannels during `configure`):**
- `StreamHolder` (commonMain) owns `MutableStateFlow<SubscriptionStatus>(Unknown)` and a `MutableSharedFlow<CustomerInfo>` from module init; the façade exposes `.asStateFlow()`/`.asSharedFlow()` — so `subscriptionStatusFlow` is collectable (emitting `Unknown`) before configure, with **no** eager `stateIn` over a native source that does not yet exist (`Superwall.instance.subscriptionStatus` **throws** before configure — the naive `stateIn(bridgeScope, Eagerly, …)` design was wrong).
- At configure-complete, `bridge.attachStreams(holder)` performs the lazy attach: Android launches a collector on `Superwall.instance.subscriptionStatus.map { it.toKmp() }` writing into the holder's `MutableStateFlow`, plus an initial sync read to close the seed gap; iOS does the equivalent from delegate callbacks + initial sync getter (§5.3). Attach is idempotent (repeat configure re-uses the existing collector). FakeBridge contract tests cover: pre-configure `Unknown`, seed-to-real transition at attach, and no duplicate collectors on double configure.
- `customerInfoFlow` — **Android source unverified; see the delegate-hook audit below.**

**Android delegate-hook audit (Phase 2, blocking for three surface members).** The Flutter Android `SuperwallDelegateHost.kt` verifiably overrides only 13 of the 15 delegate methods — **neither `customerInfoDidChange` nor `handleSuperwallDeepLink`** — and `SuperwallHost.kt:80` extends only `StreamSubscriptionStatusStreamHandler`; the `streamCustomerInfo` EventChannel is **never registered on Android**. There is no evidence superwall-android 2.8.0's `SuperwallDelegate` even has these hooks. Phase 2 therefore audits the superwall-android 2.8.0 delegate interface directly: if the hooks exist, `DelegateAdapter` wires them and `customerInfoFlow` is fed from `customerInfoDidChange` into the holder's `MutableSharedFlow`; if they do not, then `customerInfoFlow` (empty on Android), `customerInfoDidChange`, and `handleSuperwallDeepLink` join the documented Android platform-gap list below (KDoc `@platform iOS`, `handleLog` warning on first collection/registration), with upstream superwall-android issues filed alongside `getCustomerInfo`. Until the audit lands they are presented as **pending**, not working.

**Adapters:** `DelegateAdapter` implements native `com.superwall.sdk.delegate.SuperwallDelegate`, maps payloads, forwards to the multiplexer on `Dispatchers.Main`. `PurchaseControllerAdapter` implements the native `PurchaseController`; its `purchase(activity, productDetails, basePlanId, offerId)` extracts `productDetails.productId` and calls the user's `suspend purchaseFromGooglePlay(...)` **directly** — both sides are coroutines, no `suspendCoroutine` shim (unlike the Flutter host). `PresentationHandlerAdapter` builds a native `PaywallPresentationHandler` whose closures forward to the common handler; `onCustomCallback` awaits the user's suspend lambda. `OnBackPressedAdapter` invokes the user's `onBackPressed` closure **synchronously on the invoking thread** (§6 exception).

**Known native gaps, surfaced honestly (documented per-field in KDoc; upstream superwall-android issues filed in the parity phase):**
- `getCustomerInfo()` — synthesized minimal `CustomerInfo(userId = …)` with empty lists (Flutter parity). *Resolved:* keep the synthesized stub as default rather than Proposal 2's throw — throwing would break common code paths that work on iOS; log a `handleLog` warning on every call. Escalated as Open Question #1 (product call).
- `customerInfoFlow` / `customerInfoDidChange` / `handleSuperwallDeepLink` — **pending the delegate-hook audit above**; if superwall-android lacks the hooks: no emissions/invocations on Android, logged warning, upstream issue.
- `getEntitlementsByProductIds` — local-filter fallback + warning (Android `Entitlement` carries only id/type).
- `StoreProduct.subscriptionGroupIdentifier`/`isFamilyShareable` — null/false; `Entitlement.type` always `SERVICE_LEVEL`.

**Threading:** delegate/handler/feature callbacks → `Dispatchers.Main`; `onBackPressed` synchronous same-thread (§6); suspend bridging per the common contract in §6.

---

## 5. iOS implementation strategy

### 5.1 Decision: self-authored `@objc` Swift bridge (`SuperwallKMPBridge`), prebuilt XCFramework, cinterop compile-only; consumer app links the bridge (PurchasesHybridCommon pattern)

> **Revised decision.** The original draft cinterop'd directly against SuperwallKit's shipped `@objc(SWK…)` layer, accepting whatever that surface exposes and filing upstream `@objc` PRs for the rest. Two findings changed the call: (1) the `SuperwallEvent` associated-value payloads — the largest data surface in the SDK — are Swift-only and would have made v1 event fidelity hostage to upstream Superwall-iOS releases; (2) Kotlin's first-party SwiftPM import would remove the hand wiring but is Alpha on a beta compiler, which we won't bet a 1.0 on (team decision). A bridge we own resolves both on stable tooling.

Kotlin/Native still parses only ObjC (and C) headers — no direct Swift import exists on stable Kotlin. So the boundary is an Objective-C header either way; the decision is **whose** header. We author it:

- `bridge/` in this repo is a Swift package, `SuperwallKMPBridge`, depending on SuperwallKit iOS (SPM, exact-pinned). Internally it is unrestricted Swift — it destructures enums with associated values, touches structs, awaits async APIs — exactly like the Flutter plugin's `ios/Classes/*.swift` host does today, which is our porting spec. Its **public surface is 100% `@objc`** (`SWB`-prefixed ObjC names), shaped 1:1 for the `SuperwallBridge` interface: flat classes, completion handlers, typed envelope objects.
- The KMP iosMain cinterops against the bridge's generated `SuperwallKMPBridge-Swift.h` from a **prebuilt `SuperwallKMPBridge.xcframework`** (built in our macOS CI). `IosSuperwallBridge` in Kotlin becomes a thin forwarder; the semantic work lives in Swift, where the full SuperwallKit API is visible.
- Rationale over direct-SWK cinterop: full API reach on day one (no upstream coupling, no fidelity-reduced v1), the event-envelope switch is a Swift-to-Swift port of proven Flutter host code instead of a lossy Kotlin re-implementation, and SuperwallKit version bumps are absorbed inside the bridge without breaking the klib's cinterop ABI (the bridge header only changes when *we* change it).
- Cost accepted: a Swift build step in CI (macOS runner already required for iOS klibs), a second versioned artifact to release, and the consumer's iOS-side dependency becomes `SuperwallKMPBridge` instead of `SuperwallKit` (same step count; better pinning — the bridge pins SuperwallKit exactly).
- Rejected: **first-party SwiftPM import** (Alpha, Kotlin 2.4.20-Beta — revisit at stable, see v2); **Kotlin CocoaPods plugin** (pod deps don't propagate through Maven publication → forces CocoaPods + the plugin on every consumer); **runtime injection** (re-ships the host layer as user code); **static embedding now** (right end-state, wrong first step — requires folding in SuperwallKit + Superscript's binary slices at publish time). Direct-SWK cinterop remains the documented **fallback** if the bridge pipeline fails validation in Phase 1; static embedding remains the **v2 end-state**.

### 5.2 Mechanics

- **Bridge build:** `xcodebuild archive` per platform (device + simulator) from `bridge/Package.swift`, `BUILD_LIBRARY_FOR_DISTRIBUTION=NO` (Kotlin consumes the ObjC header, not the Swift interface; the bridge is rebuilt in lockstep with the klib, so module stability is unnecessary) → `xcodebuild -create-xcframework` → `SuperwallKMPBridge.xcframework.zip` + checksum, attached to each superwall-kmp GitHub release. SuperwallKit + Superscript's `libcel.xcframework` resolve via SPM during the archive; they are **linked by the consumer app**, not folded into the bridge binary (dynamic SuperwallKit; the bridge declares it as a dependency in its distribution manifest so SPM/CocoaPods pulls it transitively).
- `nativeInterop/cinterop/SuperwallKMPBridge.def`: `language = Objective-C`, `modules = SuperwallKMPBridge`, generated-bindings package `com.superwall.sdk.kmp.internal.ios.interop`. A checksum-verified `buildOrDownloadBridgeXCFramework` Gradle task (local: builds from `bridge/` when Xcode is present; CI/clean machines: downloads the release asset) unpacks per-target slices (`ios-arm64` device; `ios-arm64_x86_64-simulator` for both simulator targets) and wires `compilerOpts("-F…", "-framework", "SuperwallKMPBridge")` per target.
- The klib carries **bindings only, no binary**. All cinterop types stay `internal`; no `export()`; `@OptIn(ExperimentalForeignApi::class)` confined to iosMain — consumers never see ObjC types.
- **Version pins, two layers:** (1) `bridge/Package.swift` pins SuperwallKit **exact** — consumers can't drift it independently; (2) the klib ↔ bridge pair must match: the bridge exposes `SWBBridgeVersion`, and `configure()` asserts it against the compiled-against constant (hard log warning by default; see Open Question #4) — a mismatch is otherwise a runtime unrecognized-selector crash, not a compile error. `SUPERWALLKIT_IOS_VERSION` and `BRIDGE_VERSION` are stamped into the POM and the README compatibility table.
- **`configure` completion result (bridge-designed):** native `Superwall.configure(...completion:)` takes a bare `(() -> Void)?` — verified at `/home/user/Superwall-Flutter/ios/Classes/SuperwallHost.swift:25-47`, where the Flutter host hardcodes `success: true`. The bridge fixes this **in Swift**: its `configure` completion carries an explicit `(status: SWBConfigurationStatus, error: NSError?)`, derived inside the bridge from post-completion `configurationStatus` (`.configured → success`, `.failed → ConfigurationFailed`, `.pending → success + warning`) with full Swift API visibility if richer failure detail becomes available upstream. Kotlin maps it straight to `Result<Unit>` — the asymmetry vs. Android collapses into the bridge and is documented in `COMPATIBILITY.md`.

### 5.3 Boundary routing rules (what cannot cross Swift → ObjC → Kotlin, and where it's handled)

The constraint set is unchanged — the *handling location* moves into the bridge, which is exactly why the bridge exists:

| Shape | Examples | Routing |
|---|---|---|
| Swift enum w/ associated values | `SubscriptionStatus`, `RedemptionResult`, `PaywallResult`, `PresentationResult`, `TriggerResult`, `PurchaseResult`, `RestorationResult`, `RestoreType`, `PaywallSkippedReason` | Bridge destructures in Swift → `@objc` envelope classes (`SWBRedemptionResult`, …); Kotlin maps envelopes to sealed types |
| **`SuperwallEvent` associated values** (the single largest payload in the SDK) | the ~70-case event enum's typed payloads: `paywallInfo`, `transaction`, `product`, `result`, `survey`, `restoreType`, `attempt`, … | `BridgeDelegate.swift` ports `SuperwallDelegateHost.swift`'s destructuring switch **in Swift** into a typed `@objc` `SWBEventEnvelope`; Kotlin maps it 1:1 into `SuperwallEventInfo`. Full fidelity on both platforms in v1; envelope design is a Phase 1 deliverable |
| Swift structs / non-`@objc` types | any struct-typed model | Bridge copies fields into `@objc` envelope classes |
| `async`-only funcs | `getCustomerInfo`, `getDeviceAttributes`, `confirmAllAssignments`, `dismiss` | Bridge wraps in `Task { }` → completion handler; Kotlin: `suspendCancellableCoroutine` |
| Bare `(() -> Void)` completions | `configure(...completion:)` | Bridge exposes completion-with-result (§5.2) |
| Combine / AsyncSequence | `$subscriptionStatus`, `customerInfoStream` | Bridge subscribes in Swift and forwards through `@objc` callback methods; Kotlin feeds the common `StreamHolder` flows at `attachStreams` (§4 stream wiring) + initial sync getter |
| Generics / `Set<T>` | `Set<Entitlement>` → untyped `NSSet` | Bridge emits typed `NSArray<SWB…>`; element-wise casts in Kotlin mappers |
| Default args | `register` overloads | Bridge exposes the fullest explicit overload; defaults live in commonMain |
| Optional primitives | `maxConfigRetryCount: Int?` | `NSNumber?` + sanitizer (objCType-based bool/int/double disambiguation) |
| Swift `Error`/`throws` | `PurchaseResult.failed(Error)` | Bridge converts to `NSError` with domain/code/description preserved; Kotlin maps to `String`/`SuperwallError` |
| Protocol default impls | optional delegate methods | Kotlin implements the full bridge protocol; user optionality = common interface defaults |

**Surface protocol:** the bridge's public header is the contract between the two halves of this SDK. Phase 1 designs it as `docs/bridge-surface.md` — one row per `SuperwallBridge` member mapping {Kotlin bridge member → `@objc` bridge API → SuperwallKit call}, replacing the old upstream gap matrix (there is no "missing upstream" state anymore; anything reachable in Swift is reachable, the only question is envelope shape). A CI check diffs the generated `SuperwallKMPBridge-Swift.h` against the committed reference header on every build — an unintentional surface change fails the build (we own drift now, so we police it). Upstream `@objc` PRs to Superwall-iOS are no longer on the critical path; file them only where they'd let the bridge delete code (nice-to-have cleanup).

**Adapters (Kotlin side):** `DelegateAdapter : NSObject(), <SWBBridgeDelegate protocol>` registered with the bridge, forwarding on `Dispatchers.Main`; event mapping is a mechanical envelope→`SuperwallEventInfo` field copy (the semantic switch lives in Swift, with the Flutter host's mislabels — `transactionRestore`-as-`transactionComplete` etc. — fixed in the port). `PurchaseControllerAdapter` implements the bridge's `@objc` purchase-controller protocol — its completion-handler methods `launch` on the bridge scope and call the user's suspend impl, **never blocking the main queue** (SuperwallKit invokes these `@MainActor`; blocking = deadlock — covered by an on-device test). No view-controller plumbing is needed: the Flutter iOS host has zero `UIViewController` references — SuperwallKit resolves the presenter itself.

### 5.4 What consumers must add (v1)

- **commonMain/Gradle:** `implementation("com.superwall.sdk:superwall-kmp:x.y.z")` — that's all for Android.
- **iOS app:** add **`SuperwallKMPBridge`** via **SPM (preferred; binary target in this repo's `Package.swift` pointing at the release XCFramework) or CocoaPods (podspec wrapping the same binary)**, version-matched to the superwall-kmp release per the compatibility table; build the Kotlin framework with `isStatic = true`. SuperwallKit and Superscript resolve transitively through the bridge's manifest at the exact pinned version. Two documented steps total, same as before — but the pin is now enforced by us, not documentation.
- **v2 removes the iOS step entirely** via static embedding (RevenueCat-3.0 pattern: fold SuperwallKit + libcel + the bridge into slices the klib `-include-binary`s).

---

## 6. Callback surfaces & threading rules (enforced in commonMain, documented on every interface)

1. **Delivery:** every SDK→app callback — delegate methods, presentation-handler closures, `feature` lambdas, `configure` completion, Flow emissions — is dispatched on **`Dispatchers.Main.immediate`**. **Sole exception — `onBackPressed` (Android):** the native hook is a *synchronous* `(PaywallInfo?) -> Boolean` whose return value must be produced on the invoking thread (shape verified in `OnBackPressedHost.kt:17-25`), so async dispatch is impossible; the user's closure is invoked **synchronously on the thread the native SDK calls it on** — the main thread, since it is driven by the paywall Activity's back handling; the adapter asserts main-thread in debug builds and the exception is documented on `PaywallOptions.onBackPressed` KDoc ("keep it fast and non-blocking; do not suspend or hop dispatchers").
2. **Main-safety:** all public `suspend` functions are callable from any dispatcher, including Main; bridges hop internally where needed (Android: IO around non-main-safe native suspends; iOS: continuation resumption off the completion queue).
3. **App→SDK suspend callbacks** (`PurchaseController.*`, `onCustomCallback`) run in one internal `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` per bridge (in `StreamHolder.kt`); the user's implementation may switch dispatchers freely. The iOS purchase-controller bridge **must launch, never `runBlocking`** — SuperwallKit calls it on the main actor.
4. **Handler lifetime:** each `register` call creates a fresh adapter that strongly retains its `handler`/`feature`; the adapter is retained Kotlin-side (critical on iOS — never rely on ObjC retaining Kotlin-implemented objects; SuperwallKit's delegate may be weak). Release semantics: the iOS Flutter host verifiably cleans up **only on `onDismiss` and `onSkip`** (`SuperwallHost.swift:299` + `ios/Classes/PaywallPresentationHandler.swift` — `onError` at line 50 performs no cleanup), so "release on error" is **not** a mirror of the host pattern. We adopt it anyway as a **declared deliberate behavior** (Deltas-table row): when a presentation errors, no paywall was presented, so no dismiss/skip will ever arrive and the never-released adapter is a leak in the Flutter host. Phase 1's spike verifies empirically whether SuperwallKit can emit further handler callbacks after `onError`; if it can, release stays dismiss/skip-only with bounded retention accepted and documented — the go/no-go on error-terminality is a Phase 1 output, tested either way (post-error callback delivery test in Phase 4). *Resolved:* per-registration adapters (Proposal 1) instead of a placement-keyed registry (Proposals 2/3) — direct object references make placement keying unnecessary and it was the source of the Flutter one-handler-per-placement aliasing bug.
5. **Exception safety:** user-callback exceptions are caught by adapters, logged via `handleLog`, and mapped to domain failures (`PurchaseResult.Failed(message)`, `CustomCallbackResult.failure()`); they never propagate into native SDK internals and never kill the supervisor scope. (For the synchronous `onBackPressed`, a thrown exception is caught, logged, and treated as `false` — "don't intercept".)
6. `register` is fire-and-forget (sync), matching both natives.

---

## 7. Error handling & result types

```kotlin
public sealed class SuperwallError(message: String?, cause: Throwable? = null)
    : Exception(message, cause) {
    public class NotConfigured : SuperwallError("Superwall.configure must be called first")
    public class NotInitialized(message: String) : SuperwallError(message)   // Android startup-stripped case (§4)
    public class NotSupportedOnPlatform(api: String, platform: String) : SuperwallError(…)
    public class ConfigurationFailed(message: String?) : SuperwallError(message)
    public class Native(message: String?, cause: Throwable? = null) : SuperwallError(message, cause)
}
```

> **Resolved:** sealed hierarchy (Proposals 2/3) over Proposal 1's single class — richer diagnostics at no API cost; still the *only* exception family thrown by public suspend functions.

- **Domain outcomes never throw** — they stay in the sealed result types (`PurchaseResult.Failed`, `RestorationResult.Failed`, `TriggerResult.Error`, `RedemptionResult.Error`, handler `onError(String)`).
- **Infrastructure failures throw `SuperwallError`**: Android wraps `kotlin.Result` failures; iOS wraps `NSError` (domain/code/localizedDescription preserved in the message).
- `configure` completion delivers `Result<Unit>` on **real** configuration completion. Android: the native completion carries `Result<Unit>` directly (fixes the dropped success bool). iOS: the native completion is a bare `(() -> Void)?`, so `SuperwallKMPBridge` derives a status-carrying completion in Swift per §5.2 (fixes the Flutter host's fire-immediately-with-hardcoded-success quirk); Kotlin maps it straight to `Result<Unit>`.
- **`configure` re-invocation semantics:** a second `configure()` call after a successful (or in-flight) first is a **no-op** — the completion is invoked with `Result.success` immediately (or queued behind the in-flight completion), a `handleLog` warning is emitted, and no options/controller/multiplexer re-install occurs (matching both natives' ignore-second-configure behavior; the multiplexer install is idempotent). Covered by a FakeBridge contract test.
- **Pre-configure access:** no call queue (Flutter's deliberate choice), but a **common guard in the façade** throws `SuperwallError.NotConfigured` consistently on both platforms. **Guard exemption list (explicit, tested):** `configure`/`configureAndAwait` (obviously), **`handleDeepLink`** — both natives expose it as a *static* precisely so it works before/around configure; deep-link cold start is its primary use and a blanket guard would break it — plus the introspection members `isConfigured`, `isInitialized`, `configurationStatus`, and the stream accessors `subscriptionStatusFlow`/`customerInfoFlow` (they return the common-owned, pre-seeded flows from `StreamHolder`; §4). Setting `delegate` pre-configure is also allowed (stored; installed at configure). Everything else guards. The exemption list is enumerated in one place (`Superwall.kt`) and exhaustively covered by FakeBridge contract tests. *Resolved:* Proposal 1's guard wins over "let native behavior leak" (Proposals 2/3) — native behavior diverges (Android's `Superwall.instance` throws, iOS may not), and platform-divergent crashes are the worst outcome. `configureAndAwait` is the sanctioned ordering tool; `isConfigured`/`configurationStatus` allow gating.
- **Mapping failures degrade, never crash** (forward-compat for version skew, inherent to the compile-only iOS binding): unknown native enum case → documented fallback (`SubscriptionStatus.Unknown`, `ConfigurationStatus.FAILED`, `LogLevel.DEBUG`/`LogScope.ALL` with raw string preserved per §3.4) + `handleLog` warning; unknown payload values → `toString()`.

---

## 8. Testing & example app strategy

| Layer | What | Where |
|---|---|---|
| commonTest | Model defaults, sealed exhaustiveness, `mergePrioritized` (port Dart tests), `FakeBridge` contract tests for all façade logic (guard **+ full exemption list incl. pre-configure `handleDeepLink`**, configure re-invocation no-op, multiplexer idempotence, stream seeding + attach transition + no-duplicate-collector, handler retention), Flow tests with virtual time | every phase |
| androidHostTest (JVM) | Mapper round-trips against **real native classes** (they don't need a device), options/status/event/redemption mappers, Robolectric initializer test **+ startup-stripped test (provider disabled → `configure` throws actionable `NotInitialized`)** | Phase 2+ |
| androidDeviceTest | configure-with-bad-key → `configurationStatus == FAILED` **and completion delivers `Result.failure`**, activity-tracker instrumentation, register smoke vs `com.superwall.superapp` | Phase 2+ |
| iosSimulatorArm64Test | Link smoke (instantiate bridge classes), mapper tests against real bridge envelope types, `NSAnySanitizer` tests (NSNumber bool/int/double ambiguity gets dedicated cases), continuation bridging, **configure-result path (bad key → bridge-delivered `ConfigurationFailed`)**. If SuperwallKit hard-requires network at configure, deep behavior moves to the sample-app UI layer | Phase 3+ |
| Bridge XCTest (Swift, in `bridge/Tests/`) | Envelope destructuring against real SuperwallKit types — the ~70-case event switch, enum-with-associated-value envelopes, configure-status derivation — tested **in Swift where full visibility exists** | Phase 1+ |
| On-device integration | Purchase-controller **main-actor re-entrancy/deadlock** test (StoreKit test session / Play sandbox), `onCustomCallback` round-trip, stream initial-value + transition coverage, **post-`onError` handler behavior (error-terminality verification per §6.4)** | Phase 4 |
| Sample app | `sample/` Compose Multiplatform demo (androidApp + iosApp with SPM-linked `SuperwallKMPBridge`): Configure / Identify / Register buttons, status label bound to `subscriptionStatusFlow`; grows to mirror the Flutter `test_app` scenario list; Maestro (Android) + XCUITest (iOS) flows ported from `test_app/maestro/` | Phase 3+ |

**CI (steady state):**
- **PR (~10 min):** Linux — ktlint/Detekt (incl. the alias-import rule), commonTest, androidHostTest, `assembleAndroid`; macOS — build the bridge XCFramework (cache keyed by (bridge-source hash, SuperwallKit pin, Xcode version)), bridge XCTests, compile all three iOS targets, iosSimulatorArm64Test, **bridge-header drift check** (generated `SuperwallKMPBridge-Swift.h` vs committed reference), `publishToMavenLocal` + consumer-fixture build **at the module's declared minSdk** (validates AGP-9 KMP-library metadata + transitive superwall-android **+ minSdk manifest-merge compatibility** — do this from Phase 0, not at release).
- **Nightly / opt-in label:** managed-device Android instrumentation, iOS simulator UI tests + Maestro, sample-app release builds.
- **Release:** runs on a **macOS runner** — this is a hard requirement, not an optimization: `publishToMavenCentral` for a KMP module with iOS targets must compile the iOS klibs, and the cinterop step needs the bridge XCFramework present. Pipeline: build + test `SuperwallKMPBridge.xcframework` → attach `SuperwallKMPBridge.xcframework.zip` + checksum to the GitHub release and update the repo-root `Package.swift` binary-target URL/checksum (+ podspec) → binary-compat check → macOS `publishToMavenCentral` (tag-driven) → compatibility-table entry; SuperwallKit + bridge pins stamped into POM + `SuperwallVersions.kt`. The klib and its exact bridge build ship from the same tag — they are one release unit. **Kotlin-compiler compatibility:** cinterop-bearing klibs pin consumers to a compatible Kotlin compiler range (klib ABI); `COMPATIBILITY.md` therefore carries a **supported consumer Kotlin version range column** per release (initially: the Kotlin version we build with plus the forward-compatibility window the klib ABI guarantees), alongside the superwall-android and SuperwallKit-iOS pins, and the release checklist re-verifies it on every Kotlin bump.
- **Cross-repo canary:** scheduled job building the bridge against latest Superwall-iOS `main` + the klib against latest superwall-android, catching upstream drift before release day (bridge compile errors against a new SuperwallKit are the *desired* early signal — they surface inside our Swift code instead of as consumer runtime crashes).

---

## 9. Phased milestones

> **Resolved (sequencing):** merges Proposal 2's *spike-first go/no-go gate* (the iOS bridge build/distribution pipeline is the #1 unknown and must precede surface-area work) with Proposal 3's *vertical-slice* insight (prove the whole pipeline — cinterop, adapters, Activity plumbing, both callback directions, main-thread dispatch — with one thin end-to-end feature before breadth). Proposal 1's linear Android-then-iOS ordering was rejected: it defers the riskiest work.

### Phase 0 — Toolchain proof (~2–3 days)
Extend the existing scaffold: `explicitApi()`, `api(kotlinx-coroutines-core)`, androidMain deps, empty `SuperwallBridge` + stub actuals compiling on all four targets, `publishToMavenLocal`, consumer-fixture project. **minSdk gate:** inspect the superwall-android 2.8.0 AAR's declared `minSdk` and build the consumer fixture **explicitly at minSdk 24**; on manifest-merge failure, move the module to minSdk 26 and record the decision (Open Question #11) before any other Android work.
**Validation:** all targets build; fixture app consumes the local publication on Android **at the declared minSdk without manifest-merger errors** (transitive superwall-android resolves) and compiles the iOS klib; minSdk decision recorded; CI skeleton green.

### Phase 1 — bridge spike & pipeline validation (**go/no-go gate**, ~1 week; parallel with Phase 2 start)
Stand up a **minimal but end-to-end** `SuperwallKMPBridge`: `Package.swift` pinning SuperwallKit; Swift facade covering configure (with the §5.2 status-carrying completion), identify, register-with-handler, delegate registration, and a first cut of `SWBEventEnvelope` for 3–4 representative events **including a transaction-bearing one** (proves the associated-value destructuring pattern); XCFramework build script; `.def` + per-target cinterop; throwaway iosMain smoke on simulator: (a) `configure` with a real API key **and with a bad key (exercise the bridge's status completion)**, (b) receive a destructured event envelope in Kotlin from a Kotlin `NSObject` delegate subclass, (c) `register` with handler + feature closure **including a forced error path (probe post-`onError` callback behavior per §6.4)**, (d) one completion-handler async round-trip. Validate publishing the interop-bearing klib through vanniktech AND consuming the bridge XCFramework from a bare iOS fixture app via the repo-root `Package.swift` binary target (with SuperwallKit resolving transitively).
**Deliverable:** `docs/bridge-surface.md` — every Pigeon HostApi + FlutterApi row mapped to {Kotlin bridge member → `@objc` bridge API → SuperwallKit call}, the `SWBEventEnvelope` design, the reference header committed for the CI drift check, and the error-terminality finding (§6.4).
**Gate — explicit go/no-go criteria and fallback tree:**
- **GO** if: the vertical smoke passes on simulator; the klib publishes and is consumable through vanniktech/Maven Local; the fixture app builds with the bridge via SPM and SuperwallKit resolves transitively at the pinned version.
- **NO-GO** if: the bridge XCFramework cannot be produced/consumed reproducibly, or transitive SuperwallKit linking through the binary-target manifest fails. Fallback tree, in order: (1) **direct cinterop against SuperwallKit's `@objc(SWK…)` surface** (the previous primary — costs event-payload fidelity and re-introduces upstream `@objc` coupling; §5.1), (2) **Kotlin CocoaPods plugin** distributing the bridge as a pod, (3) **pull static embedding forward from v2**. The decision, chosen fallback, and revised schedule are recorded in `docs/bridge-surface.md` before any iOS breadth work starts.
**Validation criteria (not just activities):** surface map complete; envelope pattern proven on a transaction-bearing event; klib publish + consume verified; bridge SPM consumption verified from a clean fixture; go/no-go decision recorded.

### Phase 2 — Common surface + Android bridge (1–2 weeks)
Full model catalog (§3.5) + `MODELS.md` **including the Deltas table (§3.4)**; `Superwall` façade, delegate/controller/handler interfaces, `SuperwallError`, `SuperwallBridge`, `DelegateMultiplexer`, `StreamHolder` lazy-attach design, `EntitlementPriority`; complete `AndroidSuperwallBridge` + adapters + mappers (line-by-line port of `SuperwallHost.kt`/`utils/*`); **Android delegate-hook audit (§4): verify whether superwall-android 2.8.0's `SuperwallDelegate` has `customerInfoDidChange`/`handleSuperwallDeepLink`; wire or gap-list `customerInfoFlow` accordingly**; `SuperwallInitializer` + `CurrentActivityTracker` + `androidSetup` + startup-stripped failure mode + `consumer-rules.pro`; Detekt alias rule. **End-of-phase API review (Open Question #10): ratify every Deltas-table row.**
**Validation:** commonTest green (models, façade contract via FakeBridge incl. guard exemptions/re-configure/stream attach, mergePrioritized); androidHostTest mapper round-trips + startup-stripped Robolectric test; androidDeviceTest configure/register/stream smoke; sample androidApp shows a real paywall; **delegate-hook audit conclusion recorded in §4's gap list; `MODELS.md` Deltas table complete with a sign-off recorded per row (acceptance criterion — Phase 2 does not exit without it).**

### Phase 3 — iOS bridge core path + cross-platform vertical slice (2–3 weeks, riskiest)
Grow `SuperwallKMPBridge` (Swift) + `IosSuperwallBridge` (Kotlin forwarder) to cover configure (incl. §5.2 status completion)/identify/reset/attributes/logLevel/subscriptionStatus/register/dismiss/preload/handleDeepLink; complete `SWBEventEnvelope` across the full ~70-case switch (port of `SuperwallDelegateHost.swift`, mislabels fixed); `DelegateAdapter` envelope mapping; `Continuations.kt`; `NSAnySanitizer.kt`; klib↔bridge version assert.
**Validation:** the vertical slice — configure + register(feature) + delegate + subscriptionStatusFlow — works end-to-end **on both platforms** in the sample app (iosApp SPM-links SuperwallKMPBridge; paywall on screen on both OSes); bridge XCTests cover the event switch; iosSimulatorTest link-smoke + mappers + configure-status path green.

### Phase 4 — Inversion-of-control completeness (1–2 weeks)
`PurchaseController` adapters both platforms; `onCustomCallback` async round-trip; per-registration handler lifecycle (retention/cleanup per the §6.4 decision, incl. the error-path behavior chosen in Phase 1); `customerInfoFlow` both platforms (Android per the Phase 2 audit outcome); `onBackPressed` (Android, synchronous same-thread contract).
**Validation:** on-device purchase-flow tests (StoreKit test session / Play sandbox) specifically covering **main-actor re-entrancy** (no deadlock when the user's suspend impl hops dispatchers); stream initial-value + transition coverage; handler cleanup verified on dismiss/skip **and the post-error behavior tested against the Phase 1 finding**.

### Phase 5 — Parity closure (1–2 weeks)
Remaining surface: `getPresentationResult`, `confirmAllAssignments`, `restorePurchases`, `consume`, redemption family (bridge envelopes), integration attributes, `overrideProductsByName`, `getCustomerInfo`, full 73-event mapping both platforms at full fidelity, `enableExperimentalDeviceVariables`. Decide/document Android `getCustomerInfo`/`getEntitlementsByProductIds`/`customerInfoFlow` behavior (Open Question #1); file upstream superwall-android issues (customerInfo surface, delegate hooks if absent, entitlement fields).
**Validation:** event-mapper exhaustiveness test generated from `EventType` (fails when a native case falls to params-fallback on either platform); redemption round-trips; Maestro/XCUITest flows ported from `test_app/maestro/`.

### Phase 6 — Hardening & 1.0 release (~1 week)
Binary-compat validator; Dokka docs; README (Android one-liner + R8/startup notes; iOS two-step: `SuperwallKMPBridge` SPM/pod + `isStatic = true`); `COMPATIBILITY.md` (superwall-kmp ↔ SuperwallKMPBridge ↔ superwall-android ↔ SuperwallKit iOS **↔ supported consumer Kotlin version range**); migration notes for Flutter-SDK-familiar developers; tag-driven Maven Central publish **from the macOS release runner with the bridge build + release-asset step wired (§8 Release)**; cross-repo canary job.
**Validation:** publish dry-run consumed by the fixture from a staging repo (at the declared minSdk); full nightly suite green; docs reviewed.

### v2 (post-1.0) — Zero-step iOS & toolchain simplification
- **Static embedding** (RevenueCat-3.0 pattern): fold SuperwallKit + Superscript `libcel` + the bridge into static slices the klib `-include-binary`s, so the iOS SPM/CocoaPods step disappears.
- **Migrate to Kotlin's first-party SwiftPM integration once it's stable** (`localSwiftPackage(...)` pointing at `bridge/`, `swiftPMDependencies` for SuperwallKit) — the bridge's Swift code carries over unchanged; what disappears is the hand-rolled XCFramework build/download/def wiring. Track the feature's stabilization from Kotlin 2.4.20-Beta onward.
- Evaluate Kotlin Swift export once it exits alpha; consider platform-specific `PurchaseController` extensions exposing `ProductDetails`/`StoreProduct`.

---

## 10. Open questions / decisions needed from the team

1. **Android `getCustomerInfo` semantics** — default in this plan is the Flutter-parity synthesized stub + logged warning; Proposal 2 argued for throwing `SuperwallError.NotSupportedOnPlatform` as more honest. Product call needed before Phase 5; real fix is upstream superwall-android support — is that schedulable? (Decide together with the related `customerInfoFlow`/delegate-hook gaps from the Phase 2 audit, §4.)
2. **Bridge distribution mechanics** — SPM **binary target** in the repo-root `Package.swift` pointing at the release XCFramework (this plan's default: consumers never need our Swift sources or build settings) vs. a **source package** (simpler release pipeline — no asset upload — but consumers compile our Swift with their Xcode, and dSYM/build-setting drift becomes their problem). Also: is CocoaPods support a v1 requirement or SPM-only? Decide before Phase 1 ends (it shapes the release automation).
3. **Bridge surface ownership & review** — the `@objc` bridge header is now a second API contract we maintain (internal, but drift-sensitive: the klib is compiled against it). Who reviews `docs/bridge-surface.md` changes, and do bridge-only changes (no Kotlin surface change) get their own version line or stay lockstep with superwall-kmp versions (this plan's default: strict lockstep, one tag releases both)?
4. **klib↔bridge version-mismatch behavior at `configure()`** — hard log warning (this plan's default) vs. throwing. Lockstep releasing makes mismatch unlikely (a consumer would have to pin the SPM package to a different version than the Gradle dep), but when it happens the failure mode is an unrecognized-selector crash; throwing is safer, a warning is gentler on patch drift. Decide before Phase 3.
5. **`Product` model name** — the catalog flags collision with StoreKit's `Product` for future Swift consumers; keep `Product` (Kotlin-first, this plan's default) or rename `PaywallProduct` now while it's free?
6. **`consume()` on iOS** — echo the token (Flutter parity, this plan's default) vs. throw `NotSupportedOnPlatform`. Same policy question as #1; the two should be decided together as one "platform-gap policy".
7. **SuperwallKit pin at spike time** — 4.16.2 is current; confirm the exact pin in `bridge/Package.swift` and whether the compatibility table promises patch-range (`4.16.x`) or exact-version compatibility. (The bridge absorbs source-level drift, so patch-range is more defensible than under the old direct-cinterop design — but the promise still needs deciding.)
8. **Purchase-controller platform ergonomics timing** — do we commit to the v1.1 androidMain extension exposing raw `ProductDetails` (and iOS `StoreProduct`) now, or leave it unscheduled?
9. **Sample app scope** — one Compose Multiplatform demo (this plan) vs. also a plain-Android + plain-iOS pair to validate non-Compose consumers before 1.0?
10. **Pre-1.0 API review checkpoint** — the surface is hand-written and `explicitApi()`-locked; the formal API review at the end of Phase 2 now has a concrete artifact and acceptance criterion: **ratify every row of the `MODELS.md` Deltas table** (each recorded deviation from the Pigeon contract — renames, type changes, added fields, dropped models — gets an explicit approve/revert), plus naming, nullability, and the `Map<String, Any?>` contract. Phase 2 does not exit without it, before Android ships anything consumers might pin to.
11. **Module minSdk: 24 or 26** — the KMP scaffold declares 24 but the Flutter plugin builds superwall-android 2.8.0 at minSdkVersion 26 (`/home/user/Superwall-Flutter/android/build.gradle:46`). If the AAR declares minSdk 26, keeping 24 means every minSdk 24–25 consumer hits a manifest-merger failure at their build, not ours. Phase 0 verifies the AAR; default resolution is to align with superwall-android's declared minSdk rather than paper over with `tools:overrideLibrary`. Needs sign-off because "minSdk 24" is in the stated target spec.
12. **Bridge build reproducibility policy** — the release XCFramework is a binary artifact: which Xcode version(s) do we build with, do we commit to bit-reproducible builds or just checksum-pinned ones, and do we support consumers on older Xcode than the build machine (ObjC headers are forgiving, but the embedded Swift runtime version floor is not)? Decide before the first tagged release.
