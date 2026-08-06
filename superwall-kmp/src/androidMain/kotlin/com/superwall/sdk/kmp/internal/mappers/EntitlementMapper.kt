package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.Entitlements
import com.superwall.sdk.kmp.models.entitlements.EntitlementType
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionOfferType
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionState
import com.superwall.sdk.kmp.models.entitlements.NonSubscriptionTransaction
import com.superwall.sdk.kmp.models.entitlements.ProductStore
import com.superwall.sdk.kmp.models.entitlements.SubscriptionTransaction
import com.superwall.sdk.models.customer.CustomerInfo as NativeCustomerInfo
import com.superwall.sdk.models.customer.NonSubscriptionTransaction as NativeNonSubscriptionTransaction
import com.superwall.sdk.models.customer.SubscriptionTransaction as NativeSubscriptionTransaction
import com.superwall.sdk.models.entitlements.Entitlement as NativeEntitlement
import com.superwall.sdk.models.product.Store as NativeStore
import com.superwall.sdk.store.Entitlements as NativeEntitlements
import com.superwall.sdk.store.abstractions.product.receipt.LatestPeriodType as NativeLatestPeriodType
import com.superwall.sdk.store.abstractions.product.receipt.LatestSubscriptionState as NativeLatestSubscriptionState

/**
 * Entitlement-family mappers. Unlike the Flutter host (which flattened
 * entitlements to `{id, type}` and hardcoded `isActive = true`), these map the
 * FULL superwall-android 2.8.0 [NativeEntitlement] — all 13 device-enriched
 * fields — with epoch-millisecond `Date`s converted to [kotlin.time.Instant]
 *.
 */

// ---- Entitlement --------------------------------------------------------------

internal fun NativeEntitlement.toKmp(): Entitlement =
    Entitlement(
        id = id,
        type =
            when (type) {
                NativeEntitlement.Type.SERVICE_LEVEL -> EntitlementType.SERVICE_LEVEL
            },
        isActive = isActive,
        productIds = productIds.toList(),
        latestProductId = latestProductId,
        store = store?.toKmp(),
        startsAt = startsAt?.toKmpInstant(),
        renewedAt = renewedAt?.toKmpInstant(),
        expiresAt = expiresAt?.toKmpInstant(),
        isLifetime = isLifetime,
        willRenew = willRenew,
        state = state?.toKmp(),
        offerType = offerType?.toKmp(),
    )

internal fun Entitlement.toNative(): NativeEntitlement =
    NativeEntitlement(
        id = id,
        type =
            when (type) {
                EntitlementType.SERVICE_LEVEL -> NativeEntitlement.Type.SERVICE_LEVEL
            },
        isActive = isActive,
        productIds = productIds.toSet(),
        latestProductId = latestProductId,
        startsAt = startsAt?.toNativeDate(),
        renewedAt = renewedAt?.toNativeDate(),
        expiresAt = expiresAt?.toNativeDate(),
        isLifetime = isLifetime,
        willRenew = willRenew,
        state = state?.toNative(),
        offerType = offerType?.toNative(),
        store = store?.toNative(),
    )

// ---- Entitlements snapshot -----------------------------------------------------

/**
 * Snapshots the live native [NativeEntitlements] object into the common
 * pure-value [Entitlements] (the common type never carries a live
 * SDK reference).
 */
internal fun NativeEntitlements.toKmp(): Entitlements =
    Entitlements(
        active = active.map { it.toKmp() }.toSet(),
        inactive = inactive.map { it.toKmp() }.toSet(),
        all = all.map { it.toKmp() }.toSet(),
        web = web.map { it.toKmp() }.toSet(),
    )

// ---- CustomerInfo --------------------------------------------------------------

internal fun NativeCustomerInfo.toKmp(): CustomerInfo =
    CustomerInfo(
        subscriptions = subscriptions.map { it.toKmp() },
        nonSubscriptions = nonSubscriptions.map { it.toKmp() },
        entitlements = entitlements.map { it.toKmp() },
        userId = userId,
    )

internal fun NativeSubscriptionTransaction.toKmp(): SubscriptionTransaction =
    SubscriptionTransaction(
        transactionId = transactionId,
        productId = productId,
        purchaseDate = purchaseDate.toKmpInstant(),
        willRenew = willRenew,
        isRevoked = isRevoked,
        isInGracePeriod = isInGracePeriod,
        isInBillingRetryPeriod = isInBillingRetryPeriod,
        isActive = isActive,
        expirationDate = expirationDate?.toKmpInstant(),
        offerType = offerType?.toKmp(),
        subscriptionGroupId = subscriptionGroupId,
        store = store.toKmp(),
    )

internal fun NativeNonSubscriptionTransaction.toKmp(): NonSubscriptionTransaction =
    NonSubscriptionTransaction(
        transactionId = transactionId,
        productId = productId,
        purchaseDate = purchaseDate.toKmpInstant(),
        isConsumable = isConsumable,
        isRevoked = isRevoked,
        store = store.toKmp(),
    )

// ---- Store / ProductStore -------------------------------------------------------

internal fun NativeStore.toKmp(): ProductStore =
    when (this) {
        NativeStore.PLAY_STORE -> ProductStore.PLAY_STORE
        NativeStore.APP_STORE -> ProductStore.APP_STORE
        NativeStore.STRIPE -> ProductStore.STRIPE
        NativeStore.PADDLE -> ProductStore.PADDLE
        NativeStore.SUPERWALL -> ProductStore.SUPERWALL
        NativeStore.CUSTOM -> ProductStore.CUSTOM
        NativeStore.OTHER -> ProductStore.OTHER
    }

internal fun ProductStore.toNative(): NativeStore =
    when (this) {
        ProductStore.PLAY_STORE -> NativeStore.PLAY_STORE
        ProductStore.APP_STORE -> NativeStore.APP_STORE
        ProductStore.STRIPE -> NativeStore.STRIPE
        ProductStore.PADDLE -> NativeStore.PADDLE
        ProductStore.SUPERWALL -> NativeStore.SUPERWALL
        ProductStore.CUSTOM -> NativeStore.CUSTOM
        ProductStore.OTHER -> NativeStore.OTHER
    }

// ---- LatestSubscriptionState ------------------------------------------------------

/**
 * Native `UNKNOWN` maps to `null` — the common enum has no unknown case; a
 * `null` state already means "not determinable" (degrade, never crash).
 */
internal fun NativeLatestSubscriptionState.toKmp(): LatestSubscriptionState? =
    when (this) {
        NativeLatestSubscriptionState.GRACE_PERIOD -> LatestSubscriptionState.IN_GRACE_PERIOD
        NativeLatestSubscriptionState.EXPIRED -> LatestSubscriptionState.EXPIRED
        NativeLatestSubscriptionState.SUBSCRIBED -> LatestSubscriptionState.SUBSCRIBED
        NativeLatestSubscriptionState.BILLING_RETRY -> LatestSubscriptionState.IN_BILLING_RETRY_PERIOD
        NativeLatestSubscriptionState.REVOKED -> LatestSubscriptionState.REVOKED
        NativeLatestSubscriptionState.UNKNOWN -> null
    }

internal fun LatestSubscriptionState.toNative(): NativeLatestSubscriptionState =
    when (this) {
        LatestSubscriptionState.IN_GRACE_PERIOD -> NativeLatestSubscriptionState.GRACE_PERIOD
        LatestSubscriptionState.EXPIRED -> NativeLatestSubscriptionState.EXPIRED
        LatestSubscriptionState.SUBSCRIBED -> NativeLatestSubscriptionState.SUBSCRIBED
        LatestSubscriptionState.IN_BILLING_RETRY_PERIOD -> NativeLatestSubscriptionState.BILLING_RETRY
        LatestSubscriptionState.REVOKED -> NativeLatestSubscriptionState.REVOKED
    }

// ---- LatestPeriodType / LatestSubscriptionOfferType ----------------------------------

/**
 * The native `LatestPeriodType` also carries the non-offer cases
 * `SUBSCRIPTION` and `REVOKED`; the common [LatestSubscriptionOfferType] models
 * offers only, so those map to `null` ("no offer applied").
 */
internal fun NativeLatestPeriodType.toKmp(): LatestSubscriptionOfferType? =
    when (this) {
        NativeLatestPeriodType.TRIAL -> LatestSubscriptionOfferType.TRIAL
        NativeLatestPeriodType.CODE -> LatestSubscriptionOfferType.CODE
        NativeLatestPeriodType.PROMOTIONAL -> LatestSubscriptionOfferType.PROMOTIONAL
        NativeLatestPeriodType.WINBACK -> LatestSubscriptionOfferType.WINBACK
        NativeLatestPeriodType.SUBSCRIPTION -> null
        NativeLatestPeriodType.REVOKED -> null
    }

internal fun LatestSubscriptionOfferType.toNative(): NativeLatestPeriodType =
    when (this) {
        LatestSubscriptionOfferType.TRIAL -> NativeLatestPeriodType.TRIAL
        LatestSubscriptionOfferType.CODE -> NativeLatestPeriodType.CODE
        LatestSubscriptionOfferType.PROMOTIONAL -> NativeLatestPeriodType.PROMOTIONAL
        LatestSubscriptionOfferType.WINBACK -> NativeLatestPeriodType.WINBACK
    }
