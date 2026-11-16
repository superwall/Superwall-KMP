package com.superwall.sdk.kmp.internal

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import com.superwall.sdk.kmp.EmbeddedPaywall
import com.superwall.sdk.kmp.PaywallViewCallback
import com.superwall.sdk.kmp.internal.mappers.sanitizeParams
import com.superwall.sdk.kmp.internal.mappers.toKmp
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.paywall.PaywallOverrides
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.superwall.sdk.Superwall as NativeSuperwall
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult as NativePaywallResult
import com.superwall.sdk.paywall.presentation.get_paywall.builder.PaywallBuilder as NativePaywallBuilder
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason as NativeRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides as NativePaywallOverrides
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason as NativePaywallSkippedReason
import com.superwall.sdk.paywall.view.PaywallView as NativePaywallView
import com.superwall.sdk.paywall.view.delegate.PaywallViewCallback as NativePaywallViewCallback

/** Why `PaywallBuilder.build()` returned no paywall, in common terms. */
internal sealed interface PaywallRetrievalFailure {
    data class Skipped(val reason: PaywallSkippedReason) : PaywallRetrievalFailure

    data class Failed(val message: String) : PaywallRetrievalFailure
}

/**
 * superwall-android reports a skip and an error the same way, as the failure
 * of the builder's `Result`: a skip is the `PaywallSkippedReason` throwable.
 */
internal fun Throwable.toPaywallRetrievalFailure(): PaywallRetrievalFailure =
    when (this) {
        is NativePaywallSkippedReason -> PaywallRetrievalFailure.Skipped(toKmp())
        // Its message is null; the readable text is in `info`.
        is NativeRequestStatusReason -> PaywallRetrievalFailure.Failed(info)
        else -> PaywallRetrievalFailure.Failed(localizedMessage ?: message ?: toString())
    }

/**
 * The Android [EmbeddedPaywall]: a wrapper around the host view, which is
 * what the public `EmbeddedPaywall.view` accessor hands out. The native
 * `PaywallView` is an SDK class that is cached and reused, so it is wrapped
 * rather than exposed.
 */
internal class AndroidEmbeddedPaywall(
    val host: EmbeddedPaywalls.PaywallHostView,
) : EmbeddedPaywall {
    override val placement: String get() = host.placement

    override val info: PaywallInfo get() = host.info

    override fun destroy() {
        host.teardown()
    }
}

/**
 * Retrieves paywalls for the app to show in its own UI, and tears them down.
 *
 * The lifecycle is a port of the Flutter plugin's `PaywallPlatformView.kt`.
 * superwall-android caches `PaywallView`s and shares them (keyed by paywall,
 * not placement), which is why:
 *
 * - the app gets a host view rather than the `PaywallView` itself, so the
 *   paywall is only added, and its presentation only begun, once the host is
 *   in a window;
 * - the destructive teardown runs only when the app says the presentation is
 *   over (`EmbeddedPaywall.destroy`), never on a mere detach, which Compose and navigation
 *   cause routinely;
 * - no paywall is retrieved while a teardown is in flight, because the cache
 *   can hand the very same instance straight back.
 *
 * Everything here runs on the main thread.
 */
internal object EmbeddedPaywalls {
    // Teardown must outlive whoever asked for it: cancelling destroyed() or
    // cleanup() midway would leave a shared, cached PaywallView half torn down.
    private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val teardownJobs = mutableSetOf<Job>()

    suspend fun retrieve(
        activity: Activity,
        placement: String,
        params: Map<String, Any?>?,
        overrides: PaywallOverrides?,
        callback: PaywallViewCallback,
        log: (message: String, error: Throwable?) -> Unit,
    ): AndroidEmbeddedPaywall? =
        withContext(Dispatchers.Main.immediate) {
            val host = PaywallHostView(activity, placement, callback, log)
            val nativeOverrides = overrides?.toNative()
            while (teardownJobs.isNotEmpty()) {
                teardownJobs.toList().joinAll()
                teardownJobs.removeAll { it.isCompleted }
            }
            val result =
                NativePaywallBuilder(placement)
                    .params(sanitizeParams(params)?.filterValues { it != null }?.mapValues { (_, value) -> value as Any })
                    .overrides(nativeOverrides)
                    .delegate(host.nativeCallback)
                    .activity(activity)
                    .build()
            val failure =
                result.fold(
                    onSuccess = { paywall ->
                        if (paywall.parent != null) {
                            PaywallRetrievalFailure.Failed(
                                "The retrieved paywall is already shown in another view. " +
                                    "Destroy that view first, or use a different placement for each paywall on screen.",
                            )
                        } else {
                            host.setPaywall(paywall)
                            null
                        }
                    },
                    onFailure = { it.toPaywallRetrievalFailure() },
                )
            when (failure) {
                null -> AndroidEmbeddedPaywall(host)
                is PaywallRetrievalFailure.Skipped -> {
                    host.deliver("onSkip") { callback.onSkip(failure.reason) }
                    null
                }
                is PaywallRetrievalFailure.Failed -> {
                    host.deliver("onError") { callback.onError(failure.message) }
                    null
                }
            }
        }

    /**
     * Product identifiers become the store products the native overrides
     * take; an identifier the store doesn't know is left out, and a failed
     * fetch leaves the paywall's own products in place.
     */
    private suspend fun PaywallOverrides.toNative(): NativePaywallOverrides {
        val products =
            if (productIdsByName.isEmpty()) {
                emptyMap()
            } else {
                withContext(Dispatchers.IO) {
                    NativeSuperwall.instance.getProducts(*productIdsByName.values.toSet().toTypedArray())
                }.getOrNull().orEmpty()
            }
        return NativePaywallOverrides(
            productsByName = productIdsByName.mapNotNull { (name, id) -> products[id]?.let { name to it } }.toMap(),
            ignoreSubscriptionStatus = ignoreSubscriptionStatus,
        )
    }

    class PaywallHostView(
        context: Context,
        val placement: String,
        private val callback: PaywallViewCallback,
        private val log: (message: String, error: Throwable?) -> Unit,
    ) : FrameLayout(context) {
        // The paywall on screen, or waiting for this host to be in a window.
        private var paywall: NativePaywallView? = null
        private var isShown = false

        // Kept past teardown, so `info` still answers once the paywall is gone.
        var lastInfo: PaywallInfo = PaywallInfo()
            private set

        val info: PaywallInfo
            get() = paywall?.info?.toKmp()?.also { lastInfo = it } ?: lastInfo
        private var lastUiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        val nativeCallback =
            object : NativePaywallViewCallback {
                override fun onFinished(
                    paywall: NativePaywallView,
                    result: NativePaywallResult,
                    shouldDismiss: Boolean,
                ) {
                    // A cached paywall this host has let go of can still report.
                    if (paywall !== this@PaywallHostView.paywall) return
                    val info = paywall.info.toKmp()
                    val mappedResult = result.toKmp()
                    deliver("onFinished") { callback.onFinished(info, mappedResult, shouldDismiss) }
                }
            }

        /** App code runs here; an exception from it must not break the paywall. */
        fun deliver(
            name: String,
            block: () -> Unit,
        ) {
            try {
                block()
            } catch (throwable: Throwable) {
                log("PaywallViewCallback.$name threw an exception", throwable)
            }
        }

        fun setPaywall(paywall: NativePaywallView) {
            this.paywall = paywall
            if (isAttachedToWindow) show()
        }

        private fun show() {
            val paywall = paywall ?: return
            if (isShown) return
            if (paywall.parent != null) {
                // Another host was given the same cached paywall while this one waited.
                this.paywall = null
                deliver("onError") {
                    callback.onError("The retrieved paywall is already shown in another view.")
                }
                return
            }
            isShown = true
            addView(paywall, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            paywall.onViewCreated()
            val info = paywall.info.toKmp()
            deliver("onPresent") { callback.onPresent(info) }
        }

        fun teardown() {
            val paywall = paywall ?: return
            lastInfo = paywall.info.toKmp()
            this.paywall = null
            if (!isShown) return // Never shown: it still belongs to the SDK's cache.
            isShown = false
            removeView(paywall)
            paywall.beforeOnDestroy(forceCleanup = true)
            paywall.encapsulatingActivity = null
            val job =
                teardownScope.launch {
                    paywall.destroyed(forceCleanup = true)
                    paywall.cleanup()
                }
            teardownJobs.removeAll { it.isCompleted }
            if (!job.isCompleted) teardownJobs.add(job)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            show()
        }

        override fun onConfigurationChanged(newConfig: Configuration?) {
            super.onConfigurationChanged(newConfig)
            val uiMode = (newConfig ?: resources.configuration).uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (uiMode != lastUiMode) {
                lastUiMode = uiMode
                if (isShown) paywall?.onThemeChanged()
            }
        }
    }
}
