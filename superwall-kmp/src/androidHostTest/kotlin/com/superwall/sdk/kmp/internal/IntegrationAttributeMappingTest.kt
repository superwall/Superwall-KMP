package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.superwall.sdk.models.attribution.AttributionProvider as NativeAttributionProvider

class IntegrationAttributeMappingTest {
    @Test
    fun airbridgeId_mapsBothWays() {
        assertEquals(
            NativeAttributionProvider.AIRBRIDGE_ID,
            IntegrationAttribute.AIRBRIDGE_ID.toNativeAttributionProvider(),
        )
        assertEquals(
            IntegrationAttribute.AIRBRIDGE_ID,
            NativeAttributionProvider.AIRBRIDGE_ID.toKmpIntegrationAttribute(),
        )
        assertEquals("airbridgeId", NativeAttributionProvider.AIRBRIDGE_ID.rawName)
    }

    @Test
    fun everyMappedAttribute_roundTrips() {
        for (attribute in IntegrationAttribute.entries) {
            val native = attribute.toNativeAttributionProvider() ?: continue
            assertEquals(attribute, native.toKmpIntegrationAttribute(), "round trip of $attribute")
        }
    }

    @Test
    fun firebaseInstallationId_hasNoAndroidCounterpart() {
        assertNull(IntegrationAttribute.FIREBASE_INSTALLATION_ID.toNativeAttributionProvider())
    }
}
