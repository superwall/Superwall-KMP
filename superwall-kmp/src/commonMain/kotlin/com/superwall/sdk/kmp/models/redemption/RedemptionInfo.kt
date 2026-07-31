package com.superwall.sdk.kmp.models.redemption

import com.superwall.sdk.kmp.models.entitlements.Entitlement

/**
 * Information about a code redemption.
 *
 * @property ownership The ownership of the code.
 * @property purchaserInfo Info about the purchaser.
 * @property paywallInfo Info about the paywall the purchase was made from, if any.
 * @property entitlements The entitlements granted by the redemption.
 */
public data class RedemptionInfo(
    val ownership: Ownership,
    val purchaserInfo: PurchaserInfo,
    val paywallInfo: RedemptionPaywallInfo? = null,
    val entitlements: Set<Entitlement>,
)
