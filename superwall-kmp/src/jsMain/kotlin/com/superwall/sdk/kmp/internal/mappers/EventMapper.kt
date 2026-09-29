package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.interop.JsAnySanitizer
import com.superwall.sdk.kmp.internal.interop.numberOrNull
import com.superwall.sdk.kmp.internal.interop.stringOrNull
import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo

/**
 * Maps a web `SuperwallDelegate.onEvent(name, detail)` call to the common
 * [SuperwallEventInfo] envelope.
 *
 * Covers every key of `SuperwallEventMap` in @superwall/paywalls-js 0.3.0
 * (the test pins that list). `params` always carries the sanitized `detail`,
 * so payload fields without a typed slot are still reachable.
 *
 * Deliberate gaps, all degrading to the native mappers' fallback shape —
 * [EventType.CUSTOM_PLACEMENT] with `name = rawName`:
 * - web-only events with no common [EventType]: `page_view`,
 *   `paywall_page_view`, `discount_redeem_complete`, `discount_redeem_fail`;
 * - custom `track()` events, which web also routes through `onEvent`;
 * - anything added to the web SDK after this version.
 *
 * Web products are store-agnostic `{ id, name, entitlements, store }` and
 * carry none of the pricing fields the common `StoreProduct` requires, so the
 * transaction events leave [SuperwallEventInfo.product] `null` (the product is
 * still in `params`).
 */
internal fun webEventToKmp(
    name: String,
    detail: dynamic,
): SuperwallEventInfo {
    val d: dynamic = detail ?: js("({})")
    val params = JsAnySanitizer.fromObject(d)
    fun info(type: EventType): SuperwallEventInfo = SuperwallEventInfo(eventType = type, params = params)
    val paywallInfo = paywallInfoOrNull(d.paywall_info)
    val triggered = stringOrNull(d, "triggeredPlacementName")

    return when (name) {
        "first_seen" -> info(EventType.FIRST_SEEN)
        "app_open" -> info(EventType.APP_OPEN)
        "app_close" -> info(EventType.APP_CLOSE)
        "app_launch" -> info(EventType.APP_LAUNCH)
        "app_install" -> info(EventType.APP_INSTALL)
        "session_start" -> info(EventType.SESSION_START)
        "reset" -> info(EventType.RESET)
        "config_refresh" -> info(EventType.CONFIG_REFRESH)
        "config_fail" -> info(EventType.CONFIG_FAIL)
        "config_attributes" -> info(EventType.CONFIG_ATTRIBUTES)
        "confirm_all_assignments" -> info(EventType.CONFIRM_ALL_ASSIGNMENTS)
        "identity_alias" -> info(EventType.IDENTITY_ALIAS)
        "device_attributes" ->
            info(EventType.DEVICE_ATTRIBUTES).copy(deviceAttributes = JsAnySanitizer.fromObject(d.attributes))
        "user_attributes" ->
            info(EventType.USER_ATTRIBUTES).copy(userAttributes = JsAnySanitizer.fromObject(d.attributes))
        "integration_attributes" ->
            info(EventType.INTEGRATION_ATTRIBUTES).copy(
                integrationAttributes = JsAnySanitizer.fromObject(d.audienceFilterParams),
            )
        "deepLink_open" -> info(EventType.DEEP_LINK).copy(deepLinkUrl = stringOrNull(d, "uri"))
        "subscriptionStatus_didChange" -> info(EventType.SUBSCRIPTION_STATUS_DID_CHANGE)
        "customerInfo_didChange" -> info(EventType.CUSTOMER_INFO_DID_CHANGE)
        "trigger_fire" ->
            info(EventType.TRIGGER_FIRE).copy(
                placementName = stringOrNull(d, "placementName"),
                result = triggerResultFromJs(d.result),
            )
        "paywallPresentationRequest" ->
            info(EventType.PAYWALL_PRESENTATION_REQUEST).copy(
                status = presentationStatusTypeFromJs(stringOrNull(d, "status")),
                reason = presentationStatusReasonFromJs(d.reason),
            )

        "paywall_open" -> info(EventType.PAYWALL_OPEN).copy(paywallInfo = paywallInfo)
        "paywall_close" -> info(EventType.PAYWALL_CLOSE).copy(paywallInfo = paywallInfo)
        "paywall_decline" -> info(EventType.PAYWALL_DECLINE).copy(paywallInfo = paywallInfo)
        "paywallPreload_start" -> info(EventType.PAYWALL_PRELOAD_START)
        "paywallPreload_complete" -> info(EventType.PAYWALL_PRELOAD_COMPLETE)
        "paywallResponseLoad_start" ->
            info(EventType.PAYWALL_RESPONSE_LOAD_START).copy(triggeredPlacementName = triggered)
        "paywallResponseLoad_notFound" ->
            info(EventType.PAYWALL_RESPONSE_LOAD_NOT_FOUND).copy(triggeredPlacementName = triggered)
        "paywallResponseLoad_complete" ->
            info(EventType.PAYWALL_RESPONSE_LOAD_COMPLETE).copy(
                triggeredPlacementName = triggered,
                paywallInfo = paywallInfo,
            )
        "paywallResponseLoad_fail" ->
            info(EventType.PAYWALL_RESPONSE_LOAD_FAIL).copy(triggeredPlacementName = triggered)
        "paywallWebviewLoad_start" -> info(EventType.PAYWALL_WEBVIEW_LOAD_START).copy(paywallInfo = paywallInfo)
        "paywallWebviewLoad_complete" -> info(EventType.PAYWALL_WEBVIEW_LOAD_COMPLETE).copy(paywallInfo = paywallInfo)
        "paywallWebviewLoad_fail" ->
            info(EventType.PAYWALL_WEBVIEW_LOAD_FAIL).copy(
                paywallInfo = paywallInfo,
                error = stringOrNull(d, "errorMessage"),
            )
        "paywallWebviewLoad_timeout" -> info(EventType.PAYWALL_WEBVIEW_LOAD_TIMEOUT).copy(paywallInfo = paywallInfo)
        "paywallProductsLoad_start" ->
            info(EventType.PAYWALL_PRODUCTS_LOAD_START).copy(
                triggeredPlacementName = triggered,
                paywallInfo = paywallInfo,
            )
        "paywallProductsLoad_complete" ->
            info(EventType.PAYWALL_PRODUCTS_LOAD_COMPLETE).copy(
                triggeredPlacementName = triggered,
                paywallInfo = paywallInfo,
            )
        "paywallProductsLoad_fail" ->
            info(EventType.PAYWALL_PRODUCTS_LOAD_FAIL).copy(
                triggeredPlacementName = triggered,
                paywallInfo = paywallInfo,
                error = stringOrNull(d, "errorMessage"),
            )
        "paywallResourceLoad_fail" -> info(EventType.PAYWALL_RESOURCE_LOAD_FAIL).copy(error = stringOrNull(d, "error"))
        "shimmerView_start" -> info(EventType.SHIMMER_VIEW_START)
        "shimmerView_complete" -> info(EventType.SHIMMER_VIEW_COMPLETE)

        "survey_response" ->
            info(EventType.SURVEY_RESPONSE).copy(
                survey = if (d.survey == null) null else surveyFromJs(d.survey),
                selectedOption = if (d.selected_option == null) null else surveyOptionFromJs(d.selected_option),
                customResponse = stringOrNull(d, "custom_response"),
                paywallInfo = paywallInfo,
            )
        "survey_close" ->
            info(EventType.SURVEY_CLOSE).copy(
                survey = if (d.survey == null) null else surveyFromJs(d.survey),
                paywallInfo = paywallInfo,
            )

        "transaction_start" -> info(EventType.TRANSACTION_START).copy(paywallInfo = paywallInfo)
        "transaction_complete" ->
            info(EventType.TRANSACTION_COMPLETE).copy(
                paywallInfo = paywallInfo,
                transaction = if (d.transaction == null) null else storeTransactionFromJs(d.transaction),
            )
        "transaction_fail" -> info(EventType.TRANSACTION_FAIL).copy(paywallInfo = paywallInfo, error = stringOrNull(d, "error"))
        "transaction_abandon" -> info(EventType.TRANSACTION_ABANDON).copy(paywallInfo = paywallInfo)
        "transaction_timeout" -> info(EventType.TRANSACTION_TIMEOUT).copy(paywallInfo = paywallInfo)
        "transaction_restore" ->
            info(EventType.TRANSACTION_RESTORE).copy(
                paywallInfo = paywallInfo,
                restoreType = restoreTypeFromJs(d.restoreType),
            )
        "restore_start" -> info(EventType.RESTORE_START)
        "restore_complete" -> info(EventType.RESTORE_COMPLETE)
        "restore_fail" -> info(EventType.RESTORE_FAIL).copy(message = stringOrNull(d, "reason"))
        "subscription_start" -> info(EventType.SUBSCRIPTION_START).copy(paywallInfo = paywallInfo)
        "freeTrial_start" -> info(EventType.FREE_TRIAL_START).copy(paywallInfo = paywallInfo)
        "nonRecurringProduct_purchase" -> info(EventType.NON_RECURRING_PRODUCT_PURCHASE).copy(paywallInfo = paywallInfo)

        "enrichment_start" -> info(EventType.ENRICHMENT_START)
        "enrichment_complete" ->
            info(EventType.ENRICHMENT_COMPLETE).copy(
                userEnrichment = JsAnySanitizer.fromObject(d.userEnrichment),
                deviceEnrichment = JsAnySanitizer.fromObject(d.deviceEnrichment),
            )
        "enrichment_fail" -> info(EventType.ENRICHMENT_FAIL)

        "custom_placement" ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOM_PLACEMENT,
                name = stringOrNull(d, "placementName"),
                params = JsAnySanitizer.fromObject(d.params),
                paywallInfo = paywallInfo,
            )
        "review_requested" ->
            info(EventType.REVIEW_REQUESTED).copy(reviewRequestedCount = numberOrNull(d, "count")?.toLong())
        "permission_requested" -> info(EventType.PERMISSION_REQUESTED).copy(name = stringOrNull(d, "permissionName"))
        "permission_granted" -> info(EventType.PERMISSION_GRANTED).copy(name = stringOrNull(d, "permissionName"))
        "permission_denied" -> info(EventType.PERMISSION_DENIED).copy(name = stringOrNull(d, "permissionName"))

        // page_view, paywall_page_view, discount_redeem_*, custom track()
        // events and future additions: the fallback shape (see KDoc).
        else ->
            SuperwallEventInfo(
                eventType = EventType.CUSTOM_PLACEMENT,
                name = name,
                paywallInfo = paywallInfoOrNull(d.paywall_info ?: d.paywallInfo),
                params = params,
            )
    }
}
