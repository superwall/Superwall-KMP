package com.superwall.sdk.kmp.models.options

import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import kotlin.time.Duration

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
     * The view that appears behind the store's payment sheet during a
     * transaction. Defaults to [TransactionBackgroundView.SPINNER]. Applies on
     * both platforms: on Android it is the spinner shown over the paywall
     * while a purchase or restore is in progress.
     */
    val transactionBackgroundView: TransactionBackgroundView = TransactionBackgroundView.SPINNER,
    /**
     * Shows an alert asking the user if they'd like to try to restore on the
     * web, if you have added web checkout on the Superwall dashboard.
     * Defaults to `true`. iOS only; has no effect on Android.
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
     * checkout. Defaults to `true`. iOS only; has no effect on Android.
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
    /**
     * The alert shown when a paywall asks for notification permissions the
     * user has denied. `null` (the default) shows no alert. iOS only; has no
     * effect on Android.
     */
    val notificationPermissionsDenied: NotificationPermissionsDenied? = null,
    /**
     * Hides the paywall's loading shimmer optimistically. Defaults to
     * `false`. Android only; has no effect on iOS.
     */
    val optimisticLoading: Boolean = false,
    /**
     * How long a presented paywall may take to finish loading before it's
     * given up on: it's then dismissed as declined with the
     * `PaywallCloseReason.WEB_VIEW_FAILED_TO_LOAD` close reason, followed by
     * the feature block for a non-gated placement or `onError` for a gated
     * one. `null` (the default) means no timeout. Android only; has no effect
     * on iOS.
     */
    val timeoutAfter: Duration? = null,
    /**
     * **Experimental.** Where presented paywalls are hosted. Defaults to
     * [PresentationHost.ACTIVITY]. Android only; has no effect on iOS.
     */
    val presentationHost: PresentationHost = PresentationHost.ACTIVITY,
)

/**
 * The alert shown when a paywall asks for notification permissions the user
 * has denied. iOS only.
 */
public data class NotificationPermissionsDenied(
    val title: String = "Notification Permissions Denied",
    val message: String =
        "Please enable notification permissions from the Settings app so we can notify you when your free trial ends.",
    /** The title of the button that opens the Settings app. */
    val actionButtonTitle: String = "Open Settings",
    val closeButtonTitle: String = "Not now",
)

/** Where a presented paywall is hosted on Android. */
public enum class PresentationHost {
    /** In its own activity. */
    ACTIVITY,

    /**
     * **Experimental.** As a view on top of the presenting activity's window,
     * so that activity is never covered or stopped, e.g. for game engines
     * whose loop stops while another activity covers theirs. If the activity
     * is destroyed while the paywall is shown, including by a configuration
     * change it doesn't handle itself, the paywall is dismissed.
     */
    IN_WINDOW,
}

/**
 * Defines the different types of views that can appear behind the store's
 * payment sheet during a transaction.
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
