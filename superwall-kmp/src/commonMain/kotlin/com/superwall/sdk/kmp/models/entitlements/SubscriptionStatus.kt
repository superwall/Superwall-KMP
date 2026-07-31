package com.superwall.sdk.kmp.models.entitlements

/**
 * The subscription status of the user.
 */
public sealed interface SubscriptionStatus {
    /**
     * The user has one or more active entitlements.
     *
     * @property entitlements The entitlements that are currently active.
     */
    public data class Active(val entitlements: Set<Entitlement>) : SubscriptionStatus

    /** The user does not have an active subscription. */
    public data object Inactive : SubscriptionStatus

    /** The subscription status is unknown, e.g. before the SDK has determined it. */
    public data object Unknown : SubscriptionStatus

    /** A convenience boolean indicating whether the subscription status is [Active]. */
    public val isActive: Boolean get() = this is Active
}
