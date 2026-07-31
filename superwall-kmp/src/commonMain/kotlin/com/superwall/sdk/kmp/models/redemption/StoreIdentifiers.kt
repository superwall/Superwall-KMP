package com.superwall.sdk.kmp.models.redemption

/**
 * Identifiers of the store that a redeemed purchase was made from.
 */
public sealed interface StoreIdentifiers {
    /**
     * Stripe purchase store identifiers.
     *
     * @property customerId The Stripe customer ID.
     * @property subscriptionIds The Stripe subscription IDs.
     */
    public data class Stripe(
        val customerId: String,
        val subscriptionIds: List<String>,
    ) : StoreIdentifiers

    /**
     * Paddle purchase store identifiers.
     *
     * @property customerId The Paddle customer ID.
     * @property subscriptionIds The Paddle subscription IDs.
     */
    public data class Paddle(
        val customerId: String,
        val subscriptionIds: List<String>,
    ) : StoreIdentifiers

    /**
     * Purchase store identifiers from an unrecognized store.
     *
     * @property store The raw name of the store.
     * @property additionalInfo Any additional identifying info supplied by the store.
     */
    public data class Unknown(
        val store: String,
        val additionalInfo: Map<String, Any?>,
    ) : StoreIdentifiers
}
