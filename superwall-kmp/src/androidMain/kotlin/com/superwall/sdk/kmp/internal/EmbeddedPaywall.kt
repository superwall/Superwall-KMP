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
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.WeakHashMap
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
 * Which host each cached native view was last handed to.
 *
 * superwall-android hands a view that is already on screen to a new request:
 * the cache is keyed by paywall, and `getPaywall` detaches the view from its
 * parent and points its callback at the new request before returning it. So
 * the view's parent can't tell whether it is shared; this record can. Both
 * sides are weak, so a host the app never destroys, or a view the SDK evicts,
 * isn't kept alive by it. Main thread only.
 */
internal class PaywallOwners<V : Any, H : Any> {
    private val owners = WeakHashMap<V, WeakReference<H>>()

    /** Hands [view] to [host]; returns the live host it was taken from, if any. */
    fun claim(
        view: V,
        host: H,
    ): H? = owners.put(view, WeakReference(host))?.get()?.takeIf { it !== host }

    fun owns(
        view: V,
        host: H,
    ): Boolean = owners[view]?.get() === host

    /** Ends [host]'s claim on [view]; returns whether it still held it. */
    fun release(
        view: V,
        host: H,
    ): Boolean = owns(view, host).also { if (it) owners.remove(view) }
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
 *   can hand the very same instance straight back;
 * - a retrieval that gets a view another host is showing takes it over, as
 *   superwall-android already has by then: the old host is told and lets go
 *   without tearing down the view it no longer has.
 *
 * Everything here runs on the main thread.
 */
internal object EmbeddedPaywalls {
    // Teardown must outlive whoever asked for it: cancelling destroyed() or
    // cleanup() midway would leave a shared, cached PaywallView half torn down.
    private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val teardownJobs = mutableSetOf<Job>()
    private val owners = PaywallOwners<NativePaywallView, PaywallHostView>()

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
                        // By now the SDK has detached the view and redirected its callback.
                        owners.claim(paywall, host)?.supersede(paywall)
                        host.setPaywall(paywall)
                        null
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
        callback: PaywallViewCallback,
        log: (message: String, error: Throwable?) -> Unit,
    ) : FrameLayout(context) {
        private val presentation =
            EmbeddedPresentation(
                callback,
                log,
                object : PaywallSurface<NativePaywallView> {
                    override val isInWindow: Boolean get() = isAttachedToWindow

                    override fun info(paywall: NativePaywallView): PaywallInfo = paywall.info.toKmp()

                    override fun hasParent(paywall: NativePaywallView): Boolean = paywall.parent != null

                    override fun isInHost(paywall: NativePaywallView): Boolean = paywall.parent === this@PaywallHostView

                    override fun present(paywall: NativePaywallView) {
                        addView(paywall, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        paywall.onViewCreated()
                    }

                    override fun release(paywall: NativePaywallView): Boolean = owners.release(paywall, this@PaywallHostView)

                    override fun dismantle(paywall: NativePaywallView) {
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
                },
            )

        val info: PaywallInfo get() = presentation.info
        private var lastUiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        val nativeCallback =
            object : NativePaywallViewCallback {
                override fun onFinished(
                    paywall: NativePaywallView,
                    result: NativePaywallResult,
                    shouldDismiss: Boolean,
                ) {
                    presentation.finished(paywall, shouldDismiss) { result.toKmp() }
                }
            }

        fun deliver(
            name: String,
            block: () -> Unit,
        ): Unit = presentation.deliver(name, block)

        fun setPaywall(paywall: NativePaywallView): Unit = presentation.setPaywall(paywall)

        fun supersede(paywall: NativePaywallView): Unit = presentation.supersede(paywall)

        fun teardown(): Unit = presentation.teardown()

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            presentation.show()
        }

        override fun onConfigurationChanged(newConfig: Configuration?) {
            super.onConfigurationChanged(newConfig)
            val uiMode = (newConfig ?: resources.configuration).uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (uiMode != lastUiMode) {
                lastUiMode = uiMode
                presentation.shown?.onThemeChanged()
            }
        }
    }
}

/**
 * What [EmbeddedPresentation] needs of the native paywall view and the host
 * it is shown in. Both are Android views, which a host test can't construct,
 * so the lifecycle reaches them only through this.
 */
internal interface PaywallSurface<V : Any> {
    /** Whether the host is attached to a window. */
    val isInWindow: Boolean

    fun info(paywall: V): PaywallInfo

    /** Whether [paywall] is in a view hierarchy, the host's or any other. */
    fun hasParent(paywall: V): Boolean

    fun isInHost(paywall: V): Boolean

    /** Adds [paywall] to the host and begins its presentation. */
    fun present(paywall: V)

    /** Ends the host's claim on [paywall]; returns whether it still held it. */
    fun release(paywall: V): Boolean

    /** Takes [paywall] out of the host and ends its presentation in the SDK. */
    fun dismantle(paywall: V)
}

/**
 * The lifecycle of one embedded paywall: when it is put on screen, what the
 * app's [callback] is told, and when it is torn down. Main thread only.
 */
internal class EmbeddedPresentation<V : Any>(
    private val callback: PaywallViewCallback,
    private val log: (message: String, error: Throwable?) -> Unit,
    private val surface: PaywallSurface<V>,
) {
    // The paywall on screen, or waiting for the host to be in a window.
    private var paywall: V? = null
    private var isShown = false

    // Kept past teardown, so `info` still answers once the paywall is gone.
    private var lastInfo: PaywallInfo = PaywallInfo()

    val info: PaywallInfo
        get() = paywall?.let { surface.info(it) }?.also { lastInfo = it } ?: lastInfo

    /** The paywall, while it is on screen in the host. */
    val shown: V? get() = paywall.takeIf { isShown }

    fun finished(
        paywall: V,
        shouldDismiss: Boolean,
        result: () -> PaywallResult,
    ) {
        // A cached paywall this host has let go of can still report.
        if (paywall !== this.paywall) return
        val info = surface.info(paywall)
        val mappedResult = result()
        deliver("onFinished") { callback.onFinished(info, mappedResult, shouldDismiss) }
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

    fun setPaywall(paywall: V) {
        this.paywall = paywall
        if (surface.isInWindow) show()
    }

    /** Called whenever the host is attached to a window; a detach is deliberately not acted on. */
    fun show() {
        val paywall = paywall ?: return
        if (isShown) return
        if (surface.hasParent(paywall)) {
            // A full-screen presentation took the cached paywall while this host waited.
            this.paywall = null
            surface.release(paywall)
            deliver("onError") {
                callback.onError("The retrieved paywall is already shown in another view.")
            }
            return
        }
        isShown = true
        surface.present(paywall)
        val info = surface.info(paywall)
        deliver("onPresent") { callback.onPresent(info) }
    }

    /**
     * Another host has been handed [paywall]: let go of it without tearing
     * it down, since it is now shown there, and tell the app this view is
     * empty.
     */
    fun supersede(paywall: V) {
        if (paywall !== this.paywall) return
        lastInfo = surface.info(paywall)
        this.paywall = null
        isShown = false
        deliver("onError") {
            callback.onError(
                "The paywall was taken over by another view that retrieved the same paywall. " +
                    "Destroy a paywall in one view before retrieving it for another, or use a different paywall for each view on screen.",
            )
        }
    }

    fun teardown() {
        val paywall = paywall ?: return
        lastInfo = surface.info(paywall)
        this.paywall = null
        val wasShown = isShown
        isShown = false
        val owned = surface.release(paywall)
        if (!wasShown) return // Never shown: it still belongs to the SDK's cache.
        // Something else (a full-screen presentation of the same paywall) has
        // taken the view over: tearing it down would break it there.
        if (!owned || !surface.isInHost(paywall)) return
        surface.dismantle(paywall)
    }
}
