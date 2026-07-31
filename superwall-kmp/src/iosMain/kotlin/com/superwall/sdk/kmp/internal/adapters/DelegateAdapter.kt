@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.internal.BridgeListener
import com.superwall.sdk.kmp.internal.interop.NSAnySanitizer
import com.superwall.sdk.kmp.internal.ios.interop.SWBBridgeDelegateProtocol
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventEnvelope
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBSubscriptionStatus
import com.superwall.sdk.kmp.internal.mappers.logLevelFromRawName
import com.superwall.sdk.kmp.internal.mappers.logScopeFromRawName
import com.superwall.sdk.kmp.internal.mappers.toModel
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.darwin.NSObject

/**
 * The Kotlin implementation of the bridge's `@objc(SWBBridgeDelegate)`
 * protocol: installed into `SWBSuperwallBridge.setDelegate` and forwarding
 * every native delegate callback to the common [BridgeListener] (the
 * [com.superwall.sdk.kmp.internal.DelegateMultiplexer]).
 *
 * Threading (plan §6): SuperwallKit invokes its delegate synchronously on the
 * main actor; each forward hops through [scope] —
 * `Dispatchers.Main.immediate` — so delivery stays on the main thread
 * (executing inline when already there) and a throwing listener can never
 * propagate back into native SDK internals (the multiplexer additionally
 * guards the user delegate; the launch + supervisor scope guard the rest).
 *
 * Retention: the [com.superwall.sdk.kmp.internal.IosSuperwallBridge] strongly
 * retains this adapter — never rely on ObjC retaining Kotlin-implemented
 * objects (plan §6.4).
 */
internal class DelegateAdapter(
    internal val listener: BridgeListener,
    private val scope: CoroutineScope,
) : NSObject(), SWBBridgeDelegateProtocol {
    private fun deliver(block: () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (throwable: Throwable) {
                println("[Superwall] BridgeListener callback threw: $throwable")
            }
        }
    }

    override fun subscriptionStatusDidChangeFrom(
        oldStatus: SWBSubscriptionStatus,
        to: SWBSubscriptionStatus,
    ) {
        val from = oldStatus.toModel()
        val new = to.toModel()
        deliver { listener.subscriptionStatusDidChange(from, new) }
    }

    override fun handleSuperwallEvent(envelope: SWBEventEnvelope) {
        val eventInfo = envelope.toModel()
        deliver { listener.handleSuperwallEvent(eventInfo) }
    }

    override fun handleCustomPaywallActionWithName(name: String) {
        deliver { listener.handleCustomPaywallAction(name) }
    }

    override fun willPresentPaywall(paywallInfo: SWBPaywallInfo) {
        val info = paywallInfo.toModel()
        deliver { listener.willPresentPaywall(info) }
    }

    override fun didPresentPaywall(paywallInfo: SWBPaywallInfo) {
        val info = paywallInfo.toModel()
        deliver { listener.didPresentPaywall(info) }
    }

    override fun willDismissPaywall(paywallInfo: SWBPaywallInfo) {
        val info = paywallInfo.toModel()
        deliver { listener.willDismissPaywall(info) }
    }

    override fun didDismissPaywall(paywallInfo: SWBPaywallInfo) {
        val info = paywallInfo.toModel()
        deliver { listener.didDismissPaywall(info) }
    }

    override fun paywallWillOpenURL(url: String) {
        deliver { listener.paywallWillOpenURL(url) }
    }

    override fun paywallWillOpenDeepLink(url: String) {
        deliver { listener.paywallWillOpenDeepLink(url) }
    }

    override fun handleLogWithLevel(
        level: String,
        scope: String,
        message: String?,
        info: Map<Any?, *>?,
        error: String?,
    ) {
        // Unmappable native level/scope strings fall back to DEBUG/ALL with the
        // raw value preserved (BridgeListener contract; plan §3.4). The bridge's
        // 4.16.2-only scopes (analytics, webEntitlements) land here too.
        val mappedLevel = logLevelFromRawName(level)
        val mappedScope = logScopeFromRawName(scope)
        val sanitizedInfo = NSAnySanitizer.fromMapOrNull(info).orEmpty().toMutableMap()
        if (mappedLevel == null) sanitizedInfo["rawLevel"] = level
        if (mappedScope == null) sanitizedInfo["rawScope"] = scope
        val infoOrNull = sanitizedInfo.takeIf { it.isNotEmpty() }
        deliver {
            listener.handleLog(
                level = mappedLevel ?: LogLevel.DEBUG,
                scope = mappedScope ?: LogScope.ALL,
                message = message,
                info = infoOrNull,
                error = error,
            )
        }
    }

    override fun willRedeemLink() {
        deliver { listener.willRedeemLink() }
    }

    override fun didRedeemLink(result: SWBRedemptionResult) {
        val mapped = result.toModel()
        deliver { listener.didRedeemLink(mapped) }
    }

    override fun handleSuperwallDeepLinkWithFullURL(
        fullURL: String,
        pathComponents: List<*>,
        queryParameters: Map<Any?, *>,
    ) {
        val components = pathComponents.mapNotNull { it as? String }
        val parameters = queryParameters.entries.mapNotNull { (key, value) ->
            val stringKey = key as? String ?: return@mapNotNull null
            val stringValue = value as? String ?: return@mapNotNull null
            stringKey to stringValue
        }.toMap()
        deliver { listener.handleSuperwallDeepLink(fullURL, components, parameters) }
    }

    override fun customerInfoDidChangeFrom(
        oldInfo: SWBCustomerInfo,
        to: SWBCustomerInfo,
    ) {
        val from = oldInfo.toModel()
        val new = to.toModel()
        deliver { listener.customerInfoDidChange(from, new) }
    }

    override fun userAttributesDidChange(newAttributes: Map<Any?, *>) {
        val attributes = NSAnySanitizer.fromMap(newAttributes)
        deliver { listener.userAttributesDidChange(attributes) }
    }
}
