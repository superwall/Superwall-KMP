package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.paywall.PaywallInfo

/**
 * A paywall retrieved with [Superwall.getPaywall] for you to show inside your
 * own UI, instead of having Superwall present it.
 *
 * Common code can hold, pass around and [destroy] it. The thing to put on
 * screen is a platform object, so it is read through one accessor declared in
 * each platform source set: `EmbeddedPaywall.view` (an `android.view.View`)
 * on Android and `EmbeddedPaywall.viewController` (a `UIViewController`) on
 * iOS. Use it on the main thread.
 */
public interface EmbeddedPaywall {
    /** The placement the paywall was retrieved for. */
    public val placement: String

    /** Info about the paywall, as it is now. */
    public val info: PaywallInfo

    /**
     * Ends the presentation and releases the paywall. Call it when you are
     * done with it: after [PaywallViewCallback.onFinished] with
     * `shouldDismiss`, or when your screen goes away. Safe to call twice.
     *
     * On Android this is required, and taking the view out of your layout is
     * not enough: superwall-android caches paywalls and has to be told the
     * presentation is over before the same paywall can be shown again. On iOS
     * it lets go of the view controller; taking that off screen stays with
     * whatever put it there.
     */
    public fun destroy()
}
