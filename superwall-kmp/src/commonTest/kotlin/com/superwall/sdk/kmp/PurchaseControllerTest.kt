package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.store.StoreProduct
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.store.ProductReplacement
import com.superwall.sdk.kmp.models.store.ProductReplacementMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PurchaseControllerTest {
    @Test
    fun replacementPurchaseDefaultsToAPlainGooglePlayPurchase() =
        runTest {
            val calls = mutableListOf<Triple<String, String?, String?>>()
            val controller =
                object : PurchaseController {
                    override suspend fun purchaseFromAppStore(productId: String): PurchaseResult = PurchaseResult.Cancelled

                    override suspend fun purchaseFromGooglePlay(
                        productId: String,
                        basePlanId: String?,
                        offerId: String?,
                    ): PurchaseResult {
                        calls += Triple(productId, basePlanId, offerId)
                        return PurchaseResult.Purchased
                    }

                    override suspend fun restorePurchases(): RestorationResult = RestorationResult.Restored
                }

            val result =
                controller.purchaseFromGooglePlayWithReplacement(
                    productId = "annual",
                    basePlanId = "base",
                    offerId = null,
                    replacement = ProductReplacement(ProductReplacementMode.STORE_DEFAULT, "monthly", "token"),
                )

            assertEquals(PurchaseResult.Purchased, result)
            assertEquals(listOf(Triple<String, String?, String?>("annual", "base", null)), calls)
        }

    /** A controller written before the StoreProduct overloads existed. */
    private class ProductIdController : PurchaseController {
        val calls = mutableListOf<String>()

        override suspend fun purchaseFromAppStore(productId: String): PurchaseResult {
            calls += "appStore:$productId"
            return PurchaseResult.Purchased
        }

        override suspend fun purchaseFromGooglePlay(
            productId: String,
            basePlanId: String?,
            offerId: String?,
        ): PurchaseResult {
            calls += "googlePlay:$productId:$basePlanId:$offerId"
            return PurchaseResult.Purchased
        }

        override suspend fun purchaseFromGooglePlayWithReplacement(
            productId: String,
            basePlanId: String?,
            offerId: String?,
            replacement: ProductReplacement,
        ): PurchaseResult {
            calls += "replacement:$productId:${replacement.oldProductId}"
            return PurchaseResult.Pending
        }

        override suspend fun restorePurchases(): RestorationResult = RestorationResult.Restored
    }

    @Test
    fun productOverloadsDefaultToTheProductIdMethods() =
        runTest {
            val controller = ProductIdController()
            val product = storeProduct("annual")

            assertEquals(PurchaseResult.Purchased, controller.purchaseFromAppStore(product))
            assertEquals(PurchaseResult.Purchased, controller.purchaseFromGooglePlay(product, "base", "offer", replacement = null))
            assertEquals(
                PurchaseResult.Pending,
                controller.purchaseFromGooglePlay(
                    product,
                    "base",
                    null,
                    replacement = ProductReplacement(ProductReplacementMode.CHARGE_FULL_PRICE, "monthly", "token"),
                ),
            )

            assertEquals(listOf("appStore:annual", "googlePlay:annual:base:offer", "replacement:annual:monthly"), controller.calls)
        }

    @Test
    fun aControllerCanTellACustomProductByOverridingTheProductOverload() =
        runTest {
            val seen = mutableListOf<Pair<Boolean, String?>>()
            val controller =
                object : PurchaseController {
                    override suspend fun purchaseFromAppStore(productId: String): PurchaseResult = PurchaseResult.Cancelled

                    override suspend fun purchaseFromAppStore(product: StoreProduct): PurchaseResult {
                        seen += product.isCustomProduct to product.customTransactionId
                        return PurchaseResult.Purchased
                    }

                    override suspend fun purchaseFromGooglePlay(
                        productId: String,
                        basePlanId: String?,
                        offerId: String?,
                    ): PurchaseResult = PurchaseResult.Cancelled

                    override suspend fun restorePurchases(): RestorationResult = RestorationResult.Restored
                }

            val custom = storeProduct("web").copy(isCustomProduct = true, customTransactionId = "txn-1")
            assertEquals(PurchaseResult.Purchased, controller.purchaseFromAppStore(custom))
            assertEquals(listOf<Pair<Boolean, String?>>(true to "txn-1"), seen)
        }
}
