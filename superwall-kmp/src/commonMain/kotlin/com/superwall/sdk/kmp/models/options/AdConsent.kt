package com.superwall.sdk.kmp.models.options

/**
 * Whether a user has granted or denied consent for an advertising purpose.
 */
public enum class AdConsentStatus {
    /** The user has granted consent. This is the default. */
    GRANTED,

    /** The user has denied consent. */
    DENIED,
}

/**
 * A user's consent for how their data is used for advertising, which
 * Superwall passes on with the conversions it uploads to Google Ads and Meta.
 *
 * Set it at configure time via [SuperwallOptions.adConsent] or at runtime via
 * `Superwall.adConsent`.
 *
 * @platform iOS — superwall-android does not support ad consent yet.
 * @property adUserData Consent to send user data to ad networks for advertising.
 * @property adPersonalization Consent for them to use that data for
 * personalized advertising.
 */
public data class AdConsent(
    val adUserData: AdConsentStatus = AdConsentStatus.GRANTED,
    val adPersonalization: AdConsentStatus = AdConsentStatus.GRANTED,
)
