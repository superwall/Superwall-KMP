@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.internal.IosEmbeddedPaywall
import com.superwall.sdk.kmp.internal.IosSuperwallBridge
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration
import platform.GameController.GCControllerElement
import platform.GameController.GCExtendedGamepad
import platform.UIKit.UIViewController

// ---------------------------------------------------------------------------
// The iOS-only part of the public API: the few members whose native
// counterpart takes or returns a UIKit or GameController object. The common
// API can't name those types, so these are extensions in the iOS source set,
// callable from iOS code only. Anything that can be expressed without a
// platform type belongs in commonMain instead: an embedded paywall is
// retrieved, held and released in common code, and only its view controller
// is read here.
// ---------------------------------------------------------------------------

private fun iosBridge(): IosSuperwallBridge =
    // Only a test's fake bridge is anything else.
    Superwall.requireConfiguredBridge() as? IosSuperwallBridge
        ?: error("This member needs the real iOS Superwall bridge.")

/**
 * The view controller of a paywall retrieved with [Superwall.getPaywall], for
 * you to put on screen however you want. Don't show the same one in two
 * places at once. Call [EmbeddedPaywall.destroy] when you are done with it.
 *
 * In Compose Multiplatform:
 * ```kotlin
 * UIKitViewController(factory = { paywall.viewController }, modifier = Modifier.fillMaxSize())
 * ```
 *
 * @throws IllegalStateException for an [EmbeddedPaywall] that didn't come
 * from [Superwall.getPaywall], or that was already destroyed.
 */
public val EmbeddedPaywall.viewController: UIViewController
    get() =
        (this as? IosEmbeddedPaywall)?.viewController
            ?: error("This EmbeddedPaywall wasn't returned by Superwall.getPaywall on iOS.")

/**
 * The view controller of the paywall Superwall is presenting, or `null` when
 * none is. Callable before [Superwall.configure], where it is `null`.
 */
@Suppress("UnusedReceiverParameter") // Scoped to Superwall for discoverability.
public val Superwall.presentedViewController: UIViewController?
    get() = IosSuperwallBridge.presentedPaywallViewController()

/**
 * Forwards a game controller event to the presented paywall. Call it from the
 * gamepad's `valueChangedHandler`. Needs
 * `SuperwallOptions.isGameControllerEnabled`.
 *
 * @param gamepad The extended gamepad profile of the controller.
 * @param element The element whose value changed.
 */
@Suppress("UnusedReceiverParameter")
public fun Superwall.gamepadValueChanged(
    gamepad: GCExtendedGamepad,
    element: GCControllerElement,
) {
    iosBridge().gamepadValueChanged(gamepad, element)
}

/**
 * Makes a Customer Center view controller for you to present or push
 * yourself, instead of calling [Superwall.presentCustomerCenter]. Call it on
 * the main thread.
 *
 * @param configuration Overrides `SuperwallOptions.customerCenter` for this
 * view controller; `null` uses the configured value.
 * @param embedded `false` (the default) if you present it modally: it then
 * brings its own navigation and close button. `true` if you push it onto a
 * navigation controller of your own: it then uses your navigation bar.
 * @param delegate Receives the Customer Center's events on the main thread.
 * It is kept for as long as the view controller lives.
 */
@Suppress("UnusedReceiverParameter")
public fun Superwall.customerCenterViewController(
    configuration: CustomerCenterConfiguration? = null,
    embedded: Boolean = false,
    delegate: CustomerCenterDelegate? = null,
): UIViewController = iosBridge().customerCenterViewController(configuration, embedded, delegate)
