package com.superwall.sdk.kmp.models.options

/**
 * Defines the messaging of the alert presented to the user when restoring a
 * transaction fails.
 *
 * On Android, a field left at its default is shown in the device's language
 * (superwall-android localizes it); on iOS the defaults are shown as written.
 */
public data class RestoreFailed(
    /**
     * The title of the alert presented to the user when restoring a
     * transaction fails.
     */
    val title: String = "No Subscription Found",
    /**
     * The message of the alert presented to the user when restoring a
     * transaction fails.
     */
    val message: String = "We couldn't find an active subscription for your account.",
    /** The title of the close button in the alert presented to the user. */
    val closeButtonTitle: String = "Okay",
)
