package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import com.superwall.sdk.analytics.superwall.SuperwallEvent as NativeSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo as NativeSuperwallEventInfo
import com.superwall.sdk.store.transactions.TransactionError as NativeTransactionError

/**
 * Maps the native [NativeSuperwallEventInfo] (a `SuperwallEvent` sealed-class
 * payload plus a params map) to the common flat [SuperwallEventInfo] envelope.
 *
 * Port of the Flutter host's `EventMapper.toPigeonEventInfo`
 * (utils/EventMapper.kt) covering EVERY public case of superwall-android
 * 2.7.11's `SuperwallEvent`, with these deliberate deltas:
 *
 * - `PaywallWebviewLoadFail` no longer stringifies a null `WebviewError` into
 *   `"null"` — it maps to a null error.
 * - `TriggerFire` keeps the event params (the Flutter host dropped them).
 * - `TransactionFail` also carries the failing product when the native error
 *   is a [NativeTransactionError.Failure] (the Flutter host dropped it).
 * - `IntegrationAttributes`, `EnrichmentComplete`, `CustomerInfoDidChange`,
 *   `ReviewRequested` and the permission events map to their real event types
 *   with their payloads (the six silently-dropped fields of plan §3.4).
 *
 * Native cases with no common [EventType] counterpart (`PaywallPageView`,
 * `TestModeModalOpen/Close`, `ReviewGranted/Denied`, the SDK-internal events,
 * and anything added after this SDK version) degrade to the Flutter host's
 * fallback: [EventType.CUSTOM_PLACEMENT] with `name = rawName` — degrade,
 * never crash (plan §7).
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
        // transactionComplete mislabel (plan §3.4) is NOT replicated.
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
        is NativeSuperwallEvent.NonRecurringProductPurchase ->
            SuperwallEventInfo(
                eventType = EventType.NON_RECURRING_PRODUCT_PURCHASE,
                product = event.product.toKmp(),
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
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
                params = params,
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
            SuperwallEventInfo(eventType = EventType.SHIMMER_VIEW_COMPLETE, params = params)
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
        // ReviewGranted / ReviewDenied have no common EventType counterpart —
        // degrade to the fallback shape, keeping the count.
        is NativeSuperwallEvent.ReviewGranted ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOM_PLACEMENT,
                name = event.rawName,
                reviewRequestedCount = event.count.toLong(),
                params = params,
            )
        is NativeSuperwallEvent.ReviewDenied ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOM_PLACEMENT,
                name = event.rawName,
                reviewRequestedCount = event.count.toLong(),
                params = params,
            )
        is NativeSuperwallEvent.CustomerInfoDidChange ->
            SuperwallEventInfo(eventType = EventType.CUSTOMER_INFO_DID_CHANGE, params = params)
        is NativeSuperwallEvent.PermissionRequested ->
            SuperwallEventInfo(
                eventType = EventType.PERMISSION_REQUESTED,
                name = event.permissionName,
                params = params,
            )
        is NativeSuperwallEvent.PermissionGranted ->
            SuperwallEventInfo(
                eventType = EventType.PERMISSION_GRANTED,
                name = event.permissionName,
                params = params,
            )
        is NativeSuperwallEvent.PermissionDenied ->
            SuperwallEventInfo(
                eventType = EventType.PERMISSION_DENIED,
                name = event.permissionName,
                params = params,
            )
        is NativeSuperwallEvent.PaywallPreloadStart ->
            SuperwallEventInfo(eventType = EventType.PAYWALL_PRELOAD_START, params = params)
        is NativeSuperwallEvent.PaywallPreloadComplete ->
            SuperwallEventInfo(eventType = EventType.PAYWALL_PRELOAD_COMPLETE, params = params)
        // No common EventType counterpart, but the paywall payload is kept.
        is NativeSuperwallEvent.PaywallPageView ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOM_PLACEMENT,
                name = event.rawName,
                paywallInfo = event.paywallInfo.toKmp(),
                params = params,
            )
        // TestModeModalOpen/Close, the SDK-internal events (ErrorThrown,
        // ExpressionResult) and any case newer than superwall-android 2.7.11:
        // Flutter-host fallback shape.
        else ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOM_PLACEMENT,
                name = event.rawName,
                params = params,
            )
    }
}
