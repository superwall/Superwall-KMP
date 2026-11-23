@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.internal.BridgeListener
import com.superwall.sdk.kmp.internal.inPlaceScope
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventEnvelope
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventType
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeAppOpen
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionResultCaseInvalidCode
import com.superwall.sdk.kmp.internal.mappers.toSWB
import com.superwall.sdk.kmp.internal.swbPaywallInfo
import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionResult
import kotlin.test.Test
import kotlin.test.assertEquals

class DelegateAdapterTest {
    private val listener = RecordingListener()
    private val adapter = DelegateAdapter(listener, inPlaceScope())

    @Test
    fun subscriptionStatusChangeIsForwardedWithBothStatuses() {
        val active = SubscriptionStatus.Active(setOf(Entitlement("pro")))

        adapter.subscriptionStatusDidChangeFrom(SubscriptionStatus.Inactive.toSWB(), active.toSWB())

        assertEquals(listOf<Any?>(listOf("subscriptionStatus", SubscriptionStatus.Inactive, active)), listener.calls)
    }

    @Test
    fun eventIsForwardedAsItsMappedInfo() {
        adapter.handleSuperwallEvent(swbEvent(SWBEventTypeAppOpen, params = mapOf("source" to "cold_start")))

        assertEquals(
            listOf<Any?>(SuperwallEventInfo(EventType.APP_OPEN, params = mapOf("source" to "cold_start"))),
            listener.calls,
        )
    }

    @Test
    fun paywallLifecycleCallbacksEachReachTheirOwnListenerMethod() {
        adapter.willPresentPaywall(swbPaywallInfo("paywall_a"))
        adapter.didPresentPaywall(swbPaywallInfo("paywall_b"))
        adapter.willDismissPaywall(swbPaywallInfo("paywall_c"))
        adapter.didDismissPaywall(swbPaywallInfo("paywall_d"))

        assertEquals(
            listOf<Any?>(
                "willPresent" to "paywall_a",
                "didPresent" to "paywall_b",
                "willDismiss" to "paywall_c",
                "didDismiss" to "paywall_d",
            ),
            listener.calls,
        )
    }

    @Test
    fun plainStringCallbacksAreForwarded() {
        adapter.handleCustomPaywallActionWithName("open_settings")
        adapter.paywallWillOpenURL("https://example.com")
        adapter.paywallWillOpenDeepLink("myapp://promo")
        adapter.willRedeemLink()

        assertEquals(
            listOf<Any?>(
                "customAction" to "open_settings",
                "openURL" to "https://example.com",
                "openDeepLink" to "myapp://promo",
                "willRedeemLink",
            ),
            listener.calls,
        )
    }

    @Test
    fun logIsForwardedWithTheMappedLevelAndScope() {
        adapter.handleLogWithLevel(
            level = "warn",
            scope = "paywallPresentation",
            message = "slow load",
            info = mapOf<Any?, Any>("attempt" to 2L),
            error = "timeout",
        )
        // No info and nothing to preserve: the listener gets null, not an empty map.
        adapter.handleLogWithLevel("error", "superwallCore", null, null, null)

        assertEquals(
            listOf<Any?>(
                Log(LogLevel.WARN, LogScope.PAYWALL_PRESENTATION, "slow load", mapOf("attempt" to 2L), "timeout"),
                Log(LogLevel.ERROR, LogScope.SUPERWALL_CORE, null, null, null),
            ),
            listener.calls,
        )
    }

    /** A level or scope newer than the wrapper is still delivered, raw value kept. */
    @Test
    fun anUnknownLogLevelOrScopeFallsBackAndKeepsTheRawValue() {
        adapter.handleLogWithLevel("verbose", "quantumScope", "hello", mapOf<Any?, Any>("k" to "v"), null)

        assertEquals(
            listOf<Any?>(
                Log(
                    LogLevel.DEBUG,
                    LogScope.ALL,
                    "hello",
                    mapOf("k" to "v", "rawLevel" to "verbose", "rawScope" to "quantumScope"),
                    null,
                ),
            ),
            listener.calls,
        )
    }

    @Test
    fun redeemedLinkIsForwardedWithTheMappedResult() {
        adapter.didRedeemLink(SWBRedemptionResult(SWBRedemptionResultCaseInvalidCode, "PROMO", null, null, null))

        assertEquals(listOf<Any?>(RedemptionResult.InvalidCode("PROMO")), listener.calls)
    }

    @Test
    fun deepLinkIsForwardedWithOnlyItsStringParts() {
        adapter.handleSuperwallDeepLinkWithFullURL(
            fullURL = "https://app.superwall.app/app-link/promo?code=ABC",
            pathComponents = listOf("app-link", 7, "promo"),
            queryParameters = mapOf<Any?, Any?>("code" to "ABC", "count" to 3, 4 to "four"),
        )

        assertEquals(
            listOf<Any?>(
                Triple(
                    "https://app.superwall.app/app-link/promo?code=ABC",
                    listOf("app-link", "promo"),
                    mapOf("code" to "ABC"),
                ),
            ),
            listener.calls,
        )
    }

    @Test
    fun customerInfoChangeIsForwardedWithBothInfos() {
        val entitlement = Entitlement("pro")

        adapter.customerInfoDidChangeFrom(
            SWBCustomerInfo(emptyList<Any>(), emptyList<Any>(), emptyList<Any>(), "anon"),
            SWBCustomerInfo(emptyList<Any>(), emptyList<Any>(), listOf(entitlement.toSWB()), "user_1"),
        )

        assertEquals(
            listOf<Any?>(
                CustomerInfo(emptyList(), emptyList(), emptyList(), "anon") to
                    CustomerInfo(emptyList(), emptyList(), listOf(entitlement), "user_1"),
            ),
            listener.calls,
        )
    }

    @Test
    fun userAttributesAreForwardedAsAKotlinMap() {
        adapter.userAttributesDidChange(mapOf<Any?, Any>("plan" to "pro", "age" to 30L, "nested" to mapOf("a" to true)))

        assertEquals(
            listOf<Any?>(mapOf("plan" to "pro", "age" to 30L, "nested" to mapOf("a" to true))),
            listener.calls,
        )
    }

    /**
     * SuperwallKit calls its delegate from inside its own internals; a
     * listener that throws must neither unwind into it nor stop later
     * deliveries.
     */
    @Test
    fun aThrowingListenerDoesNotReachTheCallerOrStopLaterDeliveries() {
        listener.throwOnCustomAction = IllegalStateException("app bug")

        adapter.handleCustomPaywallActionWithName("open_settings")
        adapter.paywallWillOpenURL("https://example.com")

        assertEquals(listOf<Any?>("openURL" to "https://example.com"), listener.calls)
    }

    private data class Log(
        val level: LogLevel,
        val scope: LogScope,
        val message: String?,
        val info: Map<String, Any?>?,
        val error: String?,
    )

    private class RecordingListener : BridgeListener {
        val calls = mutableListOf<Any?>()
        var throwOnCustomAction: Throwable? = null

        override val forwardsToUserDelegate: Boolean = true
        override val observesEvents: Boolean = true

        override fun subscriptionStatusDidChange(
            from: SubscriptionStatus,
            to: SubscriptionStatus,
        ) {
            calls.add(listOf("subscriptionStatus", from, to))
        }

        override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
            calls += eventInfo
        }

        override fun handleCustomPaywallAction(name: String) {
            throwOnCustomAction?.let { throw it }
            calls += "customAction" to name
        }

        override fun willDismissPaywall(paywallInfo: PaywallInfo) {
            calls += "willDismiss" to paywallInfo.identifier
        }

        override fun willPresentPaywall(paywallInfo: PaywallInfo) {
            calls += "willPresent" to paywallInfo.identifier
        }

        override fun didDismissPaywall(paywallInfo: PaywallInfo) {
            calls += "didDismiss" to paywallInfo.identifier
        }

        override fun didPresentPaywall(paywallInfo: PaywallInfo) {
            calls += "didPresent" to paywallInfo.identifier
        }

        override fun paywallWillOpenURL(url: String) {
            calls += "openURL" to url
        }

        override fun paywallWillOpenDeepLink(url: String) {
            calls += "openDeepLink" to url
        }

        override fun handleLog(
            level: LogLevel,
            scope: LogScope,
            message: String?,
            info: Map<String, Any?>?,
            error: String?,
        ) {
            calls += Log(level, scope, message, info, error)
        }

        override fun willRedeemLink() {
            calls += "willRedeemLink"
        }

        override fun didRedeemLink(result: RedemptionResult) {
            calls += result
        }

        override fun handleSuperwallDeepLink(
            fullURL: String,
            pathComponents: List<String>,
            queryParameters: Map<String, String>,
        ) {
            calls += Triple(fullURL, pathComponents, queryParameters)
        }

        override fun customerInfoDidChange(
            from: CustomerInfo,
            to: CustomerInfo,
        ) {
            calls += from to to
        }

        override fun userAttributesDidChange(newAttributes: Map<String, Any?>) {
            calls += newAttributes
        }
    }

    /** A real bridge `SWBEventEnvelope` carrying only a type and params. */
    private fun swbEvent(
        eventType: SWBEventType,
        params: Map<Any?, Any>?,
    ): SWBEventEnvelope =
        SWBEventEnvelope(
            eventType = eventType,
            params = params,
            paywallInfo = null,
            transaction = null,
            product = null,
            transactionType = null,
            transactionProductId = null,
            restoreType = null,
            triggerResult = null,
            placementName = null,
            triggeredPlacementName = null,
            deepLinkUrl = null,
            deviceAttributes = null,
            userAttributes = null,
            attempt = null,
            missingProductIdentifiers = null,
            survey = null,
            selectedOption = null,
            customResponse = null,
            presentationRequestStatus = null,
            presentationRequestReason = null,
            error = null,
            message = null,
            token = null,
            name = null,
            userEnrichment = null,
            deviceEnrichment = null,
            integrationAttributes = null,
            reviewRequestedCount = null,
            permissionName = null,
            paywallIdentifier = null,
            paywallCount = null,
            attributionMatch = null,
            pageViewData = null,
        )
}
