package com.superwall.sdk.kmp.models.events

/**
 * The type of an internally tracked Superwall event.
 *
 * Identifies which event a [SuperwallEventInfo] envelope describes.
 */
public enum class EventType {
    /** The user was seen for the first time. */
    FIRST_SEEN,

    /** The app was opened. */
    APP_OPEN,

    /** The app was launched. */
    APP_LAUNCH,

    /** An identity alias was created. */
    IDENTITY_ALIAS,

    /** The app was installed. */
    APP_INSTALL,

    /** A restore of purchases started. */
    RESTORE_START,

    /** A restore of purchases completed. */
    RESTORE_COMPLETE,

    /** A restore of purchases failed. */
    RESTORE_FAIL,

    /** A new session started. */
    SESSION_START,

    /** Device attributes were tracked. */
    DEVICE_ATTRIBUTES,

    /** The subscription status changed. */
    SUBSCRIPTION_STATUS_DID_CHANGE,

    /** The app was closed. */
    APP_CLOSE,

    /** A deep link was opened. */
    DEEP_LINK,

    /** A trigger fired. */
    TRIGGER_FIRE,

    /** A paywall was opened. */
    PAYWALL_OPEN,

    /** A paywall was closed. */
    PAYWALL_CLOSE,

    /** The user declined a paywall. */
    PAYWALL_DECLINE,

    /** A transaction started. */
    TRANSACTION_START,

    /** A transaction failed. */
    TRANSACTION_FAIL,

    /** A transaction was abandoned. */
    TRANSACTION_ABANDON,

    /** A transaction completed. */
    TRANSACTION_COMPLETE,

    /** A subscription started. */
    SUBSCRIPTION_START,

    /** A free trial started. */
    FREE_TRIAL_START,

    /** A transaction was restored. */
    TRANSACTION_RESTORE,

    /** A transaction timed out. */
    TRANSACTION_TIMEOUT,

    /** User attributes were tracked. */
    USER_ATTRIBUTES,

    /** A non-recurring product was purchased. */
    NON_RECURRING_PRODUCT_PURCHASE,

    /** A paywall response started loading. */
    PAYWALL_RESPONSE_LOAD_START,

    /** A paywall response was not found. */
    PAYWALL_RESPONSE_LOAD_NOT_FOUND,

    /** A paywall response failed to load. */
    PAYWALL_RESPONSE_LOAD_FAIL,

    /** A paywall response finished loading. */
    PAYWALL_RESPONSE_LOAD_COMPLETE,

    /** A paywall webview started loading. */
    PAYWALL_WEBVIEW_LOAD_START,

    /** A paywall webview failed to load. */
    PAYWALL_WEBVIEW_LOAD_FAIL,

    /** A paywall webview finished loading. */
    PAYWALL_WEBVIEW_LOAD_COMPLETE,

    /** A paywall webview timed out while loading. */
    PAYWALL_WEBVIEW_LOAD_TIMEOUT,

    /** A paywall webview loaded from a fallback URL. */
    PAYWALL_WEBVIEW_LOAD_FALLBACK,

    /** Paywall products load was retried. */
    PAYWALL_PRODUCTS_LOAD_RETRY,

    /** Paywall products started loading. */
    PAYWALL_PRODUCTS_LOAD_START,

    /** Paywall products failed to load. */
    PAYWALL_PRODUCTS_LOAD_FAIL,

    /** Paywall products finished loading. */
    PAYWALL_PRODUCTS_LOAD_COMPLETE,

    /** Paywall preloading started. */
    PAYWALL_PRELOAD_START,

    /** Paywall preloading completed. */
    PAYWALL_PRELOAD_COMPLETE,

    /** A paywall resource failed to load. */
    PAYWALL_RESOURCE_LOAD_FAIL,

    /** The user responded to a survey. */
    SURVEY_RESPONSE,

    /** A paywall presentation was requested. */
    PAYWALL_PRESENTATION_REQUEST,

    /** Touches began on the app (iOS). */
    TOUCHES_BEGAN,

    /** A survey was closed without a response. */
    SURVEY_CLOSE,

    /** The Superwall SDK was reset. */
    RESET,

    /** The configuration was refreshed. */
    CONFIG_REFRESH,

    /** A custom placement was tracked from a paywall. */
    CUSTOM_PLACEMENT,

    /** Configuration attributes were tracked. */
    CONFIG_ATTRIBUTES,

    /** All assignments were confirmed. */
    CONFIRM_ALL_ASSIGNMENTS,

    /** The configuration failed to load. */
    CONFIG_FAIL,

    /** An AdServices token request started (iOS). */
    AD_SERVICES_TOKEN_REQUEST_START,

    /** An AdServices token request failed (iOS). */
    AD_SERVICES_TOKEN_REQUEST_FAIL,

    /** An AdServices token request completed (iOS). */
    AD_SERVICES_TOKEN_REQUEST_COMPLETE,

    /** A shimmer (loading) view started showing. */
    SHIMMER_VIEW_START,

    /** A shimmer (loading) view finished showing. */
    SHIMMER_VIEW_COMPLETE,

    /** A code redemption started. */
    REDEMPTION_START,

    /** A code redemption completed. */
    REDEMPTION_COMPLETE,

    /** A code redemption failed. */
    REDEMPTION_FAIL,

    /** Enrichment started. */
    ENRICHMENT_START,

    /** Enrichment completed. */
    ENRICHMENT_COMPLETE,

    /** Enrichment failed. */
    ENRICHMENT_FAIL,

    /** A network response failed to decode. */
    NETWORK_DECODING_FAIL,

    /** A paywall webview's process was terminated. */
    PAYWALL_WEBVIEW_PROCESS_TERMINATED,

    /** One or more paywall products could not be found. */
    PAYWALL_PRODUCTS_LOAD_MISSING_PRODUCTS,

    /** The customer info changed. */
    CUSTOMER_INFO_DID_CHANGE,

    /** Integration attributes were set. */
    INTEGRATION_ATTRIBUTES,

    /** An app store review was requested. */
    REVIEW_REQUESTED,

    /** A permission was requested. */
    PERMISSION_REQUESTED,

    /** A permission was granted. */
    PERMISSION_GRANTED,

    /** A permission was denied. */
    PERMISSION_DENIED,
}
