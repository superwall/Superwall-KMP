package com.superwall.sdk.kmp.models.results

/**
 * The possible outcomes of attempting to purchase a product.
 *
 * When implementing the [com.superwall.sdk.kmp.PurchaseController] purchase methods,
 * all cases should be considered.
 */
public sealed interface PurchaseResult {
    /**
     * The product was purchased.
     */
    public data object Purchased : PurchaseResult

    /**
     * The purchase was cancelled.
     *
     * This is equivalent to various cancellation cases in StoreKit 1 and `.userCancelled`
     * in StoreKit 2. In the context of RevenueCat, this is when the `userCancelled`
     * boolean returns `true` from the purchase method.
     */
    public data object Cancelled : PurchaseResult

    /**
     * The purchase is pending and requires action from the developer.
     *
     * This corresponds to the `.deferred` transaction state in StoreKit 1
     * and `.paymentPendingError` in RevenueCat.
     */
    public data object Pending : PurchaseResult

    /**
     * The purchase failed for a reason other than the user cancelling or the payment pending.
     *
     * @property error A message describing the failure, sent back for further handling.
     */
    public data class Failed(val error: String) : PurchaseResult

    public companion object {
        /** Convenience factory for [Purchased]. */
        public fun purchased(): PurchaseResult = Purchased

        /** Convenience factory for [Cancelled]. */
        public fun cancelled(): PurchaseResult = Cancelled

        /** Convenience factory for [Pending]. */
        public fun pending(): PurchaseResult = Pending

        /** Convenience factory for [Failed] with the given [error] message. */
        public fun failed(error: String): PurchaseResult = Failed(error)
    }
}
