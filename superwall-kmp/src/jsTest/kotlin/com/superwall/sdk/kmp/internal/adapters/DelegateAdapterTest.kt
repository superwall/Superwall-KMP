package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.internal.BridgeListener
import com.superwall.sdk.kmp.internal.redemptionCodeIn
import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.redemption.Ownership
import com.superwall.sdk.kmp.models.redemption.RedemptionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DelegateAdapterTest {
    private class RecordingListener(
        override var forwardsToUserDelegate: Boolean = true,
    ) : BridgeListener {
        val calls = mutableListOf<String>()
        val events = mutableListOf<SuperwallEventInfo>()
        val statusChanges = mutableListOf<Pair<SubscriptionStatus, SubscriptionStatus>>()
        val redemptions = mutableListOf<RedemptionResult>()
        var lastLog: List<Any?>? = null

        override fun subscriptionStatusDidChange(
            from: SubscriptionStatus,
            to: SubscriptionStatus,
        ) {
            statusChanges += from to to
        }

        override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
            events += eventInfo
        }

        override fun handleCustomPaywallAction(name: String) {
            calls += "customAction:$name"
        }

        override fun willDismissPaywall(paywallInfo: PaywallInfo) {
            calls += "willDismiss:${paywallInfo.identifier}"
        }

        override fun willPresentPaywall(paywallInfo: PaywallInfo) {
            calls += "willPresent:${paywallInfo.identifier}"
        }

        override fun didDismissPaywall(paywallInfo: PaywallInfo) {
            calls += "didDismiss:${paywallInfo.identifier}"
        }

        override fun didPresentPaywall(paywallInfo: PaywallInfo) {
            calls += "didPresent:${paywallInfo.identifier}"
        }

        override fun paywallWillOpenURL(url: String) {
            calls += "openURL:$url"
        }

        override fun paywallWillOpenDeepLink(url: String) {
            calls += "openDeepLink:$url"
        }

        override fun handleLog(
            level: LogLevel,
            scope: LogScope,
            message: String?,
            info: Map<String, Any?>?,
            error: String?,
        ) {
            lastLog = listOf(level, scope, message, info, error)
        }

        override fun willRedeemLink() {
            calls += "willRedeem"
        }

        override fun didRedeemLink(result: RedemptionResult) {
            redemptions += result
        }

        override fun handleSuperwallDeepLink(
            fullURL: String,
            pathComponents: List<String>,
            queryParameters: Map<String, String>,
        ) {
            calls += "superwallDeepLink"
        }

        override fun customerInfoDidChange(
            from: CustomerInfo,
            to: CustomerInfo,
        ) {
            calls += "customerInfo:${to.userId}"
        }

        override fun userAttributesDidChange(newAttributes: Map<String, Any?>) {
            calls += "attributes:$newAttributes"
        }
    }

    @Test
    fun forwardsLifecycleHooks() {
        val listener = RecordingListener()
        val d = DelegateAdapter(listener) { "user_1" }.jsDelegate
        val info: dynamic = js("({ identifier: 'pw' })")
        d.onPaywallWillPresent(info)
        d.onPaywallDidPresent(info)
        d.onPaywallWillDismiss(info)
        d.onPaywallDidDismiss(info)
        d.onPaywallWillOpenURL("https://x")
        d.onPaywallWillOpenDeepLink("app://y")
        d.onCustomPaywallAction("help")
        d.onWillRedeemLink()
        d.onUserAttributesChange(js("({ name: 'Ada' })"))
        assertEquals(
            listOf(
                "willPresent:pw",
                "didPresent:pw",
                "willDismiss:pw",
                "didDismiss:pw",
                "openURL:https://x",
                "openDeepLink:app://y",
                "customAction:help",
                "willRedeem",
                "attributes:{name=Ada}",
            ),
            listener.calls,
        )
    }

    @Test
    fun skipsForwardingHooksWithoutAUserDelegateButKeepsStreamFeeds() {
        val listener = RecordingListener(forwardsToUserDelegate = false)
        val d = DelegateAdapter(listener) { "user_1" }.jsDelegate
        d.onPaywallDidPresent(js("({ identifier: 'pw' })"))
        d.onEvent("app_open", js("({})"))
        d.onLog("info", "network", "hello", null, null)
        d.onSubscriptionStatusChange(js("({ status: 'UNKNOWN' })"), js("({ status: 'INACTIVE' })"))
        d.onCustomerInfoChange(
            js("({ userId: 'a', subscriptions: [], nonSubscriptions: [], entitlements: [] })"),
            js("({ userId: 'b', subscriptions: [], nonSubscriptions: [], entitlements: [] })"),
        )
        assertEquals(listOf("customerInfo:b"), listener.calls)
        assertTrue(listener.events.isEmpty())
        assertNull(listener.lastLog)
        assertEquals(listOf<Pair<SubscriptionStatus, SubscriptionStatus>>(SubscriptionStatus.Unknown to SubscriptionStatus.Inactive), listener.statusChanges)
    }

    @Test
    fun mapsEventsThroughTheEventMapper() {
        val listener = RecordingListener()
        DelegateAdapter(listener) { "user_1" }.jsDelegate.onEvent("paywall_open", js("({ paywall_info: { identifier: 'pw' } })"))
        assertEquals(EventType.PAYWALL_OPEN, listener.events.single().eventType)
    }

    @Test
    fun unmappableLogLevelAndScopeKeepTheRawValues() {
        val listener = RecordingListener()
        DelegateAdapter(listener) { "user_1" }.jsDelegate.onLog("trace", "brandNewScope", "msg", js("({ k: 1 })"), "err")
        assertEquals(
            listOf<Any?>(LogLevel.DEBUG, LogScope.ALL, "msg", mapOf("k" to 1L, "rawLevel" to "trace", "rawScope" to "brandNewScope"), "err"),
            listener.lastLog,
        )
    }

    @Test
    fun redemptionUsesTheCurrentUserForOwnership() {
        val listener = RecordingListener()
        DelegateAdapter(listener) { "user_42" }.jsDelegate.onDidRedeemLink(js("({ type: 'success', code: 'redemption_x', entitlements: [] })"))
        val success = assertIs<RedemptionResult.Success>(listener.redemptions.single())
        assertEquals(Ownership.AppUser("user_42"), success.redemptionInfo.ownership)
    }

    @Test
    fun aThrowingMapperDoesNotEscapeIntoTheWebSdk() {
        val listener =
            object : BridgeListener by RecordingListener() {
                override fun didPresentPaywall(paywallInfo: PaywallInfo): Unit = throw IllegalStateException("app bug")
            }
        // Must not throw.
        DelegateAdapter(listener) { "u" }.jsDelegate.onPaywallDidPresent(js("({ identifier: 'pw' })"))
    }

    @Test
    fun extractsRedemptionCodesFromDeepLinks() {
        assertEquals("redemption_abc", redemptionCodeIn("https://app.example/welcome?code=redemption_abc&x=1"))
        assertEquals("redemption_a b", redemptionCodeIn("myapp://open?code=redemption_a%20b#frag"))
        assertNull(redemptionCodeIn("https://app.example/?code=promo_123"))
        assertNull(redemptionCodeIn("https://app.example/superwall/preview?paywall_id=1"))
    }
}
