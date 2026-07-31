package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.callbacks.CustomCallback
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResult
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason

/**
 * The handler for [Superwall.register] whose closures provide status updates
 * for a paywall.
 *
 * Set the closures you care about via the builder-style setters ([onPresent],
 * [onDismiss], [onError], [onSkip], [onCustomCallback]); unset closures are
 * simply not invoked. All closures are delivered on the main thread.
 */
public class PaywallPresentationHandler {
    /** The closure invoked when the paywall is presented. */
    internal var onPresentHandler: ((PaywallInfo) -> Unit)? = null

    /** The closure invoked when the paywall is dismissed. */
    internal var onDismissHandler: ((PaywallInfo, PaywallResult) -> Unit)? = null

    /** The closure invoked when an error occurs while trying to present the paywall. */
    internal var onErrorHandler: ((String) -> Unit)? = null

    /** The closure invoked when presentation of the paywall is skipped. */
    internal var onSkipHandler: ((PaywallSkippedReason) -> Unit)? = null

    /** The suspend closure invoked when the paywall requests a custom callback. */
    internal var onCustomCallbackHandler: (suspend (CustomCallback) -> CustomCallbackResult)? = null

    /**
     * Sets the closure that's called when the paywall is presented.
     *
     * @param block Receives the [PaywallInfo] of the presented paywall.
     */
    public fun onPresent(block: (PaywallInfo) -> Unit) {
        onPresentHandler = block
    }

    /**
     * Sets the closure that's called when the paywall is dismissed.
     *
     * @param block Receives the [PaywallInfo] of the dismissed paywall and the
     * [PaywallResult] describing how it was dismissed.
     */
    public fun onDismiss(block: (PaywallInfo, PaywallResult) -> Unit) {
        onDismissHandler = block
    }

    /**
     * Sets the closure that's called when an error occurs while trying to
     * present the paywall.
     *
     * @param block Receives a description of the error.
     */
    public fun onError(block: (String) -> Unit) {
        onErrorHandler = block
    }

    /**
     * Sets the closure that's called when presentation of the paywall is skipped.
     *
     * @param block Receives the [PaywallSkippedReason] explaining why the
     * paywall was skipped.
     */
    public fun onSkip(block: (PaywallSkippedReason) -> Unit) {
        onSkipHandler = block
    }

    /**
     * Sets the suspend closure that's called when the paywall requests a
     * custom callback.
     *
     * Custom callbacks allow paywalls to request arbitrary actions from the app
     * and receive results that determine which branch (onSuccess/onFailure)
     * executes. This is the only value-returning callback; when unset, the SDK
     * responds with [CustomCallbackResult.failure].
     *
     * Example:
     * ```kotlin
     * handler.onCustomCallback { callback ->
     *     when (callback.name) {
     *         "validate_email" -> {
     *             val email = callback.variables?.get("email") as? String
     *             if (isValidEmail(email)) {
     *                 CustomCallbackResult.success(mapOf("validated" to true))
     *             } else {
     *                 CustomCallbackResult.failure(mapOf("error" to "Invalid email"))
     *             }
     *         }
     *         else -> CustomCallbackResult.failure()
     *     }
     * }
     * ```
     *
     * @param block Receives the [CustomCallback] and returns a [CustomCallbackResult].
     */
    public fun onCustomCallback(block: suspend (CustomCallback) -> CustomCallbackResult) {
        onCustomCallbackHandler = block
    }
}
