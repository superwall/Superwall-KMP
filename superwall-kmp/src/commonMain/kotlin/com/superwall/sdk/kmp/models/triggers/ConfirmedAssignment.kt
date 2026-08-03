package com.superwall.sdk.kmp.models.triggers

/**
 * A confirmed assignment of a user to an experiment variant.
 *
 * @property experimentId The id of the experiment the user was assigned to.
 * @property variant The variant of the experiment the user was assigned to.
 */
public data class ConfirmedAssignment(
    val experimentId: String,
    val variant: Variant,
)
