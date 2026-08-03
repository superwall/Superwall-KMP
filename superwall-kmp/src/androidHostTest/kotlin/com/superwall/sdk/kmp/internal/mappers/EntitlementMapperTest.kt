package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.EntitlementType
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionOfferType
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionState
import com.superwall.sdk.kmp.models.entitlements.ProductStore
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import com.superwall.sdk.models.entitlements.Entitlement as NativeEntitlement
import com.superwall.sdk.models.product.Store as NativeStore
import com.superwall.sdk.store.abstractions.product.receipt.LatestPeriodType as NativeLatestPeriodType
import com.superwall.sdk.store.abstractions.product.receipt.LatestSubscriptionState as NativeLatestSubscriptionState

/**
 * Round-trip and conversion tests for EntitlementMapper.kt against the real
 * native `Entitlement` (all 13 device-enriched fields) and its enums.
 */
class EntitlementMapperTest {
    private val startsAtMs = 1_700_000_000_000L
    private val renewedAtMs = 1_710_000_000_000L
    private val expiresAtMs = 1_720_000_000_123L

    private fun fullCommonEntitlement(): Entitlement =
        Entitlement(
            id = "pro",
            type = EntitlementType.SERVICE_LEVEL,
            isActive = true,
            productIds = listOf("com.example.monthly", "com.example.annual"),
            latestProductId = "com.example.annual",
            store = ProductStore.PLAY_STORE,
            startsAt = Instant.fromEpochMilliseconds(startsAtMs),
            renewedAt = Instant.fromEpochMilliseconds(renewedAtMs),
            expiresAt = Instant.fromEpochMilliseconds(expiresAtMs),
            isLifetime = false,
            willRenew = true,
            state = LatestSubscriptionState.SUBSCRIBED,
            offerType = LatestSubscriptionOfferType.TRIAL,
        )

    @Test
    fun fullEntitlement_roundTripsCommonToNativeToCommon() {
        val original = fullCommonEntitlement()
        assertEquals(original, original.toNative().toKmp())
    }

    @Test
    fun fullEntitlement_commonToNative_mapsEveryField() {
        val native = fullCommonEntitlement().toNative()

        assertEquals("pro", native.id)
        assertEquals(NativeEntitlement.Type.SERVICE_LEVEL, native.type)
        assertEquals(true, native.isActive)
        assertEquals(setOf("com.example.monthly", "com.example.annual"), native.productIds)
        assertEquals("com.example.annual", native.latestProductId)
        assertEquals(NativeStore.PLAY_STORE, native.store)
        // Instants convert to native java.util.Date via epoch milliseconds.
        assertEquals(Date(startsAtMs), native.startsAt)
        assertEquals(Date(renewedAtMs), native.renewedAt)
        assertEquals(Date(expiresAtMs), native.expiresAt)
        assertEquals(false, native.isLifetime)
        assertEquals(true, native.willRenew)
        assertEquals(NativeLatestSubscriptionState.SUBSCRIBED, native.state)
        assertEquals(NativeLatestPeriodType.TRIAL, native.offerType)
    }

    @Test
    fun nativeEntitlement_toCommon_mapsDatesToEpochMillisecondInstants() {
        val native =
            NativeEntitlement(
                id = "pro",
                startsAt = Date(startsAtMs),
                expiresAt = Date(expiresAtMs),
            )

        val common = native.toKmp()

        assertEquals(Instant.fromEpochMilliseconds(startsAtMs), common.startsAt)
        assertNull(common.renewedAt)
        assertEquals(Instant.fromEpochMilliseconds(expiresAtMs), common.expiresAt)
    }

    @Test
    fun minimalNativeEntitlement_toCommon_keepsServerDefaults() {
        // NOTE: NativeEntitlement(id = "basic") alone resolves to the native
        // convenience secondary constructor, which forces isActive = true.
        // Passing a second primary-constructor-only parameter (productIds)
        // selects the primary constructor, whose device-enriched fields carry
        // the server defaults this test is about (isActive = false, all
        // enrichment nullable fields null).
        val common = NativeEntitlement(id = "basic", productIds = emptySet()).toKmp()

        assertEquals("basic", common.id)
        assertEquals(EntitlementType.SERVICE_LEVEL, common.type)
        assertEquals(false, common.isActive)
        assertEquals(emptyList(), common.productIds)
        assertNull(common.latestProductId)
        assertNull(common.store)
        assertNull(common.startsAt)
        assertNull(common.renewedAt)
        assertNull(common.expiresAt)
        assertNull(common.isLifetime)
        assertNull(common.willRenew)
        assertNull(common.state)
        assertNull(common.offerType)
    }

    // ---- ProductStore -----------------------------------------------------------

    @Test
    fun productStore_roundTripsExhaustivelyInBothDirections() {
        for (common in ProductStore.entries) {
            assertEquals(common, common.toNative().toKmp())
        }
        for (native in NativeStore.entries) {
            assertEquals(native, native.toKmp().toNative())
        }
    }

    // ---- LatestSubscriptionState ---------------------------------------------------

    @Test
    fun latestSubscriptionState_coversEveryNativeCase_unknownDegradesToNull() {
        for (native in NativeLatestSubscriptionState.entries) {
            val mapped = native.toKmp()
            if (native == NativeLatestSubscriptionState.UNKNOWN) {
                // The common enum has no unknown case; null means "not determinable".
                assertNull(mapped)
            } else {
                assertEquals(native, mapped!!.toNative(), "round trip failed for $native")
            }
        }
    }

    @Test
    fun latestSubscriptionState_commonRoundTripsExhaustively() {
        for (common in LatestSubscriptionState.entries) {
            assertEquals(common, common.toNative().toKmp())
        }
    }

    // ---- LatestPeriodType / LatestSubscriptionOfferType ------------------------------

    @Test
    fun latestPeriodType_coversEveryNativeCase_nonOffersDegradeToNull() {
        for (native in NativeLatestPeriodType.entries) {
            val mapped = native.toKmp()
            if (native == NativeLatestPeriodType.SUBSCRIPTION ||
                native == NativeLatestPeriodType.REVOKED
            ) {
                // Non-offer period types have no common counterpart: "no offer applied".
                assertNull(mapped)
            } else {
                assertEquals(native, mapped!!.toNative(), "round trip failed for $native")
            }
        }
    }

    @Test
    fun latestSubscriptionOfferType_commonRoundTripsExhaustively() {
        for (common in LatestSubscriptionOfferType.entries) {
            assertEquals(common, common.toNative().toKmp())
        }
    }
}
