package com.superwall.sdk.kmp

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.superwall.sdk.kmp.internal.AndroidEmbeddedPaywall
import com.superwall.sdk.kmp.internal.AndroidSuperwallBridge

// ---------------------------------------------------------------------------
// The Android-only part of the public API: the few members whose native
// counterpart takes or returns an Android object (a View, a Play Billing
// type, an input event). The common API can't name those types, so these are
// extensions in the Android source set, callable from Android code only.
// Anything that can be expressed without a platform type belongs in
// commonMain instead: an embedded paywall is retrieved, held and released in
// common code, and only its View is read here.
// ---------------------------------------------------------------------------

private fun androidBridge(): AndroidSuperwallBridge =
    // Only a test's fake bridge is anything else.
    Superwall.requireConfiguredBridge() as? AndroidSuperwallBridge
        ?: error("This member needs the real Android Superwall bridge.")

/**
 * The view of a paywall retrieved with [Superwall.getPaywall], to add to your
 * layout. [PaywallViewCallback.onPresent] is called once it is attached to a
 * window. Call [EmbeddedPaywall.destroy] when you are done with it.
 *
 * In Compose:
 * ```kotlin
 * AndroidView(factory = { paywall.view }, onRelease = { paywall.destroy() })
 * ```
 *
 * @throws IllegalStateException for an [EmbeddedPaywall] that didn't come from [Superwall.getPaywall].
 */
public val EmbeddedPaywall.view: View
    get() =
        (this as? AndroidEmbeddedPaywall)?.host
            ?: error("This EmbeddedPaywall wasn't returned by Superwall.getPaywall on Android.")

/**
 * Reports that a purchase you make with your own Play Billing code is about
 * to start, with the `ProductDetails` you already hold. The same as the
 * common `Superwall.observePurchaseStart(productId)` without the product
 * lookup. Needs `SuperwallOptions.shouldObservePurchases`.
 *
 * @param product The product being purchased.
 */
@Suppress("UnusedReceiverParameter") // Scoped to Superwall for discoverability.
public fun Superwall.observePurchaseStart(product: ProductDetails) {
    androidBridge().observePurchaseStart(product)
}

/**
 * Reports that a purchase you made with your own Play Billing code failed,
 * with the `ProductDetails` you already hold. See [observePurchaseStart].
 *
 * @param product The product that failed to purchase.
 * @param error What went wrong.
 */
@Suppress("UnusedReceiverParameter")
public fun Superwall.observePurchaseError(
    product: ProductDetails,
    error: Throwable,
) {
    androidBridge().observePurchaseError(product, error)
}

/**
 * Reports the result of a purchase you made with your own Play Billing code:
 * pass on what your `PurchasesUpdatedListener` received. See
 * [observePurchaseStart].
 *
 * @param billingResult The result Play Billing reported.
 * @param purchases The purchases Play Billing reported.
 */
@Suppress("UnusedReceiverParameter")
public fun Superwall.observePurchaseResult(
    billingResult: BillingResult,
    purchases: List<Purchase>,
) {
    androidBridge().observePurchaseResult(billingResult, purchases)
}

/**
 * Forwards a game controller key event to the presented paywall. Call it
 * from your activity's `dispatchKeyEvent`. Needs
 * `SuperwallOptions.isGameControllerEnabled`.
 */
@Suppress("UnusedReceiverParameter")
public fun Superwall.dispatchKeyEvent(keyEvent: KeyEvent) {
    androidBridge().dispatchKeyEvent(keyEvent)
}

/**
 * Forwards a game controller motion event (sticks, triggers) to the presented
 * paywall. Call it from your activity's `dispatchGenericMotionEvent`. Needs
 * `SuperwallOptions.isGameControllerEnabled`.
 */
@Suppress("UnusedReceiverParameter")
public fun Superwall.dispatchMotionEvent(motionEvent: MotionEvent) {
    androidBridge().dispatchMotionEvent(motionEvent)
}
