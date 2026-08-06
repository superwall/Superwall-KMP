//
//  Envelopes.swift
//  SuperwallKMPBridge
//
//  @objc envelope classes for every Swift-only SuperwallKit shape that must cross
// the ObjC boundary into Kotlin/Native. Each enum-with-associated-values
//  becomes a class with a case discriminator (an @objc Int enum) plus optional payload
//  fields; structs become flat field copies.
//
//  Conventions:
//  - Every public symbol is @objc(SWB…)-named and NSObject-based.
//  - Optional primitives cross as NSNumber? (objCType disambiguation happens Kotlin-side).
//  - Dates: epoch-milliseconds NSNumber for the CustomerInfo/Entitlement family,
//    ISO-8601 strings for StoreTransaction/StoreProduct — matching the Pigeon contract
//    conventions that the KMP mappers convert to kotlin.time.Instant.
//  - Untyped payload dictionaries are sanitized via SWBValueSanitizer (port of the
//    Flutter host's mapParamsForDart: unknown values degrade to String(describing:),
//    never crash).
//

import Foundation
import SuperwallKit

// MARK: - Value sanitization (port of SuperwallDelegateHost.mapParamsForDart)

/// Sanitizes arbitrary `[String: Any]` payloads into ObjC-safe plist types.
/// Values are String/Bool/Int/Double/Array/Dictionary; Sets become Arrays;
/// anything else degrades to `String(describing:)` (degrade, never crash).
enum SWBValueSanitizer {
  static func sanitize(_ params: [String: Any]?) -> [String: Any]? {
    guard let params = params else { return nil }
    return params.compactMapValues { sanitizeValue($0) }
  }

  static func sanitizeValue(_ value: Any) -> Any? {
    switch value {
    case let stringValue as String:
      return stringValue
    case let boolValue as Bool:
      return boolValue
    case let intValue as Int:
      return intValue
    case let doubleValue as Double:
      return doubleValue
    case let floatValue as Float:
      return Double(floatValue)
    case let dateValue as Date:
      return dateValue.swbIsoString
    case let urlValue as URL:
      return urlValue.absoluteString
    case let dictValue as [String: Any]:
      return dictValue.compactMapValues { sanitizeValue($0) }
    case let arrayValue as [Any]:
      return arrayValue.compactMap { sanitizeValue($0) }
    case let setValue as Set<AnyHashable>:
      return Array(setValue).compactMap { sanitizeValue($0) }
    default:
      return String(describing: value)
    }
  }
}

// MARK: - Date helpers (same wire conventions as the Flutter host)

extension Date {
  static let swbIsoFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.calendar = Calendar(identifier: .iso8601)
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
    return formatter
  }()

  /// ISO-8601 string (StoreTransaction/StoreProduct date convention).
  var swbIsoString: String {
    return Self.swbIsoFormatter.string(from: self)
  }

  /// Epoch milliseconds (CustomerInfo/Entitlement date convention).
  var swbEpochMs: NSNumber {
    return NSNumber(value: Int64(timeIntervalSince1970 * 1000))
  }
}

// MARK: - Simple enums

/// Mirror of SuperwallKit.ConfigurationStatus.
@objc(SWBConfigurationStatus)
public enum SWBConfigurationStatus: Int {
  case pending = 0
  case configured = 1
  case failed = 2
}

/// Mirror of SuperwallKit.LogLevel.
@objc(SWBLogLevel)
public enum SWBLogLevel: Int {
  case debug = 0
  case info = 1
  case warn = 2
  case error = 3
  case none = 4
}

/// Discriminator for SWBSubscriptionStatus.
@objc(SWBSubscriptionStatusCase)
public enum SWBSubscriptionStatusCase: Int {
  case unknown = 0
  case inactive = 1
  case active = 2
}

/// Mirror of Experiment.Variant.VariantType.
@objc(SWBVariantType)
public enum SWBVariantType: Int {
  case treatment = 0
  case holdout = 1
}

/// Mirror of SuperwallKit.EntitlementType.
@objc(SWBEntitlementType)
public enum SWBEntitlementType: Int {
  case serviceLevel = 0
}

/// Mirror of SuperwallKit.ProductStore (= EntitlementStore).
@objc(SWBProductStore)
public enum SWBProductStore: Int {
  case appStore = 0
  case stripe = 1
  case paddle = 2
  case playStore = 3
  case superwall = 4
  case other = 5
  case custom = 6
}

/// Mirror of LatestSubscription.State.
@objc(SWBLatestSubscriptionState)
public enum SWBLatestSubscriptionState: Int {
  case inGracePeriod = 0
  case subscribed = 1
  case expired = 2
  case inBillingRetryPeriod = 3
  case revoked = 4
}

/// Mirror of LatestSubscription.OfferType.
@objc(SWBLatestSubscriptionOfferType)
public enum SWBLatestSubscriptionOfferType: Int {
  case trial = 0
  case code = 1
  case promotional = 2
  case winback = 3
}

/// Mirror of SuperwallKit.FeatureGatingBehavior.
@objc(SWBFeatureGatingBehavior)
public enum SWBFeatureGatingBehavior: Int {
  case gated = 0
  case nonGated = 1
}

/// Mirror of SuperwallKit.PaywallCloseReason.
@objc(SWBPaywallCloseReason)
public enum SWBPaywallCloseReason: Int {
  case systemLogic = 0
  case forNextPaywall = 1
  case webViewFailedToLoad = 2
  case manualClose = 3
  case none = 4
}

/// Mirror of SuperwallKit.LocalNotificationType.
@objc(SWBLocalNotificationType)
public enum SWBLocalNotificationType: Int {
  case trialStarted = 0
}

/// Mirror of SuperwallKit.ComputedPropertyRequestType.
@objc(SWBComputedPropertyRequestType)
public enum SWBComputedPropertyRequestType: Int {
  case minutesSince = 0
  case hoursSince = 1
  case daysSince = 2
  case monthsSince = 3
  case yearsSince = 4
  case placementsInHour = 5
  case placementsInDay = 6
  case placementsInWeek = 7
  case placementsInMonth = 8
  case placementsSinceInstall = 9
}

/// Mirror of SuperwallKit.SurveyShowCondition.
@objc(SWBSurveyShowCondition)
public enum SWBSurveyShowCondition: Int {
  case onManualClose = 0
  case onPurchase = 1
}

/// Discriminator for SWBTriggerResult.
@objc(SWBTriggerResultCase)
public enum SWBTriggerResultCase: Int {
  case placementNotFound = 0
  case noAudienceMatch = 1
  case paywall = 2
  case holdout = 3
  case error = 4
}

/// Discriminator for SWBPresentationResult.
@objc(SWBPresentationResultCase)
public enum SWBPresentationResultCase: Int {
  case placementNotFound = 0
  case noAudienceMatch = 1
  case paywall = 2
  case holdout = 3
  case paywallNotAvailable = 4
}

/// Discriminator for SWBPaywallResult.
@objc(SWBPaywallResultCase)
public enum SWBPaywallResultCase: Int {
  case purchased = 0
  case declined = 1
  case restored = 2
}

/// Discriminator for SWBPaywallSkippedReason.
@objc(SWBPaywallSkippedReasonCase)
public enum SWBPaywallSkippedReasonCase: Int {
  case holdout = 0
  case noAudienceMatch = 1
  case placementNotFound = 2
}

/// Discriminator for SWBRestoreType.
@objc(SWBRestoreTypeCase)
public enum SWBRestoreTypeCase: Int {
  case viaPurchase = 0
  case viaRestore = 1
}

/// Discriminator for SWBPurchaseResult (Kotlin PurchaseController → bridge direction).
@objc(SWBPurchaseResultCase)
public enum SWBPurchaseResultCase: Int {
  case purchased = 0
  case pending = 1
  case cancelled = 2
  case failed = 3
}

/// Discriminator for SWBRestorationResult.
@objc(SWBRestorationResultCase)
public enum SWBRestorationResultCase: Int {
  case restored = 0
  case failed = 1
}

/// Mirror of SuperwallKit.TransactionType (payload of transactionComplete —
/// the field the Flutter host dropped; carried here).
@objc(SWBTransactionType)
public enum SWBTransactionType: Int {
  case nonRecurringProductPurchase = 0
  case freeTrialStart = 1
  case subscriptionStart = 2
}

/// Discriminator for SWBRedemptionResult.
@objc(SWBRedemptionResultCase)
public enum SWBRedemptionResultCase: Int {
  case success = 0
  case error = 1
  case expiredCode = 2
  case invalidCode = 3
  case expiredSubscription = 4
}

/// Discriminator for SWBOwnership.
@objc(SWBOwnershipCase)
public enum SWBOwnershipCase: Int {
  case appUser = 0
  case device = 1
}

/// Discriminator for SWBStoreIdentifiers.
@objc(SWBStoreIdentifiersCase)
public enum SWBStoreIdentifiersCase: Int {
  case stripe = 0
  case paddle = 1
  case unknown = 2
}

/// Mirror of SuperwallKit.PaywallPresentationRequestStatus.
@objc(SWBPaywallPresentationRequestStatus)
public enum SWBPaywallPresentationRequestStatus: Int {
  case presentation = 0
  case noPresentation = 1
  case timeout = 2
}

/// Discriminator for SWBPaywallPresentationRequestStatusReason.
@objc(SWBPaywallPresentationRequestStatusReasonCase)
public enum SWBPaywallPresentationRequestStatusReasonCase: Int {
  case debuggerPresented = 0
  case paywallAlreadyPresented = 1
  case holdout = 2
  case noAudienceMatch = 3
  case placementNotFound = 4
  case noPaywallViewController = 5
  case noPresenter = 6
  case noConfig = 7
  case subscriptionStatusTimeout = 8
}

/// Mirror of SuperwallKit.IntegrationAttribute (23 cases in SuperwallKit 4.16.1).
@objc(SWBIntegrationAttribute)
public enum SWBIntegrationAttribute: Int {
  case adjustId = 0
  case amplitudeDeviceId = 1
  case amplitudeUserId = 2
  case appsflyerId = 3
  case brazeAliasName = 4
  case brazeAliasLabel = 5
  case onesignalId = 6
  case fbAnonId = 7
  case firebaseAppInstanceId = 8
  case firebaseInstallationId = 9
  case iterableUserId = 10
  case iterableCampaignId = 11
  case iterableTemplateId = 12
  case mixpanelDistinctId = 13
  case mparticleId = 14
  case clevertapId = 15
  case airshipChannelId = 16
  case kochavaDeviceId = 17
  case tenjinId = 18
  case posthogUserId = 19
  case customerioId = 20
  case appstackId = 21
  case singularDeviceId = 22

  /// The SuperwallKit counterpart.
  var native: IntegrationAttribute {
    switch self {
    case .adjustId: return .adjustId
    case .amplitudeDeviceId: return .amplitudeDeviceId
    case .amplitudeUserId: return .amplitudeUserId
    case .appsflyerId: return .appsflyerId
    case .brazeAliasName: return .brazeAliasName
    case .brazeAliasLabel: return .brazeAliasLabel
    case .onesignalId: return .onesignalId
    case .fbAnonId: return .fbAnonId
    case .firebaseAppInstanceId: return .firebaseAppInstanceId
    case .firebaseInstallationId: return .firebaseInstallationId
    case .iterableUserId: return .iterableUserId
    case .iterableCampaignId: return .iterableCampaignId
    case .iterableTemplateId: return .iterableTemplateId
    case .mixpanelDistinctId: return .mixpanelDistinctId
    case .mparticleId: return .mparticleId
    case .clevertapId: return .clevertapId
    case .airshipChannelId: return .airshipChannelId
    case .kochavaDeviceId: return .kochavaDeviceId
    case .tenjinId: return .tenjinId
    case .posthogUserId: return .posthogUserId
    case .customerioId: return .customerioId
    case .appstackId: return .appstackId
    case .singularDeviceId: return .singularDeviceId
    }
  }
}

/// Interface style override. `automatic` clears the override (native `setInterfaceStyle(to: nil)`).
@objc(SWBInterfaceStyle)
public enum SWBInterfaceStyle: Int {
  case automatic = 0
  case light = 1
  case dark = 2
}

/// Mirror of SuperwallKit.CustomCallbackResultStatus.
@objc(SWBCustomCallbackResultStatus)
public enum SWBCustomCallbackResultStatus: Int {
  case success = 0
  case failure = 1
}

// MARK: - Experiment / Variant / Assignment

/// Flat copy of Experiment.Variant.
@objc(SWBVariant)
public final class SWBVariant: NSObject {
  /// The variant id.
  @objc public let id: String
  /// Whether the variant is a treatment or holdout.
  @objc public let type: SWBVariantType
  /// The id of the paywall attached to the variant, if any.
  @objc public let paywallId: String?

  @objc public init(id: String, type: SWBVariantType, paywallId: String?) {
    self.id = id
    self.type = type
    self.paywallId = paywallId
    super.init()
  }

  convenience init(_ native: Experiment.Variant) {
    self.init(
      id: native.id,
      type: native.type == .treatment ? .treatment : .holdout,
      paywallId: native.paywallId
    )
  }
}

/// Flat copy of SuperwallKit.Experiment — carries its variant
/// (the payload the Flutter public layer faked).
@objc(SWBExperiment)
public final class SWBExperiment: NSObject {
  /// The experiment id.
  @objc public let id: String
  /// The campaign (trigger) group id.
  @objc public let groupId: String
  /// The assigned variant.
  @objc public let variant: SWBVariant

  @objc public init(id: String, groupId: String, variant: SWBVariant) {
    self.id = id
    self.groupId = groupId
    self.variant = variant
    super.init()
  }

  convenience init(_ native: Experiment) {
    self.init(id: native.id, groupId: native.groupId, variant: SWBVariant(native.variant))
  }
}

/// Flat copy of SuperwallKit.Assignment (aka ConfirmedAssignment).
@objc(SWBConfirmedAssignment)
public final class SWBConfirmedAssignment: NSObject {
  /// The experiment id.
  @objc public let experimentId: String
  /// The assigned variant.
  @objc public let variant: SWBVariant

  @objc public init(experimentId: String, variant: SWBVariant) {
    self.experimentId = experimentId
    self.variant = variant
    super.init()
  }

  convenience init(_ native: Assignment) {
    self.init(experimentId: native.experimentId, variant: SWBVariant(native.variant))
  }
}

// MARK: - Entitlements / subscription status

/// Flat copy of SuperwallKit.Entitlement (all 13 contract fields).
/// Dates are epoch-milliseconds NSNumbers.
@objc(SWBEntitlement)
public final class SWBEntitlement: NSObject {
  /// The entitlement identifier.
  @objc public let id: String
  /// The entitlement type.
  @objc public let type: SWBEntitlementType
  /// Whether the entitlement is currently active.
  @objc public let isActive: Bool
  /// Product identifiers granting this entitlement.
  @objc public let productIds: [String]
  /// The most recent product id that unlocked this entitlement.
  @objc public let latestProductId: String?
  /// Store of the latest product; NSNumber wrapping SWBProductStore.rawValue, nil when unknown.
  @objc public let store: NSNumber?
  /// Epoch-ms start date.
  @objc public let startsAt: NSNumber?
  /// Epoch-ms latest renewal date.
  @objc public let renewedAt: NSNumber?
  /// Epoch-ms expiry date.
  @objc public let expiresAt: NSNumber?
  /// Bool NSNumber; whether the entitlement is lifetime.
  @objc public let isLifetime: NSNumber?
  /// Bool NSNumber; whether the latest subscription will renew.
  @objc public let willRenew: NSNumber?
  /// NSNumber wrapping SWBLatestSubscriptionState.rawValue, nil when unknown.
  @objc public let state: NSNumber?
  /// NSNumber wrapping SWBLatestSubscriptionOfferType.rawValue, nil when none.
  @objc public let offerType: NSNumber?

  @objc public init(
    id: String,
    type: SWBEntitlementType,
    isActive: Bool,
    productIds: [String],
    latestProductId: String?,
    store: NSNumber?,
    startsAt: NSNumber?,
    renewedAt: NSNumber?,
    expiresAt: NSNumber?,
    isLifetime: NSNumber?,
    willRenew: NSNumber?,
    state: NSNumber?,
    offerType: NSNumber?
  ) {
    self.id = id
    self.type = type
    self.isActive = isActive
    self.productIds = productIds
    self.latestProductId = latestProductId
    self.store = store
    self.startsAt = startsAt
    self.renewedAt = renewedAt
    self.expiresAt = expiresAt
    self.isLifetime = isLifetime
    self.willRenew = willRenew
    self.state = state
    self.offerType = offerType
    super.init()
  }

  convenience init(_ native: Entitlement) {
    self.init(
      id: native.id,
      type: .serviceLevel,
      isActive: native.isActive,
      productIds: Array(native.productIds),
      latestProductId: native.latestProductId,
      store: native.store.map { NSNumber(value: SWBProductStore($0).rawValue) },
      startsAt: native.startsAt?.swbEpochMs,
      renewedAt: native.renewedAt?.swbEpochMs,
      expiresAt: native.expiresAt?.swbEpochMs,
      isLifetime: native.isLifetime.map { NSNumber(value: $0) },
      willRenew: native.willRenew.map { NSNumber(value: $0) },
      state: native.state.map { NSNumber(value: SWBLatestSubscriptionState($0).rawValue) },
      offerType: native.offerType.map { NSNumber(value: SWBLatestSubscriptionOfferType($0).rawValue) }
    )
  }
}

extension SWBProductStore {
  init(_ native: ProductStore) {
    switch native {
    case .appStore: self = .appStore
    case .stripe: self = .stripe
    case .paddle: self = .paddle
    case .playStore: self = .playStore
    case .superwall: self = .superwall
    case .other: self = .other
    case .custom: self = .custom
    @unknown default: self = .other
    }
  }
}

extension SWBLatestSubscriptionState {
  init(_ native: LatestSubscription.State) {
    switch native {
    case .inGracePeriod: self = .inGracePeriod
    case .subscribed: self = .subscribed
    case .expired: self = .expired
    case .inBillingRetryPeriod: self = .inBillingRetryPeriod
    case .revoked: self = .revoked
    }
  }
}

extension SWBLatestSubscriptionOfferType {
  init(_ native: LatestSubscription.OfferType) {
    switch native {
    case .trial: self = .trial
    case .code: self = .code
    case .promotional: self = .promotional
    case .winback: self = .winback
    }
  }
}

/// Snapshot of Superwall.shared.entitlements (EntitlementsInfo).
@objc(SWBEntitlements)
public final class SWBEntitlements: NSObject {
  /// Active entitlements.
  @objc public let active: [SWBEntitlement]
  /// Inactive entitlements.
  @objc public let inactive: [SWBEntitlement]
  /// All entitlements.
  @objc public let all: [SWBEntitlement]
  /// Entitlements unlocked via web checkout.
  @objc public let web: [SWBEntitlement]

  @objc public init(
    active: [SWBEntitlement],
    inactive: [SWBEntitlement],
    all: [SWBEntitlement],
    web: [SWBEntitlement]
  ) {
    self.active = active
    self.inactive = inactive
    self.all = all
    self.web = web
    super.init()
  }

  convenience init(_ native: EntitlementsInfo) {
    self.init(
      active: native.active.map { SWBEntitlement($0) },
      inactive: native.inactive.map { SWBEntitlement($0) },
      all: native.all.map { SWBEntitlement($0) },
      web: native.web.map { SWBEntitlement($0) }
    )
  }
}

/// Envelope for the SubscriptionStatus enum: case + entitlements payload
/// (non-empty only for `.active`).
@objc(SWBSubscriptionStatus)
public final class SWBSubscriptionStatus: NSObject {
  /// Which case this represents.
  @objc public let status: SWBSubscriptionStatusCase
  /// The active entitlements (`.active` only; empty otherwise).
  @objc public let entitlements: [SWBEntitlement]

  @objc public init(status: SWBSubscriptionStatusCase, entitlements: [SWBEntitlement]) {
    self.status = status
    self.entitlements = entitlements
    super.init()
  }

  convenience init(_ native: SubscriptionStatus) {
    switch native {
    case .active(let entitlements):
      self.init(status: .active, entitlements: entitlements.map { SWBEntitlement($0) })
    case .inactive:
      self.init(status: .inactive, entitlements: [])
    case .unknown:
      self.init(status: .unknown, entitlements: [])
    }
  }

  /// Back-conversion for setSubscriptionStatus. Only entitlement ids survive the
  /// round-trip (matching the native `Entitlement(id:)` initializer the Flutter host uses).
  var native: SubscriptionStatus {
    switch status {
    case .active:
      return .active(Set(entitlements.map { Entitlement(id: $0.id) }))
    case .inactive:
      return .inactive
    case .unknown:
      return .unknown
    }
  }
}

// MARK: - CustomerInfo family (epoch-ms dates)

/// Flat copy of SuperwallKit.SubscriptionTransaction.
@objc(SWBSubscriptionTransaction)
public final class SWBSubscriptionTransaction: NSObject {
  @objc public let transactionId: String
  @objc public let productId: String
  /// Epoch-ms purchase date.
  @objc public let purchaseDate: Int64
  @objc public let willRenew: Bool
  @objc public let isRevoked: Bool
  @objc public let isInGracePeriod: Bool
  @objc public let isInBillingRetryPeriod: Bool
  @objc public let isActive: Bool
  /// Epoch-ms expiration date.
  @objc public let expirationDate: NSNumber?
  /// NSNumber wrapping SWBLatestSubscriptionOfferType.rawValue.
  @objc public let offerType: NSNumber?
  @objc public let subscriptionGroupId: String?
  @objc public let store: SWBProductStore

  @objc public init(
    transactionId: String,
    productId: String,
    purchaseDate: Int64,
    willRenew: Bool,
    isRevoked: Bool,
    isInGracePeriod: Bool,
    isInBillingRetryPeriod: Bool,
    isActive: Bool,
    expirationDate: NSNumber?,
    offerType: NSNumber?,
    subscriptionGroupId: String?,
    store: SWBProductStore
  ) {
    self.transactionId = transactionId
    self.productId = productId
    self.purchaseDate = purchaseDate
    self.willRenew = willRenew
    self.isRevoked = isRevoked
    self.isInGracePeriod = isInGracePeriod
    self.isInBillingRetryPeriod = isInBillingRetryPeriod
    self.isActive = isActive
    self.expirationDate = expirationDate
    self.offerType = offerType
    self.subscriptionGroupId = subscriptionGroupId
    self.store = store
    super.init()
  }

  convenience init(_ native: SubscriptionTransaction) {
    self.init(
      transactionId: native.transactionId,
      productId: native.productId,
      purchaseDate: Int64(native.purchaseDate.timeIntervalSince1970 * 1000),
      willRenew: native.willRenew,
      isRevoked: native.isRevoked,
      isInGracePeriod: native.isInGracePeriod,
      isInBillingRetryPeriod: native.isInBillingRetryPeriod,
      isActive: native.isActive,
      expirationDate: native.expirationDate?.swbEpochMs,
      offerType: native.offerType.map { NSNumber(value: SWBLatestSubscriptionOfferType($0).rawValue) },
      subscriptionGroupId: native.subscriptionGroupId,
      store: SWBProductStore(native.store)
    )
  }
}

/// Flat copy of SuperwallKit.NonSubscriptionTransaction.
@objc(SWBNonSubscriptionTransaction)
public final class SWBNonSubscriptionTransaction: NSObject {
  @objc public let transactionId: String
  @objc public let productId: String
  /// Epoch-ms purchase date.
  @objc public let purchaseDate: Int64
  @objc public let isConsumable: Bool
  @objc public let isRevoked: Bool
  @objc public let store: SWBProductStore

  @objc public init(
    transactionId: String,
    productId: String,
    purchaseDate: Int64,
    isConsumable: Bool,
    isRevoked: Bool,
    store: SWBProductStore
  ) {
    self.transactionId = transactionId
    self.productId = productId
    self.purchaseDate = purchaseDate
    self.isConsumable = isConsumable
    self.isRevoked = isRevoked
    self.store = store
    super.init()
  }

  convenience init(_ native: NonSubscriptionTransaction) {
    self.init(
      transactionId: native.transactionId,
      productId: native.productId,
      purchaseDate: Int64(native.purchaseDate.timeIntervalSince1970 * 1000),
      isConsumable: native.isConsumable,
      isRevoked: native.isRevoked,
      store: SWBProductStore(native.store)
    )
  }
}

/// Flat copy of SuperwallKit.CustomerInfo.
@objc(SWBCustomerInfo)
public final class SWBCustomerInfo: NSObject {
  @objc public let subscriptions: [SWBSubscriptionTransaction]
  @objc public let nonSubscriptions: [SWBNonSubscriptionTransaction]
  @objc public let entitlements: [SWBEntitlement]
  @objc public let userId: String

  @objc public init(
    subscriptions: [SWBSubscriptionTransaction],
    nonSubscriptions: [SWBNonSubscriptionTransaction],
    entitlements: [SWBEntitlement],
    userId: String
  ) {
    self.subscriptions = subscriptions
    self.nonSubscriptions = nonSubscriptions
    self.entitlements = entitlements
    self.userId = userId
    super.init()
  }

  convenience init(_ native: CustomerInfo) {
    self.init(
      subscriptions: native.subscriptions.map { SWBSubscriptionTransaction($0) },
      nonSubscriptions: native.nonSubscriptions.map { SWBNonSubscriptionTransaction($0) },
      entitlements: native.entitlements.map { SWBEntitlement($0) },
      userId: native.userId
    )
  }
}

// MARK: - Paywall info family

/// Flat copy of SuperwallKit.Product (paywall product: id + name + entitlements).
@objc(SWBProduct)
public final class SWBProduct: NSObject {
  /// The product identifier.
  @objc public let id: String
  /// The product's reference name on the paywall, if any.
  @objc public let name: String?
  /// Entitlements attached to the product.
  @objc public let entitlements: [SWBEntitlement]

  @objc public init(id: String, name: String?, entitlements: [SWBEntitlement]) {
    self.id = id
    self.name = name
    self.entitlements = entitlements
    super.init()
  }

  convenience init(_ native: Product) {
    self.init(
      id: native.id,
      name: native.name,
      entitlements: native.entitlements.map { SWBEntitlement($0) }
    )
  }
}

/// Flat copy of SuperwallKit.LocalNotification.
/// Note: `id` is required here (the Flutter host hardcoded ""; fixed —
/// SuperwallKit has no notification id, so the bridge synthesizes a stable one from content).
@objc(SWBLocalNotification)
public final class SWBLocalNotification: NSObject {
  @objc public let id: String
  @objc public let type: SWBLocalNotificationType
  @objc public let title: String
  @objc public let subtitle: String?
  @objc public let body: String
  /// Delay in milliseconds.
  @objc public let delay: Int64

  @objc public init(
    id: String,
    type: SWBLocalNotificationType,
    title: String,
    subtitle: String?,
    body: String,
    delay: Int64
  ) {
    self.id = id
    self.type = type
    self.title = title
    self.subtitle = subtitle
    self.body = body
    self.delay = delay
    super.init()
  }

  convenience init(_ native: LocalNotification) {
    let type: SWBLocalNotificationType
    switch native.type {
    case .trialStarted:
      type = .trialStarted
    }
    self.init(
      id: "trialStarted-\(native.title.hashValue)-\(Int64(native.delay))",
      type: type,
      title: native.title,
      subtitle: native.subtitle,
      body: native.body,
      delay: Int64(native.delay)
    )
  }
}

/// Flat copy of SuperwallKit.ComputedPropertyRequest.
@objc(SWBComputedPropertyRequest)
public final class SWBComputedPropertyRequest: NSObject {
  @objc public let type: SWBComputedPropertyRequestType
  /// The placement name the computed property is based on
  /// (Pigeon calls this `eventName`).
  @objc public let placementName: String

  @objc public init(type: SWBComputedPropertyRequestType, placementName: String) {
    self.type = type
    self.placementName = placementName
    super.init()
  }

  convenience init(_ native: ComputedPropertyRequest) {
    let type: SWBComputedPropertyRequestType
    switch native.type {
    case .minutesSince: type = .minutesSince
    case .hoursSince: type = .hoursSince
    case .daysSince: type = .daysSince
    case .monthsSince: type = .monthsSince
    case .yearsSince: type = .yearsSince
    case .placementsInHour: type = .placementsInHour
    case .placementsInDay: type = .placementsInDay
    case .placementsInWeek: type = .placementsInWeek
    case .placementsInMonth: type = .placementsInMonth
    case .placementsSinceInstall: type = .placementsSinceInstall
    }
    self.init(type: type, placementName: native.placementName)
  }
}

/// Flat copy of SuperwallKit.SurveyOption.
@objc(SWBSurveyOption)
public final class SWBSurveyOption: NSObject {
  @objc public let id: String
  /// The option's display text (native `title`; Pigeon calls it `text`).
  @objc public let text: String

  @objc public init(id: String, text: String) {
    self.id = id
    self.text = text
    super.init()
  }

  convenience init(_ native: SurveyOption) {
    self.init(id: native.id, text: native.title)
  }
}

/// Flat copy of SuperwallKit.Survey.
@objc(SWBSurvey)
public final class SWBSurvey: NSObject {
  @objc public let id: String
  @objc public let assignmentKey: String
  @objc public let title: String
  @objc public let message: String
  @objc public let options: [SWBSurveyOption]
  @objc public let presentationCondition: SWBSurveyShowCondition
  @objc public let presentationProbability: Double
  @objc public let includeOtherOption: Bool
  @objc public let includeCloseOption: Bool

  @objc public init(
    id: String,
    assignmentKey: String,
    title: String,
    message: String,
    options: [SWBSurveyOption],
    presentationCondition: SWBSurveyShowCondition,
    presentationProbability: Double,
    includeOtherOption: Bool,
    includeCloseOption: Bool
  ) {
    self.id = id
    self.assignmentKey = assignmentKey
    self.title = title
    self.message = message
    self.options = options
    self.presentationCondition = presentationCondition
    self.presentationProbability = presentationProbability
    self.includeOtherOption = includeOtherOption
    self.includeCloseOption = includeCloseOption
    super.init()
  }

  convenience init(_ native: Survey) {
    let condition: SWBSurveyShowCondition
    switch native.presentationCondition {
    case .onManualClose: condition = .onManualClose
    case .onPurchase: condition = .onPurchase
    @unknown default: condition = .onManualClose
    }
    self.init(
      id: native.id,
      assignmentKey: native.assignmentKey,
      title: native.title,
      message: native.message,
      options: native.options.map { SWBSurveyOption($0) },
      presentationCondition: condition,
      presentationProbability: native.presentationProbability,
      includeOtherOption: native.includeOtherOption,
      includeCloseOption: native.includeCloseOption
    )
  }
}

/// Flat copy of SuperwallKit.PaywallInfo (the Pigeon PPaywallInfo field set).
@objc(SWBPaywallInfo)
public final class SWBPaywallInfo: NSObject {
  @objc public let identifier: String
  @objc public let name: String
  @objc public let experiment: SWBExperiment?
  @objc public let productIds: [String]
  @objc public let products: [SWBProduct]
  @objc public let url: String
  @objc public let presentedByPlacementWithName: String?
  @objc public let presentedByPlacementWithId: String?
  @objc public let presentedByPlacementAt: String?
  @objc public let presentedBy: String
  @objc public let presentationSourceType: String?
  @objc public let responseLoadStartTime: String?
  @objc public let responseLoadCompleteTime: String?
  @objc public let responseLoadFailTime: String?
  /// Double NSNumber (seconds).
  @objc public let responseLoadDuration: NSNumber?
  @objc public let webViewLoadStartTime: String?
  @objc public let webViewLoadCompleteTime: String?
  @objc public let webViewLoadFailTime: String?
  /// Double NSNumber (seconds).
  @objc public let webViewLoadDuration: NSNumber?
  @objc public let productsLoadStartTime: String?
  @objc public let productsLoadCompleteTime: String?
  @objc public let productsLoadFailTime: String?
  /// Double NSNumber (seconds).
  @objc public let productsLoadDuration: NSNumber?
  @objc public let paywalljsVersion: String?
  @objc public let isFreeTrialAvailable: Bool
  @objc public let featureGatingBehavior: SWBFeatureGatingBehavior
  @objc public let closeReason: SWBPaywallCloseReason
  @objc public let localNotifications: [SWBLocalNotification]
  @objc public let computedPropertyRequests: [SWBComputedPropertyRequest]
  @objc public let surveys: [SWBSurvey]
  /// Sanitized paywall state dictionary; nil when empty.
  @objc public let state: [String: Any]?

  @objc public init(
    identifier: String,
    name: String,
    experiment: SWBExperiment?,
    productIds: [String],
    products: [SWBProduct],
    url: String,
    presentedByPlacementWithName: String?,
    presentedByPlacementWithId: String?,
    presentedByPlacementAt: String?,
    presentedBy: String,
    presentationSourceType: String?,
    responseLoadStartTime: String?,
    responseLoadCompleteTime: String?,
    responseLoadFailTime: String?,
    responseLoadDuration: NSNumber?,
    webViewLoadStartTime: String?,
    webViewLoadCompleteTime: String?,
    webViewLoadFailTime: String?,
    webViewLoadDuration: NSNumber?,
    productsLoadStartTime: String?,
    productsLoadCompleteTime: String?,
    productsLoadFailTime: String?,
    productsLoadDuration: NSNumber?,
    paywalljsVersion: String?,
    isFreeTrialAvailable: Bool,
    featureGatingBehavior: SWBFeatureGatingBehavior,
    closeReason: SWBPaywallCloseReason,
    localNotifications: [SWBLocalNotification],
    computedPropertyRequests: [SWBComputedPropertyRequest],
    surveys: [SWBSurvey],
    state: [String: Any]?
  ) {
    self.identifier = identifier
    self.name = name
    self.experiment = experiment
    self.productIds = productIds
    self.products = products
    self.url = url
    self.presentedByPlacementWithName = presentedByPlacementWithName
    self.presentedByPlacementWithId = presentedByPlacementWithId
    self.presentedByPlacementAt = presentedByPlacementAt
    self.presentedBy = presentedBy
    self.presentationSourceType = presentationSourceType
    self.responseLoadStartTime = responseLoadStartTime
    self.responseLoadCompleteTime = responseLoadCompleteTime
    self.responseLoadFailTime = responseLoadFailTime
    self.responseLoadDuration = responseLoadDuration
    self.webViewLoadStartTime = webViewLoadStartTime
    self.webViewLoadCompleteTime = webViewLoadCompleteTime
    self.webViewLoadFailTime = webViewLoadFailTime
    self.webViewLoadDuration = webViewLoadDuration
    self.productsLoadStartTime = productsLoadStartTime
    self.productsLoadCompleteTime = productsLoadCompleteTime
    self.productsLoadFailTime = productsLoadFailTime
    self.productsLoadDuration = productsLoadDuration
    self.paywalljsVersion = paywalljsVersion
    self.isFreeTrialAvailable = isFreeTrialAvailable
    self.featureGatingBehavior = featureGatingBehavior
    self.closeReason = closeReason
    self.localNotifications = localNotifications
    self.computedPropertyRequests = computedPropertyRequests
    self.surveys = surveys
    self.state = state
    super.init()
  }

  convenience init(_ native: PaywallInfo) {
    let closeReason: SWBPaywallCloseReason
    switch native.closeReason {
    case .systemLogic: closeReason = .systemLogic
    case .forNextPaywall: closeReason = .forNextPaywall
    case .webViewFailedToLoad: closeReason = .webViewFailedToLoad
    case .manualClose: closeReason = .manualClose
    case .none: closeReason = .none
    @unknown default: closeReason = .none
    }
    self.init(
      identifier: native.identifier,
      name: native.name,
      experiment: native.experiment.map { SWBExperiment($0) },
      productIds: native.productIds,
      products: native.products.map { SWBProduct($0) },
      url: native.url.absoluteString,
      presentedByPlacementWithName: native.presentedByPlacementWithName,
      presentedByPlacementWithId: native.presentedByPlacementWithId,
      presentedByPlacementAt: native.presentedByPlacementAt,
      presentedBy: native.presentedBy,
      presentationSourceType: native.presentationSourceType,
      responseLoadStartTime: native.responseLoadStartTime,
      responseLoadCompleteTime: native.responseLoadCompleteTime,
      responseLoadFailTime: native.responseLoadFailTime,
      responseLoadDuration: native.responseLoadDuration.map { NSNumber(value: $0) },
      webViewLoadStartTime: native.webViewLoadStartTime,
      webViewLoadCompleteTime: native.webViewLoadCompleteTime,
      webViewLoadFailTime: native.webViewLoadFailTime,
      webViewLoadDuration: native.webViewLoadDuration.map { NSNumber(value: $0) },
      productsLoadStartTime: native.productsLoadStartTime,
      productsLoadCompleteTime: native.productsLoadCompleteTime,
      productsLoadFailTime: native.productsLoadFailTime,
      productsLoadDuration: native.productsLoadDuration.map { NSNumber(value: $0) },
      paywalljsVersion: native.paywalljsVersion,
      isFreeTrialAvailable: native.isFreeTrialAvailable,
      featureGatingBehavior: native.featureGatingBehavior == .gated ? .gated : .nonGated,
      closeReason: closeReason,
      localNotifications: native.localNotifications.map { SWBLocalNotification($0) },
      computedPropertyRequests: native.computedPropertyRequests.map { SWBComputedPropertyRequest($0) },
      surveys: native.surveys.map { SWBSurvey($0) },
      state: native.state.isEmpty ? nil : SWBValueSanitizer.sanitize(native.state)
    )
  }
}

// MARK: - Store product / transaction (ISO-8601 date strings)

/// Flat copy of SuperwallKit.StoreTransaction.
@objc(SWBStoreTransaction)
public final class SWBStoreTransaction: NSObject {
  @objc public let configRequestId: String
  @objc public let appSessionId: String
  /// ISO-8601 string.
  @objc public let transactionDate: String?
  @objc public let originalTransactionIdentifier: String
  @objc public let storeTransactionId: String?
  /// ISO-8601 string.
  @objc public let originalTransactionDate: String?
  @objc public let webOrderLineItemID: String?
  @objc public let appBundleId: String?
  @objc public let subscriptionGroupId: String?
  /// Bool NSNumber.
  @objc public let isUpgraded: NSNumber?
  /// ISO-8601 string.
  @objc public let expirationDate: String?
  @objc public let offerId: String?
  /// ISO-8601 string.
  @objc public let revocationDate: String?

  @objc public init(
    configRequestId: String,
    appSessionId: String,
    transactionDate: String?,
    originalTransactionIdentifier: String,
    storeTransactionId: String?,
    originalTransactionDate: String?,
    webOrderLineItemID: String?,
    appBundleId: String?,
    subscriptionGroupId: String?,
    isUpgraded: NSNumber?,
    expirationDate: String?,
    offerId: String?,
    revocationDate: String?
  ) {
    self.configRequestId = configRequestId
    self.appSessionId = appSessionId
    self.transactionDate = transactionDate
    self.originalTransactionIdentifier = originalTransactionIdentifier
    self.storeTransactionId = storeTransactionId
    self.originalTransactionDate = originalTransactionDate
    self.webOrderLineItemID = webOrderLineItemID
    self.appBundleId = appBundleId
    self.subscriptionGroupId = subscriptionGroupId
    self.isUpgraded = isUpgraded
    self.expirationDate = expirationDate
    self.offerId = offerId
    self.revocationDate = revocationDate
    super.init()
  }

  convenience init(_ native: StoreTransaction) {
    self.init(
      configRequestId: native.configRequestId,
      appSessionId: native.appSessionId,
      transactionDate: native.transactionDate?.swbIsoString,
      originalTransactionIdentifier: native.originalTransactionIdentifier,
      storeTransactionId: native.storeTransactionId,
      originalTransactionDate: native.originalTransactionDate?.swbIsoString,
      webOrderLineItemID: native.webOrderLineItemID,
      appBundleId: native.appBundleId,
      subscriptionGroupId: native.subscriptionGroupId,
      isUpgraded: native.isUpgraded.map { NSNumber(value: $0) },
      expirationDate: native.expirationDate?.swbIsoString,
      offerId: native.offerId,
      revocationDate: native.revocationDate?.swbIsoString
    )
  }
}

/// Flat copy of SuperwallKit.StoreProduct (the Pigeon PStoreProduct 41-field set).
@objc(SWBStoreProduct)
public final class SWBStoreProduct: NSObject {
  @objc public let entitlements: [SWBEntitlement]
  @objc public let productIdentifier: String
  @objc public let subscriptionGroupIdentifier: String?
  @objc public let attributes: [String: String]
  @objc public let localizedPrice: String
  @objc public let localizedSubscriptionPeriod: String
  @objc public let period: String
  @objc public let periodly: String
  @objc public let periodWeeks: Int64
  @objc public let periodWeeksString: String
  @objc public let periodMonths: Int64
  @objc public let periodMonthsString: String
  @objc public let periodYears: Int64
  @objc public let periodYearsString: String
  @objc public let periodDays: Int64
  @objc public let periodDaysString: String
  @objc public let dailyPrice: String
  @objc public let weeklyPrice: String
  @objc public let monthlyPrice: String
  @objc public let yearlyPrice: String
  @objc public let hasFreeTrial: Bool
  /// ISO-8601 string.
  @objc public let trialPeriodEndDate: String?
  @objc public let trialPeriodEndDateString: String
  @objc public let localizedTrialPeriodPrice: String
  @objc public let trialPeriodPrice: Double
  @objc public let trialPeriodDays: Int64
  @objc public let trialPeriodDaysString: String
  @objc public let trialPeriodWeeks: Int64
  @objc public let trialPeriodWeeksString: String
  @objc public let trialPeriodMonths: Int64
  @objc public let trialPeriodMonthsString: String
  @objc public let trialPeriodYears: Int64
  @objc public let trialPeriodYearsString: String
  @objc public let trialPeriodText: String
  @objc public let locale: String
  @objc public let languageCode: String?
  @objc public let currencySymbol: String?
  @objc public let currencyCode: String?
  @objc public let isFamilyShareable: Bool
  @objc public let regionCode: String?
  @objc public let price: Double

  @objc public init(
    entitlements: [SWBEntitlement],
    productIdentifier: String,
    subscriptionGroupIdentifier: String?,
    attributes: [String: String],
    localizedPrice: String,
    localizedSubscriptionPeriod: String,
    period: String,
    periodly: String,
    periodWeeks: Int64,
    periodWeeksString: String,
    periodMonths: Int64,
    periodMonthsString: String,
    periodYears: Int64,
    periodYearsString: String,
    periodDays: Int64,
    periodDaysString: String,
    dailyPrice: String,
    weeklyPrice: String,
    monthlyPrice: String,
    yearlyPrice: String,
    hasFreeTrial: Bool,
    trialPeriodEndDate: String?,
    trialPeriodEndDateString: String,
    localizedTrialPeriodPrice: String,
    trialPeriodPrice: Double,
    trialPeriodDays: Int64,
    trialPeriodDaysString: String,
    trialPeriodWeeks: Int64,
    trialPeriodWeeksString: String,
    trialPeriodMonths: Int64,
    trialPeriodMonthsString: String,
    trialPeriodYears: Int64,
    trialPeriodYearsString: String,
    trialPeriodText: String,
    locale: String,
    languageCode: String?,
    currencySymbol: String?,
    currencyCode: String?,
    isFamilyShareable: Bool,
    regionCode: String?,
    price: Double
  ) {
    self.entitlements = entitlements
    self.productIdentifier = productIdentifier
    self.subscriptionGroupIdentifier = subscriptionGroupIdentifier
    self.attributes = attributes
    self.localizedPrice = localizedPrice
    self.localizedSubscriptionPeriod = localizedSubscriptionPeriod
    self.period = period
    self.periodly = periodly
    self.periodWeeks = periodWeeks
    self.periodWeeksString = periodWeeksString
    self.periodMonths = periodMonths
    self.periodMonthsString = periodMonthsString
    self.periodYears = periodYears
    self.periodYearsString = periodYearsString
    self.periodDays = periodDays
    self.periodDaysString = periodDaysString
    self.dailyPrice = dailyPrice
    self.weeklyPrice = weeklyPrice
    self.monthlyPrice = monthlyPrice
    self.yearlyPrice = yearlyPrice
    self.hasFreeTrial = hasFreeTrial
    self.trialPeriodEndDate = trialPeriodEndDate
    self.trialPeriodEndDateString = trialPeriodEndDateString
    self.localizedTrialPeriodPrice = localizedTrialPeriodPrice
    self.trialPeriodPrice = trialPeriodPrice
    self.trialPeriodDays = trialPeriodDays
    self.trialPeriodDaysString = trialPeriodDaysString
    self.trialPeriodWeeks = trialPeriodWeeks
    self.trialPeriodWeeksString = trialPeriodWeeksString
    self.trialPeriodMonths = trialPeriodMonths
    self.trialPeriodMonthsString = trialPeriodMonthsString
    self.trialPeriodYears = trialPeriodYears
    self.trialPeriodYearsString = trialPeriodYearsString
    self.trialPeriodText = trialPeriodText
    self.locale = locale
    self.languageCode = languageCode
    self.currencySymbol = currencySymbol
    self.currencyCode = currencyCode
    self.isFamilyShareable = isFamilyShareable
    self.regionCode = regionCode
    self.price = price
    super.init()
  }

  convenience init(_ native: StoreProduct) {
    self.init(
      entitlements: native.entitlements.map { SWBEntitlement($0) },
      productIdentifier: native.productIdentifier,
      subscriptionGroupIdentifier: native.subscriptionGroupIdentifier,
      attributes: native.attributes,
      localizedPrice: native.localizedPrice,
      localizedSubscriptionPeriod: native.localizedSubscriptionPeriod,
      period: native.period,
      periodly: native.periodly,
      periodWeeks: Int64(native.periodWeeks),
      periodWeeksString: native.periodWeeksString,
      periodMonths: Int64(native.periodMonths),
      periodMonthsString: native.periodMonthsString,
      periodYears: Int64(native.periodYears),
      periodYearsString: native.periodYearsString,
      periodDays: Int64(native.periodDays),
      periodDaysString: native.periodDaysString,
      dailyPrice: native.dailyPrice,
      weeklyPrice: native.weeklyPrice,
      monthlyPrice: native.monthlyPrice,
      yearlyPrice: native.yearlyPrice,
      hasFreeTrial: native.hasFreeTrial,
      trialPeriodEndDate: native.trialPeriodEndDate?.swbIsoString,
      trialPeriodEndDateString: native.trialPeriodEndDateString,
      localizedTrialPeriodPrice: native.localizedTrialPeriodPrice,
      trialPeriodPrice: Double(truncating: native.trialPeriodPrice as NSNumber),
      trialPeriodDays: Int64(native.trialPeriodDays),
      trialPeriodDaysString: native.trialPeriodDaysString,
      trialPeriodWeeks: Int64(native.trialPeriodWeeks),
      trialPeriodWeeksString: native.trialPeriodWeeksString,
      trialPeriodMonths: Int64(native.trialPeriodMonths),
      trialPeriodMonthsString: native.trialPeriodMonthsString,
      trialPeriodYears: Int64(native.trialPeriodYears),
      trialPeriodYearsString: native.trialPeriodYearsString,
      trialPeriodText: native.trialPeriodText,
      locale: native.locale,
      languageCode: native.languageCode,
      currencySymbol: native.currencySymbol,
      currencyCode: native.currencyCode,
      isFamilyShareable: native.isFamilyShareable,
      regionCode: native.regionCode,
      price: Double(truncating: native.price as NSNumber)
    )
  }
}

// MARK: - Result envelopes

/// Envelope for SuperwallKit.TriggerResult.
@objc(SWBTriggerResult)
public final class SWBTriggerResult: NSObject {
  /// Which case this represents.
  @objc public let result: SWBTriggerResultCase
  /// The experiment (`.paywall` / `.holdout` only).
  @objc public let experiment: SWBExperiment?
  /// The error description (`.error` only).
  @objc public let error: String?

  @objc public init(result: SWBTriggerResultCase, experiment: SWBExperiment?, error: String?) {
    self.result = result
    self.experiment = experiment
    self.error = error
    super.init()
  }

  convenience init(_ native: TriggerResult) {
    switch native {
    case .placementNotFound:
      self.init(result: .placementNotFound, experiment: nil, error: nil)
    case .noAudienceMatch:
      self.init(result: .noAudienceMatch, experiment: nil, error: nil)
    case .paywall(let experiment):
      self.init(result: .paywall, experiment: SWBExperiment(experiment), error: nil)
    case .holdout(let experiment):
      self.init(result: .holdout, experiment: SWBExperiment(experiment), error: nil)
    case .error(let error):
      self.init(result: .error, experiment: nil, error: error.localizedDescription)
    }
  }
}

/// Envelope for SuperwallKit.PresentationResult.
@objc(SWBPresentationResult)
public final class SWBPresentationResult: NSObject {
  /// Which case this represents.
  @objc public let result: SWBPresentationResultCase
  /// The experiment (`.paywall` / `.holdout` only).
  @objc public let experiment: SWBExperiment?

  @objc public init(result: SWBPresentationResultCase, experiment: SWBExperiment?) {
    self.result = result
    self.experiment = experiment
    super.init()
  }

  convenience init(_ native: PresentationResult) {
    switch native {
    case .placementNotFound:
      self.init(result: .placementNotFound, experiment: nil)
    case .noAudienceMatch:
      self.init(result: .noAudienceMatch, experiment: nil)
    case .paywall(let experiment):
      self.init(result: .paywall, experiment: SWBExperiment(experiment))
    case .holdout(let experiment):
      self.init(result: .holdout, experiment: SWBExperiment(experiment))
    case .paywallNotAvailable:
      self.init(result: .paywallNotAvailable, experiment: nil)
    }
  }
}

/// Envelope for SuperwallKit.PaywallResult.
@objc(SWBPaywallResult)
public final class SWBPaywallResult: NSObject {
  /// Which case this represents.
  @objc public let result: SWBPaywallResultCase
  /// The purchased product id (`.purchased` only).
  @objc public let productId: String?

  @objc public init(result: SWBPaywallResultCase, productId: String?) {
    self.result = result
    self.productId = productId
    super.init()
  }

  convenience init(_ native: PaywallResult) {
    switch native {
    case .purchased(let product):
      self.init(result: .purchased, productId: product.productIdentifier)
    case .declined:
      self.init(result: .declined, productId: nil)
    case .restored:
      self.init(result: .restored, productId: nil)
    }
  }
}

/// Envelope for SuperwallKit.PaywallSkippedReason — keeps the holdout
/// experiment payload the Flutter layer drops.
@objc(SWBPaywallSkippedReason)
public final class SWBPaywallSkippedReason: NSObject {
  /// Which case this represents.
  @objc public let reason: SWBPaywallSkippedReasonCase
  /// The holdout experiment (`.holdout` only).
  @objc public let experiment: SWBExperiment?

  @objc public init(reason: SWBPaywallSkippedReasonCase, experiment: SWBExperiment?) {
    self.reason = reason
    self.experiment = experiment
    super.init()
  }

  convenience init(_ native: PaywallSkippedReason) {
    switch native {
    case .holdout(let experiment):
      self.init(reason: .holdout, experiment: SWBExperiment(experiment))
    case .noAudienceMatch:
      self.init(reason: .noAudienceMatch, experiment: nil)
    case .placementNotFound:
      self.init(reason: .placementNotFound, experiment: nil)
    }
  }
}

/// Envelope for SuperwallKit.RestoreType.
@objc(SWBRestoreType)
public final class SWBRestoreType: NSObject {
  /// Which case this represents.
  @objc public let type: SWBRestoreTypeCase
  /// The transaction (`.viaPurchase` only; may still be nil).
  @objc public let storeTransaction: SWBStoreTransaction?

  @objc public init(type: SWBRestoreTypeCase, storeTransaction: SWBStoreTransaction?) {
    self.type = type
    self.storeTransaction = storeTransaction
    super.init()
  }

  convenience init(_ native: RestoreType) {
    switch native {
    case .viaPurchase(let transaction):
      self.init(type: .viaPurchase, storeTransaction: transaction.map { SWBStoreTransaction($0) })
    case .viaRestore:
      self.init(type: .viaRestore, storeTransaction: nil)
    }
  }
}

/// Envelope for SuperwallKit.RestorationResult (both directions:
/// SDK → Kotlin for restorePurchases; Kotlin → SDK for the purchase controller).
@objc(SWBRestorationResult)
public final class SWBRestorationResult: NSObject {
  /// Which case this represents.
  @objc public let result: SWBRestorationResultCase
  /// The failure description (`.failed` only).
  @objc public let errorMessage: String?

  @objc public init(result: SWBRestorationResultCase, errorMessage: String?) {
    self.result = result
    self.errorMessage = errorMessage
    super.init()
  }

  convenience init(_ native: RestorationResult) {
    switch native {
    case .restored:
      self.init(result: .restored, errorMessage: nil)
    case .failed(let error):
      self.init(result: .failed, errorMessage: error?.localizedDescription)
    }
  }

  /// Back-conversion for the purchase-controller direction.
  var native: RestorationResult {
    switch result {
    case .restored:
      return .restored
    case .failed:
      let error = NSError(
        domain: SWBSuperwallBridge.errorDomain,
        code: 0,
        userInfo: [NSLocalizedDescriptionKey: errorMessage ?? "Unknown restoration error"]
      )
      return .failed(error)
    }
  }
}

/// Envelope for SuperwallKit.PurchaseResult (Kotlin purchase controller → SDK).
@objc(SWBPurchaseResult)
public final class SWBPurchaseResult: NSObject {
  /// Which case this represents.
  @objc public let result: SWBPurchaseResultCase
  /// The failure description (`.failed` only).
  @objc public let errorMessage: String?

  @objc public init(result: SWBPurchaseResultCase, errorMessage: String?) {
    self.result = result
    self.errorMessage = errorMessage
    super.init()
  }

  /// Back-conversion for the purchase-controller direction.
  var native: PurchaseResult {
    switch result {
    case .purchased:
      return .purchased
    case .pending:
      return .pending
    case .cancelled:
      return .cancelled
    case .failed:
      let error = NSError(
        domain: SWBSuperwallBridge.errorDomain,
        code: 0,
        userInfo: [NSLocalizedDescriptionKey: errorMessage ?? "Unknown purchase error"]
      )
      return .failed(error)
    }
  }
}

/// Envelope for SuperwallKit.PaywallPresentationRequestStatusReason.
@objc(SWBPaywallPresentationRequestStatusReason)
public final class SWBPaywallPresentationRequestStatusReason: NSObject {
  /// Which case this represents.
  @objc public let reason: SWBPaywallPresentationRequestStatusReasonCase
  /// The holdout experiment (`.holdout` only).
  @objc public let experiment: SWBExperiment?

  @objc public init(
    reason: SWBPaywallPresentationRequestStatusReasonCase,
    experiment: SWBExperiment?
  ) {
    self.reason = reason
    self.experiment = experiment
    super.init()
  }

  convenience init(_ native: PaywallPresentationRequestStatusReason) {
    switch native {
    case .debuggerPresented:
      self.init(reason: .debuggerPresented, experiment: nil)
    case .paywallAlreadyPresented:
      self.init(reason: .paywallAlreadyPresented, experiment: nil)
    case .holdout(let experiment):
      self.init(reason: .holdout, experiment: SWBExperiment(experiment))
    case .noAudienceMatch:
      self.init(reason: .noAudienceMatch, experiment: nil)
    case .placementNotFound:
      self.init(reason: .placementNotFound, experiment: nil)
    case .noPaywallViewController:
      self.init(reason: .noPaywallViewController, experiment: nil)
    case .noPresenter:
      self.init(reason: .noPresenter, experiment: nil)
    case .noConfig:
      self.init(reason: .noConfig, experiment: nil)
    case .subscriptionStatusTimeout:
      self.init(reason: .subscriptionStatusTimeout, experiment: nil)
    }
  }
}

// MARK: - Redemption family

/// Flat copy of RedemptionResult.ErrorInfo.
@objc(SWBErrorInfo)
public final class SWBErrorInfo: NSObject {
  @objc public let message: String

  @objc public init(message: String) {
    self.message = message
    super.init()
  }
}

/// Flat copy of RedemptionResult.ExpiredCodeInfo.
@objc(SWBExpiredCodeInfo)
public final class SWBExpiredCodeInfo: NSObject {
  @objc public let resent: Bool
  @objc public let obfuscatedEmail: String?

  @objc public init(resent: Bool, obfuscatedEmail: String?) {
    self.resent = resent
    self.obfuscatedEmail = obfuscatedEmail
    super.init()
  }
}

/// Envelope for RedemptionResult.RedemptionInfo.Ownership.
@objc(SWBOwnership)
public final class SWBOwnership: NSObject {
  /// Which case this represents.
  @objc public let ownership: SWBOwnershipCase
  /// The app user id (`.appUser` only).
  @objc public let appUserId: String?
  /// The device id (`.device` only).
  @objc public let deviceId: String?

  @objc public init(ownership: SWBOwnershipCase, appUserId: String?, deviceId: String?) {
    self.ownership = ownership
    self.appUserId = appUserId
    self.deviceId = deviceId
    super.init()
  }
}

/// Envelope for PurchaserInfo.StoreIdentifiers.
@objc(SWBStoreIdentifiers)
public final class SWBStoreIdentifiers: NSObject {
  /// Which case this represents.
  @objc public let store: SWBStoreIdentifiersCase
  /// Customer id (`.stripe` / `.paddle`).
  @objc public let customerId: String?
  /// Subscription ids (`.stripe` / `.paddle`).
  @objc public let subscriptionIds: [String]
  /// Raw store name (`.unknown` only).
  @objc public let unknownStore: String?
  /// Sanitized additional info (`.unknown` only).
  @objc public let additionalInfo: [String: Any]?

  @objc public init(
    store: SWBStoreIdentifiersCase,
    customerId: String?,
    subscriptionIds: [String],
    unknownStore: String?,
    additionalInfo: [String: Any]?
  ) {
    self.store = store
    self.customerId = customerId
    self.subscriptionIds = subscriptionIds
    self.unknownStore = unknownStore
    self.additionalInfo = additionalInfo
    super.init()
  }
}

/// Flat copy of RedemptionResult.RedemptionInfo.PurchaserInfo.
@objc(SWBPurchaserInfo)
public final class SWBPurchaserInfo: NSObject {
  @objc public let appUserId: String
  @objc public let email: String?
  @objc public let storeIdentifiers: SWBStoreIdentifiers

  @objc public init(appUserId: String, email: String?, storeIdentifiers: SWBStoreIdentifiers) {
    self.appUserId = appUserId
    self.email = email
    self.storeIdentifiers = storeIdentifiers
    super.init()
  }
}

/// Flat copy of the redemption-scoped PaywallInfo.
@objc(SWBRedemptionPaywallInfo)
public final class SWBRedemptionPaywallInfo: NSObject {
  @objc public let identifier: String
  @objc public let placementName: String
  /// Sanitized placement params.
  @objc public let placementParams: [String: Any]
  @objc public let variantId: String
  @objc public let experimentId: String

  @objc public init(
    identifier: String,
    placementName: String,
    placementParams: [String: Any],
    variantId: String,
    experimentId: String
  ) {
    self.identifier = identifier
    self.placementName = placementName
    self.placementParams = placementParams
    self.variantId = variantId
    self.experimentId = experimentId
    super.init()
  }
}

/// Flat copy of RedemptionResult.RedemptionInfo.
@objc(SWBRedemptionInfo)
public final class SWBRedemptionInfo: NSObject {
  @objc public let ownership: SWBOwnership
  @objc public let purchaserInfo: SWBPurchaserInfo
  @objc public let paywallInfo: SWBRedemptionPaywallInfo?
  @objc public let entitlements: [SWBEntitlement]

  @objc public init(
    ownership: SWBOwnership,
    purchaserInfo: SWBPurchaserInfo,
    paywallInfo: SWBRedemptionPaywallInfo?,
    entitlements: [SWBEntitlement]
  ) {
    self.ownership = ownership
    self.purchaserInfo = purchaserInfo
    self.paywallInfo = paywallInfo
    self.entitlements = entitlements
    super.init()
  }

  convenience init(_ native: RedemptionResult.RedemptionInfo) {
    let ownership: SWBOwnership
    switch native.ownership {
    case .appUser(let appUserId):
      ownership = SWBOwnership(ownership: .appUser, appUserId: appUserId, deviceId: nil)
    case .device(let deviceId):
      ownership = SWBOwnership(ownership: .device, appUserId: nil, deviceId: deviceId)
    }

    let storeIdentifiers: SWBStoreIdentifiers
    switch native.purchaserInfo.storeIdentifiers {
    case let .stripe(customerId, subscriptionIds):
      storeIdentifiers = SWBStoreIdentifiers(
        store: .stripe,
        customerId: customerId,
        subscriptionIds: subscriptionIds,
        unknownStore: nil,
        additionalInfo: nil
      )
    case let .paddle(customerId, subscriptionIds):
      storeIdentifiers = SWBStoreIdentifiers(
        store: .paddle,
        customerId: customerId,
        subscriptionIds: subscriptionIds,
        unknownStore: nil,
        additionalInfo: nil
      )
    case let .unknown(store, additionalInfo):
      storeIdentifiers = SWBStoreIdentifiers(
        store: .unknown,
        customerId: nil,
        subscriptionIds: [],
        unknownStore: store,
        additionalInfo: SWBValueSanitizer.sanitize(additionalInfo)
      )
    }

    let purchaserInfo = SWBPurchaserInfo(
      appUserId: native.purchaserInfo.appUserId,
      email: native.purchaserInfo.email,
      storeIdentifiers: storeIdentifiers
    )

    let paywallInfo = native.paywallInfo.map {
      SWBRedemptionPaywallInfo(
        identifier: $0.identifier,
        placementName: $0.placementName,
        placementParams: SWBValueSanitizer.sanitize($0.placementParams) ?? [:],
        variantId: $0.variantId,
        experimentId: $0.experimentId
      )
    }

    self.init(
      ownership: ownership,
      purchaserInfo: purchaserInfo,
      paywallInfo: paywallInfo,
      entitlements: native.entitlements.map { SWBEntitlement($0) }
    )
  }
}

/// Envelope for SuperwallKit.RedemptionResult.
@objc(SWBRedemptionResult)
public final class SWBRedemptionResult: NSObject {
  /// Which case this represents.
  @objc public let result: SWBRedemptionResultCase
  /// The redeemed code (every case).
  @objc public let code: String
  /// Redemption info (`.success` / `.expiredSubscription`).
  @objc public let redemptionInfo: SWBRedemptionInfo?
  /// Error info (`.error` only).
  @objc public let errorInfo: SWBErrorInfo?
  /// Expired code info (`.expiredCode` only).
  @objc public let expiredCodeInfo: SWBExpiredCodeInfo?

  @objc public init(
    result: SWBRedemptionResultCase,
    code: String,
    redemptionInfo: SWBRedemptionInfo?,
    errorInfo: SWBErrorInfo?,
    expiredCodeInfo: SWBExpiredCodeInfo?
  ) {
    self.result = result
    self.code = code
    self.redemptionInfo = redemptionInfo
    self.errorInfo = errorInfo
    self.expiredCodeInfo = expiredCodeInfo
    super.init()
  }

  convenience init(_ native: RedemptionResult) {
    switch native {
    case let .success(code, redemptionInfo):
      self.init(
        result: .success,
        code: code,
        redemptionInfo: SWBRedemptionInfo(redemptionInfo),
        errorInfo: nil,
        expiredCodeInfo: nil
      )
    case let .error(code, error):
      self.init(
        result: .error,
        code: code,
        redemptionInfo: nil,
        errorInfo: SWBErrorInfo(message: error.message),
        expiredCodeInfo: nil
      )
    case let .expiredCode(code, info):
      self.init(
        result: .expiredCode,
        code: code,
        redemptionInfo: nil,
        errorInfo: nil,
        expiredCodeInfo: SWBExpiredCodeInfo(resent: info.resent, obfuscatedEmail: info.obfuscatedEmail)
      )
    case let .invalidCode(code):
      self.init(result: .invalidCode, code: code, redemptionInfo: nil, errorInfo: nil, expiredCodeInfo: nil)
    case let .expiredSubscription(code, redemptionInfo):
      self.init(
        result: .expiredSubscription,
        code: code,
        redemptionInfo: SWBRedemptionInfo(redemptionInfo),
        errorInfo: nil,
        expiredCodeInfo: nil
      )
    }
  }
}

// MARK: - Custom callbacks

/// Flat copy of SuperwallKit.CustomCallback.
@objc(SWBCustomCallback)
public final class SWBCustomCallback: NSObject {
  /// The callback name.
  @objc public let name: String
  /// Sanitized callback variables.
  @objc public let variables: [String: Any]?

  @objc public init(name: String, variables: [String: Any]?) {
    self.name = name
    self.variables = variables
    super.init()
  }

  convenience init(_ native: CustomCallback) {
    self.init(name: native.name, variables: SWBValueSanitizer.sanitize(native.variables))
  }
}

/// Flat copy of SuperwallKit.CustomCallbackResult (Kotlin → SDK direction).
@objc(SWBCustomCallbackResult)
public final class SWBCustomCallbackResult: NSObject {
  /// Success or failure.
  @objc public let status: SWBCustomCallbackResultStatus
  /// Optional result payload.
  @objc public let data: [String: Any]?

  @objc public init(status: SWBCustomCallbackResultStatus, data: [String: Any]?) {
    self.status = status
    self.data = data
    super.init()
  }

  var native: CustomCallbackResult {
    return CustomCallbackResult(
      status: status == .success ? .success : .failure,
      data: data
    )
  }
}

// MARK: - Attribution / page view (new in SuperwallKit 4.16.x)

/// Flat copy of SuperwallKit.AttributionMatchInfo.
@objc(SWBAttributionMatchInfo)
public final class SWBAttributionMatchInfo: NSObject {
  /// Provider raw value ("mmp" or "apple_search_ads").
  @objc public let provider: String
  @objc public let matched: Bool
  @objc public let source: String?
  /// Confidence raw value ("high"/"medium"/"low"), nil when absent.
  @objc public let confidence: String?
  /// Double NSNumber.
  @objc public let matchScore: NSNumber?
  @objc public let reason: String?

  @objc public init(
    provider: String,
    matched: Bool,
    source: String?,
    confidence: String?,
    matchScore: NSNumber?,
    reason: String?
  ) {
    self.provider = provider
    self.matched = matched
    self.source = source
    self.confidence = confidence
    self.matchScore = matchScore
    self.reason = reason
    super.init()
  }

  convenience init(_ native: AttributionMatchInfo) {
    self.init(
      provider: native.provider.rawValue,
      matched: native.matched,
      source: native.source,
      confidence: native.confidence?.rawValue,
      matchScore: native.matchScore.map { NSNumber(value: $0) },
      reason: native.reason
    )
  }
}

/// Flat copy of SuperwallKit.PageViewData.
@objc(SWBPageViewData)
public final class SWBPageViewData: NSObject {
  @objc public let pageNodeId: String
  @objc public let flowPosition: Int
  @objc public let pageName: String
  @objc public let navigationNodeId: String
  @objc public let previousPageNodeId: String?
  /// Int NSNumber.
  @objc public let previousFlowPosition: NSNumber?
  /// "entry", "forward", "back", or "auto_transition".
  @objc public let navigationType: String
  /// Int NSNumber (milliseconds).
  @objc public let timeOnPreviousPageMs: NSNumber?

  @objc public init(
    pageNodeId: String,
    flowPosition: Int,
    pageName: String,
    navigationNodeId: String,
    previousPageNodeId: String?,
    previousFlowPosition: NSNumber?,
    navigationType: String,
    timeOnPreviousPageMs: NSNumber?
  ) {
    self.pageNodeId = pageNodeId
    self.flowPosition = flowPosition
    self.pageName = pageName
    self.navigationNodeId = navigationNodeId
    self.previousPageNodeId = previousPageNodeId
    self.previousFlowPosition = previousFlowPosition
    self.navigationType = navigationType
    self.timeOnPreviousPageMs = timeOnPreviousPageMs
    super.init()
  }

  convenience init(_ native: PageViewData) {
    self.init(
      pageNodeId: native.pageNodeId,
      flowPosition: native.flowPosition,
      pageName: native.pageName,
      navigationNodeId: native.navigationNodeId,
      previousPageNodeId: native.previousPageNodeId,
      previousFlowPosition: native.previousFlowPosition.map { NSNumber(value: $0) },
      navigationType: native.navigationType,
      timeOnPreviousPageMs: native.timeOnPreviousPageMs.map { NSNumber(value: $0) }
    )
  }
}
