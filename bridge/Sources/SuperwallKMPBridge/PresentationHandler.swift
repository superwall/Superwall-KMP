//
//  PresentationHandler.swift
//  SuperwallKMPBridge
//
//  The @objc presentation-handler protocol Kotlin implements for register(...),
//  plus the internal wrapper that builds the native PaywallPresentationHandler
//  and manages per-registration retention (port of the Flutter
// PaywallPresentationHandlerHost, with the leak fixed per the wrapper
//  releases on onDismiss, onSkip AND onError — the Flutter host never released
//  on error, leaking the adapter when no paywall was ever presented).
//

import Foundation
import SuperwallKit

/// Completion-handler mirror of SuperwallKit.PaywallPresentationHandler's callbacks.
///
/// Callbacks arrive on the queue SuperwallKit fires them on (main);
/// redispatching is the Kotlin side's job.
@objc(SWBPaywallPresentationHandler)
public protocol SWBPaywallPresentationHandler: AnyObject {
  /// The paywall presented.
  @objc func onPresent(_ paywallInfo: SWBPaywallInfo)
  /// The paywall dismissed with a result.
  @objc func onDismiss(_ paywallInfo: SWBPaywallInfo, result: SWBPaywallResult)
  /// Presentation failed. No further callbacks are expected after this
  /// (verified empirically in Phase 1; the bridge releases its wrapper here).
  @objc func onError(_ error: String)
  /// The paywall was skipped.
  @objc func onSkip(_ reason: SWBPaywallSkippedReason)
  /// The paywall requested a custom callback. Invoke `completion` exactly once;
  /// if the Kotlin side cannot produce a result, complete with a failure result.
  @objc func onCustomCallback(
    _ callback: SWBCustomCallback,
    completion: @escaping (SWBCustomCallbackResult) -> Void
  )
}

/// Internal per-registration wrapper. Strongly retains the Kotlin-implemented
/// handler and the feature closure for the lifetime of the presentation
/// (per-registration adapters, no placement-keyed registry).
final class PresentationHandlerWrapper {
  /// The native handler handed to Superwall.register.
  let nativeHandler: PaywallPresentationHandler
  private let bridgeHandler: SWBPaywallPresentationHandler
  private let release: (PresentationHandlerWrapper) -> Void

  init(
    bridgeHandler: SWBPaywallPresentationHandler,
    release: @escaping (PresentationHandlerWrapper) -> Void
  ) {
    self.bridgeHandler = bridgeHandler
    self.release = release
    self.nativeHandler = PaywallPresentationHandler()

    nativeHandler.onPresent { [bridgeHandler] paywallInfo in
      bridgeHandler.onPresent(SWBPaywallInfo(paywallInfo))
    }

    nativeHandler.onDismiss { [weak self, bridgeHandler] paywallInfo, result in
      bridgeHandler.onDismiss(SWBPaywallInfo(paywallInfo), result: SWBPaywallResult(result))
      // Mirror of the Flutter host's cleanup condition: only a real close
      // (closeReason !=.none) ends the registration.
      if paywallInfo.closeReason != .none, let self = self {
        self.release(self)
      }
    }

    nativeHandler.onError { [weak self, bridgeHandler] error in
      bridgeHandler.onError(error.localizedDescription)
      // Deliberate delta vs the Flutter host (which leaked here): a failed
      // presentation gets no dismiss/skip, so release now.
      if let self = self {
        self.release(self)
      }
    }

    nativeHandler.onSkip { [weak self, bridgeHandler] reason in
      bridgeHandler.onSkip(SWBPaywallSkippedReason(reason))
      if let self = self {
        self.release(self)
      }
    }

    nativeHandler.onCustomCallback { [bridgeHandler] (callback: CustomCallback) async -> CustomCallbackResult in
      return await withCheckedContinuation { continuation in
        let once = OnceGate()
        bridgeHandler.onCustomCallback(SWBCustomCallback(callback)) { result in
          guard once.tryPass() else { return }
          continuation.resume(returning: result.native)
        }
      }
    }
  }
}
