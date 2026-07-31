package com.superwall.sdk.kmp.models.paywall

/**
 * Indicates whether the `Superwall.register` `feature` block executes or not.
 */
public enum class FeatureGatingBehavior {
    /** The feature block executes only if the user has an active subscription or purchases. */
    GATED,

    /** The feature block always executes when the paywall is dismissed. */
    NON_GATED,
}

/**
 * Indicates whether the paywall was closed by user interaction or because
 * another paywall will show.
 *
 * Note for Swift readers: the native SDK's `.none` case maps to [NONE] here.
 */
public enum class PaywallCloseReason {
    /**
     * The paywall was closed by system logic, either after a purchase, because
     * a deep link was presented, the close button was pressed, etc.
     */
    SYSTEM_LOGIC,

    /**
     * The paywall was automatically closed because another paywall will show.
     *
     * This prevents the `Superwall.register` `feature` block from executing on
     * dismiss of the paywall, because another paywall is set to show.
     */
    FOR_NEXT_PAYWALL,

    /**
     * The paywall was closed because the webview couldn't be loaded.
     *
     * If this happens for a gated paywall, the [com.superwall.sdk.kmp.PaywallPresentationHandler]
     * `onError` handler will be called. If it's for a non-gated paywall, the
     * feature block will be called.
     */
    WEB_VIEW_FAILED_TO_LOAD,

    /** The paywall was closed because the user tapped the close button or dragged to dismiss. */
    MANUAL_CLOSE,

    /** The paywall hasn't been closed yet. */
    NONE,
}
