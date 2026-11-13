package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import com.superwall.sdk.kmp.models.store.SubscriptionPeriodUnit
import com.superwall.sdk.kmp.models.store.TransactionProduct
import java.math.BigDecimal
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import com.superwall.sdk.analytics.superwall.TransactionProduct as NativeTransactionProduct
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason as NativePaywallSkippedReason
import com.superwall.sdk.store.abstractions.product.SubscriptionPeriod as NativeSubscriptionPeriod

/** The typed transaction product, against the REAL superwall-android type. */
class TransactionProductMapperTest {
    private val nativePrice =
        NativeTransactionProduct.Price(
            raw = BigDecimal("9.99"),
            localized = "$9.99",
            daily = "$0.33",
            weekly = "$2.33",
            monthly = "$9.99",
            yearly = "$119.88",
        )

    @Test
    fun everyFieldIsCarried() {
        val product =
            NativeTransactionProduct(
                id = "monthly",
                price = nativePrice,
                trialPeriod =
                    NativeTransactionProduct.TrialPeriod(days = 7, weeks = 1, months = 0, years = 0, text = "7-day", endAt = Date(1_000L)),
                period =
                    NativeTransactionProduct.Period(
                        alt = "1 month",
                        ly = "monthly",
                        unit = NativeSubscriptionPeriod.Unit.month,
                        days = 30,
                        weeks = 4,
                        months = 1,
                        years = 0,
                    ),
                locale = "en_US",
                languageCode = "en",
                currency = NativeTransactionProduct.Currency(code = "USD", symbol = "$"),
            ).toKmpTransactionProduct()

        assertEquals(
            TransactionProduct(
                id = "monthly",
                price = TransactionProduct.Price(9.99, "$9.99", "$0.33", "$2.33", "$9.99", "$119.88"),
                trialPeriod = TransactionProduct.TrialPeriod(7, 1, 0, 0, "7-day", Instant.fromEpochMilliseconds(1_000L)),
                period = TransactionProduct.Period("1 month", "monthly", SubscriptionPeriodUnit.MONTH, 30, 4, 1, 0),
                locale = "en_US",
                languageCode = "en",
                currency = TransactionProduct.Currency("USD", "$"),
            ),
            product,
        )
    }

    @Test
    fun aOneOffProductHasNoPeriodOrTrial() {
        val product =
            NativeTransactionProduct(
                id = "coins",
                price = nativePrice,
                trialPeriod = null,
                period = null,
                locale = "en_US",
                languageCode = null,
                currency = NativeTransactionProduct.Currency(code = null, symbol = null),
            ).toKmpTransactionProduct()

        assertEquals(null, product.period)
        assertEquals(null, product.trialPeriod)
        assertEquals(TransactionProduct.Currency(), product.currency)
    }
}

/** The legacy native skip reason no longer hides behind NoAudienceMatch. */
class PaywallSkippedReasonMapperTest {
    @Test
    fun everyNativeReasonWithoutAPayloadMapsToItsOwnCase() {
        assertEquals(PaywallSkippedReason.NoAudienceMatch, NativePaywallSkippedReason.NoAudienceMatch().toKmp())
        assertEquals(PaywallSkippedReason.PlacementNotFound, NativePaywallSkippedReason.PlacementNotFound().toKmp())
        assertEquals(PaywallSkippedReason.UserIsSubscribed, NativePaywallSkippedReason.UserIsSubscribed().toKmp())
    }
}
