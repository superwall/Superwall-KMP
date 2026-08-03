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
)
