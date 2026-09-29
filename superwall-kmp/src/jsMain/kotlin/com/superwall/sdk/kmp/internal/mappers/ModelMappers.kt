package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.interop.JsAnySanitizer
import com.superwall.sdk.kmp.internal.interop.arrayOrEmpty
import com.superwall.sdk.kmp.internal.interop.booleanOrNull
import com.superwall.sdk.kmp.internal.interop.jsObject
import com.superwall.sdk.kmp.internal.interop.numberOrNull
import com.superwall.sdk.kmp.internal.interop.stringOrNull
import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.EntitlementType
import com.superwall.sdk.kmp.models.entitlements.Entitlements
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionOfferType
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionState
import com.superwall.sdk.kmp.models.entitlements.NonSubscriptionTransaction
import com.superwall.sdk.kmp.models.entitlements.ProductStore
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.entitlements.SubscriptionTransaction
import com.superwall.sdk.kmp.models.paywall.ComputedPropertyRequest
import com.superwall.sdk.kmp.models.paywall.ComputedPropertyRequestType
import com.superwall.sdk.kmp.models.paywall.FeatureGatingBehavior
import com.superwall.sdk.kmp.models.paywall.PaywallCloseReason
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.paywall.PaywallProduct
import com.superwall.sdk.kmp.models.paywall.Survey
import com.superwall.sdk.kmp.models.paywall.SurveyOption
import com.superwall.sdk.kmp.models.paywall.SurveyShowCondition
import com.superwall.sdk.kmp.models.redemption.ErrorInfo
import com.superwall.sdk.kmp.models.redemption.ExpiredCodeInfo
import com.superwall.sdk.kmp.models.redemption.Ownership
import com.superwall.sdk.kmp.models.redemption.PurchaserInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionResult
import com.superwall.sdk.kmp.models.redemption.StoreIdentifiers
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusReason
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusType
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import com.superwall.sdk.kmp.models.results.PresentationResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.results.RestoreType
import com.superwall.sdk.kmp.models.results.TriggerResult
import com.superwall.sdk.kmp.models.store.StoreTransaction
import com.superwall.sdk.kmp.models.triggers.ConfirmedAssignment
import com.superwall.sdk.kmp.models.triggers.Experiment
import com.superwall.sdk.kmp.models.triggers.Variant
import com.superwall.sdk.kmp.models.triggers.VariantType
import kotlin.time.Instant

// Maps @superwall/paywalls-js payloads (plain JSON-shaped objects, see its
// types.ts / @superwall/core types.ts) to the commonMain models. Like the
// native mappers these degrade rather than throw: a missing or unrecognised
// field becomes `null` / a documented default, never an exception into the
// JS SDK's callback.

// ---- Entitlements / subscription -------------------------------------------

internal fun productStoreFromJs(value: String?): ProductStore? =
    when (value) {
        null -> null
        "appStore" -> ProductStore.APP_STORE
        "stripe" -> ProductStore.STRIPE
        "paddle" -> ProductStore.PADDLE
        "playStore" -> ProductStore.PLAY_STORE
        "superwall" -> ProductStore.SUPERWALL
        "custom" -> ProductStore.CUSTOM
        else -> ProductStore.OTHER
    }

internal fun ProductStore.toJs(): String =
    when (this) {
        ProductStore.APP_STORE -> "appStore"
        ProductStore.STRIPE -> "stripe"
        ProductStore.PADDLE -> "paddle"
        ProductStore.PLAY_STORE -> "playStore"
        ProductStore.SUPERWALL -> "superwall"
        // Web's ProductStore union has no "custom"; "other" is its catch-all.
        ProductStore.CUSTOM, ProductStore.OTHER -> "other"
    }

private fun subscriptionStateFromJs(value: String?): LatestSubscriptionState? =
    when (value) {
        "inGracePeriod" -> LatestSubscriptionState.IN_GRACE_PERIOD
        "subscribed" -> LatestSubscriptionState.SUBSCRIBED
        "expired" -> LatestSubscriptionState.EXPIRED
        "inBillingRetryPeriod" -> LatestSubscriptionState.IN_BILLING_RETRY_PERIOD
        "revoked" -> LatestSubscriptionState.REVOKED
        else -> null
    }

private fun LatestSubscriptionState.toJs(): String =
    when (this) {
        LatestSubscriptionState.IN_GRACE_PERIOD -> "inGracePeriod"
        LatestSubscriptionState.SUBSCRIBED -> "subscribed"
        LatestSubscriptionState.EXPIRED -> "expired"
        LatestSubscriptionState.IN_BILLING_RETRY_PERIOD -> "inBillingRetryPeriod"
        LatestSubscriptionState.REVOKED -> "revoked"
    }

private fun offerTypeFromJs(value: String?): LatestSubscriptionOfferType? =
    when (value) {
        "trial" -> LatestSubscriptionOfferType.TRIAL
        "code" -> LatestSubscriptionOfferType.CODE
        "promotional" -> LatestSubscriptionOfferType.PROMOTIONAL
        "winback" -> LatestSubscriptionOfferType.WINBACK
        else -> null
    }

private fun LatestSubscriptionOfferType.toJs(): String =
    when (this) {
        LatestSubscriptionOfferType.TRIAL -> "trial"
        LatestSubscriptionOfferType.CODE -> "code"
        LatestSubscriptionOfferType.PROMOTIONAL -> "promotional"
        LatestSubscriptionOfferType.WINBACK -> "winback"
    }

/** Web timestamps on entitlements and transactions are ms since epoch. */
private fun epochMillisOrNull(obj: dynamic, key: String): Instant? = numberOrNull(obj, key)?.let { Instant.fromEpochMilliseconds(it.toLong()) }

/** PaywallInfo timestamps are ISO-8601 strings; unparseable ones become `null`. */
private fun isoInstantOrNull(obj: dynamic, key: String): Instant? =
    stringOrNull(obj, key)?.let { runCatching { Instant.parse(it) }.getOrNull() }

internal fun entitlementFromJs(value: dynamic): Entitlement =
    Entitlement(
        id = stringOrNull(value, "id") ?: "",
        type = EntitlementType.SERVICE_LEVEL,
        isActive = booleanOrNull(value, "isActive") ?: true,
        productIds = arrayOrEmpty(value, "productIds").mapNotNull { it as? String },
        latestProductId = stringOrNull(value, "latestProductId"),
        store = productStoreFromJs(stringOrNull(value, "store")),
        startsAt = epochMillisOrNull(value, "startsAt"),
        renewedAt = epochMillisOrNull(value, "renewedAt"),
        expiresAt = epochMillisOrNull(value, "expiresAt"),
        isLifetime = booleanOrNull(value, "isLifetime"),
        willRenew = booleanOrNull(value, "willRenew"),
        state = subscriptionStateFromJs(stringOrNull(value, "state")),
        offerType = offerTypeFromJs(stringOrNull(value, "offerType")),
    )

internal fun entitlementsFromJs(values: Array<dynamic>): Set<Entitlement> = values.map { entitlementFromJs(it) }.toSet()

internal fun Entitlement.toJs(): dynamic {
    val obj = jsObject()
    obj.id = id
    obj.type = "SERVICE_LEVEL"
    obj.isActive = isActive
    obj.productIds = productIds.toTypedArray()
    latestProductId?.let { obj.latestProductId = it }
    store?.let { obj.store = it.toJs() }
    startsAt?.let { obj.startsAt = it.toEpochMilliseconds().toDouble() }
    renewedAt?.let { obj.renewedAt = it.toEpochMilliseconds().toDouble() }
    expiresAt?.let { obj.expiresAt = it.toEpochMilliseconds().toDouble() }
    isLifetime?.let { obj.isLifetime = it }
    willRenew?.let { obj.willRenew = it }
    state?.let { obj.state = it.toJs() }
    offerType?.let { obj.offerType = it.toJs() }
    return obj
}

/**
 * Web has no separate "web entitlements" bucket — every entitlement it knows
 * about came from a web checkout — so [Entitlements.web] mirrors [Entitlements.all].
 */
internal fun entitlementsSnapshot(
    active: Array<dynamic>,
    inactive: Array<dynamic>,
    all: Array<dynamic>,
): Entitlements {
    val allSet = entitlementsFromJs(all)
    return Entitlements(
        active = entitlementsFromJs(active),
        inactive = entitlementsFromJs(inactive),
        all = allSet,
        web = allSet,
    )
}

internal fun subscriptionStatusFromJs(value: dynamic): SubscriptionStatus =
    when (if (value == null) null else stringOrNull(value, "status")) {
        "ACTIVE" -> SubscriptionStatus.Active(entitlementsFromJs(arrayOrEmpty(value, "entitlements")))
        "INACTIVE" -> SubscriptionStatus.Inactive
        else -> SubscriptionStatus.Unknown
    }

internal fun SubscriptionStatus.toJs(): dynamic {
    val obj = jsObject()
    when (this) {
        is SubscriptionStatus.Active -> {
            obj.status = "ACTIVE"
            obj.entitlements = entitlements.map { it.toJs() }.toTypedArray()
        }
        SubscriptionStatus.Inactive -> obj.status = "INACTIVE"
        SubscriptionStatus.Unknown -> obj.status = "UNKNOWN"
    }
    return obj
}

internal fun customerInfoFromJs(value: dynamic): CustomerInfo =
    CustomerInfo(
        subscriptions =
            arrayOrEmpty(value, "subscriptions").map { tx ->
                SubscriptionTransaction(
                    transactionId = stringOrNull(tx, "transactionId") ?: "",
                    productId = stringOrNull(tx, "productId") ?: "",
                    purchaseDate = epochMillisOrNull(tx, "purchaseDate") ?: Instant.fromEpochMilliseconds(0),
                    willRenew = booleanOrNull(tx, "willRenew") ?: false,
                    isRevoked = booleanOrNull(tx, "isRevoked") ?: false,
                    isInGracePeriod = booleanOrNull(tx, "isInGracePeriod") ?: false,
                    isInBillingRetryPeriod = booleanOrNull(tx, "isInBillingRetryPeriod") ?: false,
                    isActive = booleanOrNull(tx, "isActive") ?: false,
                    expirationDate = epochMillisOrNull(tx, "expirationDate"),
                    offerType = offerTypeFromJs(stringOrNull(tx, "offerType")),
                    subscriptionGroupId = stringOrNull(tx, "subscriptionGroupId"),
                    store = productStoreFromJs(stringOrNull(tx, "store")),
                )
            },
        nonSubscriptions =
            arrayOrEmpty(value, "nonSubscriptions").map { tx ->
                NonSubscriptionTransaction(
                    transactionId = stringOrNull(tx, "transactionId") ?: "",
                    productId = stringOrNull(tx, "productId") ?: "",
                    purchaseDate = epochMillisOrNull(tx, "purchaseDate") ?: Instant.fromEpochMilliseconds(0),
                    isConsumable = booleanOrNull(tx, "isConsumable") ?: false,
                    isRevoked = booleanOrNull(tx, "isRevoked") ?: false,
                    store = productStoreFromJs(stringOrNull(tx, "store")),
                )
            },
        entitlements = arrayOrEmpty(value, "entitlements").map { entitlementFromJs(it) },
        userId = stringOrNull(value, "userId") ?: "",
    )

/** Stands in for `getCustomerInfo()` resolving `null` (pre-configure / nothing fetched yet). */
internal fun emptyCustomerInfo(userId: String): CustomerInfo =
    CustomerInfo(subscriptions = emptyList(), nonSubscriptions = emptyList(), entitlements = emptyList(), userId = userId)

// ---- Experiments -------------------------------------------------------------

internal fun variantFromJs(value: dynamic): Variant =
    Variant(
        id = stringOrNull(value, "id") ?: "",
        type = if (stringOrNull(value, "type") == "holdout") VariantType.HOLDOUT else VariantType.TREATMENT,
        paywallId = stringOrNull(value, "paywallId"),
    )

internal fun experimentFromJs(value: dynamic): Experiment =
    Experiment(
        id = stringOrNull(value, "id") ?: "",
        groupId = stringOrNull(value, "groupId") ?: "",
        variant = if (value.variant == null) Variant(id = "", type = VariantType.TREATMENT) else variantFromJs(value.variant),
    )

internal fun confirmedAssignmentFromJs(value: dynamic): ConfirmedAssignment =
    ConfirmedAssignment(
        experimentId = stringOrNull(value, "experimentId") ?: "",
        variant = variantFromJs(value.variant ?: jsObject()),
    )

// ---- Paywall info ------------------------------------------------------------

internal fun paywallCloseReasonFromJs(value: String?): PaywallCloseReason? =
    when (value) {
        "systemLogic" -> PaywallCloseReason.SYSTEM_LOGIC
        "forNextPaywall" -> PaywallCloseReason.FOR_NEXT_PAYWALL
        "webViewFailedToLoad" -> PaywallCloseReason.WEB_VIEW_FAILED_TO_LOAD
        "manualClose" -> PaywallCloseReason.MANUAL_CLOSE
        "none" -> PaywallCloseReason.NONE
        else -> null
    }

private fun computedPropertyTypeFromJs(value: String?): ComputedPropertyRequestType? =
    when (value) {
        "minutesSince" -> ComputedPropertyRequestType.MINUTES_SINCE
        "hoursSince" -> ComputedPropertyRequestType.HOURS_SINCE
        "daysSince" -> ComputedPropertyRequestType.DAYS_SINCE
        "monthsSince" -> ComputedPropertyRequestType.MONTHS_SINCE
        "yearsSince" -> ComputedPropertyRequestType.YEARS_SINCE
        "placementsInHour" -> ComputedPropertyRequestType.PLACEMENTS_IN_HOUR
        "placementsInDay" -> ComputedPropertyRequestType.PLACEMENTS_IN_DAY
        "placementsInWeek" -> ComputedPropertyRequestType.PLACEMENTS_IN_WEEK
        "placementsInMonth" -> ComputedPropertyRequestType.PLACEMENTS_IN_MONTH
        "placementsSinceInstall" -> ComputedPropertyRequestType.PLACEMENTS_SINCE_INSTALL
        else -> null
    }

/** Web's option is `{ id, title }`; the common model calls the label `text`. */
internal fun surveyOptionFromJs(value: dynamic): SurveyOption =
    SurveyOption(id = stringOrNull(value, "id"), text = stringOrNull(value, "title") ?: stringOrNull(value, "text"))

internal fun surveyFromJs(value: dynamic): Survey =
    Survey(
        id = stringOrNull(value, "id") ?: "",
        assignmentKey = stringOrNull(value, "assignmentKey") ?: "",
        title = stringOrNull(value, "title") ?: "",
        message = stringOrNull(value, "message") ?: "",
        options = arrayOrEmpty(value, "options").map { surveyOptionFromJs(it) },
        presentationCondition =
            if (stringOrNull(value, "presentationCondition") == "ON_PURCHASE") {
                SurveyShowCondition.ON_PURCHASE
            } else {
                SurveyShowCondition.ON_MANUAL_CLOSE
            },
        presentationProbability = numberOrNull(value, "presentationProbability") ?: 0.0,
        includeOtherOption = booleanOrNull(value, "includeOtherOption") ?: false,
        includeCloseOption = booleanOrNull(value, "includeCloseOption") ?: false,
    )

internal fun paywallProductFromJs(value: dynamic): PaywallProduct =
    PaywallProduct(
        id = stringOrNull(value, "id"),
        name = stringOrNull(value, "name"),
        entitlements = entitlementsFromJs(arrayOrEmpty(value, "entitlements")),
    )

/**
 * Web-only PaywallInfo fields (`databaseId`, `rawProducts`, `productsV2`,
 * presentation style, colours, …) have no common counterpart and are dropped;
 * `localNotifications` does not exist on web and stays `null`.
 */
internal fun paywallInfoFromJs(value: dynamic): PaywallInfo =
    PaywallInfo(
        identifier = stringOrNull(value, "identifier"),
        name = stringOrNull(value, "name"),
        experiment = if (value.experiment == null) null else experimentFromJs(value.experiment),
        productIds = arrayOrEmpty(value, "productIds").mapNotNull { it as? String },
        products = arrayOrEmpty(value, "products").map { paywallProductFromJs(it) },
        url = stringOrNull(value, "url"),
        presentedByPlacementWithName = stringOrNull(value, "presentedByPlacementWithName"),
        presentedByPlacementWithId = stringOrNull(value, "presentedByPlacementWithId"),
        presentedByPlacementAt = isoInstantOrNull(value, "presentedByPlacementAt"),
        presentedBy = stringOrNull(value, "presentedBy"),
        presentationSourceType = stringOrNull(value, "presentationSourceType"),
        responseLoadStartTime = isoInstantOrNull(value, "responseLoadStartTime"),
        responseLoadCompleteTime = isoInstantOrNull(value, "responseLoadCompleteTime"),
        responseLoadFailTime = isoInstantOrNull(value, "responseLoadFailTime"),
        responseLoadDuration = numberOrNull(value, "responseLoadDuration"),
        webViewLoadStartTime = isoInstantOrNull(value, "webViewLoadStartTime"),
        webViewLoadCompleteTime = isoInstantOrNull(value, "webViewLoadCompleteTime"),
        webViewLoadFailTime = isoInstantOrNull(value, "webViewLoadFailTime"),
        webViewLoadDuration = numberOrNull(value, "webViewLoadDuration"),
        productsLoadStartTime = isoInstantOrNull(value, "productsLoadStartTime"),
        productsLoadCompleteTime = isoInstantOrNull(value, "productsLoadCompleteTime"),
        productsLoadFailTime = isoInstantOrNull(value, "productsLoadFailTime"),
        productsLoadDuration = numberOrNull(value, "productsLoadDuration"),
        paywalljsVersion = stringOrNull(value, "paywalljsVersion"),
        isFreeTrialAvailable = booleanOrNull(value, "isFreeTrialAvailable"),
        featureGatingBehavior =
            when (stringOrNull(value, "featureGatingBehavior")) {
                "gated" -> FeatureGatingBehavior.GATED
                "nonGated" -> FeatureGatingBehavior.NON_GATED
                else -> null
            },
        closeReason = paywallCloseReasonFromJs(stringOrNull(value, "closeReason")),
        localNotifications = null,
        computedPropertyRequests =
            arrayOrEmpty(value, "computedPropertyRequests").mapNotNull { request ->
                computedPropertyTypeFromJs(stringOrNull(request, "type"))?.let { type ->
                    ComputedPropertyRequest(type = type, eventName = stringOrNull(request, "eventName") ?: "")
                }
            },
        surveys = arrayOrEmpty(value, "surveys").map { surveyFromJs(it) },
        state = JsAnySanitizer.fromObject(value.state),
    )

/** `null`-tolerant wrapper for payload fields that may be absent. */
internal fun paywallInfoOrNull(value: dynamic): PaywallInfo? = if (value == null) null else paywallInfoFromJs(value)

// ---- Results -----------------------------------------------------------------

internal fun presentationResultFromJs(value: dynamic): PresentationResult =
    when (if (value == null) null else stringOrNull(value, "type")) {
        "paywall" -> PresentationResult.Paywall(experimentFromJs(value.experiment ?: jsObject()))
        "holdout" -> PresentationResult.Holdout(experimentFromJs(value.experiment ?: jsObject()))
        "noAudienceMatch" -> PresentationResult.NoAudienceMatch
        "placementNotFound" -> PresentationResult.PlacementNotFound
        else -> PresentationResult.PaywallNotAvailable
    }

/** `null` for an unrecognised result type — the caller skips `onDismiss` rather than invent one. */
internal fun paywallResultFromJs(value: dynamic): PaywallResult? =
    when (if (value == null) null else stringOrNull(value, "type")) {
        "purchased" -> PaywallResult.Purchased(stringOrNull(value, "productId") ?: "")
        "declined" -> PaywallResult.Declined
        "restored" -> PaywallResult.Restored
        else -> null
    }

/**
 * `null` for `userSubscribed`: web reports "already entitled" as a skip, but
 * the native SDKs don't (they just run `feature`), so the common
 * [PaywallSkippedReason] has no such case and `onSkip` is not invoked for it.
 */
internal fun paywallSkippedReasonFromJs(value: dynamic): PaywallSkippedReason? =
    when (if (value == null) null else stringOrNull(value, "type")) {
        "holdout" -> PaywallSkippedReason.Holdout(experimentFromJs(value.experiment ?: jsObject()))
        "noAudienceMatch" -> PaywallSkippedReason.NoAudienceMatch
        "placementNotFound" -> PaywallSkippedReason.PlacementNotFound
        else -> null
    }

internal fun triggerResultFromJs(value: dynamic): TriggerResult? =
    when (if (value == null) null else stringOrNull(value, "type")) {
        "placementNotFound" -> TriggerResult.PlacementNotFound
        "noAudienceMatch" -> TriggerResult.NoAudienceMatch
        "paywall" -> TriggerResult.Paywall(experimentFromJs(value.experiment ?: jsObject()))
        "holdout" -> TriggerResult.Holdout(experimentFromJs(value.experiment ?: jsObject()))
        "error" -> TriggerResult.Error(stringOrNull(value, "error") ?: "")
        else -> null
    }

internal fun presentationStatusTypeFromJs(value: String?): PaywallPresentationRequestStatusType? =
    when (value) {
        "presentation" -> PaywallPresentationRequestStatusType.PRESENTATION
        "noPresentation" -> PaywallPresentationRequestStatusType.NO_PRESENTATION
        "timeout" -> PaywallPresentationRequestStatusType.TIMEOUT
        else -> null
    }

internal fun presentationStatusReasonFromJs(value: dynamic): PaywallPresentationRequestStatusReason? =
    when (if (value == null) null else stringOrNull(value, "type")) {
        "debuggerPresented" -> PaywallPresentationRequestStatusReason.DebuggerPresented
        "paywallAlreadyPresented" -> PaywallPresentationRequestStatusReason.PaywallAlreadyPresented
        "holdout" -> PaywallPresentationRequestStatusReason.Holdout(experimentFromJs(value.experiment ?: jsObject()))
        "noAudienceMatch" -> PaywallPresentationRequestStatusReason.NoAudienceMatch
        "placementNotFound" -> PaywallPresentationRequestStatusReason.PlacementNotFound
        "noPaywallView" -> PaywallPresentationRequestStatusReason.NoPaywallViewController
        "noPresenter" -> PaywallPresentationRequestStatusReason.NoPresenter
        "noConfig" -> PaywallPresentationRequestStatusReason.NoConfig
        "subsStatusTimeout" -> PaywallPresentationRequestStatusReason.SubscriptionStatusTimeout
        else -> null
    }

internal fun storeTransactionFromJs(value: dynamic): StoreTransaction =
    StoreTransaction(
        configRequestId = stringOrNull(value, "configRequestId") ?: "",
        appSessionId = stringOrNull(value, "appSessionId") ?: "",
        originalTransactionIdentifier = stringOrNull(value, "originalTransactionIdentifier") ?: "",
        transactionDate = isoInstantOrNull(value, "transactionDate"),
        storeTransactionId = stringOrNull(value, "storeTransactionId"),
        originalTransactionDate = isoInstantOrNull(value, "originalTransactionDate"),
        webOrderLineItemID = stringOrNull(value, "webOrderLineItemID"),
        appBundleId = stringOrNull(value, "appBundleId"),
        subscriptionGroupId = stringOrNull(value, "subscriptionGroupId"),
        isUpgraded = booleanOrNull(value, "isUpgraded"),
        expirationDate = isoInstantOrNull(value, "expirationDate"),
        offerId = stringOrNull(value, "offerId"),
        revocationDate = isoInstantOrNull(value, "revocationDate"),
    )

internal fun restoreTypeFromJs(value: dynamic): RestoreType? =
    when (if (value == null) null else stringOrNull(value, "type")) {
        "viaPurchase" ->
            RestoreType.ViaPurchase(
                if (value.storeTransaction == null) null else storeTransactionFromJs(value.storeTransaction),
            )
        "viaRestore" -> RestoreType.ViaRestore
        else -> null
    }

/** Web's `{ type: "failed", error: Error }` carries an Error object; common carries its message. */
internal fun restorationResultFromJs(value: dynamic): RestorationResult =
    when (if (value == null) null else stringOrNull(value, "type")) {
        "restored" -> RestorationResult.Restored
        else -> RestorationResult.Failed(errorMessage(value?.error) ?: "Restore failed.")
    }

/**
 * Web's redemption response is slimmer than the native ones: a success
 * carries only the code and the granted entitlements. The required
 * [RedemptionInfo] ownership / purchaser fields are therefore synthesized
 * from the current user ([userId]) with an unknown `"stripe"` store
 * identifier; there is no paywall info. Web has no "expired subscription"
 * outcome.
 */
internal fun redemptionResultFromJs(
    value: dynamic,
    userId: String,
): RedemptionResult {
    val code = if (value == null) "" else stringOrNull(value, "code") ?: ""
    return when (if (value == null) null else stringOrNull(value, "type")) {
        "success" ->
            RedemptionResult.Success(
                code = code,
                redemptionInfo =
                    RedemptionInfo(
                        ownership = Ownership.AppUser(userId),
                        purchaserInfo =
                            PurchaserInfo(
                                appUserId = userId,
                                storeIdentifiers = StoreIdentifiers.Unknown(store = "stripe", additionalInfo = emptyMap()),
                            ),
                        paywallInfo = null,
                        entitlements = entitlementsFromJs(arrayOrEmpty(value, "entitlements")),
                    ),
            )
        "expired" -> RedemptionResult.ExpiredCode(code = code, info = ExpiredCodeInfo(resent = false))
        "invalid" -> RedemptionResult.InvalidCode(code = code)
        else -> RedemptionResult.Error(code = code, error = ErrorInfo(stringOrNull(value, "error") ?: "Redemption failed."))
    }
}

/** The message of a JS `Error` (or the string itself), if any. */
internal fun errorMessage(error: dynamic): String? =
    when {
        error == null -> null
        jsTypeOf(error) == "string" -> error as String
        jsTypeOf(error.message) == "string" -> error.message as String
        else -> error.toString()
    }
