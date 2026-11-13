package com.superwall.sdk.kmp.models.store

/**
 * The existing Google Play subscription a purchase replaces (an upgrade,
 * downgrade or switch), instead of the new product being bought alongside it.
 *
 * Passed to `PurchaseController.purchaseFromGooglePlayWithReplacement` when a
 * paywall's Purchase action is set to replace the user's subscription and the
 * user has an active subscription sharing an entitlement with the new product.
 * Android only.
 */
public data class ProductReplacement(
    /** How Google Play should charge for the switch. */
    val mode: ProductReplacementMode,
    /** The subscription ID of the product being replaced, if known. */
    val oldProductId: String?,
    /**
     * The Google Play purchase token of the subscription being replaced, for
     * `SubscriptionUpdateParams.oldPurchaseToken`. `null` for custom store
     * products.
     */
    val oldPurchaseToken: String?,
)

/** How the store charges when a purchase replaces a subscription. */
public enum class ProductReplacementMode {
    /** Let the store decide, using the replacement mode set in the Google Play Console. */
    STORE_DEFAULT,

    /**
     * Replace it now and charge the prorated price difference immediately.
     * Google Play only supports this for upgrades.
     */
    CHARGE_PRORATED,

    /** Replace it now and charge the full price of the new product immediately. */
    CHARGE_FULL_PRICE,
}
