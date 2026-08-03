@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.interop.NSAnySanitizer
import com.superwall.sdk.kmp.internal.ios.interop.SWBAttributionMatchInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventEnvelope
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventType
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeAdServicesTokenRequestComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeAdServicesTokenRequestFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeAdServicesTokenRequestStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeAppClose
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeAppInstall
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeAppLaunch
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeAppOpen
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeAttributionMatch
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeConfigAttributes
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeConfigFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeConfigRefresh
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeConfirmAllAssignments
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeCustomPlacement
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeCustomerInfoDidChange
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeDeepLink
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeDeviceAttributes
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeEnrichmentComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeEnrichmentFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeEnrichmentStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeFirstSeen
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeFreeTrialStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeIdentityAlias
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeIntegrationAttributes
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeNetworkDecodingFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeNonRecurringProductPurchase
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallClose
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallDecline
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallOpen
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallPageView
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallPreloadComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallPreloadStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallPresentationRequest
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallProductsLoadComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallProductsLoadFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallProductsLoadMissingProducts
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallProductsLoadRetry
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallProductsLoadStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallResponseLoadComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallResponseLoadFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallResponseLoadNotFound
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallResponseLoadStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallWebviewLoadComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallWebviewLoadFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallWebviewLoadFallback
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallWebviewLoadStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallWebviewLoadTimeout
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePaywallWebviewProcessTerminated
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePermissionDenied
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePermissionGranted
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypePermissionRequested
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeRedemptionComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeRedemptionFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeRedemptionStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeReset
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeRestoreComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeRestoreFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeRestoreStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeReviewRequested
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeSessionStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeShimmerViewComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeShimmerViewStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeStripeCheckoutComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeStripeCheckoutFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeStripeCheckoutStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeStripeCheckoutSubmit
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeSubscriptionStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeSubscriptionStatusDidChange
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeSurveyClose
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeSurveyResponse
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTestModeModalClose
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTestModeModalOpen
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTouchesBegan
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTransactionAbandon
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTransactionComplete
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTransactionFail
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTransactionRestore
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTransactionStart
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTransactionTimeout
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeTriggerFire
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTypeUserAttributes
import com.superwall.sdk.kmp.internal.interop.SWB_TRANSACTION_TYPE_FREE_TRIAL_START
import com.superwall.sdk.kmp.internal.interop.SWB_TRANSACTION_TYPE_NON_RECURRING_PRODUCT_PURCHASE
import com.superwall.sdk.kmp.internal.interop.SWB_TRANSACTION_TYPE_SUBSCRIPTION_START
import com.superwall.sdk.kmp.internal.ios.interop.SWBPageViewData
import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import platform.Foundation.NSNumber

/**
 * Maps the typed `SWBEventEnvelope` — the Swift bridge's destructuring of all
 * 80 SuperwallKit 4.16.1 event cases — into the common [SuperwallEventInfo].
 *
 * The envelope is flat and sparse, so the payload copy is uniform; only the
 * event-type discriminator needs a mapping. Two fidelity notes:
 *
 * - Envelope fields with no [SuperwallEventInfo] counterpart (transactionType,
 *   transactionProductId, permissionName, paywallIdentifier, paywallCount,
 *   attributionMatch, pageViewData) are preserved inside [SuperwallEventInfo.params].
 * - SWB event types with no common [EventType] counterpart (attributionMatch,
 *   stripeCheckout*, testModeModal*, paywallPageView, unknown) degrade to
 *   [EventType.CUSTOM_PLACEMENT] — the common surface's generic named-event
 *   envelope — with the raw case name in [SuperwallEventInfo.name] and under
 *   `params["rawEventType"]` (degrade, never crash — plan §7).
 */
internal fun SWBEventEnvelope.toModel(): SuperwallEventInfo {
    val mappedType = eventTypeFromSWB(eventType())
    val rawGapName = if (mappedType == null) rawEventNameForSWB(eventType(), name()) else null

    val mergedParams = buildMap {
        NSAnySanitizer.fromMapOrNull(params())?.let(::putAll)
        transactionType()?.let { put("transactionType", transactionTypeNameFromNSNumber(it)) }
        transactionProductId()?.let { put("transactionProductId", it) }
        permissionName()?.let { put("permissionName", it) }
        paywallIdentifier()?.let { put("paywallIdentifier", it) }
        paywallCount()?.let { put("paywallCount", it.longLongValue) }
        attributionMatch()?.let { put("attributionMatch", it.toParamsMap()) }
        pageViewData()?.let { put("pageViewData", it.toParamsMap()) }
        rawGapName?.let { put("rawEventType", it) }
    }.takeIf { it.isNotEmpty() }

    return SuperwallEventInfo(
        eventType = mappedType ?: EventType.CUSTOM_PLACEMENT,
        params = mergedParams,
        placementName = placementName(),
        deviceAttributes = NSAnySanitizer.fromMapOrNull(deviceAttributes()),
        deepLinkUrl = deepLinkUrl(),
        result = triggerResult()?.toModel(),
        paywallInfo = paywallInfo()?.toModel(),
        transaction = transaction()?.toModel(),
        product = product()?.toModel(),
        error = error(),
        triggeredPlacementName = triggeredPlacementName(),
        attempt = attempt()?.longLongValue,
        name = name() ?: rawGapName,
        survey = survey()?.toModel(),
        selectedOption = selectedOption()?.toModel(),
        customResponse = customResponse(),
        status = presentationRequestStatusFromNSNumber(presentationRequestStatus()),
        reason = presentationRequestReason()?.toModel(),
        restoreType = restoreType()?.toModel(),
        userAttributes = NSAnySanitizer.fromMapOrNull(userAttributes()),
        token = token(),
        userEnrichment = NSAnySanitizer.fromMapOrNull(userEnrichment()),
        deviceEnrichment = NSAnySanitizer.fromMapOrNull(deviceEnrichment()),
        message = message(),
        integrationAttributes = NSAnySanitizer.fromMapOrNull(integrationAttributes()),
        reviewRequestedCount = reviewRequestedCount()?.longLongValue,
        missingProductIdentifiers = missingProductIdentifiers()?.mapNotNull { it as? String },
    )
}

/**
 * SWBEventType -> common [EventType]; `null` marks a bridge event type with
 * no common counterpart (handled by the CUSTOM_PLACEMENT fallback above).
 */
internal fun eventTypeFromSWB(value: SWBEventType): EventType? =
    when (value) {
        SWBEventTypeFirstSeen -> EventType.FIRST_SEEN
        SWBEventTypeAppOpen -> EventType.APP_OPEN
        SWBEventTypeAppLaunch -> EventType.APP_LAUNCH
        SWBEventTypeIdentityAlias -> EventType.IDENTITY_ALIAS
        SWBEventTypeAppInstall -> EventType.APP_INSTALL
        SWBEventTypeSessionStart -> EventType.SESSION_START
        SWBEventTypeDeviceAttributes -> EventType.DEVICE_ATTRIBUTES
        SWBEventTypeSubscriptionStatusDidChange -> EventType.SUBSCRIPTION_STATUS_DID_CHANGE
        SWBEventTypeAppClose -> EventType.APP_CLOSE
        SWBEventTypeDeepLink -> EventType.DEEP_LINK
        SWBEventTypeTriggerFire -> EventType.TRIGGER_FIRE
        SWBEventTypePaywallOpen -> EventType.PAYWALL_OPEN
        SWBEventTypePaywallClose -> EventType.PAYWALL_CLOSE
        SWBEventTypePaywallDecline -> EventType.PAYWALL_DECLINE
        SWBEventTypeTransactionStart -> EventType.TRANSACTION_START
        SWBEventTypeTransactionFail -> EventType.TRANSACTION_FAIL
        SWBEventTypeTransactionAbandon -> EventType.TRANSACTION_ABANDON
        SWBEventTypeTransactionComplete -> EventType.TRANSACTION_COMPLETE
        SWBEventTypeSubscriptionStart -> EventType.SUBSCRIPTION_START
        SWBEventTypeFreeTrialStart -> EventType.FREE_TRIAL_START
        SWBEventTypeTransactionRestore -> EventType.TRANSACTION_RESTORE
        SWBEventTypeTransactionTimeout -> EventType.TRANSACTION_TIMEOUT
        SWBEventTypeUserAttributes -> EventType.USER_ATTRIBUTES
        SWBEventTypeNonRecurringProductPurchase -> EventType.NON_RECURRING_PRODUCT_PURCHASE
        SWBEventTypePaywallResponseLoadStart -> EventType.PAYWALL_RESPONSE_LOAD_START
        SWBEventTypePaywallResponseLoadNotFound -> EventType.PAYWALL_RESPONSE_LOAD_NOT_FOUND
        SWBEventTypePaywallResponseLoadFail -> EventType.PAYWALL_RESPONSE_LOAD_FAIL
        SWBEventTypePaywallResponseLoadComplete -> EventType.PAYWALL_RESPONSE_LOAD_COMPLETE
        SWBEventTypePaywallWebviewLoadStart -> EventType.PAYWALL_WEBVIEW_LOAD_START
        SWBEventTypePaywallWebviewLoadFail -> EventType.PAYWALL_WEBVIEW_LOAD_FAIL
        SWBEventTypePaywallWebviewLoadComplete -> EventType.PAYWALL_WEBVIEW_LOAD_COMPLETE
        SWBEventTypePaywallWebviewLoadTimeout -> EventType.PAYWALL_WEBVIEW_LOAD_TIMEOUT
        SWBEventTypePaywallWebviewLoadFallback -> EventType.PAYWALL_WEBVIEW_LOAD_FALLBACK
        SWBEventTypePaywallWebviewProcessTerminated -> EventType.PAYWALL_WEBVIEW_PROCESS_TERMINATED
        SWBEventTypePaywallProductsLoadStart -> EventType.PAYWALL_PRODUCTS_LOAD_START
        SWBEventTypePaywallProductsLoadFail -> EventType.PAYWALL_PRODUCTS_LOAD_FAIL
        SWBEventTypePaywallProductsLoadComplete -> EventType.PAYWALL_PRODUCTS_LOAD_COMPLETE
        SWBEventTypePaywallProductsLoadRetry -> EventType.PAYWALL_PRODUCTS_LOAD_RETRY
        SWBEventTypePaywallProductsLoadMissingProducts -> EventType.PAYWALL_PRODUCTS_LOAD_MISSING_PRODUCTS
        SWBEventTypeSurveyResponse -> EventType.SURVEY_RESPONSE
        SWBEventTypePaywallPresentationRequest -> EventType.PAYWALL_PRESENTATION_REQUEST
        SWBEventTypeTouchesBegan -> EventType.TOUCHES_BEGAN
        SWBEventTypeSurveyClose -> EventType.SURVEY_CLOSE
        SWBEventTypeReset -> EventType.RESET
        SWBEventTypeRestoreStart -> EventType.RESTORE_START
        SWBEventTypeRestoreFail -> EventType.RESTORE_FAIL
        SWBEventTypeRestoreComplete -> EventType.RESTORE_COMPLETE
        SWBEventTypeConfigRefresh -> EventType.CONFIG_REFRESH
        SWBEventTypeCustomPlacement -> EventType.CUSTOM_PLACEMENT
        SWBEventTypeConfigAttributes -> EventType.CONFIG_ATTRIBUTES
        SWBEventTypeConfirmAllAssignments -> EventType.CONFIRM_ALL_ASSIGNMENTS
        SWBEventTypeConfigFail -> EventType.CONFIG_FAIL
        SWBEventTypeAdServicesTokenRequestStart -> EventType.AD_SERVICES_TOKEN_REQUEST_START
        SWBEventTypeAdServicesTokenRequestFail -> EventType.AD_SERVICES_TOKEN_REQUEST_FAIL
        SWBEventTypeAdServicesTokenRequestComplete -> EventType.AD_SERVICES_TOKEN_REQUEST_COMPLETE
        SWBEventTypeShimmerViewStart -> EventType.SHIMMER_VIEW_START
        SWBEventTypeShimmerViewComplete -> EventType.SHIMMER_VIEW_COMPLETE
        SWBEventTypeRedemptionStart -> EventType.REDEMPTION_START
        SWBEventTypeRedemptionComplete -> EventType.REDEMPTION_COMPLETE
        SWBEventTypeRedemptionFail -> EventType.REDEMPTION_FAIL
        SWBEventTypeEnrichmentStart -> EventType.ENRICHMENT_START
        SWBEventTypeEnrichmentComplete -> EventType.ENRICHMENT_COMPLETE
        SWBEventTypeEnrichmentFail -> EventType.ENRICHMENT_FAIL
        SWBEventTypeNetworkDecodingFail -> EventType.NETWORK_DECODING_FAIL
        SWBEventTypeCustomerInfoDidChange -> EventType.CUSTOMER_INFO_DID_CHANGE
        SWBEventTypeIntegrationAttributes -> EventType.INTEGRATION_ATTRIBUTES
        SWBEventTypeReviewRequested -> EventType.REVIEW_REQUESTED
        SWBEventTypePermissionRequested -> EventType.PERMISSION_REQUESTED
        SWBEventTypePermissionGranted -> EventType.PERMISSION_GRANTED
        SWBEventTypePermissionDenied -> EventType.PERMISSION_DENIED
        SWBEventTypePaywallPreloadStart -> EventType.PAYWALL_PRELOAD_START
        SWBEventTypePaywallPreloadComplete -> EventType.PAYWALL_PRELOAD_COMPLETE
        // No common EventType counterpart (4.16.x-only cases + forward-compat
        // unknown): fall through to the CUSTOM_PLACEMENT gap handling.
        else -> null
    }

/** Raw case name preserved for gap event types (and `.unknown`). */
private fun rawEventNameForSWB(value: SWBEventType, envelopeName: String?): String =
    when (value) {
        SWBEventTypeAttributionMatch -> "attributionMatch"
        SWBEventTypeStripeCheckoutStart -> "stripeCheckoutStart"
        SWBEventTypeStripeCheckoutSubmit -> "stripeCheckoutSubmit"
        SWBEventTypeStripeCheckoutComplete -> "stripeCheckoutComplete"
        SWBEventTypeStripeCheckoutFail -> "stripeCheckoutFail"
        SWBEventTypeTestModeModalOpen -> "testModeModalOpen"
        SWBEventTypeTestModeModalClose -> "testModeModalClose"
        SWBEventTypePaywallPageView -> "paywallPageView"
        else -> envelopeName ?: "unknown"
    }

private fun transactionTypeNameFromNSNumber(value: NSNumber): String =
    when (value.longLongValue) {
        SWB_TRANSACTION_TYPE_NON_RECURRING_PRODUCT_PURCHASE -> "NON_RECURRING_PRODUCT_PURCHASE"
        SWB_TRANSACTION_TYPE_FREE_TRIAL_START -> "FREE_TRIAL_START"
        SWB_TRANSACTION_TYPE_SUBSCRIPTION_START -> "SUBSCRIPTION_START"
        else -> "UNKNOWN"
    }

private fun SWBAttributionMatchInfo.toParamsMap(): Map<String, Any?> =
    buildMap {
        put("provider", provider())
        put("matched", matched())
        source()?.let { put("source", it) }
        confidence()?.let { put("confidence", it) }
        matchScore()?.let { put("matchScore", it.doubleValue) }
        reason()?.let { put("reason", it) }
    }

private fun SWBPageViewData.toParamsMap(): Map<String, Any?> =
    buildMap {
        put("pageNodeId", pageNodeId())
        put("flowPosition", flowPosition())
        put("pageName", pageName())
        put("navigationNodeId", navigationNodeId())
        previousPageNodeId()?.let { put("previousPageNodeId", it) }
        previousFlowPosition()?.let { put("previousFlowPosition", it.longLongValue) }
        put("navigationType", navigationType())
        timeOnPreviousPageMs()?.let { put("timeOnPreviousPageMs", it.longLongValue) }
    }
