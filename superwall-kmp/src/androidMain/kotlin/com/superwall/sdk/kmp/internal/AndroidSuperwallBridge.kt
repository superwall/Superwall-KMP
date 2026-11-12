package com.superwall.sdk.kmp.internal

import android.net.Uri
import android.util.Log
import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.PurchaseController
import com.superwall.sdk.kmp.SuperwallError
import com.superwall.sdk.kmp.internal.adapters.DelegateAdapter
import com.superwall.sdk.kmp.internal.adapters.OnBackPressedAdapter
import com.superwall.sdk.kmp.internal.adapters.PresentationHandlerAdapter
import com.superwall.sdk.kmp.internal.adapters.PurchaseControllerAdapter
import com.superwall.sdk.kmp.internal.mappers.sanitizeParams
import com.superwall.sdk.kmp.internal.mappers.toKmp
import com.superwall.sdk.kmp.internal.mappers.toNative
import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.Entitlements
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import com.superwall.sdk.kmp.models.identity.IdentityOptions
import com.superwall.sdk.kmp.models.options.AdConsent
import com.superwall.sdk.kmp.models.options.ConfigurationStatus
import com.superwall.sdk.kmp.models.options.EventTrackingBehavior
import com.superwall.sdk.kmp.models.options.InterfaceStyle
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PresentationResult
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.store.StoreProduct
import com.superwall.sdk.kmp.models.triggers.ConfirmedAssignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.superwall.sdk.Superwall as NativeSuperwall
import com.superwall.sdk.config.models.ConfigurationStatus as NativeConfigurationStatus
import com.superwall.sdk.config.options.SuperwallOptions as NativeSuperwallOptions
import com.superwall.sdk.identity.IdentityOptions as NativeIdentityOptions
import com.superwall.sdk.identity.identify as nativeIdentify
import com.superwall.sdk.identity.setUserAttributes as nativeSetUserAttributes
import com.superwall.sdk.network.device.InterfaceStyle as NativeInterfaceStyle
import com.superwall.sdk.paywall.presentation.dismiss as nativeDismiss
import com.superwall.sdk.paywall.presentation.get_presentation_result.getPresentationResult as nativeGetPresentationResult
import com.superwall.sdk.paywall.presentation.register as nativeRegister

/**
 * The Android [SuperwallBridge]: a port of the Flutter plugin's
 * `SuperwallHost.kt` minus all Pigeon transport, calling superwall-android
 * directly (written against 2.7.11, now compiled against 2.10.2).
 *
 * Deliberate fixes over the Flutter host
 * - `configure`'s completion forwards the REAL native `Result<Unit>` instead
 *   of dropping the success bool (`ConfigureCompletionProxy` bug), and no
 *   longer force-overwrites `logging.level = debug`.
 * - `registerPlacement` creates a fresh [PresentationHandlerAdapter] per call
 *   — no placement-keyed registry, no one-handler-per-placement aliasing.
 * - `getEntitlementsByProductIds` calls the REAL native
 *   `Entitlements.byProductIds` (present since 2.7.11; the Flutter host's
 *   "Android SDK doesn't have it" local-filter stub is obsolete).
 * - `getCustomerInfo` returns the REAL native `Superwall.getCustomerInfo()`
 *   (present since 2.7.11; the Flutter host synthesized a minimal stub).
 * - Integration attributes use the native
 *   `setIntegrationAttributes(Map<AttributionProvider, String>)` API instead
 *   of the Flutter host's `$`-prefixed user-attribute workaround, with
 *   merge/null-removes semantics implemented over the native replace-all call
 *   by [IntegrationAttributeStore].
 *
 * Threading SDK→app callbacks dispatch on
 * `Dispatchers.Main.immediate` via the attached [StreamHolder]'s scope
 * (see [callbackScope]); native suspend calls keep the Flutter host's
 * conservative `Dispatchers.IO` hop until individually verified main-safe.
 * Native→common MAPPING deliberately does not run there: [DelegateAdapter]
 * maps on the native SDK's calling thread and only hands the finished value to
 * the main thread, and skips the pure-forwarding hooks entirely when no user
 * delegate is set (see its KDoc — `Logger` fires `handleLog` for every
 * internal log line, so mapping there would flood the main looper).
 */
internal class AndroidSuperwallBridge : SuperwallBridge {
    /**
     * Used for callbacks that fire before [attachStreams] hands over the
     * common [StreamHolder] (delegate installation precedes configure).
     * Same composition as `StreamHolder.scope`.
     */
    private val fallbackScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The common stream holder, available from configure-complete onward. */
    @Volatile
    private var streamHolder: StreamHolder? = null

    /** The installed [BridgeListener] (the common `DelegateMultiplexer`). */
    @Volatile
    private var listener: BridgeListener? = null

    /** The native delegate adapter wrapping [listener]; created once. */
    @Volatile
    private var delegateAdapter: DelegateAdapter? = null

    /**
     * The scope all SDK→app callbacks are launched on: the [StreamHolder]'s
     * `Main.immediate` scope once attached, an identically composed fallback
 * before that.
     */
    private fun callbackScope(): CoroutineScope = streamHolder?.scope ?: fallbackScope

    /**
     * Routes adapter-caught user-callback exceptions to the app through the
 * common `handleLog` path, with logcat as the last resort.
     */
    private fun logError(
        message: String,
        error: Throwable?,
    ) {
        Log.w(TAG, message, error)
        val target = listener?.takeIf { it.forwardsToUserDelegate } ?: return
        val mappedError = error?.let { it.localizedMessage ?: it.message ?: it.toString() }
        callbackScope().launch {
            try {
                target.handleLog(
                    level = LogLevel.ERROR,
                    scope = LogScope.SUPERWALL_CORE,
                    message = message,
                    info = null,
                    error = mappedError,
                )
            } catch (throwable: Throwable) {
                Log.w(TAG, "handleLog forwarding failed", throwable)
            }
        }
    }

    /** Merge/null-removes semantics over the native replace-all call. */
    private val integrationAttributes =
        IntegrationAttributeStore(
            readNative = { NativeSuperwall.instance.integrationAttributes },
            writeNative = { NativeSuperwall.instance.setIntegrationAttributes(it) },
            log = ::logError,
        )

    // ---- Configuration -----------------------------------------------------

    /**
     * Calls the static
     * `NativeSuperwall.configure(applicationContext, apiKey,
     * purchaseController, options, activityProvider, completion)`
     * (verified signature: superwall-android 2.10.2 `Superwall.kt:479`).
     *
     * The `Application` comes from [ApplicationContextHolder]; when neither
     * the androidx.startup initializer nor `Superwall.androidSetup` ran, this
     * throws [SuperwallError.NotInitialized] immediately and actionably
 * instead of letting the native SDK crash later.
     *
     * The native completion's `Result<Unit>` is FORWARDED (fixing the Flutter
     * host's dropped success bool); failures are wrapped in
     * [SuperwallError.ConfigurationFailed] and delivered on the main thread.
     */
    override fun configure(
        apiKey: String,
        purchaseController: PurchaseController?,
        options: SuperwallOptions?,
        completion: (Result<Unit>) -> Unit,
    ) {
        val application = ApplicationContextHolder.application

        val nativeOptions = options?.toNative() ?: NativeSuperwallOptions()
        options?.paywalls?.onBackPressed?.let { userCallback ->
            nativeOptions.paywalls.onBackPressed =
                OnBackPressedAdapter(userCallback, ::logError).nativeCallback
        }

        NativeSuperwall.configure(
            applicationContext = application,
            apiKey = apiKey,
            purchaseController =
                purchaseController?.let { PurchaseControllerAdapter(it, ::logError) },
            options = nativeOptions,
            activityProvider = CurrentActivityTracker,
            completion = { result ->
                val mapped =
                    result.fold(
                        onSuccess = { Result.success(Unit) },
                        onFailure = { throwable ->
                            Result.failure(
                                SuperwallError.ConfigurationFailed(
                                    throwable.localizedMessage ?: throwable.message
                                        ?: throwable.toString(),
                                ),
                            )
                        },
                    )
                callbackScope().launch { completion(mapped) }
            },
        )

        // The native singleton exists synchronously after configure; report the
        // wrapper before the first config request goes out, and install the
        // delegate now so no early event is missed.
        NativeSuperwall.instance.setPlatformWrapper(PLATFORM_WRAPPER, SDK_VERSION)
        installNativeDelegateIfPossible()
    }

    override fun reset() {
        NativeSuperwall.instance.reset()
    }

    /**
     * Stores the multiplexer and installs it into the native SDK's delegate
     * slot once the native singleton exists. Idempotent: repeat calls with the
     * same listener re-use the existing [DelegateAdapter], and re-installation
     * into the same slot is harmless.
     */
    override fun installDelegate(listener: BridgeListener) {
        this.listener = listener
        installNativeDelegateIfPossible()
    }

    private fun installNativeDelegateIfPossible() {
        val target = listener ?: return
        if (!NativeSuperwall.initialized) return
        val adapter =
            delegateAdapter?.takeIf { it.wraps(target) }
                ?: DelegateAdapter(target).also { delegateAdapter = it }
        NativeSuperwall.instance.delegate = adapter
    }

    override fun isConfigured(): Boolean =
        NativeSuperwall.initialized &&
            NativeSuperwall.instance.configurationState is NativeConfigurationStatus.Configured

    override fun isInitialized(): Boolean = NativeSuperwall.initialized

    override fun getConfigurationStatus(): ConfigurationStatus {
        if (!NativeSuperwall.initialized) return ConfigurationStatus.PENDING
        return when (NativeSuperwall.instance.configurationState) {
            is NativeConfigurationStatus.Configured -> ConfigurationStatus.CONFIGURED
            is NativeConfigurationStatus.Failed -> ConfigurationStatus.FAILED
            is NativeConfigurationStatus.Pending -> ConfigurationStatus.PENDING
        }
    }

    /**
     * Attaches the native subscription-status source to the common-owned flow:
     * an initial synchronous read closes the seed gap, then a collector keeps
     * it fed (stream wiring).
     *
     * The collector runs on [Dispatchers.Default], not the holder's
     * `Main.immediate` scope: it only maps a status and assigns
     * `MutableStateFlow.value`, which is thread-safe, and a `StateFlow`
     * delivers to each collector on ITS OWN context — so where this feed runs
     * is invisible to app code and has no business occupying the looper.
     *
     * `customerInfo` is deliberately NOT collected here: superwall-android
     * `SuperwallDelegate.customerInfoDidChange` hook exists and is
     * wired in [DelegateAdapter], and the common `DelegateMultiplexer` already
     * feeds `StreamHolder.customerInfo` from that hook — collecting the native
     * `Superwall.instance.customerInfo` StateFlow here as well would
     * double-emit every change.
     */
    override fun attachStreams(holder: StreamHolder) {
        streamHolder = holder
        val native = NativeSuperwall.instance
        holder.subscriptionStatus.value = native.subscriptionStatus.value.toKmp()
        holder.scope.launch(Dispatchers.Default) {
            native.subscriptionStatus.collect { status ->
                holder.subscriptionStatus.value = status.toKmp()
            }
        }
    }

    // ---- Logging -----------------------------------------------------------

    override fun getLogLevel(): LogLevel = NativeSuperwall.instance.logLevel.toKmp()

    override fun setLogLevel(level: LogLevel) {
        NativeSuperwall.instance.logLevel = level.toNative()
    }

    // ---- Identity / attributes ----------------------------------------------

    override fun getUserId(): String = NativeSuperwall.instance.userId

    override fun isLoggedIn(): Boolean = NativeSuperwall.instance.isLoggedIn

    override fun identify(
        userId: String,
        options: IdentityOptions?,
    ) {
        NativeSuperwall.instance.nativeIdentify(
            userId = userId,
            options = options?.let { NativeIdentityOptions(it.restorePaywallAssignments) },
        )
    }

    override fun getUserAttributes(): Map<String, Any?> =
        sanitizeParams(NativeSuperwall.instance.userAttributes) ?: emptyMap()

    override fun setUserAttributes(attributes: Map<String, Any?>) {
        // Native merge semantics: null removes the key, absent keys untouched.
        NativeSuperwall.instance.nativeSetUserAttributes(attributes)
    }

    override fun setIntegrationAttribute(
        attribute: IntegrationAttribute,
        value: String?,
    ) {
        setIntegrationAttributes(mapOf(attribute to value))
    }

    override fun setIntegrationAttributes(attributes: Map<IntegrationAttribute, String?>) {
        integrationAttributes.set(attributes)
    }

    override fun getIntegrationAttributes(): Map<IntegrationAttribute, String?> = integrationAttributes.get()

    override suspend fun getDeviceAttributes(): Map<String, Any?> =
        withContext(Dispatchers.IO) {
            sanitizeParams(NativeSuperwall.instance.deviceAttributes()) ?: emptyMap()
        }

    override fun getLocaleIdentifier(): String? = NativeSuperwall.instance.localeIdentifier

    override fun setLocaleIdentifier(localeIdentifier: String?) {
        NativeSuperwall.instance.localeIdentifier = localeIdentifier
    }

    // ---- Entitlements / subscription ----------------------------------------

    override fun getEntitlements(): Entitlements = NativeSuperwall.instance.entitlements.toKmp()

    /**
     * Calls the REAL native `Entitlements.byProductIds(Set<String>)` —
     * verified present in superwall-android 2.10.2 (`store/Entitlements.kt:134`).
     * The Flutter host's local-filter stub ("Android SDK doesn't have
     * byProductIds yet") is obsolete against this SDK version.
     */
    override suspend fun getEntitlementsByProductIds(productIds: Set<String>): Set<Entitlement> =
        NativeSuperwall.instance.entitlements
            .byProductIds(productIds)
            .map { it.toKmp() }
            .toSet()

    override fun getSubscriptionStatus(): SubscriptionStatus =
        NativeSuperwall.instance.subscriptionStatus.value
            .toKmp()

    override fun setSubscriptionStatus(status: SubscriptionStatus) {
        NativeSuperwall.instance.setSubscriptionStatus(status.toNative())
    }

    /**
     * Returns the REAL native `Superwall.getCustomerInfo()` — verified present
     * in superwall-android 2.10.2 (`Superwall.kt:275`, a synchronous StateFlow
     * read). The Flutter host's synthesized minimal stub is obsolete against
     * this SDK version.
     */
    override suspend fun getCustomerInfo(): CustomerInfo = NativeSuperwall.instance.getCustomerInfo().toKmp()

    /** superwall-android has no customer info refresh; this is the current value. */
    override suspend fun refreshCustomerInfo(): CustomerInfo = getCustomerInfo()

    /** Granted entitlements are an iOS-only concept: always empty here. */
    override fun getGrantedEntitlements(): Set<Entitlement> = emptySet()

    /** Granted entitlements are an iOS-only concept: a no-op here. */
    override fun setGrantedEntitlements(entitlements: Set<Entitlement>) {}

    /** A synchronous read of the stored assignments (`AssignmentService.getAssignments`). */
    override fun getAssignments(): Set<ConfirmedAssignment> =
        NativeSuperwall.instance
            .getAssignments()
            .getOrElse { throwable ->
                throw SuperwallError.Native(
                    throwable.localizedMessage ?: throwable.message,
                    throwable,
                )
            }.map { it.toKmp() }
            .toSet()

    override suspend fun confirmAllAssignments(): Set<ConfirmedAssignment> =
        withContext(Dispatchers.IO) {
            NativeSuperwall.instance
                .confirmAllAssignments()
                .getOrElse { throwable ->
                    throw SuperwallError.Native(
                        throwable.localizedMessage ?: throwable.message,
                        throwable,
                    )
                }.map { it.toKmp() }
                .toSet()
        }

 /** Restoration failure stays in the domain type — never throws. */
    override suspend fun restorePurchases(): RestorationResult =
        withContext(Dispatchers.IO) {
            NativeSuperwall.instance.restorePurchases().fold(
                onSuccess = { it.toKmp() },
                onFailure = { throwable ->
                    RestorationResult.Failed(
                        throwable.localizedMessage ?: throwable.message ?: "Unknown error",
                    )
                },
            )
        }

    /**
     * `Superwall.purchase(productId)` (verified signature: superwall-android
     * 2.10.2 `Superwall.kt:985`). Its `Result.failure` (an unknown product,
     * no activity, a billing error) becomes [PurchaseResult.Failed], as iOS
     * reports failures in the result.
     */
    override suspend fun purchase(productId: String): PurchaseResult =
        withContext(Dispatchers.IO) {
            NativeSuperwall.instance.purchase(productId).fold(
                onSuccess = { it.toKmp() },
                onFailure = { throwable ->
                    PurchaseResult.Failed(
                        throwable.localizedMessage ?: throwable.message ?: "Unknown error",
                    )
                },
            )
        }

    /**
     * `Superwall.getProducts(vararg productIds)` (`Superwall.kt:993`), keyed
     * by the requested (full) identifier.
     */
    override suspend fun getProducts(productIds: Set<String>): Map<String, StoreProduct> =
        withContext(Dispatchers.IO) {
            NativeSuperwall.instance
                .getProducts(*productIds.toTypedArray())
                .getOrElse { throwable ->
                    throw SuperwallError.Native(
                        throwable.localizedMessage ?: throwable.message,
                        throwable,
                    )
                }.mapValues { (_, product) -> product.toKmp() }
        }

    // ---- Presentation --------------------------------------------------------

    /**
     * Fire-and-forget, matching the native `Superwall.register` extension.
     * Creates a PER-CALL [PresentationHandlerAdapter] that strongly retains
     * the handler and feature — no placement-keyed registry (fixes the Flutter
 * one-handler-per-placement aliasing).
     */
    override fun registerPlacement(
        placement: String,
        params: Map<String, Any?>?,
        handler: PaywallPresentationHandler?,
        feature: (() -> Unit)?,
    ) {
        val adapter =
            if (handler != null || feature != null) {
                PresentationHandlerAdapter(
                    handler = handler,
                    feature = feature,
                    scope = ::callbackScope,
                    log = ::logError,
                )
            } else {
                null
            }
        NativeSuperwall.instance.nativeRegister(
            placement = placement,
            params = params.toNonNullParams(),
            handler = adapter?.nativeHandler,
            feature = adapter?.nativeFeature,
        )
    }

    override suspend fun getPresentationResult(
        placement: String,
        params: Map<String, Any?>?,
    ): PresentationResult =
        withContext(Dispatchers.IO) {
            NativeSuperwall.instance
                .nativeGetPresentationResult(placement, params.toNonNullParams())
                .getOrElse { throwable ->
                    throw SuperwallError.Native(
                        throwable.localizedMessage ?: throwable.message,
                        throwable,
                    )
                }.toKmp()
        }

    override suspend fun dismiss() {
        withContext(Dispatchers.IO) {
            NativeSuperwall.instance.nativeDismiss()
        }
    }

    override fun isPaywallPresented(): Boolean = NativeSuperwall.instance.isPaywallPresented

    override fun getLatestPaywallInfo(): PaywallInfo? = NativeSuperwall.instance.latestPaywallInfo?.toKmp()

    override fun preloadAllPaywalls() {
        NativeSuperwall.instance.preloadAllPaywalls()
    }

    override fun preloadPaywallsForPlacements(placementNames: Set<String>) {
        NativeSuperwall.instance.preloadPaywalls(placementNames)
    }

    /**
     * Static on the native side precisely so it is callable pre-configure
     * (guard-exempt in the facade). Flutter-host parity: a native
     * `Result.failure` degrades to `false`.
     */
    override fun handleDeepLink(url: String): Boolean = NativeSuperwall.handleDeepLink(Uri.parse(url)).getOrNull() ?: false

    override fun togglePaywallSpinner(isHidden: Boolean) {
        NativeSuperwall.instance.togglePaywallSpinner(isHidden)
    }

    // ---- Products / misc ------------------------------------------------------

    override fun getOverrideProductsByName(): Map<String, String>? =
        NativeSuperwall.instance.overrideProductsByName.takeIf { it.isNotEmpty() }

    override fun setOverrideProductsByName(overrideProducts: Map<String, String>?) {
        NativeSuperwall.instance.overrideProductsByName = overrideProducts ?: emptyMap()
    }

    /**
     * The native enum only has LIGHT/DARK (`DeviceHelper.kt:64`); "follow the
     * system" is the native `null`, so both common `AUTOMATIC` and `null` map
     * to it.
     */
    override fun setInterfaceStyle(style: InterfaceStyle?) {
        NativeSuperwall.instance.setInterfaceStyle(
            when (style) {
                InterfaceStyle.LIGHT -> NativeInterfaceStyle.LIGHT
                InterfaceStyle.DARK -> NativeInterfaceStyle.DARK
                InterfaceStyle.AUTOMATIC, null -> null
            },
        )
    }

    override fun getAdConsent(): AdConsent? = NativeSuperwall.instance.adConsent.toKmp()

    override fun setAdConsent(consent: AdConsent) {
        NativeSuperwall.instance.adConsent = consent.toNative()
    }

    override fun refreshConfiguration() {
        NativeSuperwall.instance.refreshConfiguration()
    }

    override fun getEventTrackingBehavior(): EventTrackingBehavior = NativeSuperwall.instance.eventTrackingBehavior.toKmp()

    override fun setEventTrackingBehavior(behavior: EventTrackingBehavior) {
        NativeSuperwall.instance.eventTrackingBehavior = behavior.toNative()
    }

    override fun cancelAllScheduledNotifications() {
        NativeSuperwall.instance.cancelAllScheduledNotifications()
    }

    override fun setPushToken(token: String) {
        NativeSuperwall.instance.setPushToken(token)
    }

    override fun setNotificationsEnabled(enabled: Boolean) {
        if (enabled) {
            NativeSuperwall.instance.optInToNotifications()
        } else {
            NativeSuperwall.instance.optOutOfNotifications()
        }
    }

    override fun handleNotificationOpen(data: Map<String, String>) {
        NativeSuperwall.instance.handleNotificationOpen(data)
    }

    /**
 * Actually wired (the equivalent Flutter option was dead):
     * flips the live native option — it is read per device-attribute
     * computation, so post-configure changes take effect.
     */
    override fun enableExperimentalDeviceVariables(enabled: Boolean) {
        NativeSuperwall.instance.options.enableExperimentalDeviceVariables = enabled
    }

    override suspend fun consume(purchaseToken: String): String =
        withContext(Dispatchers.IO) {
            NativeSuperwall.instance.consume(purchaseToken).getOrElse { throwable ->
                throw SuperwallError.Native(
                    throwable.localizedMessage ?: throwable.message,
                    throwable,
                )
            }
        }

    private companion object {
        const val TAG = "SuperwallKMP"
    }
}

/**
 * Sanitizes a common params map to the native `Map<String, Any>` contract:
 * values normalized per the documented value contract, `null` values dropped
 * (the native extension APIs take non-null values).
 */
private fun Map<String, Any?>?.toNonNullParams(): Map<String, Any>? {
    val sanitized = sanitizeParams(this) ?: return null
    return sanitized
        .filterValues { it != null }
        .mapValues { (_, value) -> value as Any }
}
