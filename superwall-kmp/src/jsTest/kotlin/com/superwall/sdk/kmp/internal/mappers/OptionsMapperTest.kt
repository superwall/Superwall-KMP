package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.interop.JsAnySanitizer
import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.Logging
import com.superwall.sdk.kmp.models.options.NetworkEnvironment
import com.superwall.sdk.kmp.models.options.PaywallOptions
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.options.TestModeBehavior
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OptionsMapperTest {
    /** Web's `IntegrationAttribute` union in @superwall/paywalls-js 0.3.0 (types.ts). */
    private val webIntegrationAttributes =
        setOf(
            "adjustId", "amplitudeDeviceId", "amplitudeUserId", "appsflyerId", "brazeAliasName",
            "brazeAliasLabel", "onesignalId", "fbAnonId", "firebaseAppInstanceId", "iterableUserId",
            "iterableCampaignId", "iterableTemplateId", "mixpanelDistinctId", "mparticleId", "clevertapId",
            "airshipChannelId", "kochavaDeviceId", "tenjinId", "posthogUserId", "customerioId", "meta",
            "amplitude", "mixpanel", "googleAds", "googleAppSetId", "appstackId", "custom",
        )

    @Test
    fun integrationAttributesUseWebWireNames() {
        val names = IntegrationAttribute.entries.associateWith { it.toJs() }
        // The two the web union lacks are still sent (web stores any key).
        val notInWebUnion = names.filterValues { it !in webIntegrationAttributes }.keys
        assertEquals(setOf(IntegrationAttribute.FIREBASE_INSTALLATION_ID, IntegrationAttribute.SINGULAR_DEVICE_ID), notInWebUnion)
        assertEquals("onesignalId", IntegrationAttribute.ONESIGNAL_ID.toJs())
        for ((attribute, name) in names) assertEquals(attribute, integrationAttributeFromJs(name))
        assertNull(integrationAttributeFromJs("meta"))
    }

    @Test
    fun logLevelsRoundTrip() {
        for (level in LogLevel.entries) assertEquals(level, logLevelFromJs(level.toJs()))
        assertNull(logLevelFromJs("verbose"))
    }

    @Test
    fun webLogScopesRoundTrip() {
        for (scope in LogScope.entries) {
            val wire = scope.toJsOrNull() ?: continue
            val back = logScopeFromJs(wire)
            // The two localization scopes share web's single "localization".
            if (scope != LogScope.LOCALIZATION_VIEW_CONTROLLER) assertEquals(scope, back)
        }
        assertNull(LogScope.STORE_KIT_MANAGER.toJsOrNull())
        assertNull(logScopeFromJs("somethingNew"))
    }

    @Test
    fun nullOptionsStillReportTheWrapper() {
        assertEquals(mapOf("platformWrapper" to "KMP"), JsAnySanitizer.fromObject((null as SuperwallOptions?).toJs()))
    }

    @Test
    fun optionsMapToWebShape() {
        val options =
            SuperwallOptions(
                paywalls = PaywallOptions(shouldPreload = false, automaticallyDismiss = false),
                networkEnvironment = NetworkEnvironment.DEVELOPER,
                localeIdentifier = "de_DE",
                testModeBehavior = TestModeBehavior.ALWAYS,
                maxConfigRetryCount = 2,
                logging = Logging(level = LogLevel.WARN, scopes = setOf(LogScope.NETWORK, LogScope.STORE_KIT_MANAGER)),
            )
        val web = JsAnySanitizer.fromObject(options.toJs())!!
        assertEquals("developer", web["networkEnvironment"])
        assertEquals("de_DE", web["localeIdentifier"])
        assertEquals("always", web["testModeBehavior"])
        assertEquals(2L, web["maxConfigRetryCount"])
        assertEquals("KMP", web["platformWrapper"])
        @Suppress("UNCHECKED_CAST")
        val paywalls = web["paywalls"] as Map<String, Any?>
        assertEquals(false, paywalls["shouldPreload"])
        assertEquals(false, paywalls["automaticallyDismiss"])
        @Suppress("UNCHECKED_CAST")
        val logging = web["logging"] as Map<String, Any?>
        assertEquals("warn", logging["level"])
        // Native-only scopes are dropped, not sent.
        assertEquals(listOf("network"), logging["scopes"])
    }

    @Test
    fun allNativeOnlyScopesWidenToAll() {
        val web = JsAnySanitizer.fromObject(SuperwallOptions(logging = Logging(scopes = setOf(LogScope.RECEIPTS))).toJs())!!
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("all"), (web["logging"] as Map<String, Any?>)["scopes"])
    }
}
