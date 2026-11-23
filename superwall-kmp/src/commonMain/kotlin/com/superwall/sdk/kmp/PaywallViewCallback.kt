package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.callbacks.CustomCallback
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResult
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason

/**
 * Receives the events of a paywall you show yourself, inside your own UI,
 * instead of letting Superwall present it.
 *
 * Passed to [Superwall.getPaywall], which returns the [EmbeddedPaywall].
 * Every method is called on the main thread.
 */
public interface PaywallViewCallback {
    /**
     * The user finished interacting with the paywall: they purchased,
     * restored or declined.
     *
     * Always check [shouldDismiss]. When it is `true`, remove the paywall from
     * your UI and call [EmbeddedPaywall.destroy].
     *
     * @param paywallInfo Info about the paywall.
     * @param result How the paywall finished.
     * @param shouldDismiss Whether you should remove the paywall now.
     */
    public fun onFinished(
        paywallInfo: PaywallInfo,
        result: PaywallResult,
        shouldDismiss: Boolean,
    )

    /**
     * The paywall is on screen. Android only, where it is called once the
     * paywall's view is attached to a window. On iOS you put the view
     * controller on screen yourself, and SuperwallKit has no such moment to
     * report.
     */
    public fun onPresent(paywallInfo: PaywallInfo) {}

    /**
     * No paywall was retrieved because its presentation should be skipped,
     * e.g. the user is in a holdout. [Superwall.getPaywall] then returns
     * `null`.
     */
    public fun onSkip(reason: PaywallSkippedReason) {}

    /**
     * No paywall was retrieved because of an error. [Superwall.getPaywall]
     * then returns `null`.
     */
    public fun onError(error: String) {}

    /**
     * The paywall requested a custom callback. iOS only: superwall-android
     * has no custom callbacks for paywalls retrieved this way. The default
     * reports a failure to the paywall.
     */
    public suspend fun onCustomCallback(callback: CustomCallback): CustomCallbackResult = CustomCallbackResult.failure()
}
