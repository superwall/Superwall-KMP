package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.internal.mergePrioritized
import com.superwall.sdk.kmp.internal.prioritized
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Tests for [mergePrioritized]/[prioritized], covering the six priority rules
 * ported from the Flutter SDK's `Entitlement.mergePrioritized`/`_priority`
 * (lib/src/public/CustomerInfo.dart) plus ordering and edge cases. The Flutter
 * repo ships no test file for these; the cases below are derived from that
 * implementation's documented rules:
 * 1. Active wins over inactive.
 * 2. Transaction history (`latestProductId != null`) wins.
 * 3. Lifetime wins (`null` treated as `false`).
 * 4. Non-revoked wins.
 * 5. Latest renewal date wins (only when both have one).
 * 6. Latest expiration date wins (only when both have one).
 * Ties default to the incoming entitlement.
 */
class EntitlementPriorityTest {
    private fun entitlement(
        id: String = "pro",
        isActive: Boolean = true,
        latestProductId: String? = null,
        isLifetime: Boolean? = null,
        state: LatestSubscriptionState? = null,
        renewedAtMs: Long? = null,
        expiresAtMs: Long? = null,
        productIds: List<String> = emptyList(),
    ): Entitlement =
        Entitlement(
            id = id,
            isActive = isActive,
            latestProductId = latestProductId,
            isLifetime = isLifetime,
            state = state,
            renewedAt = renewedAtMs?.let(Instant::fromEpochMilliseconds),
            expiresAt = expiresAtMs?.let(Instant::fromEpochMilliseconds),
            productIds = productIds,
        )

    // ---- mergePrioritized ------------------------------------------------------

    @Test
    fun emptySetMergesToEmptySet() {
        assertEquals(emptySet(), mergePrioritized(emptySet()))
    }

    @Test
    fun singleEntitlementPassesThrough() {
        val only = entitlement(id = "pro")
        assertEquals(setOf(only), mergePrioritized(setOf(only)))
    }

    @Test
    fun distinctIdsAreAllKept() {
        val pro = entitlement(id = "pro")
        val plus = entitlement(id = "plus", isActive = false)
        val gold = entitlement(id = "gold", latestProductId = "gold_annual")
        assertEquals(setOf(pro, plus, gold), mergePrioritized(setOf(pro, plus, gold)))
    }

    @Test
    fun duplicateIdsCollapseToOneEntry() {
        val inactive = entitlement(id = "pro", isActive = false)
        val active = entitlement(id = "pro", isActive = true)
        val merged = mergePrioritized(setOf(inactive, active))
        assertEquals(1, merged.size)
    }

    @Test
    fun mixedIdsMergeIndependently() {
        val proActive = entitlement(id = "pro", isActive = true)
        val proInactive = entitlement(id = "pro", isActive = false)
        val plusLifetime = entitlement(id = "plus", isLifetime = true)
        val plusNot = entitlement(id = "plus", isLifetime = false)
        val merged = mergePrioritized(setOf(proActive, proInactive, plusLifetime, plusNot))
        assertEquals(setOf(proActive, plusLifetime), merged)
    }

    @Test
    fun mergeAcrossThreeDuplicatesKeepsHighestPriority() {
        val inactive = entitlement(id = "pro", isActive = false)
        val activeNoHistory = entitlement(id = "pro", isActive = true)
        val activeWithHistory = entitlement(id = "pro", isActive = true, latestProductId = "pro_monthly")
        // Order in the set shouldn't matter for a strict priority chain.
        val merged = mergePrioritized(setOf(activeNoHistory, inactive, activeWithHistory))
        assertEquals(setOf(activeWithHistory), merged)
    }

    // ---- Rule 1: active wins ------------------------------------------------------

    @Test
    fun activeWinsOverInactiveBothDirections() {
        val active = entitlement(isActive = true)
        val inactive = entitlement(isActive = false)
        assertSame(active, active.prioritized(comparing = inactive))
        assertSame(active, inactive.prioritized(comparing = active))
    }

    @Test
    fun activeRuleBeatsAllLowerRules() {
        // Inactive but with history, lifetime, non-revoked, newer dates...
        val stackedInactive =
            entitlement(
                isActive = false,
                latestProductId = "pro_annual",
                isLifetime = true,
                state = LatestSubscriptionState.EXPIRED,
                renewedAtMs = 2_000,
                expiresAtMs = 2_000,
            )
        // ...still loses to a bare active entitlement.
        val bareActive = entitlement(isActive = true)
        assertSame(bareActive, stackedInactive.prioritized(comparing = bareActive))
    }

    // ---- Rule 2: transaction history wins ----------------------------------------------

    @Test
    fun transactionHistoryWinsBothDirections() {
        val withHistory = entitlement(latestProductId = "pro_monthly")
        val withoutHistory = entitlement(latestProductId = null)
        assertSame(withHistory, withHistory.prioritized(comparing = withoutHistory))
        assertSame(withHistory, withoutHistory.prioritized(comparing = withHistory))
    }

    @Test
    fun historyRuleBeatsLifetimeRule() {
        val historyNotLifetime = entitlement(latestProductId = "pro_monthly", isLifetime = false)
        val lifetimeNoHistory = entitlement(latestProductId = null, isLifetime = true)
        assertSame(historyNotLifetime, lifetimeNoHistory.prioritized(comparing = historyNotLifetime))
    }

    // ---- Rule 3: lifetime wins -----------------------------------------------------------

    @Test
    fun lifetimeWinsBothDirections() {
        val lifetime = entitlement(isLifetime = true)
        val notLifetime = entitlement(isLifetime = false)
        assertSame(lifetime, lifetime.prioritized(comparing = notLifetime))
        assertSame(lifetime, notLifetime.prioritized(comparing = lifetime))
    }

    @Test
    fun nullIsLifetimeIsTreatedAsFalse() {
        val lifetime = entitlement(isLifetime = true)
        val unknownLifetime = entitlement(isLifetime = null)
        assertSame(lifetime, unknownLifetime.prioritized(comparing = lifetime))
    }

    @Test
    fun lifetimeRuleBeatsRenewalDates() {
        val lifetimeOld = entitlement(isLifetime = true, renewedAtMs = 1_000, expiresAtMs = 1_000)
        val recentNotLifetime = entitlement(isLifetime = false, renewedAtMs = 9_000, expiresAtMs = 9_000)
        assertSame(lifetimeOld, recentNotLifetime.prioritized(comparing = lifetimeOld))
    }

    // ---- Rule 4: non-revoked wins ------------------------------------------------------------

    @Test
    fun nonRevokedWinsBothDirections() {
        val revoked = entitlement(state = LatestSubscriptionState.REVOKED)
        val subscribed = entitlement(state = LatestSubscriptionState.SUBSCRIBED)
        assertSame(subscribed, revoked.prioritized(comparing = subscribed))
        assertSame(subscribed, subscribed.prioritized(comparing = revoked))
    }

    @Test
    fun nullStateCountsAsNonRevoked() {
        val revoked = entitlement(state = LatestSubscriptionState.REVOKED)
        val noState = entitlement(state = null)
        assertSame(noState, revoked.prioritized(comparing = noState))
    }

    @Test
    fun activeButRevokedBeatsInactiveNonRevoked() {
        // Rule 1 outranks rule 4.
        val activeRevoked = entitlement(isActive = true, state = LatestSubscriptionState.REVOKED)
        val inactiveClean = entitlement(isActive = false, state = LatestSubscriptionState.EXPIRED)
        assertSame(activeRevoked, inactiveClean.prioritized(comparing = activeRevoked))
    }

    // ---- Rule 5: latest renewal wins -------------------------------------------------------------

    @Test
    fun laterRenewalWinsBothDirections() {
        val older = entitlement(renewedAtMs = 1_000)
        val newer = entitlement(renewedAtMs = 2_000)
        assertSame(newer, newer.prioritized(comparing = older))
        assertSame(newer, older.prioritized(comparing = newer))
    }

    @Test
    fun renewalRuleOutranksExpirationRule() {
        // Later renewal but earlier expiration still wins.
        val renewedLaterExpiresSooner = entitlement(renewedAtMs = 5_000, expiresAtMs = 6_000)
        val renewedEarlierExpiresLater = entitlement(renewedAtMs = 4_000, expiresAtMs = 9_000)
        assertSame(
            renewedLaterExpiresSooner,
            renewedEarlierExpiresLater.prioritized(comparing = renewedLaterExpiresSooner),
        )
    }

    @Test
    fun renewalRuleRequiresBothDatesAndFallsThroughToExpiration() {
        // One-sided renewedAt: rule 5 is skipped, rule 6 decides.
        val noRenewalLaterExpiry = entitlement(renewedAtMs = null, expiresAtMs = 9_000)
        val renewedButEarlierExpiry = entitlement(renewedAtMs = 5_000, expiresAtMs = 1_000)
        assertSame(
            noRenewalLaterExpiry,
            renewedButEarlierExpiry.prioritized(comparing = noRenewalLaterExpiry),
        )
    }

    // ---- Rule 6: latest expiration wins ----------------------------------------------------------------

    @Test
    fun laterExpirationWinsBothDirections() {
        val expiresSooner = entitlement(expiresAtMs = 1_000)
        val expiresLater = entitlement(expiresAtMs = 2_000)
        assertSame(expiresLater, expiresLater.prioritized(comparing = expiresSooner))
        assertSame(expiresLater, expiresSooner.prioritized(comparing = expiresLater))
    }

    @Test
    fun oneSidedExpirationFallsThroughToDefault() {
        // Rule 6 also requires both dates; with only one present, default-to-self applies.
        val withExpiry = entitlement(expiresAtMs = 9_000, productIds = listOf("a"))
        val withoutExpiry = entitlement(expiresAtMs = null, productIds = listOf("b"))
        assertSame(withoutExpiry, withoutExpiry.prioritized(comparing = withExpiry))
        assertSame(withExpiry, withExpiry.prioritized(comparing = withoutExpiry))
    }

    // ---- Ties -------------------------------------------------------------------------------

    @Test
    fun fullTieDefaultsToSelf() {
        // Identical on every priority-relevant field; differ only in productIds.
        val first = entitlement(productIds = listOf("first"))
        val second = entitlement(productIds = listOf("second"))
        assertSame(first, first.prioritized(comparing = second))
        assertSame(second, second.prioritized(comparing = first))
    }

    @Test
    fun mergeTieKeepsLastIteratedEntitlement() {
        // In mergePrioritized the incoming entitlement is `this`, so on a full
        // tie the later set entry wins (matching the Dart implementation).
        val first = entitlement(productIds = listOf("first"))
        val second = entitlement(productIds = listOf("second"))
        val merged = mergePrioritized(setOf(first, second)) // setOf is insertion-ordered
        assertEquals(1, merged.size)
        assertTrue(second in merged, "expected the last-iterated tied entitlement to win")
    }

    @Test
    fun equalDatesTieDefaultsToIncoming() {
        val a = entitlement(renewedAtMs = 5_000, expiresAtMs = 5_000, productIds = listOf("a"))
        val b = entitlement(renewedAtMs = 5_000, expiresAtMs = 5_000, productIds = listOf("b"))
        // Equal renewedAt: `>` is false, so rule 5 returns `comparing` — the
        // incoming entitlement loses on exact-equal dates (Dart `isAfter` parity).
        assertSame(b, a.prioritized(comparing = b))
        assertSame(a, b.prioritized(comparing = a))
    }
}
