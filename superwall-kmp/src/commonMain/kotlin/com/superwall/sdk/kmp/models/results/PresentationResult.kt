package com.superwall.sdk.kmp.models.results

import com.superwall.sdk.kmp.models.triggers.Experiment

/**
 * The result of presenting a paywall.
 *
 * Contains the possible cases resulting from a paywall presentation attempt.
 */
public sealed interface PresentationResult {
    /**
     * The placement was not found on the dashboard.
     */
    public data object PlacementNotFound : PresentationResult

    /**
     * No audience filter was matched for this placement.
     */
    public data object NoAudienceMatch : PresentationResult

    /**
     * A paywall will be presented.
     *
     * @property experiment The experiment associated with the presentation.
     */
    public data class Paywall(val experiment: Experiment) : PresentationResult

    /**
     * The user was assigned to a holdout group, so no paywall will be presented.
     *
     * @property experiment The experiment associated with the holdout.
     */
    public data class Holdout(val experiment: Experiment) : PresentationResult

    /**
     * The paywall was not available for presentation.
     */
    public data object PaywallNotAvailable : PresentationResult
}
