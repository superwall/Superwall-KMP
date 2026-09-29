package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.PurchaseController
import com.superwall.sdk.kmp.SuperwallError
import com.superwall.sdk.kmp.internal.adapters.DelegateAdapter
import com.superwall.sdk.kmp.internal.adapters.toJs
import com.superwall.sdk.kmp.internal.adapters.wrapFeature
import com.superwall.sdk.kmp.internal.interop.JsAnySanitizer
import com.superwall.sdk.kmp.internal.interop.JsSuperwall
import com.superwall.sdk.kmp.internal.interop.createSuperwall
import com.superwall.sdk.kmp.internal.interop.jsObject
import com.superwall.sdk.kmp.internal.interop.objectKeys
import com.superwall.sdk.kmp.internal.mappers.confirmedAssignmentFromJs
import com.superwall.sdk.kmp.internal.mappers.customerInfoFromJs
import com.superwall.sdk.kmp.internal.mappers.emptyCustomerInfo
import com.superwall.sdk.kmp.internal.mappers.entitlementsFromJs
import com.superwall.sdk.kmp.internal.mappers.entitlementsSnapshot
import com.superwall.sdk.kmp.internal.mappers.errorMessage
import com.superwall.sdk.kmp.internal.mappers.integrationAttributeFromJs
import com.superwall.sdk.kmp.internal.mappers.logLevelFromJs
import com.superwall.sdk.kmp.internal.mappers.paywallInfoOrNull
import com.superwall.sdk.kmp.internal.mappers.presentationResultFromJs
import com.superwall.sdk.kmp.internal.mappers.restorationResultFromJs
import com.superwall.sdk.kmp.internal.mappers.subscriptionStatusFromJs
import com.superwall.sdk.kmp.internal.mappers.toJs
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
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The web [SuperwallBridge]: forwards to a `@superwall/paywalls-js` instance.
 *
 * JS is single-threaded, so there is no dispatching here — web invokes every
 * callback on the main thread already, which is the common threading contract.
 *
 * Where web has no equivalent the member degrades instead of throwing, and
 * says why at the site: purchase controllers, product overrides, the paywall
 * spinner, device attributes and `consume`.
 */
internal class JsSuperwallBridge : SuperwallBridge {
    /** The web instance; `null` until [configure]. The facade guards most members on configure. */
    private var sw: JsSuperwall? = null

    private var delegateAdapter: DelegateAdapter? = null

    /** Redemption codes from [handleDeepLink] calls made before [configure]. */
    private val pendingRedemptionCodes: MutableList<String> = mutableListOf()

    private val instance: JsSuperwall
        get() = sw ?: throw SuperwallError.NotConfigured()

    // ---- Configuration -----------------------------------------------------

    override fun configure(
        apiKey: String,
        purchaseController: PurchaseController?,
        options: SuperwallOptions?,
        completion: (Result<Unit>) -> Unit,
    ) {
        // The common PurchaseController only has App Store / Google Play
        // entry points, so web deliberately ignores it and keeps its built-in
        // Stripe checkout, which completes purchases and updates the
        // subscription status on its own. Handing web a no-op controller
        // instead would leave every checkout hanging.
        if (purchaseController != null) {
            warn(
                "A PurchaseController was passed to Superwall.configure, but it is ignored on web: " +
                    "web purchases always go through Superwall's built-in checkout.",
            )
        }

        val opts = jsObject()
        opts.apiKey = apiKey
        opts.options = options.toJs()
        // Installed at construction so events fired during configure reach it.
        delegateAdapter?.let { opts.delegate = it.jsDelegate }
        val created = createSuperwall(opts)
        sw = created

        created.ready.then(
            onFulfilled = {
                // A failed config fetch still resolves `ready`; the status says which it was.
                val result =
                    if (created.configurationStatus.value == "failed") {
                        Result.failure(SuperwallError.ConfigurationFailed("Superwall configuration failed."))
                    } else {
                        Result.success(Unit)
                    }
                deliverConfigured(result, completion)
            },
            onRejected = { error ->
                deliverConfigured(
                    Result.failure(SuperwallError.ConfigurationFailed(errorMessage(error) ?: "Superwall configuration failed.")),
                    completion,
                )
            },
        )
    }

    private fun deliverConfigured(
        result: Result<Unit>,
        completion: (Result<Unit>) -> Unit,
    ) {
        completion(result)
        if (result.isSuccess) {
            val codes = pendingRedemptionCodes.toList()
            pendingRedemptionCodes.clear()
            codes.forEach { redeem(it) }
        }
    }

    override fun reset() {
        instance.reset().catch { warn("Superwall.reset failed: ${errorMessage(it)}") }
    }

    override fun installDelegate(listener: BridgeListener) {
        val current = delegateAdapter
        if (current != null && current.listener === listener) return
        val adapter = DelegateAdapter(listener) { sw?.user?.effectiveId?.value ?: "" }
        delegateAdapter = adapter
        sw?.setDelegate(adapter.jsDelegate)
    }

    override fun isConfigured(): Boolean = sw?.isConfigured?.value == true

    /** Web has no separate "initialized" phase: the instance exists from [configure] on. */
    override fun isInitialized(): Boolean = sw != null

    override fun getConfigurationStatus(): ConfigurationStatus =
        when (sw?.configurationStatus?.value) {
            "configured" -> ConfigurationStatus.CONFIGURED
            "failed" -> ConfigurationStatus.FAILED
            else -> ConfigurationStatus.PENDING
        }

    override fun attachStreams(holder: StreamHolder) {
        // `subscribe` fires synchronously with the current value, which also
        // seeds the flow. Customer info reaches the flow through the
        // delegate's onCustomerInfoChange (DelegateMultiplexer), like Android.
        instance.subscriptionStatus.subscribe { status ->
            holder.subscriptionStatus.value = subscriptionStatusFromJs(status)
        }
    }

    // ---- Logging -----------------------------------------------------------

    override fun getLogLevel(): LogLevel = logLevelFromJs(instance.logLevel.value) ?: LogLevel.DEBUG

    override fun setLogLevel(level: LogLevel) {
        instance.setLogLevel(level.toJs())
    }

    // ---- Identity / attributes ----------------------------------------------

    override fun getUserId(): String = instance.user.effectiveId.value

    override fun isLoggedIn(): Boolean = instance.user.isLoggedIn.value

    override fun identify(
        userId: String,
        options: IdentityOptions?,
    ) {
        val promise = if (options != null) instance.user.identify(userId, options.toJs()) else instance.user.identify(userId)
        promise.catch { warn("Superwall.identify failed: ${errorMessage(it)}") }
    }

    /**
     * Web's `setAttributes` merges shallowly and stores a `null` value rather
     * than deleting the key, so `null`-valued keys are dropped here to keep
     * the common contract ("a `null` value removes that key") on read-back.
     */
    override fun getUserAttributes(): Map<String, Any?> =
        JsAnySanitizer.fromObject(instance.user.attributes.value).orEmpty().filterValues { it != null }

    override fun setUserAttributes(attributes: Map<String, Any?>) {
        instance.user.setAttributes(JsAnySanitizer.toObject(attributes))
    }

    override fun setIntegrationAttribute(
        attribute: IntegrationAttribute,
        value: String?,
    ) {
        instance.user.setIntegrationAttribute(attribute.toJs(), value)
    }

    override fun setIntegrationAttributes(attributes: Map<IntegrationAttribute, String?>) {
        val obj = jsObject()
        for ((attribute, value) in attributes) obj[attribute.toJs()] = value
        instance.user.setIntegrationAttributes(obj)
    }

    /** Web-only keys (`meta`, `custom`, …) have no [IntegrationAttribute] and are left out. */
    override fun getIntegrationAttributes(): Map<IntegrationAttribute, String?> {
        val wire = instance.user.integrationAttributes.value ?: return emptyMap()
        val result = LinkedHashMap<IntegrationAttribute, String?>()
        for (key in objectKeys(wire)) {
            val attribute = integrationAttributeFromJs(key) ?: continue
            result[attribute] = wire[key] as? String
        }
        return result
    }

    /** Web does not expose its device attributes (they only travel on the `device_attributes` event). */
    override suspend fun getDeviceAttributes(): Map<String, Any?> = emptyMap()

    override fun getLocaleIdentifier(): String? = instance.locale.value

    override fun setLocaleIdentifier(localeIdentifier: String?) {
        instance.setLocale(localeIdentifier)
    }

    // ---- Entitlements / subscription ----------------------------------------

    override fun getEntitlements(): Entitlements =
        instance.entitlements.let { entitlementsSnapshot(it.active.value, it.inactive.value, it.all.value) }

    override suspend fun getEntitlementsByProductIds(productIds: Set<String>): Set<Entitlement> =
        entitlementsFromJs(instance.entitlements.byProductIds(productIds.toTypedArray()))

    override fun getSubscriptionStatus(): SubscriptionStatus = subscriptionStatusFromJs(instance.subscriptionStatus.value)

    override fun setSubscriptionStatus(status: SubscriptionStatus) {
        instance.purchases.setSubscriptionStatus(status.toJs())
    }

    /** Web resolves `null` before any customer info exists; that becomes an empty value for the current user. */
    override suspend fun getCustomerInfo(): CustomerInfo {
        val info = instance.purchases.getCustomerInfo().await()
        return if (info == null) emptyCustomerInfo(getUserId()) else customerInfoFromJs(info)
    }

    override suspend fun confirmAllAssignments(): Set<ConfirmedAssignment> =
        instance.placements.confirmAllAssignments().await().map { confirmedAssignmentFromJs(it) }.toSet()

    /**
     * `purchases.restore()` resolves with the `RestorationResult` from the
     * next paywalls-js release on; 0.3.0 (pinned) resolves `undefined` and
     * only publishes the outcome as a `restore_complete` / `restore_fail`
     * event, so the events captured during the call are the fallback. Drop
     * the listeners once the pin moves past the release that returns it.
     */
    override suspend fun restorePurchases(): RestorationResult {
        val events = instance.events
        var failure: String? = null
        var completed = false
        val onFail: (dynamic) -> Unit = { event -> failure = errorMessage(event.detail?.reason) ?: "Restore failed." }
        val onComplete: (dynamic) -> Unit = { completed = true }
        events.addEventListener("restore_fail", onFail)
        events.addEventListener("restore_complete", onComplete)
        try {
            val returned = instance.purchases.restore().await()
            if (returned != null) return restorationResultFromJs(returned)
        } catch (throwable: Throwable) {
            return RestorationResult.Failed(throwable.message ?: "Restore failed.")
        } finally {
            events.removeEventListener("restore_fail", onFail)
            events.removeEventListener("restore_complete", onComplete)
        }
        val failed = failure
        return when {
            failed != null -> RestorationResult.Failed(failed)
            completed -> RestorationResult.Restored
            else -> RestorationResult.Failed("Superwall restore completed without a result.")
        }
    }

    // ---- Presentation --------------------------------------------------------

    override fun registerPlacement(
        placement: String,
        params: Map<String, Any?>?,
        handler: PaywallPresentationHandler?,
        feature: (() -> Unit)?,
    ) {
        val args = jsObject()
        args.placement = placement
        params?.let { args.params = JsAnySanitizer.toObject(it) }
        handler?.let { args.handler = it.toJs() }
        feature?.let { args.feature = wrapFeature(it) }
        // Fire-and-forget like the natives. Web resolves failures as
        // `{ type: "error" }` and reports them to handler.onError itself; this
        // only catches a rejection so it is not left unhandled.
        instance.register(args).catch { warn("Superwall.register($placement) failed: ${errorMessage(it)}") }
    }

    override suspend fun getPresentationResult(
        placement: String,
        params: Map<String, Any?>?,
    ): PresentationResult {
        val promise =
            if (params != null) {
                instance.placements.getPresentationResult(placement, JsAnySanitizer.toObject(params))
            } else {
                instance.placements.getPresentationResult(placement)
            }
        return presentationResultFromJs(promise.await())
    }

    /** Web's `dismiss()` returns immediately; this resumes once the paywall is actually gone. */
    override suspend fun dismiss() {
        val web = instance
        if (!web.isPaywallPresented.value) return
        web.dismiss()
        suspendCancellableCoroutine { continuation ->
            var unsubscribe: (() -> Unit)? = null
            var done = false
            unsubscribe =
                web.isPaywallPresented.subscribe { presented ->
                    if (!presented && !done) {
                        done = true
                        unsubscribe?.invoke()
                        continuation.resume(Unit)
                    }
                }
            // subscribe() fires synchronously, possibly before `unsubscribe` was assigned.
            if (done) unsubscribe()
            continuation.invokeOnCancellation { unsubscribe() }
        }
    }

    override fun isPaywallPresented(): Boolean = instance.isPaywallPresented.value

    override fun getLatestPaywallInfo(): PaywallInfo? = paywallInfoOrNull(instance.latestPaywallInfo.value)

    override fun preloadAllPaywalls() {
        instance.placements.preloadAll().catch { warn("Superwall.preloadAllPaywalls failed: ${errorMessage(it)}") }
    }

    override fun preloadPaywallsForPlacements(placementNames: Set<String>) {
        instance.placements.preloadFor(placementNames.toTypedArray())
            .catch { warn("Superwall.preloadPaywalls failed: ${errorMessage(it)}") }
    }

    /**
     * The web deep links Superwall handles are redemption links
     * (`?code=redemption_…`). Web already redeems one in the page's own URL
     * at configure, so this is for links that arrive another way; before
     * configure the code is queued and redeemed once configuration succeeds.
     * Paywall-preview links are native-only and return `false`.
     */
    override fun handleDeepLink(url: String): Boolean {
        val code = redemptionCodeIn(url) ?: return false
        if (sw == null) pendingRedemptionCodes += code else redeem(code)
        return true
    }

    private fun redeem(code: String) {
        // Outcomes reach the app through the delegate's willRedeemLink/didRedeemLink.
        instance.redeem(code).catch { warn("Superwall redemption failed: ${errorMessage(it)}") }
    }

    /** Web paywalls manage their own loading state; there is no spinner to toggle. */
    override fun togglePaywallSpinner(isHidden: Boolean) {}

    // ---- Products / misc ------------------------------------------------------

    /** Web has no global product overrides, so nothing is ever set. */
    override fun getOverrideProductsByName(): Map<String, String>? = null

    override fun setOverrideProductsByName(overrideProducts: Map<String, String>?) {
        if (overrideProducts != null) warn("overrideProductsByName is not supported on web and is ignored.")
    }

    override fun setInterfaceStyle(style: InterfaceStyle?) {
        instance.setInterfaceStyle(style.toJs())
    }

    /**
     * Web reads this only from the options passed at configure (which the
     * facade's options already carry), so there is nothing to do afterwards.
     */
    override fun enableExperimentalDeviceVariables(enabled: Boolean) {}

    /** Play Billing only; echoes the token back like iOS. */
    override suspend fun consume(purchaseToken: String): String = purchaseToken

    // ---- Helpers ----------------------------------------------------------------

    private fun warn(message: String) {
        val listener = delegateAdapter?.listener
        if (listener != null && listener.forwardsToUserDelegate) {
            listener.handleLog(LogLevel.WARN, LogScope.SUPERWALL_CORE, message, null, null)
        } else {
            console.warn("[Superwall] $message")
        }
    }
}

/** The `code` query parameter of [url] if it is a Superwall redemption code, else `null`. */
internal fun redemptionCodeIn(url: String): String? {
    val query = url.substringAfter('?', missingDelimiterValue = "").substringBefore('#')
    for (pair in query.split('&')) {
        val key = pair.substringBefore('=')
        if (key != "code") continue
        val value = decodeURIComponent(pair.substringAfter('=', missingDelimiterValue = ""))
        if (value.startsWith("redemption_")) return value
    }
    return null
}

private external fun decodeURIComponent(encoded: String): String

internal actual fun createSuperwallBridge(): SuperwallBridge = JsSuperwallBridge()
