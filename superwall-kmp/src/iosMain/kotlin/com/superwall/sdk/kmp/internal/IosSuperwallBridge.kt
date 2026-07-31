@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.PurchaseController
import com.superwall.sdk.kmp.SuperwallError
import com.superwall.sdk.kmp.internal.adapters.DelegateAdapter
import com.superwall.sdk.kmp.internal.adapters.PresentationHandlerAdapter
import com.superwall.sdk.kmp.internal.adapters.PurchaseControllerAdapter
import com.superwall.sdk.kmp.internal.interop.NSAnySanitizer
import com.superwall.sdk.kmp.internal.interop.awaitCompletion
import com.superwall.sdk.kmp.internal.interop.awaitVoidCompletion
import com.superwall.sdk.kmp.internal.ios.interop.SWBConfigurationStatusConfigured
import com.superwall.sdk.kmp.internal.ios.interop.SWBConfigurationStatusFailed
import com.superwall.sdk.kmp.internal.ios.interop.SWBConfirmedAssignment
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBObservation
import com.superwall.sdk.kmp.internal.ios.interop.SWBPresentationResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestorationResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBSuperwallBridge
import com.superwall.sdk.kmp.internal.mappers.integrationAttributeFromWireName
import com.superwall.sdk.kmp.internal.mappers.integrationAttributeToSWB
import com.superwall.sdk.kmp.internal.mappers.logLevelFromSWB
import com.superwall.sdk.kmp.internal.mappers.logLevelToSWB
import com.superwall.sdk.kmp.internal.mappers.toEntitlementSet
import com.superwall.sdk.kmp.internal.mappers.toModel
import com.superwall.sdk.kmp.internal.mappers.toSWB
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSNull
import platform.Foundation.NSNumber

/**
 * The iOS [SuperwallBridge]: a thin forwarder to the `SWBSuperwallBridge`
 * Swift facade (plan §5). All semantic work — enum destructuring, the event
 * envelope switch, configure-status derivation — lives in the Swift bridge;
 * this class only converts between common models and the `@objc` envelope
 * types and manages Kotlin-side adapter retention (plan §6.4: never rely on
 * ObjC retaining Kotlin-implemented objects).
 *
 * Threading (plan §6): [scope] is the bridge's one internal
 * `Dispatchers.Main.immediate` supervisor scope; adapters use it to deliver
 * SDK→app callbacks on the main thread and to run app→SDK suspend callbacks.
 * Suspend members wrap the facade's completion-handler calls via
 * `suspendCancellableCoroutine` helpers and are main-safe: continuations
 * resume off whatever queue the completion fires on.
 */
internal class IosSuperwallBridge : SuperwallBridge {
    private val swb: SWBSuperwallBridge
        get() = SWBSuperwallBridge.sharedBridge()

    /** One internal supervisor scope per bridge (plan §6.3). */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Strongly retained delegate adapter (installed natively at configure). */
    private var delegateAdapter: DelegateAdapter? = null
    private var delegateInstalled: Boolean = false

    /** Strongly retained for the lifetime of the process once configured. */
    private var purchaseControllerAdapter: PurchaseControllerAdapter? = null

    /**
     * Main-confined strong retention of per-registration handler adapters for
     * each paywall's lifetime; entries are removed by the adapters' terminal
     * callbacks (dismiss with a real close reason, skip, or error). Mutated
     * only on the main thread — additions hop through [scope].
     */
    private val activeHandlers: MutableSet<PresentationHandlerAdapter> = mutableSetOf()

    /** Retained observation tokens for the stream subscriptions (plan §5.3). */
    private var subscriptionStatusObservation: SWBObservation? = null
    private var customerInfoObservation: SWBObservation? = null

    // ---- Configuration -----------------------------------------------------

    override fun configure(
        apiKey: String,
        purchaseController: PurchaseController?,
        options: SuperwallOptions?,
        completion: (Result<Unit>) -> Unit,
    ) {
        val controllerAdapter = purchaseController?.let { PurchaseControllerAdapter(it, scope) }
        purchaseControllerAdapter = controllerAdapter

        swb.configureWithApiKey(
            apiKey,
            options?.toSWB(),
            controllerAdapter,
        ) { status, error ->
            val result = if (status == SWBConfigurationStatusFailed) {
                Result.failure(
                    SuperwallError.ConfigurationFailed(
                        error?.localizedDescription ?: "Superwall configuration failed.",
                    ),
                )
            } else {
                if (status != SWBConfigurationStatusConfigured) {
                    // Post-completion pending: treated as success with a logged
                    // warning (plan §5.2).
                    warn("Superwall configuration completed while still pending; treating as success.")
                }
                Result.success(Unit)
            }
            // The facade contract delivers completions on the main thread.
            scope.launch { completion(result) }
        }

        // The native Superwall singleton exists synchronously once configure
        // has been called, so the deferred delegate install is safe now
        // (setting it earlier would hit SuperwallKit's throwaway pre-configure
        // instance and be lost).
        installPendingDelegate()
    }

    override fun reset() {
        swb.reset()
    }

    override fun installDelegate(listener: BridgeListener) {
        val current = delegateAdapter
        if (current != null && current.listener === listener) {
            // Idempotent repeat install (repeat configure).
            installPendingDelegate()
            return
        }
        delegateAdapter = DelegateAdapter(listener, scope)
        delegateInstalled = false
        installPendingDelegate()
    }

    private fun installPendingDelegate() {
        val adapter = delegateAdapter ?: return
        if (delegateInstalled) return
        if (!swb.isInitialized()) return
        swb.setDelegate(adapter)
        delegateInstalled = true
    }

    override fun isConfigured(): Boolean = swb.isConfigured()

    override fun isInitialized(): Boolean = swb.isInitialized()

    override fun getConfigurationStatus(): ConfigurationStatus =
        when (swb.configurationStatus()) {
            SWBConfigurationStatusConfigured -> ConfigurationStatus.CONFIGURED
            SWBConfigurationStatusFailed -> ConfigurationStatus.FAILED
            else -> ConfigurationStatus.PENDING
        }

    override fun attachStreams(holder: StreamHolder) {
        // Both observe* facades emit the current value immediately and then
        // every (deduplicated, for customer info) change — the initial emission
        // closes the pre-attach seed gap. The tokens are retained for the
        // lifetime of the bridge; delivery thread is irrelevant to the
        // thread-safe flow mutations.
        subscriptionStatusObservation = swb.observeSubscriptionStatus { status ->
            // The cinterop block signature is nullable although the Swift side
            // never emits nil; degrade an unexpected nil to Unknown (plan §7).
            holder.subscriptionStatus.value = status?.toModel() ?: SubscriptionStatus.Unknown
        }
        customerInfoObservation = swb.observeCustomerInfo { customerInfo ->
            // Nullable-only-in-the-binding: skip an unexpected nil emission
            // rather than fabricating an empty customer info (plan §7).
            customerInfo?.let { holder.customerInfo.tryEmit(it.toModel()) }
        }
    }

    // ---- Logging -----------------------------------------------------------

    override fun getLogLevel(): LogLevel = logLevelFromSWB(swb.logLevel())

    override fun setLogLevel(level: LogLevel) {
        swb.setLogLevel(logLevelToSWB(level))
    }

    // ---- Identity / attributes ----------------------------------------------

    override fun getUserId(): String = swb.userId()

    override fun isLoggedIn(): Boolean = swb.isLoggedIn()

    override fun identify(
        userId: String,
        options: IdentityOptions?,
    ) {
        swb.identifyWithUserId(userId, options?.toSWB())
    }

    override fun getUserAttributes(): Map<String, Any?> = NSAnySanitizer.fromMap(swb.getUserAttributes())

    override fun setUserAttributes(attributes: Map<String, Any?>) {
        // NSNull values carry the "remove this key" merge semantics across the
        // boundary (NSAnySanitizer.toNSParams converts Kotlin nulls to NSNull).
        swb.setUserAttributes(NSAnySanitizer.toNSParams(attributes))
    }

    override fun setIntegrationAttribute(
        attribute: IntegrationAttribute,
        value: String?,
    ) {
        swb.setIntegrationAttribute(integrationAttributeToSWB(attribute), value)
    }

    override fun setIntegrationAttributes(attributes: Map<IntegrationAttribute, String?>) {
        val swbAttributes: Map<Any?, Any> = attributes.entries.associate { (attribute, value) ->
            NSNumber(long = integrationAttributeToSWB(attribute)) as Any? to (value ?: NSNull())
        }
        swb.setIntegrationAttributes(swbAttributes)
    }

    override fun getIntegrationAttributes(): Map<IntegrationAttribute, String?> {
        val wire = swb.getIntegrationAttributes()
        val result = LinkedHashMap<IntegrationAttribute, String?>()
        for ((key, value) in wire) {
            val name = key as? String ?: continue
            // Wire names without a common counterpart (e.g. the 4.16.x-only
            // firebaseInstallationId / singularDeviceId) are skipped.
            val attribute = integrationAttributeFromWireName(name) ?: continue
            result[attribute] = value as? String
        }
        return result
    }

    override suspend fun getDeviceAttributes(): Map<String, Any?> {
        // The cinterop block signature is nullable although the Swift side
        // never passes nil; degrade an unexpected nil to no attributes.
        val attributes = awaitCompletion<Map<Any?, *>?> { swb.getDeviceAttributes(it) }
        return attributes?.let(NSAnySanitizer::fromMap) ?: emptyMap()
    }

    override fun getLocaleIdentifier(): String? = swb.localeIdentifier()

    override fun setLocaleIdentifier(localeIdentifier: String?) {
        swb.setLocaleIdentifier(localeIdentifier)
    }

    // ---- Entitlements / subscription ----------------------------------------

    override fun getEntitlements(): Entitlements = swb.getEntitlements().toModel()

    override suspend fun getEntitlementsByProductIds(productIds: Set<String>): Set<Entitlement> =
        swb.getEntitlementsByProductIds(productIds.toList()).toEntitlementSet()

    override fun getSubscriptionStatus(): SubscriptionStatus = swb.getSubscriptionStatus().toModel()

    override fun setSubscriptionStatus(status: SubscriptionStatus) {
        swb.setSubscriptionStatus(status.toSWB())
    }

    override suspend fun getCustomerInfo(): CustomerInfo =
        // The cinterop block signature is nullable although the Swift side
        // never passes nil; degrade an unexpected nil to an empty customer
        // info rather than crashing (plan §7).
        awaitCompletion<SWBCustomerInfo?> { swb.getCustomerInfo(it) }?.toModel()
            ?: CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                entitlements = emptyList(),
                userId = "",
            )

    override suspend fun confirmAllAssignments(): Set<ConfirmedAssignment> =
        // Nullable-only-in-the-binding: an unexpected nil reads as no assignments.
        awaitCompletion<List<*>?> { swb.confirmAllAssignments(it) }
            .orEmpty()
            .mapNotNull { (it as? SWBConfirmedAssignment)?.toModel() }
            .toSet()

    override suspend fun restorePurchases(): RestorationResult =
        // Nullable-only-in-the-binding: an unexpected nil reads as a failure.
        awaitCompletion<SWBRestorationResult?> { swb.restorePurchases(it) }?.toModel()
            ?: RestorationResult.Failed("Superwall restore completed without a result.")

    // ---- Presentation --------------------------------------------------------

    override fun registerPlacement(
        placement: String,
        params: Map<String, Any?>?,
        handler: PaywallPresentationHandler?,
        feature: (() -> Unit)?,
    ) {
        val handlerAdapter = handler?.let { userHandler ->
            PresentationHandlerAdapter(userHandler, scope) { finished ->
                // Terminal callbacks arrive on the main thread.
                activeHandlers.remove(finished)
            }
        }
        if (handlerAdapter != null) {
            // Retention is main-confined; the launch closure keeps the adapter
            // strongly reachable until the set does (plan §6.4).
            scope.launch { activeHandlers.add(handlerAdapter) }
        }

        val wrappedFeature: (() -> Unit)? = feature?.let { block ->
            {
                scope.launch {
                    try {
                        block()
                    } catch (throwable: Throwable) {
                        println("[Superwall] register feature block threw: $throwable")
                    }
                }
            }
        }

        swb.registerWithPlacement(
            placement,
            params?.let(NSAnySanitizer::toNSParams),
            handlerAdapter,
            wrappedFeature,
        )
    }

    override suspend fun getPresentationResult(
        placement: String,
        params: Map<String, Any?>?,
    ): PresentationResult =
        // Nullable-only-in-the-binding: an unexpected nil degrades to
        // PaywallNotAvailable, matching the mapper's own unknown-case fallback.
        awaitCompletion<SWBPresentationResult?> { completion ->
            swb.getPresentationResultWithPlacement(
                placement,
                params?.let(NSAnySanitizer::toNSParams),
                completion,
            )
        }?.toModel() ?: PresentationResult.PaywallNotAvailable

    override suspend fun dismiss() {
        awaitVoidCompletion { swb.dismiss(it) }
    }

    override fun isPaywallPresented(): Boolean = swb.isPaywallPresented()

    override fun getLatestPaywallInfo(): PaywallInfo? = swb.latestPaywallInfo()?.toModel()

    override fun preloadAllPaywalls() {
        swb.preloadAllPaywalls()
    }

    override fun preloadPaywallsForPlacements(placementNames: Set<String>) {
        swb.preloadPaywallsWithPlacements(placementNames.toList())
    }

    override fun handleDeepLink(url: String): Boolean = swb.handleDeepLink(url)

    override fun togglePaywallSpinner(isHidden: Boolean) {
        swb.togglePaywallSpinnerWithIsHidden(isHidden)
    }

    // ---- Products / misc ------------------------------------------------------

    override fun getOverrideProductsByName(): Map<String, String>? =
        swb.overrideProductsByName()?.entries?.mapNotNull { (key, value) ->
            val name = key as? String ?: return@mapNotNull null
            val productId = value as? String ?: return@mapNotNull null
            name to productId
        }?.toMap()

    override fun setOverrideProductsByName(overrideProducts: Map<String, String>?) {
        @Suppress("UNCHECKED_CAST")
        swb.setOverrideProductsByName(overrideProducts as Map<Any?, *>?)
    }

    override fun setInterfaceStyle(style: InterfaceStyle?) {
        swb.setInterfaceStyle(style.toSWB())
    }

    override fun enableExperimentalDeviceVariables(enabled: Boolean) {
        // The SWB facade exposes no post-configure toggle: on iOS the option is
        // applied at configure via SWBSuperwallOptions.enableExperimentalDeviceVariables,
        // so this member is a documented no-op here.
    }

    override suspend fun consume(purchaseToken: String): String =
        // Documented iOS no-op in the bridge: echoes the token back. The
        // cinterop block signature is nullable although the Swift side never
        // passes nil; an unexpected nil echoes the input token directly.
        awaitCompletion<String?> { swb.consumeWithPurchaseToken(purchaseToken, it) }
            ?: purchaseToken

    // ---- Internals ------------------------------------------------------------

    /** Routes a bridge-level warning through the installed listener when present. */
    private fun warn(message: String) {
        val listener = delegateAdapter?.listener
        if (listener != null) {
            scope.launch {
                listener.handleLog(
                    level = LogLevel.WARN,
                    scope = LogScope.SUPERWALL_CORE,
                    message = message,
                    info = null,
                    error = null,
                )
            }
        } else {
            println("[Superwall] $message")
        }
    }
}
