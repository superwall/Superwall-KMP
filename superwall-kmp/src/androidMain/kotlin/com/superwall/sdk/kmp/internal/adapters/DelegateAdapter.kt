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
 * `DelegateMultiplexer`) **on the thread the native SDK called from**. Port of
 * the Flutter plugin's `SuperwallDelegateHost.kt`, covering
 * `customerInfoDidChange` (which the Flutter host never wired despite the
 * native hook existing).
 *
 * ## Delegate-hook audit
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
 * superwall-android issue to be filed).
 *
 * Mapping failures degrade, never crash any throwable raised while
 * mapping or forwarding is caught and logged; the multiplexer itself already
 * shields native internals from user-delegate exceptions.
 *
 * ## Threading: pass through, don't override
 *
 * This adapter adds NO dispatch of its own. It maps on the calling thread and
 * calls [listener] there, so the delegate inherits superwall-android's own
 * threading — which already splits along exactly the line that matters
 * (verified against the 2.8.0 sources):
 *
 * | Hook | Native thread |
 * |---|---|
 * | `willPresentPaywall`, `didPresentPaywall`, `willDismissPaywall`, `didDismissPaywall` | main — invoked from `PaywallView` |
 * | `handleCustomPaywallAction`, `paywallWillOpenURL`, `paywallWillOpenDeepLink` | main — `Superwall.eventDidOccur` wraps them in `mainScope.launchWithTracking` |
 * | `subscriptionStatusDidChange`, `customerInfoDidChange`, `userAttributesDidChange` | `ioScope` collectors |
 * | `handleSuperwallEvent` | `track()`, i.e. IO |
 * | `handleLog` | any thread — `Logger.debug` calls it inline from wherever it logs |
 *
 * Every UI-shaped hook is already on main; every analytics-shaped one is
 * already off it. The previous `Dispatchers.Main.immediate` hop therefore did
 * nothing for the first group (`immediate` runs inline when already on main)
 * and actively hurt for the second, dragging the analytics firehose onto the
 * looper — `Logger` calls `handleLog` for EVERY internal log line regardless
 * of the configured log level (~240 call sites), so a single `register` became
 * hundreds of main-thread dispatches.
 *
 * The consequences, which [com.superwall.sdk.kmp.SuperwallDelegate] documents:
 * implementations must be thread-safe, must not touch UI from the
 * analytics-shaped hooks without hopping themselves, and run synchronously on
 * an SDK thread (a slow implementation slows the SDK, not the frame budget).
 *
 * Also: the pure-forwarding hooks bail on
 * [BridgeListener.forwardsToUserDelegate] BEFORE mapping, so an app that never
 * sets `Superwall.delegate` pays what the native SDK's own
 * `kotlinDelegate == null` check costs: nothing. Only
 * [subscriptionStatusDidChange] and [customerInfoDidChange] are exempt — they
 * feed the `StreamHolder` flows whether or not a delegate is set (both are
 * thread-safe: `MutableStateFlow.value` and `MutableSharedFlow.tryEmit`).
 *
 * `PaywallPresentationHandler` deliberately keeps its main-thread hop — see
 * [PresentationHandlerAdapter].
 */
internal class DelegateAdapter(
    private val listener: BridgeListener,
) : NativeSuperwallDelegate {
    /** Whether this adapter forwards to [candidate] (delegate-install idempotence). */
    fun wraps(candidate: BridgeListener): Boolean = listener === candidate

    /**
     * Runs [block] — a native→common mapping — returning `null` (event
     * dropped) if it throws.
     */
    private inline fun <T> mapped(block: () -> T): T? =
        try {
            block()
        } catch (throwable: Throwable) {
            // Degrade, never crash — and don't recurse into handleLog, which
            // may itself be the failing path.
            Log.w(TAG, "Dropped a delegate event after a mapping failure", throwable)
            null
        }

    /**
     * Hands an already-mapped value to [listener] on the CALLING thread,
     * catching anything it throws. The multiplexer already shields native
     * internals from user-delegate exceptions; this is the backstop for the
     * stream feeds, and it matters more now that the call is synchronous —
     * without it a throw would unwind into superwall-android's own frame.
     */
    private inline fun deliver(block: () -> Unit) {
        try {
            block()
        } catch (throwable: Throwable) {
            Log.w(TAG, "Dropped a delegate event after a forwarding failure", throwable)
        }
    }

    /** Stream-feeding hook: delivered whether or not a user delegate is set. */
    override fun subscriptionStatusDidChange(
        from: NativeSubscriptionStatus,
        to: NativeSubscriptionStatus,
    ) {
        val mappedFrom = mapped { from.toKmp() } ?: return
        val mappedTo = mapped { to.toKmp() } ?: return
        deliver { listener.subscriptionStatusDidChange(mappedFrom, mappedTo) }
    }

    override fun handleSuperwallEvent(eventInfo: NativeSuperwallEventInfo) {
        if (!listener.forwardsToUserDelegate) return
        val info = mapped { eventInfo.toKmp() } ?: return
        deliver { listener.handleSuperwallEvent(info) }
    }

    override fun handleCustomPaywallAction(withName: String) {
        if (!listener.forwardsToUserDelegate) return
        deliver { listener.handleCustomPaywallAction(withName) }
    }

    override fun willDismissPaywall(withInfo: NativePaywallInfo) {
        if (!listener.forwardsToUserDelegate) return
        val info = mapped { withInfo.toKmp() } ?: return
        deliver { listener.willDismissPaywall(info) }
    }

    override fun willPresentPaywall(withInfo: NativePaywallInfo) {
        if (!listener.forwardsToUserDelegate) return
        val info = mapped { withInfo.toKmp() } ?: return
        deliver { listener.willPresentPaywall(info) }
    }

    override fun didDismissPaywall(withInfo: NativePaywallInfo) {
        if (!listener.forwardsToUserDelegate) return
        val info = mapped { withInfo.toKmp() } ?: return
        deliver { listener.didDismissPaywall(info) }
    }

    override fun didPresentPaywall(withInfo: NativePaywallInfo) {
        if (!listener.forwardsToUserDelegate) return
        val info = mapped { withInfo.toKmp() } ?: return
        deliver { listener.didPresentPaywall(info) }
    }

    override fun paywallWillOpenURL(url: URI) {
        if (!listener.forwardsToUserDelegate) return
        val mappedUrl = url.toString()
        deliver { listener.paywallWillOpenURL(mappedUrl) }
    }

    override fun paywallWillOpenDeepLink(url: Uri) {
        if (!listener.forwardsToUserDelegate) return
        val mappedUrl = url.toString()
        deliver { listener.paywallWillOpenDeepLink(mappedUrl) }
    }

    /**
     * Maps the native stringly-typed log hook to the fully typed common one.
     * Unmappable level/scope strings degrade to [LogLevel.DEBUG] /
     * [LogScope.ALL] with the raw value preserved under `info["rawLevel"]` /
 * `info["rawScope"]` (degrade, never drop).
     *
     * The hottest hook by far: `Logger.debug` calls it for every internal log
     * line BEFORE checking the configured log level, so both the
     * no-user-delegate bail-out and the off-main `sanitizeParams` matter here
     * more than anywhere else.
     */
    override fun handleLog(
        level: String,
        scope: String,
        message: String?,
        info: Map<String, Any>?,
        error: Throwable?,
    ) {
        if (!listener.forwardsToUserDelegate) return
        val mappedLevel = logLevelFromNativeString(level)
        val mappedScope = logScopeFromNativeString(scope)
        // NOT via `mapped`: sanitizeParams legitimately returns null for a null
        // `info` (the common case), which `mapped`'s null-means-dropped
        // convention would silently swallow.
        var sanitizedInfo =
            try {
                sanitizeParams(info)
            } catch (throwable: Throwable) {
                Log.w(TAG, "Dropped a log event after a mapping failure", throwable)
                return
            }
        if (mappedLevel == null) {
            sanitizedInfo = (sanitizedInfo ?: emptyMap()) + ("rawLevel" to level)
        }
        if (mappedScope == null) {
            sanitizedInfo = (sanitizedInfo ?: emptyMap()) + ("rawScope" to scope)
        }
        val mappedError = error?.let { it.localizedMessage ?: it.message ?: it.toString() }
        val finalInfo = sanitizedInfo
        deliver {
            listener.handleLog(
                level = mappedLevel ?: LogLevel.DEBUG,
                scope = mappedScope ?: LogScope.ALL,
                message = message,
                info = finalInfo,
                error = mappedError,
            )
        }
    }

    override fun willRedeemLink() {
        if (!listener.forwardsToUserDelegate) return
        deliver { listener.willRedeemLink() }
    }

    override fun didRedeemLink(result: NativeRedemptionResult) {
        if (!listener.forwardsToUserDelegate) return
        val mappedResult = mapped { result.toKmp() } ?: return
        deliver { listener.didRedeemLink(mappedResult) }
    }

    override fun userAttributesDidChange(newAttributes: Map<String, Any>) {
        if (!listener.forwardsToUserDelegate) return
        val sanitized = mapped { sanitizeParams(newAttributes) ?: emptyMap() } ?: return
        deliver { listener.userAttributesDidChange(sanitized) }
    }

    /** Stream-feeding hook: delivered whether or not a user delegate is set. */
    override fun customerInfoDidChange(
        from: NativeCustomerInfo,
        to: NativeCustomerInfo,
    ) {
        val mappedFrom = mapped { from.toKmp() } ?: return
        val mappedTo = mapped { to.toKmp() } ?: return
        deliver { listener.customerInfoDidChange(mappedFrom, mappedTo) }
    }

    // NOTE (platform gap): superwall-android 2.8.0's SuperwallDelegate has no
    // handleSuperwallDeepLink hook, so BridgeListener.handleSuperwallDeepLink
    // is never invoked from this adapter. See the class KDoc.

    private companion object {
        const val TAG = "SuperwallKMP"
    }
}
