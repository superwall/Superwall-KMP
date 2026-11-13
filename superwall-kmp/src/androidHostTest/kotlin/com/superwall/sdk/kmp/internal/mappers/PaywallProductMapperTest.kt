package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.entitlements.ProductStore
import com.superwall.sdk.kmp.models.paywall.PaywallProductOffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.superwall.sdk.models.product.CustomStoreProduct as NativeCustomStoreProduct
import com.superwall.sdk.models.product.Offer as NativeOffer
import com.superwall.sdk.models.product.PlayStoreProduct as NativePlayStoreProduct
import com.superwall.sdk.models.product.ProductItem as NativeProductItem
import com.superwall.sdk.models.product.UnknownStoreProduct as NativeUnknownStoreProduct

/** The paywall product's store, against the REAL superwall-android `ProductItem`. */
class PaywallProductMapperTest {
    private fun item(type: NativeProductItem.StoreProductType) =
        NativeProductItem(compositeId = "id", name = "primary", type = type, entitlements = emptySet())

    @Test
    fun storeFollowsTheProductType() {
        val play =
            item(
                NativeProductItem.StoreProductType.PlayStore(
                    NativePlayStoreProduct(productIdentifier = "annual", basePlanIdentifier = "base", offer = NativeOffer.NoOffer),
                ),
            ).toKmp()
        assertEquals(ProductStore.PLAY_STORE, play.store)
        assertEquals("annual:base:sw-none", play.id)
        assertEquals("primary", play.name)

        val custom = item(NativeProductItem.StoreProductType.Custom(NativeCustomStoreProduct(productIdentifier = "web"))).toKmp()
        assertEquals(ProductStore.CUSTOM, custom.store)

        val other = item(NativeProductItem.StoreProductType.Other(NativeUnknownStoreProduct(productIdentifier = "x"))).toKmp()
        assertEquals(ProductStore.OTHER, other.store)
    }

    @Test
    fun playProductCarriesBasePlanOfferAndFullIdentifier() {
        fun play(offer: NativeOffer) =
            item(
                NativeProductItem.StoreProductType.PlayStore(
                    NativePlayStoreProduct(productIdentifier = "annual", basePlanIdentifier = "base", offer = offer),
                ),
            ).toKmp()

        val noOffer = play(NativeOffer.NoOffer)
        assertEquals("base", noOffer.basePlanIdentifier)
        assertEquals(PaywallProductOffer.NoOffer, noOffer.offer)
        assertEquals("annual:base:sw-none", noOffer.fullIdentifier)
        assertNull(noOffer.trialDays)
        assertNull(noOffer.billingPlanType)

        assertEquals(PaywallProductOffer.Automatic, play(NativeOffer.Automatic()).offer)
        assertEquals(PaywallProductOffer.Specified("intro"), play(NativeOffer.Specified(offerIdentifier = "intro")).offer)
    }

    @Test
    fun nonPlayProductsHaveNoBasePlanOrOffer() {
        val custom = item(NativeProductItem.StoreProductType.Custom(NativeCustomStoreProduct(productIdentifier = "web"))).toKmp()
        assertNull(custom.basePlanIdentifier)
        assertNull(custom.offer)
    }
}
