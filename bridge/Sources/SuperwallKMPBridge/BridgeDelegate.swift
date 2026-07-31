//
//  BridgeDelegate.swift
//  SuperwallKMPBridge
//
//  The @objc delegate protocol Kotlin implements, the typed SuperwallEvent envelope,
//  and the internal SuperwallDelegate that subscribes to Superwall.shared and forwards.
//
//  The event switch is a faithful port of the Flutter host's
//  SuperwallDelegateHost.handleSuperwallEvent (~70 cases in 4.14; 80 in SuperwallKit
//  4.16.2), with the Flutter mislabels FIXED per plan §3.4:
//  - transactionRestore is its own event type (Flutter mislabeled it transactionComplete)
//  - transactionComplete carries transaction, product AND type (Flutter dropped type)
//  - nonRecurringProductPurchase carries the TransactionProduct id (Flutter dropped it)
//
//  Threading: SuperwallKit invokes its delegate methods on the main actor; the bridge
//  forwards SYNCHRONOUSLY on that same queue (main). Dispatching onward (e.g. into a
//  Kotlin Dispatchers.Main context) is the Kotlin side's job (plan §6).
//

import Foundation
import SuperwallKit

// MARK: - Event type

/// One value per SuperwallKit.SuperwallEvent case (SuperwallKit 4.16.1: 80 cases),
/// plus `.unknown` for forward compatibility with future SDK cases
/// (degrade-never-crash; the raw case description lands in `SWBEventEnvelope.name`).
@objc(SWBEventType)
public enum SWBEventType: Int {
  case firstSeen = 0
  case appOpen = 1
  case appLaunch = 2
  case identityAlias = 3
  case appInstall = 4
  case sessionStart = 5
  case deviceAttributes = 6
  case subscriptionStatusDidChange = 7
  case appClose = 8
  case deepLink = 9
  case triggerFire = 10
  case paywallOpen = 11
  case paywallClose = 12
  case paywallDecline = 13
  case transactionStart = 14
  case transactionFail = 15
  case transactionAbandon = 16
  case transactionComplete = 17
  case subscriptionStart = 18
  case freeTrialStart = 19
  case transactionRestore = 20
  case transactionTimeout = 21
  case userAttributes = 22
  case attributionMatch = 23
  case nonRecurringProductPurchase = 24
  case paywallResponseLoadStart = 25
  case paywallResponseLoadNotFound = 26
  case paywallResponseLoadFail = 27
  case paywallResponseLoadComplete = 28
  case paywallWebviewLoadStart = 29
  case paywallWebviewLoadFail = 30
  case paywallWebviewLoadComplete = 31
  case paywallWebviewLoadTimeout = 32
  case paywallWebviewLoadFallback = 33
  case paywallWebviewProcessTerminated = 34
  case paywallProductsLoadStart = 35
  case paywallProductsLoadFail = 36
  case paywallProductsLoadComplete = 37
  case paywallProductsLoadRetry = 38
  case paywallProductsLoadMissingProducts = 39
  case surveyResponse = 40
  case paywallPresentationRequest = 41
  case touchesBegan = 42
  case surveyClose = 43
  case reset = 44
  case restoreStart = 45
  case restoreFail = 46
  case restoreComplete = 47
  case configRefresh = 48
  case customPlacement = 49
  case configAttributes = 50
  case confirmAllAssignments = 51
  case configFail = 52
  case adServicesTokenRequestStart = 53
  case adServicesTokenRequestFail = 54
  case adServicesTokenRequestComplete = 55
  case shimmerViewStart = 56
  case shimmerViewComplete = 57
  case redemptionStart = 58
  case redemptionComplete = 59
  case redemptionFail = 60
  case enrichmentStart = 61
  case enrichmentComplete = 62
  case enrichmentFail = 63
  case networkDecodingFail = 64
  case customerInfoDidChange = 65
  case integrationAttributes = 66
  case reviewRequested = 67
  case permissionRequested = 68
  case permissionGranted = 69
  case permissionDenied = 70
  case paywallPreloadStart = 71
  case paywallPreloadComplete = 72
  case stripeCheckoutStart = 73
  case stripeCheckoutSubmit = 74
  case stripeCheckoutComplete = 75
  case stripeCheckoutFail = 76
  case testModeModalOpen = 77
  case testModeModalClose = 78
  case paywallPageView = 79
  /// A SuperwallEvent case this bridge build does not know (forward compat).
  case unknown = 999
}

// MARK: - Event envelope

/// Typed, flat envelope for one SuperwallEventInfo. Every associated value of the
/// Swift event enum is destructured into an optional field here; which fields are
/// non-nil depends on `eventType` (same field-per-case matrix as the Pigeon
/// PSuperwallEventInfo, plus the fields Flutter dropped).
@objc(SWBEventEnvelope)
public final class SWBEventEnvelope: NSObject {
  /// Which SuperwallEvent case this envelope carries.
  @objc public let eventType: SWBEventType
  /// The sanitized analytics params of the event (SuperwallEventInfo.params).
  @objc public let params: [String: Any]?
  /// paywallOpen/Close/Decline, transaction*, subscriptionStart, freeTrialStart,
  /// paywall*Load*, surveyResponse, customPlacement, stripeCheckout*, paywallPageView.
  @objc public let paywallInfo: SWBPaywallInfo?
  /// transactionComplete (may be nil when a PurchaseController hid the transaction).
  @objc public let transaction: SWBStoreTransaction?
  /// transactionStart/Abandon/Complete, subscriptionStart, freeTrialStart.
  @objc public let product: SWBStoreProduct?
  /// transactionComplete only — NSNumber wrapping SWBTransactionType.rawValue
  /// (the field the Flutter host dropped).
  @objc public let transactionType: NSNumber?
  /// nonRecurringProductPurchase only — the TransactionProduct id
  /// (the field the Flutter host dropped).
  @objc public let transactionProductId: String?
  /// transactionRestore only.
  @objc public let restoreType: SWBRestoreType?
  /// triggerFire only.
  @objc public let triggerResult: SWBTriggerResult?
  /// triggerFire only.
  @objc public let placementName: String?
  /// paywallResponseLoad*, paywallProductsLoad* — the triggering placement.
  @objc public let triggeredPlacementName: String?
  /// deepLink only.
  @objc public let deepLinkUrl: String?
  /// deviceAttributes only (sanitized).
  @objc public let deviceAttributes: [String: Any]?
  /// userAttributes only (sanitized).
  @objc public let userAttributes: [String: Any]?
  /// paywallProductsLoadRetry only — Int NSNumber.
  @objc public let attempt: NSNumber?
  /// paywallProductsLoadMissingProducts only.
  @objc public let missingProductIdentifiers: [String]?
  /// surveyResponse only.
  @objc public let survey: SWBSurvey?
  /// surveyResponse only.
  @objc public let selectedOption: SWBSurveyOption?
  /// surveyResponse only.
  @objc public let customResponse: String?
  /// paywallPresentationRequest only.
  @objc public let presentationRequestStatus: NSNumber?
  /// paywallPresentationRequest only.
  @objc public let presentationRequestReason: SWBPaywallPresentationRequestStatusReason?
  /// transactionFail, adServicesTokenRequestFail — localized error description.
  @objc public let error: String?
  /// restoreFail only.
  @objc public let message: String?
  /// adServicesTokenRequestComplete only.
  @objc public let token: String?
  /// customPlacement (the placement name); also carries the raw case description
  /// for `.unknown`.
  @objc public let name: String?
  /// enrichmentComplete only (sanitized).
  @objc public let userEnrichment: [String: Any]?
  /// enrichmentComplete only (sanitized).
  @objc public let deviceEnrichment: [String: Any]?
  /// integrationAttributes only (sanitized).
  @objc public let integrationAttributes: [String: Any]?
  /// reviewRequested only — Int NSNumber.
  @objc public let reviewRequestedCount: NSNumber?
  /// permissionRequested/Granted/Denied only.
  @objc public let permissionName: String?
  /// permissionRequested/Granted/Denied only.
  @objc public let paywallIdentifier: String?
  /// paywallPreloadStart/Complete only — Int NSNumber.
  @objc public let paywallCount: NSNumber?
  /// attributionMatch only.
  @objc public let attributionMatch: SWBAttributionMatchInfo?
  /// paywallPageView only.
  @objc public let pageViewData: SWBPageViewData?

  @objc public init(
    eventType: SWBEventType,
    params: [String: Any]?,
    paywallInfo: SWBPaywallInfo?,
    transaction: SWBStoreTransaction?,
    product: SWBStoreProduct?,
    transactionType: NSNumber?,
    transactionProductId: String?,
    restoreType: SWBRestoreType?,
    triggerResult: SWBTriggerResult?,
    placementName: String?,
    triggeredPlacementName: String?,
    deepLinkUrl: String?,
    deviceAttributes: [String: Any]?,
    userAttributes: [String: Any]?,
    attempt: NSNumber?,
    missingProductIdentifiers: [String]?,
    survey: SWBSurvey?,
    selectedOption: SWBSurveyOption?,
    customResponse: String?,
    presentationRequestStatus: NSNumber?,
    presentationRequestReason: SWBPaywallPresentationRequestStatusReason?,
    error: String?,
    message: String?,
    token: String?,
    name: String?,
    userEnrichment: [String: Any]?,
    deviceEnrichment: [String: Any]?,
    integrationAttributes: [String: Any]?,
    reviewRequestedCount: NSNumber?,
    permissionName: String?,
    paywallIdentifier: String?,
    paywallCount: NSNumber?,
    attributionMatch: SWBAttributionMatchInfo?,
    pageViewData: SWBPageViewData?
  ) {
    self.eventType = eventType
    self.params = params
    self.paywallInfo = paywallInfo
    self.transaction = transaction
    self.product = product
    self.transactionType = transactionType
    self.transactionProductId = transactionProductId
    self.restoreType = restoreType
    self.triggerResult = triggerResult
    self.placementName = placementName
    self.triggeredPlacementName = triggeredPlacementName
    self.deepLinkUrl = deepLinkUrl
    self.deviceAttributes = deviceAttributes
    self.userAttributes = userAttributes
    self.attempt = attempt
    self.missingProductIdentifiers = missingProductIdentifiers
    self.survey = survey
    self.selectedOption = selectedOption
    self.customResponse = customResponse
    self.presentationRequestStatus = presentationRequestStatus
    self.presentationRequestReason = presentationRequestReason
    self.error = error
    self.message = message
    self.token = token
    self.name = name
    self.userEnrichment = userEnrichment
    self.deviceEnrichment = deviceEnrichment
    self.integrationAttributes = integrationAttributes
    self.reviewRequestedCount = reviewRequestedCount
    self.permissionName = permissionName
    self.paywallIdentifier = paywallIdentifier
    self.paywallCount = paywallCount
    self.attributionMatch = attributionMatch
    self.pageViewData = pageViewData
    super.init()
  }

  /// Swift-side sparse builder — all fields default nil except the event type.
  convenience init(
    _ eventType: SWBEventType,
    params: [String: Any]? = nil,
    paywallInfo: SWBPaywallInfo? = nil,
    transaction: SWBStoreTransaction? = nil,
    product: SWBStoreProduct? = nil,
    transactionType: NSNumber? = nil,
    transactionProductId: String? = nil,
    restoreType: SWBRestoreType? = nil,
    triggerResult: SWBTriggerResult? = nil,
    placementName: String? = nil,
    triggeredPlacementName: String? = nil,
    deepLinkUrl: String? = nil,
    deviceAttributes: [String: Any]? = nil,
    userAttributes: [String: Any]? = nil,
    attempt: NSNumber? = nil,
    missingProductIdentifiers: [String]? = nil,
    survey: SWBSurvey? = nil,
    selectedOption: SWBSurveyOption? = nil,
    customResponse: String? = nil,
    presentationRequestStatus: NSNumber? = nil,
    presentationRequestReason: SWBPaywallPresentationRequestStatusReason? = nil,
    error: String? = nil,
    message: String? = nil,
    token: String? = nil,
    name: String? = nil,
    userEnrichment: [String: Any]? = nil,
    deviceEnrichment: [String: Any]? = nil,
    integrationAttributes: [String: Any]? = nil,
    reviewRequestedCount: NSNumber? = nil,
    permissionName: String? = nil,
    paywallIdentifier: String? = nil,
    paywallCount: NSNumber? = nil,
    attributionMatch: SWBAttributionMatchInfo? = nil,
    pageViewData: SWBPageViewData? = nil
  ) {
    self.init(
      eventType: eventType,
      params: params,
      paywallInfo: paywallInfo,
      transaction: transaction,
      product: product,
      transactionType: transactionType,
      transactionProductId: transactionProductId,
      restoreType: restoreType,
      triggerResult: triggerResult,
      placementName: placementName,
      triggeredPlacementName: triggeredPlacementName,
      deepLinkUrl: deepLinkUrl,
      deviceAttributes: deviceAttributes,
      userAttributes: userAttributes,
      attempt: attempt,
      missingProductIdentifiers: missingProductIdentifiers,
      survey: survey,
      selectedOption: selectedOption,
      customResponse: customResponse,
      presentationRequestStatus: presentationRequestStatus,
      presentationRequestReason: presentationRequestReason,
      error: error,
      message: message,
      token: token,
      name: name,
      userEnrichment: userEnrichment,
      deviceEnrichment: deviceEnrichment,
      integrationAttributes: integrationAttributes,
      reviewRequestedCount: reviewRequestedCount,
      permissionName: permissionName,
      paywallIdentifier: paywallIdentifier,
      paywallCount: paywallCount,
      attributionMatch: attributionMatch,
      pageViewData: pageViewData
    )
  }

  /// Destructures a SuperwallEventInfo into a typed envelope.
  /// This is the port of SuperwallDelegateHost.swift's ~70-case switch.
  convenience init(_ info: SuperwallEventInfo) {
    let params = SWBValueSanitizer.sanitize(info.params)

    switch info.event {
    case .firstSeen:
      self.init(.firstSeen, params: params)
    case .appOpen:
      self.init(.appOpen, params: params)
    case .appLaunch:
      self.init(.appLaunch, params: params)
    case .identityAlias:
      self.init(.identityAlias, params: params)
    case .appInstall:
      self.init(.appInstall, params: params)
    case .sessionStart:
      self.init(.sessionStart, params: params)
    case .deviceAttributes(let attributes):
      self.init(.deviceAttributes, params: params, deviceAttributes: SWBValueSanitizer.sanitize(attributes))
    case .subscriptionStatusDidChange:
      self.init(.subscriptionStatusDidChange, params: params)
    case .appClose:
      self.init(.appClose, params: params)
    case .deepLink(let url):
      self.init(.deepLink, params: params, deepLinkUrl: url.absoluteString)
    case .triggerFire(let placementName, let result):
      self.init(
        .triggerFire,
        params: params,
        triggerResult: SWBTriggerResult(result),
        placementName: placementName
      )
    case .paywallOpen(let paywallInfo):
      self.init(.paywallOpen, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .paywallClose(let paywallInfo):
      self.init(.paywallClose, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .paywallDecline(let paywallInfo):
      self.init(.paywallDecline, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .transactionStart(let product, let paywallInfo):
      self.init(
        .transactionStart,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        product: SWBStoreProduct(product)
      )
    case .transactionFail(let error, let paywallInfo):
      self.init(
        .transactionFail,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        error: error.localizedDescription
      )
    case .transactionAbandon(let product, let paywallInfo):
      self.init(
        .transactionAbandon,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        product: SWBStoreProduct(product)
      )
    case let .transactionComplete(transaction, product, type, paywallInfo):
      // Fidelity fix vs Flutter: carries `type` and keeps its own label.
      let swbType: SWBTransactionType
      switch type {
      case .nonRecurringProductPurchase: swbType = .nonRecurringProductPurchase
      case .freeTrialStart: swbType = .freeTrialStart
      case .subscriptionStart: swbType = .subscriptionStart
      }
      self.init(
        .transactionComplete,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        transaction: transaction.map { SWBStoreTransaction($0) },
        product: SWBStoreProduct(product),
        transactionType: NSNumber(value: swbType.rawValue)
      )
    case .subscriptionStart(let product, let paywallInfo):
      self.init(
        .subscriptionStart,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        product: SWBStoreProduct(product)
      )
    case .freeTrialStart(let product, let paywallInfo):
      self.init(
        .freeTrialStart,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        product: SWBStoreProduct(product)
      )
    case .transactionRestore(let restoreType, let paywallInfo):
      // Fidelity fix vs Flutter: NOT mislabeled as transactionComplete.
      self.init(
        .transactionRestore,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        restoreType: SWBRestoreType(restoreType)
      )
    case .transactionTimeout(let paywallInfo):
      self.init(.transactionTimeout, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .userAttributes(let attributes):
      self.init(.userAttributes, params: params, userAttributes: SWBValueSanitizer.sanitize(attributes))
    case .attributionMatch(let info):
      self.init(.attributionMatch, params: params, attributionMatch: SWBAttributionMatchInfo(info))
    case .nonRecurringProductPurchase(let product, let paywallInfo):
      // Fidelity fix vs Flutter: carries the TransactionProduct id.
      self.init(
        .nonRecurringProductPurchase,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        transactionProductId: product.id
      )
    case .paywallResponseLoadStart(let triggeredPlacementName):
      self.init(.paywallResponseLoadStart, params: params, triggeredPlacementName: triggeredPlacementName)
    case .paywallResponseLoadNotFound(let triggeredPlacementName):
      self.init(.paywallResponseLoadNotFound, params: params, triggeredPlacementName: triggeredPlacementName)
    case .paywallResponseLoadFail(let triggeredPlacementName):
      self.init(.paywallResponseLoadFail, params: params, triggeredPlacementName: triggeredPlacementName)
    case .paywallResponseLoadComplete(let triggeredPlacementName, let paywallInfo):
      self.init(
        .paywallResponseLoadComplete,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        triggeredPlacementName: triggeredPlacementName
      )
    case .paywallWebviewLoadStart(let paywallInfo):
      self.init(.paywallWebviewLoadStart, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .paywallWebviewLoadFail(let paywallInfo):
      self.init(.paywallWebviewLoadFail, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .paywallWebviewLoadComplete(let paywallInfo):
      self.init(.paywallWebviewLoadComplete, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .paywallWebviewLoadTimeout(let paywallInfo):
      self.init(.paywallWebviewLoadTimeout, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .paywallWebviewLoadFallback(let paywallInfo):
      self.init(.paywallWebviewLoadFallback, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .paywallWebviewProcessTerminated(let paywallInfo):
      self.init(.paywallWebviewProcessTerminated, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case let .paywallProductsLoadStart(triggeredPlacementName, paywallInfo):
      self.init(
        .paywallProductsLoadStart,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        triggeredPlacementName: triggeredPlacementName
      )
    case let .paywallProductsLoadFail(triggeredPlacementName, paywallInfo):
      self.init(
        .paywallProductsLoadFail,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        triggeredPlacementName: triggeredPlacementName
      )
    case .paywallProductsLoadComplete(let triggeredPlacementName):
      self.init(.paywallProductsLoadComplete, params: params, triggeredPlacementName: triggeredPlacementName)
    case let .paywallProductsLoadRetry(triggeredPlacementName, paywallInfo, attempt):
      self.init(
        .paywallProductsLoadRetry,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        triggeredPlacementName: triggeredPlacementName,
        attempt: NSNumber(value: attempt)
      )
    case let .paywallProductsLoadMissingProducts(triggeredPlacementName, paywallInfo, identifiers):
      self.init(
        .paywallProductsLoadMissingProducts,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        triggeredPlacementName: triggeredPlacementName,
        missingProductIdentifiers: Array(identifiers)
      )
    case let .surveyResponse(survey, selectedOption, customResponse, paywallInfo):
      self.init(
        .surveyResponse,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        survey: SWBSurvey(survey),
        selectedOption: SWBSurveyOption(selectedOption),
        customResponse: customResponse
      )
    case .paywallPresentationRequest(let status, let reason):
      let swbStatus: SWBPaywallPresentationRequestStatus
      switch status {
      case .presentation: swbStatus = .presentation
      case .noPresentation: swbStatus = .noPresentation
      case .timeout: swbStatus = .timeout
      }
      self.init(
        .paywallPresentationRequest,
        params: params,
        presentationRequestStatus: NSNumber(value: swbStatus.rawValue),
        presentationRequestReason: reason.map { SWBPaywallPresentationRequestStatusReason($0) }
      )
    case .touchesBegan:
      self.init(.touchesBegan, params: params)
    case .surveyClose:
      self.init(.surveyClose, params: params)
    case .reset:
      self.init(.reset, params: params)
    case .restoreStart:
      self.init(.restoreStart, params: params)
    case .restoreFail(let message):
      self.init(.restoreFail, params: params, message: message)
    case .restoreComplete:
      self.init(.restoreComplete, params: params)
    case .configRefresh:
      self.init(.configRefresh, params: params)
    case let .customPlacement(name, eventParams, paywallInfo):
      self.init(
        .customPlacement,
        params: SWBValueSanitizer.sanitize(eventParams),
        paywallInfo: SWBPaywallInfo(paywallInfo),
        name: name
      )
    case .configAttributes:
      self.init(.configAttributes, params: params)
    case .confirmAllAssignments:
      self.init(.confirmAllAssignments, params: params)
    case .configFail:
      self.init(.configFail, params: params)
    case .adServicesTokenRequestStart:
      self.init(.adServicesTokenRequestStart, params: params)
    case .adServicesTokenRequestFail(let error):
      self.init(.adServicesTokenRequestFail, params: params, error: error.localizedDescription)
    case .adServicesTokenRequestComplete(let token):
      self.init(.adServicesTokenRequestComplete, params: params, token: token)
    case .shimmerViewStart:
      self.init(.shimmerViewStart, params: params)
    case .shimmerViewComplete:
      self.init(.shimmerViewComplete, params: params)
    case .redemptionStart:
      self.init(.redemptionStart, params: params)
    case .redemptionComplete:
      self.init(.redemptionComplete, params: params)
    case .redemptionFail:
      self.init(.redemptionFail, params: params)
    case .enrichmentStart:
      self.init(.enrichmentStart, params: params)
    case let .enrichmentComplete(userEnrichment, deviceEnrichment):
      self.init(
        .enrichmentComplete,
        params: params,
        userEnrichment: SWBValueSanitizer.sanitize(userEnrichment),
        deviceEnrichment: SWBValueSanitizer.sanitize(deviceEnrichment)
      )
    case .enrichmentFail:
      self.init(.enrichmentFail, params: params)
    case .networkDecodingFail:
      self.init(.networkDecodingFail, params: params)
    case .customerInfoDidChange:
      self.init(.customerInfoDidChange, params: params)
    case .integrationAttributes(let attributes):
      self.init(.integrationAttributes, params: params, integrationAttributes: SWBValueSanitizer.sanitize(attributes))
    case .reviewRequested(let count):
      self.init(.reviewRequested, params: params, reviewRequestedCount: NSNumber(value: count))
    case let .permissionRequested(permissionName, paywallIdentifier):
      self.init(
        .permissionRequested,
        params: params,
        permissionName: permissionName,
        paywallIdentifier: paywallIdentifier
      )
    case let .permissionGranted(permissionName, paywallIdentifier):
      self.init(
        .permissionGranted,
        params: params,
        permissionName: permissionName,
        paywallIdentifier: paywallIdentifier
      )
    case let .permissionDenied(permissionName, paywallIdentifier):
      self.init(
        .permissionDenied,
        params: params,
        permissionName: permissionName,
        paywallIdentifier: paywallIdentifier
      )
    case .paywallPreloadStart(let paywallCount):
      self.init(.paywallPreloadStart, params: params, paywallCount: NSNumber(value: paywallCount))
    case .paywallPreloadComplete(let paywallCount):
      self.init(.paywallPreloadComplete, params: params, paywallCount: NSNumber(value: paywallCount))
    case .stripeCheckoutStart(let paywallInfo):
      self.init(.stripeCheckoutStart, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .stripeCheckoutSubmit(let paywallInfo):
      self.init(.stripeCheckoutSubmit, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .stripeCheckoutComplete(let paywallInfo):
      self.init(.stripeCheckoutComplete, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .stripeCheckoutFail(let paywallInfo):
      self.init(.stripeCheckoutFail, params: params, paywallInfo: SWBPaywallInfo(paywallInfo))
    case .testModeModalOpen:
      self.init(.testModeModalOpen, params: params)
    case .testModeModalClose:
      self.init(.testModeModalClose, params: params)
    case let .paywallPageView(paywallInfo, data):
      self.init(
        .paywallPageView,
        params: params,
        paywallInfo: SWBPaywallInfo(paywallInfo),
        pageViewData: SWBPageViewData(data)
      )
    @unknown default:
      self.init(.unknown, params: params, name: String(describing: info.event))
    }
  }
}

// MARK: - Bridge delegate protocol

/// @objc mirror of SuperwallKit.SuperwallDelegate. Kotlin implements the FULL
/// protocol (all methods required — user-facing optionality lives as default
/// no-ops in the commonMain SuperwallDelegate interface, plan §5.3).
///
/// All methods are invoked on the queue SuperwallKit calls its delegate on
/// (the main actor); redispatching is the Kotlin side's responsibility.
@objc(SWBBridgeDelegate)
public protocol SWBBridgeDelegate: AnyObject {
  /// Subscription status transition.
  @objc func subscriptionStatusDidChange(from oldStatus: SWBSubscriptionStatus, to newStatus: SWBSubscriptionStatus)
  /// A tracked Superwall event, fully destructured.
  @objc func handleSuperwallEvent(_ envelope: SWBEventEnvelope)
  /// A custom paywall action fired via `custom(...)` in the paywall.
  @objc func handleCustomPaywallAction(name: String)
  /// The paywall is about to present.
  @objc func willPresentPaywall(_ paywallInfo: SWBPaywallInfo)
  /// The paywall finished presenting.
  @objc func didPresentPaywall(_ paywallInfo: SWBPaywallInfo)
  /// The paywall is about to dismiss.
  @objc func willDismissPaywall(_ paywallInfo: SWBPaywallInfo)
  /// The paywall finished dismissing.
  @objc func didDismissPaywall(_ paywallInfo: SWBPaywallInfo)
  /// The paywall will open a URL.
  @objc func paywallWillOpenURL(_ url: String)
  /// The paywall will open a deep link.
  @objc func paywallWillOpenDeepLink(_ url: String)
  /// SDK log record. level/scope are the raw native strings; the Kotlin side maps
  /// them to LogLevel/LogScope enums (degrading with raw preservation, plan §3.4).
  @objc func handleLog(
    level: String,
    scope: String,
    message: String?,
    info: [String: Any]?,
    error: String?
  )
  /// A web-paywall code redemption is starting.
  @objc func willRedeemLink()
  /// A web-paywall code was redeemed.
  @objc func didRedeemLink(_ result: SWBRedemptionResult)
  /// A `…superwall.app/app-link/…` deep link was handled.
  @objc func handleSuperwallDeepLink(
    fullURL: String,
    pathComponents: [String],
    queryParameters: [String: String]
  )
  /// CustomerInfo transition.
  @objc func customerInfoDidChange(from oldInfo: SWBCustomerInfo, to newInfo: SWBCustomerInfo)
  /// The user attributes changed.
  @objc func userAttributesDidChange(_ newAttributes: [String: Any])
}

// MARK: - Internal forwarder

/// The SuperwallDelegate installed on Superwall.shared. Holds the app-provided
/// SWBBridgeDelegate weakly-safe (strong here; the bridge owns the lifetime) and
/// forwards every callback, converting payloads into @objc envelopes.
final class BridgeDelegateForwarder: SuperwallDelegate {
  private let bridgeDelegate: SWBBridgeDelegate

  init(bridgeDelegate: SWBBridgeDelegate) {
    self.bridgeDelegate = bridgeDelegate
  }

  func subscriptionStatusDidChange(from oldValue: SubscriptionStatus, to newValue: SubscriptionStatus) {
    bridgeDelegate.subscriptionStatusDidChange(
      from: SWBSubscriptionStatus(oldValue),
      to: SWBSubscriptionStatus(newValue)
    )
  }

  func handleSuperwallEvent(withInfo eventInfo: SuperwallEventInfo) {
    bridgeDelegate.handleSuperwallEvent(SWBEventEnvelope(eventInfo))
  }

  func handleCustomPaywallAction(withName name: String) {
    bridgeDelegate.handleCustomPaywallAction(name: name)
  }

  func willDismissPaywall(withInfo paywallInfo: PaywallInfo) {
    bridgeDelegate.willDismissPaywall(SWBPaywallInfo(paywallInfo))
  }

  func willPresentPaywall(withInfo paywallInfo: PaywallInfo) {
    bridgeDelegate.willPresentPaywall(SWBPaywallInfo(paywallInfo))
  }

  func didDismissPaywall(withInfo paywallInfo: PaywallInfo) {
    bridgeDelegate.didDismissPaywall(SWBPaywallInfo(paywallInfo))
  }

  func didPresentPaywall(withInfo paywallInfo: PaywallInfo) {
    bridgeDelegate.didPresentPaywall(SWBPaywallInfo(paywallInfo))
  }

  func paywallWillOpenURL(url: URL) {
    bridgeDelegate.paywallWillOpenURL(url.absoluteString)
  }

  func paywallWillOpenDeepLink(url: URL) {
    bridgeDelegate.paywallWillOpenDeepLink(url.absoluteString)
  }

  func handleLog(
    level: String,
    scope: String,
    message: String?,
    info: [String: Any]?,
    error: Swift.Error?
  ) {
    bridgeDelegate.handleLog(
      level: level,
      scope: scope,
      message: message,
      info: SWBValueSanitizer.sanitize(info),
      error: error?.localizedDescription
    )
  }

  func willRedeemLink() {
    bridgeDelegate.willRedeemLink()
  }

  func didRedeemLink(result: RedemptionResult) {
    bridgeDelegate.didRedeemLink(SWBRedemptionResult(result))
  }

  func handleSuperwallDeepLink(
    _ fullURL: URL,
    pathComponents: [String],
    queryParameters: [String: String]
  ) {
    bridgeDelegate.handleSuperwallDeepLink(
      fullURL: fullURL.absoluteString,
      pathComponents: pathComponents,
      queryParameters: queryParameters
    )
  }

  func customerInfoDidChange(from oldValue: CustomerInfo, to newValue: CustomerInfo) {
    bridgeDelegate.customerInfoDidChange(
      from: SWBCustomerInfo(oldValue),
      to: SWBCustomerInfo(newValue)
    )
  }

  func userAttributesDidChange(newAttributes: [String: Any]) {
    bridgeDelegate.userAttributesDidChange(SWBValueSanitizer.sanitize(newAttributes) ?? [:])
  }
}
