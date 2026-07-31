package com.superwall.sdk.kmp.internal.adapters

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.superwall.sdk.kmp.PurchaseController
import com.superwall.sdk.kmp.internal.mappers.toNative
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.superwall.sdk.delegate.PurchaseResult as NativePurchaseResult
import com.superwall.sdk.delegate.RestorationResult as NativeRestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController as NativePurchaseController

/**
 * Adapts the user's common [PurchaseController] to superwall-android 2.7.11's
 * [NativePurchaseController].
 *
 * Both sides are suspend functions, so — unlike the Flutter host's
 * `PurchaseControllerHost.kt` `suspendCoroutine` shim — the user's
 * implementation is called **directly** from the native SDK's coroutine
 * (plan §4), hopped onto `Dispatchers.Main.immediate` to honor the plan §6
 * delivery contract. There is no `runBlocking` anywhere on this path: the
 * native SDK invokes these `@MainThread` and the call suspends rather than
 * blocks; the user's implementation may switch dispatchers freely.
 *
 * Exception safety (plan §6.5): anything the user's implementation throws is
 * caught, logged via the bridge's `handleLog` path, and mapped to the domain
 * failure ([PurchaseResult.Failed] / [RestorationResult.Failed]) — it never
 * propagates into native SDK internals. Coroutine cancellation is rethrown,
 * not converted.
 */
internal class PurchaseControllerAdapter(
    private val controller: PurchaseController,
    private val log: (message: String, error: Throwable?) -> Unit,
) : NativePurchaseController {
    override suspend fun purchase(
        activity: Activity,
        productDetails: ProductDetails,
        basePlanId: String?,
        offerId: String?,
    ): NativePurchaseResult {
        val result =
            try {
                withContext(Dispatchers.Main.immediate) {
                    controller.purchaseFromGooglePlay(
                        productId = productDetails.productId,
                        basePlanId = basePlanId,
                        offerId = offerId,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                log("PurchaseController.purchaseFromGooglePlay threw an exception", throwable)
                PurchaseResult.Failed(
                    throwable.localizedMessage ?: throwable.message
                        ?: "PurchaseController.purchaseFromGooglePlay threw: $throwable",
                )
            }
        return result.toNative()
    }

    override suspend fun restorePurchases(): NativeRestorationResult {
        val result =
            try {
                withContext(Dispatchers.Main.immediate) {
                    controller.restorePurchases()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                log("PurchaseController.restorePurchases threw an exception", throwable)
                RestorationResult.Failed(
                    throwable.localizedMessage ?: throwable.message
                        ?: "PurchaseController.restorePurchases threw: $throwable",
                )
            }
        return result.toNative()
    }
}
