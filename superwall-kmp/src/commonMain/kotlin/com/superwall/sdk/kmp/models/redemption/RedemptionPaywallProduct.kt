package com.superwall.sdk.kmp.models.redemption

/**
 * The product bought in the web checkout a redeemed purchase came from, with
 * the attributes a paywall can reference. Text fields the checkout didn't
 * supply are empty and numbers are zero.
 */
public data class RedemptionPaywallProduct(
    val identifier: String,
    val languageCode: String = "",
    val locale: String = "",
    val currencyCode: String = "",
    val currencySymbol: String = "",
    val period: String = "",
    val periodly: String = "",
    val localizedPeriod: String = "",
    val periodAlt: String = "",
    val periodDays: Int = 0,
    val periodWeeks: Int = 0,
    val periodMonths: Int = 0,
    val periodYears: Int = 0,
    val rawPrice: Double = 0.0,
    val price: String = "",
    val dailyPrice: String = "",
    val weeklyPrice: String = "",
    val monthlyPrice: String = "",
    val yearlyPrice: String = "",
    val rawTrialPeriodPrice: Double = 0.0,
    val trialPeriodPrice: String = "",
    val trialPeriodDailyPrice: String = "",
    val trialPeriodWeeklyPrice: String = "",
    val trialPeriodMonthlyPrice: String = "",
    val trialPeriodYearlyPrice: String = "",
    val trialPeriodDays: Int = 0,
    val trialPeriodWeeks: Int = 0,
    val trialPeriodMonths: Int = 0,
    val trialPeriodYears: Int = 0,
    val trialPeriodText: String = "",
    val trialPeriodEndDate: String = "",
)
