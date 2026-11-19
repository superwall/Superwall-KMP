package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.customercenter.CustomerCenterAction
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterPurchase
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterRefundStatus

/**
 * Receives Customer Center events. Pass one to
 * [Superwall.presentCustomerCenter]; it is kept until the Customer Center is
 * dismissed. Every method has a default implementation, and all are called
 * on the main thread.
 *
 * Exceptions thrown by an implementation are caught and reported to
 * `SuperwallDelegate.handleLog`.
 */
public interface CustomerCenterDelegate {
    /**
     * Called before purchases are restored. Return `false` to cancel, e.g.
     * after the user declines to sign in. The restore waits for the result.
     * Defaults to `true`.
     */
    public suspend fun customerCenterShouldRestorePurchases(): Boolean = true

    /**
     * Called when the user taps a path, including custom and URL paths,
     * before its action runs.
     *
     * @param action What the path does.
     * @param pathId The tapped path's id.
     * @param purchase The purchase the action applies to, or `null` for a
     * screen-level action such as restore.
     */
    public fun customerCenterDidSelectAction(
        action: CustomerCenterAction,
        pathId: String,
        purchase: CustomerCenterPurchase?,
    ) {}

    /** Called when the user answers a survey attached to the path [pathId]. */
    public fun customerCenterDidCompleteSurvey(
        surveyId: String,
        optionId: String,
        action: CustomerCenterAction,
        pathId: String,
    ) {}

    /** Called when a refund request for [productId] finishes with [status]. */
    public fun customerCenterDidCompleteRefundRequest(
        productId: String,
        status: CustomerCenterRefundStatus,
    ) {}

    /** Called when the Customer Center is dismissed. */
    public fun customerCenterDidDismiss() {}
}
