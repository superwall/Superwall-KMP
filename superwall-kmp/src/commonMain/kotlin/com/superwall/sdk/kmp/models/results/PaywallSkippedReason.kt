package com.superwall.sdk.kmp.models.results

import com.superwall.sdk.kmp.models.triggers.Experiment

/**
 * The reason the paywall presentation was skipped.
 */
public sealed interface PaywallSkippedReason {
    /**
     * The user was assigned to a holdout.
     *
     * A holdout is a control group which you can analyse against, whose members
     * don't receive any paywall when they match an audience filter.
     *
     * It's useful for testing a paywall's inclusion vs its exclusion.
     *
     * @property experiment The experiment the user was assigned to.
     */
    public data class Holdout(val experiment: Experiment) : PaywallSkippedReason

    /**
     * No audience filter was matched for this placement.
     */
    public data object NoAudienceMatch : PaywallSkippedReason

    /**
     * This placement was not found on the dashboard.
     *
     * Please make sure you have added the placement to a campaign on the
     * dashboard and double check its spelling.
     */
    public data object PlacementNotFound : PaywallSkippedReason
}
