package com.superwall.sdk.kmp.models.paywall

import com.superwall.sdk.kmp.models.entitlements.Entitlement

/**
 * A product in the paywall.
 */
public data class PaywallProduct(
    /** The product's identifier. */
    val id: String? = null,
    /** The name of the product in the editor. */
    val name: String? = null,
    /** The entitlements associated with this product. */
    val entitlements: Set<Entitlement> = emptySet(),
)
