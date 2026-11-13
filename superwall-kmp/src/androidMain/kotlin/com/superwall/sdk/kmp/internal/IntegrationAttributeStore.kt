package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import com.superwall.sdk.models.attribution.AttributionProvider as NativeAttributionProvider

/**
 * The common merge/null-removes contract for integration attributes, over
 * superwall-android's `setIntegrationAttributes`, which REPLACES the whole
 * map (`AttributionManager.kt`): each call applies its changes to the current
 * attributes and writes the merged map back.
 *
 * "Current" is the last map written here when there is one, not the native
 * getter. The native call applies its map 500 ms later on a background job,
 * cancelling a pending one, so its getter lags behind: merging a second call
 * into it would drop the first call's attributes, and a read right after a
 * write would return the old ones. superwall-android keeps the applied map in
 * memory for the process's lifetime (`reset()` does not clear it), so the
 * last written map stays the current one.
 */
internal class IntegrationAttributeStore(
    private val readNative: () -> Map<String, String>,
    private val writeNative: (Map<NativeAttributionProvider, String>) -> Unit,
    private val log: (message: String, error: Throwable?) -> Unit,
) {
    private val lock = Any()

    /** The last map handed to [writeNative]; guarded by [lock]. */
    private var latest: Map<NativeAttributionProvider, String>? = null

    fun set(attributes: Map<IntegrationAttribute, String?>) {
        synchronized(lock) {
            val merged = current().toMutableMap()
            for ((attribute, value) in attributes) {
                val native = attribute.toNativeAttributionProvider()
                if (native == null) {
                    log(
                        "Integration attribute ${attribute.name} is not supported by " +
                            "superwall-android and was skipped.",
                        null,
                    )
                    continue
                }
                if (value == null) merged.remove(native) else merged[native] = value
            }
            latest = merged
            writeNative(merged)
        }
    }

    fun get(): Map<IntegrationAttribute, String?> =
        synchronized(lock) { current() }
            .entries
            .mapNotNull { (provider, value) ->
                provider.toKmpIntegrationAttribute()?.let { it to value }
            }.toMap()

    /**
     * [latest], else the native raw-name-keyed map read back into provider
     * keys. Raw names that don't correspond to a known provider are dropped
     * (degrade, never crash).
     */
    private fun current(): Map<NativeAttributionProvider, String> =
        latest ?: readNative()
            .entries
            .mapNotNull { (rawName, value) ->
                NativeAttributionProvider.entries
                    .firstOrNull { it.rawName == rawName }
                    ?.let { it to value }
            }.toMap()
}

// ---- IntegrationAttribute <-> AttributionProvider ----------------------------

/**
 * Maps the common [IntegrationAttribute] to the native
 * [NativeAttributionProvider], or `null` for the iOS-only
 * [IntegrationAttribute.FIREBASE_INSTALLATION_ID] (superwall-android
 * has no counterpart — [IntegrationAttributeStore] skips it with a logged warning).
 */
internal fun IntegrationAttribute.toNativeAttributionProvider(): NativeAttributionProvider? =
    when (this) {
        IntegrationAttribute.FIREBASE_INSTALLATION_ID -> null
        IntegrationAttribute.SINGULAR_DEVICE_ID -> NativeAttributionProvider.SINGULAR_DEVICE_ID
        IntegrationAttribute.ADJUST_ID -> NativeAttributionProvider.ADJUST_ID
        IntegrationAttribute.AMPLITUDE_DEVICE_ID -> NativeAttributionProvider.AMPLITUDE_DEVICE_ID
        IntegrationAttribute.AMPLITUDE_USER_ID -> NativeAttributionProvider.AMPLITUDE_USER_ID
        IntegrationAttribute.APPSFLYER_ID -> NativeAttributionProvider.APPSFLYER_ID
        IntegrationAttribute.BRAZE_ALIAS_NAME -> NativeAttributionProvider.BRAZE_ALIAS_NAME
        IntegrationAttribute.BRAZE_ALIAS_LABEL -> NativeAttributionProvider.BRAZE_ALIAS_LABEL
        IntegrationAttribute.ONESIGNAL_ID -> NativeAttributionProvider.ONESIGNAL_ID
        IntegrationAttribute.FB_ANON_ID -> NativeAttributionProvider.FB_ANON_ID
        IntegrationAttribute.FIREBASE_APP_INSTANCE_ID ->
            NativeAttributionProvider.FIREBASE_APP_INSTANCE_ID
        IntegrationAttribute.ITERABLE_USER_ID -> NativeAttributionProvider.ITERABLE_USER_ID
        IntegrationAttribute.ITERABLE_CAMPAIGN_ID -> NativeAttributionProvider.ITERABLE_CAMPAIGN_ID
        IntegrationAttribute.ITERABLE_TEMPLATE_ID -> NativeAttributionProvider.ITERABLE_TEMPLATE_ID
        IntegrationAttribute.MIXPANEL_DISTINCT_ID -> NativeAttributionProvider.MIXPANEL_DISTINCT_ID
        IntegrationAttribute.MPARTICLE_ID -> NativeAttributionProvider.MPARTICLE_ID
        IntegrationAttribute.CLEVERTAP_ID -> NativeAttributionProvider.CLEVERTAP_ID
        IntegrationAttribute.AIRSHIP_CHANNEL_ID -> NativeAttributionProvider.AIRSHIP_CHANNEL_ID
        IntegrationAttribute.KOCHAVA_DEVICE_ID -> NativeAttributionProvider.KOCHAVA_DEVICE_ID
        IntegrationAttribute.TENJIN_ID -> NativeAttributionProvider.TENJIN_ID
        IntegrationAttribute.POSTHOG_USER_ID -> NativeAttributionProvider.POSTHOG_USER_ID
        IntegrationAttribute.CUSTOMERIO_ID -> NativeAttributionProvider.CUSTOMERIO_ID
        IntegrationAttribute.APPSTACK_ID -> NativeAttributionProvider.APPSTACK
        IntegrationAttribute.AIRBRIDGE_ID -> NativeAttributionProvider.AIRBRIDGE_ID
        IntegrationAttribute.META -> NativeAttributionProvider.META
        IntegrationAttribute.AMPLITUDE -> NativeAttributionProvider.AMPLITUDE
        IntegrationAttribute.MIXPANEL -> NativeAttributionProvider.MIXPANEL
        IntegrationAttribute.GOOGLE_ADS -> NativeAttributionProvider.GOOGLE_ADS
        IntegrationAttribute.GOOGLE_APP_SET_ID -> NativeAttributionProvider.GOOGLE_APP_SET
        IntegrationAttribute.CUSTOM -> NativeAttributionProvider.CUSTOM
    }

/**
 * Maps a native provider back to the common enum. Every superwall-android
 * 2.10.2 provider has a counterpart; the nullable return keeps the store's
 * "drop what can't be mapped" contract for providers added later.
 */
internal fun NativeAttributionProvider.toKmpIntegrationAttribute(): IntegrationAttribute? =
    when (this) {
        NativeAttributionProvider.SINGULAR_DEVICE_ID -> IntegrationAttribute.SINGULAR_DEVICE_ID
        NativeAttributionProvider.ADJUST_ID -> IntegrationAttribute.ADJUST_ID
        NativeAttributionProvider.AMPLITUDE_DEVICE_ID -> IntegrationAttribute.AMPLITUDE_DEVICE_ID
        NativeAttributionProvider.AMPLITUDE_USER_ID -> IntegrationAttribute.AMPLITUDE_USER_ID
        NativeAttributionProvider.APPSFLYER_ID -> IntegrationAttribute.APPSFLYER_ID
        NativeAttributionProvider.BRAZE_ALIAS_NAME -> IntegrationAttribute.BRAZE_ALIAS_NAME
        NativeAttributionProvider.BRAZE_ALIAS_LABEL -> IntegrationAttribute.BRAZE_ALIAS_LABEL
        NativeAttributionProvider.ONESIGNAL_ID -> IntegrationAttribute.ONESIGNAL_ID
        NativeAttributionProvider.FB_ANON_ID -> IntegrationAttribute.FB_ANON_ID
        NativeAttributionProvider.FIREBASE_APP_INSTANCE_ID ->
            IntegrationAttribute.FIREBASE_APP_INSTANCE_ID
        NativeAttributionProvider.ITERABLE_USER_ID -> IntegrationAttribute.ITERABLE_USER_ID
        NativeAttributionProvider.ITERABLE_CAMPAIGN_ID -> IntegrationAttribute.ITERABLE_CAMPAIGN_ID
        NativeAttributionProvider.ITERABLE_TEMPLATE_ID -> IntegrationAttribute.ITERABLE_TEMPLATE_ID
        NativeAttributionProvider.MIXPANEL_DISTINCT_ID -> IntegrationAttribute.MIXPANEL_DISTINCT_ID
        NativeAttributionProvider.MPARTICLE_ID -> IntegrationAttribute.MPARTICLE_ID
        NativeAttributionProvider.CLEVERTAP_ID -> IntegrationAttribute.CLEVERTAP_ID
        NativeAttributionProvider.AIRSHIP_CHANNEL_ID -> IntegrationAttribute.AIRSHIP_CHANNEL_ID
        NativeAttributionProvider.KOCHAVA_DEVICE_ID -> IntegrationAttribute.KOCHAVA_DEVICE_ID
        NativeAttributionProvider.TENJIN_ID -> IntegrationAttribute.TENJIN_ID
        NativeAttributionProvider.POSTHOG_USER_ID -> IntegrationAttribute.POSTHOG_USER_ID
        NativeAttributionProvider.CUSTOMERIO_ID -> IntegrationAttribute.CUSTOMERIO_ID
        NativeAttributionProvider.APPSTACK -> IntegrationAttribute.APPSTACK_ID
        NativeAttributionProvider.AIRBRIDGE_ID -> IntegrationAttribute.AIRBRIDGE_ID
        NativeAttributionProvider.META -> IntegrationAttribute.META
        NativeAttributionProvider.AMPLITUDE -> IntegrationAttribute.AMPLITUDE
        NativeAttributionProvider.MIXPANEL -> IntegrationAttribute.MIXPANEL
        NativeAttributionProvider.GOOGLE_ADS -> IntegrationAttribute.GOOGLE_ADS
        NativeAttributionProvider.GOOGLE_APP_SET -> IntegrationAttribute.GOOGLE_APP_SET_ID
        NativeAttributionProvider.CUSTOM -> IntegrationAttribute.CUSTOM
    }
