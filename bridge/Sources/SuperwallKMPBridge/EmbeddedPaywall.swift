//
//  EmbeddedPaywall.swift
//  SuperwallKMPBridge
//
//  A paywall retrieved with SuperwallKit's getPaywall for the app to show in
//  its own UI: the @objc delegate protocol Kotlin implements, the internal
//  PaywallViewControllerDelegate that forwards to it, and the envelope that
//  carries the view controller to Kotlin.
//

import Foundation
import SuperwallKit
import UIKit

/// Completion-handler mirror of SuperwallKit.PaywallViewControllerDelegate,
/// plus getPaywall's custom callback. SuperwallKit calls both on the main actor.
@objc(SWBPaywallViewDelegate)
public protocol SWBPaywallViewDelegate: AnyObject {
  /// The user finished interacting with the paywall.
  @objc func paywallDidFinish(_ paywallInfo: SWBPaywallInfo, result: SWBPaywallResult, shouldDismiss: Bool)
  /// The paywall requested a custom callback. Invoke `completion` exactly once.
  @objc func onCustomCallback(
    _ callback: SWBCustomCallback,
    completion: @escaping (SWBCustomCallbackResult) -> Void
  )
}

/// Internal adapter: SuperwallKit.PaywallViewControllerDelegate → SWBPaywallViewDelegate.
@MainActor
final class BridgePaywallViewDelegateForwarder: PaywallViewControllerDelegate {
  let bridgeDelegate: SWBPaywallViewDelegate

  init(bridgeDelegate: SWBPaywallViewDelegate) {
    self.bridgeDelegate = bridgeDelegate
  }

  func paywall(
    _ paywall: PaywallViewController,
    didFinishWith result: PaywallResult,
    shouldDismiss: Bool
  ) {
    bridgeDelegate.paywallDidFinish(
      SWBPaywallInfo(paywall.info),
      result: SWBPaywallResult(result),
      shouldDismiss: shouldDismiss
    )
  }
}

/// A retrieved paywall. SuperwallKit doesn't keep the delegate it is given
/// alive, so this object does: the paywall reports to Kotlin for as long as
/// Kotlin holds on to it.
@MainActor
@objc(SWBEmbeddedPaywall)
public final class SWBEmbeddedPaywall: NSObject {
  /// The PaywallViewController, for the app to put on screen.
  @objc public let viewController: UIViewController
  private let paywallViewController: PaywallViewController
  private let forwarder: BridgePaywallViewDelegateForwarder

  init(paywallViewController: PaywallViewController, forwarder: BridgePaywallViewDelegateForwarder) {
    self.viewController = paywallViewController
    self.paywallViewController = paywallViewController
    self.forwarder = forwarder
    super.init()
  }

  /// The paywall's info as it is now.
  @objc public func currentInfo() -> SWBPaywallInfo {
    return SWBPaywallInfo(paywallViewController.info)
  }
}
