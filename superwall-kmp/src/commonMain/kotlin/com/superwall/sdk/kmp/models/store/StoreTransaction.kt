package com.superwall.sdk.kmp.models.store

import kotlin.time.Instant

/**
 * A wrapper around a store transaction.
 */
public data class StoreTransaction(
    /** The ID of the config request associated with the transaction. */
    val configRequestId: String,
    /** The ID of the app session associated with the transaction. */
    val appSessionId: String,
    /** The identifier of the original transaction. */
    val originalTransactionIdentifier: String,
    /** The date the transaction occurred. */
    val transactionDate: Instant? = null,
    /** The store's identifier for the transaction. */
    val storeTransactionId: String? = null,
    /** The date of the original transaction. */
    val originalTransactionDate: Instant? = null,
    /** The web order line item identifier of the transaction (App Store). */
    val webOrderLineItemID: String? = null,
    /** The bundle identifier of the app the transaction belongs to. */
    val appBundleId: String? = null,
    /** The identifier of the subscription group the transaction's product belongs to. */
    val subscriptionGroupId: String? = null,
    /** Whether the user upgraded to another subscription. */
    val isUpgraded: Boolean? = null,
    /** The date the subscription expires or renews. */
    val expirationDate: Instant? = null,
    /** The identifier of the subscription offer applied to the transaction. */
    val offerId: String? = null,
    /** The date the transaction was revoked, if any. */
    val revocationDate: Instant? = null,
    /** The state of the transaction. */
    val state: StoreTransactionState? = null,
    /** The payment the transaction was made for. */
    val payment: StorePayment? = null,
    /**
     * The app account token (a UUID string) the purchase was made with, if
     * any. iOS only: superwall-android always leaves it `null`.
     */
    val appAccountToken: String? = null,
    /**
     * The Google Play purchase token, e.g. for `Superwall.consume`. Android
     * only; always `null` on iOS.
     */
    val purchaseToken: String? = null,
    /** The Google Play purchase signature. Android only; always `null` on iOS. */
    val signature: String? = null,
)

/** The state of a store transaction. */
public enum class StoreTransactionState {
    /** The purchase is in progress. */
    PURCHASING,

    /** The purchase succeeded. */
    PURCHASED,

    /** The purchase failed. */
    FAILED,

    /** A previous purchase was restored. */
    RESTORED,

    /** The purchase is waiting for an external action, such as a parent's approval. */
    DEFERRED,
}

/** The payment a store transaction was made for. */
public data class StorePayment(
    /** The identifier of the purchased product. */
    val productIdentifier: String,
    /** The quantity purchased. */
    val quantity: Int,
    /** The identifier of the discount (promotional offer) applied, if any. */
    val discountIdentifier: String? = null,
)
