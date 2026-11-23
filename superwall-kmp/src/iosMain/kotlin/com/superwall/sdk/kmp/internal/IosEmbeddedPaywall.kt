@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.EmbeddedPaywall
import com.superwall.sdk.kmp.PaywallViewCallback
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallback
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBEmbeddedPaywall
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallViewDelegateProtocol
import com.superwall.sdk.kmp.internal.mappers.toModel
import com.superwall.sdk.kmp.internal.mappers.toSWB
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResult
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.UIKit.UIViewController
import platform.darwin.NSObject

/**
 * The iOS [EmbeddedPaywall]: a wrapper around the bridge's `SWBEmbeddedPaywall`,
 * which holds SuperwallKit's `PaywallViewController` and the delegate
 * forwarder that SuperwallKit itself doesn't keep alive. The public
 * `EmbeddedPaywall.viewController` accessor hands the controller out.
 */
internal class IosEmbeddedPaywall(
    override val placement: String,
    swbPaywall: NativeEmbeddedPaywall,
    private val delegateAdapter: PaywallViewDelegateAdapter,
) : EmbeddedPaywall {
    constructor(
        placement: String,
        swbPaywall: SWBEmbeddedPaywall,
        delegateAdapter: PaywallViewDelegateAdapter,
    ) : this(placement, SwbNativeEmbeddedPaywall(swbPaywall), delegateAdapter)

    private var swbPaywall: NativeEmbeddedPaywall? = swbPaywall

    // Kept past destroy(), so `info` still answers once the paywall is let go.
    private var lastInfo: PaywallInfo = swbPaywall.currentInfo().toModel()

    val viewController: UIViewController
        get() =
            // The bridge hands UIKit objects over as `Any` (see
            // SWBEmbeddedPaywall.viewController in the Swift bridge).
            swbPaywall?.viewController() as? UIViewController
                ?: error("This EmbeddedPaywall was destroyed; its view controller has been released.")

    override val info: PaywallInfo
        get() = swbPaywall?.currentInfo()?.toModel()?.also { lastInfo = it } ?: lastInfo

    /**
     * Stops reporting to the callback and lets go of the controller. Taking
     * the controller off screen is left to whatever put it there (Compose's
     * `UIKitViewController` does it itself).
     *
     * The adapter is detached explicitly because dropping the reference is
     * not enough: the Swift `SWBEmbeddedPaywall`, and the delegate forwarder
     * it keeps alive, are only released when Kotlin/Native's GC gets to them.
     * Until then SuperwallKit still reaches the delegate, and it does report
     * once more: taking the controller off screen after destroy() makes it
     * call `didFinishWith: .declined, shouldDismiss: false`.
     */
    override fun destroy() {
        val paywall = swbPaywall ?: return
        delegateAdapter.detach()
        lastInfo = paywall.currentInfo().toModel()
        swbPaywall = null
    }
}

/**
 * What [IosEmbeddedPaywall] uses of the bridge's `SWBEmbeddedPaywall`. It
 * exists for the tests: `SWBEmbeddedPaywall` has no ObjC initializer (the
 * bridge builds it around a live SuperwallKit controller), so a test can only
 * stand in for it through this.
 */
internal interface NativeEmbeddedPaywall {
    fun viewController(): Any?

    fun currentInfo(): SWBPaywallInfo
}

private class SwbNativeEmbeddedPaywall(
    private val swbPaywall: SWBEmbeddedPaywall,
) : NativeEmbeddedPaywall {
    override fun viewController(): Any? = swbPaywall.viewController()

    override fun currentInfo(): SWBPaywallInfo = swbPaywall.currentInfo()
}

/**
 * The Kotlin implementation of the bridge's `@objc(SWBPaywallViewDelegate)`
 * protocol. SuperwallKit calls it on the main actor; app code runs through
 * [scope] (`Dispatchers.Main.immediate`) with its exceptions reported to
 * [log], as in the presentation handler adapter. The Swift forwarder holds it
 * strongly for as long as the `SWBEmbeddedPaywall` lives.
 */
internal class PaywallViewDelegateAdapter(
    private val callback: PaywallViewCallback,
    private val scope: CoroutineScope,
    private val log: (message: String, error: Throwable?) -> Unit,
) : NSObject(), SWBPaywallViewDelegateProtocol {
    // Main thread only, like every call into this adapter.
    private var isDetached = false

    /** After this the app's callback hears nothing more; see [IosEmbeddedPaywall.destroy]. */
    fun detach() {
        isDetached = true
    }

    override fun paywallDidFinish(
        paywallInfo: SWBPaywallInfo,
        result: SWBPaywallResult,
        shouldDismiss: Boolean,
    ) {
        val info = paywallInfo.toModel()
        val paywallResult = result.toModel()
        scope.launch {
            if (isDetached) return@launch
            try {
                callback.onFinished(info, paywallResult, shouldDismiss)
            } catch (throwable: Throwable) {
                log("PaywallViewCallback.onFinished threw an exception", throwable)
            }
        }
    }

    override fun onCustomCallback(
        callback: SWBCustomCallback,
        completion: (SWBCustomCallbackResult?) -> Unit,
    ) {
        val model = callback.toModel()
        scope.launch {
            if (isDetached) {
                // Same answer the bridge gives once the delegate is gone.
                completion(CustomCallbackResult.failure().toSWB())
                return@launch
            }
            val result = try {
                this@PaywallViewDelegateAdapter.callback.onCustomCallback(model)
            } catch (throwable: Throwable) {
                log("PaywallViewCallback.onCustomCallback threw an exception", throwable)
                CustomCallbackResult.failure(mapOf("error" to (throwable.message ?: throwable.toString())))
            }
            completion(result.toSWB())
        }
    }
}
