package com.superwall.sdk.kmp.models.options

import com.superwall.sdk.kmp.models.paywall.PaywallInfo

/**
 * Options for configuring the appearance and behavior of paywalls.
 */
public data class PaywallOptions(
    /**
     * Determines whether the paywall should use haptic feedback.
     * Defaults to `true`.
     */
    val isHapticFeedbackEnabled: Boolean = true,
    /**
     * Defines the messaging of the alert presented to the user when restoring
     * a transaction fails.
     */
    val restoreFailed: RestoreFailed = RestoreFailed(),
    /** Shows an alert after a purchase fails. Defaults to `true`. */
    val shouldShowPurchaseFailureAlert: Boolean = true,
    /**
     * Pre-loads and caches trigger paywalls and products when you initialize
     * the SDK. Defaults to `true`.
     */
    val shouldPreload: Boolean = true,
    /**
     * Per-device-tier overrides for [shouldPreload]. Only the tiers you
     * specify are overridden; the rest fall back to [shouldPreload].
     *
     * Use this to disable preloading on low-end devices while keeping it
     * enabled on mid/high-end devices, for example.
     *
     * Note: Android only. Has no effect on iOS.
     */
    val preloadDeviceOverrides: Map<DeviceTier, Boolean> = emptyMap(),
    /**
     * Automatically dismisses the paywall when a product is purchased or
     * restored. Defaults to `true`.
     */
    val automaticallyDismiss: Boolean = true,
    /**
     * The view that appears behind Apple's payment sheet during a transaction.
     * Defaults to [TransactionBackgroundView.SPINNER]. iOS only.
     */
    val transactionBackgroundView: TransactionBackgroundView = TransactionBackgroundView.SPINNER,
    /**
     * Shows an alert asking the user if they'd like to try to restore on the
     * web, if you have added web checkout on the Superwall dashboard.
     * Defaults to `true`.
     */
    val shouldShowWebRestorationAlert: Boolean = true,
    /**
     * Allows you to globally override products on any paywall that have a
     * given name. The key is the product name in the paywall, the value is
     * the product identifier to replace it with.
     */
    val overrideProductsByName: Map<String, String>? = null,
    /**
     * Shows a localized alert confirming a successful purchase via web
     * checkout. Defaults to `true`.
     */
    val shouldShowWebPurchaseConfirmationAlert: Boolean = true,
    /**
     * A callback that is invoked when the back button is pressed while a
     * paywall is showing. Receives the currently presented paywall's
     * [PaywallInfo], if available.
     *
     * Return `true` to indicate you handled the back press yourself (the SDK
     * takes no further action); return `false` to let the SDK's default back
     * behavior run. Call `Superwall.dismiss()` within the callback if you
     * want to dismiss the paywall.
     *
     * Note: Android only. Has no effect on iOS.
     */
    val onBackPressed: ((PaywallInfo?) -> Boolean)? = null,
)

/**
 * Defines the different types of views that can appear behind Apple's payment
 * sheet during a transaction. iOS only.
 */
public enum class TransactionBackgroundView {
    /** Shows a spinner behind the payment sheet. */
    SPINNER,

    /**
     * Shows nothing behind the payment sheet.
     *
     * Note: the corresponding Swift case is `.none` — take care when reading
     * Swift documentation, as it can be confused with an absent optional.
     */
    NONE,
}
