@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.interop.NSAnySanitizer
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequest
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypeDaysSince
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypeHoursSince
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypeMinutesSince
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypeMonthsSince
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypePlacementsInDay
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypePlacementsInHour
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypePlacementsInMonth
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypePlacementsInWeek
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypePlacementsSinceInstall
import com.superwall.sdk.kmp.internal.ios.interop.SWBComputedPropertyRequestTypeYearsSince
import com.superwall.sdk.kmp.internal.ios.interop.SWBConfirmedAssignment
import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlement
import com.superwall.sdk.kmp.internal.ios.interop.SWBExperiment
import com.superwall.sdk.kmp.internal.ios.interop.SWBFeatureGatingBehaviorGated
import com.superwall.sdk.kmp.internal.ios.interop.SWBFeatureGatingBehaviorNonGated
import com.superwall.sdk.kmp.internal.ios.interop.SWBLocalNotification
import com.superwall.sdk.kmp.internal.ios.interop.SWBLocalNotificationTypeTrialStarted
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReasonForNextPaywall
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReasonManualClose
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReasonNone
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReasonSystemLogic
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReasonWebViewFailedToLoad
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBProduct
import com.superwall.sdk.kmp.internal.ios.interop.SWBSurvey
import com.superwall.sdk.kmp.internal.ios.interop.SWBSurveyOption
import com.superwall.sdk.kmp.internal.ios.interop.SWBSurveyShowConditionOnManualClose
import com.superwall.sdk.kmp.internal.ios.interop.SWBSurveyShowConditionOnPurchase
import com.superwall.sdk.kmp.internal.ios.interop.SWBVariant
import com.superwall.sdk.kmp.internal.ios.interop.SWBVariantTypeHoldout
import com.superwall.sdk.kmp.internal.ios.interop.SWBVariantTypeTreatment
import com.superwall.sdk.kmp.models.paywall.ComputedPropertyRequest
import com.superwall.sdk.kmp.models.paywall.ComputedPropertyRequestType
import com.superwall.sdk.kmp.models.paywall.FeatureGatingBehavior
import com.superwall.sdk.kmp.models.paywall.LocalNotification
import com.superwall.sdk.kmp.models.paywall.LocalNotificationType
import com.superwall.sdk.kmp.models.paywall.PaywallCloseReason
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.paywall.Product
import com.superwall.sdk.kmp.models.paywall.Survey
import com.superwall.sdk.kmp.models.paywall.SurveyOption
import com.superwall.sdk.kmp.models.paywall.SurveyShowCondition
import com.superwall.sdk.kmp.models.triggers.ConfirmedAssignment
import com.superwall.sdk.kmp.models.triggers.Experiment
import com.superwall.sdk.kmp.models.triggers.Variant
import com.superwall.sdk.kmp.models.triggers.VariantType

// ---------------------------------------------------------------------------
// Experiment / variant / assignment.
// ---------------------------------------------------------------------------

internal fun SWBVariant.toModel(): Variant =
    Variant(
        id = id,
        type = if (type == SWBVariantTypeHoldout) VariantType.HOLDOUT else VariantType.TREATMENT,
        paywallId = paywallId,
    )

internal fun SWBExperiment.toModel(): Experiment =
    Experiment(id = id, groupId = groupId, variant = variant.toModel())

/**
 * Defensive fallback for enum-envelope cases whose contract guarantees an
 * experiment payload (`paywall`/`holdout`): should the bridge ever deliver a
 * nil experiment there, the CASE is preserved with an empty experiment rather
 * than crashing or misreporting the case (degrade, never crash — plan §7).
 */
internal fun SWBExperiment?.toModelOrEmpty(holdout: Boolean = false): Experiment =
    this?.toModel()
        ?: Experiment(
            id = "",
            groupId = "",
            variant = Variant(id = "", type = if (holdout) VariantType.HOLDOUT else VariantType.TREATMENT),
        )

internal fun SWBConfirmedAssignment.toModel(): ConfirmedAssignment =
    ConfirmedAssignment(experimentId = experimentId, variant = variant.toModel())

// ---------------------------------------------------------------------------
// Paywall info family.
// ---------------------------------------------------------------------------

internal fun SWBProduct.toModel(): Product =
    Product(
        id = id,
        name = name,
        entitlements = entitlements.mapNotNull { (it as? SWBEntitlement)?.toModel() }.toSet(),
    )

internal fun SWBLocalNotification.toModel(): LocalNotification =
    LocalNotification(
        id = id,
        type = when (type) {
            SWBLocalNotificationTypeTrialStarted -> LocalNotificationType.TRIAL_STARTED
            else -> LocalNotificationType.UNSUPPORTED
        },
        title = title,
        body = body,
        delay = delay,
        subtitle = subtitle,
    )

internal fun SWBComputedPropertyRequest.toModel(): ComputedPropertyRequest =
    ComputedPropertyRequest(
        type = when (type) {
            SWBComputedPropertyRequestTypeMinutesSince -> ComputedPropertyRequestType.MINUTES_SINCE
            SWBComputedPropertyRequestTypeHoursSince -> ComputedPropertyRequestType.HOURS_SINCE
            SWBComputedPropertyRequestTypeDaysSince -> ComputedPropertyRequestType.DAYS_SINCE
            SWBComputedPropertyRequestTypeMonthsSince -> ComputedPropertyRequestType.MONTHS_SINCE
            SWBComputedPropertyRequestTypeYearsSince -> ComputedPropertyRequestType.YEARS_SINCE
            SWBComputedPropertyRequestTypePlacementsInHour -> ComputedPropertyRequestType.PLACEMENTS_IN_HOUR
            SWBComputedPropertyRequestTypePlacementsInDay -> ComputedPropertyRequestType.PLACEMENTS_IN_DAY
            SWBComputedPropertyRequestTypePlacementsInWeek -> ComputedPropertyRequestType.PLACEMENTS_IN_WEEK
            SWBComputedPropertyRequestTypePlacementsInMonth -> ComputedPropertyRequestType.PLACEMENTS_IN_MONTH
            SWBComputedPropertyRequestTypePlacementsSinceInstall ->
                ComputedPropertyRequestType.PLACEMENTS_SINCE_INSTALL
            // Unknown native case: documented fallback (plan §7).
            else -> ComputedPropertyRequestType.MINUTES_SINCE
        },
        eventName = placementName,
    )

internal fun SWBSurveyOption.toModel(): SurveyOption = SurveyOption(id = id, text = text)

internal fun SWBSurvey.toModel(): Survey =
    Survey(
        id = id,
        assignmentKey = assignmentKey,
        title = title,
        message = message,
        options = options.mapNotNull { (it as? SWBSurveyOption)?.toModel() },
        presentationCondition = when (presentationCondition) {
            SWBSurveyShowConditionOnPurchase -> SurveyShowCondition.ON_PURCHASE
            SWBSurveyShowConditionOnManualClose -> SurveyShowCondition.ON_MANUAL_CLOSE
            else -> SurveyShowCondition.ON_MANUAL_CLOSE
        },
        presentationProbability = presentationProbability,
        includeOtherOption = includeOtherOption,
        includeCloseOption = includeCloseOption,
    )

internal fun SWBPaywallInfo.toModel(): PaywallInfo =
    PaywallInfo(
        identifier = identifier,
        name = name,
        experiment = experiment?.toModel(),
        productIds = productIds.mapNotNull { it as? String },
        products = products.mapNotNull { (it as? SWBProduct)?.toModel() },
        url = url,
        presentedByPlacementWithName = presentedByPlacementWithName,
        presentedByPlacementWithId = presentedByPlacementWithId,
        presentedByPlacementAt = presentedByPlacementAt.toInstantOrNull(),
        presentedBy = presentedBy,
        presentationSourceType = presentationSourceType,
        responseLoadStartTime = responseLoadStartTime.toInstantOrNull(),
        responseLoadCompleteTime = responseLoadCompleteTime.toInstantOrNull(),
        responseLoadFailTime = responseLoadFailTime.toInstantOrNull(),
        responseLoadDuration = responseLoadDuration?.doubleValue,
        webViewLoadStartTime = webViewLoadStartTime.toInstantOrNull(),
        webViewLoadCompleteTime = webViewLoadCompleteTime.toInstantOrNull(),
        webViewLoadFailTime = webViewLoadFailTime.toInstantOrNull(),
        webViewLoadDuration = webViewLoadDuration?.doubleValue,
        productsLoadStartTime = productsLoadStartTime.toInstantOrNull(),
        productsLoadCompleteTime = productsLoadCompleteTime.toInstantOrNull(),
        productsLoadFailTime = productsLoadFailTime.toInstantOrNull(),
        productsLoadDuration = productsLoadDuration?.doubleValue,
        paywalljsVersion = paywalljsVersion,
        isFreeTrialAvailable = isFreeTrialAvailable,
        featureGatingBehavior = when (featureGatingBehavior) {
            SWBFeatureGatingBehaviorGated -> FeatureGatingBehavior.GATED
            SWBFeatureGatingBehaviorNonGated -> FeatureGatingBehavior.NON_GATED
            else -> FeatureGatingBehavior.NON_GATED
        },
        closeReason = when (closeReason) {
            SWBPaywallCloseReasonSystemLogic -> PaywallCloseReason.SYSTEM_LOGIC
            SWBPaywallCloseReasonForNextPaywall -> PaywallCloseReason.FOR_NEXT_PAYWALL
            SWBPaywallCloseReasonWebViewFailedToLoad -> PaywallCloseReason.WEB_VIEW_FAILED_TO_LOAD
            SWBPaywallCloseReasonManualClose -> PaywallCloseReason.MANUAL_CLOSE
            SWBPaywallCloseReasonNone -> PaywallCloseReason.NONE
            else -> PaywallCloseReason.NONE
        },
        localNotifications = localNotifications.mapNotNull { (it as? SWBLocalNotification)?.toModel() },
        computedPropertyRequests =
            computedPropertyRequests.mapNotNull { (it as? SWBComputedPropertyRequest)?.toModel() },
        surveys = surveys.mapNotNull { (it as? SWBSurvey)?.toModel() },
        state = NSAnySanitizer.fromMapOrNull(state),
    )
