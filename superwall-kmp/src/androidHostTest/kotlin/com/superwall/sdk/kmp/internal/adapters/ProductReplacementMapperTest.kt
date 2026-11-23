package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.models.store.ProductReplacement
import com.superwall.sdk.kmp.models.store.ProductReplacementMode
import kotlin.test.Test
import kotlin.test.assertEquals
import com.superwall.sdk.store.ProductReplacement as NativeProductReplacement

/**
 * Tests the replacement handed to `PurchaseController.purchaseFromGooglePlayWithReplacement`
 * against the REAL superwall-android `ProductReplacement`.
 */
class ProductReplacementMapperTest {
    @Test
    fun everyNativeModeMaps() {
        assertEquals(
            listOf(
                ProductReplacementMode.STORE_DEFAULT,
                ProductReplacementMode.CHARGE_PRORATED,
                ProductReplacementMode.CHARGE_FULL_PRICE,
            ),
            NativeProductReplacement.Mode.entries.map {
                NativeProductReplacement(it, oldProductId = null, oldPurchaseToken = null).toKmp().mode
            },
        )
    }

    @Test
    fun oldProductAndTokenAreCarried() {
        val replacement =
            NativeProductReplacement(
                mode = NativeProductReplacement.Mode.CHARGE_PRORATED,
                oldProductId = "monthly",
                oldPurchaseToken = "token-123",
            ).toKmp()

        assertEquals(
            ProductReplacement(
                mode = ProductReplacementMode.CHARGE_PRORATED,
                oldProductId = "monthly",
                oldPurchaseToken = "token-123",
            ),
            replacement,
        )
    }
}
