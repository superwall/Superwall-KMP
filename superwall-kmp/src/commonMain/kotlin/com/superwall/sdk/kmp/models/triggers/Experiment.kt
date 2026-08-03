package com.superwall.sdk.kmp.models.triggers

/**
 * A campaign experiment that was assigned to a user.
 *
 * An experiment is part of a [Campaign Rule](https://docs.superwall.com/docs/campaign-rules)
 * defined in the Superwall dashboard. When a rule is matched, the user is assigned to an
 * experiment, which is a set of paywall variants determined by probabilities. An experiment will
 * result in a user seeing a paywall unless they are in a holdout group.
 *
 * To learn more, read [our docs](https://docs.superwall.com/docs/home#how-it-works).
 *
 * @property id The id of the experiment.
 * @property groupId The id of the experiment group.
 * @property variant The variant of the experiment assigned to the user.
 */
public data class Experiment(
    val id: String,
    val groupId: String,
    val variant: Variant,
)
