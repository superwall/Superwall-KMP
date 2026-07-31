@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallback
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReasonNone
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationHandlerProtocol
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallSkippedReason
import com.superwall.sdk.kmp.internal.mappers.toModel
import com.superwall.sdk.kmp.internal.mappers.toSWB
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.darwin.NSObject

/**
 * The per-registration Kotlin implementation of the bridge's
 * `@objc(SWBPaywallPresentationHandler)` protocol. One fresh adapter is
 * created for every `register` call (plan §6.4 — per-registration adapters,
 * no placement-keyed registry), strongly retaining the user's
 * [PaywallPresentationHandler] closures.
 *
 * Lifetime: the [com.superwall.sdk.kmp.internal.IosSuperwallBridge] keeps
 * each adapter in a main-confined registry for the paywall's lifetime — never
 * rely on ObjC retaining Kotlin objects (plan §6.4). [onFinished] removes it
 * on the terminal callbacks, mirroring the Swift wrapper's own release
 * condition: dismiss with `closeReason != none`, skip, or error (the
 * release-on-error is the declared deliberate delta vs the leaking Flutter
 * host).
 *
 * Threading: callbacks arrive synchronously on SuperwallKit's main actor;
 * user closures are delivered through [scope] (`Dispatchers.Main.immediate`),
 * and their exceptions are caught (plan §6.5). `onCustomCallback` launches
 * the user's suspend closure — never blocking — and bridges the result back
 * through the async completion exactly once.
 */
internal class PresentationHandlerAdapter(
    private val handler: PaywallPresentationHandler,
    private val scope: CoroutineScope,
    private val onFinished: (PresentationHandlerAdapter) -> Unit,
) : NSObject(), SWBPaywallPresentationHandlerProtocol {
    private fun deliver(block: () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (throwable: Throwable) {
                println("[Superwall] PaywallPresentationHandler callback threw: $throwable")
            }
        }
    }

    override fun onPresent(paywallInfo: SWBPaywallInfo) {
        val info = paywallInfo.toModel()
        deliver { handler.onPresentHandler?.invoke(info) }
    }

    override fun onDismiss(
        paywallInfo: SWBPaywallInfo,
        result: SWBPaywallResult,
    ) {
        val info = paywallInfo.toModel()
        val paywallResult = result.toModel()
        deliver { handler.onDismissHandler?.invoke(info, paywallResult) }
        // Mirror of the Swift wrapper's release condition: only a real close
        // (closeReason != none) ends the registration.
        if (paywallInfo.closeReason() != SWBPaywallCloseReasonNone) {
            onFinished(this)
        }
    }

    override fun onError(error: String) {
        deliver { handler.onErrorHandler?.invoke(error) }
        // Deliberate delta vs the Flutter host (which leaked here): a failed
        // presentation gets no dismiss/skip, so release now (plan §6.4).
        onFinished(this)
    }

    override fun onSkip(reason: SWBPaywallSkippedReason) {
        val skippedReason = reason.toModel()
        deliver { handler.onSkipHandler?.invoke(skippedReason) }
        onFinished(this)
    }

    override fun onCustomCallback(
        callback: SWBCustomCallback,
        completion: (SWBCustomCallbackResult?) -> Unit,
    ) {
        val model = callback.toModel()
        scope.launch {
            val result = try {
                handler.onCustomCallbackHandler?.invoke(model) ?: CustomCallbackResult.failure()
            } catch (throwable: Throwable) {
                println("[Superwall] onCustomCallback handler threw: $throwable")
                CustomCallbackResult.failure(mapOf("error" to (throwable.message ?: throwable.toString())))
            }
            completion(result.toSWB())
        }
    }
}
