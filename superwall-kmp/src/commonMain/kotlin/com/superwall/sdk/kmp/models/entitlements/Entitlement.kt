package com.superwall.sdk.kmp.models.entitlements

import kotlin.time.Instant

/**
 * An entitlement that represents a subscription tier in your app.
 *
 * @property id The identifier for the entitlement.
 * @property type The type of entitlement.
 * @property isActive Indicates whether there is any active, non-revoked transaction for this
 * entitlement.
 * @property productIds All product identifiers that map to the entitlement.
 * @property latestProductId The product identifier of the latest transaction to unlock this
 * entitlement.
 * @property store The store from which this entitlement was granted.
 * @property startsAt The purchase date of the first transaction that unlocked this entitlement.
 * @property renewedAt The date that the entitlement was last renewed.
 * @property expiresAt The expiry date of the last transaction that unlocked this entitlement.
 * @property isLifetime Indicates whether the entitlement is active for a lifetime due to a
 * non-consumable purchase.
 * @property willRenew Indicates whether the last subscription transaction will auto renew.
 * @property state The state of the last subscription transaction.
 * @property offerType The type of offer that applies to the last subscription transaction.
 */
public data class Entitlement(
    val id: String,
    val type: EntitlementType = EntitlementType.SERVICE_LEVEL,
    val isActive: Boolean = true,
    val productIds: List<String> = emptyList(),
    val latestProductId: String? = null,
    val store: ProductStore? = null,
    val startsAt: Instant? = null,
    val renewedAt: Instant? = null,
    val expiresAt: Instant? = null,
    val isLifetime: Boolean? = null,
    val willRenew: Boolean? = null,
    val state: LatestSubscriptionState? = null,
    val offerType: LatestSubscriptionOfferType? = null,
) {
    /**
     * Whether the last subscription transaction that unlocks this entitlement
     * was revoked, or `null` without one (e.g. a manually granted
     * entitlement). Derived from [state], as the native SDKs do; Android
     * tracks revocation per transaction, so there it is never `true`.
     */
    val isRevoked: Boolean?
        get() = state?.let { it == LatestSubscriptionState.REVOKED }

    /** When the subscription renews: [expiresAt] while active and set to renew, otherwise `null`. */
    val renewsAt: Instant?
        get() = if (isActive && willRenew == true) expiresAt else null

    /**
     * Whether the last subscription transaction that unlocks this entitlement
     * is in a billing grace period, or `null` without one. Derived from [state].
     */
    val isInGracePeriod: Boolean?
        get() = state?.let { it == LatestSubscriptionState.IN_GRACE_PERIOD }
}
