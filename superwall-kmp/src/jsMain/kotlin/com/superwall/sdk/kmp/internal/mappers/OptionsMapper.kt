package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.InterfaceStyle
import com.superwall.sdk.kmp.internal.interop.jsObject
import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import com.superwall.sdk.kmp.models.identity.IdentityOptions
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.NetworkEnvironment
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.options.TestModeBehavior

/** Reported to Superwall as `X-Platform-Wrapper`, like `@superwall/paywalls-react` sends `"React"`. */
internal const val PLATFORM_WRAPPER: String = "KMP"

/**
 * Maps [SuperwallOptions] to web's `PartialSuperwallOptions`.
 *
 * Only options the web SDK understands are forwarded. Native-only ones —
 * `eventTrackingBehavior`, `isGameControllerEnabled`,
 * `passIdentifiersToPlayStore`, `shouldObservePurchases`,
 * `shouldBypassAppTransactionCheck`, `useMockReviews`, and the paywall
 * options `restoreFailed`, `preloadDeviceOverrides`,
 * `transactionBackgroundView`, `overrideProductsByName`, `onBackPressed` —
 * have no web equivalent and are ignored.
 */
internal fun SuperwallOptions?.toJs(): dynamic {
    val obj = jsObject()
    obj.platformWrapper = PLATFORM_WRAPPER
    if (this == null) return obj

    val paywallsObj = jsObject()
    paywallsObj.isHapticFeedbackEnabled = paywalls.isHapticFeedbackEnabled
    paywallsObj.shouldShowPurchaseFailureAlert = paywalls.shouldShowPurchaseFailureAlert
    paywallsObj.shouldPreload = paywalls.shouldPreload
    paywallsObj.automaticallyDismiss = paywalls.automaticallyDismiss
    paywallsObj.shouldShowWebRestorationAlert = paywalls.shouldShowWebRestorationAlert
    paywallsObj.shouldShowWebPurchaseConfirmationAlert = paywalls.shouldShowWebPurchaseConfirmationAlert
    obj.paywalls = paywallsObj

    obj.networkEnvironment =
        when (networkEnvironment) {
            NetworkEnvironment.RELEASE -> "release"
            NetworkEnvironment.RELEASE_CANDIDATE -> "releaseCandidate"
            NetworkEnvironment.DEVELOPER -> "developer"
        }
    obj.isExternalDataCollectionEnabled = isExternalDataCollectionEnabled
    localeIdentifier?.let { obj.localeIdentifier = it }
    obj.enableExperimentalDeviceVariables = enableExperimentalDeviceVariables
    obj.testModeBehavior =
        when (testModeBehavior) {
            TestModeBehavior.AUTOMATIC -> "automatic"
            TestModeBehavior.WHEN_ENABLED_FOR_USER -> "whenEnabledForUser"
            TestModeBehavior.NEVER -> "never"
            TestModeBehavior.ALWAYS -> "always"
        }
    obj.maxConfigRetryCount = maxConfigRetryCount

    val loggingObj = jsObject()
    loggingObj.level = logging.level.toJs()
    loggingObj.scopes = logging.scopes.logScopesToJs()
    obj.logging = loggingObj
    return obj
}

internal fun LogLevel.toJs(): String =
    when (this) {
        LogLevel.DEBUG -> "debug"
        LogLevel.INFO -> "info"
        LogLevel.WARN -> "warn"
        LogLevel.ERROR -> "error"
        LogLevel.NONE -> "none"
    }

/** `null` when unrecognised; callers degrade to [LogLevel.DEBUG] (+ `rawLevel`), as native does. */
internal fun logLevelFromJs(value: String?): LogLevel? =
    when (value) {
        "debug" -> LogLevel.DEBUG
        "info" -> LogLevel.INFO
        "warn" -> LogLevel.WARN
        "error" -> LogLevel.ERROR
        "none" -> LogLevel.NONE
        else -> null
    }

/**
 * The web SDK's scopes, by common scope. Web has fewer scopes than the native
 * SDKs: iOS-/Android-only ones (bounce button, Core Data, game controller,
 * StoreKit, receipts, debug view controller) have no web counterpart and map
 * to `null`.
 */
internal fun LogScope.toJsOrNull(): String? =
    when (this) {
        LogScope.ALL -> "all"
        LogScope.CACHE -> "cache"
        LogScope.CONFIG_MANAGER -> "configManager"
        LogScope.DEBUG_MANAGER -> "debugManager"
        LogScope.DEVICE -> "device"
        LogScope.IDENTITY_MANAGER -> "identityManager"
        LogScope.LOCALIZATION_MANAGER, LogScope.LOCALIZATION_VIEW_CONTROLLER -> "localization"
        LogScope.NETWORK -> "network"
        LogScope.PAYWALL_EVENTS -> "paywallEvents"
        LogScope.PAYWALL_PRESENTATION -> "paywallPresentation"
        LogScope.PAYWALL_VIEW_CONTROLLER -> "paywallView"
        LogScope.PLACEMENTS -> "placements"
        LogScope.PRODUCTS_MANAGER -> "productsManager"
        LogScope.SUPERWALL_CORE -> "superwallCore"
        LogScope.TRANSACTIONS -> "transactions"
        LogScope.BOUNCE_BUTTON,
        LogScope.CORE_DATA,
        LogScope.DEBUG_VIEW_CONTROLLER,
        LogScope.GAME_CONTROLLER_MANAGER,
        LogScope.STORE_KIT_MANAGER,
        LogScope.RECEIPTS,
        -> null
    }

/** An empty result (every scope native-only) widens to `["all"]` rather than silencing logging. */
private fun Set<LogScope>.logScopesToJs(): Array<String> {
    val scopes = mapNotNull { it.toJsOrNull() }.distinct()
    return if (scopes.isEmpty()) arrayOf("all") else scopes.toTypedArray()
}

/** `null` when unrecognised; callers degrade to [LogScope.ALL] (+ `rawScope`), as native does. */
internal fun logScopeFromJs(value: String?): LogScope? =
    when (value) {
        "all" -> LogScope.ALL
        "cache" -> LogScope.CACHE
        "configManager" -> LogScope.CONFIG_MANAGER
        "debugManager" -> LogScope.DEBUG_MANAGER
        "device" -> LogScope.DEVICE
        "identityManager" -> LogScope.IDENTITY_MANAGER
        "localization" -> LogScope.LOCALIZATION_MANAGER
        "network" -> LogScope.NETWORK
        "paywallEvents" -> LogScope.PAYWALL_EVENTS
        "paywallPresentation" -> LogScope.PAYWALL_PRESENTATION
        "paywallView" -> LogScope.PAYWALL_VIEW_CONTROLLER
        "placements" -> LogScope.PLACEMENTS
        "productsManager" -> LogScope.PRODUCTS_MANAGER
        "superwallCore" -> LogScope.SUPERWALL_CORE
        "transactions" -> LogScope.TRANSACTIONS
        else -> null
    }

/**
 * The web wire name: the enum name in lower camel case (`ADJUST_ID` →
 * `adjustId`), which is exactly web's `IntegrationAttribute` union. Web
 * stores any key, so the two attributes its union lacks
 * (`firebaseInstallationId`, `singularDeviceId`) are still sent verbatim.
 */
internal fun IntegrationAttribute.toJs(): String =
    name.lowercase().split('_').mapIndexed { index, part ->
        if (index == 0) part else part.replaceFirstChar { it.uppercaseChar() }
    }.joinToString("")

/** The attribute for a web wire name, or `null` for web-only keys (`meta`, `custom`, …). */
internal fun integrationAttributeFromJs(name: String): IntegrationAttribute? = IntegrationAttribute.entries.firstOrNull { it.toJs() == name }

internal fun IdentityOptions.toJs(): dynamic {
    val obj = jsObject()
    obj.restorePaywallAssignments = restorePaywallAssignments
    return obj
}

/** Web takes `"light" | "dark" | null`; `null` restores automatic detection. */
internal fun InterfaceStyle?.toJs(): String? =
    when (this) {
        InterfaceStyle.LIGHT -> "light"
        InterfaceStyle.DARK -> "dark"
        InterfaceStyle.AUTOMATIC, null -> null
    }
