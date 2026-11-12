package com.superwall.sdk.kmp

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
}
