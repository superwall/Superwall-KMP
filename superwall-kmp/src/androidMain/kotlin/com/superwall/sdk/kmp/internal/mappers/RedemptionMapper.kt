package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.redemption.ErrorInfo
import com.superwall.sdk.kmp.models.redemption.ExpiredCodeInfo
import com.superwall.sdk.kmp.models.redemption.Ownership
import com.superwall.sdk.kmp.models.redemption.PurchaserInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionPaywallInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionResult
import com.superwall.sdk.kmp.models.redemption.StoreIdentifiers
import com.superwall.sdk.models.internal.ErrorInfo as NativeErrorInfo
import com.superwall.sdk.models.internal.ExpiredInfo as NativeExpiredInfo
import com.superwall.sdk.models.internal.PurchaserInfo as NativePurchaserInfo
import com.superwall.sdk.models.internal.RedemptionInfo as NativeRedemptionInfo
import com.superwall.sdk.models.internal.RedemptionOwnership as NativeRedemptionOwnership
import com.superwall.sdk.models.internal.RedemptionResult as NativeRedemptionResult
import com.superwall.sdk.models.internal.StoreIdentifiers as NativeStoreIdentifiers

/**
 * Maps the native web-redemption family (models/internal/WebRedemption.kt) to
 * the common redemption models. Port of the Flutter host's
 * utils/RedemptionResultMapper.kt; the `JsonElement`-typed
 * `placementParams`/`properties` payloads are normalized through
 * [sanitizeAny]/[sanitizeParams], and entitlements keep their full native
 * payload (via `Entitlement.toKmp`) instead of the Flutter host's
 * `{id, SERVICE_LEVEL}` flattening.
 */
internal fun NativeRedemptionResult.toKmp(): RedemptionResult =
    when (this) {
        is NativeRedemptionResult.Success ->
            RedemptionResult.Success(
                code = code,
                redemptionInfo = redemptionInfo.toKmp(),
            )
        is NativeRedemptionResult.Error ->
            RedemptionResult.Error(
                code = code,
                error = error.toKmp(),
            )
        is NativeRedemptionResult.Expired ->
            RedemptionResult.ExpiredCode(
                code = code,
                info = expired.toKmp(),
            )
        is NativeRedemptionResult.InvalidCode ->
            RedemptionResult.InvalidCode(
                code = code,
            )
        is NativeRedemptionResult.ExpiredSubscription ->
            RedemptionResult.ExpiredSubscription(
                code = code,
                redemptionInfo = redemptionInfo.toKmp(),
            )
    }

internal fun NativeRedemptionInfo.toKmp(): RedemptionInfo =
    RedemptionInfo(
        ownership = ownership.toKmp(),
        purchaserInfo = purchaserInfo.toKmp(),
        paywallInfo = paywallInfo?.toKmp(),
        entitlements = entitlements.map { it.toKmp() }.toSet(),
    )

internal fun NativeRedemptionOwnership.toKmp(): Ownership =
    when (this) {
        is NativeRedemptionOwnership.AppUser -> Ownership.AppUser(appUserId = appUserId)
        is NativeRedemptionOwnership.Device -> Ownership.Device(deviceId = deviceId)
    }

internal fun NativePurchaserInfo.toKmp(): PurchaserInfo =
    PurchaserInfo(
        appUserId = appUserId,
        email = email,
        storeIdentifiers = storeIdentifiers.toKmp(),
    )

internal fun NativeStoreIdentifiers.toKmp(): StoreIdentifiers =
    when (this) {
        is NativeStoreIdentifiers.Stripe ->
            StoreIdentifiers.Stripe(
                customerId = stripeCustomerId,
                subscriptionIds = subscriptionIds,
            )
        is NativeStoreIdentifiers.Paddle ->
            StoreIdentifiers.Paddle(
                customerId = paddleCustomerId,
                subscriptionIds = paddleSubscriptionIds,
            )
        is NativeStoreIdentifiers.Unknown ->
            StoreIdentifiers.Unknown(
                // The native Unknown case carries no store name outside its
                // properties bag — "UNKNOWN" matches the Flutter host.
                store = "UNKNOWN",
                additionalInfo = sanitizeParams(properties).orEmpty(),
            )
    }

internal fun NativeRedemptionResult.PaywallInfo.toKmp(): RedemptionPaywallInfo =
    RedemptionPaywallInfo(
        identifier = identifier,
        placementName = placementName,
        placementParams = sanitizeParams(placementParams).orEmpty(),
        variantId = variantId,
        experimentId = experimentId,
    )

internal fun NativeErrorInfo.toKmp(): ErrorInfo = ErrorInfo(message = message)

internal fun NativeExpiredInfo.toKmp(): ExpiredCodeInfo =
    ExpiredCodeInfo(
        resent = resent,
        obfuscatedEmail = obfuscatedEmail,
    )
