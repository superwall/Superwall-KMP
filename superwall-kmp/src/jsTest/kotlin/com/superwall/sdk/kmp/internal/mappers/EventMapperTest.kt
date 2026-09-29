package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusReason
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusType
import com.superwall.sdk.kmp.models.results.RestoreType
import com.superwall.sdk.kmp.models.results.TriggerResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventMapperTest {
    /**
     * Every key of `SuperwallEventMap` in @superwall/paywalls-js 0.3.0
     * (packages/paywalls-js/src/events.ts). Update this list when bumping the
     * pinned version, and give any new key a real mapping or add it to
     * [webOnlyEvents] deliberately.
     */
    private val webEventNames =
        listOf(
            "first_seen", "app_open", "app_close", "app_launch", "app_install", "session_start", "reset",
            "config_refresh", "config_fail", "config_attributes", "confirm_all_assignments",
            "device_attributes", "user_attributes", "integration_attributes", "identity_alias",
            "deepLink_open", "subscriptionStatus_didChange", "customerInfo_didChange", "trigger_fire",
            "paywallPresentationRequest", "paywall_open", "paywall_page_view", "paywall_close",
            "paywall_decline", "paywallPreload_start", "paywallPreload_complete",
            "paywallResponseLoad_start", "paywallResponseLoad_notFound", "paywallResponseLoad_complete",
            "paywallResponseLoad_fail", "paywallWebviewLoad_start", "paywallWebviewLoad_complete",
            "paywallWebviewLoad_fail", "paywallWebviewLoad_timeout", "paywallProductsLoad_start",
            "paywallProductsLoad_complete", "paywallProductsLoad_fail", "paywallResourceLoad_fail",
            "shimmerView_start", "shimmerView_complete", "survey_response", "survey_close",
            "transaction_start", "transaction_complete", "transaction_fail", "transaction_abandon",
            "transaction_timeout", "transaction_restore", "restore_start", "restore_complete",
            "restore_fail", "subscription_start", "freeTrial_start", "nonRecurringProduct_purchase",
            "page_view", "enrichment_start", "enrichment_complete", "enrichment_fail",
            "discount_redeem_complete", "discount_redeem_fail", "custom_placement", "review_requested",
            "permission_requested", "permission_granted", "permission_denied",
        )

    /** Web events with no common EventType: they take the documented fallback shape. */
    private val webOnlyEvents = setOf("page_view", "paywall_page_view", "discount_redeem_complete", "discount_redeem_fail")

    @Test
    fun everyWebEventHasARealMappingExceptTheWebOnlyOnes() {
        for (name in webEventNames) {
            val info = webEventToKmp(name, js("({})"))
            if (name in webOnlyEvents) {
                assertEquals(EventType.CUSTOM_PLACEMENT, info.eventType, name)
                assertEquals(name, info.name, name)
            } else if (name != "custom_placement") {
                assertNotEquals(EventType.CUSTOM_PLACEMENT, info.eventType, "$name fell through to the fallback")
            }
        }
    }

    @Test
    fun webEventNamesMapToDistinctTypes() {
        val types =
            webEventNames
                .filter { it !in webOnlyEvents && it != "custom_placement" }
                .map { webEventToKmp(it, null).eventType }
        assertEquals(types.size, types.toSet().size, "two web events map to the same EventType")
    }

    @Test
    fun customTrackEventsAndUnknownNamesUseTheFallback() {
        val info = webEventToKmp("signed_up", js("({ plan: 'pro', seats: 3 })"))
        assertEquals(EventType.CUSTOM_PLACEMENT, info.eventType)
        assertEquals("signed_up", info.name)
        assertEquals(mapOf("plan" to "pro", "seats" to 3L), info.params)
    }

    @Test
    fun customPlacementCarriesItsOwnNameAndParams() {
        val info =
            webEventToKmp(
                "custom_placement",
                js("({ placementName: 'tapped_upgrade', params: { from: 'home' }, paywall_info: { identifier: 'pw' } })"),
            )
        assertEquals(EventType.CUSTOM_PLACEMENT, info.eventType)
        assertEquals("tapped_upgrade", info.name)
        assertEquals(mapOf("from" to "home"), info.params)
        assertEquals("pw", info.paywallInfo?.identifier)
    }

    @Test
    fun paywallEventsCarryPaywallInfo() {
        val info = webEventToKmp("paywall_open", js("({ paywall_info: { identifier: 'pw_1', name: 'Main' } })"))
        assertEquals(EventType.PAYWALL_OPEN, info.eventType)
        assertEquals("pw_1", info.paywallInfo?.identifier)
        assertEquals("Main", info.paywallInfo?.name)
    }

    @Test
    fun triggerFireMapsPlacementAndResult() {
        val info =
            webEventToKmp(
                "trigger_fire",
                js("({ placementName: 'campaign', result: { type: 'holdout', experiment: { id: 'e', groupId: 'g', variant: { id: 'v', type: 'holdout' } } } })"),
            )
        assertEquals(EventType.TRIGGER_FIRE, info.eventType)
        assertEquals("campaign", info.placementName)
        val result = assertIs<TriggerResult.Holdout>(info.result)
        assertEquals("e", result.experiment.id)
    }

    @Test
    fun presentationRequestMapsStatusAndReason() {
        val info =
            webEventToKmp(
                "paywallPresentationRequest",
                js("({ status: 'noPresentation', reason: { type: 'subsStatusTimeout' } })"),
            )
        assertEquals(PaywallPresentationRequestStatusType.NO_PRESENTATION, info.status)
        assertEquals(PaywallPresentationRequestStatusReason.SubscriptionStatusTimeout, info.reason)
    }

    @Test
    fun surveyResponseMapsSurveyOptionAndCustomResponse() {
        val info =
            webEventToKmp(
                "survey_response",
                js(
                    "({ survey: { id: 's', assignmentKey: 'k', title: 'Why?', message: 'm', options: [{ id: 'o1', title: 'Too pricey' }], presentationCondition: 'ON_PURCHASE', presentationProbability: 0.5, includeOtherOption: true, includeCloseOption: false }, selected_option: { id: 'o1', title: 'Too pricey' }, custom_response: null, paywall_info: { identifier: 'pw' } })",
                ),
            )
        assertEquals(EventType.SURVEY_RESPONSE, info.eventType)
        assertEquals("Too pricey", info.selectedOption?.text)
        assertEquals("Too pricey", info.survey?.options?.single()?.text)
        assertEquals(0.5, info.survey?.presentationProbability)
        assertNull(info.customResponse)
    }

    @Test
    fun failureEventsCarryTheirMessage() {
        assertEquals("boom", webEventToKmp("transaction_fail", js("({ error: 'boom' })")).error)
        assertEquals("timeout", webEventToKmp("paywallWebviewLoad_fail", js("({ errorMessage: 'timeout' })")).error)
        assertEquals("no purchases", webEventToKmp("restore_fail", js("({ reason: 'no purchases' })")).message)
    }

    @Test
    fun transactionRestoreMapsRestoreType() {
        val info = webEventToKmp("transaction_restore", js("({ restoreType: { type: 'viaRestore' } })"))
        assertEquals(RestoreType.ViaRestore, info.restoreType)
    }

    @Test
    fun attributeEventsFillTheirTypedSlots() {
        assertEquals(mapOf("os" to "web"), webEventToKmp("device_attributes", js("({ attributes: { os: 'web' } })")).deviceAttributes)
        assertEquals(mapOf("name" to "Ada"), webEventToKmp("user_attributes", js("({ attributes: { name: 'Ada' } })")).userAttributes)
        assertEquals(
            mapOf("adjustId" to "a1"),
            webEventToKmp("integration_attributes", js("({ audienceFilterParams: { adjustId: 'a1' } })")).integrationAttributes,
        )
        assertEquals("https://x.y/z", webEventToKmp("deepLink_open", js("({ uri: 'https://x.y/z' })")).deepLinkUrl)
    }

    @Test
    fun reviewAndPermissionEventsCarryCountAndName() {
        assertEquals(2L, webEventToKmp("review_requested", js("({ count: 2 })")).reviewRequestedCount)
        val permission = webEventToKmp("permission_granted", js("({ permissionName: 'notifications', paywallIdentifier: 'pw' })"))
        assertEquals(EventType.PERMISSION_GRANTED, permission.eventType)
        assertEquals("notifications", permission.name)
    }

    @Test
    fun paramsAlwaysCarryTheRawDetail() {
        val info = webEventToKmp("paywallResourceLoad_fail", js("({ url: 'https://cdn/x.css', error: '404' })"))
        assertEquals("404", info.error)
        assertTrue(info.params?.get("url") == "https://cdn/x.css")
    }
}
