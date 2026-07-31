package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.internal.DelegateMultiplexer
import com.superwall.sdk.kmp.internal.StreamHolder
import com.superwall.sdk.kmp.internal.SuperwallBridge
import com.superwall.sdk.kmp.internal.createSuperwallBridge
import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.Entitlements
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import com.superwall.sdk.kmp.models.identity.IdentityOptions
import com.superwall.sdk.kmp.models.options.ConfigurationStatus
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PresentationResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.triggers.ConfirmedAssignment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The primary class for integrating Superwall into your application.
 *
 * Call [configure] (or [configureAndAwait]) as early as possible in your app's
 * lifecycle, then use the instance members to identify users, register
 * placements, and observe subscription state. The API is identical on Android
 * and iOS — no `Context` parameter is needed on Android (the SDK's
 * androidx.startup initializer captures the `Application`).
 *
 * **Pre-configure access:** there is no call queue. Every member throws
 * [SuperwallError.NotConfigured] when invoked before [configure], with these
 * deliberate exemptions: [configure]/[configureAndAwait], [handleDeepLink]
 * (deep-link cold start is its primary use), the introspection members
 * [isConfigured], [isInitialized], and [configurationStatus], the stream
 * accessors [subscriptionStatusFlow] and [customerInfoFlow] (common-owned,
 * pre-seeded flows), and the [delegate] property (stored immediately;
 * installed natively at configure). Use [configureAndAwait] or gate on
 * [isConfigured] to order calls.
 *
 * **Threading:** every SDK-to-app callback — delegate methods,
 * presentation-handler closures, `feature` lambdas, configure completions,
 * and Flow emissions — is delivered on the main thread. All `suspend`
 * functions are main-safe and callable from any dispatcher.
 */
public object Superwall {
    /** Test seam: commonTest FakeBridge contract tests inject here; `null` in production. */
    private var bridgeOverride: SuperwallBridge? = null

    /** The platform bridge, created lazily on first use. */
    private val defaultBridge: SuperwallBridge by lazy { createSuperwallBridge() }

    private val bridge: SuperwallBridge
        get() = bridgeOverride ?: defaultBridge

    /** Owns the public flows and the internal bridge coroutine scope. */
    private var streams: StreamHolder = StreamHolder()

    /** The always-installed native delegate: feeds [streams] and forwards to [delegate]. */
    private var multiplexer: DelegateMultiplexer = DelegateMultiplexer(streams)

    /** Whether [configure] has been invoked (the pre-configure guard's gate). */
    private var configureCalled: Boolean = false

    /** The first configure call's outcome, once delivered. */
    private var configureResult: Result<Unit>? = null

    /** Completions from repeat configure calls made while the first is in flight. */
    private val pendingConfigureCompletions: MutableList<(Result<Unit>) -> Unit> = mutableListOf()

    // ---- Configuration -------------------------------------------------------

    /**
     * Configures a shared instance of Superwall for use in your app.
     *
     * Call this as soon as possible in your app's lifecycle. The call is
     * fire-and-forget; [completion] is invoked on the main thread with the
     * real configuration outcome (success, or failure with
     * [SuperwallError.ConfigurationFailed] — e.g. an invalid API key).
     * For suspend-style ordering, use [configureAndAwait].
     *
     * A second `configure` call is a **no-op**: a warning is logged via the
     * delegate's `handleLog`, no options/controller are re-installed, and its
     * [completion] is invoked immediately with the first call's outcome (or
     * queued behind the in-flight first call).
     *
     * @param apiKey Your Public API Key from the Superwall dashboard settings.
     * @param purchaseController An optional [PurchaseController] that handles
     * all purchasing and restoring yourself; `null` lets Superwall handle
     * purchases and subscription state automatically.
     * @param options An optional [SuperwallOptions] to customize paywall
     * appearance and behavior.
     * @param completion Invoked on the main thread when configuration
     * completes, with the configuration outcome.
     */
    public fun configure(
        apiKey: String,
        purchaseController: PurchaseController? = null,
        options: SuperwallOptions? = null,
        completion: ((Result<Unit>) -> Unit)? = null,
    ) {
        if (configureCalled) {
            multiplexer.handleLog(
                level = LogLevel.WARN,
                scope = LogScope.SUPERWALL_CORE,
                message = "Superwall.configure was called more than once. " +
                    "The repeat call is ignored; options and purchase controller are not re-installed.",
                info = null,
                error = null,
            )
            if (completion != null) {
                val delivered = configureResult
                if (delivered != null) completion(delivered) else pendingConfigureCompletions += completion
            }
            return
        }
        configureCalled = true
        // Install the multiplexer before configuring so no delegate event is
        // missed; the bridge's install is idempotent and may defer the native
        // installation until its SDK singleton exists.
        bridge.installDelegate(multiplexer)
        bridge.configure(apiKey, purchaseController, options) { result ->
            configureResult = result
            if (result.isSuccess) {
                if (options?.enableExperimentalDeviceVariables == true) {
                    bridge.enableExperimentalDeviceVariables(true)
                }
                // Lazily attach the native sources to the common-owned flows;
                // idempotent so a repeat configure never wires duplicates.
                streams.attach(bridge)
            }
            completion?.invoke(result)
            val queued = pendingConfigureCompletions.toList()
            pendingConfigureCompletions.clear()
            queued.forEach { it(result) }
        }
    }

    /**
     * Suspend twin of [configure] — resumes when the native SDK reports
     * configuration complete, throwing [SuperwallError.ConfigurationFailed]
     * on failure.
     *
     * This is the sanctioned ordering tool: there is no pre-configure call
     * queue, so await this before calling other SDK members.
     *
     * @param apiKey Your Public API Key from the Superwall dashboard settings.
     * @param purchaseController An optional [PurchaseController] that handles
     * all purchasing and restoring yourself.
     * @param options An optional [SuperwallOptions] to customize paywall
     * appearance and behavior.
     */
    public suspend fun configureAndAwait(
        apiKey: String,
        purchaseController: PurchaseController? = null,
        options: SuperwallOptions? = null,
    ) {
        val outcome = CompletableDeferred<Result<Unit>>()
        configure(apiKey, purchaseController, options) { outcome.complete(it) }
        outcome.await().getOrThrow()
    }

    /**
     * The delegate that handles Superwall lifecycle events; `null` clears it.
     *
     * Guard-exempt: may be set before [configure] — the value is stored
     * immediately and installed into the native SDK at configure. Setting or
     * clearing it never uninstalls the SDK's internal delegate, so the event
     * flows stay live regardless.
     */
    public var delegate: SuperwallDelegate?
        get() = multiplexer.userDelegate
        set(value) {
            multiplexer.userDelegate = value
        }

    /**
     * The current configuration status of the SDK: `PENDING`, `CONFIGURED`,
     * or `FAILED`. Guard-exempt — callable before [configure].
     */
    public val configurationStatus: ConfigurationStatus
        get() = bridge.getConfigurationStatus()

    /**
     * Whether [configure] has completed successfully.
     * Guard-exempt — callable before [configure].
     */
    public val isConfigured: Boolean
        get() = bridge.isConfigured()

    /**
     * Whether the native SDK singleton has been initialized.
     * Guard-exempt — callable before [configure].
     */
    public val isInitialized: Boolean
        get() = bridge.isInitialized()

    /**
     * Resets the `userId`, on-device paywall assignments, and data stored by
     * Superwall.
     */
    public fun reset() {
        requireConfigured().reset()
    }

    // ---- Identity / attributes ------------------------------------------------

    /** The current user's id — Superwall's generated alias until [identify] is called. */
    public val userId: String
        get() = requireConfigured().getUserId()

    /** Whether the user is logged in to Superwall via [identify]. */
    public val isLoggedIn: Boolean
        get() = requireConfigured().isLoggedIn()

    /**
     * Creates an account with Superwall by linking [userId] to Superwall's
     * automatically generated alias.
     *
     * Call this as soon as you have a user id, e.g. right after log in or
     * sign up.
     *
     * @param userId Your user's unique identifier.
     * @param options An optional [IdentityOptions] controlling how
     * identification is handled, e.g. waiting for restored paywall assignments.
     */
    public fun identify(
        userId: String,
        options: IdentityOptions? = null,
    ) {
        requireConfigured().identify(userId, options)
    }

    /**
     * A snapshot of the current user's attributes.
     *
     * Deliberately NOT symmetric with [setUserAttributes], which merges rather
     * than replaces — set-then-get identity does not hold.
     */
    public val userAttributes: Map<String, Any?>
        get() = requireConfigured().getUserAttributes()

    /**
     * Sets user attributes for use in audience filters and on paywalls.
     *
     * **Merge semantics** (native behavior on both platforms): [attributes]
     * are MERGED into the existing attributes; a `null` value REMOVES that
     * key; absent keys are left untouched. This is a method rather than a
     * `var` precisely because it does not replace the whole map.
     *
     * Values may be `String`, `Boolean`, `Long`, `Double`, `List`, `Map`, or
     * `Set`; anything else is stringified.
     *
     * @param attributes The attributes to merge into the user's attributes.
     */
    public fun setUserAttributes(attributes: Map<String, Any?>) {
        requireConfigured().setUserAttributes(attributes)
    }

    /**
     * Sets a single attribute for third-party integrations.
     *
     * Use this to sync user identifiers from your analytics and attribution
     * providers with Superwall for better tracking and attribution.
     *
     * @param attribute The [IntegrationAttribute] key specifying the provider.
     * @param value The value to associate with the attribute; `null` removes it.
     */
    public fun setIntegrationAttribute(
        attribute: IntegrationAttribute,
        value: String?,
    ) {
        requireConfigured().setIntegrationAttribute(attribute, value)
    }

    /**
     * Sets multiple attributes for third-party integrations at once.
     *
     * @param attributes A map of [IntegrationAttribute] keys to their values;
     * a `null` value removes that attribute.
     */
    public fun setIntegrationAttributes(attributes: Map<IntegrationAttribute, String?>) {
        requireConfigured().setIntegrationAttributes(attributes)
    }

    /** A snapshot of the currently set third-party integration attributes. */
    public val integrationAttributes: Map<IntegrationAttribute, String?>
        get() = requireConfigured().getIntegrationAttributes()

    /**
     * The device attributes Superwall tracks, usable in audience filters.
     *
     * @return A map of device attribute names to their current values.
     */
    public suspend fun getDeviceAttributes(): Map<String, Any?> = requireConfigured().getDeviceAttributes()

    /**
     * The locale identifier used by the SDK, or `null` when following the
     * device locale. Set to override (e.g. `"en_GB"`); set `null` to restore
     * the device locale.
     */
    public var localeIdentifier: String?
        get() = requireConfigured().getLocaleIdentifier()
        set(value) {
            requireConfigured().setLocaleIdentifier(value)
        }

    /**
     * The log level, determining which logs are printed and forwarded to
     * `SuperwallDelegate.handleLog`. Typed end-to-end — note the Swift
     * `.none` gotcha maps to [LogLevel.NONE].
     */
    public var logLevel: LogLevel
        get() = requireConfigured().getLogLevel()
        set(value) {
            requireConfigured().setLogLevel(value)
        }

    // ---- Entitlements / subscription --------------------------------------------

    /**
     * An immutable snapshot of the entitlements available to the user
     * (active/inactive/all/web). To filter by product ids via the native SDK,
     * use [getEntitlementsByProductIds].
     */
    public val entitlements: Entitlements
        get() = requireConfigured().getEntitlements()

    /**
     * The entitlements associated with the given product ids, resolved by the
     * native SDK.
     *
     * On Android this currently local-filters with a logged warning until
     * upstream support exists.
     *
     * @param productIds The store product ids to look up.
     * @return The entitlements granted by those products.
     */
    public suspend fun getEntitlementsByProductIds(productIds: Set<String>): Set<Entitlement> =
        requireConfigured().getEntitlementsByProductIds(productIds)

    /**
     * The current subscription status of the user, synchronously.
     *
     * Set this only when using a custom [PurchaseController] — otherwise
     * Superwall manages it automatically. For reactive observation, collect
     * [subscriptionStatusFlow].
     */
    public var subscriptionStatus: SubscriptionStatus
        get() = requireConfigured().getSubscriptionStatus()
        set(value) {
            requireConfigured().setSubscriptionStatus(value)
        }

    /**
     * A [StateFlow] of the user's subscription status.
     *
     * Guard-exempt: collectable before [configure], seeded with
     * [SubscriptionStatus.Unknown]; the native source is attached at
     * configure-complete. Emissions are delivered on the main thread.
     */
    public val subscriptionStatusFlow: StateFlow<SubscriptionStatus>
        get() = streams.subscriptionStatus.asStateFlow()

    /**
     * A [Flow] of customer info changes.
     *
     * Guard-exempt: collectable before [configure]; it has nothing to emit
     * until the native source is attached at configure-complete. No replay —
     * use [getCustomerInfo] for the current value.
     *
     * @platform iOS (Android: pending upstream delegate support — the flow may
     * not emit there).
     */
    public val customerInfoFlow: Flow<CustomerInfo>
        get() = streams.customerInfo.asSharedFlow()

    /**
     * The latest subscription and entitlement info about the customer.
     *
     * On Android this currently returns a synthesized minimal value with a
     * logged warning, pending upstream superwall-android support.
     *
     * @return The customer's latest [CustomerInfo].
     */
    public suspend fun getCustomerInfo(): CustomerInfo = requireConfigured().getCustomerInfo()

    /**
     * Confirms all unconfirmed experiment assignments and returns them.
     *
     * @return The set of all confirmed assignments.
     */
    public suspend fun confirmAllAssignments(): Set<ConfirmedAssignment> = requireConfigured().confirmAllAssignments()

    /**
     * Restores purchases via the native SDK (or your [PurchaseController], if
     * one was supplied at [configure]).
     *
     * @return The [RestorationResult] — restoration failure stays in the
     * domain type and does not throw.
     */
    public suspend fun restorePurchases(): RestorationResult = requireConfigured().restorePurchases()

    // ---- Presentation -----------------------------------------------------------

    /**
     * Registers a placement to access a feature, potentially presenting a
     * paywall first depending on your campaign settings.
     *
     * Fire-and-forget, matching the native SDKs. Each call retains its own
     * [handler] and [feature] until the presentation concludes; all callbacks
     * are delivered on the main thread.
     *
     * @param placement The name of the placement, as defined in a campaign on
     * the Superwall dashboard.
     * @param params Optional parameters usable in audience filters and on the
     * paywall (String/Boolean/Long/Double/List/Map/Set values; anything else
     * is stringified).
     * @param handler An optional [PaywallPresentationHandler] whose closures
     * provide status updates for the paywall.
     * @param feature An optional closure containing the feature to gate:
     * executed per the paywall's feature-gating behavior (immediately when no
     * paywall shows, or after purchase/restore when gated).
     */
    public fun register(
        placement: String,
        params: Map<String, Any?>? = null,
        handler: PaywallPresentationHandler? = null,
        feature: (() -> Unit)? = null,
    ) {
        requireConfigured().registerPlacement(placement, params, handler, feature)
    }

    /**
     * Previews the result of registering [placement] without presenting a
     * paywall, e.g. to modify UI ahead of time.
     *
     * @param placement The name of the placement.
     * @param params Optional parameters usable in audience filters.
     * @return The [PresentationResult] describing what registering would do.
     */
    public suspend fun getPresentationResult(
        placement: String,
        params: Map<String, Any?>? = null,
    ): PresentationResult = requireConfigured().getPresentationResult(placement, params)

    /** Dismisses the presented paywall, if one exists, resuming when done. */
    public suspend fun dismiss() {
        requireConfigured().dismiss()
    }

    /** Whether a paywall is currently being presented. */
    public val isPaywallPresented: Boolean
        get() = requireConfigured().isPaywallPresented()

    /** Info about the most recently presented paywall, or `null` if none. */
    public val latestPaywallInfo: PaywallInfo?
        get() = requireConfigured().getLatestPaywallInfo()

    /**
     * Preloads all paywalls the user may see, per campaign audience filters.
     * Use with `SuperwallOptions.paywalls.shouldPreload = false` to control
     * preload timing yourself.
     */
    public fun preloadAllPaywalls() {
        requireConfigured().preloadAllPaywalls()
    }

    /**
     * Preloads the paywalls the user may see for the given placement names.
     *
     * @param placementNames The placement names whose paywalls to preload.
     */
    public fun preloadPaywalls(placementNames: Set<String>) {
        requireConfigured().preloadPaywallsForPlacements(placementNames)
    }

    /**
     * Toggles the loading spinner on the currently presented paywall.
     *
     * @param isHidden `true` hides the spinner; `false` shows it.
     */
    public fun togglePaywallSpinner(isHidden: Boolean) {
        requireConfigured().togglePaywallSpinner(isHidden)
    }

    /**
     * Handles a deep link to preview paywalls or redeem web-checkout codes.
     *
     * Guard-EXEMPT: callable before/around [configure] on both platforms —
     * deep-link cold start is its primary use.
     *
     * @param url The deep-link URL as a string.
     * @return Whether the SDK handled the deep link.
     */
    public fun handleDeepLink(url: String): Boolean = bridge.handleDeepLink(url)

    // ---- Products / misc ------------------------------------------------------

    /**
     * Globally overrides products by product name for all paywalls, e.g.
     * `mapOf("primary" to "my_product_id")`; `null` clears the overrides.
     */
    public var overrideProductsByName: Map<String, String>?
        get() = requireConfigured().getOverrideProductsByName()
        set(value) {
            requireConfigured().setOverrideProductsByName(value)
        }

    /**
     * Consumes the Play Billing purchase identified by [purchaseToken] so the
     * product can be purchased again.
     *
     * @platform Android — iOS echoes the token back unchanged.
     * @param purchaseToken The Play Billing purchase token to consume.
     * @return The consumed purchase token.
     */
    public suspend fun consume(purchaseToken: String): String = requireConfigured().consume(purchaseToken)

    // ---- Internals --------------------------------------------------------------

    /**
     * The pre-configure guard: returns the bridge, or throws
     * [SuperwallError.NotConfigured] when [configure] has not been called.
     * The guard-exemption list lives ONLY in the members that bypass this
     * (see the class KDoc).
     */
    private fun requireConfigured(): SuperwallBridge {
        if (!configureCalled) throw SuperwallError.NotConfigured()
        return bridge
    }

    /**
     * Test seam for commonTest FakeBridge contract tests: swaps the bridge and
     * resets all facade state (configure gate, completions, delegate, streams).
     * Passing `null` restores the real platform bridge. Never called in
     * production code.
     */
    internal fun resetForTest(bridge: SuperwallBridge? = null) {
        bridgeOverride = bridge
        configureCalled = false
        configureResult = null
        pendingConfigureCompletions.clear()
        streams = StreamHolder()
        multiplexer = DelegateMultiplexer(streams)
    }
}
