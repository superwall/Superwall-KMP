package com.superwall.sdk.kmp.models.store

import kotlin.time.Instant

/**
 * The product of a transaction as the native SDKs report it on
 * `EventType.NON_RECURRING_PRODUCT_PURCHASE`: a smaller snapshot than
 * [StoreProduct], with the same shape on both platforms.
 */
public data class TransactionProduct(
    /** The product's identifier. */
    val id: String,
    /** The product's price. */
    val price: Price,
    /** The trial period, or `null` when the product has none. */
    val trialPeriod: TrialPeriod? = null,
    /** The subscription period, or `null` for a product that isn't a subscription. */
    val period: Period? = null,
    /** The locale of the product's price formatting. */
    val locale: String,
    /** The language code of the product's locale (e.g. `en`). */
    val languageCode: String? = null,
    /** The currency of the product's price. */
    val currency: Currency,
) {
    /** A product's price: the [raw] amount and its localized, formatted forms. */
    public data class Price(
        val raw: Double,
        val localized: String,
        val daily: String,
        val weekly: String,
        val monthly: String,
        val yearly: String,
    )

    /** A trial period expressed in each unit, with its user-facing [text] (e.g. `7-day`). */
    public data class TrialPeriod(
        val days: Int,
        val weeks: Int,
        val months: Int,
        val years: Int,
        val text: String,
        /** When the trial ends. */
        val endAt: Instant? = null,
    )

    /** A subscription period expressed in each unit. */
    public data class Period(
        /** The localized period (e.g. `1 month`). */
        val alt: String,
        /** The period in adverb form (e.g. `monthly`). */
        val ly: String,
        val unit: SubscriptionPeriodUnit,
        val days: Int,
        val weeks: Int,
        val months: Int,
        val years: Int,
    )

    /** A currency's [code] (e.g. `USD`) and [symbol] (e.g. `$`). */
    public data class Currency(
        val code: String? = null,
        val symbol: String? = null,
    )
}
