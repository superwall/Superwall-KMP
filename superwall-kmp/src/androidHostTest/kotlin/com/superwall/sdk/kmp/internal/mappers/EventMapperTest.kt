package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusReason
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusType
import com.superwall.sdk.kmp.models.results.TriggerResult
import com.superwall.sdk.kmp.models.triggers.VariantType
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import com.superwall.sdk.analytics.superwall.SuperwallEvent as NativeSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo as NativeSuperwallEventInfo
import com.superwall.sdk.models.customer.CustomerInfo as NativeCustomerInfo
import com.superwall.sdk.models.triggers.Experiment as NativeExperiment
import com.superwall.sdk.models.triggers.TriggerResult as NativeTriggerResult
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatus as NativePaywallPresentationRequestStatus
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason as NativePaywallPresentationRequestStatusReason

/**
 * Event-type mapping tests for EventMapper.kt over a representative sample of
 * real native `SuperwallEvent` cases that are constructible on the JVM without
 * an Android runtime (cases carrying a native `PaywallInfo`, `StoreProduct` or
 * `StoreTransaction` need device-backed objects and are exercised in
 * instrumented tests instead).
 */
class EventMapperTest {
    private fun infoOf(
        event: NativeSuperwallEvent,
        params: Map<String, Any> = emptyMap(),
    ): NativeSuperwallEventInfo = NativeSuperwallEventInfo(event = event, params = params)

    private val nativeExperiment =
        NativeExperiment(
            id = "exp-1",
            groupId = "group-1",
            variant =
                NativeExperiment.Variant(
                    id = "var-1",
                    type = NativeExperiment.Variant.VariantType.TREATMENT,
                    paywallId = "pw-1",
                ),
        )

    // ---- Simple lifecycle events ---------------------------------------------------

    @Test
    fun simpleLifecycleEvents_mapToTheirEventTypes() {
        val expectations =
            listOf<Pair<NativeSuperwallEvent, EventType>>(
                NativeSuperwallEvent.FirstSeen() to EventType.FIRST_SEEN,
                NativeSuperwallEvent.AppOpen() to EventType.APP_OPEN,
                NativeSuperwallEvent.AppLaunch() to EventType.APP_LAUNCH,
                NativeSuperwallEvent.IdentityAlias() to EventType.IDENTITY_ALIAS,
                NativeSuperwallEvent.AppInstall() to EventType.APP_INSTALL,
                NativeSuperwallEvent.SessionStart() to EventType.SESSION_START,
                NativeSuperwallEvent.ConfigAttributes to EventType.CONFIG_ATTRIBUTES,
                NativeSuperwallEvent.SubscriptionStatusDidChange() to
                    EventType.SUBSCRIPTION_STATUS_DID_CHANGE,
                NativeSuperwallEvent.AppClose() to EventType.APP_CLOSE,
                NativeSuperwallEvent.SurveyClose() to EventType.SURVEY_CLOSE,
                NativeSuperwallEvent.ConfigRefresh to EventType.CONFIG_REFRESH,
                NativeSuperwallEvent.ConfigFail to EventType.CONFIG_FAIL,
                NativeSuperwallEvent.ConfirmAllAssignments to EventType.CONFIRM_ALL_ASSIGNMENTS,
                NativeSuperwallEvent.Reset to EventType.RESET,
                NativeSuperwallEvent.ShimmerViewStart to EventType.SHIMMER_VIEW_START,
                NativeSuperwallEvent.ShimmerViewComplete(1.5) to EventType.SHIMMER_VIEW_COMPLETE,
                NativeSuperwallEvent.RedemptionStart to EventType.REDEMPTION_START,
                NativeSuperwallEvent.RedemptionComplete to EventType.REDEMPTION_COMPLETE,
                NativeSuperwallEvent.EnrichmentStart to EventType.ENRICHMENT_START,
                NativeSuperwallEvent.EnrichmentFail to EventType.ENRICHMENT_FAIL,
                NativeSuperwallEvent.Restore.Start to EventType.RESTORE_START,
                NativeSuperwallEvent.Restore.Complete to EventType.RESTORE_COMPLETE,
            )

        for ((native, expected) in expectations) {
            assertEquals(
                expected,
                infoOf(native).toKmp().eventType,
                "wrong EventType for ${native::class.simpleName}",
            )
        }
    }

    @Test
    fun params_areSanitizedIntoTheCommonEnvelope() {
        val info = infoOf(NativeSuperwallEvent.FirstSeen(), params = mapOf("count" to 3, "flag" to true))
        assertEquals(mapOf("count" to 3L, "flag" to true), info.toKmp().params)
    }

    // ---- Payload-carrying events ------------------------------------------------------

    @Test
    fun deviceAttributes_carrySanitizedAttributes() {
        val info =
            infoOf(NativeSuperwallEvent.DeviceAttributes(mapOf("ram" to 8, "model" to "Pixel")))
                .toKmp()

        assertEquals(EventType.DEVICE_ATTRIBUTES, info.eventType)
        assertEquals(mapOf("ram" to 8L, "model" to "Pixel"), info.deviceAttributes)
    }

    @Test
    fun deepLink_carriesUrlString() {
        val info = infoOf(NativeSuperwallEvent.DeepLink(URI("https://example.com/p?a=b"))).toKmp()

        assertEquals(EventType.DEEP_LINK, info.eventType)
        assertEquals("https://example.com/p?a=b", info.deepLinkUrl)
    }

    @Test
    fun triggerFire_carriesPlacementNameResultAndParams() {
        val info =
            infoOf(
                NativeSuperwallEvent.TriggerFire(
                    placementName = "campaign_trigger",
                    result = NativeTriggerResult.Paywall(nativeExperiment),
                ),
                // Plan §3.4 fix: unlike the Flutter host, params are NOT dropped.
                params = mapOf("source" to "test"),
            ).toKmp()

        assertEquals(EventType.TRIGGER_FIRE, info.eventType)
        assertEquals("campaign_trigger", info.placementName)
        assertEquals(mapOf("source" to "test"), info.params)
        val result = assertIs<TriggerResult.Paywall>(info.result)
        assertEquals("exp-1", result.experiment.id)
        assertEquals("group-1", result.experiment.groupId)
        assertEquals("var-1", result.experiment.variant.id)
        assertEquals(VariantType.TREATMENT, result.experiment.variant.type)
        assertEquals("pw-1", result.experiment.variant.paywallId)
    }

    @Test
    fun triggerFire_noAudienceMatch_mapsResult() {
        val info =
            infoOf(
                NativeSuperwallEvent.TriggerFire("p", NativeTriggerResult.NoAudienceMatch),
            ).toKmp()
        assertEquals(TriggerResult.NoAudienceMatch, info.result)
    }

    @Test
    fun userAttributes_carrySanitizedAttributes() {
        val info = infoOf(NativeSuperwallEvent.UserAttributes(mapOf("plan" to "pro"))).toKmp()

        assertEquals(EventType.USER_ATTRIBUTES, info.eventType)
        assertEquals(mapOf("plan" to "pro"), info.userAttributes)
    }

    @Test
    fun restoreFail_carriesReasonAsMessage() {
        val info = infoOf(NativeSuperwallEvent.Restore.Fail("no receipt")).toKmp()

        assertEquals(EventType.RESTORE_FAIL, info.eventType)
        assertEquals("no receipt", info.message)
    }

    @Test
    fun integrationAttributes_currentAndDeprecatedCases_bothMap() {
        val attrs = mapOf("adjust_id" to "abc")

        val current = infoOf(NativeSuperwallEvent.IntegrationAttributes(attrs)).toKmp()
        assertEquals(EventType.INTEGRATION_ATTRIBUTES, current.eventType)
        assertEquals(mapOf("adjust_id" to "abc"), current.integrationAttributes)

        @Suppress("DEPRECATION")
        val deprecated = infoOf(NativeSuperwallEvent.IntegrationProps(attrs)).toKmp()
        assertEquals(EventType.INTEGRATION_ATTRIBUTES, deprecated.eventType)
        assertEquals(mapOf("adjust_id" to "abc"), deprecated.integrationAttributes)
    }

    @Test
    fun enrichmentComplete_carriesUserAndDeviceEnrichment() {
        val info =
            infoOf(
                NativeSuperwallEvent.EnrichmentComplete(
                    userEnrichment = mapOf("segment" to "power"),
                    deviceEnrichment = mapOf("region" to "eu"),
                ),
            ).toKmp()

        assertEquals(EventType.ENRICHMENT_COMPLETE, info.eventType)
        assertEquals(mapOf("segment" to "power"), info.userEnrichment)
        assertEquals(mapOf("region" to "eu"), info.deviceEnrichment)
    }

    @Test
    fun reviewRequested_carriesCount() {
        val info = infoOf(NativeSuperwallEvent.ReviewRequested(3)).toKmp()

        assertEquals(EventType.REVIEW_REQUESTED, info.eventType)
        assertEquals(3L, info.reviewRequestedCount)
    }

    @Test
    fun customerInfoDidChange_mapsToItsEventType() {
        val customerInfo =
            NativeCustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "u1",
                entitlements = emptyList(),
            )
        val info =
            infoOf(NativeSuperwallEvent.CustomerInfoDidChange(customerInfo, customerInfo)).toKmp()
        assertEquals(EventType.CUSTOMER_INFO_DID_CHANGE, info.eventType)
    }

    @Test
    fun permissionEvents_carryPermissionName() {
        val requested =
            infoOf(NativeSuperwallEvent.PermissionRequested("camera", "pw-1")).toKmp()
        assertEquals(EventType.PERMISSION_REQUESTED, requested.eventType)
        assertEquals("camera", requested.name)

        val granted = infoOf(NativeSuperwallEvent.PermissionGranted("camera", "pw-1")).toKmp()
        assertEquals(EventType.PERMISSION_GRANTED, granted.eventType)
        assertEquals("camera", granted.name)

        val denied = infoOf(NativeSuperwallEvent.PermissionDenied("camera", "pw-1")).toKmp()
        assertEquals(EventType.PERMISSION_DENIED, denied.eventType)
        assertEquals("camera", denied.name)
    }

    @Test
    fun paywallPreloadEvents_mapToTheirEventTypes() {
        assertEquals(
            EventType.PAYWALL_PRELOAD_START,
            infoOf(NativeSuperwallEvent.PaywallPreloadStart(2)).toKmp().eventType,
        )
        assertEquals(
            EventType.PAYWALL_PRELOAD_COMPLETE,
            infoOf(NativeSuperwallEvent.PaywallPreloadComplete(2)).toKmp().eventType,
        )
    }

    @Test
    fun redemptionFail_carriesRawNameAsError() {
        val info = infoOf(NativeSuperwallEvent.RedemptionFail).toKmp()

        assertEquals(EventType.REDEMPTION_FAIL, info.eventType)
        assertEquals(NativeSuperwallEvent.RedemptionFail.rawName, info.error)
    }

    // ---- Presentation request status --------------------------------------------------

    @Test
    fun paywallPresentationRequest_mapsStatusAndNullReason() {
        val info =
            infoOf(
                NativeSuperwallEvent.PaywallPresentationRequest(
                    status = NativePaywallPresentationRequestStatus.Timeout,
                    reason = null,
                ),
            ).toKmp()

        assertEquals(EventType.PAYWALL_PRESENTATION_REQUEST, info.eventType)
        assertEquals(PaywallPresentationRequestStatusType.TIMEOUT, info.status)
        assertNull(info.reason)
    }

    @Test
    fun paywallPresentationRequest_mapsHoldoutReasonWithExperiment() {
        val info =
            infoOf(
                NativeSuperwallEvent.PaywallPresentationRequest(
                    status = NativePaywallPresentationRequestStatus.NoPresentation,
                    reason = NativePaywallPresentationRequestStatusReason.Holdout(nativeExperiment),
                ),
            ).toKmp()

        assertEquals(PaywallPresentationRequestStatusType.NO_PRESENTATION, info.status)
        val reason = assertIs<PaywallPresentationRequestStatusReason.Holdout>(info.reason)
        assertEquals("exp-1", reason.experiment.id)
    }

    // ---- Fallback: no common EventType counterpart --------------------------------------

    @Test
    fun eventsWithoutCommonCounterpart_degradeToCustomPlacementWithRawName() {
        val event = NativeSuperwallEvent.TestModeModalOpen()
        val info = infoOf(event, params = mapOf("k" to "v")).toKmp()

        assertEquals(EventType.CUSTOM_PLACEMENT, info.eventType)
        assertEquals(event.rawName, info.name)
        assertEquals(mapOf("k" to "v"), info.params)
    }

    @Test
    fun reviewGrantedAndDenied_degradeToFallbackButKeepCount() {
        val granted = infoOf(NativeSuperwallEvent.ReviewGranted(4)).toKmp()
        assertEquals(EventType.CUSTOM_PLACEMENT, granted.eventType)
        assertEquals("review_granted", granted.name)
        assertEquals(4L, granted.reviewRequestedCount)

        val denied = infoOf(NativeSuperwallEvent.ReviewDenied(5)).toKmp()
        assertEquals(EventType.CUSTOM_PLACEMENT, denied.eventType)
        assertEquals("review_denied", denied.name)
        assertEquals(5L, denied.reviewRequestedCount)
    }
}
