@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.internal.mappers.toModel
import com.superwall.sdk.kmp.internal.ios.interop.SWBStoreProduct
import com.superwall.sdk.kmp.PurchaseController
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseControllerProtocol
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestorationResult
import com.superwall.sdk.kmp.internal.mappers.toSWB
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.darwin.NSObject

/**
 * The Kotlin implementation of the bridge's `@objc(SWBPurchaseController)`
 * protocol: handed to `SWBSuperwallBridge.configure` and forwarding purchase
 * and restore requests to the user's suspend [PurchaseController].
 *
 * THREADING (critical): SuperwallKit invokes these methods on
 * the MAIN ACTOR. The suspend work is `launch`ed on [scope]
 * (`Dispatchers.Main.immediate` + SupervisorJob) — NEVER `runBlocking`, which
 * would deadlock the main actor. The completion is invoked exactly once from
 * whatever context the user's implementation completes on; the Swift-side
 * forwarder resumes its continuation safely from any queue.
 *
 * Exceptions thrown by the user's implementation are caught, reported
 * through [log] (the app's `handleLog`, as on Android) and mapped to domain
 * failures — they never propagate into native internals and never kill the
 * supervisor scope.
 *
 * Retention: the [com.superwall.sdk.kmp.internal.IosSuperwallBridge] strongly
 * retains this adapter for the lifetime of the process (never rely on ObjC
 * retaining Kotlin-implemented objects).
 */
internal class PurchaseControllerAdapter(
    private val controller: PurchaseController,
    private val scope: CoroutineScope,
    private val log: (message: String, error: Throwable?) -> Unit,
) : NSObject(), SWBPurchaseControllerProtocol {
    override fun purchaseWithProduct(
        product: SWBStoreProduct,
        completion: (SWBPurchaseResult?) -> Unit,
    ) {
        // The StoreProduct overload: its default calls the product-id method,
        // and a controller that overrides it sees customTransactionId.
        val mappedProduct = product.toModel()
        scope.launch {
            val result = try {
                controller.purchaseFromAppStore(mappedProduct)
            } catch (throwable: Throwable) {
                log("PurchaseController.purchaseFromAppStore threw an exception", throwable)
                PurchaseResult.Failed(throwable.message ?: "PurchaseController threw: $throwable")
            }
            completion(result.toSWB())
        }
    }

    override fun restorePurchasesWithCompletion(completion: (SWBRestorationResult?) -> Unit) {
        scope.launch {
            val result = try {
                controller.restorePurchases()
            } catch (throwable: Throwable) {
                log("PurchaseController.restorePurchases threw an exception", throwable)
                RestorationResult.Failed(throwable.message ?: "PurchaseController threw: $throwable")
            }
            completion(result.toSWB())
        }
    }
}
