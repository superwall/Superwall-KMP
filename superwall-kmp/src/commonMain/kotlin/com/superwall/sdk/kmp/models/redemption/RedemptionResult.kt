package com.superwall.sdk.kmp.models.redemption

/**
 * The result of redeeming a promotional/web code via Superwall.
 *
 * Delivered to `SuperwallDelegate.didRedeemLink` after a redemption attempt completes.
 */
public sealed interface RedemptionResult {
    /**
     * The code was redeemed successfully.
     *
     * @property code The code that was redeemed.
     * @property redemptionInfo Information about the redemption.
     */
    public data class Success(
        val code: String,
        val redemptionInfo: RedemptionInfo,
    ) : RedemptionResult

    /**
     * Redemption of the code failed with an error.
     *
     * @property code The code whose redemption failed.
     * @property error Info about the error that occurred.
     */
    public data class Error(
        val code: String,
        val error: ErrorInfo,
    ) : RedemptionResult

    /**
     * The code has expired.
     *
     * @property code The expired code.
     * @property info Info about the expired code.
     */
    public data class ExpiredCode(
        val code: String,
        val info: ExpiredCodeInfo,
    ) : RedemptionResult

    /**
     * The code is invalid.
     *
     * @property code The invalid code.
     */
    public data class InvalidCode(
        val code: String,
    ) : RedemptionResult

    /**
     * The subscription associated with the code has expired.
     *
     * @property code The code whose subscription has expired.
     * @property redemptionInfo Information about the redemption.
     */
    public data class ExpiredSubscription(
        val code: String,
        val redemptionInfo: RedemptionInfo,
    ) : RedemptionResult
}
