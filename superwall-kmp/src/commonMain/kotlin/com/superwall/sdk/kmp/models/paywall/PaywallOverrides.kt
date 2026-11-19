package com.superwall.sdk.kmp.models.paywall

/**
 * Overrides for one paywall you retrieve to show yourself, passed to
 * `Superwall.getPaywall`.
 */
public data class PaywallOverrides(
    /**
     * Products to show instead of the paywall's own, as product identifiers
     * keyed by the product's name in the paywall editor (e.g. `primary`). The
     * SDK fetches them from the store; an identifier the store doesn't know
     * is left out.
     */
    val productIdsByName: Map<String, String> = emptyMap(),
    /**
     * Whether to retrieve the paywall even when the user has an active
     * subscription. Android only: SuperwallKit's overrides have no such
     * setting.
     */
    val ignoreSubscriptionStatus: Boolean = false,
)
