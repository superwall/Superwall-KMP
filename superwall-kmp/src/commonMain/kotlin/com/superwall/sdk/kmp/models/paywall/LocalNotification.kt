package com.superwall.sdk.kmp.models.paywall

/**
 * A local notification scheduled by a paywall.
 */
public data class LocalNotification(
    /** The identifier of the notification. */
    val id: String,
    /** The type of the notification. */
    val type: LocalNotificationType,
    /** The title text of the notification. */
    val title: String,
    /** The body text of the notification. */
    val body: String,
    /** The delay until the notification fires, in milliseconds. */
    val delay: Long,
    /** The subtitle text of the notification. */
    val subtitle: String? = null,
)

/**
 * The type of a [LocalNotification].
 */
public enum class LocalNotificationType {
    /** The notification is scheduled when a free trial starts. */
    TRIAL_STARTED,

    /** The notification type isn't recognized by this version of the SDK. */
    UNSUPPORTED,
}
