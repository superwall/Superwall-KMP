package com.superwall.sdk.kmp.models.paywall

import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.ProductStore
import com.superwall.sdk.kmp.models.store.BillingPlanType

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
    /**
     * The billing plan configured for an App Store product, if any. iOS only;
     * `null` on Android.
     */
    val billingPlanType: BillingPlanType? = null,
    /** The trial length in days configured for a Stripe product, if any. */
    val trialDays: Int? = null,
    /** The base plan of a Play Store product. Android only; `null` on iOS. */
    val basePlanIdentifier: String? = null,
    /** The offer chosen for a Play Store product. Android only; `null` on iOS. */
    val offer: PaywallProductOffer? = null,
    /**
     * The identifier with its base plan and offer, `productId:basePlanId:offerId`,
     * as set in the Superwall dashboard. Android only; `null` on iOS.
     */
    val fullIdentifier: String? = null,
)

/** Which offer of a Play Store product a paywall sells. */
public sealed interface PaywallProductOffer {
    /** The SDK picks the best offer, e.g. the longest free trial. */
    public data object Automatic : PaywallProductOffer

    /** No offer: the base plan's own price. */
    public data object NoOffer : PaywallProductOffer

    /** The offer with this identifier. */
    public data class Specified(val offerIdentifier: String) : PaywallProductOffer
}
