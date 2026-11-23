package com.superwall.sdk.kmp.internal

import com.android.billingclient.api.Purchase
import com.superwall.sdk.kmp.models.store.ObservedPurchase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `observePurchaseResult(code, message, List<ObservedPurchase>)` hands
 * superwall-android Play Billing purchases rebuilt from JSON and signature,
 * against the REAL `Purchase` parser.
 */
class ObservedPurchaseTest {
    // The shape Google Play returns from a purchase, as `Purchase.getOriginalJson()` has it.
    private fun playJson(
        orderId: String = "GPA.3345-1234-5678-90123",
        productId: String = "premium_monthly",
        token: String = "token-abc",
        purchaseState: Int = 0,
    ): String =
        """{"orderId":"$orderId","packageName":"com.example.app","productId":"$productId",""" +
            """"purchaseTime":1700000000000,"purchaseState":$purchaseState,"purchaseToken":"$token",""" +
            """"quantity":1,"autoRenewing":true,"acknowledged":false}"""

    private fun rebuild(vararg purchases: ObservedPurchase): List<Purchase> =
        purchases.toList().toPlayPurchases { throw AssertionError("unexpected invalid purchase", it) }

    @Test
    fun aPurchaseIsRebuiltFromItsOriginalJsonAndSignature() {
        val json = playJson()

        val purchase = rebuild(ObservedPurchase(originalJson = json, signature = "sig-1")).single()

        assertEquals(listOf("premium_monthly"), purchase.products)
        assertEquals("token-abc", purchase.purchaseToken)
        assertEquals("GPA.3345-1234-5678-90123", purchase.orderId)
        assertEquals(Purchase.PurchaseState.PURCHASED, purchase.purchaseState)
        assertEquals(1_700_000_000_000, purchase.purchaseTime)
        assertEquals("com.example.app", purchase.packageName)
        assertTrue(purchase.isAutoRenewing)
        // superwall-android verifies and reports the purchase from these two, so they must arrive untouched.
        assertEquals("sig-1", purchase.signature)
        assertEquals(json, purchase.originalJson)
    }

    @Test
    fun aPendingPurchaseStaysPending() {
        // Play's JSON says 4 for a pending purchase, which `Purchase` reports as PENDING (2).
        val purchase = rebuild(ObservedPurchase(playJson(purchaseState = 4), "sig")).single()

        assertEquals(Purchase.PurchaseState.PENDING, purchase.purchaseState)
    }

    @Test
    fun aMultiProductPurchaseKeepsEveryProduct() {
        val json = """{"orderId":"GPA.1","productIds":["a","b"],"purchaseToken":"t","purchaseState":0}"""

        val purchase = rebuild(ObservedPurchase(json, "sig")).single()

        assertEquals(listOf("a", "b"), purchase.products)
    }

    @Test
    fun purchasesKeepTheirOrderAndTheirOwnSignatures() {
        val purchases =
            rebuild(
                ObservedPurchase(playJson(productId = "first", token = "t1"), "sig-first"),
                ObservedPurchase(playJson(productId = "second", token = "t2"), "sig-second"),
            )

        assertEquals(listOf("t1" to "sig-first", "t2" to "sig-second"), purchases.map { it.purchaseToken to it.signature })
    }

    @Test
    fun aPurchaseWhoseJsonCannotBeReadIsReportedAndLeftOutWithoutLosingTheRest() {
        val invalid = mutableListOf<Throwable>()

        val purchases =
            listOf(
                ObservedPurchase("not json", "sig-bad"),
                ObservedPurchase(playJson(token = "kept"), "sig-good"),
                ObservedPurchase("", "sig-empty"),
                // Valid JSON, but Play Billing needs an object.
                ObservedPurchase("[1,2]", "sig-array"),
            ).toPlayPurchases { invalid += it }

        assertEquals(listOf("kept"), purchases.map { it.purchaseToken })
        assertEquals(3, invalid.size)
    }

    @Test
    fun noPurchasesRebuildToNone() {
        assertEquals(emptyList(), rebuild())
    }
}
