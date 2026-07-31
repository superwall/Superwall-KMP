package com.superwall.sdk.kmp.internal.adapters

import android.os.Looper
import com.superwall.sdk.kmp.internal.mappers.toKmp
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.paywall.presentation.PaywallInfo as NativePaywallInfo

/**
 * Adapts the common `PaywallOptions.onBackPressed` closure onto
 * superwall-android 2.7.11's native hook
 * (`com.superwall.sdk.config.options.PaywallOptions.onBackPressed:
 * ((PaywallInfo?) -> Boolean)?` — verified present at PaywallOptions.kt:153).
 *
 * This is the SOLE synchronous exception to the plan §6 main-dispatch rule:
 * the native hook's `Boolean` return value must be produced on the invoking
 * thread, so the user's closure is invoked **synchronously on the thread the
 * native SDK calls it on** (the main thread — it is driven by the paywall
 * Activity's back handling). No dispatcher hop, no suspension. The adapter
 * logs a warning when it is ever invoked off the main thread.
 *
 * Return-value semantics (fixing the Flutter host's `OnBackPressedHost.kt`
 * comment-vs-code disagreement, plan §3.4): the user's actual return value is
 * forwarded — `true` means "I handled the back press", `false` means "run the
 * SDK's default back behavior". A throwing closure is caught, logged, and
 * treated as `false` ("don't intercept", plan §6.5).
 */
internal class OnBackPressedAdapter(
    private val userCallback: (PaywallInfo?) -> Boolean,
    private val log: (message: String, error: Throwable?) -> Unit,
) {
    /** The closure to install on the native `PaywallOptions.onBackPressed`. */
    val nativeCallback: (NativePaywallInfo?) -> Boolean = { nativeInfo ->
        if (Looper.myLooper() != Looper.getMainLooper()) {
            log(
                "PaywallOptions.onBackPressed was invoked off the main thread; " +
                    "the user callback runs synchronously on the invoking thread.",
                null,
            )
        }
        try {
            userCallback(nativeInfo?.toKmp())
        } catch (throwable: Throwable) {
            log("PaywallOptions.onBackPressed threw an exception; treating as false", throwable)
            false
        }
    }
}
