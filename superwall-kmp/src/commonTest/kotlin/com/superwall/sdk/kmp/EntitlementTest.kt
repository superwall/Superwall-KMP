package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** The derived properties follow the native SDKs' definitions. */
class EntitlementTest {
    private val expiry = Instant.fromEpochMilliseconds(1_800_000_000_000)

    @Test
    fun derivedPropertiesAreNullWithoutASubscription() {
        val entitlement = Entitlement(id = "pro")
        assertNull(entitlement.isRevoked)
        assertNull(entitlement.isInGracePeriod)
        assertNull(entitlement.renewsAt)
    }

    @Test
    fun revokedAndGracePeriodFollowTheState() {
        assertEquals(true, Entitlement(id = "pro", state = LatestSubscriptionState.REVOKED).isRevoked)
        assertEquals(false, Entitlement(id = "pro", state = LatestSubscriptionState.SUBSCRIBED).isRevoked)
        assertEquals(true, Entitlement(id = "pro", state = LatestSubscriptionState.IN_GRACE_PERIOD).isInGracePeriod)
        assertEquals(false, Entitlement(id = "pro", state = LatestSubscriptionState.EXPIRED).isInGracePeriod)
    }

    @Test
    fun renewsAtIsTheExpiryOnlyWhileActiveAndRenewing() {
        assertEquals(expiry, Entitlement(id = "pro", isActive = true, willRenew = true, expiresAt = expiry).renewsAt)
        assertNull(Entitlement(id = "pro", isActive = true, willRenew = false, expiresAt = expiry).renewsAt)
        assertNull(Entitlement(id = "pro", isActive = false, willRenew = true, expiresAt = expiry).renewsAt)
    }
}
