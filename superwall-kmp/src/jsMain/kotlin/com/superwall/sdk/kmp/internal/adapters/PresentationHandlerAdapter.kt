package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.internal.interop.jsObject
import com.superwall.sdk.kmp.internal.mappers.errorMessage
import com.superwall.sdk.kmp.internal.mappers.paywallInfoFromJs
import com.superwall.sdk.kmp.internal.mappers.paywallResultFromJs
import com.superwall.sdk.kmp.internal.mappers.paywallSkippedReasonFromJs

/**
 * Builds web's per-call `PaywallPresentationHandler` object from the common
 * [PaywallPresentationHandler].
 *
 * `onCustomCallback` has no web counterpart — web paywalls do not issue
 * custom callbacks — so a registered handler for it is simply never invoked.
 * `onPurchase` is deliberately left unset: setting it would replace web's
 * default checkout completion (entitlements, dismissal) entirely.
 *
 * Callbacks run on the JS main thread already; exceptions from the app's
 * callbacks are caught and reported so they cannot abort web's register flow.
 */
internal fun PaywallPresentationHandler.toJs(): dynamic {
    val common = this
    val h = jsObject()
    h.onPresent = { info: dynamic ->
        guard("onPresent") { common.onPresentHandler?.invoke(paywallInfoFromJs(info)) }
    }
    h.onDismiss = { info: dynamic, result: dynamic ->
        guard("onDismiss") {
            val mapped = paywallResultFromJs(result)
            if (mapped != null) common.onDismissHandler?.invoke(paywallInfoFromJs(info), mapped)
        }
    }
    h.onError = { error: dynamic ->
        guard("onError") { common.onErrorHandler?.invoke(errorMessage(error) ?: "Unknown error") }
    }
    h.onSkip = { reason: dynamic ->
        guard("onSkip") {
            // `null` for web's userSubscribed skip — see paywallSkippedReasonFromJs.
            val mapped = paywallSkippedReasonFromJs(reason)
            if (mapped != null) common.onSkipHandler?.invoke(mapped)
        }
    }
    return h
}

/** Wraps the `feature` block so an exception from it is reported, not thrown into web's register flow. */
internal fun wrapFeature(feature: () -> Unit): () -> Unit = { guard("feature") { feature() } }

private inline fun guard(
    callbackName: String,
    block: () -> Unit,
) {
    try {
        block()
    } catch (throwable: Throwable) {
        console.warn("[Superwall] register $callbackName threw an exception: $throwable")
    }
}
