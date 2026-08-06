package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.SuperwallDelegate
import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionResult

/**
 * The always-installed [BridgeListener]: installed into the native SDK's
 * delegate slot exactly once at configure (idempotently on repeat configure),
 * regardless of whether the app set a [SuperwallDelegate].
 *
 * Every native delegate event is (1) fed into the [StreamHolder] flows where
 * applicable and (2) forwarded to the user's delegate if one is set. This
 * fixes the Flutter layer's `setDelegate(Boolean)` install/uninstall artifact
 * and its stream re-wrap quirk: the streams stay live whether or not a user
 * delegate exists, and setting/clearing [userDelegate] never touches the
 * native SDK.
 *
 * Platform adapters invoke this on the native SDK's own calling thread (see
 * [com.superwall.sdk.kmp.SuperwallDelegate]'s threading contract), so this
 * class must stay thread-safe: both stream feeds below are
 * (`MutableStateFlow.value`, `MutableSharedFlow.tryEmit`). Exceptions thrown
 * by the user's delegate are caught and swallowed (degrade, never crash) so
 * they cannot propagate into native SDK internals.
 */
internal class DelegateMultiplexer(
    private val streams: StreamHolder,
) : BridgeListener {
    /**
     * The app's delegate, if any. Set via `Superwall.delegate` (allowed
     * pre-configure — stored here, installed natively at configure); `null`
     * clears forwarding without uninstalling the multiplexer.
     */
    var userDelegate: SuperwallDelegate? = null

    /**
     * Only the user's delegate consumes the pure-forwarding hooks, so when
     * none is set the platform adapters can skip mapping and dispatching them
     * altogether (see [BridgeListener.forwardsToUserDelegate]).
     */
    override val forwardsToUserDelegate: Boolean
        get() = userDelegate != null

    override fun subscriptionStatusDidChange(
        from: SubscriptionStatus,
        to: SubscriptionStatus,
    ) {
        streams.subscriptionStatus.value = to
        forward { it.subscriptionStatusDidChange(from, to) }
    }

    override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
        forward { it.handleSuperwallEvent(eventInfo) }
    }

    override fun handleCustomPaywallAction(name: String) {
        forward { it.handleCustomPaywallAction(name) }
    }

    override fun willDismissPaywall(paywallInfo: PaywallInfo) {
        forward { it.willDismissPaywall(paywallInfo) }
    }

    override fun willPresentPaywall(paywallInfo: PaywallInfo) {
        forward { it.willPresentPaywall(paywallInfo) }
    }

    override fun didDismissPaywall(paywallInfo: PaywallInfo) {
        forward { it.didDismissPaywall(paywallInfo) }
    }

    override fun didPresentPaywall(paywallInfo: PaywallInfo) {
        forward { it.didPresentPaywall(paywallInfo) }
    }

    override fun paywallWillOpenURL(url: String) {
        forward { it.paywallWillOpenURL(url) }
    }

    override fun paywallWillOpenDeepLink(url: String) {
        forward { it.paywallWillOpenDeepLink(url) }
    }

    override fun handleLog(
        level: LogLevel,
        scope: LogScope,
        message: String?,
        info: Map<String, Any?>?,
        error: String?,
    ) {
        forward { it.handleLog(level, scope, message, info, error) }
    }

    override fun willRedeemLink() {
        forward { it.willRedeemLink() }
    }

    override fun didRedeemLink(result: RedemptionResult) {
        forward { it.didRedeemLink(result) }
    }

    override fun handleSuperwallDeepLink(
        fullURL: String,
        pathComponents: List<String>,
        queryParameters: Map<String, String>,
    ) {
        forward { it.handleSuperwallDeepLink(fullURL, pathComponents, queryParameters) }
    }

    override fun customerInfoDidChange(
        from: CustomerInfo,
        to: CustomerInfo,
    ) {
        // Non-suspending: the holder's buffer + DROP_OLDEST make tryEmit safe.
        streams.customerInfo.tryEmit(to)
        forward { it.customerInfoDidChange(from, to) }
    }

    override fun userAttributesDidChange(newAttributes: Map<String, Any?>) {
        forward { it.userAttributesDidChange(newAttributes) }
    }

    /**
     * Forwards to the user delegate if set, catching anything it throws —
     * user-callback exceptions must never reach native SDK internals.
     */
    private inline fun forward(block: (SuperwallDelegate) -> Unit) {
        val delegate = userDelegate ?: return
        try {
            block(delegate)
        } catch (throwable: Throwable) {
            println("[Superwall] SuperwallDelegate callback threw: $throwable")
        }
    }
}
