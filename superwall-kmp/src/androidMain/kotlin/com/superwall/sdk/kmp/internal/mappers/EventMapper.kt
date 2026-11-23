package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.events.AttributionMatchInfo
import com.superwall.sdk.kmp.models.events.CustomerCenterEventInfo
import com.superwall.sdk.kmp.models.events.CustomerCenterScreenType
import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.events.PageViewData
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import com.superwall.sdk.kmp.models.events.WebviewError
import com.superwall.sdk.analytics.superwall.AttributionMatchInfo as NativeAttributionMatchInfo
import com.superwall.sdk.analytics.superwall.SuperwallEvent as NativeSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo as NativeSuperwallEventInfo
import com.superwall.sdk.customercenter.CustomerCenterScreenType as NativeCustomerCenterScreenType
import com.superwall.sdk.paywall.view.webview.WebviewError as NativeWebviewError
import com.superwall.sdk.paywall.view.webview.messaging.PageViewData as NativePageViewData
import com.superwall.sdk.store.transactions.TransactionError as NativeTransactionError

/**
 * Maps the native [NativeSuperwallEventInfo] (a `SuperwallEvent` sealed-class
 * payload plus a params map) to the common flat [SuperwallEventInfo] envelope.
 *
 * Port of the Flutter host's `EventMapper.toPigeonEventInfo`
 * (utils/EventMapper.kt) covering EVERY public case of superwall-android
 * 2.10.2's `SuperwallEvent`, with these deliberate deltas:
 *
 * - `PaywallWebviewLoadFail` no longer stringifies a null `WebviewError` into
 *   `"null"` — it maps to a null error.
 * - `TriggerFire` keeps the event params (the Flutter host dropped them).
 * - `TransactionFail` also carries the failing product when the native error
 *   is a [NativeTransactionError.Failure] (the Flutter host dropped it).
 * - `IntegrationAttributes`, `EnrichmentComplete`, `CustomerInfoDidChange`,
 *   `ReviewRequested` and the permission events map to their real event types
 * with their payloads (the six silently-dropped fields of).
 *
 * Payload fields that had no [SuperwallEventInfo] counterpart before 0.2.0
 * also still travel in [SuperwallEventInfo.params] under the same keys the
 * iOS mapper uses
 * (`permissionName`, `paywallIdentifier`, `paywallCount`, `pageViewData`,
 * `attributionMatch`, `transactionProductId`), plus the Android-only `url`
 * (resource load failures) and `duration` (shimmer view completion).
 *
 * Only the SDK-internal events (`ErrorThrown`, `ExpressionResult`) and any
 * case newer than this SDK version reach the fallback:
 * [EventType.CUSTOM_PLACEMENT] with `name = rawName` and
 * `params["rawEventType"] = rawName` — degrade, never crash.
 */
@Suppress("DEPRECATION") // IntegrationProps + PaywallWebviewLoadTimeout are deprecated-but-live native cases.
internal fun NativeSuperwallEventInfo.toKmp(): SuperwallEventInfo {
    val params = sanitizeParams(this.params)
    return when (val event = this.event) {
        is NativeSuperwallEvent.FirstSeen ->
            SuperwallEventInfo(eventType = EventType.FIRST_SEEN, params = params)
        is NativeSuperwallEvent.AppOpen ->
            SuperwallEventInfo(eventType = EventType.APP_OPEN, params = params)
        is NativeSuperwallEvent.AppLaunch ->
            SuperwallEventInfo(eventType = EventType.APP_LAUNCH, params = params)
        is NativeSuperwallEvent.IdentityAlias ->
            SuperwallEventInfo(eventType = EventType.IDENTITY_ALIAS, params = params)
        is NativeSuperwallEvent.AppInstall ->
            SuperwallEventInfo(eventType = EventType.APP_INSTALL, params = params)
        is NativeSuperwallEvent.SessionStart ->
            SuperwallEventInfo(eventType = EventType.SESSION_START, params = params)
        is NativeSuperwallEvent.ConfigAttributes ->
            SuperwallEventInfo(eventType = EventType.CONFIG_ATTRIBUTES, params = params)
        is NativeSuperwallEvent.DeviceAttributes ->
            SuperwallEventInfo(
                eventType = EventType.DEVICE_ATTRIBUTES,
                deviceAttributes = sanitizeParams(event.attributes),
                params = params,
            )
        is NativeSuperwallEvent.IntegrationAttributes ->
            SuperwallEventInfo(
                eventType = EventType.INTEGRATION_ATTRIBUTES,
                integrationAttributes = sanitizeParams(event.audienceFilterParams),
                params = params,
            )
        is NativeSuperwallEvent.IntegrationProps ->
            SuperwallEventInfo(
                eventType = EventType.INTEGRATION_ATTRIBUTES,
                integrationAttributes = sanitizeParams(event.audienceFilterParams),
                params = params,
            )
        is NativeSuperwallEvent.SubscriptionStatusDidChange ->
            SuperwallEventInfo(eventType = EventType.SUBSCRIPTION_STATUS_DID_CHANGE, params = params)
        is NativeSuperwallEvent.AppClose ->
            SuperwallEventInfo(eventType = EventType.APP_CLOSE, params = params)
        is NativeSuperwallEvent.DeepLink ->
            SuperwallEventInfo(
                eventType = EventType.DEEP_LINK,
                deepLinkUrl = event.uri.toString(),
                params = params,
            )
        is NativeSuperwallEvent.TriggerFire ->
            SuperwallEventInfo(
                eventType = EventType.TRIGGER_FIRE,
                placementName = event.placementName,
                result = event.result.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallOpen ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_OPEN,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallClose ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_CLOSE,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallDecline ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_DECLINE,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.TransactionStart ->
            SuperwallEventInfo(
                eventType = EventType.TRANSACTION_START,
                product = event.product.toKmp(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.TransactionFail ->
            SuperwallEventInfo(
                eventType = EventType.TRANSACTION_FAIL,
                error = event.error.message,
                product = (event.error as? NativeTransactionError.Failure)?.product?.toKmp(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.TransactionAbandon ->
            SuperwallEventInfo(
                eventType = EventType.TRANSACTION_ABANDON,
                product = event.product.toKmp(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.TransactionComplete ->
            SuperwallEventInfo(
                eventType = EventType.TRANSACTION_COMPLETE,
                transaction = event.transaction?.toKmp(),
                product = event.product.toKmp(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.SubscriptionStart ->
            SuperwallEventInfo(
                eventType = EventType.SUBSCRIPTION_START,
                product = event.product.toKmp(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.FreeTrialStart ->
            SuperwallEventInfo(
                eventType = EventType.FREE_TRIAL_START,
                product = event.product.toKmp(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        // Correctly labeled TRANSACTION_RESTORE — the iOS Flutter host's
        // transactionComplete mislabel is NOT replicated.
        is NativeSuperwallEvent.TransactionRestore ->
            SuperwallEventInfo(
                eventType = EventType.TRANSACTION_RESTORE,
                restoreType = event.restoreType.toKmp(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.Restore.Start ->
            SuperwallEventInfo(eventType = EventType.RESTORE_START, params = params)
        is NativeSuperwallEvent.Restore.Fail ->
            SuperwallEventInfo(
                eventType = EventType.RESTORE_FAIL,
                message = event.reason,
                params = params,
            )
        is NativeSuperwallEvent.Restore.Complete ->
            SuperwallEventInfo(eventType = EventType.RESTORE_COMPLETE, params = params)
        is NativeSuperwallEvent.TransactionTimeout ->
            SuperwallEventInfo(
                eventType = EventType.TRANSACTION_TIMEOUT,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.UserAttributes ->
            SuperwallEventInfo(
                eventType = EventType.USER_ATTRIBUTES,
                userAttributes = sanitizeParams(event.attributes),
                params = params,
            )
        // `product` is synthesized from the native TransactionProduct; the id
        // is also under params["transactionProductId"], the only place iOS
        // carries it.
        is NativeSuperwallEvent.NonRecurringProductPurchase ->
            SuperwallEventInfo(
                eventType = EventType.NON_RECURRING_PRODUCT_PURCHASE,
                product = event.product.toKmp(),
                transactionProduct = event.product.toKmpTransactionProduct(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params.with("transactionProductId" to event.product.id),
            )
        is NativeSuperwallEvent.PaywallResponseLoadStart ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_RESPONSE_LOAD_START,
                triggeredPlacementName = event.triggeredPlacementName,
                params = params,
            )
        is NativeSuperwallEvent.PaywallResponseLoadNotFound ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_RESPONSE_LOAD_NOT_FOUND,
                triggeredPlacementName = event.triggeredPlacementName,
                params = params,
            )
        is NativeSuperwallEvent.PaywallResponseLoadFail ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_RESPONSE_LOAD_FAIL,
                triggeredPlacementName = event.triggeredPlacementName,
                params = params,
            )
        is NativeSuperwallEvent.PaywallResponseLoadComplete ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_RESPONSE_LOAD_COMPLETE,
                triggeredPlacementName = event.triggeredPlacementName,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallWebviewLoadStart ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_WEBVIEW_LOAD_START,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallWebviewLoadFail ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_WEBVIEW_LOAD_FAIL,
                // Fixed vs the Flutter host: a null WebviewError stays null
                // instead of becoming the string "null".
                error = event.errorMessage?.toString(),
                webviewError = event.errorMessage?.toKmp(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallWebviewLoadComplete ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_WEBVIEW_LOAD_COMPLETE,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallWebviewLoadTimeout ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_WEBVIEW_LOAD_TIMEOUT,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallWebviewLoadFallback ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_WEBVIEW_LOAD_FALLBACK,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallProductsLoadStart ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_PRODUCTS_LOAD_START,
                triggeredPlacementName = event.triggeredPlacementName,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallProductsLoadFail ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_PRODUCTS_LOAD_FAIL,
                error = event.errorMessage,
                triggeredPlacementName = event.triggeredPlacementName,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallProductsLoadComplete ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_PRODUCTS_LOAD_COMPLETE,
                triggeredPlacementName = event.triggeredPlacementName,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PaywallResourceLoadFail ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_RESOURCE_LOAD_FAIL,
                error = event.error,
                resourceUrl = event.url,
                params = params.with("url" to event.url),
            )
        is NativeSuperwallEvent.PaywallPresentationRequest ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_PRESENTATION_REQUEST,
                status = event.status.toKmp(),
                reason = event.reason?.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.SurveyResponse ->
            SuperwallEventInfo(
                eventType = EventType.SURVEY_RESPONSE,
                survey = event.survey.toKmp(),
                selectedOption = event.selectedOption.toKmp(),
                customResponse = event.customResponse,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.SurveyClose ->
            SuperwallEventInfo(eventType = EventType.SURVEY_CLOSE, params = params)
        is NativeSuperwallEvent.ConfigRefresh ->
            SuperwallEventInfo(eventType = EventType.CONFIG_REFRESH, params = params)
        is NativeSuperwallEvent.ConfigFail ->
            SuperwallEventInfo(eventType = EventType.CONFIG_FAIL, params = params)
        is NativeSuperwallEvent.ConfirmAllAssignments ->
            SuperwallEventInfo(eventType = EventType.CONFIRM_ALL_ASSIGNMENTS, params = params)
        is NativeSuperwallEvent.Reset ->
            SuperwallEventInfo(eventType = EventType.RESET, params = params)
        is NativeSuperwallEvent.CustomPlacement ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOM_PLACEMENT,
                name = event.placementName,
                params = sanitizeParams(event.params),
                paywallInfo = event.paywallInfo.toKmp(),
            )
        is NativeSuperwallEvent.ShimmerViewStart ->
            SuperwallEventInfo(eventType = EventType.SHIMMER_VIEW_START, params = params)
        is NativeSuperwallEvent.ShimmerViewComplete ->
            SuperwallEventInfo(
                eventType = EventType.SHIMMER_VIEW_COMPLETE,
                shimmerDuration = event.duration,
                params = params.with("duration" to event.duration),
            )
        is NativeSuperwallEvent.RedemptionStart ->
            SuperwallEventInfo(eventType = EventType.REDEMPTION_START, params = params)
        is NativeSuperwallEvent.RedemptionComplete ->
            SuperwallEventInfo(eventType = EventType.REDEMPTION_COMPLETE, params = params)
        is NativeSuperwallEvent.RedemptionFail ->
            SuperwallEventInfo(
                eventType = EventType.REDEMPTION_FAIL,
                // The native case carries no error payload; the raw event name
                // stands in for it (Flutter host parity).
                error = event.rawName,
                params = params,
            )
        is NativeSuperwallEvent.EnrichmentStart ->
            SuperwallEventInfo(eventType = EventType.ENRICHMENT_START, params = params)
        is NativeSuperwallEvent.EnrichmentFail ->
            SuperwallEventInfo(eventType = EventType.ENRICHMENT_FAIL, params = params)
        is NativeSuperwallEvent.EnrichmentComplete ->
            SuperwallEventInfo(
                eventType = EventType.ENRICHMENT_COMPLETE,
                userEnrichment = sanitizeParams(event.userEnrichment),
                deviceEnrichment = sanitizeParams(event.deviceEnrichment),
                params = params,
            )
        is NativeSuperwallEvent.ReviewRequested ->
            SuperwallEventInfo(
                eventType = EventType.REVIEW_REQUESTED,
                reviewRequestedCount = event.count.toLong(),
                params = params,
            )
        is NativeSuperwallEvent.ReviewGranted ->
            SuperwallEventInfo(
                eventType = EventType.REVIEW_GRANTED,
                reviewRequestedCount = event.count.toLong(),
                params = params,
            )
        is NativeSuperwallEvent.ReviewDenied ->
            SuperwallEventInfo(
                eventType = EventType.REVIEW_DENIED,
                reviewRequestedCount = event.count.toLong(),
                params = params,
            )
        is NativeSuperwallEvent.CustomerInfoDidChange ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOMER_INFO_DID_CHANGE,
                previousCustomerInfo = event.from.toKmp(),
                customerInfo = event.to.toKmp(),
                params = params,
            )
        is NativeSuperwallEvent.PermissionRequested ->
            SuperwallEventInfo(
                eventType = EventType.PERMISSION_REQUESTED,
                name = event.permissionName,
                permissionName = event.permissionName,
                paywallIdentifier = event.paywallIdentifier,
                params = params.withPermission(event.permissionName, event.paywallIdentifier),
            )
        is NativeSuperwallEvent.PermissionGranted ->
            SuperwallEventInfo(
                eventType = EventType.PERMISSION_GRANTED,
                name = event.permissionName,
                permissionName = event.permissionName,
                paywallIdentifier = event.paywallIdentifier,
                params = params.withPermission(event.permissionName, event.paywallIdentifier),
            )
        is NativeSuperwallEvent.PermissionDenied ->
            SuperwallEventInfo(
                eventType = EventType.PERMISSION_DENIED,
                name = event.permissionName,
                permissionName = event.permissionName,
                paywallIdentifier = event.paywallIdentifier,
                params = params.withPermission(event.permissionName, event.paywallIdentifier),
            )
        is NativeSuperwallEvent.PaywallPreloadStart ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_PRELOAD_START,
                paywallCount = event.paywallCount,
                params = params.with("paywallCount" to event.paywallCount.toLong()),
            )
        is NativeSuperwallEvent.PaywallPreloadComplete ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_PRELOAD_COMPLETE,
                paywallCount = event.paywallCount,
                params = params.with("paywallCount" to event.paywallCount.toLong()),
            )
        is NativeSuperwallEvent.PaywallPageView ->
            SuperwallEventInfo(
                eventType = EventType.PAYWALL_PAGE_VIEW,
                paywallInfo = event.paywallInfo.toKmp(),
                pageViewData = event.data.toKmp(),
                params = params.with("pageViewData" to event.data.toParamsMap()),
            )
        is NativeSuperwallEvent.AttributionMatch ->
            SuperwallEventInfo(
                eventType = EventType.ATTRIBUTION_MATCH,
                attributionMatch = event.info.toKmp(),
                params = params.with("attributionMatch" to event.info.toParamsMap()),
            )
        is NativeSuperwallEvent.TestModeModalOpen ->
            SuperwallEventInfo(eventType = EventType.TEST_MODE_MODAL_OPEN, params = params)
        is NativeSuperwallEvent.TestModeModalClose ->
            SuperwallEventInfo(eventType = EventType.TEST_MODE_MODAL_CLOSE, params = params)
        is NativeSuperwallEvent.CustomerCenterOpen ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOMER_CENTER_OPEN,
                customerCenter =
                    CustomerCenterEventInfo(
                        screen =
                            when (event.screen) {
                                NativeCustomerCenterScreenType.MANAGEMENT -> CustomerCenterScreenType.MANAGEMENT
                                NativeCustomerCenterScreenType.NO_PURCHASES -> CustomerCenterScreenType.NO_PURCHASES
                            },
                    ),
                params = params,
            )
        is NativeSuperwallEvent.CustomerCenterClose ->
            SuperwallEventInfo(eventType = EventType.CUSTOMER_CENTER_CLOSE, params = params)
        is NativeSuperwallEvent.CustomerCenterAction ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOMER_CENTER_ACTION,
                customerCenter =
                    CustomerCenterEventInfo(
                        action = event.action.toKmp(),
                        pathId = event.pathId,
                        productId = event.productId,
                    ),
                params = params,
            )
        is NativeSuperwallEvent.CustomerCenterSurveyResponse ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOMER_CENTER_SURVEY_RESPONSE,
                customerCenter =
                    CustomerCenterEventInfo(
                        surveyId = event.surveyId,
                        optionId = event.optionId,
                        action = event.action.toKmp(),
                        pathId = event.pathId,
                        productId = event.productId,
                    ),
                params = params,
            )
        is NativeSuperwallEvent.CustomerCenterRefundRequest ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOMER_CENTER_REFUND_REQUEST,
                customerCenter =
                    CustomerCenterEventInfo(
                        productId = event.productId,
                        refundStatus = event.status.toKmp(),
                    ),
                params = params,
            )
        // The SDK-internal events (ErrorThrown, ExpressionResult) and any case
        // newer than superwall-android 2.10.2: Flutter-host fallback shape, with
        // the raw name also under params["rawEventType"] as on iOS.
        else ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOM_PLACEMENT,
                name = event.rawName,
                params = params.with("rawEventType" to event.rawName),
            )
    }
}

/** [this] plus [entries]; never null, since [entries] is never empty. */
private fun Map<String, Any?>?.with(vararg entries: Pair<String, Any?>): Map<String, Any?> = (this ?: emptyMap()) + entries

/** The permission payload, under the keys the iOS mapper uses. */
private fun Map<String, Any?>?.withPermission(
    permissionName: String,
    paywallIdentifier: String,
): Map<String, Any?> = with("permissionName" to permissionName, "paywallIdentifier" to paywallIdentifier)

/** [NativePageViewData] as a params map, keyed like the iOS mapper's copy. */
internal fun NativePageViewData.toParamsMap(): Map<String, Any?> =
    buildMap {
        put("pageNodeId", pageNodeId)
        put("flowPosition", flowPosition.toLong())
        put("pageName", pageName)
        put("navigationNodeId", navigationNodeId)
        previousPageNodeId?.let { put("previousPageNodeId", it) }
        previousFlowPosition?.let { put("previousFlowPosition", it.toLong()) }
        put("navigationType", navigationType)
        timeOnPreviousPageMs?.let { put("timeOnPreviousPageMs", it.toLong()) }
    }

/**
 * [NativeAttributionMatchInfo] as a params map, keyed like the iOS mapper's
 * copy, with the provider and confidence as their raw wire names.
 */
internal fun NativeAttributionMatchInfo.toParamsMap(): Map<String, Any?> =
    buildMap {
        put("provider", provider.rawName)
        put("matched", matched)
        source?.let { put("source", it) }
        confidence?.let { put("confidence", it.rawName) }
        matchScore?.let { put("matchScore", it) }
        reason?.let { put("reason", it) }
    }

internal fun NativePageViewData.toKmp(): PageViewData =
    PageViewData(
        pageNodeId = pageNodeId,
        flowPosition = flowPosition,
        pageName = pageName,
        navigationNodeId = navigationNodeId,
        previousPageNodeId = previousPageNodeId,
        previousFlowPosition = previousFlowPosition,
        navigationType = navigationType,
        timeOnPreviousPageMs = timeOnPreviousPageMs?.toLong(),
    )

internal fun NativeAttributionMatchInfo.toKmp(): AttributionMatchInfo =
    AttributionMatchInfo(
        provider = provider.rawName,
        matched = matched,
        source = source,
        confidence = confidence?.rawName,
        matchScore = matchScore,
        reason = reason,
    )

internal fun NativeWebviewError.toKmp(): WebviewError =
    when (this) {
        is NativeWebviewError.NetworkError -> WebviewError.NetworkError(code = code, description = description, url = url)
        is NativeWebviewError.Timeout -> WebviewError.Timeout(msg)
        is NativeWebviewError.MaxAttemptsReached -> WebviewError.MaxAttemptsReached(urls)
        is NativeWebviewError.NoUrls -> WebviewError.NoUrls
        is NativeWebviewError.AllUrlsFailed -> WebviewError.AllUrlsFailed(urls)
    }
