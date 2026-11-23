@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.PaywallViewCallback
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallback
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResultStatusFailure
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResultStatusSuccess
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallInfo
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResultCaseDeclined
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResultCasePurchased
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResultCaseRestored
import com.superwall.sdk.kmp.models.callbacks.CustomCallback
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResult
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PaywallResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import platform.UIKit.UIViewController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The iOS embedded paywall and its delegate adapter, driven the way
 * SuperwallKit drives them: through the `SWBPaywallViewDelegate` methods, with
 * real bridge objects as arguments.
 */
class IosEmbeddedPaywallTest {
    private val callback = RecordingCallback()
    private val log = RecordingLog()
    private val adapter = PaywallViewDelegateAdapter(callback, inPlaceScope(), log.log)

    // -- PaywallViewDelegateAdapter ------------------------------------------

    @Test
    fun finishIsForwardedWithTheMappedInfoResultAndDismissFlag() {
        adapter.paywallDidFinish(
            swbPaywallInfo("paywall_a"),
            SWBPaywallResult(SWBPaywallResultCasePurchased, "annual"),
            shouldDismiss = true,
        )
        adapter.paywallDidFinish(
            swbPaywallInfo("paywall_b"),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
            shouldDismiss = false,
        )
        adapter.paywallDidFinish(
            swbPaywallInfo("paywall_c"),
            SWBPaywallResult(SWBPaywallResultCaseRestored, null),
            shouldDismiss = true,
        )

        assertEquals(
            listOf(
                Finished("paywall_a", PaywallResult.Purchased("annual"), true),
                Finished("paywall_b", PaywallResult.Declined, false),
                Finished("paywall_c", PaywallResult.Restored, true),
            ),
            callback.finished,
        )
        assertTrue(log.entries.isEmpty())
    }

    @Test
    fun aThrowingOnFinishedIsLoggedAndDoesNotReachTheCaller() {
        val failure = IllegalStateException("app bug")
        callback.onFinishedThrows = failure

        // SuperwallKit is the caller: an exception here would unwind into it.
        adapter.paywallDidFinish(
            swbPaywallInfo("paywall_a"),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
            shouldDismiss = false,
        )

        assertEquals(1, log.entries.size)
        assertEquals("PaywallViewCallback.onFinished threw an exception", log.entries.single().first)
        assertSame(failure, log.entries.single().second)

        // The adapter keeps delivering afterwards.
        callback.onFinishedThrows = null
        adapter.paywallDidFinish(
            swbPaywallInfo("paywall_b"),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
            shouldDismiss = true,
        )
        assertEquals(listOf(Finished("paywall_b", PaywallResult.Declined, true)), callback.finished)
    }

    @Test
    fun customCallbackGoesToTheAppAndItsResultGoesBack() {
        callback.customCallbackAnswer = { CustomCallbackResult.success(mapOf("granted" to true)) }
        val answers = mutableListOf<SWBCustomCallbackResult?>()

        adapter.onCustomCallback(SWBCustomCallback("validate", mapOf<Any?, Any>("code" to "ABC"))) { answers += it }

        assertEquals(listOf(CustomCallback("validate", mapOf("code" to "ABC"))), callback.customCallbacks)
        val answer = answers.single()!!
        assertEquals(SWBCustomCallbackResultStatusSuccess, answer.status())
        assertEquals(true, answer.data()?.get("granted"))
    }

    @Test
    fun customCallbackAnswersOnlyOnceTheAppsSuspendFunctionReturns() {
        val gate = CompletableDeferred<CustomCallbackResult>()
        callback.customCallbackAnswer = { gate.await() }
        val answers = mutableListOf<SWBCustomCallbackResult?>()

        adapter.onCustomCallback(SWBCustomCallback("slow", null)) { answers += it }
        assertTrue(answers.isEmpty())

        gate.complete(CustomCallbackResult.success())
        assertEquals(SWBCustomCallbackResultStatusSuccess, answers.single()!!.status())
    }

    @Test
    fun aThrowingCustomCallbackAnswersFailureWithTheMessageAndIsLogged() {
        val failure = IllegalArgumentException("bad code")
        callback.customCallbackAnswer = { throw failure }
        val answers = mutableListOf<SWBCustomCallbackResult?>()

        adapter.onCustomCallback(SWBCustomCallback("validate", null)) { answers += it }

        val answer = answers.single()!!
        assertEquals(SWBCustomCallbackResultStatusFailure, answer.status())
        assertEquals("bad code", answer.data()?.get("error"))
        assertEquals("PaywallViewCallback.onCustomCallback threw an exception", log.entries.single().first)
        assertSame(failure, log.entries.single().second)
    }

    @Test
    fun afterDetachAFinishIsDropped() {
        adapter.detach()

        adapter.paywallDidFinish(
            swbPaywallInfo("paywall_a"),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
            shouldDismiss = false,
        )

        assertTrue(callback.finished.isEmpty())
        assertTrue(log.entries.isEmpty())
    }

    @Test
    fun afterDetachACustomCallbackAnswersFailureWithoutAskingTheApp() {
        callback.customCallbackAnswer = { CustomCallbackResult.success() }
        adapter.detach()
        val answers = mutableListOf<SWBCustomCallbackResult?>()

        adapter.onCustomCallback(SWBCustomCallback("validate", null)) { answers += it }

        assertTrue(callback.customCallbacks.isEmpty())
        // Still exactly one answer: the paywall's web view is waiting on it.
        val answer = answers.single()!!
        assertEquals(SWBCustomCallbackResultStatusFailure, answer.status())
        assertEquals(null, answer.data())
    }

    /**
     * `Main.immediate` only runs in place on the main thread; otherwise the
     * delivery is queued, and a destroy() can land before it runs. A queueing
     * dispatcher reproduces that ordering.
     */
    @Test
    fun aDeliveryStillQueuedWhenDetachedIsDropped() {
        val dispatcher = StandardTestDispatcher()
        val queued = PaywallViewDelegateAdapter(callback, CoroutineScope(dispatcher), log.log)
        callback.customCallbackAnswer = { CustomCallbackResult.success() }
        val answers = mutableListOf<SWBCustomCallbackResult?>()

        queued.paywallDidFinish(
            swbPaywallInfo("paywall_a"),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
            shouldDismiss = false,
        )
        queued.onCustomCallback(SWBCustomCallback("validate", null)) { answers += it }
        queued.detach()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(callback.finished.isEmpty())
        assertTrue(callback.customCallbacks.isEmpty())
        assertEquals(SWBCustomCallbackResultStatusFailure, answers.single()!!.status())
    }

    // -- IosEmbeddedPaywall --------------------------------------------------

    @Test
    fun infoAndViewControllerComeFromTheNativePaywall() {
        val native = FakeNativePaywall("paywall_a")
        val paywall = IosEmbeddedPaywall("campaign_trigger", native, adapter)

        assertEquals("campaign_trigger", paywall.placement)
        assertEquals("paywall_a", paywall.info.identifier)
        assertSame(native.controller, paywall.viewController)

        // `info` is the paywall as it is now, not a snapshot from creation.
        native.identifier = "paywall_b"
        assertEquals("paywall_b", paywall.info.identifier)
    }

    @Test
    fun afterDestroyTheCallbackHearsNothingMore() {
        val paywall = IosEmbeddedPaywall("campaign_trigger", FakeNativePaywall("paywall_a"), adapter)
        callback.customCallbackAnswer = { CustomCallbackResult.success() }

        paywall.destroy()

        // What SuperwallKit sends when the controller leaves the screen after
        // destroy(): the report commit 047811f stopped.
        adapter.paywallDidFinish(
            swbPaywallInfo("paywall_a"),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
            shouldDismiss = false,
        )
        val answers = mutableListOf<SWBCustomCallbackResult?>()
        adapter.onCustomCallback(SWBCustomCallback("validate", null)) { answers += it }

        assertTrue(callback.finished.isEmpty())
        assertTrue(callback.customCallbacks.isEmpty())
        assertEquals(SWBCustomCallbackResultStatusFailure, answers.single()!!.status())
    }

    @Test
    fun beforeDestroyThePaywallsAdapterStillReports() {
        IosEmbeddedPaywall("campaign_trigger", FakeNativePaywall("paywall_a"), adapter)

        adapter.paywallDidFinish(
            swbPaywallInfo("paywall_a"),
            SWBPaywallResult(SWBPaywallResultCaseDeclined, null),
            shouldDismiss = true,
        )

        assertEquals(listOf(Finished("paywall_a", PaywallResult.Declined, true)), callback.finished)
    }

    @Test
    fun infoStillAnswersAfterDestroyWithTheInfoAsOfDestroy() {
        val native = FakeNativePaywall("paywall_a")
        val paywall = IosEmbeddedPaywall("campaign_trigger", native, adapter)
        native.identifier = "paywall_b"

        paywall.destroy()
        val readsAtDestroy = native.infoReads
        // The released paywall is not consulted again, whatever it would say.
        native.identifier = "paywall_c"

        assertEquals("paywall_b", paywall.info.identifier)
        assertEquals(readsAtDestroy, native.infoReads)
    }

    @Test
    fun viewControllerFailsAfterDestroy() {
        val paywall = IosEmbeddedPaywall("campaign_trigger", FakeNativePaywall("paywall_a"), adapter)

        paywall.destroy()

        val error = assertFailsWith<IllegalStateException> { paywall.viewController }
        assertTrue(error.message!!.contains("destroyed"), error.message)
    }

    @Test
    fun destroyTwiceIsSafe() {
        val native = FakeNativePaywall("paywall_a")
        val paywall = IosEmbeddedPaywall("campaign_trigger", native, adapter)

        paywall.destroy()
        val readsAfterFirst = native.infoReads
        paywall.destroy()

        // The second call does nothing, the released paywall included.
        assertEquals(readsAfterFirst, native.infoReads)
        assertEquals("paywall_a", paywall.info.identifier)
    }

    private data class Finished(
        val identifier: String?,
        val result: PaywallResult,
        val shouldDismiss: Boolean,
    )

    private class RecordingCallback : PaywallViewCallback {
        val finished = mutableListOf<Finished>()
        val customCallbacks = mutableListOf<CustomCallback>()
        var onFinishedThrows: Throwable? = null
        var customCallbackAnswer: suspend () -> CustomCallbackResult = { CustomCallbackResult.failure() }

        override fun onFinished(
            paywallInfo: PaywallInfo,
            result: PaywallResult,
            shouldDismiss: Boolean,
        ) {
            onFinishedThrows?.let { throw it }
            finished += Finished(paywallInfo.identifier, result, shouldDismiss)
        }

        override suspend fun onCustomCallback(callback: CustomCallback): CustomCallbackResult {
            customCallbacks += callback
            return customCallbackAnswer()
        }
    }

    private class FakeNativePaywall(
        var identifier: String,
    ) : NativeEmbeddedPaywall {
        // Lazy: the first UIKit object in a process with no app costs seconds.
        val controller by lazy { UIViewController() }
        var infoReads = 0

        override fun viewController(): Any? = controller

        override fun currentInfo(): SWBPaywallInfo {
            infoReads++
            return swbPaywallInfo(identifier)
        }
    }
}
