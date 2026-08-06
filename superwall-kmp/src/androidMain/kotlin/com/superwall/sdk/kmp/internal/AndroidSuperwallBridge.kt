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
import kotlinx.coroutines.withContext
import com.superwall.sdk.Superwall as NativeSuperwall
import com.superwall.sdk.config.models.ConfigurationStatus as NativeConfigurationStatus
import com.superwall.sdk.config.options.SuperwallOptions as NativeSuperwallOptions
import com.superwall.sdk.identity.IdentityOptions as NativeIdentityOptions
import com.superwall.sdk.identity.identify as nativeIdentify
import com.superwall.sdk.identity.setUserAttributes as nativeSetUserAttributes
import com.superwall.sdk.models.attribution.AttributionProvider as NativeAttributionProvider
import com.superwall.sdk.network.device.InterfaceStyle as NativeInterfaceStyle
import com.superwall.sdk.paywall.presentation.dismiss as nativeDismiss
import com.superwall.sdk.paywall.presentation.get_presentation_result.getPresentationResult as nativeGetPresentationResult
import com.superwall.sdk.paywall.presentation.register as nativeRegister

/**
 * The Android [SuperwallBridge]: a port of the Flutter plugin's
 * `SuperwallHost.kt` minus all Pigeon transport, calling superwall-android
 * 2.7.11 directly (every native symbol verified against the 2.7.11 sources).
 *
 * Deliberate fixes over the Flutter host
 * - `configure`'s completion forwards the REAL native `Result<Unit>` instead
 *   of dropping the success bool (`ConfigureCompletionProxy` bug), and no
 *   longer force-overwrites `logging.level = debug`.
 * - `registerPlacement` creates a fresh [PresentationHandlerAdapter] per call
 *   — no placement-keyed registry, no one-handler-per-placement aliasing.
 * - `getEntitlementsByProductIds` calls the REAL native
 *   `Entitlements.byProductIds` (present in 2.7.11; the Flutter host's
 *   "Android SDK doesn't have it" local-filter stub is obsolete).
 * - `getCustomerInfo` returns the REAL native `Superwall.getCustomerInfo()`
 *   (present in 2.7.11; the Flutter host synthesized a minimal stub).
 * - Integration attributes use the native
 *   `setIntegrationAttributes(Map<AttributionProvider, String>)` API instead
 *   of the Flutter host's `$`-prefixed user-attribute workaround, with
 *   merge/null-removes semantics implemented over the native replace-all call.
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

    // ---- Configuration -----------------------------------------------------

    /**
     * Calls the static
     * `NativeSuperwall.configure(applicationContext, apiKey,
     * purchaseController, options, activityProvider, completion)`
     * (verified signature: superwall-android 2.8.0 `Superwall.kt:526`).
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

        // The native singleton exists synchronously after configure; install
        // the delegate now so no early event is missed.
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
     * 2.7.11's `SuperwallDelegate.customerInfoDidChange` hook exists and is
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

    /**
     * The native `setIntegrationAttributes` REPLACES the whole map
     * (`AttributionManager.kt`), so the common merge/null-removes contract is
     * implemented by reading the current native attributes, applying the
     * changes, and writing the merged map back.
     */
    override fun setIntegrationAttributes(attributes: Map<IntegrationAttribute, String?>) {
        val merged = currentNativeIntegrationAttributes().toMutableMap()
        for ((attribute, value) in attributes) {
            val native = attribute.toNativeAttributionProvider()
            if (native == null) {
                logError(
                    "Integration attribute ${attribute.name} is not supported by " +
                        "superwall-android and was skipped.",
                    error = null,
                )
                continue
            }
            if (value == null) merged.remove(native) else merged[native] = value
        }
        NativeSuperwall.instance.setIntegrationAttributes(merged)
    }

    override fun getIntegrationAttributes(): Map<IntegrationAttribute, String?> =
        currentNativeIntegrationAttributes()
            .entries
            .mapNotNull { (provider, value) ->
                provider.toKmpIntegrationAttribute()?.let { it to value }
            }.toMap()

    /**
     * Reads the native raw-name-keyed attribute map back into provider keys.
     * Raw names that don't correspond to a known provider are dropped
     * (degrade, never crash).
     */
    private fun currentNativeIntegrationAttributes(): Map<NativeAttributionProvider, String> =
        NativeSuperwall.instance.integrationAttributes
            .entries
            .mapNotNull { (rawName, value) ->
                NativeAttributionProvider.entries
                    .firstOrNull { it.rawName == rawName }
                    ?.let { it to value }
            }.toMap()

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
     * verified present in superwall-android 2.8.0 (`store/Entitlements.kt:213`).
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
     * in superwall-android 2.8.0 (`Superwall.kt:245`, a synchronous StateFlow
     * read). The Flutter host's synthesized minimal stub is obsolete against
     * this SDK version.
     */
    override suspend fun getCustomerInfo(): CustomerInfo = NativeSuperwall.instance.getCustomerInfo().toKmp()

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

// ---- IntegrationAttribute <-> AttributionProvider ----------------------------

/**
 * Maps the common [IntegrationAttribute] to the native
 * [NativeAttributionProvider], or `null` for the iOS-only
 * [IntegrationAttribute.FIREBASE_INSTALLATION_ID] (superwall-android 2.8.0
 * has no counterpart — the caller skips it with a logged warning; the native
 * enum's extra values — META, AMPLITUDE, MIXPANEL, GOOGLE_ADS, GOOGLE_APP_SET,
 * CUSTOM — have no common counterpart and surface only through
 * [toKmpIntegrationAttribute] as `null`).
 */
private fun IntegrationAttribute.toNativeAttributionProvider(): NativeAttributionProvider? =
    when (this) {
        IntegrationAttribute.FIREBASE_INSTALLATION_ID -> null
        IntegrationAttribute.SINGULAR_DEVICE_ID -> NativeAttributionProvider.SINGULAR_DEVICE_ID
        IntegrationAttribute.ADJUST_ID -> NativeAttributionProvider.ADJUST_ID
        IntegrationAttribute.AMPLITUDE_DEVICE_ID -> NativeAttributionProvider.AMPLITUDE_DEVICE_ID
        IntegrationAttribute.AMPLITUDE_USER_ID -> NativeAttributionProvider.AMPLITUDE_USER_ID
        IntegrationAttribute.APPSFLYER_ID -> NativeAttributionProvider.APPSFLYER_ID
        IntegrationAttribute.BRAZE_ALIAS_NAME -> NativeAttributionProvider.BRAZE_ALIAS_NAME
        IntegrationAttribute.BRAZE_ALIAS_LABEL -> NativeAttributionProvider.BRAZE_ALIAS_LABEL
        IntegrationAttribute.ONESIGNAL_ID -> NativeAttributionProvider.ONESIGNAL_ID
        IntegrationAttribute.FB_ANON_ID -> NativeAttributionProvider.FB_ANON_ID
        IntegrationAttribute.FIREBASE_APP_INSTANCE_ID ->
            NativeAttributionProvider.FIREBASE_APP_INSTANCE_ID
        IntegrationAttribute.ITERABLE_USER_ID -> NativeAttributionProvider.ITERABLE_USER_ID
        IntegrationAttribute.ITERABLE_CAMPAIGN_ID -> NativeAttributionProvider.ITERABLE_CAMPAIGN_ID
        IntegrationAttribute.ITERABLE_TEMPLATE_ID -> NativeAttributionProvider.ITERABLE_TEMPLATE_ID
        IntegrationAttribute.MIXPANEL_DISTINCT_ID -> NativeAttributionProvider.MIXPANEL_DISTINCT_ID
        IntegrationAttribute.MPARTICLE_ID -> NativeAttributionProvider.MPARTICLE_ID
        IntegrationAttribute.CLEVERTAP_ID -> NativeAttributionProvider.CLEVERTAP_ID
        IntegrationAttribute.AIRSHIP_CHANNEL_ID -> NativeAttributionProvider.AIRSHIP_CHANNEL_ID
        IntegrationAttribute.KOCHAVA_DEVICE_ID -> NativeAttributionProvider.KOCHAVA_DEVICE_ID
        IntegrationAttribute.TENJIN_ID -> NativeAttributionProvider.TENJIN_ID
        IntegrationAttribute.POSTHOG_USER_ID -> NativeAttributionProvider.POSTHOG_USER_ID
        IntegrationAttribute.CUSTOMERIO_ID -> NativeAttributionProvider.CUSTOMERIO_ID
        IntegrationAttribute.APPSTACK_ID -> NativeAttributionProvider.APPSTACK
    }

/**
 * Maps a native provider back to the common enum, or `null` when the native
 * value has no common counterpart (dropped — degrade, never crash).
 */
private fun NativeAttributionProvider.toKmpIntegrationAttribute(): IntegrationAttribute? =
    when (this) {
        NativeAttributionProvider.SINGULAR_DEVICE_ID -> IntegrationAttribute.SINGULAR_DEVICE_ID
        NativeAttributionProvider.ADJUST_ID -> IntegrationAttribute.ADJUST_ID
        NativeAttributionProvider.AMPLITUDE_DEVICE_ID -> IntegrationAttribute.AMPLITUDE_DEVICE_ID
        NativeAttributionProvider.AMPLITUDE_USER_ID -> IntegrationAttribute.AMPLITUDE_USER_ID
        NativeAttributionProvider.APPSFLYER_ID -> IntegrationAttribute.APPSFLYER_ID
        NativeAttributionProvider.BRAZE_ALIAS_NAME -> IntegrationAttribute.BRAZE_ALIAS_NAME
        NativeAttributionProvider.BRAZE_ALIAS_LABEL -> IntegrationAttribute.BRAZE_ALIAS_LABEL
        NativeAttributionProvider.ONESIGNAL_ID -> IntegrationAttribute.ONESIGNAL_ID
        NativeAttributionProvider.FB_ANON_ID -> IntegrationAttribute.FB_ANON_ID
        NativeAttributionProvider.FIREBASE_APP_INSTANCE_ID ->
            IntegrationAttribute.FIREBASE_APP_INSTANCE_ID
        NativeAttributionProvider.ITERABLE_USER_ID -> IntegrationAttribute.ITERABLE_USER_ID
        NativeAttributionProvider.ITERABLE_CAMPAIGN_ID -> IntegrationAttribute.ITERABLE_CAMPAIGN_ID
        NativeAttributionProvider.ITERABLE_TEMPLATE_ID -> IntegrationAttribute.ITERABLE_TEMPLATE_ID
        NativeAttributionProvider.MIXPANEL_DISTINCT_ID -> IntegrationAttribute.MIXPANEL_DISTINCT_ID
        NativeAttributionProvider.MPARTICLE_ID -> IntegrationAttribute.MPARTICLE_ID
        NativeAttributionProvider.CLEVERTAP_ID -> IntegrationAttribute.CLEVERTAP_ID
        NativeAttributionProvider.AIRSHIP_CHANNEL_ID -> IntegrationAttribute.AIRSHIP_CHANNEL_ID
        NativeAttributionProvider.KOCHAVA_DEVICE_ID -> IntegrationAttribute.KOCHAVA_DEVICE_ID
        NativeAttributionProvider.TENJIN_ID -> IntegrationAttribute.TENJIN_ID
        NativeAttributionProvider.POSTHOG_USER_ID -> IntegrationAttribute.POSTHOG_USER_ID
        NativeAttributionProvider.CUSTOMERIO_ID -> IntegrationAttribute.CUSTOMERIO_ID
        NativeAttributionProvider.APPSTACK -> IntegrationAttribute.APPSTACK_ID
        NativeAttributionProvider.META,
        NativeAttributionProvider.AMPLITUDE,
        NativeAttributionProvider.MIXPANEL,
        NativeAttributionProvider.GOOGLE_ADS,
        NativeAttributionProvider.GOOGLE_APP_SET,
        NativeAttributionProvider.CUSTOM,
        -> null
    }
