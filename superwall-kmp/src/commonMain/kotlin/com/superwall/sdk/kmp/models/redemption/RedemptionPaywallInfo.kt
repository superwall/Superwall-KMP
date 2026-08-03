package com.superwall.sdk.kmp.models.redemption

/**
 * Info about the paywall the redeemed purchase was made from.
 *
 * @property identifier The identifier of the paywall.
 * @property placementName The name of the placement.
 * @property placementParams The params of the placement.
 * @property variantId The ID of the paywall variant.
 * @property experimentId The ID of the experiment that the paywall belongs to.
 */
public data class RedemptionPaywallInfo(
    val identifier: String,
    val placementName: String,
    val placementParams: Map<String, Any?>,
    val variantId: String,
    val experimentId: String,
)
