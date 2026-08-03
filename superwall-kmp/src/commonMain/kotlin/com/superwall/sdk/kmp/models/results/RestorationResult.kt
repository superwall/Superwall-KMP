package com.superwall.sdk.kmp.models.results

/**
 * The possible outcomes of attempting to restore purchases.
 *
 * When implementing the [com.superwall.sdk.kmp.PurchaseController.restorePurchases]
 * method, all cases should be considered.
 */
public sealed interface RestorationResult {
    /**
     * Purchases were restored successfully.
     */
    public data object Restored : RestorationResult

    /**
     * The restoration failed.
     *
     * @property error A message describing why the restoration failed.
     */
    public data class Failed(val error: String) : RestorationResult

    public companion object {
        /** Convenience factory for [Restored]. */
        public fun restored(): RestorationResult = Restored

        /** Convenience factory for [Failed] with the given [error] message. */
        public fun failed(error: String): RestorationResult = Failed(error)
    }
}
