package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionState

/**
 * Merges entitlements by id using priority-based deduplication: when multiple
 * entitlements share the same id, the one with the highest priority (per
 * [prioritized]) is kept.
 *
 * Pure common logic, ported one-to-one from the Flutter SDK's
 * `Entitlement.mergePrioritized`.
 */
internal fun mergePrioritized(entitlements: Set<Entitlement>): Set<Entitlement> {
    val byId = LinkedHashMap<String, Entitlement>()
    for (entitlement in entitlements) {
        val existing = byId[entitlement.id]
        byId[entitlement.id] =
            if (existing != null) entitlement.prioritized(comparing = existing) else entitlement
    }
    return byId.values.toSet()
}

/**
 * Compares this entitlement with [comparing] and returns the one with higher
 * priority.
 *
 * Priority rules, in order:
 * 1. Active entitlements win over inactive.
 * 2. Entitlements with transaction history (a `latestProductId`) win.
 * 3. Lifetime entitlements win.
 * 4. Non-revoked entitlements win.
 * 5. Latest renewal date wins.
 * 6. Latest expiration date wins.
 *
 * Defaults to `this` when no rule discriminates.
 */
internal fun Entitlement.prioritized(comparing: Entitlement): Entitlement {
    // 1. Active wins over inactive.
    if (isActive != comparing.isActive) {
        return if (isActive) this else comparing
    }

    // 2. Has transaction history wins.
    val selfHasHistory = latestProductId != null
    val otherHasHistory = comparing.latestProductId != null
    if (selfHasHistory != otherHasHistory) {
        return if (selfHasHistory) this else comparing
    }

    // 3. Lifetime wins.
    val selfIsLifetime = isLifetime ?: false
    val otherIsLifetime = comparing.isLifetime ?: false
    if (selfIsLifetime != otherIsLifetime) {
        return if (selfIsLifetime) this else comparing
    }

    // 4. Non-revoked wins.
    val selfIsRevoked = state == LatestSubscriptionState.REVOKED
    val otherIsRevoked = comparing.state == LatestSubscriptionState.REVOKED
    if (selfIsRevoked != otherIsRevoked) {
        return if (selfIsRevoked) comparing else this
    }

    // 5. Latest renewal wins.
    val selfRenewedAt = renewedAt
    val otherRenewedAt = comparing.renewedAt
    if (selfRenewedAt != null && otherRenewedAt != null) {
        return if (selfRenewedAt > otherRenewedAt) this else comparing
    }

    // 6. Latest expiration wins.
    val selfExpiresAt = expiresAt
    val otherExpiresAt = comparing.expiresAt
    if (selfExpiresAt != null && otherExpiresAt != null) {
        return if (selfExpiresAt > otherExpiresAt) this else comparing
    }

    // Default to self.
    return this
}
