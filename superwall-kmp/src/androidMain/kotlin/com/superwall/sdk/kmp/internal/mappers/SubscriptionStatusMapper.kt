package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.entitlements.SubscriptionStatus as NativeSubscriptionStatus

/**
 * Maps subscription status in both directions. Port of the Flutter host's
 * utils/SubscriptionStatusMapper.kt, upgraded to carry full entitlement
 * payloads (via [toKmp]/[toNative] on `Entitlement`) instead of the Flutter
 * host's `{id, SERVICE_LEVEL, isActive=true}` flattening.
 */
internal fun NativeSubscriptionStatus.toKmp(): SubscriptionStatus =
    when (this) {
        is NativeSubscriptionStatus.Active ->
            SubscriptionStatus.Active(entitlements.map { it.toKmp() }.toSet())
        is NativeSubscriptionStatus.Inactive -> SubscriptionStatus.Inactive
        is NativeSubscriptionStatus.Unknown -> SubscriptionStatus.Unknown
    }

internal fun SubscriptionStatus.toNative(): NativeSubscriptionStatus =
    when (this) {
        is SubscriptionStatus.Active ->
            NativeSubscriptionStatus.Active(entitlements.map { it.toNative() }.toSet())
        is SubscriptionStatus.Inactive -> NativeSubscriptionStatus.Inactive
        is SubscriptionStatus.Unknown -> NativeSubscriptionStatus.Unknown
    }
