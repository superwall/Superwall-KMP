package com.superwall.sdk.kmp.models.options

/**
 * Options for configuring Superwall, including paywall presentation and
 * appearance.
 */
public data class SuperwallOptions(
    /** Configures the appearance and behaviour of paywalls. */
    val paywalls: PaywallOptions = PaywallOptions(),
    /**
     * Determines which network environment your SDK should use.
     * Defaults to [NetworkEnvironment.RELEASE]. You should under no
     * circumstance change this unless you received the go-ahead from the
     * Superwall team.
     */
    val networkEnvironment: NetworkEnvironment = NetworkEnvironment.RELEASE,
    /**
     * Enables the sending of non-Superwall tracked events and properties back
     * to the Superwall servers. Defaults to `true`.
     */
    val isExternalDataCollectionEnabled: Boolean = true,
    /**
     * Controls which events are sent to the Superwall servers. Defaults to
     * [EventTrackingBehavior.ALL]. [EventTrackingBehavior.SUPERWALL_ONLY] is
     * the typed replacement for `isExternalDataCollectionEnabled = false`.
     */
    val eventTrackingBehavior: EventTrackingBehavior = EventTrackingBehavior.ALL,
    /** Sets the device locale identifier to use when evaluating rules. */
    val localeIdentifier: String? = null,
    /**
     * Forwards events from the game controller to the paywall.
     * Defaults to `false`.
     */
    val isGameControllerEnabled: Boolean = false,
    /**
     * Enables experimental device variables for use in audience filters and
     * paywalls. Defaults to `false`.
     */
    val enableExperimentalDeviceVariables: Boolean = false,
    /** The log scope and level to print to the console. */
    val logging: Logging = Logging(),
    /**
     * Enables passing identifiers to the Play Store as AccountIds.
     * Defaults to `false`. Android only.
     */
    val passIdentifiersToPlayStore: Boolean = false,
    /**
     * Controls when the SDK enters test mode.
     * Defaults to [TestModeBehavior.AUTOMATIC].
     */
    val testModeBehavior: TestModeBehavior = TestModeBehavior.AUTOMATIC,
    /**
     * Observe purchases made outside of Superwall. When `true`, Superwall
     * will observe StoreKit/Play Store transactions and report them.
     * Defaults to `false`.
     */
    val shouldObservePurchases: Boolean = false,
    /**
     * Disables the app transaction check on SDK launch. Defaults to `false`.
     * iOS only.
     */
    val shouldBypassAppTransactionCheck: Boolean = false,
    /**
     * Number of times the SDK will attempt to get the Superwall configuration
     * after a network failure before it times out. Defaults to `6`. iOS only.
     */
    val maxConfigRetryCount: Int = 6,
    /**
     * Enable mock review functionality. Defaults to `false`. Android only.
     */
    val useMockReviews: Boolean = false,
    /**
     * The user's consent for how their data is used for advertising, passed on
     * with the conversions Superwall uploads to Google Ads and Meta. You can
     * also change it at runtime via `Superwall.adConsent`.
     *
     * Leave it `null` unless you collect consent yourself: while it has never
     * been set, the SDK uses the consent stored by an IAB TCF consent banner
     * when GDPR applies, and otherwise defaults to granted. Setting it — even
     * to the granted defaults — replaces that fallback. Apps with users in the
     * EEA, UK or Switzerland that don't use a TCF banner should set it from
     * their consent flow. Defaults to `null`. iOS only.
     */
    val adConsent: AdConsent? = null,
    /**
     * Connects to a running `superwall dev` server so paywalls with a local
     * counterpart render from your live, local paywall code, while
     * configuration, placements, audience evaluation and assignment stay real.
     * It also activates test mode and disables paywall preloading. Use
     * [DevServer.Default] on a simulator and [DevServer.Url] on a physical
     * device; the app must allow local networking
     * (`NSAppTransportSecurity` → `NSAllowsLocalNetworking`).
     *
     * For development builds only. Defaults to `null`: no dev server. iOS only.
     */
    val devServer: DevServer? = null,
)
