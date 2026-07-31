package com.superwall.sdk.kmp.internal.adapters

import android.net.Uri
import android.util.Log
import com.superwall.sdk.kmp.internal.BridgeListener
import com.superwall.sdk.kmp.internal.mappers.logLevelFromNativeString
import com.superwall.sdk.kmp.internal.mappers.logScopeFromNativeString
import com.superwall.sdk.kmp.internal.mappers.sanitizeParams
import com.superwall.sdk.kmp.internal.mappers.toKmp
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.URI
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo as NativeSuperwallEventInfo
import com.superwall.sdk.delegate.SuperwallDelegate as NativeSuperwallDelegate
import com.superwall.sdk.models.customer.CustomerInfo as NativeCustomerInfo
import com.superwall.sdk.models.entitlements.SubscriptionStatus as NativeSubscriptionStatus
import com.superwall.sdk.models.internal.RedemptionResult as NativeRedemptionResult
import com.superwall.sdk.paywall.presentation.PaywallInfo as NativePaywallInfo

/**
 * The always-installed native delegate: implements superwall-android 2.8.0's
 * [NativeSuperwallDelegate] in full and forwards every hook — payloads mapped
 * to common model types — to the common [BridgeListener] (the
 * `DelegateMultiplexer`) on `Dispatchers.Main.immediate` via the bridge's
 * callback scope. Port of the Flutter plugin's `SuperwallDelegateHost.kt`,
 * upgraded from `Dispatchers.Main` to `Main.immediate` delivery and covering
 * `customerInfoDidChange` (which the Flutter host never wired despite the
 * native hook existing).
 *
 * ## Delegate-hook audit (plan §4)
 *
 * Verified against `/home/user/refs/superwall-android-src/com/superwall/sdk/
 * delegate/SuperwallDelegate.kt` (superwall-android 2.8.0):
 *
 * - `customerInfoDidChange(from, to)` — **present natively**; wired below.
 *   It also feeds `Superwall.customerInfoFlow` through the multiplexer, so the
 *   bridge deliberately does NOT additionally collect the native
 *   `Superwall.instance.customerInfo` StateFlow (that would double-emit).
 * - `handleSuperwallDeepLink(fullURL, pathComponents, queryParameters)` —
 *   **absent from the native delegate**. [BridgeListener.handleSuperwallDeepLink]
 *   is therefore never invoked on Android (documented platform gap; upstream
 *   superwall-android issue to be filed per plan §4).
 *
 * Mapping failures degrade, never crash (plan §7): any throwable raised while
 * mapping or forwarding is caught and logged; the multiplexer itself already
 * shields native internals from user-delegate exceptions.
 */
internal class DelegateAdapter(
    private val listener: BridgeListener,
    private val scope: () -> CoroutineScope,
) : NativeSuperwallDelegate {
    /** Whether this adapter forwards to [candidate] (delegate-install idempotence). */
    fun wraps(candidate: BridgeListener): Boolean = listener === candidate

    /**
     * Delivers [block] on the main thread via the bridge scope
     * (`Dispatchers.Main.immediate`), catching anything it throws.
     */
    private fun onMain(block: () -> Unit) {
        scope().launch {
            try {
                block()
            } catch (throwable: Throwable) {
                // The multiplexer catches user-delegate exceptions; anything
                // reaching here is a mapping failure. Degrade, never crash —
                // and don't recurse into handleLog, which may itself be the
                // failing path.
                Log.w(TAG, "Dropped a delegate event after a mapping failure", throwable)
            }
        }
    }

    override fun subscriptionStatusDidChange(
        from: NativeSubscriptionStatus,
        to: NativeSubscriptionStatus,
    ) {
        onMain { listener.subscriptionStatusDidChange(from.toKmp(), to.toKmp()) }
    }

    override fun handleSuperwallEvent(eventInfo: NativeSuperwallEventInfo) {
        onMain { listener.handleSuperwallEvent(eventInfo.toKmp()) }
    }

    override fun handleCustomPaywallAction(withName: String) {
        onMain { listener.handleCustomPaywallAction(withName) }
    }

    override fun willDismissPaywall(withInfo: NativePaywallInfo) {
        onMain { listener.willDismissPaywall(withInfo.toKmp()) }
    }

    override fun willPresentPaywall(withInfo: NativePaywallInfo) {
        onMain { listener.willPresentPaywall(withInfo.toKmp()) }
    }

    override fun didDismissPaywall(withInfo: NativePaywallInfo) {
        onMain { listener.didDismissPaywall(withInfo.toKmp()) }
    }

    override fun didPresentPaywall(withInfo: NativePaywallInfo) {
        onMain { listener.didPresentPaywall(withInfo.toKmp()) }
    }

    override fun paywallWillOpenURL(url: URI) {
        onMain { listener.paywallWillOpenURL(url.toString()) }
    }

    override fun paywallWillOpenDeepLink(url: Uri) {
        onMain { listener.paywallWillOpenDeepLink(url.toString()) }
    }

    /**
     * Maps the native stringly-typed log hook to the fully typed common one.
     * Unmappable level/scope strings degrade to [LogLevel.DEBUG] /
     * [LogScope.ALL] with the raw value preserved under `info["rawLevel"]` /
     * `info["rawScope"]` (plan §3.4 — degrade, never drop).
     */
    override fun handleLog(
        level: String,
        scope: String,
        message: String?,
        info: Map<String, Any>?,
        error: Throwable?,
    ) {
        onMain {
            val mappedLevel = logLevelFromNativeString(level)
            val mappedScope = logScopeFromNativeString(scope)
            var sanitizedInfo = sanitizeParams(info)
            if (mappedLevel == null) {
                sanitizedInfo = (sanitizedInfo ?: emptyMap()) + ("rawLevel" to level)
            }
            if (mappedScope == null) {
                sanitizedInfo = (sanitizedInfo ?: emptyMap()) + ("rawScope" to scope)
            }
            listener.handleLog(
                level = mappedLevel ?: LogLevel.DEBUG,
                scope = mappedScope ?: LogScope.ALL,
                message = message,
                info = sanitizedInfo,
                error = error?.let { it.localizedMessage ?: it.message ?: it.toString() },
            )
        }
    }

    override fun willRedeemLink() {
        onMain { listener.willRedeemLink() }
    }

    override fun didRedeemLink(result: NativeRedemptionResult) {
        onMain { listener.didRedeemLink(result.toKmp()) }
    }

    override fun userAttributesDidChange(newAttributes: Map<String, Any>) {
        onMain { listener.userAttributesDidChange(sanitizeParams(newAttributes) ?: emptyMap()) }
    }

    override fun customerInfoDidChange(
        from: NativeCustomerInfo,
        to: NativeCustomerInfo,
    ) {
        onMain { listener.customerInfoDidChange(from.toKmp(), to.toKmp()) }
    }

    // NOTE (platform gap): superwall-android 2.8.0's SuperwallDelegate has no
    // handleSuperwallDeepLink hook, so BridgeListener.handleSuperwallDeepLink
    // is never invoked from this adapter. See the class KDoc.

    private companion object {
        const val TAG = "SuperwallKMP"
    }
}
