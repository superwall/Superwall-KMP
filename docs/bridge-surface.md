# Bridge surface map — iOS

**Phase 1 deliverable** (IMPLEMENTATION_PLAN.md §5, §9): the contract between the Kotlin
`internal interface SuperwallBridge`
(`superwall-kmp/src/commonMain/kotlin/com/superwall/sdk/kmp/internal/SuperwallBridge.kt`),
the self-authored `@objc` Swift bridge **SuperwallKMPBridge**
(`bridge/Sources/SuperwallKMPBridge/`), and the underlying **SuperwallKit iOS 4.16.1** calls.

The Swift facade is `@objc(SWBSuperwallBridge)` (singleton `+sharedBridge`,
`SuperwallKMPBridge.swift`). All bridge types use the `SWB` ObjC prefix; payload classes are
immutable `NSObject` subclasses; optional primitives are `NSNumber?`; enum values cross the
boundary as `@objc` Int enums whose rawValues are the wire contract (locked by the bridge's
`ContractStabilityTests`). `bridgeVersion` and `errorDomain`
(`"com.superwall.kmp.bridge"`) are static members.

## Control plane (one row per `SuperwallBridge` member)

| Kotlin bridge member | `@objc` bridge API (SWBSuperwallBridge) | Underlying SuperwallKit call |
|---|---|---|
| `configure(apiKey, purchaseController, options, completion)` | `configure(apiKey:options:purchaseController:completion:)` — completion `(SWBConfigurationStatus, NSError?)`; result **derived** by reading `configurationStatus` after the native bare completion fires (plan §5.2) | `Superwall.configure(apiKey:purchaseController:options:completion:)` + `Superwall.shared.configurationStatus` |
| `reset()` | `reset()` | `Superwall.shared.reset()` |
| `installDelegate(listener)` | `setDelegate(_: SWBBridgeDelegate?)` (Kotlin adapter implements the protocol; bridge keeps an internal `SuperwallDelegate` forwarder) | `Superwall.shared.delegate = …` |
| `isConfigured()` | `isConfigured: Bool` | `Superwall.shared.configurationStatus == .configured` |
| `isInitialized()` | `isInitialized: Bool` | `Superwall.isInitialized` |
| `getConfigurationStatus()` | `configurationStatus: SWBConfigurationStatus` (pre-configure-safe: `.pending`) | `Superwall.shared.configurationStatus` |
| `attachStreams(holder)` | `observeSubscriptionStatus(_:) -> SWBObservation` (emits current then changes) + `observeCustomerInfo(_:) -> SWBObservation` (deduplicated); `SWBObservation.cancel()` tears down | Combine `Superwall.shared.$subscriptionStatus` / `Superwall.shared.$customerInfo` (`.removeDuplicates()`) |
| `getLogLevel()` / `setLogLevel(level)` | `logLevel: SWBLogLevel { get set }` | `Superwall.shared.logLevel` |
| `getUserId()` | `userId: String` | `Superwall.shared.userId` |
| `isLoggedIn()` | `isLoggedIn: Bool` | `Superwall.shared.isLoggedIn` |
| `identify(userId, options)` | `identify(userId:options: SWBIdentityOptions?)` | `Superwall.shared.identify(userId:options:)` |
| `getUserAttributes()` | `getUserAttributes() -> [String: Any]` | `Superwall.shared.userAttributes` |
| `setUserAttributes(attributes)` | `setUserAttributes(_: [String: Any])` — `NSNull` value = remove key (merge semantics) | `Superwall.shared.setUserAttributes(_:)` |
| `setIntegrationAttribute(attribute, value)` | `setIntegrationAttribute(_: SWBIntegrationAttribute, value: String?)` | `Superwall.shared.setIntegrationAttribute(_:_:)` |
| `setIntegrationAttributes(attributes)` | `setIntegrationAttributes(_: [NSNumber: Any])` — key = `SWBIntegrationAttribute.rawValue`; value `String` or `NSNull` (= clear) | `Superwall.shared.setIntegrationAttributes(_:)` |
| `getIntegrationAttributes()` | `getIntegrationAttributes() -> [String: String]` | `Superwall.shared.integrationAttributes` |
| `getDeviceAttributes()` (suspend) | `getDeviceAttributes(_: @escaping ([String: Any]) -> Void)` (`Task { }`-wrapped) | `await Superwall.shared.getDeviceAttributes()` |
| `getLocaleIdentifier()` / `setLocaleIdentifier(id)` | `localeIdentifier: String? { get set }` | `Superwall.shared.localeIdentifier` |
| `getEntitlements()` | `getEntitlements() -> SWBEntitlements` | `Superwall.shared.entitlements` (`.active/.inactive/.all/.web`) |
| `getEntitlementsByProductIds(productIds)` (suspend) | `getEntitlements(byProductIds: [String]) -> [SWBEntitlement]` (synchronous on iOS) | `Superwall.shared.entitlements.byProductIds(Set(_))` |
| `getSubscriptionStatus()` / `setSubscriptionStatus(status)` | `getSubscriptionStatus() -> SWBSubscriptionStatus` / `setSubscriptionStatus(_:)` | `Superwall.shared.subscriptionStatus` |
| `getCustomerInfo()` (suspend) | `getCustomerInfo(_: @escaping (SWBCustomerInfo) -> Void)` | `await Superwall.shared.getCustomerInfo()` |
| `confirmAllAssignments()` (suspend) | `confirmAllAssignments(_: @escaping ([SWBConfirmedAssignment]) -> Void)` | `await Superwall.shared.confirmAllAssignments()` — 4.16.2 returns `[Assignment]` (typealias `ConfirmedAssignment`) |
| `restorePurchases()` (suspend) | `restorePurchases(_: @escaping (SWBRestorationResult) -> Void)` | `Superwall.shared.restorePurchases(completion:)` |
| `registerPlacement(placement, params, handler, feature)` | `register(placement:params:handler: SWBPaywallPresentationHandler?, feature:)` — fresh handler wrapper per call | `Superwall.shared.register(placement:params:handler:feature:)` |
| `getPresentationResult(placement, params)` (suspend) | `getPresentationResult(placement:params:completion: @escaping (SWBPresentationResult) -> Void)` | `Superwall.shared.getPresentationResult(forPlacement:params:)` |
| `dismiss()` (suspend) | `dismiss(_: (() -> Void)?)` | `Superwall.shared.dismiss(completion:)` |
| `isPaywallPresented()` | `isPaywallPresented: Bool` | `Superwall.shared.isPaywallPresented` |
| `getLatestPaywallInfo()` | `latestPaywallInfo: SWBPaywallInfo?` | `Superwall.shared.latestPaywallInfo` |
| `preloadAllPaywalls()` | `preloadAllPaywalls()` | `Superwall.shared.preloadAllPaywalls()` |
| `preloadPaywallsForPlacements(names)` | `preloadPaywalls(placements: [String])` | `Superwall.shared.preloadPaywalls(forPlacements:)` |
| `handleDeepLink(url)` | `handleDeepLink(_: String) -> Bool` — **static** native entry, works pre-configure | `Superwall.handleDeepLink(_:)` |
| `togglePaywallSpinner(isHidden)` | `togglePaywallSpinner(isHidden: Bool)` | `Superwall.shared.togglePaywallSpinner(isHidden:)` |
| `getOverrideProductsByName()` / `setOverrideProductsByName(map)` | `overrideProductsByName: [String: String]? { get set }` | `Superwall.shared.overrideProductsByName` |
| `setInterfaceStyle(style)` | `setInterfaceStyle(_: SWBInterfaceStyle)` — `.automatic` clears the override | `Superwall.shared.setInterfaceStyle(to:)` |
| `enableExperimentalDeviceVariables(enabled)` | no standalone facade call on iOS — set via `SWBSuperwallOptions.enableExperimentalDeviceVariables` at `configure` | `SuperwallOptions.enableExperimentalDeviceVariables` |
| `consume(purchaseToken)` (suspend) | `consume(purchaseToken:completion:)` — documented iOS no-op | none (echoes the token back) |

## Callback protocols (Kotlin implements; all methods required)

| Kotlin seam | `@objc` protocol | Fed by SuperwallKit |
|---|---|---|
| `BridgeListener` (the `DelegateMultiplexer`, installed via `installDelegate`) | `@objc(SWBBridgeDelegate)`: `subscriptionStatusDidChange(from:to:)`, `handleSuperwallEvent(_: SWBEventEnvelope)`, `handleCustomPaywallAction(name:)`, `will/didPresentPaywall`, `will/didDismissPaywall`, `paywallWillOpenURL`, `paywallWillOpenDeepLink`, `handleLog(level:scope:message:info:error:)` (level/scope as `String`; Kotlin maps unmappables to `DEBUG`/`ALL` with raw values preserved), `willRedeemLink`, `didRedeemLink(_: SWBRedemptionResult)`, `handleSuperwallDeepLink(fullURL:pathComponents:queryParameters:)`, `customerInfoDidChange(from:to:)`, `userAttributesDidChange(_:)` | `SuperwallDelegate` (internal Swift forwarder in `BridgeDelegate.swift` destructures associated values into envelopes) |
| user `PurchaseController` (handed to `configure`) | `@objc(SWBPurchaseController)`: `purchase(productId:completion: (SWBPurchaseResult) -> Void)`, `restorePurchases(completion: (SWBRestorationResult) -> Void)` — MUST NOT block: called on the main actor; Kotlin adapter launches a coroutine | `PurchaseController` (`BridgePurchaseController.swift`) |
| `PaywallPresentationHandler` (per-`register` call) | `@objc(SWBPaywallPresentationHandler)`: `onPresent(_:)`, `onDismiss(_:result:)`, `onError(_:)`, `onSkip(_:)`, `onCustomCallback(_:completion:)` — wrapper released on dismiss (`closeReason != none`), skip, **and error** (documented delta: the Flutter host leaked on error) | `PaywallPresentationHandler` (`PresentationHandler.swift`) |

**Threading contract:** delegate/handler callbacks are forwarded synchronously on
SuperwallKit's queue (main actor) — the Kotlin adapters redispatch to the main dispatcher;
facade completion blocks may fire on arbitrary queues.

## SWBEventEnvelope

`handleSuperwallEvent` delivers a single flat envelope (`BridgeDelegate.swift`): the
80-case `SuperwallEvent` destructuring switch runs **in Swift**, where the enum's
associated values are visible, and populates `eventType: SWBEventType` (rawValues 0–79 in
`SuperwallEvent` declaration order; `unknown = 999`) plus these 33 optional fields —
Kotlin maps the envelope 1:1 into `SuperwallEventInfo`:

```
params                    paywallInfo               transaction (SWBStoreTransaction)
product (SWBStoreProduct) transactionType (NSNumber<SWBTransactionType>)
transactionProductId      restoreType               triggerResult
placementName             triggeredPlacementName    deepLinkUrl
deviceAttributes          userAttributes            attempt
missingProductIdentifiers survey                    selectedOption
customResponse            presentationRequestStatus (NSNumber<SWBPaywallPresentationRequestStatus>)
presentationRequestReason error                     message
token                     name                      userEnrichment
deviceEnrichment          integrationAttributes     reviewRequestedCount
permissionName            paywallIdentifier         paywallCount
attributionMatch (SWBAttributionMatchInfo)          pageViewData (SWBPageViewData)
```

## SuperwallKit 4.16.1 deltas (vs the Flutter host / plan §3.4 assumptions)

Found by the bridge author while verifying every referenced symbol against the 4.16.2
sources (all absorbed into the bridge and the enums above):

- `SuperwallEvent` now has **80 cases** (plan assumed ~73): added `attributionMatch`,
  `paywallPreloadStart` / `paywallPreloadComplete(paywallCount:)`,
  `stripeCheckoutStart/Submit/Complete/Fail`, `testModeModalOpen/Close`,
  `paywallPageView`; the permission events now carry
  `(permissionName, paywallIdentifier)`.
- `paywallResourceLoadFail` does **not** exist on iOS (Android-only event type).
- `IntegrationAttribute` has **23 cases** (+`firebaseInstallationId`, `singularDeviceId`;
  rawValues `adjustId = 0 … singularDeviceId = 22`).
- `LogScope` has **24 cases** (+`analytics`, `webEntitlements`) — `SWBLogScope` is the
  native superset.
- `ProductStore` gained `custom` (`SWBProductStore` has 7 cases).
- `confirmAllAssignments()` returns `[Assignment]` (`ConfirmedAssignment` is now a
  typealias).
- Flutter-host mislabels corrected in the envelope (plan §3.4): `transactionRestore` is
  its own event type (rawValue 20, not folded into `transactionComplete` = 17);
  `transactionComplete` carries `transactionType`; `nonRecurringProductPurchase` carries
  `transactionProductId`.
- Handler wrappers are released on dismiss/skip **and** error (deliberate fix of the
  Flutter host's leak).

Full report: bridge author output, task `w0zf7l1jc` (2026-07-31). Payload-class field
lists and the complete `@objc` Int-enum inventory live in
`bridge/Sources/SuperwallKMPBridge/Envelopes.swift` and are locked by
`bridge/Tests/SuperwallKMPBridgeTests/EnvelopeTests.swift`.
