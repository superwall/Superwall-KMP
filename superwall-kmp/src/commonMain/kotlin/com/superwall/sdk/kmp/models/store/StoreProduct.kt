package com.superwall.sdk.kmp.models.store

import com.superwall.sdk.kmp.models.entitlements.Entitlement
import kotlin.time.Instant

/**
 * A wrapper around a store product.
 */
public data class StoreProduct(
    /** The entitlements associated with this product. */
    val entitlements: Set<Entitlement>,
    /** The product's identifier. */
    val productIdentifier: String,
    /** A dictionary of localized, formatted attributes of the product usable in paywall templating. */
    val attributes: Map<String, String>,
    /** The price of the product, localized and formatted for the user's locale (e.g. `$9.99`). */
    val localizedPrice: String,
    /** The subscription period of the product, localized for the user's locale (e.g. `1 month`). */
    val localizedSubscriptionPeriod: String,
    /** The subscription period unit of the product: `day`, `week`, `month`, or `year`. */
    val period: String,
    /** The subscription period of the product in adverb form: `daily`, `weekly`, `monthly`, or `yearly`. */
    val periodly: String,
    /** The subscription period of the product in weeks. */
    val periodWeeks: Int,
    /** The subscription period of the product in weeks, as a string. */
    val periodWeeksString: String,
    /** The subscription period of the product in months. */
    val periodMonths: Int,
    /** The subscription period of the product in months, as a string. */
    val periodMonthsString: String,
    /** The subscription period of the product in years. */
    val periodYears: Int,
    /** The subscription period of the product in years, as a string. */
    val periodYearsString: String,
    /** The subscription period of the product in days. */
    val periodDays: Int,
    /** The subscription period of the product in days, as a string. */
    val periodDaysString: String,
    /** The price of the product per day, localized and formatted. */
    val dailyPrice: String,
    /** The price of the product per week, localized and formatted. */
    val weeklyPrice: String,
    /** The price of the product per month, localized and formatted. */
    val monthlyPrice: String,
    /** The price of the product per year, localized and formatted. */
    val yearlyPrice: String,
    /** Whether the product has an introductory free trial. */
    val hasFreeTrial: Boolean,
    /** The date the trial period ends, formatted as a string (e.g. `June 21, 2024`). */
    val trialPeriodEndDateString: String,
    /** The price of the trial period, localized and formatted. */
    val localizedTrialPeriodPrice: String,
    /** The price of the trial period. */
    val trialPeriodPrice: Double,
    /** The trial period of the product in days. */
    val trialPeriodDays: Int,
    /** The trial period of the product in days, as a string. */
    val trialPeriodDaysString: String,
    /** The trial period of the product in weeks. */
    val trialPeriodWeeks: Int,
    /** The trial period of the product in weeks, as a string. */
    val trialPeriodWeeksString: String,
    /** The trial period of the product in months. */
    val trialPeriodMonths: Int,
    /** The trial period of the product in months, as a string. */
    val trialPeriodMonthsString: String,
    /** The trial period of the product in years. */
    val trialPeriodYears: Int,
    /** The trial period of the product in years, as a string. */
    val trialPeriodYearsString: String,
    /** The trial period as user-facing text (e.g. `7-day`). */
    val trialPeriodText: String,
    /** The locale of the product's price formatting. */
    val locale: String,
    /** Whether the product is family shareable (App Store). */
    val isFamilyShareable: Boolean,
    /** The price of the product in the local currency. */
    val price: Double,
    /** The identifier of the subscription group the product belongs to (App Store). */
    val subscriptionGroupIdentifier: String? = null,
    /** The date the trial period ends. */
    val trialPeriodEndDate: Instant? = null,
    /** The language code of the product's locale (e.g. `en`). */
    val languageCode: String? = null,
    /** The currency symbol of the product's price (e.g. `$`). */
    val currencySymbol: String? = null,
    /** The currency code of the product's price (e.g. `USD`). */
    val currencyCode: String? = null,
    /** The region code of the product's locale (e.g. `US`). */
    val regionCode: String? = null,
)
