package com.superwall.sdk.kmp.models.events

/**
 * Attributes for third-party integrations with Superwall.
 *
 * Set via `Superwall.setIntegrationAttribute`/`setIntegrationAttributes`.
 */
public enum class IntegrationAttribute {
    /** The unique Adjust identifier for the user. */
    ADJUST_ID,

    /** The Amplitude device identifier. */
    AMPLITUDE_DEVICE_ID,

    /** The Amplitude user identifier. */
    AMPLITUDE_USER_ID,

    /** The unique AppsFlyer identifier for the user. */
    APPSFLYER_ID,

    /** The Braze `alias_name` in User Alias Object. */
    BRAZE_ALIAS_NAME,

    /** The Braze `alias_label` in User Alias Object. */
    BRAZE_ALIAS_LABEL,

    /** The OneSignal Player identifier for the user. */
    ONESIGNAL_ID,

    /** The Facebook Anonymous identifier for the user. */
    FB_ANON_ID,

    /** The Firebase instance identifier. */
    FIREBASE_APP_INSTANCE_ID,

    /**
     * The Firebase installation identifier (FID).
     *
     * @platform iOS-only: superwall-android has no counterpart; setting it on
     * Android is skipped with a logged warning.
     */
    FIREBASE_INSTALLATION_ID,

    /** The Singular Device ID (SDID). */
    SINGULAR_DEVICE_ID,

    /** The Iterable identifier for the user. */
    ITERABLE_USER_ID,

    /** The Iterable campaign identifier. */
    ITERABLE_CAMPAIGN_ID,

    /** The Iterable template identifier. */
    ITERABLE_TEMPLATE_ID,

    /** The Mixpanel user identifier. */
    MIXPANEL_DISTINCT_ID,

    /** The unique mParticle user identifier (mpid). */
    MPARTICLE_ID,

    /** The CleverTap user identifier. */
    CLEVERTAP_ID,

    /** The Airship channel identifier for the user. */
    AIRSHIP_CHANNEL_ID,

    /** The unique Kochava device identifier. */
    KOCHAVA_DEVICE_ID,

    /** The Tenjin identifier. */
    TENJIN_ID,

    /** The PostHog user identifier. */
    POSTHOG_USER_ID,

    /** The Customer.io person's identifier (`id`). */
    CUSTOMERIO_ID,

    /** The Appstack identifier. */
    APPSTACK_ID,
}
