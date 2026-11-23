package com.superwall.sdk.kmp.models.events

import com.superwall.sdk.kmp.models.customercenter.CustomerCenterAction
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterRefundStatus

/** The kind of purchase a completed transaction was, on [EventType.TRANSACTION_COMPLETE]. */
public enum class TransactionType {
    /** A one-off purchase of a consumable or non-consumable product. */
    NON_RECURRING_PRODUCT_PURCHASE,

    /** A subscription purchase that began with a free trial. */
    FREE_TRIAL_START,

    /** A subscription purchase without a free trial. */
    SUBSCRIPTION_START,
}

/**
 * The result of matching an install to the ad click that led to it, on
 * [EventType.ATTRIBUTION_MATCH].
 *
 * [provider] and [confidence] stay strings so that a value the native SDKs
 * add later still arrives.
 */
public data class AttributionMatchInfo(
    /** The provider that made the match: `mmp` or `apple_search_ads`. */
    val provider: String,
    /** Whether the install was matched to a click. */
    val matched: Boolean,
    /** The source the install was attributed to, e.g. the ad network. */
    val source: String? = null,
    /** How confident the provider is in the match: `high`, `medium` or `low`. */
    val confidence: String? = null,
    /** The provider's match score, when it reports one. */
    val matchScore: Double? = null,
    /** Why the install was or wasn't matched, when the provider says. */
    val reason: String? = null,
)

/** A page view in a multi-page paywall, on [EventType.PAYWALL_PAGE_VIEW]. */
public data class PageViewData(
    /** The identifier of the page the user is now on. */
    val pageNodeId: String,
    /** The position of the page in the flow, starting at 0. */
    val flowPosition: Int,
    /** The name of the page. */
    val pageName: String,
    /** The identifier of the navigation element holding the pages. */
    val navigationNodeId: String,
    /** The identifier of the page the user came from, if any. */
    val previousPageNodeId: String? = null,
    /** The position of the page the user came from, if any. */
    val previousFlowPosition: Int? = null,
    /** How the user got here: `entry`, `forward`, `back` or `auto_transition`. */
    val navigationType: String,
    /** How long the user spent on the previous page, in milliseconds. */
    val timeOnPreviousPageMs: Long? = null,
)

/**
 * Why a paywall's web view failed to load, on
 * [EventType.PAYWALL_WEBVIEW_LOAD_FAIL]. Android only: SuperwallKit reports no
 * reason.
 */
public sealed interface WebviewError {
    /** The network request for [url] failed with [code]. */
    public data class NetworkError(
        val code: Int,
        val description: String,
        val url: String,
    ) : WebviewError

    /** Loading took longer than the configured timeout. */
    public data class Timeout(val message: String) : WebviewError

    /** Every retry was used up; [urls] are the URLs that were tried. */
    public data class MaxAttemptsReached(val urls: List<String>) : WebviewError

    /** The paywall had no URL to load. */
    public data object NoUrls : WebviewError

    /** All of the paywall's [urls] failed to load. */
    public data class AllUrlsFailed(val urls: List<String>) : WebviewError

    /** A failure this SDK version has no case for, as the native SDK describes it. */
    public data class Unknown(val description: String) : WebviewError
}

/** Which Customer Center screen was shown, on [EventType.CUSTOMER_CENTER_OPEN]. */
public enum class CustomerCenterScreenType {
    /** The user has, or had, at least one purchase. */
    MANAGEMENT,

    /** The user has no purchases. */
    NO_PURCHASES,
}

/** How the Customer Center was shown, on [EventType.CUSTOMER_CENTER_OPEN]. */
public enum class CustomerCenterPresentationStyle {
    /** Presented over the app by `Superwall.presentCustomerCenter`. */
    SHEET,

    /** Embedded in the app's own view hierarchy. */
    EMBEDDED,
}

/**
 * The payload of the Customer Center events. Only the fields of the event at
 * hand are set:
 *
 * - [EventType.CUSTOMER_CENTER_OPEN]: [screen], and on iOS [presentationStyle].
 * - [EventType.CUSTOMER_CENTER_ACTION]: [action], [pathId], [productId].
 * - [EventType.CUSTOMER_CENTER_SURVEY_RESPONSE]: [surveyId], [optionId],
 *   [action], [pathId], [productId].
 * - [EventType.CUSTOMER_CENTER_REFUND_REQUEST]: [productId], [refundStatus].
 */
public data class CustomerCenterEventInfo(
    /** The screen that was opened. */
    val screen: CustomerCenterScreenType? = null,
    /** How the Customer Center was shown. iOS only; `null` on Android. */
    val presentationStyle: CustomerCenterPresentationStyle? = null,
    /** The action of the path the user tapped. */
    val action: CustomerCenterAction? = null,
    /** The identifier of the path the user tapped. */
    val pathId: String? = null,
    /** The product the action or refund concerns, if any. */
    val productId: String? = null,
    /** The identifier of the survey the user answered. */
    val surveyId: String? = null,
    /** The identifier of the survey option the user chose. */
    val optionId: String? = null,
    /** How the refund request ended. */
    val refundStatus: CustomerCenterRefundStatus? = null,
)
