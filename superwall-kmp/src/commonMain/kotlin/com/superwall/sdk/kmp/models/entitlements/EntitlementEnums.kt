package com.superwall.sdk.kmp.models.entitlements

/**
 * The store from which a product, transaction, or entitlement originated.
 */
public enum class ProductStore {
    /** The Apple App Store. */
    APP_STORE,

    /** Stripe. */
    STRIPE,

    /** Paddle. */
    PADDLE,

    /** The Google Play Store. */
    PLAY_STORE,

    /** Superwall. */
    SUPERWALL,

    /** A custom store integration. */
    CUSTOM,

    /** Any other store. */
    OTHER,
}

/**
 * The type of an [Entitlement].
 */
public enum class EntitlementType {
    /** A service-level entitlement. */
    SERVICE_LEVEL,
}

/**
 * The state of the latest subscription transaction.
 */
public enum class LatestSubscriptionState {
    /** The subscription has expired but is still in its billing grace period. */
    IN_GRACE_PERIOD,

    /** The user is currently subscribed. */
    SUBSCRIBED,

    /** The subscription has expired. */
    EXPIRED,

    /** The subscription is in a billing retry period. */
    IN_BILLING_RETRY_PERIOD,

    /** The subscription has been revoked. */
    REVOKED,
}

/**
 * The type of offer that applies to a subscription transaction.
 */
public enum class LatestSubscriptionOfferType {
    /** An introductory free-trial offer. */
    TRIAL,

    /** An offer redeemed via an offer code. */
    CODE,

    /** A promotional offer. */
    PROMOTIONAL,

    /** A win-back offer. */
    WINBACK,
}
