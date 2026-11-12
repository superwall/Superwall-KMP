package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.superwall.sdk.models.attribution.AttributionProvider as NativeAttributionProvider

/**
 * The merge/null-removes contract over superwall-android's replace-all
 * `setIntegrationAttributes`, against a fake native side whose getter lags
 * behind its setter, as the real one does for 500 ms.
 */
class IntegrationAttributeStoreTest {
    /** Native attributes as the native getter reports them (raw names). */
    private var nativeAttributes: Map<String, String> = emptyMap()
    private val writes = mutableListOf<Map<NativeAttributionProvider, String>>()
    private val logs = mutableListOf<String>()

    private val store =
        IntegrationAttributeStore(
            readNative = { nativeAttributes },
            // Deliberately does NOT update nativeAttributes: the native SDK
            // applies the map later.
            writeNative = { writes += it },
            log = { message, _ -> logs += message },
        )

    @Test
    fun quickSuccessiveSetsAreMergedNotDropped() {
        store.set(mapOf(IntegrationAttribute.ADJUST_ID to "adj"))
        store.set(mapOf(IntegrationAttribute.APPSFLYER_ID to "af"))

        assertEquals(
            mapOf(
                NativeAttributionProvider.ADJUST_ID to "adj",
                NativeAttributionProvider.APPSFLYER_ID to "af",
            ),
            writes.last(),
        )
        assertEquals(
            mapOf<IntegrationAttribute, String?>(
                IntegrationAttribute.ADJUST_ID to "adj",
                IntegrationAttribute.APPSFLYER_ID to "af",
            ),
            store.get(),
        )
    }

    @Test
    fun theFirstSetMergesIntoTheNativeAttributes() {
        nativeAttributes = mapOf(NativeAttributionProvider.MIXPANEL_DISTINCT_ID.rawName to "mp")

        store.set(mapOf(IntegrationAttribute.ADJUST_ID to "adj"))

        assertEquals(
            mapOf(
                NativeAttributionProvider.MIXPANEL_DISTINCT_ID to "mp",
                NativeAttributionProvider.ADJUST_ID to "adj",
            ),
            writes.single(),
        )
    }

    @Test
    fun nullRemovesTheAttribute() {
        store.set(mapOf(IntegrationAttribute.ADJUST_ID to "adj", IntegrationAttribute.APPSFLYER_ID to "af"))
        store.set(mapOf(IntegrationAttribute.ADJUST_ID to null))

        assertEquals(mapOf(NativeAttributionProvider.APPSFLYER_ID to "af"), writes.last())
        assertNull(store.get()[IntegrationAttribute.ADJUST_ID])
    }

    @Test
    fun getReadsTheNativeAttributesUntilSomethingIsSet() {
        nativeAttributes =
            mapOf(
                NativeAttributionProvider.ADJUST_ID.rawName to "adj",
                // Native-only provider and an unknown raw name: dropped on read.
                NativeAttributionProvider.META.rawName to "meta",
                "someFutureProvider" to "x",
            )

        assertEquals(mapOf<IntegrationAttribute, String?>(IntegrationAttribute.ADJUST_ID to "adj"), store.get())
    }

    @Test
    fun nativeOnlyProvidersSurviveAMerge() {
        nativeAttributes = mapOf(NativeAttributionProvider.META.rawName to "meta")

        store.set(mapOf(IntegrationAttribute.ADJUST_ID to "adj"))

        assertEquals("meta", writes.single()[NativeAttributionProvider.META])
    }

    @Test
    fun anIosOnlyAttributeIsSkippedWithALog() {
        store.set(mapOf(IntegrationAttribute.FIREBASE_INSTALLATION_ID to "fid"))

        assertTrue(writes.single().isEmpty())
        assertTrue(logs.single().contains("FIREBASE_INSTALLATION_ID"))
    }
}
