package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.PurchaseController
import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.Entitlements
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import com.superwall.sdk.kmp.models.identity.IdentityOptions
import com.superwall.sdk.kmp.models.options.ConfigurationStatus
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.redemption.RedemptionResult
import com.superwall.sdk.kmp.models.results.PresentationResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.triggers.ConfirmedAssignment

/**
 * The interface style the paywall UI should adopt.
 *
 * Internal for now: the bridge forwards it to the native SDKs; it is promoted
 * to a public model if/when the facade exposes an interface-style API.
 */
internal enum class InterfaceStyle {
    /** Force light mode. */
    LIGHT,

    /** Force dark mode. */
    DARK,

    /** Follow the system appearance. */
    AUTOMATIC,
}

/**
 * The callback seam through which native SDK delegate events flow up into
 * common code.
 *
 * A single implementation — the [DelegateMultiplexer] — is installed into the
 * platform bridge exactly once at configure (idempotently on repeated
 * configure calls) via [SuperwallBridge.installDelegate]. Platform adapters
 * map native payloads to common model types and invoke these methods on the
 * main thread.
 *
 * Method-for-method this mirrors [com.superwall.sdk.kmp.SuperwallDelegate];
 * the multiplexer forwards to the user's delegate (if set) and feeds the
 * [StreamHolder] flows. Purchase-controller calls do NOT flow through this
 * seam — the user's [PurchaseController] is handed to the platform bridge at
 * [SuperwallBridge.configure] and invoked directly by platform adapters.
 */
internal interface BridgeListener {
    /**
     * Whether anything downstream still consumes the PURE-FORWARDING hooks —
     * i.e. whether the app has a [com.superwall.sdk.kmp.SuperwallDelegate] set.
     *
     * Platform adapters check this BEFORE mapping a native payload and before
     * dispatching to the main thread, and skip the callback entirely when it is
     * `false`. This matters a lot on Android: superwall-android's `Logger`
     * invokes the native delegate's `handleLog` for EVERY internal log line
     * (ungated by log level, ~240 call sites), so an always-installed delegate
     * that maps-then-discards turns a `register` into hundreds of main-thread
     * dispatches and deep map copies. With no user delegate the native SDK's
     * own null-check is free; this restores that.
     *
     * The hooks that also feed the [StreamHolder] flows —
     * [subscriptionStatusDidChange] and [customerInfoDidChange] — must be
     * delivered regardless of this flag and are exempt.
     */
    val forwardsToUserDelegate: Boolean

    /** Native subscription status changed from [from] to [to]. */
    fun subscriptionStatusDidChange(
        from: SubscriptionStatus,
        to: SubscriptionStatus,
    )

    /** An internal analytics placement was tracked. */
    fun handleSuperwallEvent(eventInfo: SuperwallEventInfo)

    /** The user tapped a paywall element with a custom action named [name]. */
    fun handleCustomPaywallAction(name: String)

    /** A paywall is about to be dismissed. */
    fun willDismissPaywall(paywallInfo: PaywallInfo)

    /** A paywall is about to be presented. */
    fun willPresentPaywall(paywallInfo: PaywallInfo)

    /** A paywall was dismissed. */
    fun didDismissPaywall(paywallInfo: PaywallInfo)

    /** A paywall was presented. */
    fun didPresentPaywall(paywallInfo: PaywallInfo)

    /** The user opened a tagged URL from a paywall. */
    fun paywallWillOpenURL(url: String)

    /** The user tapped a deep link in a paywall. */
    fun paywallWillOpenDeepLink(url: String)

    /**
     * A log message was produced by the native SDK. Platform adapters map
     * unmappable native level/scope strings to [LogLevel.DEBUG] /
     * [LogScope.ALL], preserving the raw value under `info["rawLevel"]` /
     * `info["rawScope"]`.
     */
    fun handleLog(
        level: LogLevel,
        scope: LogScope,
        message: String?,
        info: Map<String, Any?>?,
        error: String?,
    )

    /** A web-paywall code redemption is about to start. */
    fun willRedeemLink()

    /** A web-paywall code redemption finished with [result]. */
    fun didRedeemLink(result: RedemptionResult)

    /**
     * A Superwall deep link (`yoursubdomain.superwall.app/app-link/...`) was
     * handled. iOS-sourced today; Android wiring pending upstream support.
     */
    fun handleSuperwallDeepLink(
        fullURL: String,
        pathComponents: List<String>,
        queryParameters: Map<String, String>,
    )

    /**
     * Customer info changed from [from] to [to]. iOS-sourced today; Android
     * wiring pending upstream support.
     */
    fun customerInfoDidChange(
        from: CustomerInfo,
        to: CustomerInfo,
    )

    /** User attributes changed to [newAttributes]. */
    fun userAttributesDidChange(newAttributes: Map<String, Any?>)
}

/**
 * The internal platform seam of the SDK — one member per Pigeon
 * `PSuperwallHostApi` method, taking and returning ONLY commonMain model
 * types.
 *
 * This interface plays exactly the role the Pigeon HostApi plus the two
 * native `SuperwallHost` classes play in the Flutter plugin, but as an
 * ordinary in-process Kotlin interface with real object references instead of
 * hostId routing. The public [com.superwall.sdk.kmp.Superwall] facade is a
 * thin layer over it: default arguments, the pre-configure guard, delegate
 * multiplexing, and stream ownership live in common code.
 *
 * Members are `suspend` exactly where the underlying native operation is
 * asynchronous; synchronous native properties stay synchronous. Failures from
 * the native layer surface as thrown
 * [com.superwall.sdk.kmp.SuperwallError.Native] except where the domain type
 * already models failure.
 */
internal interface SuperwallBridge {
    // ---- Configuration -----------------------------------------------------

    /**
     * Configures the native Superwall SDK.
     *
     * @param apiKey The Superwall API key.
     * @param purchaseController The user's purchase controller, invoked
     * directly by platform adapters, or `null` to let Superwall handle
     * purchases.
     * @param options Options to customize the SDK, already defaulted by the
     * facade.
     * @param completion Invoked with the real configuration outcome — Android
     * relays the native `Result<Unit>`; iOS derives it from the
     * post-completion configuration status inside the Swift bridge.
     */
    fun configure(
        apiKey: String,
        purchaseController: PurchaseController?,
        options: SuperwallOptions?,
        completion: (Result<Unit>) -> Unit,
    )

    /** Resets the `userId`, on-device paywall assignments, and stored data. */
    fun reset()

    /**
     * Installs the always-on [BridgeListener] (the [DelegateMultiplexer])
     * into the native SDK's delegate slot. Installed exactly once; repeated
     * calls are idempotent. Replaces Pigeon's `setDelegate(Boolean)`.
     */
    fun installDelegate(listener: BridgeListener)

    /** Whether [configure] has completed successfully. */
    fun isConfigured(): Boolean

    /** Whether the native SDK singleton has been initialized. */
    fun isInitialized(): Boolean

    /** The current configuration status of the native SDK. */
    fun getConfigurationStatus(): ConfigurationStatus

    /**
     * Attaches the native event sources to the common-owned flows in
     * [holder]: the native subscription-status stream feeds
     * [StreamHolder.subscriptionStatus] (plus an initial synchronous read to
     * close the seed gap) and, where available, customer-info changes feed
     * [StreamHolder.customerInfo]. Called once at configure-complete via
     * [StreamHolder.attach]; implementations may assume single invocation.
     */
    fun attachStreams(holder: StreamHolder)

    // ---- Logging -----------------------------------------------------------

    /** The current log level of the native SDK. */
    fun getLogLevel(): LogLevel

    /** Sets the log level of the native SDK. */
    fun setLogLevel(level: LogLevel)

    // ---- Identity / attributes ----------------------------------------------

    /** The current user's id, or the alias id if anonymous. */
    fun getUserId(): String

    /** Whether the user is logged in to Superwall via [identify]. */
    fun isLoggedIn(): Boolean

    /**
     * Links [userId] to Superwall's automatically generated alias.
     *
     * @param options Options controlling how identification is handled.
     */
    fun identify(
        userId: String,
        options: IdentityOptions?,
    )

    /** A snapshot of the current user's attributes. */
    fun getUserAttributes(): Map<String, Any?>

    /**
     * MERGES [attributes] into the existing user attributes (native
     * semantics on both platforms): a `null` value removes that key; absent
     * keys are left untouched.
     */
    fun setUserAttributes(attributes: Map<String, Any?>)

    /** Sets a single third-party integration [attribute] to [value] (`null` clears it). */
    fun setIntegrationAttribute(
        attribute: IntegrationAttribute,
        value: String?,
    )

    /** Sets multiple third-party integration attributes; `null` values clear their keys. */
    fun setIntegrationAttributes(attributes: Map<IntegrationAttribute, String?>)

    /** A snapshot of the currently set third-party integration attributes. */
    fun getIntegrationAttributes(): Map<IntegrationAttribute, String?>

    /** The device attributes Superwall tracks, e.g. for audience filters. */
    suspend fun getDeviceAttributes(): Map<String, Any?>

    /** The locale identifier override, or `null` when using the device locale. */
    fun getLocaleIdentifier(): String?

    /** Overrides the locale used by the SDK (`null` restores the device locale). */
    fun setLocaleIdentifier(localeIdentifier: String?)

    // ---- Entitlements / subscription ----------------------------------------

    /** A snapshot of the entitlements available to the user. */
    fun getEntitlements(): Entitlements

    /**
     * The entitlements associated with the given product ids, resolved by the
     * native SDK (Android local-filters with a logged warning until upstream
     * support exists).
     */
    suspend fun getEntitlementsByProductIds(productIds: Set<String>): Set<Entitlement>

    /** The current subscription status of the user. */
    fun getSubscriptionStatus(): SubscriptionStatus

    /** Sets the subscription status (used with a custom [PurchaseController]). */
    fun setSubscriptionStatus(status: SubscriptionStatus)

    /**
     * The latest subscription and entitlement info about the customer.
     * Android currently synthesizes a minimal value with a logged warning.
     */
    suspend fun getCustomerInfo(): CustomerInfo

    /** Confirms all unconfirmed experiment assignments and returns them. */
    suspend fun confirmAllAssignments(): Set<ConfirmedAssignment>

    /** Restores purchases via the native SDK (or the user's [PurchaseController]). */
    suspend fun restorePurchases(): RestorationResult

    // ---- Presentation --------------------------------------------------------

    /**
     * Registers [placement] to potentially present a paywall.
     *
     * Fire-and-forget, matching both natives. The per-call [handler] and
     * [feature] callbacks are retained by a fresh platform adapter for each
     * invocation and delivered on the main thread.
     */
    fun registerPlacement(
        placement: String,
        params: Map<String, Any?>?,
        handler: PaywallPresentationHandler?,
        feature: (() -> Unit)?,
    )

    /**
     * Previews the outcome of registering [placement] without presenting a
     * paywall.
     */
    suspend fun getPresentationResult(
        placement: String,
        params: Map<String, Any?>?,
    ): PresentationResult

    /** Dismisses the presented paywall, if one exists, resuming when done. */
    suspend fun dismiss()

    /** Whether a paywall is currently being presented. */
    fun isPaywallPresented(): Boolean

    /** Info about the most recently presented paywall, if any. */
    fun getLatestPaywallInfo(): PaywallInfo?

    /** Preloads all paywalls the user may see, per campaign audience filters. */
    fun preloadAllPaywalls()

    /** Preloads the paywalls the user may see for the given placement names. */
    fun preloadPaywallsForPlacements(placementNames: Set<String>)

    /**
     * Handles [url] as a deep link for previewing paywalls or redeeming web
     * codes. Callable before configure on both natives (guard-exempt in the
     * facade); returns whether the SDK handled it.
     */
    fun handleDeepLink(url: String): Boolean

    /** Toggles the loading spinner on the presented paywall. */
    fun togglePaywallSpinner(isHidden: Boolean)

    // ---- Products / misc ------------------------------------------------------

    /** The globally set product overrides by product name, if any. */
    fun getOverrideProductsByName(): Map<String, String>?

    /**
     * Globally overrides products by name for all paywalls (`null` clears
     * the overrides).
     */
    fun setOverrideProductsByName(overrideProducts: Map<String, String>?)

    /**
     * Sets the interface style the paywall UI adopts (`null` / [InterfaceStyle.AUTOMATIC]
     * follows the system appearance).
     */
    fun setInterfaceStyle(style: InterfaceStyle?)

    /**
     * Enables experimental device variables in audience filters. Actually
     * wired to the native SDKs (the equivalent Flutter option was dead).
     */
    fun enableExperimentalDeviceVariables(enabled: Boolean)

    /**
     * Consumes the Play Billing purchase identified by [purchaseToken] and
     * returns the token.
     *
     * @platform Android — iOS echoes the token back unchanged.
     */
    suspend fun consume(purchaseToken: String): String
}

/**
 * Creates the platform [SuperwallBridge] — the only `expect` declaration in
 * the module. Android returns `AndroidSuperwallBridge`; iOS returns
 * `IosSuperwallBridge` (a thin forwarder to the `SuperwallKMPBridge` Swift
 * facade).
 */
internal expect fun createSuperwallBridge(): SuperwallBridge
