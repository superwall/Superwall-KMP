package com.superwall.sdk.kmp.models.entitlements

import kotlin.time.Instant

/**
 * Contains the latest subscription and entitlement info about the customer.
 *
 * @property subscriptions The subscription transactions the user has made.
 * @property nonSubscriptions The non-subscription transactions the user has made.
 * @property entitlements All entitlements available to the user.
 * @property userId The ID of the user.
 */
public data class CustomerInfo(
    val subscriptions: List<SubscriptionTransaction>,
    val nonSubscriptions: List<NonSubscriptionTransaction>,
    val entitlements: List<Entitlement>,
    val userId: String,
)

/**
 * A subscription transaction.
 *
 * @property transactionId The unique identifier for the transaction.
 * @property productId The product identifier of the subscription.
 * @property purchaseDate The date that the store charged the user's account.
 * @property willRenew Indicates whether the subscription will renew.
 * @property isRevoked Indicates whether the transaction has been revoked.
 * @property isInGracePeriod Indicates whether the subscription is in a billing grace period state.
 * @property isInBillingRetryPeriod Indicates whether the subscription is in a billing retry
 * period state.
 * @property isActive Indicates whether the subscription is active.
 * @property expirationDate The date that the subscription expires, or `null` if non-renewing.
 * @property offerType The type of offer that applies to the subscription transaction.
 * @property subscriptionGroupId The subscription group identifier.
 * @property store The store from which this transaction originated.
 */
public data class SubscriptionTransaction(
    val transactionId: String,
    val productId: String,
    val purchaseDate: Instant,
    val willRenew: Boolean,
    val isRevoked: Boolean,
    val isInGracePeriod: Boolean,
    val isInBillingRetryPeriod: Boolean,
    val isActive: Boolean,
    val expirationDate: Instant? = null,
    val offerType: LatestSubscriptionOfferType? = null,
    val subscriptionGroupId: String? = null,
    val store: ProductStore? = null,
)

/**
 * A non-subscription transaction (consumable or non-consumable).
 *
 * @property transactionId The unique identifier for the transaction.
 * @property productId The product identifier of the in-app purchase.
 * @property purchaseDate The date that the store charged the user's account.
 * @property isConsumable Indicates whether it's a consumable in-app purchase.
 * @property isRevoked Indicates whether the transaction has been revoked.
 * @property store The store from which this transaction originated.
 */
public data class NonSubscriptionTransaction(
    val transactionId: String,
    val productId: String,
    val purchaseDate: Instant,
    val isConsumable: Boolean,
    val isRevoked: Boolean,
    val store: ProductStore? = null,
)
