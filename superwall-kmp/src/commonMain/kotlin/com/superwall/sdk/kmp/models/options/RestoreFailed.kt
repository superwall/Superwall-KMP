package com.superwall.sdk.kmp.models.options

/**
 * Defines the messaging of the alert presented to the user when restoring a
 * transaction fails.
 *
 * On Android, a text equal to its English default below is shown in the
 * device's language instead; any other value is shown as set. The common
 * defaults can't tell "left unset" from "set to the default", so an explicit
 * English default is localized too.
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
