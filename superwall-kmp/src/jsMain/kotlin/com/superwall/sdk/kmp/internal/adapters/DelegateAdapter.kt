package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.internal.BridgeListener
import com.superwall.sdk.kmp.internal.interop.JsAnySanitizer
import com.superwall.sdk.kmp.internal.interop.jsObject
import com.superwall.sdk.kmp.internal.mappers.customerInfoFromJs
import com.superwall.sdk.kmp.internal.mappers.logLevelFromJs
import com.superwall.sdk.kmp.internal.mappers.logScopeFromJs
import com.superwall.sdk.kmp.internal.mappers.paywallInfoFromJs
import com.superwall.sdk.kmp.internal.mappers.redemptionResultFromJs
import com.superwall.sdk.kmp.internal.mappers.subscriptionStatusFromJs
import com.superwall.sdk.kmp.internal.mappers.webEventToKmp
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope

/**
 * Builds the web `SuperwallDelegate` object installed via `setDelegate`,
 * mapping each hook onto the [listener] (the common `DelegateMultiplexer`).
 *
 * JS is single-threaded, so every hook already runs on the main thread. The
 * pure-forwarding hooks are skipped entirely while
 * [BridgeListener.forwardsToUserDelegate] is `false`, like the native
 * adapters — web's `onLog` fires for every internal log line. The
 * stream-feeding hooks (`onSubscriptionStatusChange`, `onCustomerInfoChange`)
 * always go through.
 *
 * Web-only hooks with no common counterpart (`onCheckoutCompleted`,
 * `onRedemptionCodesReceived`) are not installed; their information is also
 * delivered as events (`transaction_complete`) and in `PaywallResult`.
 *
 * @param currentUserId Reads the current user id, used to fill the
 * ownership fields web's redemption result does not carry.
 */
internal class DelegateAdapter(
    val listener: BridgeListener,
    private val currentUserId: () -> String,
) {
    val jsDelegate: dynamic = buildDelegate()

    private fun buildDelegate(): dynamic {
        val d = jsObject()
        d.onSubscriptionStatusChange = { from: dynamic, to: dynamic ->
            guard("onSubscriptionStatusChange") {
                listener.subscriptionStatusDidChange(subscriptionStatusFromJs(from), subscriptionStatusFromJs(to))
            }
        }
        d.onCustomerInfoChange = { from: dynamic, to: dynamic ->
            guard("onCustomerInfoChange") {
                listener.customerInfoDidChange(customerInfoFromJs(from), customerInfoFromJs(to))
            }
        }
        d.onUserAttributesChange = { attributes: dynamic ->
            forwarding("onUserAttributesChange") {
                listener.userAttributesDidChange(JsAnySanitizer.fromObject(attributes) ?: emptyMap())
            }
        }
        d.onPaywallWillPresent = { info: dynamic ->
            forwarding("onPaywallWillPresent") { listener.willPresentPaywall(paywallInfoFromJs(info)) }
        }
        d.onPaywallDidPresent = { info: dynamic ->
            forwarding("onPaywallDidPresent") { listener.didPresentPaywall(paywallInfoFromJs(info)) }
        }
        d.onPaywallWillDismiss = { info: dynamic ->
            forwarding("onPaywallWillDismiss") { listener.willDismissPaywall(paywallInfoFromJs(info)) }
        }
        d.onPaywallDidDismiss = { info: dynamic ->
            forwarding("onPaywallDidDismiss") { listener.didDismissPaywall(paywallInfoFromJs(info)) }
        }
        d.onPaywallWillOpenURL = { url: String ->
            forwarding("onPaywallWillOpenURL") { listener.paywallWillOpenURL(url) }
        }
        d.onPaywallWillOpenDeepLink = { url: String ->
            forwarding("onPaywallWillOpenDeepLink") { listener.paywallWillOpenDeepLink(url) }
        }
        d.onCustomPaywallAction = { name: String ->
            forwarding("onCustomPaywallAction") { listener.handleCustomPaywallAction(name) }
        }
        d.onWillRedeemLink = {
            forwarding("onWillRedeemLink") { listener.willRedeemLink() }
        }
        d.onDidRedeemLink = { result: dynamic ->
            forwarding("onDidRedeemLink") { listener.didRedeemLink(redemptionResultFromJs(result, currentUserId())) }
        }
        d.onLog = { level: String?, scope: String?, message: String?, info: dynamic, error: String? ->
            forwarding("onLog") { handleLog(level, scope, message, info, error) }
        }
        d.onEvent = { name: String, detail: dynamic ->
            forwarding("onEvent") { listener.handleSuperwallEvent(webEventToKmp(name, detail)) }
        }
        return d
    }

    /** Unmappable level/scope strings degrade to DEBUG/ALL with the raw value kept, as on native. */
    private fun handleLog(
        rawLevel: String?,
        rawScope: String?,
        message: String?,
        info: dynamic,
        error: String?,
    ) {
        val level = logLevelFromJs(rawLevel)
        val scope = logScopeFromJs(rawScope)
        var mappedInfo = JsAnySanitizer.fromObject(info)
        if (level == null || scope == null) {
            val withRaw = LinkedHashMap(mappedInfo.orEmpty())
            if (level == null) withRaw["rawLevel"] = rawLevel
            if (scope == null) withRaw["rawScope"] = rawScope
            mappedInfo = withRaw
        }
        listener.handleLog(level ?: LogLevel.DEBUG, scope ?: LogScope.ALL, message, mappedInfo, error)
    }

    private inline fun forwarding(
        hook: String,
        block: () -> Unit,
    ) {
        if (!listener.forwardsToUserDelegate) return
        guard(hook, block)
    }

    /**
     * Web already swallows delegate exceptions, but a mapping failure should
     * be visible, so it is reported to the console here instead.
     */
    private inline fun guard(
        hook: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (throwable: Throwable) {
            console.warn("[Superwall] delegate $hook failed: $throwable")
        }
    }
}
