package com.superwall.sdk.kmp.models.results

import com.superwall.sdk.kmp.models.triggers.Experiment

/**
 * The result of a paywall trigger.
 *
 * Triggers can conditionally show paywalls. Contains the possible cases
 * resulting from the trigger.
 */
public sealed interface TriggerResult {
    /**
     * This placement was not found on the dashboard.
     *
     * Please make sure you have added the placement to a campaign on the
     * dashboard and double check its spelling.
     */
    public data object PlacementNotFound : TriggerResult

    /**
     * No audience filter was matched for this placement.
     */
    public data object NoAudienceMatch : TriggerResult

    /**
     * A matching audience filter was found and this placement will show a paywall.
     *
     * @property experiment The experiment associated with the trigger.
     */
    public data class Paywall(val experiment: Experiment) : TriggerResult

    /**
     * A matching audience filter was found and this placement was assigned to a holdout
     * group, so no paywall will be shown.
     *
     * @property experiment The experiment associated with the trigger.
     */
    public data class Holdout(val experiment: Experiment) : TriggerResult

    /**
     * An error occurred and the user will not be shown a paywall.
     *
     * @property error A message describing the error that occurred.
     */
    public data class Error(val error: String) : TriggerResult
}
