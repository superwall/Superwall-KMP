package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.store.StoreProduct
import com.superwall.sdk.kmp.models.store.StoreTransaction
import java.util.Date
import java.util.Locale
import kotlin.time.Instant
import com.superwall.sdk.analytics.superwall.TransactionProduct as NativeTransactionProduct
import com.superwall.sdk.store.abstractions.product.StoreProductType as NativeStoreProductType
import com.superwall.sdk.store.abstractions.transactions.StoreTransaction as NativeStoreTransaction
import com.superwall.sdk.store.abstractions.transactions.StoreTransactionType as NativeStoreTransactionType

/**
 * Store-layer mappers: native product/transaction types to the common models.
 * Port of the Flutter host's json/StoreProductMapper.kt and
 * json/StoreTransactionMapper.kt, with the date convention changed from
 * ISO-8601 strings to [kotlin.time.Instant] native `java.util.Date`
 * values convert directly via epoch milliseconds — no formatting round trip.
 */

/** Converts a native `java.util.Date` to the common [Instant] via epoch millis. */
internal fun Date.toKmpInstant(): Instant = Instant.fromEpochMilliseconds(time)

/** Converts a common [Instant] back to a native `java.util.Date`. */
internal fun Instant.toNativeDate(): Date = Date(toEpochMilliseconds())

/**
 * Maps a native [NativeStoreProductType] (implemented by both the native
 * `StoreProduct` wrapper and `RawStoreProduct`) to the common [StoreProduct].
 *
 * Android gaps surfaced honestly `entitlements` is empty (the
 * native product type carries none), `subscriptionGroupIdentifier` is `null`
 * and `isFamilyShareable` is `false` (App Store concepts).
 */
internal fun NativeStoreProductType.toKmp(): StoreProduct =
    StoreProduct(
        entitlements = emptySet(),
        productIdentifier = productIdentifier,
        attributes = attributes,
        localizedPrice = localizedPrice,
        localizedSubscriptionPeriod = localizedSubscriptionPeriod,
        period = period,
        periodly = periodly,
        periodWeeks = periodWeeks,
        periodWeeksString = periodWeeksString,
        periodMonths = periodMonths,
        periodMonthsString = periodMonthsString,
        periodYears = periodYears,
        periodYearsString = periodYearsString,
        periodDays = periodDays,
        periodDaysString = periodDaysString,
        dailyPrice = dailyPrice,
        weeklyPrice = weeklyPrice,
        monthlyPrice = monthlyPrice,
        yearlyPrice = yearlyPrice,
        hasFreeTrial = hasFreeTrial,
        trialPeriodEndDateString = trialPeriodEndDateString,
        localizedTrialPeriodPrice = localizedTrialPeriodPrice,
        trialPeriodPrice = trialPeriodPrice.toDouble(),
        trialPeriodDays = trialPeriodDays,
        trialPeriodDaysString = trialPeriodDaysString,
        trialPeriodWeeks = trialPeriodWeeks,
        trialPeriodWeeksString = trialPeriodWeeksString,
        trialPeriodMonths = trialPeriodMonths,
        trialPeriodMonthsString = trialPeriodMonthsString,
        trialPeriodYears = trialPeriodYears,
        trialPeriodYearsString = trialPeriodYearsString,
        trialPeriodText = trialPeriodText,
        locale = locale,
        isFamilyShareable = false,
        price = price.toDouble(),
        subscriptionGroupIdentifier = null,
        trialPeriodEndDate = trialPeriodEndDate?.toKmpInstant(),
        languageCode = languageCode,
        currencySymbol = currencySymbol,
        currencyCode = currencyCode,
        regionCode = regionCode,
    )

/**
 * Maps the native [NativeTransactionProduct] (the reduced product snapshot
 * carried by `SuperwallEvent.NonRecurringProductPurchase`) to the common
 * [StoreProduct]. Port of the Flutter host's
 * `TransactionProduct.pigeonify()` — absent fields default to the same
 * empty/zero values.
 */
internal fun NativeTransactionProduct.toKmp(): StoreProduct {
    fun countString(
        count: Int?,
        unit: String,
    ): String = count?.takeIf { it > 0 }?.let { "$it $unit" } ?: ""

    return StoreProduct(
        entitlements = emptySet(),
        productIdentifier = id,
        attributes = emptyMap(),
        localizedPrice = price.localized,
        localizedSubscriptionPeriod = period?.alt ?: "",
        period = period?.unit?.name?.lowercase(Locale.getDefault()) ?: "",
        periodly = period?.ly ?: "",
        periodWeeks = period?.weeks ?: 0,
        periodWeeksString = countString(period?.weeks, "weeks"),
        periodMonths = period?.months ?: 0,
        periodMonthsString = countString(period?.months, "months"),
        periodYears = period?.years ?: 0,
        periodYearsString = countString(period?.years, "years"),
        periodDays = period?.days ?: 0,
        periodDaysString = countString(period?.days, "days"),
        dailyPrice = price.daily,
        weeklyPrice = price.weekly,
        monthlyPrice = price.monthly,
        yearlyPrice = price.yearly,
        hasFreeTrial = trialPeriod != null,
        trialPeriodEndDateString = trialPeriod?.endAt?.toKmpInstant()?.toString() ?: "",
        localizedTrialPeriodPrice = "",
        trialPeriodPrice = 0.0,
        trialPeriodDays = trialPeriod?.days ?: 0,
        trialPeriodDaysString = countString(trialPeriod?.days, "days"),
        trialPeriodWeeks = trialPeriod?.weeks ?: 0,
        trialPeriodWeeksString = countString(trialPeriod?.weeks, "weeks"),
        trialPeriodMonths = trialPeriod?.months ?: 0,
        trialPeriodMonthsString = countString(trialPeriod?.months, "months"),
        trialPeriodYears = trialPeriod?.years ?: 0,
        trialPeriodYearsString = countString(trialPeriod?.years, "years"),
        trialPeriodText = trialPeriod?.text ?: "",
        locale = locale,
        isFamilyShareable = false,
        price = price.raw.toDouble(),
        subscriptionGroupIdentifier = null,
        trialPeriodEndDate = trialPeriod?.endAt?.toKmpInstant(),
        languageCode = languageCode,
        currencySymbol = currency.symbol,
        currencyCode = currency.code,
        regionCode = null,
    )
}

/**
 * Maps the native [NativeStoreTransaction] class (which carries the real
 * `configRequestId`/`appSessionId`) to the common [StoreTransaction].
 */
internal fun NativeStoreTransaction.toKmp(): StoreTransaction =
    toKmpTransaction(
        configRequestId = configRequestId,
        appSessionId = appSessionId,
    )

/**
 * Maps a native [NativeStoreTransactionType] (the interface — e.g. the
 * `transaction` payload of `SuperwallEvent.TransactionComplete`) to the common
 * [StoreTransaction]. The interface carries no config-request/app-session ids,
 * so they map to `""` (Flutter host parity).
 */
internal fun NativeStoreTransactionType.toKmp(): StoreTransaction =
    when (this) {
        is NativeStoreTransaction -> toKmp()
        else -> toKmpTransaction(configRequestId = "", appSessionId = "")
    }

private fun NativeStoreTransactionType.toKmpTransaction(
    configRequestId: String,
    appSessionId: String,
): StoreTransaction =
    StoreTransaction(
        configRequestId = configRequestId,
        appSessionId = appSessionId,
        // Nullable on the native interface; "" matches the Flutter host.
        originalTransactionIdentifier = originalTransactionIdentifier ?: "",
        transactionDate = transactionDate?.toKmpInstant(),
        storeTransactionId = storeTransactionId,
        originalTransactionDate = originalTransactionDate?.toKmpInstant(),
        webOrderLineItemID = webOrderLineItemID,
        appBundleId = appBundleId,
        subscriptionGroupId = subscriptionGroupId,
        isUpgraded = isUpgraded,
        expirationDate = expirationDate?.toKmpInstant(),
        offerId = offerId,
        revocationDate = revocationDate?.toKmpInstant(),
    )
