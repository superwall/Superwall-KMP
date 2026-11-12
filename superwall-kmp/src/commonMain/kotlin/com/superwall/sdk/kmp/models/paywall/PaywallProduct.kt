package com.superwall.sdk.kmp.models.paywall

import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.ProductStore

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
    /**
     * The store the product is sold through, e.g. [ProductStore.STRIPE] for
     * a web checkout product or [ProductStore.CUSTOM] for one bought through
     * your own checkout.
     */
    val store: ProductStore? = null,
)
