package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.store.ProductReplacement
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The replacement-aware `purchaseFromGooglePlay` overload is what Android calls
 * for every purchase, so its default must keep controllers written before it
 * existed working unchanged.
 */
class PurchaseControllerDefaultsTest {
    private class LegacyController : PurchaseController {
        val calls = mutableListOf<Triple<String, String?, String?>>()

        override suspend fun purchaseFromAppStore(productId: String): PurchaseResult = PurchaseResult.cancelled()

        override suspend fun purchaseFromGooglePlay(
            productId: String,
            basePlanId: String?,
            offerId: String?,
        ): PurchaseResult {
            calls += Triple(productId, basePlanId, offerId)
            return PurchaseResult.purchased()
        }

        override suspend fun restorePurchases(): RestorationResult = RestorationResult.restored()
    }

    @Test
    fun replacementOverload_defaultsToThreeArgumentPurchase() =
        runTest {
            val controller = LegacyController()
            val replacement =
                ProductReplacement(ProductReplacement.Mode.CHARGE_PRORATED, oldProductId = "monthly", oldPurchaseToken = "t")

            val result = controller.purchaseFromGooglePlay("annual", "base", "offer", replacement)

            assertEquals(PurchaseResult.purchased(), result)
            assertEquals<List<Triple<String, String?, String?>>>(listOf(Triple("annual", "base", "offer")), controller.calls)
        }
}
