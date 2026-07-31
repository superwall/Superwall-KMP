@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.InterfaceStyle
import com.superwall.sdk.kmp.internal.ios.interop.SWBIdentityOptions
import com.superwall.sdk.kmp.internal.ios.interop.SWBInterfaceStyle
import com.superwall.sdk.kmp.internal.ios.interop.SWBInterfaceStyleAutomatic
import com.superwall.sdk.kmp.internal.ios.interop.SWBInterfaceStyleDark
import com.superwall.sdk.kmp.internal.ios.interop.SWBInterfaceStyleLight
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogLevel
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogLevelDebug
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogLevelError
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogLevelInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogLevelNone
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogLevelWarn
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScope
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeAll
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeBounceButton
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeCache
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeConfigManager
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeCoreData
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeDebugManager
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeDebugViewController
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeDevice
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeGameControllerManager
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeIdentityManager
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeLocalizationManager
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeLocalizationViewController
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeNetwork
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopePaywallEvents
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopePaywallPresentation
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopePaywallViewController
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopePlacements
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeProductsManager
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeReceipts
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeStoreKitManager
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeSuperwallCore
import com.superwall.sdk.kmp.internal.ios.interop.SWBLogScopeTransactions
import com.superwall.sdk.kmp.internal.ios.interop.SWBLoggingOptions
import com.superwall.sdk.kmp.internal.ios.interop.SWBNetworkEnvironmentDeveloper
import com.superwall.sdk.kmp.internal.ios.interop.SWBNetworkEnvironmentRelease
import com.superwall.sdk.kmp.internal.ios.interop.SWBNetworkEnvironmentReleaseCandidate
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallOptions
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestoreFailedOptions
import com.superwall.sdk.kmp.internal.ios.interop.SWBSuperwallOptions
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTrackingBehaviorAll
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTrackingBehaviorNone
import com.superwall.sdk.kmp.internal.ios.interop.SWBEventTrackingBehaviorSuperwallOnly
import com.superwall.sdk.kmp.internal.ios.interop.SWBTestModeBehaviorAlways
import com.superwall.sdk.kmp.internal.ios.interop.SWBTestModeBehaviorAutomatic
import com.superwall.sdk.kmp.internal.ios.interop.SWBTestModeBehaviorNever
import com.superwall.sdk.kmp.internal.ios.interop.SWBTestModeBehaviorWhenEnabledForUser
import com.superwall.sdk.kmp.internal.ios.interop.SWBTransactionBackgroundViewNone
import com.superwall.sdk.kmp.internal.ios.interop.SWBTransactionBackgroundViewSpinner
import com.superwall.sdk.kmp.models.identity.IdentityOptions
import com.superwall.sdk.kmp.models.options.EventTrackingBehavior
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.NetworkEnvironment
import com.superwall.sdk.kmp.models.options.PaywallOptions
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.options.TestModeBehavior
import com.superwall.sdk.kmp.models.options.TransactionBackgroundView
import platform.Foundation.NSNumber

// ---------------------------------------------------------------------------
// Options (common -> SWB). The facade always hands the bridge fully-defaulted
// common options, so every SWB field is set explicitly; the SWB envelopes'
// "nil = keep native default" convention is only exercised when the whole
// options object is absent. Android-only members (preloadDeviceOverrides,
// onBackPressed, passIdentifiersToPlayStore, useMockReviews) have no iOS
// counterpart and are intentionally not mapped.
// ---------------------------------------------------------------------------

internal fun SuperwallOptions.toSWB(): SWBSuperwallOptions {
    val options = SWBSuperwallOptions()
    options.paywalls = paywalls.toSWB()
    options.networkEnvironment = NSNumber(long = networkEnvironment.toSWBRaw())
    options.isExternalDataCollectionEnabled = NSNumber(bool = isExternalDataCollectionEnabled)
    // ALL is the common default and indistinguishable from "unset"; leaving
    // the SWB field nil lets the deprecated boolean keep its effect (the
    // native setter derives one from the other — same rule as androidMain).
    if (eventTrackingBehavior != EventTrackingBehavior.ALL) {
        options.eventTrackingBehavior = NSNumber(long = eventTrackingBehavior.toSWBRaw())
    }
    options.localeIdentifier = localeIdentifier
    options.isGameControllerEnabled = NSNumber(bool = isGameControllerEnabled)
    options.enableExperimentalDeviceVariables = NSNumber(bool = enableExperimentalDeviceVariables)
    options.testModeBehavior = NSNumber(long = testModeBehavior.toSWBRaw())
    options.shouldObservePurchases = NSNumber(bool = shouldObservePurchases)
    options.shouldBypassAppTransactionCheck = NSNumber(bool = shouldBypassAppTransactionCheck)
    options.maxConfigRetryCount = NSNumber(int = maxConfigRetryCount)
    options.logging = SWBLoggingOptions().also { swbLogging ->
        swbLogging.level = NSNumber(long = logLevelToSWB(logging.level))
        swbLogging.scopes = logging.scopes.map { NSNumber(long = logScopeToSWB(it)) }
    }
    return options
}

internal fun PaywallOptions.toSWB(): SWBPaywallOptions {
    val options = SWBPaywallOptions()
    options.isHapticFeedbackEnabled = NSNumber(bool = isHapticFeedbackEnabled)
    options.restoreFailed = SWBRestoreFailedOptions().also { swbRestoreFailed ->
        swbRestoreFailed.title = restoreFailed.title
        swbRestoreFailed.message = restoreFailed.message
        swbRestoreFailed.closeButtonTitle = restoreFailed.closeButtonTitle
    }
    options.shouldShowWebRestorationAlert = NSNumber(bool = shouldShowWebRestorationAlert)
    options.shouldShowPurchaseFailureAlert = NSNumber(bool = shouldShowPurchaseFailureAlert)
    options.shouldPreload = NSNumber(bool = shouldPreload)
    options.automaticallyDismiss = NSNumber(bool = automaticallyDismiss)
    options.transactionBackgroundView = NSNumber(
        long = when (transactionBackgroundView) {
            TransactionBackgroundView.SPINNER -> SWBTransactionBackgroundViewSpinner
            TransactionBackgroundView.NONE -> SWBTransactionBackgroundViewNone
        },
    )
    overrideProductsByName?.let { overrides ->
        @Suppress("UNCHECKED_CAST")
        options.overrideProductsByName = overrides as Map<Any?, *>
    }
    options.shouldShowWebPurchaseConfirmationAlert = NSNumber(bool = shouldShowWebPurchaseConfirmationAlert)
    return options
}

internal fun IdentityOptions.toSWB(): SWBIdentityOptions = SWBIdentityOptions(restorePaywallAssignments)

private fun NetworkEnvironment.toSWBRaw(): Long =
    when (this) {
        NetworkEnvironment.RELEASE -> SWBNetworkEnvironmentRelease
        NetworkEnvironment.RELEASE_CANDIDATE -> SWBNetworkEnvironmentReleaseCandidate
        NetworkEnvironment.DEVELOPER -> SWBNetworkEnvironmentDeveloper
    }

private fun EventTrackingBehavior.toSWBRaw(): Long =
    when (this) {
        EventTrackingBehavior.ALL -> SWBEventTrackingBehaviorAll
        EventTrackingBehavior.SUPERWALL_ONLY -> SWBEventTrackingBehaviorSuperwallOnly
        EventTrackingBehavior.NONE -> SWBEventTrackingBehaviorNone
    }

private fun TestModeBehavior.toSWBRaw(): Long =
    when (this) {
        TestModeBehavior.AUTOMATIC -> SWBTestModeBehaviorAutomatic
        TestModeBehavior.WHEN_ENABLED_FOR_USER -> SWBTestModeBehaviorWhenEnabledForUser
        TestModeBehavior.NEVER -> SWBTestModeBehaviorNever
        TestModeBehavior.ALWAYS -> SWBTestModeBehaviorAlways
    }

internal fun InterfaceStyle?.toSWB(): SWBInterfaceStyle =
    when (this) {
        null, InterfaceStyle.AUTOMATIC -> SWBInterfaceStyleAutomatic
        InterfaceStyle.LIGHT -> SWBInterfaceStyleLight
        InterfaceStyle.DARK -> SWBInterfaceStyleDark
    }

// ---------------------------------------------------------------------------
// Log level / scope. The SWB enum typealiases all collapse to NSInteger in
// Kotlin, so these are named functions rather than extensions to keep the
// enums from aliasing each other.
// ---------------------------------------------------------------------------

internal fun logLevelToSWB(level: LogLevel): SWBLogLevel =
    when (level) {
        LogLevel.DEBUG -> SWBLogLevelDebug
        LogLevel.INFO -> SWBLogLevelInfo
        LogLevel.WARN -> SWBLogLevelWarn
        LogLevel.ERROR -> SWBLogLevelError
        LogLevel.NONE -> SWBLogLevelNone
    }

internal fun logLevelFromSWB(level: SWBLogLevel): LogLevel =
    when (level) {
        SWBLogLevelDebug -> LogLevel.DEBUG
        SWBLogLevelInfo -> LogLevel.INFO
        SWBLogLevelWarn -> LogLevel.WARN
        SWBLogLevelError -> LogLevel.ERROR
        SWBLogLevelNone -> LogLevel.NONE
        // Unknown native case: documented fallback (plan §7).
        else -> LogLevel.DEBUG
    }

/**
 * Maps a common [LogScope] to the SWB raw value. The bridge's two 4.16.x-only
 * scopes (`analytics`, `webEntitlements`) have no common counterpart and only
 * appear in the read direction ([logScopeFromRawName]).
 */
internal fun logScopeToSWB(scope: LogScope): SWBLogScope =
    when (scope) {
        LogScope.LOCALIZATION_MANAGER -> SWBLogScopeLocalizationManager
        LogScope.BOUNCE_BUTTON -> SWBLogScopeBounceButton
        LogScope.CORE_DATA -> SWBLogScopeCoreData
        LogScope.CONFIG_MANAGER -> SWBLogScopeConfigManager
        LogScope.IDENTITY_MANAGER -> SWBLogScopeIdentityManager
        LogScope.DEBUG_MANAGER -> SWBLogScopeDebugManager
        LogScope.DEBUG_VIEW_CONTROLLER -> SWBLogScopeDebugViewController
        LogScope.LOCALIZATION_VIEW_CONTROLLER -> SWBLogScopeLocalizationViewController
        LogScope.GAME_CONTROLLER_MANAGER -> SWBLogScopeGameControllerManager
        LogScope.DEVICE -> SWBLogScopeDevice
        LogScope.NETWORK -> SWBLogScopeNetwork
        LogScope.PAYWALL_EVENTS -> SWBLogScopePaywallEvents
        LogScope.PRODUCTS_MANAGER -> SWBLogScopeProductsManager
        LogScope.STORE_KIT_MANAGER -> SWBLogScopeStoreKitManager
        LogScope.PLACEMENTS -> SWBLogScopePlacements
        LogScope.RECEIPTS -> SWBLogScopeReceipts
        LogScope.SUPERWALL_CORE -> SWBLogScopeSuperwallCore
        LogScope.PAYWALL_PRESENTATION -> SWBLogScopePaywallPresentation
        LogScope.TRANSACTIONS -> SWBLogScopeTransactions
        LogScope.PAYWALL_VIEW_CONTROLLER -> SWBLogScopePaywallViewController
        LogScope.CACHE -> SWBLogScopeCache
        LogScope.ALL -> SWBLogScopeAll
    }

/**
 * Maps the raw native level string delivered by `handleLog` (e.g. `"DEBUG"`)
 * to a common [LogLevel], or `null` when unmappable — the caller preserves
 * the raw string under `info["rawLevel"]` per the BridgeListener contract.
 */
internal fun logLevelFromRawName(raw: String): LogLevel? {
    val normalized = raw.normalizedEnumKey()
    return LogLevel.entries.firstOrNull { it.name.normalizedEnumKey() == normalized }
}

/**
 * Maps the raw native scope string delivered by `handleLog` (camelCase, e.g.
 * `"paywallPresentation"`) to a common [LogScope], or `null` when unmappable
 * (including the 4.16.x-only `analytics`/`webEntitlements` scopes) — the
 * caller falls back to [LogScope.ALL] and preserves `info["rawScope"]`.
 */
internal fun logScopeFromRawName(raw: String): LogScope? {
    val normalized = raw.normalizedEnumKey()
    return LogScope.entries.firstOrNull { it.name.normalizedEnumKey() == normalized }
}

/** Normalizes `SNAKE_CASE`/`camelCase`/`kebab-case` spellings for comparison. */
internal fun String.normalizedEnumKey(): String =
    lowercase().replace("_", "").replace("-", "").replace(" ", "")
