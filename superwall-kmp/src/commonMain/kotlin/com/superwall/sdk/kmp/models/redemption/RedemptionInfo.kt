package com.superwall.sdk.kmp.models.redemption

import com.superwall.sdk.kmp.models.entitlements.Entitlement

/**
 * Information about a code redemption.
 *
 * @property ownership The ownership of the code.
 * @property purchaserInfo Info about the purchaser.
 * @property paywallInfo Info about the paywall the purchase was made from, if any.
 * @property entitlements The entitlements granted by the redemption.
 * @property userAttributes The answers collected on the web paywall funnel that
 * led to the purchase, merged across every checkout the code redeems. Values
 * are strings, numbers, booleans, `null` for an answer left blank, or lists of
 * those. `null` when the funnel collected none. Always `null` on Android,
 * which does not report them yet.
 */
public data class RedemptionInfo(
    val ownership: Ownership,
    val purchaserInfo: PurchaserInfo,
    val paywallInfo: RedemptionPaywallInfo? = null,
    val entitlements: Set<Entitlement>,
    val userAttributes: Map<String, Any?>? = null,
)
