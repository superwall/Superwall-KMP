package com.superwall.sdk.kmp.sample

import androidx.compose.runtime.mutableStateListOf
import com.superwall.sdk.kmp.SuperwallDelegate
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.paywall.PaywallInfo

/**
 * Port of the Flutter test_app's `TestDelegateEvent` sealed class: one variant
 * per [SuperwallDelegate] callback the Delegate Test screen records.
 */
sealed interface TestDelegateEvent {
    data class DidDismissPaywall(val paywallInfo: PaywallInfo) : TestDelegateEvent
    data class DidPresentPaywall(val paywallInfo: PaywallInfo) : TestDelegateEvent
    data class HandleCustomPaywallAction(val name: String) : TestDelegateEvent
    data class HandleLog(
        val level: LogLevel,
        val scope: LogScope,
        val message: String?,
        val info: Map<String, Any?>?,
        val error: String?,
    ) : TestDelegateEvent
    data class HandleSuperwallEvent(val eventInfo: SuperwallEventInfo) : TestDelegateEvent
    data class PaywallWillOpenDeepLink(val url: String) : TestDelegateEvent
    data class PaywallWillOpenURL(val url: String) : TestDelegateEvent
    data class SubscriptionStatusDidChange(
        val from: SubscriptionStatus,
        val to: SubscriptionStatus,
    ) : TestDelegateEvent
    data class WillDismissPaywall(val paywallInfo: PaywallInfo) : TestDelegateEvent
    data class WillPresentPaywall(val paywallInfo: PaywallInfo) : TestDelegateEvent
    data class HandleSuperwallDeepLink(
        val fullURL: String,
        val pathComponents: List<String>,
        val queryParameters: Map<String, String>,
    ) : TestDelegateEvent
}

/**
 * Port of the Flutter test_app's `TestDelegate`: records every delegate
 * callback into [events] for the Delegate Test screen to display.
 *
 * The list is Compose state so event-count/list UI recomposes as callbacks
 * arrive (all delegate callbacks are delivered on the main thread).
 */
class TestDelegate : SuperwallDelegate {
    val events = mutableStateListOf<TestDelegateEvent>()

    /** Events excluding the high-volume log and analytics-event callbacks. */
    val eventsWithoutLog: List<TestDelegateEvent>
        get() = events.filter {
            it !is TestDelegateEvent.HandleLog && it !is TestDelegateEvent.HandleSuperwallEvent
        }

    override fun didDismissPaywall(paywallInfo: PaywallInfo) {
        events += TestDelegateEvent.DidDismissPaywall(paywallInfo)
    }

    override fun didPresentPaywall(paywallInfo: PaywallInfo) {
        events += TestDelegateEvent.DidPresentPaywall(paywallInfo)
    }

    override fun handleCustomPaywallAction(name: String) {
        events += TestDelegateEvent.HandleCustomPaywallAction(name)
    }

    override fun handleLog(
        level: LogLevel,
        scope: LogScope,
        message: String?,
        info: Map<String, Any?>?,
        error: String?,
    ) {
        events += TestDelegateEvent.HandleLog(level, scope, message, info, error)
    }

    override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
        events += TestDelegateEvent.HandleSuperwallEvent(eventInfo)
    }

    override fun paywallWillOpenDeepLink(url: String) {
        events += TestDelegateEvent.PaywallWillOpenDeepLink(url)
    }

    override fun paywallWillOpenURL(url: String) {
        events += TestDelegateEvent.PaywallWillOpenURL(url)
    }

    override fun subscriptionStatusDidChange(
        from: SubscriptionStatus,
        to: SubscriptionStatus,
    ) {
        events += TestDelegateEvent.SubscriptionStatusDidChange(from, to)
    }

    override fun willDismissPaywall(paywallInfo: PaywallInfo) {
        events += TestDelegateEvent.WillDismissPaywall(paywallInfo)
    }

    override fun willPresentPaywall(paywallInfo: PaywallInfo) {
        events += TestDelegateEvent.WillPresentPaywall(paywallInfo)
    }

    override fun handleSuperwallDeepLink(
        fullURL: String,
        pathComponents: List<String>,
        queryParameters: Map<String, String>,
    ) {
        events += TestDelegateEvent.HandleSuperwallDeepLink(fullURL, pathComponents, queryParameters)
    }
}

/**
 * Shared instance: Superwall.delegate holds whatever was last set, so the
 * recorded events survive navigating away from and back to the Delegate Test
 * screen.
 */
val testDelegate: TestDelegate = TestDelegate()
