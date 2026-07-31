package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.internal.BridgeListener
import com.superwall.sdk.kmp.internal.InterfaceStyle
import com.superwall.sdk.kmp.internal.StreamHolder
import com.superwall.sdk.kmp.internal.SuperwallBridge
import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.Entitlements
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import com.superwall.sdk.kmp.models.identity.IdentityOptions
import com.superwall.sdk.kmp.models.options.ConfigurationStatus
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PresentationResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.triggers.ConfirmedAssignment

/**
 * A full recording fake of [SuperwallBridge] for facade contract tests.
 *
 * - Every bridge call appends its method name to [calls].
 * - Return values come from settable `canned*` properties (prefixed to avoid
 *   JVM getter-signature clashes with the interface's `getX()` members).
 * - [installedListener] captures the [BridgeListener] (the facade's
 *   DelegateMultiplexer) so tests can drive native-delegate events.
 * - [configure] completes synchronously with [configureOutcome]; set it to
 *   `null` to hold completions and release them later via
 *   [completeHeldConfigures] (simulating an in-flight native configure).
 */
internal class FakeBridge : SuperwallBridge {
    /** One entry per bridge method invocation, in call order. */
    val calls: MutableList<String> = mutableListOf()

    // ---- Configure -----------------------------------------------------------

    var configureCount: Int = 0
    var lastApiKey: String? = null
    var lastPurchaseController: PurchaseController? = null
    var lastOptions: SuperwallOptions? = null

    /**
     * The result configure completes with, synchronously. `null` holds the
     * completion in [heldConfigureCompletions] until [completeHeldConfigures].
     */
    var configureOutcome: Result<Unit>? = Result.success(Unit)
    val heldConfigureCompletions: MutableList<(Result<Unit>) -> Unit> = mutableListOf()

    fun completeHeldConfigures(result: Result<Unit>) {
        val held = heldConfigureCompletions.toList()
        heldConfigureCompletions.clear()
        held.forEach { it(result) }
    }

    override fun configure(
        apiKey: String,
        purchaseController: PurchaseController?,
        options: SuperwallOptions?,
        completion: (Result<Unit>) -> Unit,
    ) {
        calls += "configure"
        configureCount++
        lastApiKey = apiKey
        lastPurchaseController = purchaseController
        lastOptions = options
        val outcome = configureOutcome
        if (outcome != null) completion(outcome) else heldConfigureCompletions += completion
    }

    // ---- Listener / streams ----------------------------------------------------

    var installedListener: BridgeListener? = null
    var installDelegateCount: Int = 0

    override fun installDelegate(listener: BridgeListener) {
        calls += "installDelegate"
        installDelegateCount++
        installedListener = listener
    }

    val attachedHolders: MutableList<StreamHolder> = mutableListOf()

    /** When set, written into the holder's status flow at [attachStreams] — the "initial synchronous read". */
    var statusAtAttach: SubscriptionStatus? = null

    override fun attachStreams(holder: StreamHolder) {
        calls += "attachStreams"
        attachedHolders += holder
        statusAtAttach?.let { holder.subscriptionStatus.value = it }
    }

    // ---- Canned state ------------------------------------------------------------

    var cannedIsConfigured: Boolean = false
    var cannedIsInitialized: Boolean = false
    var cannedConfigurationStatus: ConfigurationStatus = ConfigurationStatus.PENDING
    var cannedLogLevel: LogLevel = LogLevel.INFO
    var cannedUserId: String = "fake-user"
    var cannedIsLoggedIn: Boolean = false
    var cannedUserAttributes: Map<String, Any?> = emptyMap()
    var cannedIntegrationAttributes: Map<IntegrationAttribute, String?> = emptyMap()
    var cannedDeviceAttributes: Map<String, Any?> = emptyMap()
    var cannedLocaleIdentifier: String? = null
    var cannedEntitlements: Entitlements =
        Entitlements(active = emptySet(), inactive = emptySet(), all = emptySet(), web = emptySet())
    var cannedEntitlementsByProductIds: Set<Entitlement> = emptySet()
    var cannedSubscriptionStatus: SubscriptionStatus = SubscriptionStatus.Unknown
    var cannedCustomerInfo: CustomerInfo =
        CustomerInfo(subscriptions = emptyList(), nonSubscriptions = emptyList(), entitlements = emptyList(), userId = "fake-user")
    var cannedConfirmedAssignments: Set<ConfirmedAssignment> = emptySet()
    var cannedRestorationResult: RestorationResult = RestorationResult.Restored
    var cannedPresentationResult: PresentationResult = PresentationResult.PlacementNotFound
    var cannedIsPaywallPresented: Boolean = false
    var cannedLatestPaywallInfo: PaywallInfo? = null
    var cannedHandleDeepLinkResult: Boolean = false
    var cannedOverrideProductsByName: Map<String, String>? = null

    override fun reset() {
        calls += "reset"
    }

    override fun isConfigured(): Boolean {
        calls += "isConfigured"
        return cannedIsConfigured
    }

    override fun isInitialized(): Boolean {
        calls += "isInitialized"
        return cannedIsInitialized
    }

    override fun getConfigurationStatus(): ConfigurationStatus {
        calls += "getConfigurationStatus"
        return cannedConfigurationStatus
    }

    // ---- Logging -----------------------------------------------------------------

    override fun getLogLevel(): LogLevel {
        calls += "getLogLevel"
        return cannedLogLevel
    }

    override fun setLogLevel(level: LogLevel) {
        calls += "setLogLevel"
        cannedLogLevel = level
    }

    // ---- Identity / attributes ------------------------------------------------------

    var lastIdentifiedUserId: String? = null
    var lastIdentityOptions: IdentityOptions? = null
    var lastSetUserAttributes: Map<String, Any?>? = null
    var lastIntegrationAttribute: Pair<IntegrationAttribute, String?>? = null
    var lastIntegrationAttributes: Map<IntegrationAttribute, String?>? = null

    override fun getUserId(): String {
        calls += "getUserId"
        return cannedUserId
    }

    override fun isLoggedIn(): Boolean {
        calls += "isLoggedIn"
        return cannedIsLoggedIn
    }

    override fun identify(
        userId: String,
        options: IdentityOptions?,
    ) {
        calls += "identify"
        lastIdentifiedUserId = userId
        lastIdentityOptions = options
    }

    override fun getUserAttributes(): Map<String, Any?> {
        calls += "getUserAttributes"
        return cannedUserAttributes
    }

    override fun setUserAttributes(attributes: Map<String, Any?>) {
        calls += "setUserAttributes"
        lastSetUserAttributes = attributes
    }

    override fun setIntegrationAttribute(
        attribute: IntegrationAttribute,
        value: String?,
    ) {
        calls += "setIntegrationAttribute"
        lastIntegrationAttribute = attribute to value
    }

    override fun setIntegrationAttributes(attributes: Map<IntegrationAttribute, String?>) {
        calls += "setIntegrationAttributes"
        lastIntegrationAttributes = attributes
    }

    override fun getIntegrationAttributes(): Map<IntegrationAttribute, String?> {
        calls += "getIntegrationAttributes"
        return cannedIntegrationAttributes
    }

    override suspend fun getDeviceAttributes(): Map<String, Any?> {
        calls += "getDeviceAttributes"
        return cannedDeviceAttributes
    }

    override fun getLocaleIdentifier(): String? {
        calls += "getLocaleIdentifier"
        return cannedLocaleIdentifier
    }

    override fun setLocaleIdentifier(localeIdentifier: String?) {
        calls += "setLocaleIdentifier"
        cannedLocaleIdentifier = localeIdentifier
    }

    // ---- Entitlements / subscription ------------------------------------------------

    override fun getEntitlements(): Entitlements {
        calls += "getEntitlements"
        return cannedEntitlements
    }

    override suspend fun getEntitlementsByProductIds(productIds: Set<String>): Set<Entitlement> {
        calls += "getEntitlementsByProductIds"
        return cannedEntitlementsByProductIds
    }

    override fun getSubscriptionStatus(): SubscriptionStatus {
        calls += "getSubscriptionStatus"
        return cannedSubscriptionStatus
    }

    override fun setSubscriptionStatus(status: SubscriptionStatus) {
        calls += "setSubscriptionStatus"
        cannedSubscriptionStatus = status
    }

    override suspend fun getCustomerInfo(): CustomerInfo {
        calls += "getCustomerInfo"
        return cannedCustomerInfo
    }

    override suspend fun confirmAllAssignments(): Set<ConfirmedAssignment> {
        calls += "confirmAllAssignments"
        return cannedConfirmedAssignments
    }

    override suspend fun restorePurchases(): RestorationResult {
        calls += "restorePurchases"
        return cannedRestorationResult
    }

    // ---- Presentation ------------------------------------------------------------------

    /** One recorded [registerPlacement] invocation, with the exact references passed through. */
    data class Registration(
        val placement: String,
        val params: Map<String, Any?>?,
        val handler: PaywallPresentationHandler?,
        val feature: (() -> Unit)?,
    )

    val registrations: MutableList<Registration> = mutableListOf()

    override fun registerPlacement(
        placement: String,
        params: Map<String, Any?>?,
        handler: PaywallPresentationHandler?,
        feature: (() -> Unit)?,
    ) {
        calls += "registerPlacement"
        registrations += Registration(placement, params, handler, feature)
    }

    override suspend fun getPresentationResult(
        placement: String,
        params: Map<String, Any?>?,
    ): PresentationResult {
        calls += "getPresentationResult"
        return cannedPresentationResult
    }

    override suspend fun dismiss() {
        calls += "dismiss"
    }

    override fun isPaywallPresented(): Boolean {
        calls += "isPaywallPresented"
        return cannedIsPaywallPresented
    }

    override fun getLatestPaywallInfo(): PaywallInfo? {
        calls += "getLatestPaywallInfo"
        return cannedLatestPaywallInfo
    }

    override fun preloadAllPaywalls() {
        calls += "preloadAllPaywalls"
    }

    var lastPreloadedPlacements: Set<String>? = null

    override fun preloadPaywallsForPlacements(placementNames: Set<String>) {
        calls += "preloadPaywallsForPlacements"
        lastPreloadedPlacements = placementNames
    }

    var lastHandledDeepLink: String? = null

    override fun handleDeepLink(url: String): Boolean {
        calls += "handleDeepLink"
        lastHandledDeepLink = url
        return cannedHandleDeepLinkResult
    }

    var lastSpinnerIsHidden: Boolean? = null

    override fun togglePaywallSpinner(isHidden: Boolean) {
        calls += "togglePaywallSpinner"
        lastSpinnerIsHidden = isHidden
    }

    // ---- Products / misc -------------------------------------------------------------------

    override fun getOverrideProductsByName(): Map<String, String>? {
        calls += "getOverrideProductsByName"
        return cannedOverrideProductsByName
    }

    override fun setOverrideProductsByName(overrideProducts: Map<String, String>?) {
        calls += "setOverrideProductsByName"
        cannedOverrideProductsByName = overrideProducts
    }

    var lastInterfaceStyle: InterfaceStyle? = null

    override fun setInterfaceStyle(style: InterfaceStyle?) {
        calls += "setInterfaceStyle"
        lastInterfaceStyle = style
    }

    val enableExperimentalDeviceVariablesCalls: MutableList<Boolean> = mutableListOf()

    override fun enableExperimentalDeviceVariables(enabled: Boolean) {
        calls += "enableExperimentalDeviceVariables"
        enableExperimentalDeviceVariablesCalls += enabled
    }

    override suspend fun consume(purchaseToken: String): String {
        calls += "consume"
        return purchaseToken
    }
}
