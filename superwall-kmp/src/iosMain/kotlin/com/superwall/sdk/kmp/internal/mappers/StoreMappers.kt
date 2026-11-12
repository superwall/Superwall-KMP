@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.interop.SWB_STORE_TRANSACTION_STATE_DEFERRED
import com.superwall.sdk.kmp.internal.interop.SWB_STORE_TRANSACTION_STATE_FAILED
import com.superwall.sdk.kmp.internal.interop.SWB_STORE_TRANSACTION_STATE_PURCHASED
import com.superwall.sdk.kmp.internal.interop.SWB_STORE_TRANSACTION_STATE_PURCHASING
import com.superwall.sdk.kmp.internal.interop.SWB_STORE_TRANSACTION_STATE_RESTORED
import com.superwall.sdk.kmp.internal.interop.SWB_SUBSCRIPTION_PERIOD_UNIT_DAY
import com.superwall.sdk.kmp.internal.interop.SWB_SUBSCRIPTION_PERIOD_UNIT_MONTH
import com.superwall.sdk.kmp.internal.interop.SWB_SUBSCRIPTION_PERIOD_UNIT_WEEK
import com.superwall.sdk.kmp.internal.interop.SWB_SUBSCRIPTION_PERIOD_UNIT_YEAR
import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlement
import com.superwall.sdk.kmp.internal.ios.interop.SWBStoreProduct
import com.superwall.sdk.kmp.internal.ios.interop.SWBStoreTransaction
import com.superwall.sdk.kmp.models.store.StorePayment
import com.superwall.sdk.kmp.models.store.StoreProduct
import com.superwall.sdk.kmp.models.store.StoreTransaction
import com.superwall.sdk.kmp.models.store.StoreTransactionState
import com.superwall.sdk.kmp.models.store.SubscriptionPeriod
import com.superwall.sdk.kmp.models.store.SubscriptionPeriodUnit
import platform.Foundation.NSNumber
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
        state = storeTransactionStateFromSWBRaw(stateRawValue()),
        payment =
            StorePayment(
                productIdentifier = paymentProductIdentifier(),
                quantity = paymentQuantity().toInt(),
                discountIdentifier = paymentDiscountIdentifier(),
            ),
        appAccountToken = appAccountToken(),
    )

/** `null` for a raw value newer than the bridge. */
internal fun storeTransactionStateFromSWBRaw(raw: Long): StoreTransactionState? =
    when (raw) {
        SWB_STORE_TRANSACTION_STATE_PURCHASING -> StoreTransactionState.PURCHASING
        SWB_STORE_TRANSACTION_STATE_PURCHASED -> StoreTransactionState.PURCHASED
        SWB_STORE_TRANSACTION_STATE_FAILED -> StoreTransactionState.FAILED
        SWB_STORE_TRANSACTION_STATE_RESTORED -> StoreTransactionState.RESTORED
        SWB_STORE_TRANSACTION_STATE_DEFERRED -> StoreTransactionState.DEFERRED
        else -> null
    }

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
        subscriptionPeriod = subscriptionPeriodFromSWB(subscriptionPeriodValue(), subscriptionPeriodUnitRawValue()),
        // fullIdentifier, productType, storeFrontCountryCode and
        // isCustomProduct are Android only: left at their defaults.
    )

/** `null` when either half is missing or the unit is newer than the bridge. */
private fun subscriptionPeriodFromSWB(
    value: NSNumber?,
    unitRaw: NSNumber?,
): SubscriptionPeriod? {
    if (value == null || unitRaw == null) return null
    val unit =
        when (unitRaw.longLongValue) {
            SWB_SUBSCRIPTION_PERIOD_UNIT_DAY -> SubscriptionPeriodUnit.DAY
            SWB_SUBSCRIPTION_PERIOD_UNIT_WEEK -> SubscriptionPeriodUnit.WEEK
            SWB_SUBSCRIPTION_PERIOD_UNIT_MONTH -> SubscriptionPeriodUnit.MONTH
            SWB_SUBSCRIPTION_PERIOD_UNIT_YEAR -> SubscriptionPeriodUnit.YEAR
            else -> return null
        }
    return SubscriptionPeriod(value = value.longLongValue.toInt(), unit = unit)
}
