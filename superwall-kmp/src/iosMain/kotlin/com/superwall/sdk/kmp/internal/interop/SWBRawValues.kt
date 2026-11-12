package com.superwall.sdk.kmp.internal.interop

// ---------------------------------------------------------------------------
// Raw values of the Swift bridge's `@objc` Int enums, redeclared as Kotlin
// constants.
//
// The cinterop bindings do not expose generated NS_ENUM case constants for
// every bridge enum (several enums never appear in an `@objc` API signature
// and are only carried as NSNumber raw values), so the mappers depend on these
// local declarations instead of generated symbols.
//
// IMPORTANT: every value below MUST match the explicit raw value declared in
// bridge/Sources/SuperwallKMPBridge (Envelopes.swift / SuperwallKMPBridge.swift).
// The bridge's raw-value stability XCTests cover these numbers; each constant's
// comment names the Swift enum case it mirrors.
// ---------------------------------------------------------------------------

// SWBLatestSubscriptionState (Envelopes.swift)
internal const val SWB_LATEST_SUBSCRIPTION_STATE_IN_GRACE_PERIOD: Long = 0 // SWBLatestSubscriptionState.inGracePeriod
internal const val SWB_LATEST_SUBSCRIPTION_STATE_SUBSCRIBED: Long = 1 // SWBLatestSubscriptionState.subscribed
internal const val SWB_LATEST_SUBSCRIPTION_STATE_EXPIRED: Long = 2 // SWBLatestSubscriptionState.expired
internal const val SWB_LATEST_SUBSCRIPTION_STATE_IN_BILLING_RETRY_PERIOD: Long = 3 // SWBLatestSubscriptionState.inBillingRetryPeriod
internal const val SWB_LATEST_SUBSCRIPTION_STATE_REVOKED: Long = 4 // SWBLatestSubscriptionState.revoked

// SWBLatestSubscriptionOfferType (Envelopes.swift)
internal const val SWB_LATEST_SUBSCRIPTION_OFFER_TYPE_TRIAL: Long = 0 // SWBLatestSubscriptionOfferType.trial
internal const val SWB_LATEST_SUBSCRIPTION_OFFER_TYPE_CODE: Long = 1 // SWBLatestSubscriptionOfferType.code
internal const val SWB_LATEST_SUBSCRIPTION_OFFER_TYPE_PROMOTIONAL: Long = 2 // SWBLatestSubscriptionOfferType.promotional
internal const val SWB_LATEST_SUBSCRIPTION_OFFER_TYPE_WINBACK: Long = 3 // SWBLatestSubscriptionOfferType.winback

// SWBStoreTransactionState (Envelopes.swift)
internal const val SWB_STORE_TRANSACTION_STATE_PURCHASING: Long = 0 // SWBStoreTransactionState.purchasing
internal const val SWB_STORE_TRANSACTION_STATE_PURCHASED: Long = 1 // SWBStoreTransactionState.purchased
internal const val SWB_STORE_TRANSACTION_STATE_FAILED: Long = 2 // SWBStoreTransactionState.failed
internal const val SWB_STORE_TRANSACTION_STATE_RESTORED: Long = 3 // SWBStoreTransactionState.restored
internal const val SWB_STORE_TRANSACTION_STATE_DEFERRED: Long = 4 // SWBStoreTransactionState.deferred

// SWBTransactionType (Envelopes.swift)
internal const val SWB_TRANSACTION_TYPE_NON_RECURRING_PRODUCT_PURCHASE: Long = 0 // SWBTransactionType.nonRecurringProductPurchase
internal const val SWB_TRANSACTION_TYPE_FREE_TRIAL_START: Long = 1 // SWBTransactionType.freeTrialStart
internal const val SWB_TRANSACTION_TYPE_SUBSCRIPTION_START: Long = 2 // SWBTransactionType.subscriptionStart

// SWBPaywallPresentationRequestStatus (Envelopes.swift)
internal const val SWB_PAYWALL_PRESENTATION_REQUEST_STATUS_PRESENTATION: Long = 0 // SWBPaywallPresentationRequestStatus.presentation
internal const val SWB_PAYWALL_PRESENTATION_REQUEST_STATUS_NO_PRESENTATION: Long = 1 // SWBPaywallPresentationRequestStatus.noPresentation
internal const val SWB_PAYWALL_PRESENTATION_REQUEST_STATUS_TIMEOUT: Long = 2 // SWBPaywallPresentationRequestStatus.timeout

// SWBNetworkEnvironment (SuperwallKMPBridge.swift)
internal const val SWB_NETWORK_ENVIRONMENT_RELEASE: Long = 0 // SWBNetworkEnvironment.release
internal const val SWB_NETWORK_ENVIRONMENT_RELEASE_CANDIDATE: Long = 1 // SWBNetworkEnvironment.releaseCandidate
internal const val SWB_NETWORK_ENVIRONMENT_DEVELOPER: Long = 2 // SWBNetworkEnvironment.developer

// SWBTestModeBehavior (SuperwallKMPBridge.swift)
internal const val SWB_TEST_MODE_BEHAVIOR_AUTOMATIC: Long = 0 // SWBTestModeBehavior.automatic
internal const val SWB_TEST_MODE_BEHAVIOR_WHEN_ENABLED_FOR_USER: Long = 1 // SWBTestModeBehavior.whenEnabledForUser
internal const val SWB_TEST_MODE_BEHAVIOR_NEVER: Long = 2 // SWBTestModeBehavior.never
internal const val SWB_TEST_MODE_BEHAVIOR_ALWAYS: Long = 3 // SWBTestModeBehavior.always

// SWBEventTrackingBehavior (SuperwallKMPBridge.swift)
internal const val SWB_EVENT_TRACKING_BEHAVIOR_ALL: Long = 0 // SWBEventTrackingBehavior.all
internal const val SWB_EVENT_TRACKING_BEHAVIOR_SUPERWALL_ONLY: Long = 1 // SWBEventTrackingBehavior.superwallOnly
internal const val SWB_EVENT_TRACKING_BEHAVIOR_NONE: Long = 2 // SWBEventTrackingBehavior.none

// SuperwallOptions.StoreKitVersion (SuperwallKit), carried as
// SWBSuperwallOptions.storeKitVersion
internal const val SWB_STORE_KIT_VERSION_STOREKIT1: Long = 0 // SuperwallOptions.StoreKitVersion.storeKit1
internal const val SWB_STORE_KIT_VERSION_STOREKIT2: Long = 1 // SuperwallOptions.StoreKitVersion.storeKit2

// SWBTransactionBackgroundView (SuperwallKMPBridge.swift)
internal const val SWB_TRANSACTION_BACKGROUND_VIEW_SPINNER: Long = 0 // SWBTransactionBackgroundView.spinner
internal const val SWB_TRANSACTION_BACKGROUND_VIEW_NONE: Long = 1 // SWBTransactionBackgroundView.none

// SWBLogScope (SuperwallKMPBridge.swift)
internal const val SWB_LOG_SCOPE_LOCALIZATION_MANAGER: Long = 0 // SWBLogScope.localizationManager
internal const val SWB_LOG_SCOPE_BOUNCE_BUTTON: Long = 1 // SWBLogScope.bounceButton
internal const val SWB_LOG_SCOPE_CORE_DATA: Long = 2 // SWBLogScope.coreData
internal const val SWB_LOG_SCOPE_CONFIG_MANAGER: Long = 3 // SWBLogScope.configManager
internal const val SWB_LOG_SCOPE_IDENTITY_MANAGER: Long = 4 // SWBLogScope.identityManager
internal const val SWB_LOG_SCOPE_DEBUG_MANAGER: Long = 5 // SWBLogScope.debugManager
internal const val SWB_LOG_SCOPE_DEBUG_VIEW_CONTROLLER: Long = 6 // SWBLogScope.debugViewController
internal const val SWB_LOG_SCOPE_LOCALIZATION_VIEW_CONTROLLER: Long = 7 // SWBLogScope.localizationViewController
internal const val SWB_LOG_SCOPE_GAME_CONTROLLER_MANAGER: Long = 8 // SWBLogScope.gameControllerManager
internal const val SWB_LOG_SCOPE_DEVICE: Long = 9 // SWBLogScope.device
internal const val SWB_LOG_SCOPE_NETWORK: Long = 10 // SWBLogScope.network
internal const val SWB_LOG_SCOPE_PAYWALL_EVENTS: Long = 11 // SWBLogScope.paywallEvents
internal const val SWB_LOG_SCOPE_PRODUCTS_MANAGER: Long = 12 // SWBLogScope.productsManager
internal const val SWB_LOG_SCOPE_STORE_KIT_MANAGER: Long = 13 // SWBLogScope.storeKitManager
internal const val SWB_LOG_SCOPE_PLACEMENTS: Long = 14 // SWBLogScope.placements
internal const val SWB_LOG_SCOPE_RECEIPTS: Long = 15 // SWBLogScope.receipts
internal const val SWB_LOG_SCOPE_SUPERWALL_CORE: Long = 16 // SWBLogScope.superwallCore
internal const val SWB_LOG_SCOPE_PAYWALL_PRESENTATION: Long = 17 // SWBLogScope.paywallPresentation
internal const val SWB_LOG_SCOPE_TRANSACTIONS: Long = 18 // SWBLogScope.transactions
internal const val SWB_LOG_SCOPE_PAYWALL_VIEW_CONTROLLER: Long = 19 // SWBLogScope.paywallViewController
internal const val SWB_LOG_SCOPE_CACHE: Long = 20 // SWBLogScope.cache
internal const val SWB_LOG_SCOPE_ALL: Long = 21 // SWBLogScope.all
internal const val SWB_LOG_SCOPE_ANALYTICS: Long = 22 // SWBLogScope.analytics
internal const val SWB_LOG_SCOPE_WEB_ENTITLEMENTS: Long = 23 // SWBLogScope.webEntitlements
internal const val SWB_LOG_SCOPE_GRANTED_ENTITLEMENTS: Long = 24 // SWBLogScope.grantedEntitlements
internal const val SWB_LOG_SCOPE_CUSTOMER_CENTER: Long = 25 // SWBLogScope.customerCenter

// SWBCustomerCenterPathType (CustomerCenter.swift)
internal const val SWB_CUSTOMER_CENTER_PATH_TYPE_RESTORE: Long = 0 // SWBCustomerCenterPathType.restore
internal const val SWB_CUSTOMER_CENTER_PATH_TYPE_MANAGE_SUBSCRIPTION: Long = 1 // SWBCustomerCenterPathType.manageSubscription
internal const val SWB_CUSTOMER_CENTER_PATH_TYPE_REFUND: Long = 2 // SWBCustomerCenterPathType.refund
internal const val SWB_CUSTOMER_CENTER_PATH_TYPE_CHANGE_PLAN: Long = 3 // SWBCustomerCenterPathType.changePlan
internal const val SWB_CUSTOMER_CENTER_PATH_TYPE_CONTACT_SUPPORT: Long = 4 // SWBCustomerCenterPathType.contactSupport
internal const val SWB_CUSTOMER_CENTER_PATH_TYPE_URL: Long = 5 // SWBCustomerCenterPathType.url
internal const val SWB_CUSTOMER_CENTER_PATH_TYPE_CUSTOM: Long = 6 // SWBCustomerCenterPathType.custom

// SWBCustomerCenterActionType (CustomerCenter.swift)
internal const val SWB_CUSTOMER_CENTER_ACTION_TYPE_RESTORE: Long = 0 // SWBCustomerCenterActionType.restore
internal const val SWB_CUSTOMER_CENTER_ACTION_TYPE_MANAGE_SUBSCRIPTION: Long = 1 // SWBCustomerCenterActionType.manageSubscription
internal const val SWB_CUSTOMER_CENTER_ACTION_TYPE_REFUND: Long = 2 // SWBCustomerCenterActionType.refund
internal const val SWB_CUSTOMER_CENTER_ACTION_TYPE_CHANGE_PLAN: Long = 3 // SWBCustomerCenterActionType.changePlan
internal const val SWB_CUSTOMER_CENTER_ACTION_TYPE_CONTACT_SUPPORT: Long = 4 // SWBCustomerCenterActionType.contactSupport
internal const val SWB_CUSTOMER_CENTER_ACTION_TYPE_URL: Long = 5 // SWBCustomerCenterActionType.url
internal const val SWB_CUSTOMER_CENTER_ACTION_TYPE_CUSTOM: Long = 6 // SWBCustomerCenterActionType.custom

// Refund status raw values (CustomerCenterRefundStatus.swbRawValue, CustomerCenter.swift)
internal const val SWB_CUSTOMER_CENTER_REFUND_STATUS_SUCCESS: Long = 0 // .success
internal const val SWB_CUSTOMER_CENTER_REFUND_STATUS_USER_CANCELLED: Long = 1 // .userCancelled
internal const val SWB_CUSTOMER_CENTER_REFUND_STATUS_ERROR: Long = 2 // .error
