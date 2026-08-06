//
//  BridgePurchaseController.swift
//  SuperwallKMPBridge
//
//  The @objc purchase-controller protocol Kotlin implements, plus the internal
//  SuperwallKit.PurchaseController that forwards to it. Registered at configure
//  time when the caller provides one (port of the Flutter PurchaseControllerHost).
//

import Foundation
import SuperwallKit

/// Completion-handler mirror of SuperwallKit.PurchaseController.
///
/// THREADING (critical): SuperwallKit invokes purchases on the MAIN ACTOR. The
/// Kotlin implementation must dispatch its suspend work asynchronously (launch,
/// never runBlocking) and may invoke the completion from ANY queue — the bridge
/// resumes the awaiting continuation safely from wherever the completion fires
/// (blocking the main queue here deadlocks the SDK).
@objc(SWBPurchaseController)
public protocol SWBPurchaseController: AnyObject {
  /// Purchase the App Store product with the given identifier.
  /// Invoke `completion` exactly once with the outcome.
  @objc func purchase(productId: String, completion: @escaping (SWBPurchaseResult) -> Void)

  /// Restore purchases. Invoke `completion` exactly once with the outcome.
  @objc func restorePurchases(completion: @escaping (SWBRestorationResult) -> Void)
}

/// Internal adapter: SuperwallKit.PurchaseController → SWBPurchaseController.
final class BridgePurchaseControllerForwarder: PurchaseController {
  private let bridgeController: SWBPurchaseController

  init(bridgeController: SWBPurchaseController) {
    self.bridgeController = bridgeController
  }

  @MainActor
  func purchase(product: StoreProduct) async -> PurchaseResult {
    return await withCheckedContinuation { continuation in
      // Guard against double-resume from a misbehaving Kotlin impl:
      // only the first completion counts (degrade, never crash).
      let once = OnceGate()
      bridgeController.purchase(productId: product.productIdentifier) { result in
        guard once.tryPass() else { return }
        continuation.resume(returning: result.native)
      }
    }
  }

  @MainActor
  func restorePurchases() async -> RestorationResult {
    return await withCheckedContinuation { continuation in
      let once = OnceGate()
      bridgeController.restorePurchases { result in
        guard once.tryPass() else { return }
        continuation.resume(returning: result.native)
      }
    }
  }
}

/// Thread-safe single-pass gate.
final class OnceGate {
  private let lock = NSLock()
  private var passed = false

  func tryPass() -> Bool {
    lock.lock()
    defer { lock.unlock() }
    if passed { return false }
    passed = true
    return true
  }
}
