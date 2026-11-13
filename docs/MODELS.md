# MODELS.md — Pigeon → KMP Model Cross-Reference & Deltas Ledger

**Contract:** `/home/user/Superwall-Flutter/pigeons/configure.dart` (Superwall-Flutter v2.4.5)
**Ground truth:** `superwall-kmp/src/commonMain/kotlin/com/superwall/sdk/kmp/models/` (as written)
**Governance:** every deliberate deviation from the Pigeon contract is a row in the [Deltas table](#deltas-table-34-ledger). Nothing ships as a generic "fidelity fix" without a row. All rows are `PENDING` sign-off until the Phase 2 API review (Open Question #10).

Package root: `com.superwall.sdk.kmp.models` (abbreviated `…` below). Naming convention throughout: `P` prefix stripped; sealed subtypes nested and prefix-free (`PErrorTriggerResult` → `TriggerResult.Error`); Pigeon `ignore: bool?` placeholder fields become `data object`s.

---

## 1. Cross-reference by area

### Configuration & options — `….options`

| Pigeon | KMP | Notes |
|---|---|---|
| `PSuperwallOptions` | `SuperwallOptions` | All 13 fields present; nullable-with-implicit-defaults → non-null with explicit defaults (Δ13) |
| `PPaywallOptions` | `PaywallOptions` | `onBackPressedHost` → real closure `onBackPressed: ((PaywallInfo?) -> Boolean)?` (Δ7); `preloadDeviceOverrides: Map<String,bool>` → `Map<DeviceTier, Boolean>` (Δ14) |
| `PRestoreFailed` | `RestoreFailed` | Nullable `String?` fields → non-null with the SDK's default strings (Δ13) |
| `PLogging` | `Logging` | `level: PLogLevel?` → `LogLevel? = null`, the native default (Δ47); `scopes: List<PLogScope>?` → `Set<LogScope> = {ALL}` (Δ12, Δ13); adds ported client helpers `handleLogRecord`/`debug`/`info`/`warn`/`error` (Δ16) |
| `PTestModeBehavior` | `TestModeBehavior` | 4 values, 1:1 |
| `PNetworkEnvironment` | `NetworkEnvironment` | 3 values, 1:1, plus `LOCAL` and `SuperwallOptions.customNetworkEnvironment` (Δ46) |
| `PLogLevel` | `LogLevel` | 5 values, 1:1; Swift `.none` gotcha in KDoc |
| `PLogScope` | `LogScope` | 22 values, 1:1 |
| `PTransactionBackgroundView` | `TransactionBackgroundView` | 2 values, 1:1; Swift `.none` gotcha in KDoc |
| `PConfigurationStatus` | `ConfigurationStatus` | 3 values, 1:1 |
| — (raw strings `ultra_low`…) | `DeviceTier` | New typed enum, 6 values (Δ14) |

### Identity — `….identity`

| Pigeon | KMP | Notes |
|---|---|---|
| `PIdentityOptions` | `IdentityOptions` | `restorePaywallAssignments: Boolean = false` |

### Paywall — `….paywall`

| Pigeon | KMP | Notes |
|---|---|---|
| `PPaywallInfo` | `PaywallInfo` | ~30 fields, all nullable, 1:1; timestamp fields (`presentedByPlacementAt`, `*LoadStartTime` etc.) → `kotlin.time.Instant` (Δ11); `entitlements` on products via `PaywallProduct` |
| `PProduct` | `PaywallProduct` (Δ33) | `entitlements: List` → `Set<Entitlement>` (Δ10) |
| `PLocalNotification` | `LocalNotification` | `id` now required `String` (Pigeon default is `0` assigned to a `String` field / Flutter hardcodes `""`) (Δ22); `delay: Long` |
| `PLocalNotificationType` | `LocalNotificationType` | 2 values, 1:1 |
| `PComputedPropertyRequest` | `ComputedPropertyRequest` | 1:1 |
| `PComputedPropertyRequestType` | `ComputedPropertyRequestType` | 10 values, 1:1 |
| `PSurvey` | `Survey` | 1:1 |
| `PSurveyOption` | `SurveyOption` | 1:1 |
| `PSurveyShowCondition` | `SurveyShowCondition` | 2 values, 1:1 |
| `PFeatureGatingBehavior` | `FeatureGatingBehavior` | 2 values, 1:1 |
| `PPaywallCloseReason` | `PaywallCloseReason` | 5 values, 1:1; Swift `.none` gotcha in KDoc |

### Store — `….store`

| Pigeon | KMP | Notes |
|---|---|---|
| `PStoreTransaction` | `StoreTransaction` | 13 fields, 1:1; ISO-8601 `String?` dates → `Instant?` (Δ11) |
| `PStoreProduct` | `StoreProduct` | 41 fields, 1:1; `trialPeriodEndDate: String?` → `Instant?` (Δ11); `entitlements: List` → `Set` (Δ10) |

### Entitlements / customer — `….entitlements`

| Pigeon | KMP | Notes |
|---|---|---|
| `PEntitlement` | `Entitlement` | 13 fields; epoch-ms `int?` dates (`startsAt`/`renewedAt`/`expiresAt`) → `Instant?` (Δ11); defaults for `type`/`isActive`/`productIds` |
| `PEntitlements` | `Entitlements` | Pure value snapshot; 4 × `List` → `Set<Entitlement>` (Δ10); byProductIds filtering moved to façade (Δ19) |
| `PCustomerInfo` | `CustomerInfo` | 1:1 (`List`s kept — transaction order is meaningful) |
| `PSubscriptionTransaction` | `SubscriptionTransaction` | 12 fields, 1:1; epoch-ms dates → `Instant` (Δ11) |
| `PNonSubscriptionTransaction` | `NonSubscriptionTransaction` | 6 fields, 1:1; epoch-ms date → `Instant` (Δ11) |
| `PSubscriptionStatus` / `PActive` / `PInactive` / `PUnknown` | sealed `SubscriptionStatus` { `Active(Set<Entitlement>)`, `Inactive`, `Unknown` } | `Active.entitlements: List` → `Set` (Δ10); adds `isActive` convenience (ported Flutter client logic) |
| `PSubscriptionStatusType` | — dropped | Redundant enum shadow of the sealed type (Δ1) |
| `PEntitlementType` | `EntitlementType` | 1 value, 1:1 |
| `PProductStore` | `ProductStore` | 7 values: 6 from Pigeon 1:1 + `CUSTOM` (Δ31) |
| `PLatestSubscriptionState` | `LatestSubscriptionState` | 5 values, 1:1 |
| `PLatestSubscriptionOfferType` | `LatestSubscriptionOfferType` | 4 values, 1:1 |

### Purchase / restore results — `….results`

| Pigeon | KMP | Notes |
|---|---|---|
| `PPurchaseResult` + `PPurchasePurchased`/`PPurchaseCancelled`/`PPurchasePending`/`PPurchaseFailed` | sealed `PurchaseResult` { `Purchased`, `Cancelled`, `Pending`, `Failed(error)` } | `ignore` fields dropped (Δ2); companion factories `purchased()`/`cancelled()`/`pending()`/`failed(error)` |
| `PRestorationResult` + `PRestorationRestored`/`PRestorationFailed` | sealed `RestorationResult` { `Restored`, `Failed(error)` } | Companion factories `restored()`/`failed(error)` |
| `PRestoreType` + `PViaPurchase`/`PViaRestore` | sealed `RestoreType` { `ViaPurchase(StoreTransaction?)`, `ViaRestore` } | 1:1 |

### Trigger / presentation — `….triggers`, `….results`

| Pigeon | KMP | Notes |
|---|---|---|
| `PExperiment` | `Experiment(id, groupId, variant)` | 1:1 with Pigeon (which already carries `variant`); differs from Flutter *public* `Experiment`, which fakes an empty variant (Δ8) |
| `PVariant` | `Variant(id, type, paywallId?)` | 1:1 |
| `PVariantType` | `VariantType` | 2 values, 1:1 |
| `PConfirmedAssignment` | `ConfirmedAssignment` | 1:1 |
| `PTriggerResult` + 5 subtypes | sealed `TriggerResult` { `PlacementNotFound`, `NoAudienceMatch`, `Paywall(experiment)`, `Holdout(experiment)`, `Error(error)` } | Nested prefix-free subtypes (Δ2) |
| `PPresentationResult` + 5 subtypes | sealed `PresentationResult` { `PlacementNotFound`, `NoAudienceMatch`, `Paywall(experiment)`, `Holdout(experiment)`, `PaywallNotAvailable` } | 1:1 shape |
| `PPaywallResult` + 3 subtypes | sealed `PaywallResult` { `Purchased(productId)`, `Declined`, `Restored` } | 1:1 shape |
| `PPaywallSkippedReason` (payload-less **enum**) | sealed `PaywallSkippedReason` { `Holdout(experiment)`, `NoAudienceMatch`, `PlacementNotFound` } | Enum → sealed; restores the holdout experiment payload Flutter drops (Δ9) |
| `PPaywallPresentationRequestStatusType` | `PaywallPresentationRequestStatusType` | 3 values, 1:1 |
| `PPaywallPresentationRequestStatusReason` + 9 `PStatusReason*` subtypes | sealed `PaywallPresentationRequestStatusReason` (9 nested cases) | `PStatusReasonNoPaywallVc` → `NoPaywallViewController`, `PStatusReasonSubsStatusTimeout` → `SubscriptionStatusTimeout` (name expansion, same semantics) |

### Redemption — `….redemption`

| Pigeon | KMP | Notes |
|---|---|---|
| `PRedemptionResult` + `PSuccessRedemptionResult`/`PErrorRedemptionResult`/`PExpiredCodeRedemptionResult`/`PInvalidCodeRedemptionResult`/`PExpiredSubscriptionCode` | sealed `RedemptionResult` { `Success`, `Error`, `ExpiredCode`, `InvalidCode`, `ExpiredSubscription` } | Nested prefix-free (Δ2) |
| `PRedemptionInfo` | `RedemptionInfo` | `entitlements: List` → `Set<Entitlement>` (Δ10) |
| `PPurchaserInfo` | `PurchaserInfo` | 1:1 |
| `PErrorInfo` | `ErrorInfo` | 1:1 |
| `PExpiredCodeInfo` | `ExpiredCodeInfo` | 1:1 |
| `PRedemptionPaywallInfo` | `RedemptionPaywallInfo` | 1:1; `placementParams: Map<String, Any?>` |
| `POwnership` + `PAppUserOwnership`/`PDeviceOwnership` | sealed `Ownership` { `AppUser(appUserId)`, `Device(deviceId)` } | 1:1 shape |
| `PStoreIdentifiers` + `PStripeStoreIdentifiers`/`PPaddleStoreIdentifiers`/`PUnknownStoreIdentifiers` | sealed `StoreIdentifiers` { `Stripe`, `Paddle`, `Unknown` } | 1:1 shape |

### Custom callbacks — `….callbacks`

| Pigeon | KMP | Notes |
|---|---|---|
| `PCustomCallback` | `CustomCallback` | 1:1; `variables: Map<String, Any?>?` |
| `PCustomCallbackResult` | `CustomCallbackResult` | 1:1; companion factories `success(data)`/`failure(data)` |
| `PCustomCallbackResultStatus` | `CustomCallbackResultStatus` | 2 values, 1:1 |

### Events — `….events`

| Pigeon | KMP | Notes |
|---|---|---|
| `PSuperwallEventInfo` | `SuperwallEventInfo` | Flat envelope, all 27 fields present incl. the six the Flutter mapping layer silently drops (Δ15); `attempt`/`reviewRequestedCount`: `int?` → `Long?` |
| `PEventType` | `EventType` | 90 values: the 73 Pigeon values 1:1 (incl. `paywallResourceLoadFail`) + 17 native-only events (Δ34) |
| `PIntegrationAttribute` | `IntegrationAttribute` | 21 values, 1:1 |

### Interfaces (contract callbacks → KMP interfaces, `com.superwall.sdk.kmp`)

| Pigeon | KMP | Notes |
|---|---|---|
| `PSuperwallDelegateGenerated` (FlutterApi) | `SuperwallDelegate` | All 15 members, default no-op impls; `handleLog` fully typed (Δ12) |
| `PPurchaseControllerGenerated` (FlutterApi) | `PurchaseController` | 3 suspend funs, 1:1 |
| `PPaywallPresentationHandlerGenerated` (FlutterApi) | `PaywallPresentationHandler` | `onPresent`/`onDismiss`/`onError`/`onSkip`/`onCustomCallback` (suspend), 1:1 |
| `PFeatureHandlerGenerated` / `PFeatureHandlerHost` | `feature: (() -> Unit)?` param on `Superwall.register` | Host-ID plumbing dissolved (Δ3) |
| `POnBackPressedGenerated` / `POnBackPressedHost` | `PaywallOptions.onBackPressed` closure | Δ7; return-value semantics defined & tested (Δ23) |
| `PConfigureCompletionGenerated` / `PConfigureCompletionHost` | `configure(completion: (Result<Unit>) -> Unit)` / `suspend configureAndAwait()` | Success bool no longer dropped (Δ20) |
| `SuperwallEventStreams` (EventChannelApi) | `Superwall.subscriptionStatusFlow: StateFlow<SubscriptionStatus>`, `Superwall.customerInfoFlow: Flow<CustomerInfo>` | Streams → coroutine Flows |

### Host-API façade renames (verified against `Superwall.kt`)

| Pigeon (`PSuperwallHostApi`) | KMP façade (`Superwall`) | Notes |
|---|---|---|
| `registerPlacement(placement, params, handler, feature)` | `register(placement, params, handler, feature)` | Δ4 |
| `preloadPaywallsForPlacements(List<String>)` | `preloadPaywalls(placementNames: Set<String>)` | Δ5, Δ10 |
| `setDelegate(bool hasDelegate)` | `var delegate: SuperwallDelegate?` | Δ6 |
| `getLogLevel()/setLogLevel(String)` | `var logLevel: LogLevel` | Δ12 |
| `getEntitlementsByProductIds(List<String>): List<PEntitlement>` | `suspend getEntitlementsByProductIds(Set<String>): Set<Entitlement>` | Δ10, Δ19 |
| `confirmAllAssignments(): List<PConfirmedAssignment>` | `suspend confirmAllAssignments(): Set<ConfirmedAssignment>` | Δ10 |
| getter/setter pairs (`getUserAttributes`, `getSubscriptionStatus`/`setSubscriptionStatus`, `getLocaleIdentifier`/…, `getOverrideProductsByName`/…) | Kotlin properties (`userAttributes`, `subscriptionStatus`, `localeIdentifier`, `overrideProductsByName`, `configurationStatus`, `isConfigured`, `isInitialized`, `userId`, `isLoggedIn`, `isPaywallPresented`, `latestPaywallInfo`, `entitlements`) | Idiomatic property mapping, not counted as renames |

---

## Deltas table (§3.4 ledger)

One row per deliberate deviation from the Pigeon contract. Category: rename / type / add / drop. All rows **bulk-APPROVED** by ian@superwall.com on 2026-07-31 (interactive API review; the contentious calls — PaywallProduct rename, platform-gap policy, version-bump additions — were decided individually). The Phase 2 API-review gate is satisfied.

| # | Pigeon shape | KMP shape | Category | Rationale | Sign-off |
|---|---|---|---|---|---|
| Δ1 | `PSubscriptionStatusType` enum {active, inactive, unknown} | — (none) | drop | Redundant payload-less shadow of the sealed `SubscriptionStatus`; Kotlin callers pattern-match the sealed type directly | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ2 | `ignore: bool?` placeholder fields on every payload-less sealed subtype (`PPurchaseCancelled`, `PInactive`, `PViaRestore`, `PStatusReason*`, …) | `data object` cases | drop | The field exists only because Pigeon can't encode empty classes; `data object` is the honest shape | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ3 | `POnBackPressedHost`, `PPurchaseControllerHost`, `PConfigureCompletionHost`, `PPaywallPresentationHandlerHost`, `PFeatureHandlerHost` (hostId envelopes) | — (direct object/closure references) | drop | Host-ID indirection is Pigeon wire plumbing; in-process Kotlin passes the handler itself. Also removes the `"${placement}handler"` feature-hostId suffix mismatch bug by construction | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ4 | `registerPlacement(...)` | `Superwall.register(...)` | rename | Matches native SuperwallKit API name on both platforms | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ5 | `preloadPaywallsForPlacements(...)` | `Superwall.preloadPaywalls(...)` | rename | Matches native SuperwallKit API name | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ6 | `setDelegate(hasDelegate: bool)` | `var delegate: SuperwallDelegate?` | type | The bool was a wire-protocol artifact (delegate lives Flutter-side); KMP holds the real delegate object (multiplexed internally) | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ7 | `PPaywallOptions.onBackPressedHost: POnBackPressedHost?` | `PaywallOptions.onBackPressed: ((PaywallInfo?) -> Boolean)?` | type | Real closure instead of host envelope; Android-only, documented | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ8 | Flutter public `Experiment` omits variant (fakes empty); Pigeon `PExperiment` carries it | `Experiment(id, groupId, variant)` | add (vs Flutter public layer) | Pigeon already has `variant`; KMP surfaces it instead of discarding it like the Flutter public model | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ9 | `PPaywallSkippedReason` payload-less **enum** {holdout, noAudienceMatch, placementNotFound} | sealed `PaywallSkippedReason` with `Holdout(experiment)` | type + add | Keeps the holdout `Experiment` payload the Flutter layer drops; consistent with the sibling sealed results | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ10 | `List<...>` where semantically a set: `PEntitlements.active/inactive/all/web`, `PActive.entitlements`, `PProduct.entitlements`, `PStoreProduct.entitlements`, `PRedemptionInfo.entitlements`, `getEntitlementsByProductIds`, `confirmAllAssignments`, `preloadPaywallsForPlacements` arg | `Set<...>` | type | Uniqueness is the actual contract; `Set` gives correct equality/dedup semantics | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ11 | Dates as epoch-ms `int?` (Entitlement, CustomerInfo family) and ISO-8601 `String?` (StoreTransaction, StoreProduct, PaywallInfo timestamps) | `kotlin.time.Instant?` everywhere | type | One typed date convention (stable in Kotlin 2.3, no kotlinx-datetime dep); mappers convert both wire conventions at the boundary | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ12 | `getLogLevel()/setLogLevel(String)`; delegate `handleLog(level: String, scope: String, …)`; `PLogging.scopes: List<PLogScope>?` | `LogLevel`/`LogScope` enums end-to-end (`Superwall.logLevel: LogLevel`, typed `handleLog`, `Logging.scopes: Set<LogScope>`) | type | Wire API was stringly-typed although the enums already existed; unmappable native strings degrade to `DEBUG`/`ALL` with raw value preserved in `info["rawLevel"]`/`info["rawScope"]` | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ13 | Options fields nullable with implicit native defaults (`PSuperwallOptions`, `PPaywallOptions`, `PRestoreFailed`, `PLogging`) | Non-null data-class fields with explicit defaults | type | Matches Flutter *public* layer style; defaults are visible in the API instead of hidden native fallbacks | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ14 | `preloadDeviceOverrides: Map<String, bool>?` (raw tier strings) | `Map<DeviceTier, Boolean>` with new `DeviceTier` enum (6 values) | type + add | Typed keys over magic strings; Android-only, documented | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ15 | `PSuperwallEventInfo` fields `userEnrichment`, `deviceEnrichment`, `message`, `integrationAttributes`, `reviewRequestedCount`, `missingProductIdentifiers` exist in Pigeon but are silently dropped by the Flutter mapping layer | All six mapped through on both platforms (iOS via typed Swift envelope destructuring) | add (vs Flutter behavior) | Envelope declared them; the drop was a Flutter-layer bug, not a contract decision | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ16 | Pigeon `PLogging` is data-only; Flutter public `Logging` adds `handleLogRecord` + `debug/info/warn/error` | KMP `Logging` keeps the client-side helpers | add | App-side log routing ports one-to-one from the Flutter public layer (SuperwallOptions.dart:138-163) | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ17 | `TransactionProduct` (Flutter public-layer model; not in Pigeon) | — (none) | drop | Dead public-layer type with no Pigeon wire representation; nothing produces it | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ18 | `SuperwallBuilder` + deprecated typedefs (Flutter public layer) | — (none) | drop | Dart widget/back-compat constructs with no KMP equivalent | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ19 | `PEntitlements` reachable via live bridge filter (`getEntitlementsByProductIds` hidden `nativeFilterCallback`) | `Entitlements` is a pure immutable snapshot; filtering is `suspend Superwall.getEntitlementsByProductIds(Set<String>): Set<Entitlement>` on the façade | type | Value equality, snapshotting, and testability; native filtering still reachable, just relocated. Android local-filters with a warning until upstream support exists | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ20 | `PConfigureCompletionGenerated.onConfigureCompleted(bool success)` — Flutter's proxy drops the bool | `configure(completion: (Result<Unit>) -> Unit)` + `suspend configureAndAwait(): Result<Unit>` | type | Success/failure is surfaced instead of discarded (`ConfigureCompletionProxy` bug not replicated) | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ21 | `attempt: int?` (stringified in parts of the Flutter layer) | `attempt: Long?` | type | Stays numeric per contract; Flutter stringification was a bug | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ22 | `PLocalNotification.id: String` with default `0` (sic) / Flutter hardcodes `""` | `LocalNotification.id: String` required, no default | type | The Pigeon default is type-incoherent; id is always known at the source | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ23 | `POnBackPressedGenerated.onBackPressed` returns void; comment vs behavior disagree on handling semantics | `onBackPressed` returns `Boolean` — `true` = app handled it, `false` = SDK default | type | Semantics defined explicitly and covered by tests instead of porting the ambiguity | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ24 | `enableExperimentalDeviceVariables` present but dead (never wired) in Flutter | Actually wired to the native SDKs | add (behavior) | Field existed in the contract; only the plumbing was missing | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ25 | Untyped payloads `Map<String, Object>` | `Map<String, Any?>` with documented value contract (String/Boolean/Long/Double/List/Map/Set); per-platform Any-sanitizer (unknown → `toString()`) | type | Kotlin-idiomatic nullable values; degrade-never-crash sanitization ported from `mapParamsForDart` | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ26 | `PStatusReasonNoPaywallVc`, `PStatusReasonSubsStatusTimeout` | `NoPaywallViewController`, `SubscriptionStatusTimeout` | rename | Abbreviation expansion only; semantics identical | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ27 | `SuperwallEventStreams` event channels | `subscriptionStatusFlow: StateFlow<SubscriptionStatus>`, `customerInfoFlow: Flow<CustomerInfo>` | type | Kotlin coroutines Flow is the platform-native stream primitive | APPROVED (ian@superwall.com, 2026-07-31) |

### Plan-vs-written discrepancies (recorded, not deltas)

- §3.4 says "`Experiment` carries its `variant` (Flutter's public type fakes an empty one)" — note the *Pigeon* `PExperiment` already carries `variant`; the deviation (Δ8) is only relative to the Flutter public layer, not the Pigeon contract.
- §3.5 lists `Entitlement` at 13 fields and `StoreProduct` at 41 fields; the written models match (verified).
- `EventType` = 73 values and `IntegrationAttribute` = 21 values as written — matches both plan and Pigeon.
- No other divergence between the plan's §3.5 catalog and the written `models/` tree was found.

### Native-version-bump additions (superwall-android 2.8.0 / SuperwallKit iOS 4.16.1)

Additions beyond the Pigeon contract picked up when the wrapper moved to the latest native SDKs. Same governance as above.

| # | Pigeon shape | KMP shape | Category | Rationale | Sign-off |
|---|---|---|---|---|---|
| Δ28 | — (absent) | `SuperwallOptions.eventTrackingBehavior: EventTrackingBehavior` (ALL / SUPERWALL_ONLY / NONE) | add | New option on both natives; typed replacement for the deprecated `isExternalDataCollectionEnabled`. Precedence rule: the typed field wins when non-default; `ALL` (the default) never overrides the boolean, since the natives derive one from the other | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ29 | — (absent) | `IntegrationAttribute.SINGULAR_DEVICE_ID` | add | Present on both natives (Android 2.8.0 `AttributionProvider`, iOS `IntegrationAttribute`) | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ30 | — (absent) | `IntegrationAttribute.FIREBASE_INSTALLATION_ID` | add | iOS-only (`@platform` KDoc); Android skips it with a logged warning | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ31 | — (absent) | `ProductStore.CUSTOM` | add | Present on both natives; previously degraded to `OTHER` on iOS and was unmappable on Android | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ32 | `purchase(activity, ProductDetails, …)` (Flutter host path) | Android adapter overrides 2.8.0's primary `purchase(activity, StoreProduct, …)` overload | add (behavior) | The `ProductDetails` overload is deprecated in 2.8.0 and its replacement's default implementation fails for custom-store products; overriding the new entry point routes Play AND custom products to the user's productId-based `purchaseFromGooglePlay` | APPROVED (ian@superwall.com, 2026-07-31) |
| Δ33 | `PProduct` (paywall product slot: id, name, entitlements) | `PaywallProduct` | rename | Disambiguates from the store-level `StoreProduct` and avoids the StoreKit `Product` collision for future Swift consumers | APPROVED (ian@superwall.com, 2026-07-31) |

### Native-version-bump additions (superwall-android 2.10.2 / SuperwallKit iOS 4.19.1)

| # | Pigeon shape | KMP shape | Category | Rationale | Sign-off |
|---|---|---|---|---|---|
| Δ34 | — (absent; these events reached the delegate as `customPlacement`) | `EventType.ATTRIBUTION_MATCH`, `PAYWALL_PAGE_VIEW`, `TEST_MODE_MODAL_OPEN`/`CLOSE`, `REVIEW_GRANTED`/`DENIED` (Android), `STRIPE_CHECKOUT_START`/`SUBMIT`/`COMPLETE`/`FAIL` (iOS), `TELEPORT_OPEN`/`RETURN` (iOS), `CUSTOMER_CENTER_OPEN`/`CLOSE`/`ACTION`/`SURVEY_RESPONSE`/`REFUND_REQUEST`; payload values without a `SuperwallEventInfo` field travel in `params` under one key set on both platforms (`permissionName`, `paywallIdentifier`, `paywallCount`, `pageViewData`, `attributionMatch`, plus iOS `transactionType`/`transactionProductId` and Android `url`/`duration`) | add (behavior) | Every native event now arrives under its own type instead of `CUSTOM_PLACEMENT` with a per-platform raw name, per the "unmapped native case is a bug" rule. Only a native case newer than the wrapper still falls back, with `params["rawEventType"]` on both platforms | PENDING |
| Δ35 | — (absent) | `Superwall.purchase(productId): PurchaseResult`, `Superwall.getProducts(Set<String>): Map<String, StoreProduct>` | add | Native on both platforms, missing from the wrapper. A purchase failure (including an unknown product) stays in `PurchaseResult.Failed`, like `restorePurchases`; iOS fetches the `StoreProduct` first because SuperwallKit's `purchase` takes one. `getProducts` is keyed by the requested identifier | PENDING |
| Δ36 | — (absent) | `getAssignments()`, `refreshConfiguration()`, `var eventTrackingBehavior`, `setInterfaceStyle(InterfaceStyle?)` (enum now public), `cancelAllScheduledNotifications()`, `suspend refreshCustomerInfo()`; Android-only `setPushToken`, `optOutOfNotifications`, `optInToNotifications`, `handleNotificationOpen(Map<String, String>)`; iOS-only `var grantedEntitlements` | add | Platform-only members are documented no-ops (or empty / the current value) on the other platform, never a throw | PENDING |
| Δ37 | — (absent) | `AdConsent(adUserData, adPersonalization)` + `AdConsentStatus`; `SuperwallOptions.adConsent: AdConsent?`, `Superwall.adConsent: AdConsent?` (read-only) + `setAdConsent(AdConsent)` | add | `null` = never set, so a TCF banner's answer applies (Android `AdConsent.Unspecified`; the Swift bridge records whether the app set one because SuperwallKit's flag is internal). There is no way back to "not set" on iOS, hence a setter that takes a non-null value | PENDING |
| Δ38 | — (absent) | Customer Center: `CustomerCenterConfiguration` (+ nested `Screen`, `Path`, sealed `PathType`, `OpenMethod`, `FeedbackSurvey`, `Support`, `Appearance`), `CustomerCenterAction`, `CustomerCenterPurchase`, `CustomerCenterRefundStatus`, `CustomerCenterDelegate` (suspend `customerCenterShouldRestorePurchases`), `SuperwallOptions.customerCenter`, `presentCustomerCenter` / `suspend dismissCustomerCenter` | add | `Path.id` is optional so each native SDK keeps its own default id; refund windows are milliseconds (converted to seconds for iOS); `Support.checksAppStoreForUpdates` is iOS only | PENDING |
| Δ39 | — (absent) | `PurchaseController.purchaseFromGooglePlayWithReplacement(productId, basePlanId, offerId, ProductReplacement)` with a default that calls `purchaseFromGooglePlay`; `ProductReplacement`, `ProductReplacementMode` | add | superwall-android 2.9+ calls the replacement overload; the default keeps existing controllers source-compatible | PENDING |
| Δ40 | `PIntegrationAttribute` / `PLogScope` value sets | `IntegrationAttribute` + `AIRBRIDGE_ID`, Android-only `META`, `AMPLITUDE`, `MIXPANEL`, `GOOGLE_ADS`, `GOOGLE_APP_SET_ID`, `CUSTOM`; `LogScope` + `ANALYTICS`, `WEB_ENTITLEMENTS`, `GRANTED_ENTITLEMENTS`, `CUSTOMER_CENTER`, `CUSTOMER_INFO`, `JS_EVALUATOR`, `PAYWALL_TRANSACTIONS`, `NATIVE_PURCHASE_CONTROLLER`, `DEEP_LINKS`, `BILLING` | add (behavior) | Every native value now has a common one. A value the other platform lacks is skipped (attributes, with a logged warning) or left out of `Logging.scopes`; `handleLog` no longer reports these scopes as `ALL` + `rawScope` | PENDING |
| Δ41 | — (absent) | `PaywallPresentationHandler.onWillDismiss` (iOS only); `SuperwallOptions.storeKitVersion: StoreKitVersion?` (iOS), `PaywallOptions.notificationPermissionsDenied: NotificationPermissionsDenied?` (iOS), `optimisticLoading`, `timeoutAfter: Duration?`, `presentationHost: PresentationHost` (Android) | add | `storeKitVersion` is nullable because SuperwallKit picks its default at runtime | PENDING |
| Δ42 | `PEntitlement`, `PStoreTransaction`, `PStoreProduct`, `PProduct`, `PPaywallInfo`, `PRedemptionPaywallInfo`, `PRedemptionInfo`, `PCustomerInfo` field sets | `Entitlement.isRevoked/renewsAt/isInGracePeriod` and `CustomerInfo.activeSubscriptionProductIds` (computed in common, as natively); `StoreTransaction.state/payment/appAccountToken/purchaseToken/signature` (+ `StoreTransactionState`, `StorePayment`); `StoreProduct.subscriptionPeriod` (+ `SubscriptionPeriod`, `SubscriptionPeriodUnit`) and Android-only `fullIdentifier/productType/storeFrontCountryCode/isCustomProduct`; `PaywallProduct.store`; `PaywallInfo.cacheKey/buildId/shimmerLoad*Time/presentation/isScrollEnabled/presentationId` + iOS `isLocal/introOfferEligibility` (+ `PaywallPresentationInfo`, sealed `PaywallPresentationStyle`, `IntroOfferEligibility`); `RedemptionPaywallInfo.productIdentifier`, iOS `RedemptionInfo.userAttributes` | add | All new fields default to `null` / `false` so existing constructor calls compile; platform-only fields are documented | PENDING |
| Δ43 | `PPurchaseControllerGenerated.purchaseFromAppStore(productId)` / `purchaseFromGooglePlay(productId, basePlanId, offerId)` | adds `purchaseFromAppStore(product: StoreProduct)` and `purchaseFromGooglePlay(product: StoreProduct, basePlanId, offerId, replacement: ProductReplacement?)`, which the adapters call and whose defaults call the product-id methods; `StoreProduct.customTransactionId` on both platforms | add | A custom store product can't be told from a store product by its id, and its checkout needs the transaction id the native SDK generated. SuperwallKit keeps `isCustomProduct` internal, so on iOS it is derived from `customTransactionId != null` | PENDING |
| Δ44 | `PSuperwallEventInfo` field set (Flutter later added the same payloads) | `SuperwallEventInfo.transactionType`, `transactionProduct` (`TransactionProduct`, superseding the drop in Δ17 on both platforms; Android keeps the synthesized `product` too), `attributionMatch`, `pageViewData`, `permissionName`, `paywallIdentifier`, `paywallCount`, `customerCenter` (`CustomerCenterEventInfo`), Android-only `previousCustomerInfo`, `customerInfo`, `webviewError` (sealed `WebviewError`), `resourceUrl`, `shimmerDuration` | add | Typed fields next to the `params` entries, which stay. `AttributionMatchInfo.provider`/`confidence` and `PageViewData.navigationType` stay strings so a new native value still arrives | PENDING |
| Δ45 | `PStoreProduct`, `PProduct`, `PPaywallInfo`, `PStoreTransaction`, `PRedemptionPaywallInfo` field sets | `StoreProduct.introductoryDiscount/discounts/billingPlanType/isBillingPlanAvailable/introOfferToken` (iOS); `PaywallProduct.billingPlanType` (iOS), `trialDays`, `basePlanIdentifier/offer/fullIdentifier` (Android); `PaywallInfo.databaseId/customerInfo` (Android); `StoreTransaction.id` (Android); `RedemptionPaywallInfo.product` (`RedemptionPaywallProduct`), `RedemptionResult.stripeSubscriptionIds/paddleSubscriptionIds`; `PaywallSkippedReason.UserIsSubscribed` (Android) | add | Fields one native has and the other lacks are nullable or empty on the platform without them. `PaywallProduct.id` on Android stays the full identifier it always was | PENDING |
| Δ46 | — (absent) | `Superwall.configurationStatusFlow: StateFlow<ConfigurationStatus>`, `Superwall.eventsFlow: Flow<SuperwallEventInfo>`; `SuperwallOptions.localResources: Map<String, PaywallResource>`, `devServer: DevServer?` (iOS), `customNetworkEnvironment`, `NetworkEnvironment.LOCAL`; Android-only `showAlert`, `openRestoreOnWeb`, `dismissForNextPaywall`, `externalAccountId`, `teardown()` | add | `eventsFlow` is the common form of superwall-android's `placements` flow and works on iOS too. `PaywallResource.FromUri` takes a string because a `Uri`/`URL` is a platform type. `teardown` resets the facade's configure-once state only when the native SDK was torn down | PENDING |
| Δ47 | `PLogging.level` defaulted to `info` in the Flutter public layer | `Logging.level: LogLevel? = null` | type (behavior) | Writing `INFO` on every configure overrode superwall-android's `warn` default. `null` leaves each native default alone, a narrow exception to Δ13 | PENDING |
