package com.superwall.sdk.kmp.models.store

import kotlin.time.Instant

/**
 * An introductory or promotional offer of an App Store product. iOS only:
 * superwall-android exposes Play offers through the product's price and trial
 * fields instead.
 */
public data class StoreProductDiscount(
    /** The offer's identifier; `null` for an introductory offer. */
    val offerIdentifier: String? = null,
    /** The currency code of the discounted price (e.g. `USD`). */
    val currencyCode: String? = null,
    /** The discounted price in the local currency. */
    val price: Double,
    /** The discounted price, localized and formatted. */
    val localizedPriceString: String,
    /** How the discounted price is charged. */
    val paymentMode: PaymentMode,
    /** The length of one discounted period. */
    val subscriptionPeriod: SubscriptionPeriod,
    /** How many periods the discount lasts. */
    val numberOfPeriods: Int,
    /** Whether this is the introductory offer or a promotional one. */
    val type: DiscountType,
) {
    /** How a discounted price is charged. */
    public enum class PaymentMode {
        /** The discounted price is charged every period. */
        PAY_AS_YOU_GO,

        /** The discounted price is charged once, for all periods. */
        PAY_UP_FRONT,

        /** Nothing is charged during the discount. */
        FREE_TRIAL,
    }

    /** The kind of discount. */
    public enum class DiscountType {
        INTRODUCTORY,
        PROMOTIONAL,
    }
}

/**
 * The App Store billing plan of a subscription with more than one (iOS 26.4+).
 * iOS only.
 */
public enum class BillingPlanType {
    /** The whole period is paid for at once. */
    UP_FRONT,

    /** The period is paid for in monthly instalments. */
    MONTHLY,
}

/**
 * A token that overrides Apple's own introductory offer eligibility check
 * (iOS 18.2+). Pass [token] to StoreKit 2's
 * `introductoryOfferEligibility(compactJWS:)` purchase option from your
 * `PurchaseController`. iOS only.
 */
public data class IntroOfferToken(
    /** The JWS string to pass to StoreKit. */
    val token: String,
    /** When the token stops being valid. */
    val expiry: Instant,
)
