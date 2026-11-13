//
//  SuperwallKMPBridge.swift
//  SuperwallKMPBridge
//
// The @objc control-plane facade for the Superwall KMP SDK. One method
//  per operation the Kotlin `SuperwallBridge` interface needs — the same set the
//  Flutter host (SuperwallHost.swift) implements, minus Pigeon transport, plus the
// fidelity fixes recorded in
//
//  Conventions:
//  - The full public surface is @objc-compatible: NSObject classes, @objc Int enums,
//    completion blocks, NSDictionary/NSArray/NSNumber. No Swift-only types.
//  - Async SuperwallKit APIs wrap in Task { } and report through completion handlers.
//    Completions fire on the queue the underlying API completes on — main-thread
// delivery is the Kotlin side's job, EXCEPT where documented.
//  - Getters that would trip SuperwallKit's not-configured assertion are guarded
//    with Superwall.isInitialized and return documented pre-configure defaults;
// the real pre-configure guard lives in the Kotlin façade.
//

import Combine
import Foundation
import SuperwallKit
import UIKit
import UserNotifications

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

/// Mirror of SuperwallKit.EventTrackingBehavior.
@objc(SWBEventTrackingBehavior)
public enum SWBEventTrackingBehavior: Int {
  case all = 0
  case superwallOnly = 1
  case none = 2
}

/// Mirror of PaywallOptions.TransactionBackgroundView.
@objc(SWBTransactionBackgroundView)
public enum SWBTransactionBackgroundView: Int {
  case spinner = 0
  case none = 1
}

/// Mirror of SuperwallKit.LogScope (all 26 cases of 4.19.1).
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
  case grantedEntitlements = 24
  case customerCenter = 25

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
    case .grantedEntitlements: return .grantedEntitlements
    case .customerCenter: return .customerCenter
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

/// Mirror of SuperwallKit.AdConsent (4.18.0+). Each status is a Bool: true =
/// `.granted`, false = `.denied`.
@objc(SWBAdConsent)
public final class SWBAdConsent: NSObject {
  @objc public let adUserDataGranted: Bool
  @objc public let adPersonalizationGranted: Bool

  @objc public init(adUserDataGranted: Bool, adPersonalizationGranted: Bool) {
    self.adUserDataGranted = adUserDataGranted
    self.adPersonalizationGranted = adPersonalizationGranted
    super.init()
  }

  convenience init(_ native: AdConsent) {
    self.init(
      adUserDataGranted: native.adUserData == .granted,
      adPersonalizationGranted: native.adPersonalization == .granted
    )
  }

  var native: AdConsent {
    return AdConsent(
      adUserData: adUserDataGranted ? .granted : .denied,
      adPersonalization: adPersonalizationGranted ? .granted : .denied
    )
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

/// Notification-permissions-denied alert text (PaywallOptions.NotificationPermissionsDenied).
/// Every field is set by Kotlin; the object's presence turns the alert on.
@objc(SWBNotificationPermissionsDeniedOptions)
public final class SWBNotificationPermissionsDeniedOptions: NSObject {
  @objc public var title: String?
  @objc public var message: String?
  @objc public var actionButtonTitle: String?
  @objc public var closeButtonTitle: String?

  @objc public override init() {
    super.init()
  }

  var native: PaywallOptions.NotificationPermissionsDenied {
    let alert = PaywallOptions.NotificationPermissionsDenied()
    if let title = title {
      alert.title = title
    }
    if let message = message {
      alert.message = message
    }
    if let actionButtonTitle = actionButtonTitle {
      alert.actionButtonTitle = actionButtonTitle
    }
    if let closeButtonTitle = closeButtonTitle {
      alert.closeButtonTitle = closeButtonTitle
    }
    return alert
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
  /// nil = no alert (SuperwallKit's default).
  @objc public var notificationPermissionsDenied: SWBNotificationPermissionsDeniedOptions?

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
  /// Wraps SWBEventTrackingBehavior.rawValue.
  @objc public var eventTrackingBehavior: NSNumber?
  @objc public var localeIdentifier: String?
  @objc public var isGameControllerEnabled: NSNumber?
  /// Wired for real (dead in the Flutter public layer).
  @objc public var enableExperimentalDeviceVariables: NSNumber?
  /// Wraps SWBTestModeBehavior.rawValue.
  @objc public var testModeBehavior: NSNumber?
  @objc public var shouldObservePurchases: NSNumber?
  /// iOS-only option.
  @objc public var shouldBypassAppTransactionCheck: NSNumber?
  /// iOS-only option.
  @objc public var maxConfigRetryCount: NSNumber?
  /// iOS-only option. Wraps SuperwallOptions.StoreKitVersion.rawValue
  /// (0 = storeKit1, 1 = storeKit2); nil = SuperwallKit's choice.
  @objc public var storeKitVersion: NSNumber?
  @objc public var logging: SWBLoggingOptions?
  /// nil = leave unset, so a TCF consent banner's stored answer applies.
  @objc public var adConsent: SWBAdConsent?
  /// nil = SuperwallKit's default Customer Center configuration.
  @objc public var customerCenter: SWBCustomerCenterConfiguration?

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
      if let value = paywalls.notificationPermissionsDenied {
        options.paywalls.notificationPermissionsDenied = value.native
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
    // Applied AFTER the deprecated boolean: SuperwallKit derives the boolean
    // from eventTrackingBehavior, so the typed field (when set) must win.
    if let value = eventTrackingBehavior,
      let behavior = SWBEventTrackingBehavior(rawValue: value.intValue) {
      switch behavior {
      case .all:
        options.eventTrackingBehavior = .all
      case .superwallOnly:
        options.eventTrackingBehavior = .superwallOnly
      case .none:
        options.eventTrackingBehavior = .none
      }
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
    if let value = storeKitVersion,
      let version = SuperwallOptions.StoreKitVersion(rawValue: value.intValue) {
      options.storeKitVersion = version
    }
    if let adConsent = adConsent {
      options.adConsent = adConsent.native
    }
    if let customerCenter = customerCenter {
      options.customerCenter = customerCenter.native
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
  /// The bridge version: the superwall-kmp release that ships this bridge.
  /// Informational only, nothing checks it at runtime; bump it with
  /// version.env. (The version reported to SuperwallKit comes from Kotlin, see
  /// `setPlatformWrapper`.)
  @objc public static let bridgeVersion: String = SWBBridgeVersion

  /// NSError domain used for all bridge-synthesized errors.
  @objc public static let errorDomain: String = "com.superwall.kmp.bridge"

  /// The shared bridge instance.
  @objc(sharedBridge)
  public static let shared = SWBSuperwallBridge()

  // Retained collaborators (never rely on ObjC retaining
  // Kotlin-implemented objects).
  private var delegateForwarder: BridgeDelegateForwarder?
  private var purchaseControllerForwarder: BridgePurchaseControllerForwarder?
  private var handlerWrappers: [ObjectIdentifier: PresentationHandlerWrapper] = [:]
  private let handlerLock = NSLock()
  /// Whether the app set an ad consent through this bridge. SuperwallKit's own
  /// flag (`SuperwallOptions.isAdConsentSet`) is internal, and its getter
  /// returns the all-granted default before an assignment, so the bridge keeps
  /// its own to report "not set" as nil, as superwall-android's Unspecified does.
  private var isAdConsentSet = false

  private override init() {
    super.init()
  }

  // MARK: Configuration

  /// Configures the shared Superwall instance.
  ///
  /// Unlike native SuperwallKit — whose completion is a bare `() -> Void` — this
  /// completion carries a derived outcome when the native completion
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
    // A repeat configure is a native-level no-op; never clobber the forwarder
    // the first configure handed to SuperwallKit.
    let controller: PurchaseController?
    if let purchaseController = purchaseController, !Superwall.isInitialized {
      let forwarder = BridgePurchaseControllerForwarder(bridgeController: purchaseController)
      self.purchaseControllerForwarder = forwarder
      controller = forwarder
    } else {
      controller = nil
    }

    if options?.adConsent != nil, !Superwall.isInitialized {
      isAdConsentSet = true
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
  /// Factored out so it is unit-testable without network.
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
      // so the Kotlin side can log-and-continue.
      return (.pending, nil)
    @unknown default:
      return (.pending, nil)
    }
  }

  /// Reports the wrapper SDK and its version to SuperwallKit, which sends them
  /// with its requests and device attributes. Kotlin calls this right after
  /// `configure` with the superwall-kmp version. A no-op before configure, where
  /// `Superwall.shared` would be a throwaway instance.
  @objc public func setPlatformWrapper(_ wrapper: String, version: String) {
    guard Superwall.isInitialized else { return }
    Superwall.shared.setPlatformWrapper(wrapper, version: version)
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

  /// Current configuration status. Safe pre-configure (returns.pending).
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
  /// value immediately, then every change, usually on the thread that changed
  /// the status, which is often a background one.
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
  /// Cancel via the returned token. The Kotlin side does not use this: it feeds
  /// `customerInfoFlow` from the delegate's `customerInfoDidChange` instead, as
  /// on Android, since using both emitted every change twice.
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

  /// Fetches fresh customer info (SuperwallKit 4.19.0+), then returns it.
  @objc public func refreshCustomerInfo(_ completion: @escaping (SWBCustomerInfo) -> Void) {
    Task {
      let customerInfo = await Superwall.shared.refreshCustomerInfo()
      completion(SWBCustomerInfo(customerInfo))
    }
  }

  /// The entitlements granted outside the store (SuperwallKit 4.17.0+).
  @objc public func getGrantedEntitlements() -> [SWBEntitlement] {
    return Superwall.shared.grantedEntitlements.map { SWBEntitlement($0) }
  }

  /// Replaces the granted entitlements. Only ids cross the bridge, matching the
  /// native `Entitlement(id:)` initializer (as `setSubscriptionStatus` does).
  @objc public func setGrantedEntitlementIds(_ ids: [String]) {
    Superwall.shared.grantedEntitlements = Set(ids.map { Entitlement(id: $0) })
  }

  /// The stored experiment assignments, without confirming any.
  @objc public func getAssignments() -> [SWBConfirmedAssignment] {
    return Superwall.shared.getAssignments().map { SWBConfirmedAssignment($0) }
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

  /// Purchases the product with the given identifier outside of a paywall.
  /// SuperwallKit's `purchase` takes a StoreProduct, so the product is fetched
  /// first; an identifier the store doesn't know completes with `.failed`, as
  /// superwall-android's `purchase(productId)` does.
  @objc public func purchase(productId: String, completion: @escaping (SWBPurchaseResult) -> Void) {
    Task {
      let products = await Superwall.shared.products(for: [productId])
      guard let product = products.first(where: { $0.productIdentifier == productId }) else {
        completion(SWBPurchaseResult(result: .failed, errorMessage: "Product with id \(productId) not found"))
        return
      }
      let result = await Superwall.shared.purchase(product)
      completion(SWBPurchaseResult(result))
    }
  }

  /// Fetches the store products for the given identifiers. Identifiers the
  /// store doesn't know are left out.
  @objc public func getProducts(productIds: [String], completion: @escaping ([SWBStoreProduct]) -> Void) {
    Task {
      let products = await Superwall.shared.products(for: Set(productIds))
      completion(products.map { SWBStoreProduct($0) })
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
  /// (closeReason != none), skip, or error (the error release is a
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
  /// before/around configure (deep-link cold start; guard-exempt).
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

  /// The ad consent the app set, or nil while it hasn't set one.
  @objc public func adConsent() -> SWBAdConsent? {
    guard isAdConsentSet else { return nil }
    return SWBAdConsent(Superwall.shared.adConsent)
  }

  /// Sets the ad consent (SuperwallKit 4.18.0+).
  @objc public func setAdConsent(_ consent: SWBAdConsent) {
    Superwall.shared.adConsent = consent.native
    isAdConsentSet = true
  }

  /// Fetches the configuration again. Fire-and-forget.
  @objc public func refreshConfiguration() {
    Superwall.shared.refreshConfiguration(completion: nil)
  }

  /// The runtime event tracking behavior, as an SWBEventTrackingBehavior raw
  /// value (a plain Int so Kotlin reads it like the option's NSNumber).
  @objc public func eventTrackingBehaviorRawValue() -> Int {
    switch Superwall.shared.eventTrackingBehavior {
    case .all: return SWBEventTrackingBehavior.all.rawValue
    case .superwallOnly: return SWBEventTrackingBehavior.superwallOnly.rawValue
    case .none: return SWBEventTrackingBehavior.none.rawValue
    @unknown default: return SWBEventTrackingBehavior.all.rawValue
    }
  }

  /// Sets the runtime event tracking behavior from an SWBEventTrackingBehavior
  /// raw value. Unknown values are ignored.
  @objc public func setEventTrackingBehaviorRawValue(_ rawValue: Int) {
    guard let behavior = SWBEventTrackingBehavior(rawValue: rawValue) else { return }
    switch behavior {
    case .all: Superwall.shared.eventTrackingBehavior = .all
    case .superwallOnly: Superwall.shared.eventTrackingBehavior = .superwallOnly
    case .none: Superwall.shared.eventTrackingBehavior = .none
    }
  }

  /// Removes Superwall's pending local notifications (trial reminders). SuperwallKit
  /// exposes this as a UNUserNotificationCenter extension rather than on Superwall.
  @objc public func cancelAllScheduledNotifications() {
    UNUserNotificationCenter.current().removeAllPendingSuperwallNotificationRequests()
  }

  // MARK: Customer Center
  // These stay in the class body, not in an extension next to the Customer
  // Center types: Swift exports an extension's @objc members as an ObjC
  // category, and Kotlin/Native imports category methods as top-level
  // extension functions that need their own import, not as members.

  /// Presents the Customer Center on the main actor. `onDismiss` runs on the
  /// main thread once it has been dismissed, or straight away when it could
  /// not be shown, as superwall-android does. Presenting while one is shown
  /// does nothing (SuperwallKit logs it), and then `onDismiss` is not called.
  @objc public func presentCustomerCenter(
    configuration: SWBCustomerCenterConfiguration?,
    delegate: SWBCustomerCenterDelegate?,
    onDismiss: (() -> Void)?
  ) {
    let nativeConfiguration = configuration?.native
    Task { @MainActor in
      let forwarder = delegate.map { BridgeCustomerCenterDelegateForwarder(bridgeDelegate: $0) }
      // One gate for SuperwallKit's dismissal and the refusal below.
      let once = OnceGate()
      let onDismissOnce: () -> Void = {
        guard once.tryPass() else { return }
        onDismiss?()
      }
      Superwall.shared.presentCustomerCenter(
        configuration: nativeConfiguration,
        from: nil,
        delegate: forwarder,
        onDismiss: onDismissOnce
      )
      // SuperwallKit only logs a presentation it couldn't start (nothing to
      // present from, or UIKit refused it) and exposes no "is presented"
      // state. UIKit links the presented controller in before `present`
      // returns, so none in the hierarchy now means none was shown.
      if !SWBSuperwallBridge.isCustomerCenterInHierarchy() {
        onDismissOnce()
      }
    }
  }

  /// Dismisses the Customer Center, if one is shown. `completion` fires once
  /// it has gone, or straight away when none is shown.
  @objc public func dismissCustomerCenter(_ completion: @escaping () -> Void) {
    Task { @MainActor in
      // SuperwallKit returns without calling the completion when not configured.
      guard Superwall.isInitialized else {
        completion()
        return
      }
      Superwall.shared.dismissCustomerCenter(completion: completion)
    }
  }

  /// Whether a Customer Center is in any window's chain of presented view
  /// controllers, which is where SuperwallKit presents it.
  @MainActor
  private static func isCustomerCenterInHierarchy() -> Bool {
    for scene in UIApplication.shared.connectedScenes {
      guard let windowScene = scene as? UIWindowScene else { continue }
      for window in windowScene.windows {
        var controller = window.rootViewController
        while let current = controller {
          if current is CustomerCenterViewController {
            return true
          }
          controller = current.presentedViewController
        }
      }
    }
    return false
  }

  // MARK: Platform no-ops

  /// Play-Billing-only API: consuming purchases has no iOS counterpart. This is a
  /// documented no-op that echoes the token back (Flutter parity; plan Open
  /// Question #6 — the Kotlin façade documents the echo semantics).
  @objc public func consume(purchaseToken: String, completion: @escaping (String) -> Void) {
    completion(purchaseToken)
  }
}

/// Swift-side version constant, mirrored @objc as SWBSuperwallBridge.bridgeVersion.
public let SWBBridgeVersion = "0.2.0"
