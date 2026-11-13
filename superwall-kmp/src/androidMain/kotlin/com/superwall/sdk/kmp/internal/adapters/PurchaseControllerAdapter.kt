package com.superwall.sdk.kmp.internal.adapters

import android.app.Activity
import com.superwall.sdk.kmp.PurchaseController
import com.superwall.sdk.kmp.internal.mappers.toKmp
import com.superwall.sdk.kmp.internal.mappers.toNative
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.store.ProductReplacement
import com.superwall.sdk.kmp.models.store.ProductReplacementMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.superwall.sdk.delegate.PurchaseResult as NativePurchaseResult
import com.superwall.sdk.delegate.RestorationResult as NativeRestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController as NativePurchaseController
import com.superwall.sdk.store.ProductReplacement as NativeProductReplacement
import com.superwall.sdk.store.abstractions.product.StoreProduct as NativeStoreProduct

/**
 * Adapts the user's common [PurchaseController] to superwall-android's
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
     * Overrides the `StoreProduct` overload. Its interface default fails for
     * custom-store products and routes Play products to the deprecated
     * `ProductDetails` overload — overriding here instead means BOTH Play and
     * custom products reach the user's [PurchaseController.purchaseFromGooglePlay],
     * which is productId-based and store-agnostic.
     *
     * Since superwall-android 2.9.0 the SDK calls the five-argument overload
     * below instead; this one stays for any caller of the four-argument form.
     */
    override suspend fun purchase(
        activity: Activity,
        product: NativeStoreProduct,
        basePlanId: String?,
        offerId: String?,
    ): NativePurchaseResult = purchase(product, basePlanId, offerId, replacement = null)

    /**
     * The overload superwall-android 2.9.0+ calls. A purchase that replaces
     * an existing subscription reaches
     * [PurchaseController.purchaseFromGooglePlayWithReplacement] (whose default
     * falls back to [PurchaseController.purchaseFromGooglePlay]); every other
     * purchase reaches [PurchaseController.purchaseFromGooglePlay].
     */
    override suspend fun purchase(
        activity: Activity,
        product: NativeStoreProduct,
        basePlanId: String?,
        offerId: String?,
        replacement: NativeProductReplacement?,
    ): NativePurchaseResult = purchase(product, basePlanId, offerId, replacement?.toKmp())

    private suspend fun purchase(
        product: NativeStoreProduct,
        basePlanId: String?,
        offerId: String?,
        replacement: ProductReplacement?,
    ): NativePurchaseResult {
        val callbackName =
            if (replacement != null) "purchaseFromGooglePlayWithReplacement" else "purchaseFromGooglePlay"
        val result =
            try {
                // The StoreProduct overload: its default picks one of the
                // product-id methods named above, and a controller that
                // overrides it sees isCustomProduct and customTransactionId.
                val mappedProduct = product.toKmp()
                withContext(Dispatchers.Main.immediate) {
                    controller.purchaseFromGooglePlay(
                        product = mappedProduct,
                        basePlanId = basePlanId,
                        offerId = offerId,
                        replacement = replacement,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                log("PurchaseController.$callbackName threw an exception", throwable)
                PurchaseResult.Failed(
                    throwable.localizedMessage ?: throwable.message
                        ?: "PurchaseController.$callbackName threw: $throwable",
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

internal fun NativeProductReplacement.toKmp(): ProductReplacement =
    ProductReplacement(
        mode =
            when (mode) {
                NativeProductReplacement.Mode.DEFAULT -> ProductReplacementMode.STORE_DEFAULT
                NativeProductReplacement.Mode.CHARGE_PRORATED -> ProductReplacementMode.CHARGE_PRORATED
                NativeProductReplacement.Mode.CHARGE_FULL_PRICE -> ProductReplacementMode.CHARGE_FULL_PRICE
            },
        oldProductId = oldProductId,
        oldPurchaseToken = oldPurchaseToken,
    )
