@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.redemption.RedemptionPaywallProduct
import com.superwall.sdk.kmp.internal.interop.NSAnySanitizer
import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlement
import com.superwall.sdk.kmp.internal.ios.interop.SWBErrorInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBExpiredCodeInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBOwnership
import com.superwall.sdk.kmp.internal.ios.interop.SWBOwnershipCaseAppUser
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaserInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionPaywallInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionResultCaseError
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionResultCaseExpiredCode
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionResultCaseExpiredSubscription
import com.superwall.sdk.kmp.internal.ios.interop.SWBRedemptionResultCaseSuccess
import com.superwall.sdk.kmp.internal.ios.interop.SWBStoreIdentifiers
import com.superwall.sdk.kmp.internal.ios.interop.SWBStoreIdentifiersCasePaddle
import com.superwall.sdk.kmp.internal.ios.interop.SWBStoreIdentifiersCaseStripe
import com.superwall.sdk.kmp.models.redemption.ErrorInfo
import com.superwall.sdk.kmp.models.redemption.ExpiredCodeInfo
import com.superwall.sdk.kmp.models.redemption.Ownership
import com.superwall.sdk.kmp.models.redemption.PurchaserInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionPaywallInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionResult
import com.superwall.sdk.kmp.models.redemption.StoreIdentifiers

internal fun SWBOwnership.toModel(): Ownership =
    when (ownership()) {
        SWBOwnershipCaseAppUser -> Ownership.AppUser(appUserId() ?: "")
        else -> Ownership.Device(deviceId() ?: "")
    }

internal fun SWBStoreIdentifiers.toModel(): StoreIdentifiers =
    when (store()) {
        SWBStoreIdentifiersCaseStripe ->
            StoreIdentifiers.Stripe(
                customerId = customerId() ?: "",
                subscriptionIds = subscriptionIds().mapNotNull { it as? String },
            )
        SWBStoreIdentifiersCasePaddle ->
            StoreIdentifiers.Paddle(
                customerId = customerId() ?: "",
                subscriptionIds = subscriptionIds().mapNotNull { it as? String },
            )
        else ->
            StoreIdentifiers.Unknown(
                store = unknownStore() ?: "unknown",
                additionalInfo = NSAnySanitizer.fromMapOrNull(additionalInfo()) ?: emptyMap(),
            )
    }

internal fun SWBPurchaserInfo.toModel(): PurchaserInfo =
    PurchaserInfo(
        appUserId = appUserId(),
        email = email(),
        storeIdentifiers = storeIdentifiers().toModel(),
    )

internal fun SWBRedemptionPaywallInfo.toModel(): RedemptionPaywallInfo =
    RedemptionPaywallInfo(
        identifier = identifier(),
        placementName = placementName(),
        placementParams = NSAnySanitizer.fromMap(placementParams()),
        variantId = variantId(),
        experimentId = experimentId(),
        productIdentifier = productIdentifier(),
        product = product()?.let { redemptionPaywallProductFromMap(NSAnySanitizer.fromMap(it)) },
    )

/**
 * Reads the product dictionary the bridge builds from SuperwallKit's
 * `PaywallProduct`, keyed by the common field names. `null` without an
 * identifier, the one field a product can't do without; any other missing
 * value takes the model's default.
 */
internal fun redemptionPaywallProductFromMap(map: Map<String, Any?>): RedemptionPaywallProduct? {
    val identifier = map["identifier"] as? String ?: return null

    fun string(key: String): String = map[key] as? String ?: ""

    fun int(key: String): Int = (map[key] as? Number)?.toInt() ?: 0

    fun double(key: String): Double = (map[key] as? Number)?.toDouble() ?: 0.0

    return RedemptionPaywallProduct(
        identifier = identifier,
        languageCode = string("languageCode"),
        locale = string("locale"),
        currencyCode = string("currencyCode"),
        currencySymbol = string("currencySymbol"),
        period = string("period"),
        periodly = string("periodly"),
        localizedPeriod = string("localizedPeriod"),
        periodAlt = string("periodAlt"),
        periodDays = int("periodDays"),
        periodWeeks = int("periodWeeks"),
        periodMonths = int("periodMonths"),
        periodYears = int("periodYears"),
        rawPrice = double("rawPrice"),
        price = string("price"),
        dailyPrice = string("dailyPrice"),
        weeklyPrice = string("weeklyPrice"),
        monthlyPrice = string("monthlyPrice"),
        yearlyPrice = string("yearlyPrice"),
        rawTrialPeriodPrice = double("rawTrialPeriodPrice"),
        trialPeriodPrice = string("trialPeriodPrice"),
        trialPeriodDailyPrice = string("trialPeriodDailyPrice"),
        trialPeriodWeeklyPrice = string("trialPeriodWeeklyPrice"),
        trialPeriodMonthlyPrice = string("trialPeriodMonthlyPrice"),
        trialPeriodYearlyPrice = string("trialPeriodYearlyPrice"),
        trialPeriodDays = int("trialPeriodDays"),
        trialPeriodWeeks = int("trialPeriodWeeks"),
        trialPeriodMonths = int("trialPeriodMonths"),
        trialPeriodYears = int("trialPeriodYears"),
        trialPeriodText = string("trialPeriodText"),
        trialPeriodEndDate = string("trialPeriodEndDate"),
    )
}

internal fun SWBRedemptionInfo.toModel(): RedemptionInfo =
    RedemptionInfo(
        ownership = ownership().toModel(),
        purchaserInfo = purchaserInfo().toModel(),
        paywallInfo = paywallInfo()?.toModel(),
        entitlements = entitlements().mapNotNull { (it as? SWBEntitlement)?.toModel() }.toSet(),
        userAttributes = NSAnySanitizer.fromMapOrNull(userAttributes()),
    )

internal fun SWBErrorInfo.toModel(): ErrorInfo = ErrorInfo(message = message())

internal fun SWBExpiredCodeInfo.toModel(): ExpiredCodeInfo =
    ExpiredCodeInfo(resent = resent(), obfuscatedEmail = obfuscatedEmail())

/**
 * Maps the SWB redemption envelope back into the sealed common result. A
 * case whose contract-guaranteed payload is missing degrades to
 * [RedemptionResult.InvalidCode]-adjacent shapes rather than crashing
 *.
 */
internal fun SWBRedemptionResult.toModel(): RedemptionResult =
    when (result()) {
        SWBRedemptionResultCaseSuccess ->
            redemptionInfo()?.let { RedemptionResult.Success(code = code(), redemptionInfo = it.toModel()) }
                ?: RedemptionResult.Error(code(), ErrorInfo("Missing redemption info payload"))
        SWBRedemptionResultCaseError ->
            RedemptionResult.Error(
                code = code(),
                error = errorInfo()?.toModel() ?: ErrorInfo("Unknown redemption error"),
            )
        SWBRedemptionResultCaseExpiredCode ->
            RedemptionResult.ExpiredCode(
                code = code(),
                info = expiredCodeInfo()?.toModel() ?: ExpiredCodeInfo(resent = false),
            )
        SWBRedemptionResultCaseExpiredSubscription ->
            redemptionInfo()?.let { RedemptionResult.ExpiredSubscription(code = code(), redemptionInfo = it.toModel()) }
                ?: RedemptionResult.Error(code(), ErrorInfo("Missing redemption info payload"))
        else -> RedemptionResult.InvalidCode(code = code())
    }
