package com.superwall.sdk.kmp.models.options

import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration

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
     *
     * On Android, superwall-android only sees purchases the app reports to it
     * through its observer APIs (or its billing-flow wrapper), which this SDK
     * does not expose yet; the option alone reports nothing there.
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
     * The user's [AdConsent], if the app already knows it at configure. `null`
     * (the default) leaves it unset, so the SDK falls back to a TCF consent
     * banner's stored answer. Change it later with `Superwall.setAdConsent`.
     */
    val adConsent: AdConsent? = null,
    /**
     * The Customer Center configuration `Superwall.presentCustomerCenter`
     * uses when it isn't given one. `null` (the default) keeps the native
     * default, [CustomerCenterConfiguration.default].
     */
    val customerCenter: CustomerCenterConfiguration? = null,
    /**
     * The StoreKit version the SDK uses. `null` (the default) lets
     * SuperwallKit choose: StoreKit 2, unless the app opts into consumable
     * purchase history on iOS versions before 18. iOS only; has no effect on
     * Android.
     */
    val storeKitVersion: StoreKitVersion? = null,
    /**
     * Local files a paywall can load instead of fetching them over the
     * network, keyed by the local resource id the paywall references
     * (`swlocal://<id>`).
     */
    val localResources: Map<String, PaywallResource> = emptyMap(),
    /**
     * Connects the SDK to a running `superwall dev` server, for development
     * builds only: paywalls with a local counterpart render from your local
     * paywall code. It also turns on test mode and turns off preloading. iOS
     * only; superwall-android has no dev server support and ignores it.
     */
    val devServer: DevServer? = null,
    /**
     * A custom network environment, used instead of [networkEnvironment] when
     * set. Only set this if told so explicitly by the Superwall team.
     */
    val customNetworkEnvironment: CustomNetworkEnvironment? = null,
)

/** A local file served to paywalls through [SuperwallOptions.localResources]. */
public sealed interface PaywallResource {
    /**
     * A file addressed by a URI string: a `file://` URL on iOS, and a
     * `file://`, `content://` or other URI Android can open.
     */
    public data class FromUri(val uri: String) : PaywallResource

    /**
     * An Android resource, e.g. `R.raw.hero_video` or `R.drawable.bg`. Android
     * only: iOS has no resource ids and leaves the entry out.
     */
    public data class FromAndroidResource(val resourceId: Int) : PaywallResource
}

/** A running `superwall dev` server for [SuperwallOptions.devServer] to connect to. */
public sealed interface DevServer {
    /**
     * Finds the dev server on `localhost` ports 6100 to 6104, which reaches a
     * server running on the same machine from a simulator.
     */
    public data object Default : DevServer

    /**
     * The dev server at an exact address: the `Device` URL's origin that
     * `superwall dev` prints, e.g. `http://192.168.1.10:6100`. Use this on a
     * physical device.
     */
    public data class Url(val url: String) : DevServer
}

/**
 * A network environment other than the ones [NetworkEnvironment] names.
 *
 * iOS builds the address from [scheme], [baseHost] and [port] and uses
 * Superwall's developer collector whatever [collectorHost] says; Android uses
 * all four.
 */
public data class CustomNetworkEnvironment(
    /** The host of the Superwall API, without a scheme or port. */
    val baseHost: String,
    /** The host events are sent to. Android only. */
    val collectorHost: String = "collector.superwall.dev",
    /** `https` or `http`. */
    val scheme: String = "https",
    /** The port, or `null` for the scheme's default. */
    val port: Int? = null,
)

/** The StoreKit version the iOS SDK uses for purchases. */
public enum class StoreKitVersion {
    /** StoreKit 1. */
    STOREKIT1,

    /** StoreKit 2. */
    STOREKIT2,
}
