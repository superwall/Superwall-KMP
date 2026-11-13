//
//  CustomerCenter.swift
//  SuperwallKMPBridge
//
//  The Customer Center (SuperwallKit 4.18.0+): configuration envelopes Kotlin
//  fills in, the action/purchase envelopes it reads back, the @objc delegate
//  protocol Kotlin implements and the SuperwallKit.CustomerCenterDelegate that
//  forwards to it. The present/dismiss facade methods are in
//  SuperwallKMPBridge.swift, inside the SWBSuperwallBridge class body.
//
//  The configuration envelopes follow SWBSuperwallOptions: mutable properties
//  set from Kotlin after `init()`, NSNumber for optional numbers and Bools
//  (nil = SuperwallKit's default), and plain Int raw values for enums.
//

import Foundation
import SuperwallKit

// MARK: - Configuration (input direction: Kotlin → SuperwallKit)

/// Raw values of SWBCustomerCenterPath.type.
@objc(SWBCustomerCenterPathType)
public enum SWBCustomerCenterPathType: Int {
  case restore = 0
  case manageSubscription = 1
  case refund = 2
  case changePlan = 3
  case contactSupport = 4
  case url = 5
  case custom = 6
}

/// Mirror of CustomerCenterConfiguration.FeedbackSurvey.Option.
@objc(SWBCustomerCenterSurveyOption)
public final class SWBCustomerCenterSurveyOption: NSObject {
  @objc public var id: String = ""
  @objc public var title: String?

  @objc public override init() {
    super.init()
  }

  var native: CustomerCenterConfiguration.FeedbackSurvey.Option {
    return .init(id: id, title: title)
  }
}

/// Mirror of CustomerCenterConfiguration.FeedbackSurvey.
@objc(SWBCustomerCenterSurvey)
public final class SWBCustomerCenterSurvey: NSObject {
  @objc public var id: String = ""
  @objc public var title: String?
  @objc public var options: [SWBCustomerCenterSurveyOption] = []

  @objc public override init() {
    super.init()
  }

  var native: CustomerCenterConfiguration.FeedbackSurvey {
    return .init(id: id, title: title, options: options.map(\.native))
  }
}

/// Mirror of CustomerCenterConfiguration.Path with its PathType flattened:
/// `typeRawValue` is an SWBCustomerCenterPathType raw value, and only the
/// fields of that type are read.
@objc(SWBCustomerCenterPath)
public final class SWBCustomerCenterPath: NSObject {
  @objc public var typeRawValue: Int = SWBCustomerCenterPathType.restore.rawValue
  /// nil = SuperwallKit's default id for the type.
  @objc public var id: String?
  @objc public var title: String?
  @objc public var survey: SWBCustomerCenterSurvey?
  /// `.refund`: Int64 NSNumber, milliseconds after purchase; nil = no limit.
  @objc public var refundWindowMillis: NSNumber?
  /// `.changePlan`: the plans to offer; nil = every plan.
  @objc public var changePlanProductIds: [String]?
  /// `.url`: the URL string.
  @objc public var url: String?
  /// `.url`: true opens it in whichever app handles it, false in-app.
  @objc public var opensExternally: Bool = false
  /// `.custom`: the identifier reported back on tap.
  @objc public var customIdentifier: String?

  @objc public override init() {
    super.init()
  }

  /// nil when the type is unknown or a `.url` path's URL doesn't parse; the
  /// path is then left out rather than crashing.
  var native: CustomerCenterConfiguration.Path? {
    guard let type = nativeType else { return nil }
    return CustomerCenterConfiguration.Path(id: id, type: type, title: title, survey: survey?.native)
  }

  private var nativeType: CustomerCenterConfiguration.PathType? {
    switch SWBCustomerCenterPathType(rawValue: typeRawValue) {
    case .restore:
      return .restore
    case .manageSubscription:
      return .manageSubscription
    case .refund:
      // SuperwallKit takes the window in seconds.
      return .refund(window: refundWindowMillis.map { $0.doubleValue / 1000 })
    case .changePlan:
      return .changePlan(productIds: changePlanProductIds)
    case .contactSupport:
      return .contactSupport
    case .url:
      guard let string = url, let parsed = URL(string: string) else { return nil }
      return .url(parsed, openMethod: opensExternally ? .external : .inApp)
    case .custom:
      return .custom(identifier: customIdentifier ?? "")
    case nil:
      return nil
    }
  }
}

/// Mirror of CustomerCenterConfiguration.Screen.
@objc(SWBCustomerCenterScreen)
public final class SWBCustomerCenterScreen: NSObject {
  @objc public var title: String?
  @objc public var subtitle: String?
  @objc public var paths: [SWBCustomerCenterPath] = []

  @objc public override init() {
    super.init()
  }

  var native: CustomerCenterConfiguration.Screen {
    return .init(title: title, subtitle: subtitle, paths: paths.compactMap(\.native))
  }
}

/// Mirror of CustomerCenterConfiguration, with Support and Appearance
/// flattened into it. Bool NSNumbers: nil = SuperwallKit's default (true).
@objc(SWBCustomerCenterConfiguration)
public final class SWBCustomerCenterConfiguration: NSObject {
  @objc public var managementScreen = SWBCustomerCenterScreen()
  @objc public var noPurchasesScreen = SWBCustomerCenterScreen()
  @objc public var supportEmail: String?
  @objc public var latestAppVersion: String?
  @objc public var warnsAboutUpdates: NSNumber?
  @objc public var checksAppStoreForUpdates: NSNumber?
  /// Unparseable URLs are ignored.
  @objc public var webManagementUrl: String?
  /// Hex strings; the accent is applied only when both are set.
  @objc public var accentLight: String?
  @objc public var accentDark: String?
  @objc public var showsAccountDetails: NSNumber?
  @objc public var warnsAboutDuplicateSubscriptions: NSNumber?

  @objc public override init() {
    super.init()
  }

  var native: CustomerCenterConfiguration {
    let support = CustomerCenterConfiguration.Support(
      email: supportEmail,
      latestAppVersion: latestAppVersion,
      warnsAboutUpdates: warnsAboutUpdates?.boolValue ?? true,
      checksAppStoreForUpdates: checksAppStoreForUpdates?.boolValue ?? true,
      webManagementURL: webManagementUrl.flatMap { URL(string: $0) }
    )
    var accent: CustomerCenterConfiguration.Appearance.ColorPair?
    if let light = accentLight, let dark = accentDark {
      accent = .init(light: light, dark: dark)
    }
    return CustomerCenterConfiguration(
      managementScreen: managementScreen.native,
      noPurchasesScreen: noPurchasesScreen.native,
      support: support,
      appearance: .init(accent: accent),
      showsAccountDetails: showsAccountDetails?.boolValue ?? true,
      warnsAboutDuplicateSubscriptions: warnsAboutDuplicateSubscriptions?.boolValue ?? true
    )
  }
}

// MARK: - Delegate payloads (output direction: SuperwallKit → Kotlin)

/// Raw values of SWBCustomerCenterAction.type (CustomerCenterAction's cases).
@objc(SWBCustomerCenterActionType)
public enum SWBCustomerCenterActionType: Int {
  case restore = 0
  case manageSubscription = 1
  case refund = 2
  case changePlan = 3
  case contactSupport = 4
  case url = 5
  case custom = 6
}

/// Flat copy of SuperwallKit.CustomerCenterAction.
@objc(SWBCustomerCenterAction)
public final class SWBCustomerCenterAction: NSObject {
  /// An SWBCustomerCenterActionType raw value.
  @objc public let typeRawValue: Int
  /// `.url` only.
  @objc public let url: String?
  /// `.custom` only.
  @objc public let customIdentifier: String?

  @objc public init(typeRawValue: Int, url: String?, customIdentifier: String?) {
    self.typeRawValue = typeRawValue
    self.url = url
    self.customIdentifier = customIdentifier
    super.init()
  }

  convenience init(_ native: CustomerCenterAction) {
    switch native {
    case .restore:
      self.init(typeRawValue: SWBCustomerCenterActionType.restore.rawValue, url: nil, customIdentifier: nil)
    case .manageSubscription:
      self.init(typeRawValue: SWBCustomerCenterActionType.manageSubscription.rawValue, url: nil, customIdentifier: nil)
    case .refund:
      self.init(typeRawValue: SWBCustomerCenterActionType.refund.rawValue, url: nil, customIdentifier: nil)
    case .changePlan:
      self.init(typeRawValue: SWBCustomerCenterActionType.changePlan.rawValue, url: nil, customIdentifier: nil)
    case .contactSupport:
      self.init(typeRawValue: SWBCustomerCenterActionType.contactSupport.rawValue, url: nil, customIdentifier: nil)
    case .url(let url):
      self.init(typeRawValue: SWBCustomerCenterActionType.url.rawValue, url: url.absoluteString, customIdentifier: nil)
    case .custom(let identifier):
      self.init(typeRawValue: SWBCustomerCenterActionType.custom.rawValue, url: nil, customIdentifier: identifier)
    }
  }
}

/// Flat copy of SuperwallKit.CustomerCenterPurchase.
@objc(SWBCustomerCenterPurchase)
public final class SWBCustomerCenterPurchase: NSObject {
  @objc public let productId: String?
  @objc public let store: SWBProductStore
  @objc public let entitlements: [SWBEntitlement]
  @objc public let subscription: SWBSubscriptionTransaction?
  @objc public let nonSubscription: SWBNonSubscriptionTransaction?

  @objc public init(
    productId: String?,
    store: SWBProductStore,
    entitlements: [SWBEntitlement],
    subscription: SWBSubscriptionTransaction?,
    nonSubscription: SWBNonSubscriptionTransaction?
  ) {
    self.productId = productId
    self.store = store
    self.entitlements = entitlements
    self.subscription = subscription
    self.nonSubscription = nonSubscription
    super.init()
  }

  convenience init(_ native: CustomerCenterPurchase) {
    self.init(
      productId: native.productId,
      store: SWBProductStore(native.store),
      entitlements: native.entitlements.map { SWBEntitlement($0) },
      subscription: native.subscription.map { SWBSubscriptionTransaction($0) },
      nonSubscription: native.nonSubscription.map { SWBNonSubscriptionTransaction($0) }
    )
  }
}

/// Raw values for refund statuses, matching SuperwallKit.CustomerCenterRefundStatus.
extension CustomerCenterRefundStatus {
  /// 0 = success, 1 = userCancelled, 2 = error.
  var swbRawValue: Int {
    switch self {
    case .success: return 0
    case .userCancelled: return 1
    case .error: return 2
    }
  }
}

// MARK: - Delegate

/// Completion-handler mirror of SuperwallKit.CustomerCenterDelegate, which
/// Kotlin implements. SuperwallKit calls every method on the main actor.
@objc(SWBCustomerCenterDelegate)
public protocol SWBCustomerCenterDelegate: AnyObject {
  /// Invoke `completion` exactly once: true restores, false cancels.
  @objc func customerCenterShouldRestorePurchases(completion: @escaping (Bool) -> Void)
  @objc func customerCenterDidSelectAction(
    _ action: SWBCustomerCenterAction,
    pathId: String,
    purchase: SWBCustomerCenterPurchase?
  )
  @objc func customerCenterDidCompleteSurvey(
    surveyId: String,
    optionId: String,
    action: SWBCustomerCenterAction,
    pathId: String
  )
  /// `statusRawValue`: 0 = success, 1 = userCancelled, 2 = error.
  @objc func customerCenterDidCompleteRefundRequest(productId: String, statusRawValue: Int)
  @objc func customerCenterDidDismiss()
}

/// Internal adapter: SuperwallKit.CustomerCenterDelegate → SWBCustomerCenterDelegate.
/// SuperwallKit retains it while the Customer Center is presented.
@MainActor
final class BridgeCustomerCenterDelegateForwarder: CustomerCenterDelegate {
  private let bridgeDelegate: SWBCustomerCenterDelegate

  init(bridgeDelegate: SWBCustomerCenterDelegate) {
    self.bridgeDelegate = bridgeDelegate
  }

  func customerCenterShouldRestorePurchases() async -> Bool {
    return await withCheckedContinuation { continuation in
      // Only the first completion counts (degrade, never crash).
      let once = OnceGate()
      bridgeDelegate.customerCenterShouldRestorePurchases { proceed in
        guard once.tryPass() else { return }
        continuation.resume(returning: proceed)
      }
    }
  }

  func customerCenterDidSelectAction(
    _ action: CustomerCenterAction,
    pathId: String,
    purchase: CustomerCenterPurchase?
  ) {
    bridgeDelegate.customerCenterDidSelectAction(
      SWBCustomerCenterAction(action),
      pathId: pathId,
      purchase: purchase.map { SWBCustomerCenterPurchase($0) }
    )
  }

  func customerCenterDidCompleteSurvey(
    surveyId: String,
    optionId: String,
    action: CustomerCenterAction,
    pathId: String
  ) {
    bridgeDelegate.customerCenterDidCompleteSurvey(
      surveyId: surveyId,
      optionId: optionId,
      action: SWBCustomerCenterAction(action),
      pathId: pathId
    )
  }

  func customerCenterDidCompleteRefundRequest(productId: String, status: CustomerCenterRefundStatus) {
    bridgeDelegate.customerCenterDidCompleteRefundRequest(productId: productId, statusRawValue: status.swbRawValue)
  }

  func customerCenterDidDismiss() {
    bridgeDelegate.customerCenterDidDismiss()
  }
}
