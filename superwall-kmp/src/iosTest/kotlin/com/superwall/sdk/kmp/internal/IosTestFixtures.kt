@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlement
import com.superwall.sdk.kmp.internal.ios.interop.SWBFeatureGatingBehaviorNonGated
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReason
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReasonNone
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBStoreProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Stands in for the bridge's `Dispatchers.Main.immediate` scope: unconfined
 * runs a launch in place, as `Main.immediate` does for a call that arrives on
 * the main thread, and needs no run loop, which the test binary doesn't have.
 */
internal fun inPlaceScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

/** Records what an adapter reports through its `log` lambda. */
internal class RecordingLog {
    val entries = mutableListOf<Pair<String, Throwable?>>()

    val log: (String, Throwable?) -> Unit = { message, error -> entries += message to error }
}

/** A real bridge `SWBPaywallInfo` with only [identifier] and [closeReason] set. */
internal fun swbPaywallInfo(
    identifier: String,
    closeReason: SWBPaywallCloseReason = SWBPaywallCloseReasonNone,
): SWBPaywallInfo =
    SWBPaywallInfo(
        identifier = identifier,
        name = "",
        experiment = null,
        productIds = emptyList<String>(),
        products = emptyList<Any>(),
        url = "",
        presentedByPlacementWithName = null,
        presentedByPlacementWithId = null,
        presentedByPlacementAt = null,
        presentedBy = "",
        presentationSourceType = null,
        responseLoadStartTime = null,
        responseLoadCompleteTime = null,
        responseLoadFailTime = null,
        responseLoadDuration = null,
        webViewLoadStartTime = null,
        webViewLoadCompleteTime = null,
        webViewLoadFailTime = null,
        webViewLoadDuration = null,
        productsLoadStartTime = null,
        productsLoadCompleteTime = null,
        productsLoadFailTime = null,
        productsLoadDuration = null,
        paywalljsVersion = null,
        isFreeTrialAvailable = false,
        featureGatingBehavior = SWBFeatureGatingBehaviorNonGated,
        closeReason = closeReason,
        localNotifications = emptyList<Any>(),
        computedPropertyRequests = emptyList<Any>(),
        surveys = emptyList<Any>(),
        state = null,
    )

/** A real bridge `SWBStoreProduct` with only [productIdentifier] set. */
internal fun swbStoreProduct(productIdentifier: String): SWBStoreProduct =
    SWBStoreProduct(
        entitlements = emptyList<SWBEntitlement>(),
        productIdentifier = productIdentifier,
        subscriptionGroupIdentifier = null,
        attributes = emptyMap<Any?, Any>(),
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
        trialPeriodEndDate = null,
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
        languageCode = null,
        currencySymbol = null,
        currencyCode = null,
        isFamilyShareable = false,
        regionCode = null,
        price = 0.0,
    )
