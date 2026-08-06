package com.superwall.sdk.kmp.internal.adapters

import android.app.Activity
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
import com.superwall.sdk.store.abstractions.product.StoreProduct as NativeStoreProduct

/**
 * Adapts the user's common [PurchaseController] to superwall-android 2.8.0's
 * [NativePurchaseController].
 *
 * Both sides are suspend functions, so — unlike the Flutter host's
 * `PurchaseControllerHost.kt` `suspendCoroutine` shim — the user's
 * implementation is called **directly** from the native SDK's coroutine
 *, hopped onto `Dispatchers.Main.immediate` to honor the
 * delivery contract. There is no `runBlocking` anywhere on this path: the
 * native SDK invokes these `@MainThread` and the call suspends rather than
 * blocks; the user's implementation may switch dispatchers freely.
 *
 * Exception safety anything the user's implementation throws is
 * caught, logged via the bridge's `handleLog` path, and mapped to the domain
 * failure ([PurchaseResult.Failed] / [RestorationResult.Failed]) — it never
 * propagates into native SDK internals. Coroutine cancellation is rethrown,
 * not converted.
 */
internal class PurchaseControllerAdapter(
    private val controller: PurchaseController,
    private val log: (message: String, error: Throwable?) -> Unit,
) : NativePurchaseController {
    /**
     * Overrides superwall-android 2.8.0's primary entry point (the
     * `StoreProduct` overload). Its interface default fails for custom-store
     * products and routes Play products to the deprecated `ProductDetails`
     * overload — overriding here instead means BOTH Play and custom products
     * reach the user's [PurchaseController.purchaseFromGooglePlay], which is
     * productId-based and store-agnostic.
     */
    override suspend fun purchase(
        activity: Activity,
        product: NativeStoreProduct,
        basePlanId: String?,
        offerId: String?,
    ): NativePurchaseResult {
        val result =
            try {
                withContext(Dispatchers.Main.immediate) {
                    controller.purchaseFromGooglePlay(
                        productId = product.productIdentifier,
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
