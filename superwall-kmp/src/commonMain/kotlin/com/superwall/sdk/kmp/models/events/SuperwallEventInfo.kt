package com.superwall.sdk.kmp.models.events

import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.paywall.Survey
import com.superwall.sdk.kmp.models.paywall.SurveyOption
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusReason
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusType
import com.superwall.sdk.kmp.models.results.RestoreType
import com.superwall.sdk.kmp.models.results.TriggerResult
import com.superwall.sdk.kmp.models.store.StoreProduct
import com.superwall.sdk.kmp.models.store.StoreTransaction
import com.superwall.sdk.kmp.models.store.TransactionProduct

/**
 * Contains information about an internally tracked Superwall event.
 *
 * A flat envelope: [eventType] identifies the event and only the fields relevant
 * to that event are non-null. Delivered to `SuperwallDelegate.handleSuperwallEvent`.
 *
 * @property eventType The type of the event.
 * @property params Parameters associated with the event. Some payload
 * values that now have a field of their own are still added here, as they
 * were before the fields existed, under the same key on both platforms: `permissionName` and `paywallIdentifier` (permission events),
 * `paywallCount` (preload events), `pageViewData` ([EventType.PAYWALL_PAGE_VIEW]),
 * `attributionMatch` ([EventType.ATTRIBUTION_MATCH]), `transactionProductId`
 * ([EventType.NON_RECURRING_PRODUCT_PURCHASE], whose [product] is only set
 * on Android); on iOS only, `transactionType` ([EventType.TRANSACTION_COMPLETE]);
 * on Android only, `url` ([EventType.PAYWALL_RESOURCE_LOAD_FAIL]) and `duration`
 * ([EventType.SHIMMER_VIEW_COMPLETE]). An event newer than this SDK arrives
 * as [EventType.CUSTOM_PLACEMENT] with its native name in [name] and under
 * `rawEventType`.
 * @property placementName The name of the placement, where applicable.
 * @property deviceAttributes The device attributes, for device-attribute events.
 * @property deepLinkUrl The deep link URL, for deep-link events.
 * @property result The trigger result, for trigger-fire events.
 * @property paywallInfo Info about the paywall involved in the event.
 * @property transaction The store transaction involved in the event.
 * @property product The store product involved in the event.
 * @property error A description of the error, for failure events.
 * @property triggeredPlacementName The name of the placement that triggered the event.
 * @property attempt The retry attempt number, for retry events.
 * @property name The name associated with the event (e.g. a custom placement name).
 * @property survey The survey involved in the event.
 * @property selectedOption The survey option the user selected.
 * @property customResponse The user's custom survey response.
 * @property status The status of a paywall presentation request.
 * @property reason The reason for a paywall presentation request status.
 * @property restoreType The type of restore, for transaction-restore events.
 * @property userAttributes The user attributes, for user-attribute events.
 * @property token The AdServices token, for AdServices events.
 * @property userEnrichment User enrichment data, for enrichment events.
 * @property deviceEnrichment Device enrichment data, for enrichment events.
 * @property message A message associated with the event.
 * @property integrationAttributes The integration attributes, for integration-attribute events.
 * @property reviewRequestedCount The number of times a review has been requested.
 * @property missingProductIdentifiers The product identifiers that could not be loaded.
 * @property transactionType The kind of purchase, for [EventType.TRANSACTION_COMPLETE].
 * iOS only: superwall-android doesn't report it.
 * @property transactionProduct The purchased product, for
 * [EventType.NON_RECURRING_PRODUCT_PURCHASE], with the same shape on both platforms.
 * @property attributionMatch The match result, for [EventType.ATTRIBUTION_MATCH].
 * @property pageViewData The page view, for [EventType.PAYWALL_PAGE_VIEW].
 * @property permissionName The permission, for the permission events.
 * @property paywallIdentifier The paywall that asked for the permission, for the permission events.
 * @property paywallCount The number of paywalls, for the preload events.
 * @property previousCustomerInfo The customer info before the change, for
 * [EventType.CUSTOMER_INFO_DID_CHANGE]. Android only: SuperwallKit's event has no payload.
 * @property customerInfo The customer info after the change, for
 * [EventType.CUSTOMER_INFO_DID_CHANGE]. Android only.
 * @property webviewError Why the web view failed to load, for
 * [EventType.PAYWALL_WEBVIEW_LOAD_FAIL]. Android only; [error] is its description.
 * @property resourceUrl The resource that failed to load, for
 * [EventType.PAYWALL_RESOURCE_LOAD_FAIL]. Android only.
 * @property shimmerDuration How long the loading shimmer was shown, in the
 * native SDK's unit, for [EventType.SHIMMER_VIEW_COMPLETE]. Android only.
 * @property customerCenter The payload of the Customer Center events.
 */
public data class SuperwallEventInfo(
    val eventType: EventType,
    val params: Map<String, Any?>? = null,
    val placementName: String? = null,
    val deviceAttributes: Map<String, Any?>? = null,
    val deepLinkUrl: String? = null,
    val result: TriggerResult? = null,
    val paywallInfo: PaywallInfo? = null,
    val transaction: StoreTransaction? = null,
    val product: StoreProduct? = null,
    val error: String? = null,
    val triggeredPlacementName: String? = null,
    val attempt: Long? = null,
    val name: String? = null,
    val survey: Survey? = null,
    val selectedOption: SurveyOption? = null,
    val customResponse: String? = null,
    val status: PaywallPresentationRequestStatusType? = null,
    val reason: PaywallPresentationRequestStatusReason? = null,
    val restoreType: RestoreType? = null,
    val userAttributes: Map<String, Any?>? = null,
    val token: String? = null,
    val userEnrichment: Map<String, Any?>? = null,
    val deviceEnrichment: Map<String, Any?>? = null,
    val message: String? = null,
    val integrationAttributes: Map<String, Any?>? = null,
    val reviewRequestedCount: Long? = null,
    val missingProductIdentifiers: List<String>? = null,
    val transactionType: TransactionType? = null,
    val transactionProduct: TransactionProduct? = null,
    val attributionMatch: AttributionMatchInfo? = null,
    val pageViewData: PageViewData? = null,
    val permissionName: String? = null,
    val paywallIdentifier: String? = null,
    val paywallCount: Int? = null,
    val previousCustomerInfo: CustomerInfo? = null,
    val customerInfo: CustomerInfo? = null,
    val webviewError: WebviewError? = null,
    val resourceUrl: String? = null,
    val shimmerDuration: Double? = null,
    val customerCenter: CustomerCenterEventInfo? = null,
)
