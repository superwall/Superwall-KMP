package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusReason
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusType
import com.superwall.sdk.kmp.models.results.TriggerResult
import com.superwall.sdk.kmp.models.triggers.VariantType
import java.net.URI
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterAction
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterRefundStatus
import com.superwall.sdk.kmp.models.events.AttributionMatchInfo
import com.superwall.sdk.kmp.models.events.CustomerCenterEventInfo
import com.superwall.sdk.kmp.models.events.CustomerCenterScreenType
import com.superwall.sdk.kmp.models.events.PageViewData
import com.superwall.sdk.kmp.models.events.WebviewError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import com.superwall.sdk.analytics.superwall.AttributionMatchInfo as NativeAttributionMatchInfo
import com.superwall.sdk.analytics.superwall.SuperwallEvent as NativeSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo as NativeSuperwallEventInfo
import com.superwall.sdk.customercenter.CustomerCenterAction as NativeCustomerCenterAction
import com.superwall.sdk.customercenter.CustomerCenterRefundStatus as NativeCustomerCenterRefundStatus
import com.superwall.sdk.customercenter.CustomerCenterScreenType as NativeCustomerCenterScreenType
import com.superwall.sdk.models.customer.CustomerInfo as NativeCustomerInfo
import com.superwall.sdk.models.triggers.Experiment as NativeExperiment
import com.superwall.sdk.models.triggers.TriggerResult as NativeTriggerResult
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatus as NativePaywallPresentationRequestStatus
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason as NativePaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.view.webview.WebviewError as NativeWebviewError
import com.superwall.sdk.paywall.view.webview.messaging.PageViewData as NativePageViewData

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
                // fix: unlike the Flutter host, params are NOT dropped.
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
    fun permissionEvents_carryPermissionNameAndPaywallIdentifier() {
        val expected = mapOf("k" to "v", "permissionName" to "camera", "paywallIdentifier" to "pw-1")

        val requested =
            infoOf(NativeSuperwallEvent.PermissionRequested("camera", "pw-1"), mapOf("k" to "v")).toKmp()
        assertEquals(EventType.PERMISSION_REQUESTED, requested.eventType)
        assertEquals("camera", requested.name)
        assertEquals(expected, requested.params)

        val granted =
            infoOf(NativeSuperwallEvent.PermissionGranted("camera", "pw-1"), mapOf("k" to "v")).toKmp()
        assertEquals(EventType.PERMISSION_GRANTED, granted.eventType)
        assertEquals("camera", granted.name)
        assertEquals(expected, granted.params)

        val denied =
            infoOf(NativeSuperwallEvent.PermissionDenied("camera", "pw-1"), mapOf("k" to "v")).toKmp()
        assertEquals(EventType.PERMISSION_DENIED, denied.eventType)
        assertEquals("camera", denied.name)
        assertEquals(expected, denied.params)
    }

    @Test
    fun paywallPreloadEvents_carryPaywallCount() {
        val start = infoOf(NativeSuperwallEvent.PaywallPreloadStart(2)).toKmp()
        assertEquals(EventType.PAYWALL_PRELOAD_START, start.eventType)
        assertEquals(mapOf<String, Any?>("paywallCount" to 2L), start.params)

        val complete = infoOf(NativeSuperwallEvent.PaywallPreloadComplete(3)).toKmp()
        assertEquals(EventType.PAYWALL_PRELOAD_COMPLETE, complete.eventType)
        assertEquals(mapOf<String, Any?>("paywallCount" to 3L), complete.params)
    }

    @Test
    fun paywallResourceLoadFail_carriesUrlAndError() {
        val info =
            infoOf(NativeSuperwallEvent.PaywallResourceLoadFail("https://a.b/c.js", "404")).toKmp()

        assertEquals(EventType.PAYWALL_RESOURCE_LOAD_FAIL, info.eventType)
        assertEquals("404", info.error)
        assertEquals(mapOf<String, Any?>("url" to "https://a.b/c.js"), info.params)
    }

    @Test
    fun shimmerViewComplete_carriesDuration() {
        val info = infoOf(NativeSuperwallEvent.ShimmerViewComplete(1.5)).toKmp()

        assertEquals(EventType.SHIMMER_VIEW_COMPLETE, info.eventType)
        assertEquals(mapOf<String, Any?>("duration" to 1.5), info.params)
    }

    @Test
    fun attributionMatch_mapsToItsEventTypeWithTheMatchInParams() {
        val info =
            infoOf(
                NativeSuperwallEvent.AttributionMatch(
                    NativeAttributionMatchInfo(
                        provider = NativeAttributionMatchInfo.Provider.MMP,
                        matched = true,
                        source = "meta",
                        confidence = NativeAttributionMatchInfo.Confidence.HIGH,
                        matchScore = 0.9,
                    ),
                ),
            ).toKmp()

        assertEquals(EventType.ATTRIBUTION_MATCH, info.eventType)
        assertNull(info.name)
        assertEquals(
            mapOf<String, Any?>(
                "attributionMatch" to
                    mapOf(
                        "provider" to "mmp",
                        "matched" to true,
                        "source" to "meta",
                        "confidence" to "high",
                        "matchScore" to 0.9,
                    ),
            ),
            info.params,
        )
    }

    @Test
    fun pageViewData_isKeyedLikeTheIosMapper() {
        val data =
            NativePageViewData(
                pageNodeId = "node-2",
                flowPosition = 1,
                pageName = "Plans",
                navigationNodeId = "nav-1",
                previousPageNodeId = "node-1",
                previousFlowPosition = 0,
                navigationType = "forward",
                timeOnPreviousPageMs = 1200,
            )

        assertEquals(
            mapOf<String, Any?>(
                "pageNodeId" to "node-2",
                "flowPosition" to 1L,
                "pageName" to "Plans",
                "navigationNodeId" to "nav-1",
                "previousPageNodeId" to "node-1",
                "previousFlowPosition" to 0L,
                "navigationType" to "forward",
                "timeOnPreviousPageMs" to 1200L,
            ),
            data.toParamsMap(),
        )
        // Absent optionals are left out rather than written as null.
        assertEquals(
            setOf("pageNodeId", "flowPosition", "pageName", "navigationNodeId", "navigationType"),
            data.copy(previousPageNodeId = null, previousFlowPosition = null, timeOnPreviousPageMs = null)
                .toParamsMap()
                .keys,
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

    // ---- Events that used to fall back to CUSTOM_PLACEMENT ---------------------------------

    @Test
    fun testModeModalEvents_mapToTheirEventTypes() {
        val open = infoOf(NativeSuperwallEvent.TestModeModalOpen(), params = mapOf("k" to "v")).toKmp()
        assertEquals(EventType.TEST_MODE_MODAL_OPEN, open.eventType)
        assertNull(open.name)
        assertEquals(mapOf("k" to "v"), open.params)

        val close = infoOf(NativeSuperwallEvent.TestModeModalClose()).toKmp()
        assertEquals(EventType.TEST_MODE_MODAL_CLOSE, close.eventType)
    }

    @Test
    fun reviewGrantedAndDenied_mapToTheirEventTypesWithCount() {
        val granted = infoOf(NativeSuperwallEvent.ReviewGranted(4)).toKmp()
        assertEquals(EventType.REVIEW_GRANTED, granted.eventType)
        assertNull(granted.name)
        assertEquals(4L, granted.reviewRequestedCount)

        val denied = infoOf(NativeSuperwallEvent.ReviewDenied(5)).toKmp()
        assertEquals(EventType.REVIEW_DENIED, denied.eventType)
        assertNull(denied.name)
        assertEquals(5L, denied.reviewRequestedCount)
    }

    @Test
    fun customerCenterEvents_mapToTheirEventTypes() {
        val expectations =
            listOf<Pair<NativeSuperwallEvent, EventType>>(
                NativeSuperwallEvent.CustomerCenterOpen(NativeCustomerCenterScreenType.MANAGEMENT) to
                    EventType.CUSTOMER_CENTER_OPEN,
                NativeSuperwallEvent.CustomerCenterClose() to EventType.CUSTOMER_CENTER_CLOSE,
                NativeSuperwallEvent.CustomerCenterAction(
                    action = NativeCustomerCenterAction.Refund,
                    pathId = "path-1",
                    productId = "pro",
                ) to EventType.CUSTOMER_CENTER_ACTION,
                NativeSuperwallEvent.CustomerCenterSurveyResponse(
                    surveyId = "survey-1",
                    optionId = "option-1",
                    action = NativeCustomerCenterAction.Refund,
                    pathId = "path-1",
                    productId = null,
                ) to EventType.CUSTOMER_CENTER_SURVEY_RESPONSE,
                NativeSuperwallEvent.CustomerCenterRefundRequest(
                    productId = "pro",
                    status = NativeCustomerCenterRefundStatus.SUCCESS,
                ) to EventType.CUSTOMER_CENTER_REFUND_REQUEST,
            )

        for ((native, expected) in expectations) {
            val info = infoOf(native, params = mapOf("k" to "v")).toKmp()
            assertEquals(expected, info.eventType, "wrong EventType for ${native.rawName}")
            assertNull(info.name)
            assertEquals(mapOf("k" to "v"), info.params)
        }
    }

    // ---- Typed payload fields ---------------------------------------------------------

    @Test
    fun customerInfoDidChange_carriesFromAndTo() {
        fun customerInfo(userId: String) =
            NativeCustomerInfo(subscriptions = emptyList(), nonSubscriptions = emptyList(), userId = userId, entitlements = emptyList())

        val info = infoOf(NativeSuperwallEvent.CustomerInfoDidChange(customerInfo("before"), customerInfo("after"))).toKmp()

        assertEquals("before", info.previousCustomerInfo?.userId)
        assertEquals("after", info.customerInfo?.userId)
    }

    @Test
    fun permissionAndPreloadEvents_carryTypedFields() {
        val requested = infoOf(NativeSuperwallEvent.PermissionRequested("camera", "pw-1")).toKmp()
        assertEquals("camera", requested.permissionName)
        assertEquals("pw-1", requested.paywallIdentifier)

        assertEquals(2, infoOf(NativeSuperwallEvent.PaywallPreloadStart(2)).toKmp().paywallCount)
        assertEquals(3, infoOf(NativeSuperwallEvent.PaywallPreloadComplete(3)).toKmp().paywallCount)
    }

    @Test
    fun resourceLoadFailAndShimmerComplete_carryTypedFields() {
        assertEquals(
            "https://a.b/c.js",
            infoOf(NativeSuperwallEvent.PaywallResourceLoadFail("https://a.b/c.js", "404")).toKmp().resourceUrl,
        )
        assertEquals(1.5, infoOf(NativeSuperwallEvent.ShimmerViewComplete(1.5)).toKmp().shimmerDuration)
    }

    @Test
    fun attributionMatch_carriesTypedInfo() {
        val info =
            infoOf(
                NativeSuperwallEvent.AttributionMatch(
                    NativeAttributionMatchInfo(
                        provider = NativeAttributionMatchInfo.Provider.MMP,
                        matched = true,
                        source = "meta",
                        confidence = NativeAttributionMatchInfo.Confidence.HIGH,
                        matchScore = 0.9,
                    ),
                ),
            ).toKmp()

        assertEquals(
            AttributionMatchInfo(provider = "mmp", matched = true, source = "meta", confidence = "high", matchScore = 0.9),
            info.attributionMatch,
        )
    }

    @Test
    fun pageViewData_mapsToTheTypedModel() {
        val data =
            NativePageViewData(
                pageNodeId = "node-2",
                flowPosition = 1,
                pageName = "Plans",
                navigationNodeId = "nav-1",
                previousPageNodeId = "node-1",
                previousFlowPosition = 0,
                navigationType = "forward",
                timeOnPreviousPageMs = 1200,
            )

        assertEquals(
            PageViewData(
                pageNodeId = "node-2",
                flowPosition = 1,
                pageName = "Plans",
                navigationNodeId = "nav-1",
                previousPageNodeId = "node-1",
                previousFlowPosition = 0,
                navigationType = "forward",
                timeOnPreviousPageMs = 1200L,
            ),
            data.toKmp(),
        )
    }

    @Test
    fun webviewErrors_mapEveryNativeCase() {
        assertEquals(
            WebviewError.NetworkError(code = -2, description = "no host", url = "https://a.b"),
            NativeWebviewError.NetworkError(-2, "no host", "https://a.b").toKmp(),
        )
        assertEquals(WebviewError.Timeout("slow"), NativeWebviewError.Timeout("slow").toKmp())
        assertEquals(WebviewError.MaxAttemptsReached(listOf("a")), NativeWebviewError.MaxAttemptsReached(listOf("a")).toKmp())
        assertEquals(WebviewError.NoUrls, NativeWebviewError.NoUrls.toKmp())
        assertEquals(WebviewError.AllUrlsFailed(listOf("a", "b")), NativeWebviewError.AllUrlsFailed(listOf("a", "b")).toKmp())
    }

    @Test
    fun customerCenterEvents_carryTheirPayloads() {
        assertEquals(
            CustomerCenterEventInfo(screen = CustomerCenterScreenType.NO_PURCHASES),
            infoOf(NativeSuperwallEvent.CustomerCenterOpen(NativeCustomerCenterScreenType.NO_PURCHASES)).toKmp().customerCenter,
        )
        assertNull(infoOf(NativeSuperwallEvent.CustomerCenterClose()).toKmp().customerCenter)
        assertEquals(
            CustomerCenterEventInfo(action = CustomerCenterAction.Refund, pathId = "path-1", productId = "pro"),
            infoOf(
                NativeSuperwallEvent.CustomerCenterAction(action = NativeCustomerCenterAction.Refund, pathId = "path-1", productId = "pro"),
            ).toKmp().customerCenter,
        )
        assertEquals(
            CustomerCenterEventInfo(
                surveyId = "survey-1",
                optionId = "option-1",
                action = CustomerCenterAction.Refund,
                pathId = "path-1",
            ),
            infoOf(
                NativeSuperwallEvent.CustomerCenterSurveyResponse(
                    surveyId = "survey-1",
                    optionId = "option-1",
                    action = NativeCustomerCenterAction.Refund,
                    pathId = "path-1",
                    productId = null,
                ),
            ).toKmp().customerCenter,
        )
        assertEquals(
            CustomerCenterEventInfo(productId = "pro", refundStatus = CustomerCenterRefundStatus.USER_CANCELLED),
            infoOf(
                NativeSuperwallEvent.CustomerCenterRefundRequest(
                    productId = "pro",
                    status = NativeCustomerCenterRefundStatus.USER_CANCELLED,
                ),
            ).toKmp().customerCenter,
        )
    }
}
