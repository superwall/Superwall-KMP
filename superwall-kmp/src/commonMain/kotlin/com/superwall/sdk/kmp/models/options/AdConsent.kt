package com.superwall.sdk.kmp.models.options

/**
 * Whether the user agreed to one use of their data for advertising.
 */
public enum class AdConsentStatus {
    /** The user agreed. */
    GRANTED,

    /** The user declined. */
    DENIED,
}

/**
 * The user's consent for how their data is used for advertising, which
 * Superwall passes on with the conversions it uploads to Google Ads and Meta.
 *
 * Until the app sets one, with `SuperwallOptions.adConsent` or
 * `Superwall.setAdConsent`, the native SDKs use the consent stored by an IAB
 * TCF consent banner when GDPR applies, and otherwise treat both as granted.
 * Apps with users in the EEA, UK or Switzerland that don't use a TCF banner
 * should set it from their consent flow. Both are reported as denied while
 * `Superwall.eventTrackingBehavior` is [EventTrackingBehavior.NONE]; on iOS,
 * personalization is also reported as denied when the user hasn't allowed
 * tracking, unless the app set it.
 */
public data class AdConsent(
    /** Consent to sending the user's data to Google and Meta for advertising. */
    val adUserData: AdConsentStatus = AdConsentStatus.GRANTED,
    /** Consent to personalized advertising. */
    val adPersonalization: AdConsentStatus = AdConsentStatus.GRANTED,
)
