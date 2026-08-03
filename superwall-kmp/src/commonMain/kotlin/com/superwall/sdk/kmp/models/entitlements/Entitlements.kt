package com.superwall.sdk.kmp.models.entitlements

/**
 * An immutable snapshot of the entitlements available to the user.
 *
 * This is a pure value type with structural equality — it never carries a live SDK reference.
 * To filter entitlements by product ID via the native SDK, use
 * `Superwall.getEntitlementsByProductIds(productIds)`.
 *
 * @property active The entitlements that are currently active for the user.
 * @property inactive The entitlements that are inactive for the user.
 * @property all Every entitlement, regardless of whether it is active.
 * @property web The entitlements granted via web checkout.
 */
public data class Entitlements(
    val active: Set<Entitlement>,
    val inactive: Set<Entitlement>,
    val all: Set<Entitlement>,
    val web: Set<Entitlement>,
)
