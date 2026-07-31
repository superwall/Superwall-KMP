//
//  SuperwallKMPBridge.swift
//  SuperwallKMPBridge
//
//  The @objc control-plane facade for the Superwall KMP SDK (plan §5). One method
//  per operation the Kotlin `SuperwallBridge` interface needs — the same set the
//  Flutter host (SuperwallHost.swift) implements, minus Pigeon transport, plus the
//  fidelity fixes recorded in plan §3.4/§5.2.
//
//  Conventions:
//  - The full public surface is @objc-compatible: NSObject classes, @objc Int enums,
//    completion blocks, NSDictionary/NSArray/NSNumber. No Swift-only types.
//  - Async SuperwallKit APIs wrap in Task { } and report through completion handlers.
//    Completions fire on the queue the underlying API completes on — main-thread
//    delivery is the Kotlin side's job (plan §6), EXCEPT where documented.
//  - Getters that would trip SuperwallKit's not-configured assertion are guarded
//    with Superwall.isInitialized and return documented pre-configure defaults;
//    the real pre-configure guard lives in the Kotlin façade (plan §7).
//

import Combine
import Foundation
import SuperwallKit

// MARK: - Option envelopes (input direction: Kotlin → SuperwallKit)
// A nil/NSNumber-nil field means "not set — keep the SuperwallKit default",
// mirroring the Pigeon PSuperwallOptions semantics.

/// Mirror of SuperwallOptions.NetworkEnvironment (the Pigeon subset).
@objc(SWBNetworkEnvironment)
public enum SWBNetworkEnvironment: Int {
  case release = 0
  case releaseCandidate = 1
  case developer = 2
}

/// Mirror of SuperwallOptions.TestModeBehavior.
@objc(SWBTestModeBehavior)
public enum SWBTestModeBehavior: Int {
  case automatic = 0
  case whenEnabledForUser = 1
  case never = 2
  case always = 3
}

/// Mirror of PaywallOptions.TransactionBackgroundView.
@objc(SWBTransactionBackgroundView)
public enum SWBTransactionBackgroundView: Int {
  case spinner = 0
  case none = 1
}

/// Mirror of SuperwallKit.LogScope (all 24 cases in 4.16.2).
@objc(SWBLogScope)
public enum SWBLogScope: Int {
  case localizationManager = 0
  case bounceButton = 1
  case coreData = 2
  case configManager = 3
  case identityManager = 4
  case debugManager = 5
  case debugViewController = 6
  case localizationViewController = 7
  case gameControllerManager = 8
  case device = 9
  case network = 10
  case paywallEvents = 11
  case productsManager = 12
  case storeKitManager = 13
  case placements = 14
  case receipts = 15
  case superwallCore = 16
  case paywallPresentation = 17
  case transactions = 18
  case paywallViewController = 19
  case cache = 20
  case all = 21
  case analytics = 22
  case webEntitlements = 23

  var native: LogScope {
    switch self {
    case .localizationManager: return .localizationManager
    case .bounceButton: return .bounceButton
    case .coreData: return .coreData
    case .configManager: return .configManager
    case .identityManager: return .identityManager
    case .debugManager: return .debugManager
    case .debugViewController: return .debugViewController
    case .localizationViewController: return .localizationViewController
    case .gameControllerManager: return .gameControllerManager
    case .device: return .device
    case .network: return .network
    case .paywallEvents: return .paywallEvents
    case .productsManager: return .productsManager
    case .storeKitManager: return .storeKitManager
    case .placements: return .placements
    case .receipts: return .receipts
    case .superwallCore: return .superwallCore
    case .paywallPresentation: return .paywallPresentation
    case .transactions: return .transactions
    case .paywallViewController: return .paywallViewController
    case .cache: return .cache
    case .all: return .all
    case .analytics: return .analytics
    case .webEntitlements: return .webEntitlements
    }
  }
}

/// Restore-failed alert text overrides.
@objc(SWBRestoreFailedOptions)
public final class SWBRestoreFailedOptions: NSObject {
  @objc public var title: String?
  @objc public var message: String?
  @objc public var closeButtonTitle: String?

  @objc public override init() {
    super.init()
  }
}

/// Logging options. `level` wraps SWBLogLevel.rawValue; `scopes` wraps
/// SWBLogScope.rawValue values. nil = keep native default.
@objc(SWBLoggingOptions)
public final class SWBLoggingOptions: NSObject {
  @objc public var level: NSNumber?
  @objc public var scopes: [NSNumber]?

  @objc public override init() {
    super.init()
  }
}

/// Paywall presentation options (Pigeon PPaywallOptions field set).
/// Bool fields are NSNumber?; nil = keep native default.
@objc(SWBPaywallOptions)
public final class SWBPaywallOptions: NSObject {
  @objc public var isHapticFeedbackEnabled: NSNumber?
  @objc public var restoreFailed: SWBRestoreFailedOptions?
  @objc public var shouldShowWebRestorationAlert: NSNumber?
  @objc public var shouldShowPurchaseFailureAlert: NSNumber?
  @objc public var shouldPreload: NSNumber?
  @objc public var automaticallyDismiss: NSNumber?
  /// Wraps SWBTransactionBackgroundView.rawValue.
  @objc public var transactionBackgroundView: NSNumber?
  @objc public var overrideProductsByName: [String: String]?
  @objc public var shouldShowWebPurchaseConfirmationAlert: NSNumber?

  @objc public override init() {
    super.init()
  }
}

/// SDK configuration options (Pigeon PSuperwallOptions field set).
/// Bool/Int fields are NSNumber?; nil = keep native default.
@objc(SWBSuperwallOptions)
public final class SWBSuperwallOptions: NSObject {
  @objc public var paywalls: SWBPaywallOptions?
  /// Wraps SWBNetworkEnvironment.rawValue.
  @objc public var networkEnvironment: NSNumber?
  @objc public var isExternalDataCollectionEnabled: NSNumber?
  @objc public var localeIdentifier: String?
  @objc public var isGameControllerEnabled: NSNumber?
  /// Wired for real (dead in the Flutter public layer; plan §3.4).
  @objc public var enableExperimentalDeviceVariables: NSNumber?
  /// Wraps SWBTestModeBehavior.rawValue.
  @objc public var testModeBehavior: NSNumber?
  @objc public var shouldObservePurchases: NSNumber?
  /// iOS-only option.
  @objc public var shouldBypassAppTransactionCheck: NSNumber?
  /// iOS-only option.
  @objc public var maxConfigRetryCount: NSNumber?
  @objc public var logging: SWBLoggingOptions?

  @objc public override init() {
    super.init()
  }

  /// Builds the SuperwallKit options object (port of OptionsMapper.toSdkOptions).
  var native: SuperwallOptions {
    let options = SuperwallOptions()

    if let paywalls = paywalls {
      if let value = paywalls.isHapticFeedbackEnabled {
        options.paywalls.isHapticFeedbackEnabled = value.boolValue
      }
      if let restoreFailed = paywalls.restoreFailed {
        if let title = restoreFailed.title {
          options.paywalls.restoreFailed.title = title
        }
        if let message = restoreFailed.message {
          options.paywalls.restoreFailed.message = message
        }
        if let closeButtonTitle = restoreFailed.closeButtonTitle {
          options.paywalls.restoreFailed.closeButtonTitle = closeButtonTitle
        }
      }
      if let value = paywalls.shouldShowWebRestorationAlert {
        options.paywalls.shouldShowWebRestorationAlert = value.boolValue
      }
      if let value = paywalls.shouldShowPurchaseFailureAlert {
        options.paywalls.shouldShowPurchaseFailureAlert = value.boolValue
      }
      if let value = paywalls.shouldPreload {
        options.paywalls.shouldPreload = value.boolValue
      }
      if let value = paywalls.automaticallyDismiss {
        options.paywalls.automaticallyDismiss = value.boolValue
      }
      if let value = paywalls.transactionBackgroundView,
        let view = SWBTransactionBackgroundView(rawValue: value.intValue) {
        switch view {
        case .spinner:
          options.paywalls.transactionBackgroundView = .spinner
        case .none:
          options.paywalls.transactionBackgroundView = .none
        }
      }
      if let value = paywalls.overrideProductsByName {
        options.paywalls.overrideProductsByName = value
      }
      if let value = paywalls.shouldShowWebPurchaseConfirmationAlert {
        options.paywalls.shouldShowWebPurchaseConfirmationAlert = value.boolValue
      }
    }

    if let value = networkEnvironment,
      let env = SWBNetworkEnvironment(rawValue: value.intValue) {
      switch env {
      case .release:
        options.networkEnvironment = .release
      case .releaseCandidate:
        options.networkEnvironment = .releaseCandidate
      case .developer:
        options.networkEnvironment = .developer
      }
    }
    if let value = isExternalDataCollectionEnabled {
      options.isExternalDataCollectionEnabled = value.boolValue
    }
    options.localeIdentifier = localeIdentifier
    if let value = isGameControllerEnabled {
      options.isGameControllerEnabled = value.boolValue
    }
    if let value = enableExperimentalDeviceVariables {
      options.enableExperimentalDeviceVariables = value.boolValue
    }
    if let value = testModeBehavior,
      let behavior = SWBTestModeBehavior(rawValue: value.intValue) {
      switch behavior {
      case .automatic:
        options.testModeBehavior = .automatic
      case .whenEnabledForUser:
        options.testModeBehavior = .whenEnabledForUser
      case .never:
        options.testModeBehavior = .never
      case .always:
        options.testModeBehavior = .always
      }
    }
    if let value = shouldObservePurchases {
      options.shouldObservePurchases = value.boolValue
    }
    if let value = shouldBypassAppTransactionCheck {
      options.shouldBypassAppTransactionCheck = value.boolValue
    }
    if let value = maxConfigRetryCount {
      options.maxConfigRetryCount = value.intValue
    }
    if let logging = logging {
      if let level = logging.level,
        let logLevel = SWBLogLevel(rawValue: level.intValue) {
        options.logging.level = logLevel.native
      }
      if let scopes = logging.scopes {
        var nativeScopes = Set<LogScope>()
        for scope in scopes {
          if let swbScope = SWBLogScope(rawValue: scope.intValue) {
            nativeScopes.insert(swbScope.native)
          }
        }
        options.logging.scopes = nativeScopes
      }
    }

    return options
  }
}

/// Identity options for identify().
@objc(SWBIdentityOptions)
public final class SWBIdentityOptions: NSObject {
  @objc public let restorePaywallAssignments: Bool

  @objc public init(restorePaywallAssignments: Bool) {
    self.restorePaywallAssignments = restorePaywallAssignments
    super.init()
  }
}

// MARK: - Observation token

/// Cancellation token for observe* subscriptions. Retain it for as long as
/// the observation should live; call cancel() (or let it deinit) to stop.
@objc(SWBObservation)
public final class SWBObservation: NSObject {
  private var onCancel: (() -> Void)?

  init(onCancel: @escaping () -> Void) {
    self.onCancel = onCancel
    super.init()
  }

  /// Stops the observation. Idempotent.
  @objc public func cancel() {
    onCancel?()
    onCancel = nil
  }

  deinit {
    cancel()
  }
}

// MARK: - LogLevel conversion

extension SWBLogLevel {
  var native: LogLevel {
    switch self {
    case .debug: return .debug
    case .info: return .info
    case .warn: return .warn
    case .error: return .error
    case .none: return .none
    }
  }

  init(_ native: LogLevel) {
    switch native {
    case .debug: self = .debug
    case .info: self = .info
    case .warn: self = .warn
    case .error: self = .error
    case .none: self = .none
    @unknown default: self = .none
    }
  }
}

// MARK: - Facade

/// The control-plane facade the Kotlin `IosSuperwallBridge` forwards to.
/// Use the `shared` instance; all state (delegate forwarder, purchase controller,
/// handler wrappers, observations) is owned here so Kotlin-side references stay thin.
@objc(SWBSuperwallBridge)
public final class SWBSuperwallBridge: NSObject {
  /// The bridge version, asserted by the klib at configure time against the
  /// constant it was compiled against (plan §5.2). Bumped in lockstep with
  /// the superwall-kmp release that ships this bridge.
  @objc public static let bridgeVersion: String = SWBBridgeVersion

  /// NSError domain used for all bridge-synthesized errors.
  @objc public static let errorDomain: String = "com.superwall.kmp.bridge"

  /// The shared bridge instance.
  @objc(sharedBridge)
  public static let shared = SWBSuperwallBridge()

  // Retained collaborators (plan §6.4: never rely on ObjC retaining
  // Kotlin-implemented objects).
  private var delegateForwarder: BridgeDelegateForwarder?
  private var purchaseControllerForwarder: BridgePurchaseControllerForwarder?
  private var handlerWrappers: [ObjectIdentifier: PresentationHandlerWrapper] = [:]
  private let handlerLock = NSLock()

  private override init() {
    super.init()
  }

  // MARK: Configuration

  /// Configures the shared Superwall instance.
  ///
  /// Unlike native SuperwallKit — whose completion is a bare `() -> Void` — this
  /// completion carries a derived outcome (plan §5.2): when the native completion
  /// fires, the bridge reads `Superwall.shared.configurationStatus` and maps
  /// `.configured` → (`.configured`, nil), `.failed` → (`.failed`, NSError),
  /// `.pending` → (`.pending`, nil) (treated as success by the Kotlin side, with
  /// a logged warning; it should not occur post-completion).
  ///
  /// Re-invocation is a native-level no-op: SuperwallKit logs a warning and
  /// invokes the completion immediately.
  ///
  /// - Parameters:
  ///   - apiKey: The Superwall public API key.
  ///   - options: Optional configuration; nil keeps every SuperwallKit default.
  ///   - purchaseController: Optional Kotlin-implemented purchase controller;
  ///     when non-nil the bridge registers a forwarding SuperwallKit
  ///     PurchaseController for the lifetime of the process.
  ///   - completion: Derived configuration outcome; may fire on any queue.
  @objc public func configure(
    apiKey: String,
    options: SWBSuperwallOptions?,
    purchaseController: SWBPurchaseController?,
    completion: ((SWBConfigurationStatus, NSError?) -> Void)?
  ) {
    let controller: PurchaseController?
    if let purchaseController = purchaseController {
      let forwarder = BridgePurchaseControllerForwarder(bridgeController: purchaseController)
      self.purchaseControllerForwarder = forwarder
      controller = forwarder
    } else {
      controller = nil
    }

    Superwall.configure(
      apiKey: apiKey,
      purchaseController: controller,
      options: options?.native,
      completion: {
        guard let completion = completion else { return }
        let outcome = SWBSuperwallBridge.deriveConfigureOutcome(
          from: Superwall.shared.configurationStatus
        )
        completion(outcome.status, outcome.error)
      }
    )
  }

  /// Status → (status, error) derivation used by configure's completion.
  /// Factored out so it is unit-testable without network (plan §5.2).
  static func deriveConfigureOutcome(
    from status: ConfigurationStatus
  ) -> (status: SWBConfigurationStatus, error: NSError?) {
    switch status {
    case .configured:
      return (.configured, nil)
    case .failed:
      return (
        .failed,
        NSError(
          domain: errorDomain,
          code: 1,
          userInfo: [
            NSLocalizedDescriptionKey:
              "Superwall configuration failed. Check the API key and network reachability."
          ]
        )
      )
    case .pending:
      // Should not happen after the native completion fires; surfaced as pending
      // so the Kotlin side can log-and-continue (plan §5.2).
      return (.pending, nil)
    @unknown default:
      return (.pending, nil)
    }
  }

  /// Installs (or clears, with nil) the Kotlin-implemented delegate.
  /// The bridge retains the forwarder; pass nil to remove it.
  @objc public func setDelegate(_ delegate: SWBBridgeDelegate?) {
    if let delegate = delegate {
      let forwarder = BridgeDelegateForwarder(bridgeDelegate: delegate)
      delegateForwarder = forwarder
      Superwall.shared.delegate = forwarder
    } else {
      delegateForwarder = nil
      Superwall.shared.delegate = nil
    }
  }

  /// Resets the userId, on-device paywall assignments, and stored data.
  @objc public func reset() {
    Superwall.shared.reset()
  }

  /// Current configuration status. Safe pre-configure (returns .pending).
  @objc public var configurationStatus: SWBConfigurationStatus {
    guard Superwall.isInitialized else { return .pending }
    switch Superwall.shared.configurationStatus {
    case .configured: return .configured
    case .failed: return .failed
    case .pending: return .pending
    @unknown default: return .pending
    }
  }

  /// True once configuration completed successfully. Safe pre-configure (false).
  @objc public var isConfigured: Bool {
    return configurationStatus == .configured
  }

  /// True immediately after Superwall.configure was called. Safe pre-configure (false).
  @objc public var isInitialized: Bool {
    return Superwall.isInitialized
  }

  // MARK: Identity & attributes

  /// Links the given userId to Superwall's generated alias.
  @objc public func identify(userId: String, options: SWBIdentityOptions?) {
    let nativeOptions = options.map {
      IdentityOptions(restorePaywallAssignments: $0.restorePaywallAssignments)
    }
    Superwall.shared.identify(userId: userId, options: nativeOptions)
  }

  /// The current app user id (or generated alias).
  @objc public var userId: String {
    return Superwall.shared.userId
  }

  /// Whether identify has been called for the current user.
  @objc public var isLoggedIn: Bool {
    return Superwall.shared.isLoggedIn
  }

  /// Snapshot of the user attributes (sanitized).
  @objc public func getUserAttributes() -> [String: Any] {
    return SWBValueSanitizer.sanitize(Superwall.shared.userAttributes) ?? [:]
  }

  /// MERGES the given attributes into the existing set (native semantics):
  /// an NSNull value REMOVES that key; absent keys are untouched.
  @objc public func setUserAttributes(_ attributes: [String: Any]) {
    var merged: [String: Any?] = [:]
    for (key, value) in attributes {
      merged[key] = value is NSNull ? Optional<Any>.none : value
    }
    Superwall.shared.setUserAttributes(merged)
  }

  /// Async snapshot of the device attributes (sanitized).
  @objc public func getDeviceAttributes(_ completion: @escaping ([String: Any]) -> Void) {
    Task {
      let attributes = await Superwall.shared.getDeviceAttributes()
      completion(SWBValueSanitizer.sanitize(attributes) ?? [:])
    }
  }

  /// The locale override used for paywall localization, or nil.
  @objc public var localeIdentifier: String? {
    get { return Superwall.shared.localeIdentifier }
    set { Superwall.shared.localeIdentifier = newValue }
  }

  /// The SDK log level.
  @objc public var logLevel: SWBLogLevel {
    get { return SWBLogLevel(Superwall.shared.logLevel) }
    set { Superwall.shared.logLevel = newValue.native }
  }

  /// Sets one integration attribute; nil value clears it.
  @objc public func setIntegrationAttribute(_ attribute: SWBIntegrationAttribute, value: String?) {
    Superwall.shared.setIntegrationAttribute(attribute.native, value)
  }

  /// Batch-sets integration attributes. Keys are NSNumbers wrapping
  /// SWBIntegrationAttribute.rawValue; values are String or NSNull (= clear).
  /// Unknown keys are skipped (degrade, never crash).
  @objc public func setIntegrationAttributes(_ attributes: [NSNumber: Any]) {
    var native: [IntegrationAttribute: String?] = [:]
    for (key, value) in attributes {
      guard let attribute = SWBIntegrationAttribute(rawValue: key.intValue) else { continue }
      if let string = value as? String {
        native[attribute.native] = string
      } else {
        native[attribute.native] = String?.none
      }
    }
    Superwall.shared.setIntegrationAttributes(native)
  }

  /// Snapshot of the currently-set integration attributes, keyed by wire name.
  @objc public func getIntegrationAttributes() -> [String: String] {
    return Superwall.shared.integrationAttributes
  }

  // MARK: Entitlements & subscription status

  /// Snapshot of the entitlements info.
  @objc public func getEntitlements() -> SWBEntitlements {
    return SWBEntitlements(Superwall.shared.entitlements)
  }

  /// Entitlements granted by any of the given product ids.
  @objc public func getEntitlements(byProductIds productIds: [String]) -> [SWBEntitlement] {
    let result = Superwall.shared.entitlements.byProductIds(Set(productIds))
    return result.map { SWBEntitlement($0) }
  }

  /// The current subscription status.
  @objc public func getSubscriptionStatus() -> SWBSubscriptionStatus {
    return SWBSubscriptionStatus(Superwall.shared.subscriptionStatus)
  }

  /// Sets the subscription status (PurchaseController flows). Only entitlement
  /// ids are consumed (matching the native Entitlement(id:) initializer).
  @objc public func setSubscriptionStatus(_ status: SWBSubscriptionStatus) {
    Superwall.shared.subscriptionStatus = status.native
  }

  /// Subscribes to subscription-status changes via Combine. Emits the current
  /// value immediately, then every change, on the publisher's queue (main).
  /// Cancel via the returned token; the token is retained by the caller.
  @objc public func observeSubscriptionStatus(
    _ onChange: @escaping (SWBSubscriptionStatus) -> Void
  ) -> SWBObservation {
    let cancellable = Superwall.shared.$subscriptionStatus
      .sink { status in
        onChange(SWBSubscriptionStatus(status))
      }
    return SWBObservation { cancellable.cancel() }
  }

  /// Async snapshot of the customer info.
  @objc public func getCustomerInfo(_ completion: @escaping (SWBCustomerInfo) -> Void) {
    Task {
      let customerInfo = await Superwall.shared.getCustomerInfo()
      completion(SWBCustomerInfo(customerInfo))
    }
  }

  /// Subscribes to customer-info changes via Combine (deduplicated). Emits the
  /// current value immediately, then every distinct change.
  /// Cancel via the returned token.
  @objc public func observeCustomerInfo(
    _ onChange: @escaping (SWBCustomerInfo) -> Void
  ) -> SWBObservation {
    let cancellable = Superwall.shared.$customerInfo
      .removeDuplicates()
      .sink { customerInfo in
        onChange(SWBCustomerInfo(customerInfo))
      }
    return SWBObservation { cancellable.cancel() }
  }

  /// Confirms all experiment assignments and returns them.
  @objc public func confirmAllAssignments(
    _ completion: @escaping ([SWBConfirmedAssignment]) -> Void
  ) {
    Task {
      let assignments = await Superwall.shared.confirmAllAssignments()
      completion(assignments.map { SWBConfirmedAssignment($0) })
    }
  }

  /// Restores purchases (via the registered PurchaseController when present).
  @objc public func restorePurchases(_ completion: @escaping (SWBRestorationResult) -> Void) {
    Superwall.shared.restorePurchases { result in
      completion(SWBRestorationResult(result))
    }
  }

  // MARK: Presentation

  /// Registers a placement. Fire-and-forget, like the native API.
  ///
  /// - Parameters:
  ///   - placement: The placement name.
  ///   - params: Optional sanit-izable placement params.
  ///   - handler: Optional Kotlin-implemented presentation handler. The bridge
  ///     retains a per-registration wrapper and releases it on dismiss
  ///     (closeReason != none), skip, or error (plan §6.4 — the error release is a
  ///     deliberate fix of the Flutter host's leak).
  ///   - feature: Optional feature closure, invoked per SuperwallKit's gating rules.
  @objc public func register(
    placement: String,
    params: [String: Any]?,
    handler: SWBPaywallPresentationHandler?,
    feature: (() -> Void)?
  ) {
    let nativeHandler: PaywallPresentationHandler?
    if let handler = handler {
      let wrapper = PresentationHandlerWrapper(bridgeHandler: handler) { [weak self] finished in
        guard let self = self else { return }
        self.handlerLock.lock()
        self.handlerWrappers.removeValue(forKey: ObjectIdentifier(finished))
        self.handlerLock.unlock()
      }
      handlerLock.lock()
      handlerWrappers[ObjectIdentifier(wrapper)] = wrapper
      handlerLock.unlock()
      nativeHandler = wrapper.nativeHandler
    } else {
      nativeHandler = nil
    }

    if let feature = feature {
      Superwall.shared.register(
        placement: placement,
        params: params,
        handler: nativeHandler,
        feature: feature
      )
    } else {
      Superwall.shared.register(
        placement: placement,
        params: params,
        handler: nativeHandler
      )
    }
  }

  /// Preemptively evaluates what registering the placement would do.
  @objc public func getPresentationResult(
    placement: String,
    params: [String: Any]?,
    completion: @escaping (SWBPresentationResult) -> Void
  ) {
    Superwall.shared.getPresentationResult(
      forPlacement: placement,
      params: params
    ) { result in
      completion(SWBPresentationResult(result))
    }
  }

  /// Dismisses the presented paywall, if any. Completion fires after dismissal.
  @objc public func dismiss(_ completion: (() -> Void)?) {
    Task {
      await Superwall.shared.dismiss()
      completion?()
    }
  }

  /// Whether a paywall is currently presented.
  @objc public var isPaywallPresented: Bool {
    return Superwall.shared.isPaywallPresented
  }

  /// Info for the most recently presented paywall, if any.
  @objc public var latestPaywallInfo: SWBPaywallInfo? {
    return Superwall.shared.latestPaywallInfo.map { SWBPaywallInfo($0) }
  }

  /// Preloads all paywalls the user could see.
  @objc public func preloadAllPaywalls() {
    Superwall.shared.preloadAllPaywalls()
  }

  /// Preloads paywalls for the given placement names.
  @objc public func preloadPaywalls(placements: [String]) {
    Superwall.shared.preloadPaywalls(forPlacements: Set(placements))
  }

  /// Shows/hides the paywall's transaction spinner.
  @objc public func togglePaywallSpinner(isHidden: Bool) {
    Superwall.shared.togglePaywallSpinner(isHidden: isHidden)
  }

  /// Routes a deep link. Uses the STATIC native entry point, so this works
  /// before/around configure (deep-link cold start; guard-exempt, plan §7).
  /// Returns false for unparseable URLs.
  @objc public func handleDeepLink(_ url: String) -> Bool {
    guard let url = URL(string: url) else { return false }
    return Superwall.handleDeepLink(url)
  }

  /// Per-paywall product override mapping (product name → product id), or nil.
  @objc public var overrideProductsByName: [String: String]? {
    get { return Superwall.shared.overrideProductsByName }
    set { Superwall.shared.overrideProductsByName = newValue }
  }

  /// Overrides the interface style; `.automatic` clears the override.
  @objc public func setInterfaceStyle(_ style: SWBInterfaceStyle) {
    switch style {
    case .automatic:
      Superwall.shared.setInterfaceStyle(to: nil)
    case .light:
      Superwall.shared.setInterfaceStyle(to: .light)
    case .dark:
      Superwall.shared.setInterfaceStyle(to: .dark)
    }
  }

  // MARK: Platform no-ops

  /// Play-Billing-only API: consuming purchases has no iOS counterpart. This is a
  /// documented no-op that echoes the token back (Flutter parity; plan Open
  /// Question #6 — the Kotlin façade documents the echo semantics).
  @objc public func consume(purchaseToken: String, completion: @escaping (String) -> Void) {
    completion(purchaseToken)
  }
}

/// Swift-side version constant (mirrored @objc as SWBSuperwallBridge.bridgeVersion,
/// which is what the Kotlin klib asserts against at configure; plan §5.2).
public let SWBBridgeVersion = "0.1.0"
