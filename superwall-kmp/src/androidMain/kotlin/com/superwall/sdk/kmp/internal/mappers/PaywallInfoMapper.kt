package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.paywall.ComputedPropertyRequest
import com.superwall.sdk.kmp.models.paywall.ComputedPropertyRequestType
import com.superwall.sdk.kmp.models.paywall.FeatureGatingBehavior
import com.superwall.sdk.kmp.models.paywall.LocalNotification
import com.superwall.sdk.kmp.models.paywall.LocalNotificationType
import com.superwall.sdk.kmp.models.paywall.PaywallCloseReason
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.paywall.PaywallProduct
import com.superwall.sdk.kmp.models.paywall.Survey
import com.superwall.sdk.kmp.models.paywall.SurveyOption
import com.superwall.sdk.kmp.models.paywall.SurveyShowCondition
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Instant
import com.superwall.sdk.config.models.Survey as NativeSurvey
import com.superwall.sdk.config.models.SurveyOption as NativeSurveyOption
import com.superwall.sdk.config.models.SurveyShowCondition as NativeSurveyShowCondition
import com.superwall.sdk.models.config.ComputedPropertyRequest as NativeComputedPropertyRequest
import com.superwall.sdk.models.config.FeatureGatingBehavior as NativeFeatureGatingBehavior
import com.superwall.sdk.models.paywall.LocalNotification as NativeLocalNotification
import com.superwall.sdk.models.paywall.LocalNotificationType as NativeLocalNotificationType
import com.superwall.sdk.models.product.ProductItem as NativeProductItem
import com.superwall.sdk.paywall.presentation.PaywallCloseReason as NativePaywallCloseReason
import com.superwall.sdk.paywall.presentation.PaywallInfo as NativePaywallInfo

/**
 * Maps the native [NativePaywallInfo] to the common [PaywallInfo]. Port of the
 * Flutter host's `PaywallInfoMapper.toPPaywallInfo` (utils/OptionsMapper.kt)
 * with the date convention changed to [kotlin.time.Instant] (plan §3.4):
 * superwall-android 2.8.0 renders its load-time fields as **strings**
 * (`DateFormatterUtil`, pattern `yyyy-MM-dd'T'HH:mm:ss.SSS` in the device's
 * default zone, empty string when absent), so [parseNativePaywallDate] parses
 * them leniently and degrades to `null` rather than crashing.
 *
 * `LocalNotification.id` is the real native `String` id (the Flutter layer
 * hardcoded `""`/`0` — plan §3.4 fix).
 */
internal fun NativePaywallInfo.toKmp(): PaywallInfo =
    PaywallInfo(
        identifier = identifier,
        name = name,
        experiment = experiment?.toKmp(),
        productIds = productIds,
        products = products.map { it.toKmp() },
        url = url.toString(),
        presentedByPlacementWithName = presentedByEventWithName,
        presentedByPlacementWithId = presentedByEventWithId,
        presentedByPlacementAt = parseNativePaywallDate(presentedByEventAt),
        presentedBy = presentedBy,
        presentationSourceType = presentationSourceType,
        responseLoadStartTime = parseNativePaywallDate(responseLoadStartTime),
        responseLoadCompleteTime = parseNativePaywallDate(responseLoadCompleteTime),
        responseLoadFailTime = parseNativePaywallDate(responseLoadFailTime),
        responseLoadDuration = responseLoadDuration,
        webViewLoadStartTime = parseNativePaywallDate(webViewLoadStartTime),
        webViewLoadCompleteTime = parseNativePaywallDate(webViewLoadCompleteTime),
        webViewLoadFailTime = parseNativePaywallDate(webViewLoadFailTime),
        webViewLoadDuration = webViewLoadDuration,
        productsLoadStartTime = parseNativePaywallDate(productsLoadStartTime),
        productsLoadCompleteTime = parseNativePaywallDate(productsLoadCompleteTime),
        productsLoadFailTime = parseNativePaywallDate(productsLoadFailTime),
        productsLoadDuration = productsLoadDuration,
        paywalljsVersion = paywalljsVersion,
        isFreeTrialAvailable = isFreeTrialAvailable,
        featureGatingBehavior = featureGatingBehavior.toKmp(),
        closeReason = closeReason.toKmp(),
        localNotifications = localNotifications.map { it.toKmp() },
        computedPropertyRequests = computedPropertyRequests.map { it.toKmp() },
        surveys = surveys.map { it.toKmp() },
        state = sanitizeParams(state)?.ifEmpty { null },
    )

// ---- PaywallProduct -------------------------------------------------------------------

internal fun NativeProductItem.toKmp(): PaywallProduct =
    PaywallProduct(
        id = fullProductId,
        name = name,
        entitlements = entitlements.map { it.toKmp() }.toSet(),
    )

// ---- Enums ----------------------------------------------------------------------

internal fun NativeFeatureGatingBehavior.toKmp(): FeatureGatingBehavior =
    when (this) {
        is NativeFeatureGatingBehavior.Gated -> FeatureGatingBehavior.GATED
        is NativeFeatureGatingBehavior.NonGated -> FeatureGatingBehavior.NON_GATED
    }

internal fun NativePaywallCloseReason.toKmp(): PaywallCloseReason =
    when (this) {
        is NativePaywallCloseReason.SystemLogic -> PaywallCloseReason.SYSTEM_LOGIC
        is NativePaywallCloseReason.ForNextPaywall -> PaywallCloseReason.FOR_NEXT_PAYWALL
        is NativePaywallCloseReason.WebViewFailedToLoad -> PaywallCloseReason.WEB_VIEW_FAILED_TO_LOAD
        is NativePaywallCloseReason.ManualClose -> PaywallCloseReason.MANUAL_CLOSE
        is NativePaywallCloseReason.None -> PaywallCloseReason.NONE
    }

// ---- LocalNotification -------------------------------------------------------------

internal fun NativeLocalNotification.toKmp(): LocalNotification =
    LocalNotification(
        id = id,
        type =
            when (type) {
                is NativeLocalNotificationType.TrialStarted -> LocalNotificationType.TRIAL_STARTED
                is NativeLocalNotificationType.Unsupported -> LocalNotificationType.UNSUPPORTED
            },
        title = title,
        body = body,
        delay = delay,
        subtitle = subtitle,
    )

// ---- ComputedPropertyRequest ----------------------------------------------------------

internal fun NativeComputedPropertyRequest.toKmp(): ComputedPropertyRequest =
    ComputedPropertyRequest(
        // All 10 native cases mapped (the Flutter host silently collapsed the
        // PLACEMENTS_* cases to DAYS_SINCE in one direction).
        type =
            when (type) {
                NativeComputedPropertyRequest.ComputedPropertyRequestType.MINUTES_SINCE ->
                    ComputedPropertyRequestType.MINUTES_SINCE
                NativeComputedPropertyRequest.ComputedPropertyRequestType.HOURS_SINCE ->
                    ComputedPropertyRequestType.HOURS_SINCE
                NativeComputedPropertyRequest.ComputedPropertyRequestType.DAYS_SINCE ->
                    ComputedPropertyRequestType.DAYS_SINCE
                NativeComputedPropertyRequest.ComputedPropertyRequestType.MONTHS_SINCE ->
                    ComputedPropertyRequestType.MONTHS_SINCE
                NativeComputedPropertyRequest.ComputedPropertyRequestType.YEARS_SINCE ->
                    ComputedPropertyRequestType.YEARS_SINCE
                NativeComputedPropertyRequest.ComputedPropertyRequestType.PLACEMENTS_IN_HOUR ->
                    ComputedPropertyRequestType.PLACEMENTS_IN_HOUR
                NativeComputedPropertyRequest.ComputedPropertyRequestType.PLACEMENTS_IN_DAY ->
                    ComputedPropertyRequestType.PLACEMENTS_IN_DAY
                NativeComputedPropertyRequest.ComputedPropertyRequestType.PLACEMENTS_IN_WEEK ->
                    ComputedPropertyRequestType.PLACEMENTS_IN_WEEK
                NativeComputedPropertyRequest.ComputedPropertyRequestType.PLACEMENTS_IN_MONTH ->
                    ComputedPropertyRequestType.PLACEMENTS_IN_MONTH
                NativeComputedPropertyRequest.ComputedPropertyRequestType.PLACEMENTS_SINCE_INSTALL ->
                    ComputedPropertyRequestType.PLACEMENTS_SINCE_INSTALL
            },
        eventName = eventName,
    )

// ---- Survey ------------------------------------------------------------------------------

internal fun NativeSurvey.toKmp(): Survey =
    Survey(
        id = id,
        assignmentKey = assignmentKey,
        title = title,
        message = message,
        options = options.map { it.toKmp() },
        presentationCondition =
            when (presentationCondition) {
                NativeSurveyShowCondition.ON_MANUAL_CLOSE -> SurveyShowCondition.ON_MANUAL_CLOSE
                NativeSurveyShowCondition.ON_PURCHASE -> SurveyShowCondition.ON_PURCHASE
            },
        presentationProbability = presentationProbability,
        includeOtherOption = includeOtherOption,
        includeCloseOption = includeCloseOption,
    )

internal fun NativeSurveyOption.toKmp(): SurveyOption =
    SurveyOption(
        id = id,
        text = title,
    )

// ---- Date parsing -----------------------------------------------------------------------------

private val paywallDatePatterns =
    listOf(
        // DateFormatterUtil (utilities/DateUtils.kt): ISO millis, no zone suffix.
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        // presentedByEventAt is `Date.toString()` (PaywallInfo.kt secondary ctor).
        "EEE MMM dd HH:mm:ss zzz yyyy",
    )

/**
 * Leniently parses the native PaywallInfo date strings to [Instant]:
 * ISO-8601 with an offset first (`Instant.parse`), then the native
 * `DateFormatterUtil` patterns (device default zone, matching how they were
 * formatted), then `Date.toString()`. Blank or unparseable input degrades to
 * `null` — never crash (plan §7).
 */
internal fun parseNativePaywallDate(raw: String?): Instant? {
    val value = raw?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    try {
        return Instant.parse(value)
    } catch (_: Throwable) {
        // Not ISO-8601-with-offset; fall through to the native patterns.
    }
    for (pattern in paywallDatePatterns) {
        try {
            val parsed = SimpleDateFormat(pattern, Locale.US).parse(value)
            if (parsed != null) return parsed.toKmpInstant()
        } catch (_: Throwable) {
            // Try the next pattern.
        }
    }
    return null
}
