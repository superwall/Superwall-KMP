package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.internal.mappers.sanitizeParams
import com.superwall.sdk.kmp.internal.mappers.toKmp
import com.superwall.sdk.kmp.models.callbacks.CustomCallback
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResultStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.superwall.sdk.paywall.presentation.CustomCallback as NativeCustomCallback
import com.superwall.sdk.paywall.presentation.CustomCallbackResult as NativeCustomCallbackResult
import com.superwall.sdk.paywall.presentation.PaywallPresentationHandler as NativePaywallPresentationHandler

/**
 * A PER-REGISTRATION adapter mapping superwall-android 2.8.0's
 * [NativePaywallPresentationHandler] callbacks onto the common
 * [PaywallPresentationHandler] closures.
 *
 * `AndroidSuperwallBridge.registerPlacement` creates a **fresh instance per
 * call** — there is deliberately no placement-keyed registry, which was the
 * source of the Flutter plugin's one-handler-per-placement aliasing bug
 * (plan §3.4/§6.4): two `register` calls for the same placement each keep
 * their own handler and feature here.
 *
 * The adapter **strongly retains** both the [handler] and the [feature]
 * closure (constructor properties) for as long as the native SDK retains the
 * native handler/feature it hands out via [nativeHandler]/[nativeFeature].
 *
 * Threading (plan §6): every closure is delivered on
 * `Dispatchers.Main.immediate` via the bridge scope; `onCustomCallback` — the
 * only value-returning, suspending callback — is awaited in place on
 * `Main.immediate` (the native SDK calls it from a coroutine; no blocking).
 * User-closure exceptions are caught, logged via the bridge's `handleLog`
 * path, and degrade (custom callbacks degrade to
 * `CustomCallbackResult.failure()`); they never reach native SDK internals.
 */
internal class PresentationHandlerAdapter(
    private val handler: PaywallPresentationHandler?,
    private val feature: (() -> Unit)?,
    private val scope: () -> CoroutineScope,
    private val log: (message: String, error: Throwable?) -> Unit,
) {
    /**
     * The native handler to pass to `Superwall.register`, or `null` when the
     * caller registered without a handler.
     */
    val nativeHandler: NativePaywallPresentationHandler? =
        handler?.let { common ->
            NativePaywallPresentationHandler().apply {
                onPresent { info ->
                    dispatch("onPresent") { common.onPresentHandler?.invoke(info.toKmp()) }
                }
                onDismiss { info, result ->
                    dispatch("onDismiss") {
                        common.onDismissHandler?.invoke(info.toKmp(), result.toKmp())
                    }
                }
                onError { throwable ->
                    dispatch("onError") {
                        common.onErrorHandler?.invoke(
                            throwable.localizedMessage ?: throwable.message ?: "Unknown error",
                        )
                    }
                }
                onSkip { reason ->
                    dispatch("onSkip") { common.onSkipHandler?.invoke(reason.toKmp()) }
                }
                onCustomCallback { callback ->
                    handleCustomCallback(common, callback)
                }
            }
        }

    /**
     * The native feature closure to pass to `Superwall.register`, or `null`
     * when the caller registered without one. Fixes the Flutter host's
     * `"${placement}handler"` feature-hostId suffix mismatch by holding a
     * direct object reference instead of routing by id.
     */
    val nativeFeature: (() -> Unit)? =
        feature?.let { block ->
            { dispatch("feature") { block() } }
        }

    /**
     * Awaits the user's suspend custom-callback closure on `Main.immediate`,
     * defaulting to failure when unset (the common contract) and degrading to
     * failure — logged — when the user's closure throws.
     */
    private suspend fun handleCustomCallback(
        common: PaywallPresentationHandler,
        callback: NativeCustomCallback,
    ): NativeCustomCallbackResult {
        val userHandler =
            common.onCustomCallbackHandler
                ?: return NativeCustomCallbackResult.failure()
        return try {
            val result =
                withContext(Dispatchers.Main.immediate) {
                    userHandler(
                        CustomCallback(
                            name = callback.name,
                            variables = sanitizeParams(callback.variables),
                        ),
                    )
                }
            val data: Map<String, Any>? =
                sanitizeParams(result.data)
                    ?.filterValues { it != null }
                    ?.mapValues { (_, value) -> value as Any }
            when (result.status) {
                CustomCallbackResultStatus.SUCCESS -> NativeCustomCallbackResult.success(data)
                CustomCallbackResultStatus.FAILURE -> NativeCustomCallbackResult.failure(data)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            log("PaywallPresentationHandler.onCustomCallback threw an exception", throwable)
            NativeCustomCallbackResult.failure()
        }
    }

    /**
     * Delivers [block] on the main thread via the bridge scope, catching and
     * logging anything the user's closure throws (plan §6.5).
     */
    private fun dispatch(
        callbackName: String,
        block: () -> Unit,
    ) {
        scope().launch {
            try {
                block()
            } catch (throwable: Throwable) {
                log("PaywallPresentationHandler.$callbackName threw an exception", throwable)
            }
        }
    }
}
