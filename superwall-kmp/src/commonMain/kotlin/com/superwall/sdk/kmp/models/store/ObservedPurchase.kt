package com.superwall.sdk.kmp.models.store

/**
 * A Google Play purchase made by your own billing code, reported to
 * `Superwall.observePurchaseResult`: what Play Billing's `Purchase` has as
 * `originalJson` and `signature`.
 */
public data class ObservedPurchase(
    /** The purchase's original JSON, as Google Play returned it. */
    val originalJson: String,
    /** The signature of [originalJson]. */
    val signature: String,
)
