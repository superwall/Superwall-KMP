package com.superwall.sdk.kmp.models.triggers

/**
 * A variant of an experiment assigned to a user.
 *
 * @property id The id of the experiment variant.
 * @property type The type of variant: treatment or holdout.
 * @property paywallId The identifier of the paywall variant. Only valid when the variant [type]
 * is [VariantType.TREATMENT].
 */
public data class Variant(
    val id: String,
    val type: VariantType,
    val paywallId: String? = null,
)

/**
 * The type of an experiment [Variant].
 */
public enum class VariantType {
    /** The user sees a paywall. */
    TREATMENT,

    /** The user is in a holdout group and does not see a paywall. */
    HOLDOUT,
}
