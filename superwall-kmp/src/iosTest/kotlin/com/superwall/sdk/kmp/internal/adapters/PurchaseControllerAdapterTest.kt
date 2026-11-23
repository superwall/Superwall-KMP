@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.PurchaseController
import com.superwall.sdk.kmp.internal.RecordingLog
import com.superwall.sdk.kmp.internal.inPlaceScope
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResultCaseCancelled
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResultCaseFailed
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResultCasePending
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResultCasePurchased
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestorationResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestorationResultCaseFailed
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestorationResultCaseRestored
import com.superwall.sdk.kmp.internal.swbStoreProduct
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.store.StoreProduct
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PurchaseControllerAdapterTest {
    private val log = RecordingLog()

    private fun adapter(controller: PurchaseController) = PurchaseControllerAdapter(controller, inPlaceScope(), log.log)

    private fun PurchaseControllerAdapter.purchase(productId: String): SWBPurchaseResult {
        val results = mutableListOf<SWBPurchaseResult?>()
        purchaseWithProduct(swbStoreProduct(productId)) { results += it }
        return results.single()!!
    }

    private fun PurchaseControllerAdapter.restore(): SWBRestorationResult {
        val results = mutableListOf<SWBRestorationResult?>()
        restorePurchasesWithCompletion { results += it }
        return results.single()!!
    }

    @Test
    fun purchaseReachesAProductIdControllerWithTheProductsIdentifier() {
        val controller = ProductIdController()

        val result = adapter(controller).purchase("annual")

        assertEquals(listOf("annual"), controller.purchases)
        assertEquals(SWBPurchaseResultCasePurchased, result.result())
        assertEquals(null, result.errorMessage())
    }

    @Test
    fun purchaseHandsTheWholeProductToAControllerThatTakesIt() {
        val products = mutableListOf<StoreProduct>()
        val controller =
            object : ProductIdController() {
                override suspend fun purchaseFromAppStore(product: StoreProduct): PurchaseResult {
                    products += product
                    return PurchaseResult.Pending
                }
            }

        val result = adapter(controller).purchase("annual")

        assertEquals(listOf("annual"), products.map { it.productIdentifier })
        // The override replaces the product-id method rather than adding to it.
        assertTrue(controller.purchases.isEmpty())
        assertEquals(SWBPurchaseResultCasePending, result.result())
    }

    @Test
    fun eachPurchaseResultMapsToItsBridgeCase() {
        val controller = ProductIdController()
        val adapter = adapter(controller)

        controller.purchaseResult = { PurchaseResult.Cancelled }
        assertEquals(SWBPurchaseResultCaseCancelled, adapter.purchase("annual").result())

        controller.purchaseResult = { PurchaseResult.Failed("card declined") }
        val failed = adapter.purchase("annual")
        assertEquals(SWBPurchaseResultCaseFailed, failed.result())
        assertEquals("card declined", failed.errorMessage())
        assertTrue(log.entries.isEmpty())
    }

    @Test
    fun purchaseCompletesOnlyOnceTheControllerReturns() {
        val outcome = CompletableDeferred<PurchaseResult>()
        val controller = ProductIdController().apply { purchaseResult = { outcome.await() } }
        val results = mutableListOf<SWBPurchaseResult?>()

        adapter(controller).purchaseWithProduct(swbStoreProduct("annual")) { results += it }
        assertTrue(results.isEmpty())

        outcome.complete(PurchaseResult.Purchased)
        assertEquals(SWBPurchaseResultCasePurchased, results.single()!!.result())
    }

    @Test
    fun aThrowingPurchaseCompletesAsFailedWithTheMessageAndIsLogged() {
        val failure = IllegalStateException("store unavailable")
        val controller = ProductIdController().apply { purchaseResult = { throw failure } }

        val result = adapter(controller).purchase("annual")

        assertEquals(SWBPurchaseResultCaseFailed, result.result())
        assertEquals("store unavailable", result.errorMessage())
        assertEquals("PurchaseController.purchaseFromAppStore threw an exception", log.entries.single().first)
        assertSame(failure, log.entries.single().second)
    }

    @Test
    fun aThrowingPurchaseWithNoMessageStillSaysWhatWasThrown() {
        val controller = ProductIdController().apply { purchaseResult = { throw IllegalStateException() } }

        val result = adapter(controller).purchase("annual")

        assertEquals(SWBPurchaseResultCaseFailed, result.result())
        assertTrue(result.errorMessage()!!.contains("IllegalStateException"), result.errorMessage())
    }

    @Test
    fun restoreMapsBothOutcomes() {
        val controller = ProductIdController()
        val adapter = adapter(controller)

        val restored = adapter.restore()
        assertEquals(SWBRestorationResultCaseRestored, restored.result())
        assertEquals(null, restored.errorMessage())

        controller.restoreResult = { RestorationResult.Failed("nothing to restore") }
        val failed = adapter.restore()
        assertEquals(SWBRestorationResultCaseFailed, failed.result())
        assertEquals("nothing to restore", failed.errorMessage())
        assertTrue(log.entries.isEmpty())
    }

    @Test
    fun aThrowingRestoreCompletesAsFailedWithTheMessageAndIsLogged() {
        val failure = IllegalStateException("store unavailable")
        val controller = ProductIdController().apply { restoreResult = { throw failure } }

        val result = adapter(controller).restore()

        assertEquals(SWBRestorationResultCaseFailed, result.result())
        assertEquals("store unavailable", result.errorMessage())
        assertEquals("PurchaseController.restorePurchases threw an exception", log.entries.single().first)
        assertSame(failure, log.entries.single().second)
    }

    /** A controller that implements only the required, product-id methods. */
    private open class ProductIdController : PurchaseController {
        val purchases = mutableListOf<String>()
        var purchaseResult: suspend () -> PurchaseResult = { PurchaseResult.Purchased }
        var restoreResult: suspend () -> RestorationResult = { RestorationResult.Restored }

        override suspend fun purchaseFromAppStore(productId: String): PurchaseResult {
            purchases += productId
            return purchaseResult()
        }

        override suspend fun purchaseFromGooglePlay(
            productId: String,
            basePlanId: String?,
            offerId: String?,
        ): PurchaseResult = error("Google Play is never asked on iOS")

        override suspend fun restorePurchases(): RestorationResult = restoreResult()
    }
}
