package com.superwall.sdk.kmp.models.customercenter

import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.NonSubscriptionTransaction
import com.superwall.sdk.kmp.models.entitlements.ProductStore
import com.superwall.sdk.kmp.models.entitlements.SubscriptionTransaction

/**
 * The action of a Customer Center path the user tapped, reported to
 * [com.superwall.sdk.kmp.CustomerCenterDelegate].
 */
public sealed interface CustomerCenterAction {
    /** Restore purchases. */
    public data object Restore : CustomerCenterAction

    /** Manage the subscription in the store. */
    public data object ManageSubscription : CustomerCenterAction

    /** Request a refund. */
    public data object Refund : CustomerCenterAction

    /** Change plan. */
    public data object ChangePlan : CustomerCenterAction

    /** Contact support. */
    public data object ContactSupport : CustomerCenterAction

    /** Open [url]. */
    public data class Url(val url: String) : CustomerCenterAction

    /** A custom path with the [identifier] it was configured with. */
    public data class Custom(val identifier: String) : CustomerCenterAction
}

/** The outcome of a refund request made from the Customer Center. */
public enum class CustomerCenterRefundStatus {
    /** The request was submitted. */
    SUCCESS,

    /** The user cancelled the request. */
    USER_CANCELLED,

    /** The request failed. */
    ERROR,
}

/** A purchase shown in the Customer Center, which a path's action applies to. */
public data class CustomerCenterPurchase(
    /** The product's identifier, when known. */
    val productId: String?,
    /** The store the purchase was made in. */
    val store: ProductStore,
    /** The entitlements the purchase grants. */
    val entitlements: Set<Entitlement>,
    /** The subscription, when the purchase is one. */
    val subscription: SubscriptionTransaction? = null,
    /** The one-time purchase, when the purchase is one. */
    val nonSubscription: NonSubscriptionTransaction? = null,
)
