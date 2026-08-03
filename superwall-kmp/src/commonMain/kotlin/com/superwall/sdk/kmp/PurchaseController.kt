package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult

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
