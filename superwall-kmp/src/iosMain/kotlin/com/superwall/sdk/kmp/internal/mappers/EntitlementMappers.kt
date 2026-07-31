@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlement
import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlementTypeServiceLevel
import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlements
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttribute
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeAdjustId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeAirshipChannelId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeAmplitudeDeviceId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeAmplitudeUserId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeAppsflyerId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeAppstackId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeBrazeAliasLabel
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeBrazeAliasName
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeClevertapId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeCustomerioId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeFbAnonId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeFirebaseInstallationId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeSingularDeviceId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeFirebaseAppInstanceId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeIterableCampaignId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeIterableTemplateId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeIterableUserId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeKochavaDeviceId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeMixpanelDistinctId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeMparticleId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeOnesignalId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributePosthogUserId
import com.superwall.sdk.kmp.internal.ios.interop.SWBIntegrationAttributeTenjinId
import com.superwall.sdk.kmp.internal.ios.interop.SWBLatestSubscriptionOfferTypeCode
import com.superwall.sdk.kmp.internal.ios.interop.SWBLatestSubscriptionOfferTypePromotional
import com.superwall.sdk.kmp.internal.ios.interop.SWBLatestSubscriptionOfferTypeTrial
import com.superwall.sdk.kmp.internal.ios.interop.SWBLatestSubscriptionOfferTypeWinback
import com.superwall.sdk.kmp.internal.ios.interop.SWBLatestSubscriptionStateExpired
import com.superwall.sdk.kmp.internal.ios.interop.SWBLatestSubscriptionStateInBillingRetryPeriod
import com.superwall.sdk.kmp.internal.ios.interop.SWBLatestSubscriptionStateInGracePeriod
import com.superwall.sdk.kmp.internal.ios.interop.SWBLatestSubscriptionStateRevoked
import com.superwall.sdk.kmp.internal.ios.interop.SWBLatestSubscriptionStateSubscribed
import com.superwall.sdk.kmp.internal.ios.interop.SWBNonSubscriptionTransaction
import com.superwall.sdk.kmp.internal.ios.interop.SWBProductStore
import com.superwall.sdk.kmp.internal.ios.interop.SWBProductStoreAppStore
import com.superwall.sdk.kmp.internal.ios.interop.SWBProductStoreCustom
import com.superwall.sdk.kmp.internal.ios.interop.SWBProductStoreOther
import com.superwall.sdk.kmp.internal.ios.interop.SWBProductStorePaddle
import com.superwall.sdk.kmp.internal.ios.interop.SWBProductStorePlayStore
import com.superwall.sdk.kmp.internal.ios.interop.SWBProductStoreStripe
import com.superwall.sdk.kmp.internal.ios.interop.SWBProductStoreSuperwall
import com.superwall.sdk.kmp.internal.ios.interop.SWBSubscriptionStatus
import com.superwall.sdk.kmp.internal.ios.interop.SWBSubscriptionStatusCaseActive
import com.superwall.sdk.kmp.internal.ios.interop.SWBSubscriptionStatusCaseInactive
import com.superwall.sdk.kmp.internal.ios.interop.SWBSubscriptionStatusCaseUnknown
import com.superwall.sdk.kmp.internal.ios.interop.SWBSubscriptionTransaction
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
import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import kotlin.time.Instant
import platform.Foundation.NSNumber

// ---------------------------------------------------------------------------
// Date helpers: the SWB CustomerInfo/Entitlement family uses epoch-ms
// NSNumbers, converted here to kotlin.time.Instant (plan §3.5 conventions).
// ---------------------------------------------------------------------------

internal fun NSNumber?.toInstantFromEpochMs(): Instant? =
    this?.let { Instant.fromEpochMilliseconds(it.longLongValue) }

internal fun Instant.toEpochMsNSNumber(): NSNumber = NSNumber(longLong = toEpochMilliseconds())

// ---------------------------------------------------------------------------
// Stores / subscription enums (SWB Int enums arrive as NSInteger constants).
// ---------------------------------------------------------------------------

internal fun productStoreFromSWB(value: SWBProductStore): ProductStore =
    when (value) {
        SWBProductStoreAppStore -> ProductStore.APP_STORE
        SWBProductStoreStripe -> ProductStore.STRIPE
        SWBProductStorePaddle -> ProductStore.PADDLE
        SWBProductStorePlayStore -> ProductStore.PLAY_STORE
        SWBProductStoreSuperwall -> ProductStore.SUPERWALL
        SWBProductStoreCustom -> ProductStore.CUSTOM
        SWBProductStoreOther -> ProductStore.OTHER
        else -> ProductStore.OTHER
    }

internal fun productStoreToSWB(store: ProductStore): SWBProductStore =
    when (store) {
        ProductStore.APP_STORE -> SWBProductStoreAppStore
        ProductStore.STRIPE -> SWBProductStoreStripe
        ProductStore.PADDLE -> SWBProductStorePaddle
        ProductStore.PLAY_STORE -> SWBProductStorePlayStore
        ProductStore.SUPERWALL -> SWBProductStoreSuperwall
        ProductStore.CUSTOM -> SWBProductStoreCustom
        ProductStore.OTHER -> SWBProductStoreOther
    }

internal fun productStoreFromNSNumber(value: NSNumber?): ProductStore? =
    value?.let { productStoreFromSWB(it.longLongValue) }

internal fun subscriptionStateFromNSNumber(value: NSNumber?): LatestSubscriptionState? =
    value?.let {
        when (it.longLongValue) {
            SWBLatestSubscriptionStateInGracePeriod -> LatestSubscriptionState.IN_GRACE_PERIOD
            SWBLatestSubscriptionStateSubscribed -> LatestSubscriptionState.SUBSCRIBED
            SWBLatestSubscriptionStateExpired -> LatestSubscriptionState.EXPIRED
            SWBLatestSubscriptionStateInBillingRetryPeriod -> LatestSubscriptionState.IN_BILLING_RETRY_PERIOD
            SWBLatestSubscriptionStateRevoked -> LatestSubscriptionState.REVOKED
            else -> null
        }
    }

internal fun offerTypeFromNSNumber(value: NSNumber?): LatestSubscriptionOfferType? =
    value?.let {
        when (it.longLongValue) {
            SWBLatestSubscriptionOfferTypeTrial -> LatestSubscriptionOfferType.TRIAL
            SWBLatestSubscriptionOfferTypeCode -> LatestSubscriptionOfferType.CODE
            SWBLatestSubscriptionOfferTypePromotional -> LatestSubscriptionOfferType.PROMOTIONAL
            SWBLatestSubscriptionOfferTypeWinback -> LatestSubscriptionOfferType.WINBACK
            else -> null
        }
    }

// ---------------------------------------------------------------------------
// Entitlements.
// ---------------------------------------------------------------------------

internal fun SWBEntitlement.toModel(): Entitlement =
    Entitlement(
        id = id,
        type = EntitlementType.SERVICE_LEVEL,
        isActive = isActive,
        productIds = productIds.mapNotNull { it as? String },
        latestProductId = latestProductId,
        store = productStoreFromNSNumber(store),
        startsAt = startsAt.toInstantFromEpochMs(),
        renewedAt = renewedAt.toInstantFromEpochMs(),
        expiresAt = expiresAt.toInstantFromEpochMs(),
        isLifetime = isLifetime?.boolValue,
        willRenew = willRenew?.boolValue,
        state = subscriptionStateFromNSNumber(state),
        offerType = offerTypeFromNSNumber(offerType),
    )

/**
 * Back-conversion for `setSubscriptionStatus`. The SWB bridge's own
 * back-conversion consumes only the entitlement id (matching the native
 * `Entitlement(id:)` initializer), but the full field set is carried for
 * envelope fidelity.
 */
internal fun Entitlement.toSWB(): SWBEntitlement =
    SWBEntitlement(
        id,
        SWBEntitlementTypeServiceLevel,
        isActive,
        productIds,
        latestProductId,
        store?.let { NSNumber(long = productStoreToSWB(it)) },
        startsAt?.toEpochMsNSNumber(),
        renewedAt?.toEpochMsNSNumber(),
        expiresAt?.toEpochMsNSNumber(),
        isLifetime?.let { NSNumber(bool = it) },
        willRenew?.let { NSNumber(bool = it) },
        state?.let { NSNumber(long = subscriptionStateToSWBRaw(it)) },
        offerType?.let { NSNumber(long = offerTypeToSWBRaw(it)) },
    )

private fun subscriptionStateToSWBRaw(state: LatestSubscriptionState): Long =
    when (state) {
        LatestSubscriptionState.IN_GRACE_PERIOD -> SWBLatestSubscriptionStateInGracePeriod
        LatestSubscriptionState.SUBSCRIBED -> SWBLatestSubscriptionStateSubscribed
        LatestSubscriptionState.EXPIRED -> SWBLatestSubscriptionStateExpired
        LatestSubscriptionState.IN_BILLING_RETRY_PERIOD -> SWBLatestSubscriptionStateInBillingRetryPeriod
        LatestSubscriptionState.REVOKED -> SWBLatestSubscriptionStateRevoked
    }

private fun offerTypeToSWBRaw(offerType: LatestSubscriptionOfferType): Long =
    when (offerType) {
        LatestSubscriptionOfferType.TRIAL -> SWBLatestSubscriptionOfferTypeTrial
        LatestSubscriptionOfferType.CODE -> SWBLatestSubscriptionOfferTypeCode
        LatestSubscriptionOfferType.PROMOTIONAL -> SWBLatestSubscriptionOfferTypePromotional
        LatestSubscriptionOfferType.WINBACK -> SWBLatestSubscriptionOfferTypeWinback
    }

internal fun SWBEntitlements.toModel(): Entitlements =
    Entitlements(
        active = active.toEntitlementSet(),
        inactive = inactive.toEntitlementSet(),
        all = all.toEntitlementSet(),
        web = web.toEntitlementSet(),
    )

internal fun List<*>.toEntitlementSet(): Set<Entitlement> =
    mapNotNull { (it as? SWBEntitlement)?.toModel() }.toSet()

// ---------------------------------------------------------------------------
// Subscription status (both directions).
// ---------------------------------------------------------------------------

internal fun SWBSubscriptionStatus.toModel(): SubscriptionStatus =
    when (status) {
        SWBSubscriptionStatusCaseActive -> SubscriptionStatus.Active(entitlements.toEntitlementSet())
        SWBSubscriptionStatusCaseInactive -> SubscriptionStatus.Inactive
        SWBSubscriptionStatusCaseUnknown -> SubscriptionStatus.Unknown
        // Unknown native case: documented fallback (plan §7).
        else -> SubscriptionStatus.Unknown
    }

internal fun SubscriptionStatus.toSWB(): SWBSubscriptionStatus =
    when (this) {
        is SubscriptionStatus.Active ->
            SWBSubscriptionStatus(SWBSubscriptionStatusCaseActive, entitlements.map { it.toSWB() })
        SubscriptionStatus.Inactive ->
            SWBSubscriptionStatus(SWBSubscriptionStatusCaseInactive, emptyList<SWBEntitlement>())
        SubscriptionStatus.Unknown ->
            SWBSubscriptionStatus(SWBSubscriptionStatusCaseUnknown, emptyList<SWBEntitlement>())
    }

// ---------------------------------------------------------------------------
// CustomerInfo family.
// ---------------------------------------------------------------------------

internal fun SWBCustomerInfo.toModel(): CustomerInfo =
    CustomerInfo(
        subscriptions = subscriptions.mapNotNull { (it as? SWBSubscriptionTransaction)?.toModel() },
        nonSubscriptions = nonSubscriptions.mapNotNull { (it as? SWBNonSubscriptionTransaction)?.toModel() },
        entitlements = entitlements.mapNotNull { (it as? SWBEntitlement)?.toModel() },
        userId = userId,
    )

internal fun SWBSubscriptionTransaction.toModel(): SubscriptionTransaction =
    SubscriptionTransaction(
        transactionId = transactionId,
        productId = productId,
        purchaseDate = Instant.fromEpochMilliseconds(purchaseDate),
        willRenew = willRenew,
        isRevoked = isRevoked,
        isInGracePeriod = isInGracePeriod,
        isInBillingRetryPeriod = isInBillingRetryPeriod,
        isActive = isActive,
        expirationDate = expirationDate.toInstantFromEpochMs(),
        offerType = offerTypeFromNSNumber(offerType),
        subscriptionGroupId = subscriptionGroupId,
        store = productStoreFromSWB(store),
    )

internal fun SWBNonSubscriptionTransaction.toModel(): NonSubscriptionTransaction =
    NonSubscriptionTransaction(
        transactionId = transactionId,
        productId = productId,
        purchaseDate = Instant.fromEpochMilliseconds(purchaseDate),
        isConsumable = isConsumable,
        isRevoked = isRevoked,
        store = productStoreFromSWB(store),
    )

// ---------------------------------------------------------------------------
// Integration attributes.
// ---------------------------------------------------------------------------

/**
 * Maps a common [IntegrationAttribute] to the SWB raw value. Every common
 * value has a SuperwallKit counterpart on iOS (including
 * `FIREBASE_INSTALLATION_ID` and `SINGULAR_DEVICE_ID`, which are Android-gaps
 * in the other direction).
 */
internal fun integrationAttributeToSWB(attribute: IntegrationAttribute): SWBIntegrationAttribute =
    when (attribute) {
        IntegrationAttribute.FIREBASE_INSTALLATION_ID -> SWBIntegrationAttributeFirebaseInstallationId
        IntegrationAttribute.SINGULAR_DEVICE_ID -> SWBIntegrationAttributeSingularDeviceId
        IntegrationAttribute.ADJUST_ID -> SWBIntegrationAttributeAdjustId
        IntegrationAttribute.AMPLITUDE_DEVICE_ID -> SWBIntegrationAttributeAmplitudeDeviceId
        IntegrationAttribute.AMPLITUDE_USER_ID -> SWBIntegrationAttributeAmplitudeUserId
        IntegrationAttribute.APPSFLYER_ID -> SWBIntegrationAttributeAppsflyerId
        IntegrationAttribute.BRAZE_ALIAS_NAME -> SWBIntegrationAttributeBrazeAliasName
        IntegrationAttribute.BRAZE_ALIAS_LABEL -> SWBIntegrationAttributeBrazeAliasLabel
        IntegrationAttribute.ONESIGNAL_ID -> SWBIntegrationAttributeOnesignalId
        IntegrationAttribute.FB_ANON_ID -> SWBIntegrationAttributeFbAnonId
        IntegrationAttribute.FIREBASE_APP_INSTANCE_ID -> SWBIntegrationAttributeFirebaseAppInstanceId
        IntegrationAttribute.ITERABLE_USER_ID -> SWBIntegrationAttributeIterableUserId
        IntegrationAttribute.ITERABLE_CAMPAIGN_ID -> SWBIntegrationAttributeIterableCampaignId
        IntegrationAttribute.ITERABLE_TEMPLATE_ID -> SWBIntegrationAttributeIterableTemplateId
        IntegrationAttribute.MIXPANEL_DISTINCT_ID -> SWBIntegrationAttributeMixpanelDistinctId
        IntegrationAttribute.MPARTICLE_ID -> SWBIntegrationAttributeMparticleId
        IntegrationAttribute.CLEVERTAP_ID -> SWBIntegrationAttributeClevertapId
        IntegrationAttribute.AIRSHIP_CHANNEL_ID -> SWBIntegrationAttributeAirshipChannelId
        IntegrationAttribute.KOCHAVA_DEVICE_ID -> SWBIntegrationAttributeKochavaDeviceId
        IntegrationAttribute.TENJIN_ID -> SWBIntegrationAttributeTenjinId
        IntegrationAttribute.POSTHOG_USER_ID -> SWBIntegrationAttributePosthogUserId
        IntegrationAttribute.CUSTOMERIO_ID -> SWBIntegrationAttributeCustomerioId
        IntegrationAttribute.APPSTACK_ID -> SWBIntegrationAttributeAppstackId
    }

/**
 * Maps a wire name from `getIntegrationAttributes()` (native camelCase or
 * snake_case) back to the common enum. Returns `null` for names without a
 * common counterpart — the caller skips them (degrade, never crash).
 */
internal fun integrationAttributeFromWireName(name: String): IntegrationAttribute? {
    val normalized = name.normalizedEnumKey()
    return IntegrationAttribute.entries.firstOrNull { it.name.normalizedEnumKey() == normalized }
}
