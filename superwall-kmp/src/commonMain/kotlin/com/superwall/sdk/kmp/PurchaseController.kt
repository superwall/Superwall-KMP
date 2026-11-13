package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.store.ProductReplacement
import com.superwall.sdk.kmp.models.store.StoreProduct

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
 * The SDK calls the overloads that take a [StoreProduct]. Their defaults
 * forward to the product-id methods, so a controller that only buys store
 * products needs nothing more. Override them to tell a custom store product
 * (one you sell through your own checkout) from a store product, and to read
 * its [StoreProduct.customTransactionId].
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
     * What the SDK calls on iOS when the user initiates a purchase, with the
     * whole [product] rather than only its identifier.
     *
     * The default implementation calls [purchaseFromAppStore] with the product
     * identifier. Override it to handle custom store products: for those,
     * [StoreProduct.isCustomProduct] is `true` and
     * [StoreProduct.customTransactionId] is the transaction id the SDK
     * generated for this purchase. Pass that id to your checkout and have your
     * server report it back to Superwall as `custom_transaction_id`, so the
     * purchase and its renewals are attributed to the paywall that sold it.
     *
     * @param product The product the user would like to purchase.
     * @return The [PurchaseResult] of your purchase logic.
     */
    public suspend fun purchaseFromAppStore(product: StoreProduct): PurchaseResult =
        purchaseFromAppStore(product.productIdentifier)

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
     * Called on Android instead of [purchaseFromGooglePlay] when the purchase
     * replaces the user's existing subscription (an upgrade, downgrade or
     * switch), with the [replacement] to pass to Google Play Billing as
     * `SubscriptionUpdateParams`.
     *
     * The default implementation ignores [replacement] and calls
     * [purchaseFromGooglePlay], so the new product is bought alongside the old
     * subscription; override it to perform the replacement.
     *
     * @param productId The product identifier of the product the user would like to purchase.
     * @param basePlanId An optional base plan identifier of the product that's being purchased.
     * @param offerId An optional offer identifier of the product that's being purchased.
     * @param replacement The subscription being replaced and how to charge for it.
     * @return The [PurchaseResult] of your purchase logic.
     */
    public suspend fun purchaseFromGooglePlayWithReplacement(
        productId: String,
        basePlanId: String?,
        offerId: String?,
        replacement: ProductReplacement,
    ): PurchaseResult = purchaseFromGooglePlay(productId, basePlanId, offerId)

    /**
     * What the SDK calls on Android when the user initiates a purchase, with
     * the whole [product] rather than only its identifier.
     *
     * The default implementation calls
     * [purchaseFromGooglePlayWithReplacement] when there is a [replacement]
     * and [purchaseFromGooglePlay] otherwise, with the product identifier.
     * Override it to handle custom store products, which are not Play
     * products: for those, [StoreProduct.isCustomProduct] is `true` and
     * [StoreProduct.customTransactionId] is the transaction id the SDK
     * generated for this purchase (see [purchaseFromAppStore]).
     *
     * @param product The product the user would like to purchase.
     * @param basePlanId An optional base plan identifier of the product that's being purchased.
     * @param offerId An optional offer identifier of the product that's being purchased.
     * @param replacement The subscription being replaced and how to charge
     * for it, or `null` for a purchase that replaces nothing.
     * @return The [PurchaseResult] of your purchase logic.
     */
    public suspend fun purchaseFromGooglePlay(
        product: StoreProduct,
        basePlanId: String?,
        offerId: String?,
        replacement: ProductReplacement?,
    ): PurchaseResult =
        if (replacement != null) {
            purchaseFromGooglePlayWithReplacement(product.productIdentifier, basePlanId, offerId, replacement)
        } else {
            purchaseFromGooglePlay(product.productIdentifier, basePlanId, offerId)
        }

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
