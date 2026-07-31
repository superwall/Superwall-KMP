package com.superwall.sdk.kmp.models.results

import com.superwall.sdk.kmp.models.store.StoreTransaction

/**
 * Describes the type of restore that occurred.
 */
public sealed interface RestoreType {
    /**
     * The restore happened as a side effect of a purchase.
     *
     * @property storeTransaction The transaction associated with the purchase, if available.
     */
    public data class ViaPurchase(val storeTransaction: StoreTransaction?) : RestoreType

    /**
     * The restore happened via an explicit restore request.
     */
    public data object ViaRestore : RestoreType
}
