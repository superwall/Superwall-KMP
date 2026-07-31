package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.options.DeviceTier
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.Logging
import com.superwall.sdk.kmp.models.options.NetworkEnvironment
import com.superwall.sdk.kmp.models.options.PaywallOptions
import com.superwall.sdk.kmp.models.options.RestoreFailed
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.options.TestModeBehavior
import com.superwall.sdk.kmp.models.options.TransactionBackgroundView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.superwall.sdk.analytics.Tier as NativeTier
import com.superwall.sdk.config.options.PaywallOptions as NativePaywallOptions
import com.superwall.sdk.config.options.SuperwallOptions as NativeSuperwallOptions
import com.superwall.sdk.logger.LogLevel as NativeLogLevel
import com.superwall.sdk.logger.LogScope as NativeLogScope
import com.superwall.sdk.store.testmode.TestModeBehavior as NativeTestModeBehavior

/**
 * Conversion tests for OptionsMapper.kt against the REAL native
 * `SuperwallOptions` / `PaywallOptions` classes from superwall-android.
 */
class OptionsMapperTest {
    // ---- SuperwallOptions ------------------------------------------------------

    @Test
    fun superwallOptions_nonDefaultValues_mapToNativeFields() {
        val common =
            SuperwallOptions(
                paywalls = PaywallOptions(),
                networkEnvironment = NetworkEnvironment.DEVELOPER,
                isExternalDataCollectionEnabled = false,
                localeIdentifier = "en_GB",
                isGameControllerEnabled = true,
                enableExperimentalDeviceVariables = true,
                logging = Logging(level = LogLevel.ERROR, scopes = setOf(LogScope.NETWORK)),
                passIdentifiersToPlayStore = true,
                testModeBehavior = TestModeBehavior.ALWAYS,
                shouldObservePurchases = true,
                useMockReviews = true,
            )

        val native = common.toNative()

        assertTrue(native.networkEnvironment is NativeSuperwallOptions.NetworkEnvironment.Developer)
        assertEquals(false, native.isExternalDataCollectionEnabled)
        assertEquals("en_GB", native.localeIdentifier)
        assertEquals(true, native.isGameControllerEnabled)
        assertEquals(true, native.enableExperimentalDeviceVariables)
        assertEquals(true, native.passIdentifiersToPlayStore)
        assertEquals(NativeTestModeBehavior.ALWAYS, native.testModeBehavior)
        assertEquals(true, native.shouldObservePurchases)
        assertEquals(true, native.useMockReviews)
        assertEquals(NativeLogLevel.error, native.logging.level)
        assertEquals(setOf(NativeLogScope.network), native.logging.scopes.toSet())
    }

    @Test
    fun superwallOptions_defaults_mapToNativeDefaults() {
        val native = SuperwallOptions().toNative()

        assertTrue(native.networkEnvironment is NativeSuperwallOptions.NetworkEnvironment.Release)
        assertEquals(true, native.isExternalDataCollectionEnabled)
        assertNull(native.localeIdentifier)
        assertEquals(false, native.isGameControllerEnabled)
        assertEquals(false, native.enableExperimentalDeviceVariables)
        assertEquals(false, native.passIdentifiersToPlayStore)
        assertEquals(NativeTestModeBehavior.AUTOMATIC, native.testModeBehavior)
        assertEquals(false, native.shouldObservePurchases)
        assertEquals(false, native.useMockReviews)
    }

    @Test
    fun networkEnvironment_releaseCandidate_mapsToNativeReleaseCandidate() {
        val native =
            SuperwallOptions(networkEnvironment = NetworkEnvironment.RELEASE_CANDIDATE).toNative()
        assertTrue(
            native.networkEnvironment is NativeSuperwallOptions.NetworkEnvironment.ReleaseCandidate,
        )
    }

    // ---- PaywallOptions --------------------------------------------------------

    @Test
    fun paywallOptions_nonDefaultValues_mapToNativeFields() {
        val common =
            PaywallOptions(
                isHapticFeedbackEnabled = false,
                restoreFailed =
                    RestoreFailed(
                        title = "t",
                        message = "m",
                        closeButtonTitle = "c",
                    ),
                shouldShowPurchaseFailureAlert = false,
                shouldPreload = false,
                preloadDeviceOverrides =
                    mapOf(
                        DeviceTier.LOW to false,
                        DeviceTier.ULTRA_HIGH to true,
                    ),
                automaticallyDismiss = false,
                transactionBackgroundView = TransactionBackgroundView.SPINNER,
                overrideProductsByName = mapOf("primary" to "com.example.annual"),
            )

        val native = common.toNative()

        assertEquals(false, native.isHapticFeedbackEnabled)
        assertEquals("t", native.restoreFailed.title)
        assertEquals("m", native.restoreFailed.message)
        assertEquals("c", native.restoreFailed.closeButtonTitle)
        assertEquals(false, native.shouldShowPurchaseFailureAlert)
        assertEquals(false, native.shouldPreload)
        assertEquals(
            mapOf(NativeTier.LOW to false, NativeTier.ULTRA_HIGH to true),
            native.preloadDeviceOverrides,
        )
        assertEquals(false, native.automaticallyDismiss)
        assertEquals(
            NativePaywallOptions.TransactionBackgroundView.SPINNER,
            native.transactionBackgroundView,
        )
        assertEquals(mapOf("primary" to "com.example.annual"), native.overrideProductsByName)
    }

    @Test
    fun paywallOptions_transactionBackgroundViewNone_mapsToNativeNull() {
        val native =
            PaywallOptions(transactionBackgroundView = TransactionBackgroundView.NONE).toNative()
        assertNull(native.transactionBackgroundView)
    }

    @Test
    fun paywallOptions_nullOverrideProductsByName_mapsToEmptyNativeMap() {
        val native = PaywallOptions(overrideProductsByName = null).toNative()
        assertEquals(emptyMap(), native.overrideProductsByName)
    }

    // ---- Logging ---------------------------------------------------------------

    @Test
    fun logging_levelAndScopes_mapToNative() {
        val native = Logging(level = LogLevel.WARN, scopes = setOf(LogScope.NETWORK, LogScope.CACHE)).toNative()

        assertEquals(NativeLogLevel.warn, native.level)
        assertEquals(setOf(NativeLogScope.network, NativeLogScope.cache), native.scopes.toSet())
    }

    @Test
    fun logging_emptyScopes_keepsNativeDefaultScopes() {
        // The mapper only assigns scopes when non-empty; the native default (all) survives.
        val native = Logging(scopes = emptySet()).toNative()
        assertEquals(setOf(NativeLogScope.all), native.scopes.toSet())
    }

    // ---- TestModeBehavior ------------------------------------------------------

    @Test
    fun testModeBehavior_roundTripsExhaustivelyInBothDirections() {
        for (common in TestModeBehavior.entries) {
            assertEquals(common, common.toNative().toKmp())
        }
        for (native in NativeTestModeBehavior.entries) {
            assertEquals(native, native.toKmp().toNative())
        }
    }

    // ---- DeviceTier --------------------------------------------------------------

    @Test
    fun deviceTier_roundTripsExhaustivelyInBothDirections() {
        for (common in DeviceTier.entries) {
            assertEquals(common, common.toNative().toKmp())
        }
        for (native in NativeTier.entries) {
            assertEquals(native, native.toKmp().toNative())
        }
    }

    // ---- LogLevel ----------------------------------------------------------------

    @Test
    fun logLevel_roundTripsExhaustivelyInBothDirections() {
        for (common in LogLevel.entries) {
            assertEquals(common, common.toNative().toKmp())
        }
        for (native in NativeLogLevel.entries) {
            assertEquals(native, native.toKmp().toNative())
        }
    }

    @Test
    fun logLevelFromNativeString_mapsEveryNativeToStringRendering() {
        // The native delegate passes level.toString() ("DEBUG", "INFO", ...).
        for (native in NativeLogLevel.entries) {
            assertEquals(native.toKmp(), logLevelFromNativeString(native.toString()))
        }
    }

    @Test
    fun logLevelFromNativeString_isCaseAndWhitespaceInsensitive() {
        assertEquals(LogLevel.DEBUG, logLevelFromNativeString(" debug "))
        assertEquals(LogLevel.ERROR, logLevelFromNativeString("Error"))
    }

    @Test
    fun logLevelFromNativeString_unknownLevelReturnsNullSoCallerCanDegrade() {
        // Plan §3.4: unmappable levels degrade to DEBUG + info["rawLevel"] at the
        // call site; the mapper's contract is to return null.
        assertNull(logLevelFromNativeString("VERBOSE"))
        assertNull(logLevelFromNativeString(""))
    }

    // ---- LogScope ----------------------------------------------------------------

    @Test
    fun logScope_commonToNativeRoundTripsExhaustively() {
        for (common in LogScope.entries) {
            assertEquals(common, common.toNative().toKmp())
        }
    }

    private val unmappedNativeScopes =
        setOf(
            NativeLogScope.webEntitlements,
            NativeLogScope.customerInfo,
            NativeLogScope.jsEvaluator,
            NativeLogScope.paywallTransactions,
            NativeLogScope.nativePurchaseController,
            NativeLogScope.deepLinks,
        )

    @Test
    fun logScope_nativeToCommonCoversEveryNativeCase() {
        for (native in NativeLogScope.entries) {
            val mapped = native.toKmp()
            if (native in unmappedNativeScopes) {
                // No common counterpart: null so the caller degrades to ALL + rawScope.
                assertNull(mapped, "expected $native to be unmapped")
            } else {
                assertEquals(native, mapped!!.toNative(), "round trip failed for $native")
            }
        }
    }

    @Test
    fun logScopeFromNativeString_mapsNativeEnumNames() {
        assertEquals(LogScope.PAYWALL_PRESENTATION, logScopeFromNativeString("paywallPresentation"))
        assertEquals(LogScope.ALL, logScopeFromNativeString("all"))
        // Native LogScope.toString() is the enum name; a case-insensitive
        // fallback also matches.
        assertEquals(LogScope.SUPERWALL_CORE, logScopeFromNativeString("SUPERWALLCORE"))
    }

    @Test
    fun logScopeFromNativeString_unknownOrUnmappedReturnsNullSoCallerCanDegrade() {
        assertNull(logScopeFromNativeString("someBrandNewScope"))
        // A real native scope with no common counterpart also returns null.
        assertNull(logScopeFromNativeString("webEntitlements"))
    }
}
