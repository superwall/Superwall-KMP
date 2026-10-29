package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.store.ProductReplacement

/**
 * The interface that handles Superwall's subscription-related logic.
 *
 * By default, the Superwall SDK handles all subscription-related logic. However,
 * if you'd like more control, you can pass a [PurchaseController] when configuring
 * the SDK via [Superwall.configure].
 *
 * One common interface covers both stores; each platform invokes only its own
 * store's method. When implementing this, you also need to set the subscription
 * status using `Superwall.subscriptionStatus`.
 *
 * To learn how to implement a `PurchaseController` in your app and best practices,
 * see [Purchases and Subscription Status](https://docs.superwall.com/docs/advanced-configuration).
 */
public interface PurchaseController {
    /**
     * Called when the user initiates purchasing of a product on iOS.
     *
     * Add your purchase logic here and return its result. You can use
     * platform-specific purchasing APIs. Make sure you handle all cases of
     * [PurchaseResult].
     *
     * @param productId The product identifier of the product the user would like to purchase.
     * @return The [PurchaseResult] of your purchase logic.
     */
    public suspend fun purchaseFromAppStore(productId: String): PurchaseResult

    /**
     * Called when the user initiates purchasing of a product on Android.
     *
     * Add your purchase logic here and return its result. You can use
     * platform-specific purchasing APIs. Make sure you handle all cases of
     * [PurchaseResult].
     *
     * @param productId The product identifier of the product the user would like to purchase.
     * @param basePlanId An optional base plan identifier of the product that's being purchased.
     * @param offerId An optional offer identifier of the product that's being purchased.
     * @return The [PurchaseResult] of your purchase logic.
     */
    public suspend fun purchaseFromGooglePlay(
        productId: String,
        basePlanId: String?,
        offerId: String?,
    ): PurchaseResult

    /**
     * Called on Android for every purchase, in place of the three-argument
     * [purchaseFromGooglePlay]. [replacement] is non-null only when the
     * paywall's Purchase action is set to replace an existing subscription and
     * the user has an active subscription that shares an entitlement with the
     * product. To perform the replacement, pass [ProductReplacement.oldPurchaseToken]
     * and [ProductReplacement.mode] to the billing flow's `SubscriptionUpdateParams`
     * (or your billing library's equivalent, such as RevenueCat's `oldProductId`
     * and `googleReplacementMode`).
     *
     * The default implementation ignores [replacement] and calls the
     * three-argument [purchaseFromGooglePlay], so existing implementations keep
     * working; the purchase is then a normal one bought alongside the old
     * subscription.
     *
     * @platform Android — never called on iOS.
     * @param productId The product identifier of the product the user would like to purchase.
     * @param basePlanId An optional base plan identifier of the product that's being purchased.
     * @param offerId An optional offer identifier of the product that's being purchased.
     * @param replacement The existing subscription this purchase replaces, if any.
     * @return The [PurchaseResult] of your purchase logic.
     */
    public suspend fun purchaseFromGooglePlay(
        productId: String,
        basePlanId: String?,
        offerId: String?,
        replacement: ProductReplacement?,
    ): PurchaseResult = purchaseFromGooglePlay(productId, basePlanId, offerId)

    /**
     * Called when the user initiates a restore.
     *
     * Add your restore logic here, making sure that the user's subscription status
     * is updated after restore, and return its result.
     *
     * @return A [RestorationResult] that's [RestorationResult.Restored] if the user's
     * purchases were restored or [RestorationResult.Failed] if they weren't.
     * Note: restored does not imply the user has an active subscription, it just
     * means the restore had no errors.
     */
    public suspend fun restorePurchases(): RestorationResult
}
