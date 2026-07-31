package com.superwall.sdk.kmp.models.results

import com.superwall.sdk.kmp.models.triggers.Experiment

/**
 * The status of a paywall presentation request.
 */
public enum class PaywallPresentationRequestStatusType {
    /** The paywall will be presented. */
    PRESENTATION,

    /** The paywall will not be presented. */
    NO_PRESENTATION,

    /** The request timed out. */
    TIMEOUT,
}

/**
 * The reason why a paywall couldn't be presented.
 */
public sealed interface PaywallPresentationRequestStatusReason {
    /**
     * The debugger is currently presented, blocking paywall presentation.
     */
    public data object DebuggerPresented : PaywallPresentationRequestStatusReason

    /**
     * A paywall is already being presented.
     */
    public data object PaywallAlreadyPresented : PaywallPresentationRequestStatusReason

    /**
     * The user was assigned to a holdout group, so no paywall is presented.
     *
     * @property experiment The experiment the user was assigned to.
     */
    public data class Holdout(val experiment: Experiment) : PaywallPresentationRequestStatusReason

    /**
     * No audience filter was matched for this placement.
     */
    public data object NoAudienceMatch : PaywallPresentationRequestStatusReason

    /**
     * The placement was not found on the dashboard.
     */
    public data object PlacementNotFound : PaywallPresentationRequestStatusReason

    /**
     * No paywall view controller could be created for presentation.
     */
    public data object NoPaywallViewController : PaywallPresentationRequestStatusReason

    /**
     * There was no presenter available to present the paywall from.
     */
    public data object NoPresenter : PaywallPresentationRequestStatusReason

    /**
     * The Superwall configuration was not available.
     */
    public data object NoConfig : PaywallPresentationRequestStatusReason

    /**
     * The subscription status timed out while being determined.
     */
    public data object SubscriptionStatusTimeout : PaywallPresentationRequestStatusReason
}
