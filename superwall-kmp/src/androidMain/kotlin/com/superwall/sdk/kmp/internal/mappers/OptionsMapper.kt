package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.options.DeviceTier
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.Logging
import com.superwall.sdk.kmp.models.options.NetworkEnvironment
import com.superwall.sdk.kmp.models.options.PaywallOptions
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.options.TestModeBehavior
import com.superwall.sdk.kmp.models.options.TransactionBackgroundView
import java.util.EnumSet
import com.superwall.sdk.analytics.Tier as NativeTier
import com.superwall.sdk.config.options.PaywallOptions as NativePaywallOptions
import com.superwall.sdk.config.options.SuperwallOptions as NativeSuperwallOptions
import com.superwall.sdk.logger.LogLevel as NativeLogLevel
import com.superwall.sdk.logger.LogScope as NativeLogScope
import com.superwall.sdk.store.testmode.TestModeBehavior as NativeTestModeBehavior

/**
 * Maps the common [SuperwallOptions] to the native SDK's mutable
 * [NativeSuperwallOptions]. Port of the Flutter host's
 * `PSuperwallOptions.toSdkOptions()` (utils/OptionsMapper.kt) with two
 * deliberate deltas:
 *
 * - `enableExperimentalDeviceVariables` is actually wired (dead in the
 *   Flutter layer — plan §3.4).
 * - iOS-only options (`shouldBypassAppTransactionCheck`, `maxConfigRetryCount`,
 *   `shouldShowWebRestorationAlert`, `shouldShowWebPurchaseConfirmationAlert`,
 *   `transactionBackgroundView` semantics aside) are documented no-ops here
 *   because superwall-android 2.7.11 has no equivalent fields.
 *
 * `PaywallOptions.onBackPressed` is deliberately NOT wired here: the bridge
 * installs an `OnBackPressedAdapter` on the returned native options so the
 * synchronous-callback threading rules (plan §6.1) live in one place.
 */
internal fun SuperwallOptions.toNative(): NativeSuperwallOptions {
    val native = NativeSuperwallOptions()
    native.paywalls = paywalls.toNative()
    native.networkEnvironment =
        when (networkEnvironment) {
            NetworkEnvironment.RELEASE -> NativeSuperwallOptions.NetworkEnvironment.Release()
            NetworkEnvironment.RELEASE_CANDIDATE ->
                NativeSuperwallOptions.NetworkEnvironment.ReleaseCandidate()
            NetworkEnvironment.DEVELOPER -> NativeSuperwallOptions.NetworkEnvironment.Developer()
        }
    native.isExternalDataCollectionEnabled = isExternalDataCollectionEnabled
    native.localeIdentifier = localeIdentifier
    native.isGameControllerEnabled = isGameControllerEnabled
    native.enableExperimentalDeviceVariables = enableExperimentalDeviceVariables
    native.passIdentifiersToPlayStore = passIdentifiersToPlayStore
    native.testModeBehavior = testModeBehavior.toNative()
    native.shouldObservePurchases = shouldObservePurchases
    native.useMockReviews = useMockReviews
    native.logging = logging.toNative()
    // shouldBypassAppTransactionCheck / maxConfigRetryCount: iOS-only, no
    // superwall-android counterpart — documented no-op.
    return native
}

/** Maps the common [PaywallOptions] to the native SDK's [NativePaywallOptions]. */
internal fun PaywallOptions.toNative(): NativePaywallOptions {
    val native = NativePaywallOptions()
    native.isHapticFeedbackEnabled = isHapticFeedbackEnabled
    native.restoreFailed =
        NativePaywallOptions.RestoreFailed().also { alert ->
            alert.title = restoreFailed.title
            alert.message = restoreFailed.message
            alert.closeButtonTitle = restoreFailed.closeButtonTitle
        }
    native.shouldShowPurchaseFailureAlert = shouldShowPurchaseFailureAlert
    native.shouldPreload = shouldPreload
    native.preloadDeviceOverrides = preloadDeviceOverrides.mapKeys { (tier, _) -> tier.toNative() }
    native.automaticallyDismiss = automaticallyDismiss
    // The native enum only has SPINNER; the common NONE case maps to the
    // native nullable `transactionBackgroundView = null` ("show nothing").
    native.transactionBackgroundView =
        when (transactionBackgroundView) {
            TransactionBackgroundView.SPINNER -> NativePaywallOptions.TransactionBackgroundView.SPINNER
            TransactionBackgroundView.NONE -> null
        }
    native.overrideProductsByName = overrideProductsByName ?: emptyMap()
    // onBackPressed: installed by the bridge via OnBackPressedAdapter (see KDoc above).
    // shouldShowWebRestorationAlert / shouldShowWebPurchaseConfirmationAlert:
    // iOS-only, no superwall-android counterpart — documented no-op.
    return native
}

/** Maps the common [Logging] configuration to the native nested Logging object. */
internal fun Logging.toNative(): NativeSuperwallOptions.Logging {
    val native = NativeSuperwallOptions.Logging()
    native.level = level.toNative()
    val nativeScopes = EnumSet.noneOf(NativeLogScope::class.java)
    scopes.forEach { nativeScopes.add(it.toNative()) }
    if (nativeScopes.isNotEmpty()) {
        native.scopes = nativeScopes
    }
    return native
}

// ---- TestModeBehavior -------------------------------------------------------

internal fun TestModeBehavior.toNative(): NativeTestModeBehavior =
    when (this) {
        TestModeBehavior.AUTOMATIC -> NativeTestModeBehavior.AUTOMATIC
        TestModeBehavior.WHEN_ENABLED_FOR_USER -> NativeTestModeBehavior.WHEN_ENABLED_FOR_USER
        TestModeBehavior.NEVER -> NativeTestModeBehavior.NEVER
        TestModeBehavior.ALWAYS -> NativeTestModeBehavior.ALWAYS
    }

internal fun NativeTestModeBehavior.toKmp(): TestModeBehavior =
    when (this) {
        NativeTestModeBehavior.AUTOMATIC -> TestModeBehavior.AUTOMATIC
        NativeTestModeBehavior.WHEN_ENABLED_FOR_USER -> TestModeBehavior.WHEN_ENABLED_FOR_USER
        NativeTestModeBehavior.NEVER -> TestModeBehavior.NEVER
        NativeTestModeBehavior.ALWAYS -> TestModeBehavior.ALWAYS
    }

// ---- DeviceTier --------------------------------------------------------------

internal fun DeviceTier.toNative(): NativeTier =
    when (this) {
        DeviceTier.ULTRA_LOW -> NativeTier.ULTRA_LOW
        DeviceTier.LOW -> NativeTier.LOW
        DeviceTier.MID -> NativeTier.MID
        DeviceTier.HIGH -> NativeTier.HIGH
        DeviceTier.ULTRA_HIGH -> NativeTier.ULTRA_HIGH
        DeviceTier.UNKNOWN -> NativeTier.UNKNOWN
    }

internal fun NativeTier.toKmp(): DeviceTier =
    when (this) {
        NativeTier.ULTRA_LOW -> DeviceTier.ULTRA_LOW
        NativeTier.LOW -> DeviceTier.LOW
        NativeTier.MID -> DeviceTier.MID
        NativeTier.HIGH -> DeviceTier.HIGH
        NativeTier.ULTRA_HIGH -> DeviceTier.ULTRA_HIGH
        NativeTier.UNKNOWN -> DeviceTier.UNKNOWN
    }

// ---- LogLevel ----------------------------------------------------------------

internal fun LogLevel.toNative(): NativeLogLevel =
    when (this) {
        LogLevel.DEBUG -> NativeLogLevel.debug
        LogLevel.INFO -> NativeLogLevel.info
        LogLevel.WARN -> NativeLogLevel.warn
        LogLevel.ERROR -> NativeLogLevel.error
        LogLevel.NONE -> NativeLogLevel.none
    }

internal fun NativeLogLevel.toKmp(): LogLevel =
    when (this) {
        NativeLogLevel.debug -> LogLevel.DEBUG
        NativeLogLevel.info -> LogLevel.INFO
        NativeLogLevel.warn -> LogLevel.WARN
        NativeLogLevel.error -> LogLevel.ERROR
        NativeLogLevel.none -> LogLevel.NONE
    }

/**
 * Maps a native log-level string (the native delegate's `handleLog` passes
 * `level.toString()`, e.g. `"DEBUG"`) to the common [LogLevel]. Returns `null`
 * when unmappable so the caller can apply the documented degrade rule
 * ([LogLevel.DEBUG] + `info["rawLevel"]`, plan §3.4).
 */
internal fun logLevelFromNativeString(raw: String): LogLevel? =
    when (raw.trim().uppercase()) {
        "DEBUG" -> LogLevel.DEBUG
        "INFO" -> LogLevel.INFO
        "WARN" -> LogLevel.WARN
        "ERROR" -> LogLevel.ERROR
        "NONE" -> LogLevel.NONE
        else -> null
    }

// ---- LogScope ----------------------------------------------------------------

internal fun LogScope.toNative(): NativeLogScope =
    when (this) {
        LogScope.LOCALIZATION_MANAGER -> NativeLogScope.localizationManager
        LogScope.BOUNCE_BUTTON -> NativeLogScope.bounceButton
        LogScope.CORE_DATA -> NativeLogScope.coreData
        LogScope.CONFIG_MANAGER -> NativeLogScope.configManager
        LogScope.IDENTITY_MANAGER -> NativeLogScope.identityManager
        LogScope.DEBUG_MANAGER -> NativeLogScope.debugManager
        LogScope.DEBUG_VIEW_CONTROLLER -> NativeLogScope.debugView
        LogScope.LOCALIZATION_VIEW_CONTROLLER -> NativeLogScope.localizationView
        LogScope.GAME_CONTROLLER_MANAGER -> NativeLogScope.gameControllerManager
        LogScope.DEVICE -> NativeLogScope.device
        LogScope.NETWORK -> NativeLogScope.network
        LogScope.PAYWALL_EVENTS -> NativeLogScope.paywallEvents
        LogScope.PRODUCTS_MANAGER -> NativeLogScope.productsManager
        LogScope.STORE_KIT_MANAGER -> NativeLogScope.storeKitManager
        LogScope.PLACEMENTS -> NativeLogScope.placements
        LogScope.RECEIPTS -> NativeLogScope.receipts
        LogScope.SUPERWALL_CORE -> NativeLogScope.superwallCore
        LogScope.PAYWALL_PRESENTATION -> NativeLogScope.paywallPresentation
        LogScope.TRANSACTIONS -> NativeLogScope.transactions
        LogScope.PAYWALL_VIEW_CONTROLLER -> NativeLogScope.paywallView
        LogScope.CACHE -> NativeLogScope.cache
        LogScope.ALL -> NativeLogScope.all
    }

/**
 * Maps a native [NativeLogScope] to the common [LogScope], or `null` when the
 * native scope has no common counterpart (superwall-android 2.7.11 carries
 * `webEntitlements`, `customerInfo`, `jsEvaluator`, `paywallTransactions`,
 * `nativePurchaseController`, `deepLinks`, which the 22-value common enum
 * lacks). Callers apply the documented degrade rule ([LogScope.ALL] +
 * `info["rawScope"]`, plan §3.4).
 */
internal fun NativeLogScope.toKmp(): LogScope? =
    when (this) {
        NativeLogScope.localizationManager -> LogScope.LOCALIZATION_MANAGER
        NativeLogScope.bounceButton -> LogScope.BOUNCE_BUTTON
        NativeLogScope.coreData -> LogScope.CORE_DATA
        NativeLogScope.configManager -> LogScope.CONFIG_MANAGER
        NativeLogScope.identityManager -> LogScope.IDENTITY_MANAGER
        NativeLogScope.debugManager -> LogScope.DEBUG_MANAGER
        NativeLogScope.debugView -> LogScope.DEBUG_VIEW_CONTROLLER
        NativeLogScope.localizationView -> LogScope.LOCALIZATION_VIEW_CONTROLLER
        NativeLogScope.gameControllerManager -> LogScope.GAME_CONTROLLER_MANAGER
        NativeLogScope.device -> LogScope.DEVICE
        NativeLogScope.network -> LogScope.NETWORK
        NativeLogScope.paywallEvents -> LogScope.PAYWALL_EVENTS
        NativeLogScope.productsManager -> LogScope.PRODUCTS_MANAGER
        NativeLogScope.storeKitManager -> LogScope.STORE_KIT_MANAGER
        NativeLogScope.placements -> LogScope.PLACEMENTS
        NativeLogScope.receipts -> LogScope.RECEIPTS
        NativeLogScope.superwallCore -> LogScope.SUPERWALL_CORE
        NativeLogScope.paywallPresentation -> LogScope.PAYWALL_PRESENTATION
        NativeLogScope.transactions -> LogScope.TRANSACTIONS
        NativeLogScope.paywallView -> LogScope.PAYWALL_VIEW_CONTROLLER
        NativeLogScope.cache -> LogScope.CACHE
        NativeLogScope.all -> LogScope.ALL
        // No common counterpart in the 22-value LogScope enum:
        NativeLogScope.webEntitlements,
        NativeLogScope.customerInfo,
        NativeLogScope.jsEvaluator,
        NativeLogScope.paywallTransactions,
        NativeLogScope.nativePurchaseController,
        NativeLogScope.deepLinks,
        -> null
    }

/**
 * Maps a native log-scope string (the native delegate's `handleLog` passes
 * `scope.toString()`, which is the camelCase enum name, e.g.
 * `"paywallPresentation"`) to the common [LogScope]. Returns `null` when
 * unmappable so the caller can degrade to [LogScope.ALL] + `info["rawScope"]`.
 */
internal fun logScopeFromNativeString(raw: String): LogScope? {
    val trimmed = raw.trim()
    val native =
        NativeLogScope.entries.firstOrNull { it.name == trimmed }
            ?: NativeLogScope.entries.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
    return native?.toKmp()
}
