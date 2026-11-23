@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.internal.RecordingLog
import com.superwall.sdk.kmp.internal.inPlaceScope
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallback
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResultStatusFailure
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResultStatusSuccess
import com.superwall.sdk.kmp.internal.ios.interop.SWBExperiment
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReasonManualClose
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallCloseReasonNone
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResultCaseDeclined
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResultCasePurchased
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallSkippedReason
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallSkippedReasonCaseHoldout
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallSkippedReasonCasePlacementNotFound
import com.superwall.sdk.kmp.internal.ios.interop.SWBVariant
import com.superwall.sdk.kmp.internal.ios.interop.SWBVariantTypeHoldout
import com.superwall.sdk.kmp.internal.swbPaywallInfo
import com.superwall.sdk.kmp.models.callbacks.CustomCallback
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResult
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import com.superwall.sdk.kmp.models.triggers.Experiment
import com.superwall.sdk.kmp.models.triggers.Variant
import com.superwall.sdk.kmp.models.triggers.VariantType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PresentationHandlerAdapterTest {
    private val handler = PaywallPresentationHandler()
    private val log = RecordingLog()
    private val finished = mutableListOf<PresentationHandlerAdapter>()
    private val adapter = PresentationHandlerAdapter(handler, inPlaceScope(), log.log) { finished += it }

    @Test
    fun presentAndWillDismissForwardTheMappedValuesAndKeepTheRegistration() {
        val calls = mutableListOf<String>()
        handler.onPresent { calls += "present:${it.identifier}" }
        handler.onWillDismiss { info, result -> calls += "willDismiss:${info.identifier}:$result" }

        adapter.onPresent(swbPaywallInfo("paywall_a"))
        adapter.onWillDismiss(
            swbPaywallInfo("paywall_a", SWBPaywallCloseReasonManualClose),
            SWBPaywallResult(SWBPaywallResultCasePurchased, "annual"),
        )

        assertEquals(
            listOf("present:paywall_a", "willDismiss:paywall_a:${PaywallResult.Purchased("annual")}"),
            calls,
        )
        assertTrue(finished.isEmpty())
    }

    @Test
    fun aRealDismissIsForwardedAndEndsTheRegistration() {
        val dismissals = mutableListOf<Pair<String?, PaywallResult>>()
        handler.onDismiss { info, result -> dismissals += info.identifier to result }

        adapter.onDismiss(
            swbPaywallInfo("paywall_a", SWBPaywallCloseReasonManualClose),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
        )

        assertEquals(listOf<Pair<String?, PaywallResult>>("paywall_a" to PaywallResult.Declined), dismissals)
        assertSame(adapter, finished.single())
    }

    /**
     * closeReason `none` is a dismiss the paywall comes back from (it stays
     * registered natively), so later callbacks must still find the adapter.
     */
    @Test
    fun aDismissWithNoCloseReasonIsForwardedButKeepsTheRegistration() {
        val dismissals = mutableListOf<String?>()
        handler.onDismiss { info, _ -> dismissals += info.identifier }

        adapter.onDismiss(
            swbPaywallInfo("paywall_a", SWBPaywallCloseReasonNone),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
        )

        assertEquals(listOf<String?>("paywall_a"), dismissals)
        assertTrue(finished.isEmpty())
    }

    @Test
    fun errorIsForwardedAndEndsTheRegistration() {
        val errors = mutableListOf<String>()
        handler.onError { errors += it }

        adapter.onError("no network")

        assertEquals(listOf("no network"), errors)
        assertSame(adapter, finished.single())
    }

    @Test
    fun skipIsForwardedWithTheMappedReasonAndEndsTheRegistration() {
        val reasons = mutableListOf<PaywallSkippedReason>()
        handler.onSkip { reasons += it }

        adapter.onSkip(
            SWBPaywallSkippedReason(
                SWBPaywallSkippedReasonCaseHoldout,
                SWBExperiment("exp_1", "group_1", SWBVariant("variant_1", SWBVariantTypeHoldout, null)),
            ),
        )

        assertEquals(
            listOf<PaywallSkippedReason>(
                PaywallSkippedReason.Holdout(
                    Experiment("exp_1", "group_1", Variant("variant_1", VariantType.HOLDOUT, null)),
                ),
            ),
            reasons,
        )
        assertSame(adapter, finished.single())
    }

    @Test
    fun terminalCallbacksEndTheRegistrationEvenWithNoHandlerSet() {
        adapter.onSkip(SWBPaywallSkippedReason(SWBPaywallSkippedReasonCasePlacementNotFound, null))

        assertSame(adapter, finished.single())
        assertTrue(log.entries.isEmpty())
    }

    @Test
    fun aThrowingHandlerIsLoggedByNameAndDoesNotReachTheCaller() {
        val failure = IllegalStateException("app bug")
        handler.onPresent { throw failure }
        handler.onDismiss { _, _ -> throw failure }

        adapter.onPresent(swbPaywallInfo("paywall_a"))
        adapter.onDismiss(
            swbPaywallInfo("paywall_a", SWBPaywallCloseReasonManualClose),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
        )

        assertEquals(
            listOf(
                "PaywallPresentationHandler.onPresent threw an exception",
                "PaywallPresentationHandler.onDismiss threw an exception",
            ),
            log.entries.map { it.first },
        )
        assertTrue(log.entries.all { it.second === failure })
        // A handler that throws on dismiss must not leak the registration.
        assertSame(adapter, finished.single())
    }

    @Test
    fun customCallbackGoesToTheHandlerAndItsResultGoesBack() {
        val received = mutableListOf<CustomCallback>()
        handler.onCustomCallback {
            received += it
            CustomCallbackResult.success(mapOf("granted" to true))
        }
        val answers = mutableListOf<SWBCustomCallbackResult?>()

        adapter.onCustomCallback(SWBCustomCallback("validate", mapOf<Any?, Any>("code" to "ABC"))) { answers += it }

        assertEquals(listOf(CustomCallback("validate", mapOf("code" to "ABC"))), received)
        val answer = answers.single()!!
        assertEquals(SWBCustomCallbackResultStatusSuccess, answer.status())
        assertEquals(true, answer.data()?.get("granted"))
        assertTrue(finished.isEmpty())
    }

    @Test
    fun customCallbackWithNoHandlerAnswersFailure() {
        val answers = mutableListOf<SWBCustomCallbackResult?>()

        adapter.onCustomCallback(SWBCustomCallback("validate", null)) { answers += it }

        assertEquals(SWBCustomCallbackResultStatusFailure, answers.single()!!.status())
        assertTrue(log.entries.isEmpty())
    }

    @Test
    fun aThrowingCustomCallbackAnswersFailureWithTheMessageAndIsLogged() {
        val failure = IllegalArgumentException("bad code")
        handler.onCustomCallback { throw failure }
        val answers = mutableListOf<SWBCustomCallbackResult?>()

        adapter.onCustomCallback(SWBCustomCallback("validate", null)) { answers += it }

        val answer = answers.single()!!
        assertEquals(SWBCustomCallbackResultStatusFailure, answer.status())
        assertEquals("bad code", answer.data()?.get("error"))
        assertEquals("PaywallPresentationHandler.onCustomCallback threw an exception", log.entries.single().first)
        assertSame(failure, log.entries.single().second)
    }
}
