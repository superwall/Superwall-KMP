@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.InterfaceStyle
import com.superwall.sdk.kmp.internal.interop.SWB_EVENT_TRACKING_BEHAVIOR_ALL
import com.superwall.sdk.kmp.internal.interop.SWB_EVENT_TRACKING_BEHAVIOR_NONE
import com.superwall.sdk.kmp.internal.interop.SWB_EVENT_TRACKING_BEHAVIOR_SUPERWALL_ONLY
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_ALL
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_BOUNCE_BUTTON
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_CACHE
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_CONFIG_MANAGER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_CORE_DATA
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_DEBUG_MANAGER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_DEBUG_VIEW_CONTROLLER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_DEVICE
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_GAME_CONTROLLER_MANAGER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_IDENTITY_MANAGER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_LOCALIZATION_MANAGER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_LOCALIZATION_VIEW_CONTROLLER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_NETWORK
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_PAYWALL_EVENTS
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_PAYWALL_PRESENTATION
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_PAYWALL_VIEW_CONTROLLER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_PLACEMENTS
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_PRODUCTS_MANAGER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_RECEIPTS
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_STORE_KIT_MANAGER
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_SUPERWALL_CORE
import com.superwall.sdk.kmp.internal.interop.SWB_LOG_SCOPE_TRANSACTIONS
import com.superwall.sdk.kmp.internal.interop.SWB_NETWORK_ENVIRONMENT_DEVELOPER
import com.superwall.sdk.kmp.internal.interop.SWB_NETWORK_ENVIRONMENT_RELEASE
import com.superwall.sdk.kmp.internal.interop.SWB_NETWORK_ENVIRONMENT_RELEASE_CANDIDATE
import com.superwall.sdk.kmp.internal.interop.SWB_TEST_MODE_BEHAVIOR_ALWAYS
import com.superwall.sdk.kmp.internal.interop.SWB_TEST_MODE_BEHAVIOR_AUTOMATIC
import com.superwall.sdk.kmp.internal.interop.SWB_TEST_MODE_BEHAVIOR_NEVER
import com.superwall.sdk.kmp.internal.interop.SWB_TEST_MODE_BEHAVIOR_WHEN_ENABLED_FOR_USER
import com.superwall.sdk.kmp.internal.interop.SWB_TRANSACTION_BACKGROUND_VIEW_NONE
import com.superwall.sdk.kmp.internal.interop.SWB_TRANSACTION_BACKGROUND_VIEW_SPINNER
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
import com.superwall.sdk.kmp.internal.ios.interop.SWBLoggingOptions
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallOptions
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestoreFailedOptions
import com.superwall.sdk.kmp.internal.ios.interop.SWBSuperwallOptions
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
    options.setPaywalls(paywalls.toSWB())
    options.setNetworkEnvironment(NSNumber(long = networkEnvironment.toSWBRaw()))
    options.setIsExternalDataCollectionEnabled(NSNumber(bool = isExternalDataCollectionEnabled))
    // ALL is the common default and indistinguishable from "unset"; leaving
    // the SWB field nil lets the deprecated boolean keep its effect (the
    // native setter derives one from the other — same rule as androidMain).
    if (eventTrackingBehavior != EventTrackingBehavior.ALL) {
        options.setEventTrackingBehavior(NSNumber(long = eventTrackingBehavior.toSWBRaw()))
    }
    options.setLocaleIdentifier(localeIdentifier)
    options.setIsGameControllerEnabled(NSNumber(bool = isGameControllerEnabled))
    options.setEnableExperimentalDeviceVariables(NSNumber(bool = enableExperimentalDeviceVariables))
    options.setTestModeBehavior(NSNumber(long = testModeBehavior.toSWBRaw()))
    options.setShouldObservePurchases(NSNumber(bool = shouldObservePurchases))
    options.setShouldBypassAppTransactionCheck(NSNumber(bool = shouldBypassAppTransactionCheck))
    options.setMaxConfigRetryCount(NSNumber(int = maxConfigRetryCount))
    options.setLogging(
        SWBLoggingOptions().also { swbLogging ->
            swbLogging.setLevel(NSNumber(long = logLevelToSWB(logging.level)))
            swbLogging.setScopes(logging.scopes.map { NSNumber(long = logScopeToSWB(it)) })
        },
    )
    return options
}

internal fun PaywallOptions.toSWB(): SWBPaywallOptions {
    val options = SWBPaywallOptions()
    options.setIsHapticFeedbackEnabled(NSNumber(bool = isHapticFeedbackEnabled))
    options.setRestoreFailed(
        SWBRestoreFailedOptions().also { swbRestoreFailed ->
            swbRestoreFailed.setTitle(restoreFailed.title)
            swbRestoreFailed.setMessage(restoreFailed.message)
            swbRestoreFailed.setCloseButtonTitle(restoreFailed.closeButtonTitle)
        },
    )
    options.setShouldShowWebRestorationAlert(NSNumber(bool = shouldShowWebRestorationAlert))
    options.setShouldShowPurchaseFailureAlert(NSNumber(bool = shouldShowPurchaseFailureAlert))
    options.setShouldPreload(NSNumber(bool = shouldPreload))
    options.setAutomaticallyDismiss(NSNumber(bool = automaticallyDismiss))
    options.setTransactionBackgroundView(
        NSNumber(
            long = when (transactionBackgroundView) {
                TransactionBackgroundView.SPINNER -> SWB_TRANSACTION_BACKGROUND_VIEW_SPINNER
                TransactionBackgroundView.NONE -> SWB_TRANSACTION_BACKGROUND_VIEW_NONE
            },
        ),
    )
    overrideProductsByName?.let { overrides ->
        @Suppress("UNCHECKED_CAST")
        options.setOverrideProductsByName(overrides as Map<Any?, *>)
    }
    options.setShouldShowWebPurchaseConfirmationAlert(NSNumber(bool = shouldShowWebPurchaseConfirmationAlert))
    return options
}

internal fun IdentityOptions.toSWB(): SWBIdentityOptions = SWBIdentityOptions(restorePaywallAssignments)

private fun NetworkEnvironment.toSWBRaw(): Long =
    when (this) {
        NetworkEnvironment.RELEASE -> SWB_NETWORK_ENVIRONMENT_RELEASE
        NetworkEnvironment.RELEASE_CANDIDATE -> SWB_NETWORK_ENVIRONMENT_RELEASE_CANDIDATE
        NetworkEnvironment.DEVELOPER -> SWB_NETWORK_ENVIRONMENT_DEVELOPER
    }

private fun EventTrackingBehavior.toSWBRaw(): Long =
    when (this) {
        EventTrackingBehavior.ALL -> SWB_EVENT_TRACKING_BEHAVIOR_ALL
        EventTrackingBehavior.SUPERWALL_ONLY -> SWB_EVENT_TRACKING_BEHAVIOR_SUPERWALL_ONLY
        EventTrackingBehavior.NONE -> SWB_EVENT_TRACKING_BEHAVIOR_NONE
    }

private fun TestModeBehavior.toSWBRaw(): Long =
    when (this) {
        TestModeBehavior.AUTOMATIC -> SWB_TEST_MODE_BEHAVIOR_AUTOMATIC
        TestModeBehavior.WHEN_ENABLED_FOR_USER -> SWB_TEST_MODE_BEHAVIOR_WHEN_ENABLED_FOR_USER
        TestModeBehavior.NEVER -> SWB_TEST_MODE_BEHAVIOR_NEVER
        TestModeBehavior.ALWAYS -> SWB_TEST_MODE_BEHAVIOR_ALWAYS
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
 * Maps a common [LogScope] to the SWB raw value (the `SWBLogScope` raw Int —
 * carried as an NSNumber across the bridge). The bridge's two 4.16.x-only
 * scopes (`analytics`, `webEntitlements`) have no common counterpart and only
 * appear in the read direction ([logScopeFromRawName]).
 */
internal fun logScopeToSWB(scope: LogScope): Long =
    when (scope) {
        LogScope.LOCALIZATION_MANAGER -> SWB_LOG_SCOPE_LOCALIZATION_MANAGER
        LogScope.BOUNCE_BUTTON -> SWB_LOG_SCOPE_BOUNCE_BUTTON
        LogScope.CORE_DATA -> SWB_LOG_SCOPE_CORE_DATA
        LogScope.CONFIG_MANAGER -> SWB_LOG_SCOPE_CONFIG_MANAGER
        LogScope.IDENTITY_MANAGER -> SWB_LOG_SCOPE_IDENTITY_MANAGER
        LogScope.DEBUG_MANAGER -> SWB_LOG_SCOPE_DEBUG_MANAGER
        LogScope.DEBUG_VIEW_CONTROLLER -> SWB_LOG_SCOPE_DEBUG_VIEW_CONTROLLER
        LogScope.LOCALIZATION_VIEW_CONTROLLER -> SWB_LOG_SCOPE_LOCALIZATION_VIEW_CONTROLLER
        LogScope.GAME_CONTROLLER_MANAGER -> SWB_LOG_SCOPE_GAME_CONTROLLER_MANAGER
        LogScope.DEVICE -> SWB_LOG_SCOPE_DEVICE
        LogScope.NETWORK -> SWB_LOG_SCOPE_NETWORK
        LogScope.PAYWALL_EVENTS -> SWB_LOG_SCOPE_PAYWALL_EVENTS
        LogScope.PRODUCTS_MANAGER -> SWB_LOG_SCOPE_PRODUCTS_MANAGER
        LogScope.STORE_KIT_MANAGER -> SWB_LOG_SCOPE_STORE_KIT_MANAGER
        LogScope.PLACEMENTS -> SWB_LOG_SCOPE_PLACEMENTS
        LogScope.RECEIPTS -> SWB_LOG_SCOPE_RECEIPTS
        LogScope.SUPERWALL_CORE -> SWB_LOG_SCOPE_SUPERWALL_CORE
        LogScope.PAYWALL_PRESENTATION -> SWB_LOG_SCOPE_PAYWALL_PRESENTATION
        LogScope.TRANSACTIONS -> SWB_LOG_SCOPE_TRANSACTIONS
        LogScope.PAYWALL_VIEW_CONTROLLER -> SWB_LOG_SCOPE_PAYWALL_VIEW_CONTROLLER
        LogScope.CACHE -> SWB_LOG_SCOPE_CACHE
        LogScope.ALL -> SWB_LOG_SCOPE_ALL
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
