package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.events.EventType
import com.superwall.sdk.kmp.models.events.IntegrationAttribute
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import com.superwall.sdk.kmp.models.options.ConfigurationStatus
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A [SuperwallDelegate] that records every callback it receives.
 */
private class RecordingDelegate : SuperwallDelegate {
    val events = mutableListOf<String>()
    val logs = mutableListOf<Triple<LogLevel, LogScope, String?>>()
    var lastStatusChange: Pair<SubscriptionStatus, SubscriptionStatus>? = null
    var lastEventInfo: SuperwallEventInfo? = null
    var lastCustomAction: String? = null
    var lastPaywallInfo: PaywallInfo? = null
    var lastUrl: String? = null
    var lastUserAttributes: Map<String, Any?>? = null
    var lastCustomerInfoChange: Pair<CustomerInfo, CustomerInfo>? = null

    override fun subscriptionStatusDidChange(
        from: SubscriptionStatus,
        to: SubscriptionStatus,
    ) {
        events += "subscriptionStatusDidChange"
        lastStatusChange = from to to
    }

    override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
        events += "handleSuperwallEvent"
        lastEventInfo = eventInfo
    }

    override fun handleCustomPaywallAction(name: String) {
        events += "handleCustomPaywallAction"
        lastCustomAction = name
    }

    override fun willPresentPaywall(paywallInfo: PaywallInfo) {
        events += "willPresentPaywall"
        lastPaywallInfo = paywallInfo
    }

    override fun didDismissPaywall(paywallInfo: PaywallInfo) {
        events += "didDismissPaywall"
        lastPaywallInfo = paywallInfo
    }

    override fun paywallWillOpenURL(url: String) {
        events += "paywallWillOpenURL"
        lastUrl = url
    }

    override fun handleLog(
        level: LogLevel,
        scope: LogScope,
        message: String?,
        info: Map<String, Any?>?,
        error: String?,
    ) {
        events += "handleLog"
        logs += Triple(level, scope, message)
    }

    override fun customerInfoDidChange(
        from: CustomerInfo,
        to: CustomerInfo,
    ) {
        events += "customerInfoDidChange"
        lastCustomerInfoChange = from to to
    }

    override fun userAttributesDidChange(newAttributes: Map<String, Any?>) {
        events += "userAttributesDidChange"
        lastUserAttributes = newAttributes
    }
}

/**
 * FakeBridge contract tests for the [Superwall] facade (plan section 8):
 * pre-configure guard + the full exemption list, configure re-invocation
 * semantics, completion delivery, delegate multiplexing, stream seeding and
 * attach transition.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SuperwallFacadeTest {
    private lateinit var fake: FakeBridge

    private val activeStatus =
        SubscriptionStatus.Active(setOf(Entitlement(id = "pro")))

    @BeforeTest
    fun setUp() {
        // StreamHolder's scope uses Dispatchers.Main.immediate.
        Dispatchers.setMain(StandardTestDispatcher())
        fake = FakeBridge()
        Superwall.resetForTest(fake)
    }

    @AfterTest
    fun tearDown() {
        Superwall.resetForTest(null)
        Dispatchers.resetMain()
    }

    // ---- Pre-configure guard -------------------------------------------------

    @Test
    fun guardedSyncMembersThrowNotConfiguredBeforeConfigure() {
        val guarded: List<Pair<String, () -> Any?>> =
            listOf(
                "reset" to { Superwall.reset() },
                "userId" to { Superwall.userId },
                "isLoggedIn" to { Superwall.isLoggedIn },
                "identify" to { Superwall.identify("abc") },
                "userAttributes" to { Superwall.userAttributes },
                "setUserAttributes" to { Superwall.setUserAttributes(mapOf("a" to 1L)) },
                "setIntegrationAttribute" to {
                    Superwall.setIntegrationAttribute(IntegrationAttribute.ADJUST_ID, "x")
                },
                "setIntegrationAttributes" to {
                    Superwall.setIntegrationAttributes(mapOf(IntegrationAttribute.ADJUST_ID to "x"))
                },
                "integrationAttributes" to { Superwall.integrationAttributes },
                "localeIdentifier get" to { Superwall.localeIdentifier },
                "localeIdentifier set" to { Superwall.localeIdentifier = "en_GB" },
                "logLevel get" to { Superwall.logLevel },
                "logLevel set" to { Superwall.logLevel = LogLevel.ERROR },
                "entitlements" to { Superwall.entitlements },
                "subscriptionStatus get" to { Superwall.subscriptionStatus },
                "subscriptionStatus set" to { Superwall.subscriptionStatus = SubscriptionStatus.Inactive },
                "register" to { Superwall.register("placement") },
                "isPaywallPresented" to { Superwall.isPaywallPresented },
                "latestPaywallInfo" to { Superwall.latestPaywallInfo },
                "preloadAllPaywalls" to { Superwall.preloadAllPaywalls() },
                "preloadPaywalls" to { Superwall.preloadPaywalls(setOf("p")) },
                "togglePaywallSpinner" to { Superwall.togglePaywallSpinner(true) },
                "overrideProductsByName get" to { Superwall.overrideProductsByName },
                "overrideProductsByName set" to { Superwall.overrideProductsByName = mapOf("primary" to "id") },
            )

        for ((name, block) in guarded) {
            assertFailsWith<SuperwallError.NotConfigured>("expected $name to be guarded") { block() }
        }
        // The guard fires before the bridge — nothing may have leaked through.
        assertTrue(fake.calls.isEmpty(), "guarded members must not reach the bridge, saw: ${fake.calls}")
    }

    @Test
    fun guardedSuspendMembersThrowNotConfiguredBeforeConfigure() =
        runTest {
            assertFailsWith<SuperwallError.NotConfigured> { Superwall.getDeviceAttributes() }
            assertFailsWith<SuperwallError.NotConfigured> { Superwall.getEntitlementsByProductIds(setOf("p")) }
            assertFailsWith<SuperwallError.NotConfigured> { Superwall.getCustomerInfo() }
            assertFailsWith<SuperwallError.NotConfigured> { Superwall.confirmAllAssignments() }
            assertFailsWith<SuperwallError.NotConfigured> { Superwall.restorePurchases() }
            assertFailsWith<SuperwallError.NotConfigured> { Superwall.getPresentationResult("placement") }
            assertFailsWith<SuperwallError.NotConfigured> { Superwall.dismiss() }
            assertFailsWith<SuperwallError.NotConfigured> { Superwall.consume("token") }
            assertTrue(fake.calls.isEmpty(), "guarded members must not reach the bridge, saw: ${fake.calls}")
        }

    // ---- Guard exemption list (full) ------------------------------------------

    @Test
    fun isConfiguredIsGuardExempt() {
        fake.cannedIsConfigured = false
        assertFalse(Superwall.isConfigured)
        assertEquals(listOf("isConfigured"), fake.calls)
    }

    @Test
    fun isInitializedIsGuardExempt() {
        fake.cannedIsInitialized = true
        assertTrue(Superwall.isInitialized)
        assertEquals(listOf("isInitialized"), fake.calls)
    }

    @Test
    fun configurationStatusIsGuardExempt() {
        fake.cannedConfigurationStatus = ConfigurationStatus.PENDING
        assertEquals(ConfigurationStatus.PENDING, Superwall.configurationStatus)
        assertEquals(listOf("getConfigurationStatus"), fake.calls)
    }

    @Test
    fun handleDeepLinkIsGuardExemptAndForwardsPreConfigure() {
        fake.cannedHandleDeepLinkResult = true
        assertTrue(Superwall.handleDeepLink("myapp://deep/link"))
        assertEquals("myapp://deep/link", fake.lastHandledDeepLink)
        assertEquals(listOf("handleDeepLink"), fake.calls)
    }

    @Test
    fun subscriptionStatusFlowIsGuardExemptAndSeededWithUnknown() {
        assertEquals(SubscriptionStatus.Unknown, Superwall.subscriptionStatusFlow.value)
        assertTrue(fake.calls.isEmpty(), "stream access must not touch the bridge")
    }

    @Test
    fun customerInfoFlowIsGuardExempt() {
        assertNotNull(Superwall.customerInfoFlow)
        assertTrue(fake.calls.isEmpty(), "stream access must not touch the bridge")
    }

    @Test
    fun delegateIsGuardExemptPreConfigure() {
        val delegate = RecordingDelegate()
        Superwall.delegate = delegate
        assertSame(delegate, Superwall.delegate)
        Superwall.delegate = null
        assertNull(Superwall.delegate)
        assertTrue(fake.calls.isEmpty(), "delegate storage must not touch the bridge")
    }

    // ---- Configure ---------------------------------------------------------------

    @Test
    fun configureForwardsToBridgeAndDeliversSuccess() {
        var delivered: Result<Unit>? = null
        val controller =
            object : PurchaseController {
                override suspend fun purchaseFromAppStore(productId: String) =
                    com.superwall.sdk.kmp.models.results.PurchaseResult.cancelled()

                override suspend fun purchaseFromGooglePlay(
                    productId: String,
                    basePlanId: String?,
                    offerId: String?,
                ) = com.superwall.sdk.kmp.models.results.PurchaseResult.cancelled()

                override suspend fun restorePurchases() =
                    com.superwall.sdk.kmp.models.results.RestorationResult.restored()
            }
        val options = SuperwallOptions()

        Superwall.configure("pk_test", controller, options) { delivered = it }

        assertEquals(Result.success(Unit), delivered)
        assertEquals(1, fake.configureCount)
        assertEquals("pk_test", fake.lastApiKey)
        assertSame(controller, fake.lastPurchaseController)
        assertSame(options, fake.lastOptions)
        // The multiplexer is installed before configure so no event is missed.
        assertEquals(listOf("installDelegate", "configure"), fake.calls.take(2))
        assertNotNull(fake.installedListener)
    }

    @Test
    fun configureDeliversBridgeFailureAndDoesNotAttachStreams() {
        val failure = SuperwallError.ConfigurationFailed("bad api key")
        fake.configureOutcome = Result.failure(failure)

        var delivered: Result<Unit>? = null
        Superwall.configure("bad_key") { delivered = it }

        val deliveredResult = assertNotNull(delivered)
        assertSame(failure, deliveredResult.exceptionOrNull())
        assertTrue(fake.attachedHolders.isEmpty(), "streams must not attach on configure failure")
    }

    @Test
    fun configureAndAwaitResumesOnSuccess() =
        runTest {
            Superwall.configureAndAwait("pk_test")
            assertEquals(1, fake.configureCount)
        }

    @Test
    fun configureAndAwaitThrowsConfigurationFailed() =
        runTest {
            fake.configureOutcome = Result.failure(SuperwallError.ConfigurationFailed("bad api key"))
            assertFailsWith<SuperwallError.ConfigurationFailed> {
                Superwall.configureAndAwait("bad_key")
            }
        }

    @Test
    fun repeatConfigureIsNoOpWithImmediateCompletionAndLoggedWarning() {
        val delegate = RecordingDelegate()
        Superwall.delegate = delegate

        Superwall.configure("pk_test")
        var repeatResult: Result<Unit>? = null
        Superwall.configure("pk_other") { repeatResult = it }

        // No-op: the bridge saw exactly one configure and one delegate install,
        // and the second call's options/controller were not re-installed.
        assertEquals(1, fake.configureCount)
        assertEquals(1, fake.installDelegateCount)
        assertEquals("pk_test", fake.lastApiKey)
        // Immediate completion with the first call's outcome.
        assertEquals(Result.success(Unit), repeatResult)
        // A warning was logged via the delegate's handleLog.
        val warning =
            assertNotNull(
                delegate.logs.singleOrNull(),
                "expected exactly one logged warning, got ${delegate.logs}",
            )
        assertEquals(LogLevel.WARN, warning.first)
    }

    @Test
    fun repeatConfigureWhileFirstInFlightQueuesCompletionBehindIt() {
        fake.configureOutcome = null // hold the native completion

        var first: Result<Unit>? = null
        var second: Result<Unit>? = null
        Superwall.configure("pk_test") { first = it }
        Superwall.configure("pk_test") { second = it }

        assertEquals(1, fake.configureCount)
        assertNull(first)
        assertNull(second)

        fake.completeHeldConfigures(Result.success(Unit))

        assertEquals(Result.success(Unit), first)
        assertEquals(Result.success(Unit), second)
    }

    @Test
    fun configureWiresExperimentalDeviceVariablesWhenOptedIn() {
        Superwall.configure(
            "pk_test",
            options = SuperwallOptions(enableExperimentalDeviceVariables = true),
        )
        assertEquals(listOf(true), fake.enableExperimentalDeviceVariablesCalls)
    }

    @Test
    fun configureDoesNotWireExperimentalDeviceVariablesByDefault() {
        Superwall.configure("pk_test")
        assertTrue(fake.enableExperimentalDeviceVariablesCalls.isEmpty())
    }

    // ---- Streams: seeding, attach transition, no duplicate attach -----------------

    @Test
    fun attachTransitionUpdatesSeededFlowAtConfigureComplete() {
        assertEquals(SubscriptionStatus.Unknown, Superwall.subscriptionStatusFlow.value)

        fake.statusAtAttach = SubscriptionStatus.Inactive
        Superwall.configure("pk_test")

        assertEquals(1, fake.attachedHolders.size)
        assertEquals(SubscriptionStatus.Inactive, Superwall.subscriptionStatusFlow.value)
    }

    @Test
    fun doubleConfigureDoesNotAttachStreamsTwice() {
        Superwall.configure("pk_test")
        Superwall.configure("pk_test")
        assertEquals(1, fake.attachedHolders.size, "repeat configure must not re-attach native streams")
    }

    @Test
    fun subscriptionStatusEventFeedsFlowAndDelegate() {
        val delegate = RecordingDelegate()
        Superwall.delegate = delegate
        Superwall.configure("pk_test")

        val listener = assertNotNull(fake.installedListener)
        listener.subscriptionStatusDidChange(from = SubscriptionStatus.Unknown, to = activeStatus)

        assertEquals(activeStatus, Superwall.subscriptionStatusFlow.value)
        assertEquals(SubscriptionStatus.Unknown to activeStatus, delegate.lastStatusChange)
    }

    @Test
    fun customerInfoEventFeedsFlowAndDelegate() =
        runTest {
            val delegate = RecordingDelegate()
            Superwall.delegate = delegate
            Superwall.configure("pk_test")

            val received = mutableListOf<CustomerInfo>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                Superwall.customerInfoFlow.collect { received += it }
            }

            val before = CustomerInfo(emptyList(), emptyList(), emptyList(), "user")
            val after = before.copy(entitlements = listOf(Entitlement(id = "pro")))
            assertNotNull(fake.installedListener).customerInfoDidChange(before, after)
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(after), received)
            assertEquals(before to after, delegate.lastCustomerInfoChange)
        }

    // ---- Delegate multiplexing -------------------------------------------------------

    @Test
    fun delegateMultiplexerForwardsEventsWithPayloads() {
        val delegate = RecordingDelegate()
        Superwall.delegate = delegate
        Superwall.configure("pk_test")
        val listener = assertNotNull(fake.installedListener)

        val info = PaywallInfo(identifier = "pw_1", name = "Onboarding")
        val eventInfo = SuperwallEventInfo(eventType = EventType.PAYWALL_OPEN, paywallInfo = info)

        listener.handleCustomPaywallAction("custom_action")
        listener.willPresentPaywall(info)
        listener.didDismissPaywall(info)
        listener.paywallWillOpenURL("https://superwall.com")
        listener.handleSuperwallEvent(eventInfo)
        listener.userAttributesDidChange(mapOf("plan" to "pro"))

        assertEquals(
            listOf(
                "handleCustomPaywallAction",
                "willPresentPaywall",
                "didDismissPaywall",
                "paywallWillOpenURL",
                "handleSuperwallEvent",
                "userAttributesDidChange",
            ),
            delegate.events,
        )
        assertEquals("custom_action", delegate.lastCustomAction)
        assertEquals(info, delegate.lastPaywallInfo)
        assertEquals("https://superwall.com", delegate.lastUrl)
        assertEquals(eventInfo, delegate.lastEventInfo)
        assertEquals(mapOf<String, Any?>("plan" to "pro"), delegate.lastUserAttributes)
    }

    @Test
    fun delegateSetPreConfigureReceivesEventsAfterConfigure() {
        val delegate = RecordingDelegate()
        Superwall.delegate = delegate // before configure — stored, installed at configure

        Superwall.configure("pk_test")
        assertNotNull(fake.installedListener).handleCustomPaywallAction("hello")

        assertEquals("hello", delegate.lastCustomAction)
    }

    @Test
    fun delegateExceptionsAreSwallowedAndStreamsStayLive() {
        Superwall.delegate =
            object : SuperwallDelegate {
                override fun subscriptionStatusDidChange(
                    from: SubscriptionStatus,
                    to: SubscriptionStatus,
                ): Unit = throw IllegalStateException("user code blew up")
            }
        Superwall.configure("pk_test")

        // Must not throw into the (simulated) native caller...
        assertNotNull(fake.installedListener)
            .subscriptionStatusDidChange(SubscriptionStatus.Unknown, activeStatus)
        // ...and the stream still updates.
        assertEquals(activeStatus, Superwall.subscriptionStatusFlow.value)
    }

    @Test
    fun clearingDelegateStopsForwardingButStreamsStayLive() {
        val delegate = RecordingDelegate()
        Superwall.delegate = delegate
        Superwall.configure("pk_test")
        Superwall.delegate = null

        assertNotNull(fake.installedListener)
            .subscriptionStatusDidChange(SubscriptionStatus.Unknown, activeStatus)

        assertTrue(delegate.events.isEmpty(), "cleared delegate must not receive events")
        assertEquals(activeStatus, Superwall.subscriptionStatusFlow.value)
    }

    // ---- Post-configure pass-through smoke ----------------------------------------------

    @Test
    fun registerPassesDistinctHandlerReferencesPerCall() {
        Superwall.configure("pk_test")

        val handlerA = PaywallPresentationHandler()
        val handlerB = PaywallPresentationHandler()
        val featureA: () -> Unit = {}
        Superwall.register("same_placement", mapOf("k" to 1L), handlerA, featureA)
        Superwall.register("same_placement", null, handlerB, null)

        assertEquals(2, fake.registrations.size)
        val (first, second) = fake.registrations
        assertEquals("same_placement", first.placement)
        assertEquals(mapOf<String, Any?>("k" to 1L), first.params)
        assertSame(handlerA, first.handler)
        assertSame(featureA, first.feature)
        assertSame(handlerB, second.handler)
        assertNull(second.feature)
        // The fix for the Flutter one-handler-per-placement aliasing bug: each
        // registration carries its own handler reference, never a shared
        // placement-keyed one.
        assertNotSame(first.handler, second.handler)
    }

    @Test
    fun guardedMembersForwardToBridgeAfterConfigure() =
        runTest {
            Superwall.configure("pk_test")
            fake.calls.clear()

            fake.cannedUserId = "user_42"
            assertEquals("user_42", Superwall.userId)

            Superwall.subscriptionStatus = activeStatus
            assertEquals(activeStatus, Superwall.subscriptionStatus)

            fake.cannedRestorationResult = com.superwall.sdk.kmp.models.results.RestorationResult.Failed("nope")
            val restore = Superwall.restorePurchases()
            assertIs<com.superwall.sdk.kmp.models.results.RestorationResult.Failed>(restore)
            assertEquals("nope", restore.error)

            assertEquals("token_1", Superwall.consume("token_1"))

            assertTrue(fake.calls.containsAll(listOf("getUserId", "setSubscriptionStatus", "restorePurchases", "consume")))
        }
}
