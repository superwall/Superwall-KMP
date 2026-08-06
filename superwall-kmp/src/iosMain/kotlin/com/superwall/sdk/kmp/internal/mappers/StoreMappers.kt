@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlement
import com.superwall.sdk.kmp.internal.ios.interop.SWBStoreProduct
import com.superwall.sdk.kmp.internal.ios.interop.SWBStoreTransaction
import com.superwall.sdk.kmp.models.store.StoreProduct
import com.superwall.sdk.kmp.models.store.StoreTransaction
import kotlin.time.Instant

/**
 * Parses the ISO-8601 date strings the SWB StoreTransaction/StoreProduct
 * family carries (and the native PaywallInfo time strings) into
 * [kotlin.time.Instant]. Unparseable values degrade to `null` rather than
 * crashing; the bridge emits
 * `yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX`, which `Instant.parse` accepts.
 */
internal fun String?.toInstantOrNull(): Instant? =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() }

internal fun SWBStoreTransaction.toModel(): StoreTransaction =
    StoreTransaction(
        configRequestId = configRequestId(),
        appSessionId = appSessionId(),
        originalTransactionIdentifier = originalTransactionIdentifier(),
        transactionDate = transactionDate().toInstantOrNull(),
        storeTransactionId = storeTransactionId(),
        originalTransactionDate = originalTransactionDate().toInstantOrNull(),
        webOrderLineItemID = webOrderLineItemID(),
        appBundleId = appBundleId(),
        subscriptionGroupId = subscriptionGroupId(),
        isUpgraded = isUpgraded()?.boolValue,
        expirationDate = expirationDate().toInstantOrNull(),
        offerId = offerId(),
        revocationDate = revocationDate().toInstantOrNull(),
    )

internal fun SWBStoreProduct.toModel(): StoreProduct =
    StoreProduct(
        entitlements = entitlements().mapNotNull { (it as? SWBEntitlement)?.toModel() }.toSet(),
        productIdentifier = productIdentifier(),
        attributes = attributes().entries.associate { (key, value) ->
            (key?.toString() ?: "") to (value?.toString() ?: "")
        },
        localizedPrice = localizedPrice(),
        localizedSubscriptionPeriod = localizedSubscriptionPeriod(),
        period = period(),
        periodly = periodly(),
        periodWeeks = periodWeeks().toInt(),
        periodWeeksString = periodWeeksString(),
        periodMonths = periodMonths().toInt(),
        periodMonthsString = periodMonthsString(),
        periodYears = periodYears().toInt(),
        periodYearsString = periodYearsString(),
        periodDays = periodDays().toInt(),
        periodDaysString = periodDaysString(),
        dailyPrice = dailyPrice(),
        weeklyPrice = weeklyPrice(),
        monthlyPrice = monthlyPrice(),
        yearlyPrice = yearlyPrice(),
        hasFreeTrial = hasFreeTrial(),
        trialPeriodEndDateString = trialPeriodEndDateString(),
        localizedTrialPeriodPrice = localizedTrialPeriodPrice(),
        trialPeriodPrice = trialPeriodPrice(),
        trialPeriodDays = trialPeriodDays().toInt(),
        trialPeriodDaysString = trialPeriodDaysString(),
        trialPeriodWeeks = trialPeriodWeeks().toInt(),
        trialPeriodWeeksString = trialPeriodWeeksString(),
        trialPeriodMonths = trialPeriodMonths().toInt(),
        trialPeriodMonthsString = trialPeriodMonthsString(),
        trialPeriodYears = trialPeriodYears().toInt(),
        trialPeriodYearsString = trialPeriodYearsString(),
        trialPeriodText = trialPeriodText(),
        locale = locale(),
        isFamilyShareable = isFamilyShareable(),
        price = price(),
        subscriptionGroupIdentifier = subscriptionGroupIdentifier(),
        trialPeriodEndDate = trialPeriodEndDate().toInstantOrNull(),
        languageCode = languageCode(),
        currencySymbol = currencySymbol(),
        currencyCode = currencyCode(),
        regionCode = regionCode(),
    )
