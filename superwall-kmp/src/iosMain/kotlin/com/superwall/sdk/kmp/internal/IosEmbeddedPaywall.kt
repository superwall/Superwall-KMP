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
    swbPaywall: SWBEmbeddedPaywall,
) : EmbeddedPaywall {
    private var swbPaywall: SWBEmbeddedPaywall? = swbPaywall

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
     * Lets go of the controller and, with it, of the delegate forwarder, so
     * nothing more is reported. Taking the controller off screen is left to
     * whatever put it there (Compose's `UIKitViewController` does it itself).
     */
    override fun destroy() {
        val paywall = swbPaywall ?: return
        lastInfo = paywall.currentInfo().toModel()
        swbPaywall = null
    }
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
    override fun paywallDidFinish(
        paywallInfo: SWBPaywallInfo,
        result: SWBPaywallResult,
        shouldDismiss: Boolean,
    ) {
        val info = paywallInfo.toModel()
        val paywallResult = result.toModel()
        scope.launch {
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
