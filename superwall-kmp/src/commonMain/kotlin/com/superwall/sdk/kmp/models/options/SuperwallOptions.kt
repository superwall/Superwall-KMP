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
)
