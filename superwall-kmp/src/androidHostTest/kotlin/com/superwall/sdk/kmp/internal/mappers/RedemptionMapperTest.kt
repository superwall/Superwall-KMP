package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.redemption.RedemptionPaywallProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.superwall.sdk.models.internal.RedemptionResult as NativeRedemptionResult

/** Redemption paywall info, against the REAL superwall-android type. */
class RedemptionMapperTest {
    @Test
    fun productIdentifierIsCarried() {
        val info =
            NativeRedemptionResult.PaywallInfo(
                identifier = "paywall",
                placementName = "campaign_trigger",
                placementParams = emptyMap(),
                variantId = "variant",
                experimentId = "experiment",
                productIdentifier = "annual",
            ).toKmp()

        assertEquals("annual", info.productIdentifier)
        assertEquals("campaign_trigger", info.placementName)
    }

    @Test
    fun missingProductIdentifierStaysNull() {
        val info =
            NativeRedemptionResult.PaywallInfo(
                identifier = "paywall",
                placementName = "campaign_trigger",
                placementParams = emptyMap(),
                variantId = "variant",
                experimentId = "experiment",
            ).toKmp()

        assertNull(info.productIdentifier)
    }

    @Test
    fun productIsCarriedWithItsAttributes() {
        val info =
            NativeRedemptionResult.PaywallInfo(
                identifier = "paywall",
                placementName = "campaign_trigger",
                placementParams = emptyMap(),
                variantId = "variant",
                experimentId = "experiment",
                productIdentifier = "annual",
                product =
                    NativeRedemptionResult.PaywallInfo.PaywallProduct(
                        identifier = "annual",
                        currencyCode = "USD",
                        rawPrice = 39.99,
                        price = "$39.99",
                        periodYears = 1,
                        trialPeriodDays = 7,
                        trialPeriodText = "7-day",
                    ),
            ).toKmp()

        assertEquals(
            RedemptionPaywallProduct(
                identifier = "annual",
                currencyCode = "USD",
                rawPrice = 39.99,
                price = "$39.99",
                periodYears = 1,
                trialPeriodDays = 7,
                trialPeriodText = "7-day",
            ),
            info.product,
        )
    }

    @Test
    fun missingProductStaysNull() {
        val info =
            NativeRedemptionResult.PaywallInfo(
                identifier = "paywall",
                placementName = "campaign_trigger",
                placementParams = emptyMap(),
                variantId = "variant",
                experimentId = "experiment",
            ).toKmp()

        assertNull(info.product)
    }
}
