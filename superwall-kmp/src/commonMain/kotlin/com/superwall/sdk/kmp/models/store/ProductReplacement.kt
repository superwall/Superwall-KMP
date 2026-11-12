package com.superwall.sdk.kmp.models.store

/**
 * An existing subscription that a purchase replaces (an upgrade, downgrade or
 * plan switch), instead of the new product being bought alongside it.
 *
 * Passed to the replacement-aware
 * [com.superwall.sdk.kmp.PurchaseController.purchaseFromGooglePlay] overload
 * only when the paywall's Purchase action is set to replace a subscription and
 * the user has an active subscription that shares an entitlement with the
 * product being purchased.
 *
 * @platform Android — SuperwallKit iOS has no subscription replacement yet.
 * @property mode How the store should charge for the switch.
 * @property oldProductId The subscription ID of the product being replaced, if known.
 * @property oldPurchaseToken The Google Play purchase token of the subscription being
 * replaced, for `SubscriptionUpdateParams.oldPurchaseToken`. `null` for custom store products.
 */
public data class ProductReplacement(
    val mode: Mode,
    val oldProductId: String?,
    val oldPurchaseToken: String?,
) {
    /** How the store charges for a subscription replacement. */
    public enum class Mode {
        /** Let the store decide, using the replacement mode configured in Google Play Console. */
        DEFAULT,

        /**
         * Replace it now and charge the prorated price difference immediately. Google Play
         * only supports this for upgrades, where the price per time unit increases.
         */
        CHARGE_PRORATED,

        /** Replace it now and charge the full price of the new product immediately. */
        CHARGE_FULL_PRICE,
    }
}
