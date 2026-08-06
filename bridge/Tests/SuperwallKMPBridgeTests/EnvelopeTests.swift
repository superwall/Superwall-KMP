//
//  EnvelopeTests.swift
//  SuperwallKMPBridgeTests
//
//  Tests everything testable without network or a configured Superwall instance:
//  - envelope construction and field carriage (including the Flutter-mislabel fixes)
// - the configure-status derivation
//  - options mapping to real SuperwallKit types
//  - value sanitization (degrade-never-crash)
//  - enum raw-value stability (the Kotlin side codes against these numbers)
//
//  Deep destructuring of SDK-produced types (PaywallInfo, StoreTransaction, …)
//  requires a configured SDK and lives in the simulator/Phase-3 test tiers.
//

import XCTest
import SuperwallKit
@testable import SuperwallKMPBridge

final class ConfigureStatusDerivationTests: XCTestCase {
  func testConfiguredMapsToConfiguredWithoutError() {
    let outcome = SWBSuperwallBridge.deriveConfigureOutcome(from: .configured)
    XCTAssertEqual(outcome.status, .configured)
    XCTAssertNil(outcome.error)
  }

  func testFailedMapsToFailedWithError() {
    let outcome = SWBSuperwallBridge.deriveConfigureOutcome(from: .failed)
    XCTAssertEqual(outcome.status, .failed)
    let error = try! XCTUnwrap(outcome.error)
    XCTAssertEqual(error.domain, SWBSuperwallBridge.errorDomain)
    XCTAssertFalse(error.localizedDescription.isEmpty)
  }

  func testPendingMapsToPendingWithoutError() {
    // Post-completion.pending should not happen, but must degrade, not fail.
    let outcome = SWBSuperwallBridge.deriveConfigureOutcome(from: .pending)
    XCTAssertEqual(outcome.status, .pending)
    XCTAssertNil(outcome.error)
  }
}

final class EnvelopeConstructionTests: XCTestCase {
  private func makeEntitlement(id: String = "premium") -> SWBEntitlement {
    return SWBEntitlement(SuperwallKit.Entitlement(id: id))
  }

  // MARK: Entitlement / subscription status

  func testEntitlementFromNativeIdOnlyInitializer() {
    let entitlement = makeEntitlement(id: "pro")
    XCTAssertEqual(entitlement.id, "pro")
    XCTAssertEqual(entitlement.type, .serviceLevel)
    XCTAssertTrue(entitlement.isActive)
    // The id-only native initializer implies the App Store.
    XCTAssertEqual(entitlement.store?.intValue, SWBProductStore.appStore.rawValue)
    XCTAssertNil(entitlement.startsAt)
    XCTAssertNil(entitlement.state)
  }

  func testSubscriptionStatusRoundTripActive() {
    let native = SubscriptionStatus.active([Entitlement(id: "premium")])
    let envelope = SWBSubscriptionStatus(native)
    XCTAssertEqual(envelope.status, .active)
    XCTAssertEqual(envelope.entitlements.count, 1)
    XCTAssertEqual(envelope.entitlements.first?.id, "premium")

    // Back-conversion keeps the entitlement ids.
    guard case .active(let entitlements) = envelope.native else {
      return XCTFail("Expected .active")
    }
    XCTAssertEqual(entitlements.map { $0.id }, ["premium"])
  }

  func testSubscriptionStatusEmptyCases() {
    XCTAssertEqual(SWBSubscriptionStatus(.inactive).status, .inactive)
    XCTAssertTrue(SWBSubscriptionStatus(.inactive).entitlements.isEmpty)
    XCTAssertEqual(SWBSubscriptionStatus(.unknown).status, .unknown)

    guard case .inactive = SWBSubscriptionStatus(.inactive).native else {
      return XCTFail("Expected .inactive")
    }
    guard case .unknown = SWBSubscriptionStatus(.unknown).native else {
      return XCTFail("Expected .unknown")
    }
  }

  // MARK: Result envelopes

  func testTriggerResultErrorCarriesDescription() {
    let nsError = NSError(
      domain: "test",
      code: 7,
      userInfo: [NSLocalizedDescriptionKey: "boom"]
    )
    let envelope = SWBTriggerResult(TriggerResult.error(nsError))
    XCTAssertEqual(envelope.result, .error)
    XCTAssertEqual(envelope.error, "boom")
    XCTAssertNil(envelope.experiment)
  }

  func testTriggerResultEmptyCases() {
    XCTAssertEqual(SWBTriggerResult(.placementNotFound).result, .placementNotFound)
    XCTAssertEqual(SWBTriggerResult(.noAudienceMatch).result, .noAudienceMatch)
  }

  func testPresentationResultCases() {
    XCTAssertEqual(SWBPresentationResult(.placementNotFound).result, .placementNotFound)
    XCTAssertEqual(SWBPresentationResult(.noAudienceMatch).result, .noAudienceMatch)
    XCTAssertEqual(SWBPresentationResult(.paywallNotAvailable).result, .paywallNotAvailable)
    XCTAssertNil(SWBPresentationResult(.paywallNotAvailable).experiment)
  }

  func testRestorationResultEnvelope() {
    XCTAssertEqual(SWBRestorationResult(.restored).result, .restored)
    XCTAssertNil(SWBRestorationResult(.restored).errorMessage)

    let nsError = NSError(
      domain: "test",
      code: 1,
      userInfo: [NSLocalizedDescriptionKey: "restore failed"]
    )
    let failed = SWBRestorationResult(RestorationResult.failed(nsError))
    XCTAssertEqual(failed.result, .failed)
    XCTAssertEqual(failed.errorMessage, "restore failed")

    let failedNilError = SWBRestorationResult(RestorationResult.failed(nil))
    XCTAssertEqual(failedNilError.result, .failed)
    XCTAssertNil(failedNilError.errorMessage)
  }

  func testRestorationResultBackConversion() {
    let restored = SWBRestorationResult(result: .restored, errorMessage: nil)
    guard case .restored = restored.native else {
      return XCTFail("Expected .restored")
    }
    let failed = SWBRestorationResult(result: .failed, errorMessage: "nope")
    guard case .failed(let error) = failed.native else {
      return XCTFail("Expected .failed")
    }
    XCTAssertEqual(error?.localizedDescription, "nope")
  }

  func testPurchaseResultBackConversion() {
    guard case .purchased = SWBPurchaseResult(result: .purchased, errorMessage: nil).native else {
      return XCTFail("Expected .purchased")
    }
    guard case .pending = SWBPurchaseResult(result: .pending, errorMessage: nil).native else {
      return XCTFail("Expected .pending")
    }
    guard case .cancelled = SWBPurchaseResult(result: .cancelled, errorMessage: nil).native else {
      return XCTFail("Expected .cancelled")
    }
    guard
      case .failed(let error) = SWBPurchaseResult(result: .failed, errorMessage: "declined").native
    else {
      return XCTFail("Expected .failed")
    }
    XCTAssertEqual(error.localizedDescription, "declined")
  }

  func testRestoreTypeViaRestore() {
    let envelope = SWBRestoreType(RestoreType.viaRestore)
    XCTAssertEqual(envelope.type, .viaRestore)
    XCTAssertNil(envelope.storeTransaction)
  }

  func testRestoreTypeViaPurchaseWithNilTransaction() {
    let envelope = SWBRestoreType(RestoreType.viaPurchase(nil))
    XCTAssertEqual(envelope.type, .viaPurchase)
    XCTAssertNil(envelope.storeTransaction)
  }

  func testCustomCallbackResultBackConversion() {
    let success = SWBCustomCallbackResult(status: .success, data: ["k": "v"]).native
    XCTAssertEqual(success.status, .success)
    XCTAssertEqual(success.data?["k"] as? String, "v")

    let failure = SWBCustomCallbackResult(status: .failure, data: nil).native
    XCTAssertEqual(failure.status, .failure)
    XCTAssertNil(failure.data)
  }

  // MARK: Event envelope — sparse construction & fidelity fixes

  func testSparseEnvelopeDefaultsAllPayloadsNil() {
    let envelope = SWBEventEnvelope(.appOpen, params: ["k": "v"])
    XCTAssertEqual(envelope.eventType, .appOpen)
    XCTAssertEqual(envelope.params?["k"] as? String, "v")
    XCTAssertNil(envelope.paywallInfo)
    XCTAssertNil(envelope.transaction)
    XCTAssertNil(envelope.product)
    XCTAssertNil(envelope.transactionType)
    XCTAssertNil(envelope.restoreType)
    XCTAssertNil(envelope.triggerResult)
    XCTAssertNil(envelope.survey)
    XCTAssertNil(envelope.attributionMatch)
    XCTAssertNil(envelope.pageViewData)
  }

  func testTransactionRestoreKeepsItsOwnEventType() {
    // Fidelity fix: the Flutter host mislabeled transactionRestore as
    // transactionComplete. The two must be distinct envelope types.
    XCTAssertNotEqual(SWBEventType.transactionRestore, SWBEventType.transactionComplete)
    let envelope = SWBEventEnvelope(
      .transactionRestore,
      restoreType: SWBRestoreType(RestoreType.viaRestore)
    )
    XCTAssertEqual(envelope.eventType, .transactionRestore)
    XCTAssertEqual(envelope.restoreType?.type, .viaRestore)
  }

  func testTransactionCompleteCarriesTransactionType() {
    // Fidelity fix: the Flutter host dropped `type` on transactionComplete.
    let envelope = SWBEventEnvelope(
      .transactionComplete,
      transactionType: NSNumber(value: SWBTransactionType.freeTrialStart.rawValue)
    )
    XCTAssertEqual(
      envelope.transactionType?.intValue,
      SWBTransactionType.freeTrialStart.rawValue
    )
  }

  func testDeepLinkEnvelopeCarriesUrl() {
    let info = SuperwallEventInfo(
      event: .deepLink(url: URL(string: "https://example.com/promo?x=1")!),
      params: ["source": "test"]
    )
    let envelope = SWBEventEnvelope(info)
    XCTAssertEqual(envelope.eventType, .deepLink)
    XCTAssertEqual(envelope.deepLinkUrl, "https://example.com/promo?x=1")
    XCTAssertEqual(envelope.params?["source"] as? String, "test")
  }

  func testDeviceAttributesEnvelopeSanitizesPayload() {
    let info = SuperwallEventInfo(
      event: .deviceAttributes(attributes: [
        "os": "iOS",
        "count": 3,
        "weird": URL(string: "https://x.dev")!
      ]),
      params: [:]
    )
    let envelope = SWBEventEnvelope(info)
    XCTAssertEqual(envelope.eventType, .deviceAttributes)
    XCTAssertEqual(envelope.deviceAttributes?["os"] as? String, "iOS")
    XCTAssertEqual(envelope.deviceAttributes?["count"] as? Int, 3)
    // URL degrades to a string, never dropped/crashed.
    XCTAssertEqual(envelope.deviceAttributes?["weird"] as? String, "https://x.dev")
  }

  func testRestoreFailEnvelopeCarriesMessage() {
    let info = SuperwallEventInfo(event: .restoreFail(message: "no receipt"), params: [:])
    let envelope = SWBEventEnvelope(info)
    XCTAssertEqual(envelope.eventType, .restoreFail)
    XCTAssertEqual(envelope.message, "no receipt")
  }

  func testAdServicesTokenCompleteCarriesToken() {
    let info = SuperwallEventInfo(
      event: .adServicesTokenRequestComplete(token: "tok_123"),
      params: [:]
    )
    let envelope = SWBEventEnvelope(info)
    XCTAssertEqual(envelope.eventType, .adServicesTokenRequestComplete)
    XCTAssertEqual(envelope.token, "tok_123")
  }

  func testReviewRequestedCarriesCount() {
    let info = SuperwallEventInfo(event: .reviewRequested(count: 4), params: [:])
    let envelope = SWBEventEnvelope(info)
    XCTAssertEqual(envelope.eventType, .reviewRequested)
    XCTAssertEqual(envelope.reviewRequestedCount?.intValue, 4)
  }

  func testPermissionEventsCarryNameAndPaywall() {
    let info = SuperwallEventInfo(
      event: .permissionGranted(permissionName: "notifications", paywallIdentifier: "pw_1"),
      params: [:]
    )
    let envelope = SWBEventEnvelope(info)
    XCTAssertEqual(envelope.eventType, .permissionGranted)
    XCTAssertEqual(envelope.permissionName, "notifications")
    XCTAssertEqual(envelope.paywallIdentifier, "pw_1")
  }

  func testPaywallPreloadEventsCarryCount() {
    let start = SWBEventEnvelope(
      SuperwallEventInfo(event: .paywallPreloadStart(paywallCount: 5), params: [:])
    )
    XCTAssertEqual(start.eventType, .paywallPreloadStart)
    XCTAssertEqual(start.paywallCount?.intValue, 5)
  }

  func testEnrichmentCompleteCarriesBothEnrichments() {
    let info = SuperwallEventInfo(
      event: .enrichmentComplete(
        userEnrichment: ["tier": "gold"],
        deviceEnrichment: ["region": "EU"]
      ),
      params: [:]
    )
    let envelope = SWBEventEnvelope(info)
    XCTAssertEqual(envelope.eventType, .enrichmentComplete)
    XCTAssertEqual(envelope.userEnrichment?["tier"] as? String, "gold")
    XCTAssertEqual(envelope.deviceEnrichment?["region"] as? String, "EU")
  }

  func testUserAttributesEnvelope() {
    let info = SuperwallEventInfo(
      event: .userAttributes(["plan": "annual"]),
      params: [:]
    )
    let envelope = SWBEventEnvelope(info)
    XCTAssertEqual(envelope.eventType, .userAttributes)
    XCTAssertEqual(envelope.userAttributes?["plan"] as? String, "annual")
  }

  func testSimpleEventsMapOneToOne() {
    let cases: [(SuperwallEvent, SWBEventType)] = [
      (.firstSeen, .firstSeen),
      (.appOpen, .appOpen),
      (.appLaunch, .appLaunch),
      (.identityAlias, .identityAlias),
      (.appInstall, .appInstall),
      (.sessionStart, .sessionStart),
      (.subscriptionStatusDidChange, .subscriptionStatusDidChange),
      (.appClose, .appClose),
      (.touchesBegan, .touchesBegan),
      (.surveyClose, .surveyClose),
      (.reset, .reset),
      (.restoreStart, .restoreStart),
      (.restoreComplete, .restoreComplete),
      (.configRefresh, .configRefresh),
      (.configAttributes, .configAttributes),
      (.confirmAllAssignments, .confirmAllAssignments),
      (.configFail, .configFail),
      (.adServicesTokenRequestStart, .adServicesTokenRequestStart),
      (.shimmerViewStart, .shimmerViewStart),
      (.shimmerViewComplete, .shimmerViewComplete),
      (.redemptionStart, .redemptionStart),
      (.redemptionComplete, .redemptionComplete),
      (.redemptionFail, .redemptionFail),
      (.enrichmentStart, .enrichmentStart),
      (.enrichmentFail, .enrichmentFail),
      (.networkDecodingFail, .networkDecodingFail),
      (.customerInfoDidChange, .customerInfoDidChange),
      (.testModeModalOpen, .testModeModalOpen),
      (.testModeModalClose, .testModeModalClose)
    ]
    for (event, expected) in cases {
      let envelope = SWBEventEnvelope(SuperwallEventInfo(event: event, params: [:]))
      XCTAssertEqual(envelope.eventType, expected, "\(event) mapped to \(envelope.eventType)")
    }
  }

  // MARK: Redemption

  func testInvalidCodeRedemptionEnvelope() {
    let envelope = SWBRedemptionResult(RedemptionResult.invalidCode(code: "CODE1"))
    XCTAssertEqual(envelope.result, .invalidCode)
    XCTAssertEqual(envelope.code, "CODE1")
    XCTAssertNil(envelope.redemptionInfo)
    XCTAssertNil(envelope.errorInfo)
    XCTAssertNil(envelope.expiredCodeInfo)
  }
}

final class ValueSanitizerTests: XCTestCase {
  func testPassThroughBasicTypes() {
    let sanitized = SWBValueSanitizer.sanitize([
      "string": "s",
      "int": 42,
      "double": 1.5,
      "bool": true,
      "array": ["a", 1],
      "dict": ["nested": "x"]
    ])!
    XCTAssertEqual(sanitized["string"] as? String, "s")
    XCTAssertEqual(sanitized["int"] as? Int, 42)
    XCTAssertEqual(sanitized["double"] as? Double, 1.5)
    XCTAssertEqual(sanitized["bool"] as? Bool, true)
    XCTAssertEqual((sanitized["array"] as? [Any])?.count, 2)
    XCTAssertEqual((sanitized["dict"] as? [String: Any])?["nested"] as? String, "x")
  }

  func testSetsBecomeArrays() {
    let sanitized = SWBValueSanitizer.sanitize(["set": Set(["one"])])!
    XCTAssertEqual(sanitized["set"] as? [String], ["one"])
  }

  func testUnknownValuesDegradeToDescription() {
    struct Opaque { let x = 1 }
    let sanitized = SWBValueSanitizer.sanitize(["weird": Opaque()])!
    XCTAssertNotNil(sanitized["weird"] as? String)
  }

  func testNilInputStaysNil() {
    XCTAssertNil(SWBValueSanitizer.sanitize(nil))
  }
}

final class OptionsMappingTests: XCTestCase {
  func testNilFieldsKeepNativeDefaults() {
    let defaults = SuperwallOptions()
    let mapped = SWBSuperwallOptions().native
    XCTAssertEqual(mapped.paywalls.shouldPreload, defaults.paywalls.shouldPreload)
    XCTAssertEqual(mapped.maxConfigRetryCount, defaults.maxConfigRetryCount)
    XCTAssertEqual(mapped.logging.level, defaults.logging.level)
    XCTAssertEqual(
      mapped.enableExperimentalDeviceVariables,
      defaults.enableExperimentalDeviceVariables
    )
  }

  func testSetFieldsReachNativeOptions() {
    let options = SWBSuperwallOptions()
    options.enableExperimentalDeviceVariables = NSNumber(value: true)
    options.maxConfigRetryCount = NSNumber(value: 2)
    options.testModeBehavior = NSNumber(value: SWBTestModeBehavior.never.rawValue)
    options.networkEnvironment = NSNumber(value: SWBNetworkEnvironment.developer.rawValue)
    options.localeIdentifier = "en_GB"

    let paywalls = SWBPaywallOptions()
    paywalls.shouldPreload = NSNumber(value: false)
    paywalls.transactionBackgroundView = NSNumber(
      value: SWBTransactionBackgroundView.none.rawValue
    )
    let restoreFailed = SWBRestoreFailedOptions()
    restoreFailed.title = "Restore failed"
    paywalls.restoreFailed = restoreFailed
    options.paywalls = paywalls

    let logging = SWBLoggingOptions()
    logging.level = NSNumber(value: SWBLogLevel.error.rawValue)
    logging.scopes = [NSNumber(value: SWBLogScope.network.rawValue)]
    options.logging = logging

    let native = options.native
    XCTAssertTrue(native.enableExperimentalDeviceVariables)
    XCTAssertEqual(native.maxConfigRetryCount, 2)
    XCTAssertEqual(native.testModeBehavior, .never)
    XCTAssertEqual(native.localeIdentifier, "en_GB")
    XCTAssertFalse(native.paywalls.shouldPreload)
    XCTAssertEqual(native.paywalls.transactionBackgroundView, PaywallOptions.TransactionBackgroundView.none)
    XCTAssertEqual(native.paywalls.restoreFailed.title, "Restore failed")
    XCTAssertEqual(native.logging.level, .error)
    XCTAssertEqual(native.logging.scopes, [.network])
  }
}

final class ContractStabilityTests: XCTestCase {
  // The Kotlin side maps these raw values 1:1 — they are the wire contract
  // A change here is a breaking bridge change.

  func testEventTypeRawValuesAreStable() {
    XCTAssertEqual(SWBEventType.firstSeen.rawValue, 0)
    XCTAssertEqual(SWBEventType.transactionComplete.rawValue, 17)
    XCTAssertEqual(SWBEventType.transactionRestore.rawValue, 20)
    XCTAssertEqual(SWBEventType.attributionMatch.rawValue, 23)
    XCTAssertEqual(SWBEventType.paywallPageView.rawValue, 79)
    XCTAssertEqual(SWBEventType.unknown.rawValue, 999)
  }

  func testStatusEnumRawValues() {
    XCTAssertEqual(SWBConfigurationStatus.pending.rawValue, 0)
    XCTAssertEqual(SWBConfigurationStatus.configured.rawValue, 1)
    XCTAssertEqual(SWBConfigurationStatus.failed.rawValue, 2)
    XCTAssertEqual(SWBSubscriptionStatusCase.unknown.rawValue, 0)
    XCTAssertEqual(SWBSubscriptionStatusCase.inactive.rawValue, 1)
    XCTAssertEqual(SWBSubscriptionStatusCase.active.rawValue, 2)
  }

  func testBridgeVersionIsExposed() {
    XCTAssertEqual(SWBSuperwallBridge.bridgeVersion, SWBBridgeVersion)
    XCTAssertFalse(SWBSuperwallBridge.bridgeVersion.isEmpty)
  }

  func testConsumeEchoesToken() {
    let expectation = expectation(description: "consume echoes")
    SWBSuperwallBridge.shared.consume(purchaseToken: "token-1") { echoed in
      XCTAssertEqual(echoed, "token-1")
      expectation.fulfill()
    }
    waitForExpectations(timeout: 1)
  }

  func testHandleDeepLinkRejectsUnparseableUrl() {
    XCTAssertFalse(SWBSuperwallBridge.shared.handleDeepLink(""))
  }
}

final class OnceGateTests: XCTestCase {
  func testGatePassesExactlyOnce() {
    let gate = OnceGate()
    XCTAssertTrue(gate.tryPass())
    XCTAssertFalse(gate.tryPass())
    XCTAssertFalse(gate.tryPass())
  }
}
