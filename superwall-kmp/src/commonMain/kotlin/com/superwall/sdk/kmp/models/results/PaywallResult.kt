package com.superwall.sdk.kmp.models.results

/**
 * The result of the user interacting with a presented paywall.
 */
public sealed interface PaywallResult {
    /**
     * The user purchased a product from the paywall.
     *
     * @property productId The identifier of the purchased product.
     */
    public data class Purchased(val productId: String) : PaywallResult

    /**
     * The user declined the paywall without purchasing.
     */
    public data object Declined : PaywallResult

    /**
     * The user restored their purchases from the paywall.
     */
    public data object Restored : PaywallResult
}
