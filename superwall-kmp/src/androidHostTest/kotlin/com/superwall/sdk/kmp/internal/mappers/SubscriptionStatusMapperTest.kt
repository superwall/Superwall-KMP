package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import com.superwall.sdk.models.entitlements.Entitlement as NativeEntitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus as NativeSubscriptionStatus

/**
 * Both-direction tests for SubscriptionStatusMapper.kt against the real native
 * `SubscriptionStatus` sealed class.
 */
class SubscriptionStatusMapperTest {
    @Test
    fun inactive_mapsInBothDirections() {
        assertEquals(SubscriptionStatus.Inactive, NativeSubscriptionStatus.Inactive.toKmp())
        assertIs<NativeSubscriptionStatus.Inactive>(SubscriptionStatus.Inactive.toNative())
    }

    @Test
    fun unknown_mapsInBothDirections() {
        assertEquals(SubscriptionStatus.Unknown, NativeSubscriptionStatus.Unknown.toKmp())
        assertIs<NativeSubscriptionStatus.Unknown>(SubscriptionStatus.Unknown.toNative())
    }

    @Test
    fun active_nativeToCommon_carriesFullEntitlements() {
        val native =
            NativeSubscriptionStatus.Active(
                setOf(
                    NativeEntitlement(id = "pro", isActive = true, productIds = setOf("p1", "p2")),
                    NativeEntitlement(id = "plus"),
                ),
            )

        val common = native.toKmp()

        assertIs<SubscriptionStatus.Active>(common)
        assertEquals(setOf("pro", "plus"), common.entitlements.map { it.id }.toSet())
        val pro = common.entitlements.first { it.id == "pro" }
        assertEquals(true, pro.isActive)
        assertEquals(listOf("p1", "p2"), pro.productIds)
        assertTrue(common.isActive)
    }

    @Test
    fun active_commonToNative_carriesEntitlements() {
        val common =
            SubscriptionStatus.Active(
                setOf(Entitlement(id = "pro", isActive = true, productIds = listOf("p1"))),
            )

        val native = common.toNative()

        assertIs<NativeSubscriptionStatus.Active>(native)
        assertEquals(1, native.entitlements.size)
        val entitlement = native.entitlements.single()
        assertEquals("pro", entitlement.id)
        assertEquals(true, entitlement.isActive)
        assertEquals(setOf("p1"), entitlement.productIds)
        assertTrue(native.isActive)
    }

    @Test
    fun active_roundTripsCommonToNativeToCommon() {
        val original =
            SubscriptionStatus.Active(
                setOf(
                    Entitlement(id = "pro", isActive = true, productIds = listOf("p1", "p2")),
                    Entitlement(id = "plus", isActive = false),
                ),
            )

        assertEquals(original, original.toNative().toKmp())
    }
}
