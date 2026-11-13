package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.store.StoreProduct

/** A [StoreProduct] with only [productIdentifier] set; every other required field is empty or zero. */
internal fun storeProduct(productIdentifier: String): StoreProduct =
    StoreProduct(
        entitlements = emptySet(),
        productIdentifier = productIdentifier,
        attributes = emptyMap(),
        localizedPrice = "",
        localizedSubscriptionPeriod = "",
        period = "",
        periodly = "",
        periodWeeks = 0,
        periodWeeksString = "",
        periodMonths = 0,
        periodMonthsString = "",
        periodYears = 0,
        periodYearsString = "",
        periodDays = 0,
        periodDaysString = "",
        dailyPrice = "",
        weeklyPrice = "",
        monthlyPrice = "",
        yearlyPrice = "",
        hasFreeTrial = false,
        trialPeriodEndDateString = "",
        localizedTrialPeriodPrice = "",
        trialPeriodPrice = 0.0,
        trialPeriodDays = 0,
        trialPeriodDaysString = "",
        trialPeriodWeeks = 0,
        trialPeriodWeeksString = "",
        trialPeriodMonths = 0,
        trialPeriodMonthsString = "",
        trialPeriodYears = 0,
        trialPeriodYearsString = "",
        trialPeriodText = "",
        locale = "",
        isFamilyShareable = false,
        price = 0.0,
    )
