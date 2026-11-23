package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.PaywallViewCallback
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PaywallResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The embedded host view's lifecycle. The host and the native `PaywallView`
 * are Android views a host test can't construct, so this drives the
 * [EmbeddedPresentation] the host delegates to, with the views behind a fake
 * [PaywallSurface].
 */
class EmbeddedPresentationTest {
    private class Paywall(var info: PaywallInfo = PaywallInfo(identifier = "paywall"))

    /** Stands in for the host `FrameLayout`, the paywall's parent and the owner record. */
    private class Surface : PaywallSurface<Paywall> {
        override var isInWindow = false
        var parents = mutableMapOf<Paywall, Any>()
        var owned = true
        val presented = mutableListOf<Paywall>()
        val released = mutableListOf<Paywall>()
        val dismantled = mutableListOf<Paywall>()

        override fun info(paywall: Paywall): PaywallInfo = paywall.info

        override fun hasParent(paywall: Paywall): Boolean = paywall in parents

        override fun isInHost(paywall: Paywall): Boolean = parents[paywall] === this

        override fun present(paywall: Paywall) {
            parents[paywall] = this
            presented += paywall
        }

        override fun release(paywall: Paywall): Boolean {
            released += paywall
            return owned
        }

        override fun dismantle(paywall: Paywall) {
            parents.remove(paywall)
            dismantled += paywall
        }
    }

    private class Callback : PaywallViewCallback {
        val events = mutableListOf<String>()
        var failWith: Throwable? = null

        override fun onFinished(
            paywallInfo: PaywallInfo,
            result: PaywallResult,
            shouldDismiss: Boolean,
        ) {
            events += "finished:${paywallInfo.identifier}:$result:$shouldDismiss"
            failWith?.let { throw it }
        }

        override fun onPresent(paywallInfo: PaywallInfo) {
            events += "present:${paywallInfo.identifier}"
            failWith?.let { throw it }
        }

        override fun onError(error: String) {
            events += "error:$error"
        }
    }

    private val surface = Surface()
    private val callback = Callback()
    private val logged = mutableListOf<Pair<String, Throwable?>>()
    private val presentation = EmbeddedPresentation(callback, { message, error -> logged += message to error }, surface)
    private val paywall = Paywall()

    /** What `onAttachedToWindow` does. */
    private fun attach() {
        surface.isInWindow = true
        presentation.show()
    }

    @Test
    fun aPaywallRetrievedBeforeTheHostIsInAWindowWaitsForIt() {
        presentation.setPaywall(paywall)

        assertEquals(emptyList(), surface.presented)
        assertEquals(emptyList(), callback.events)
        assertNull(presentation.shown)

        attach()

        assertEquals(listOf(paywall), surface.presented)
        assertEquals(listOf("present:paywall"), callback.events)
        assertSame(paywall, presentation.shown)
    }

    @Test
    fun aPaywallRetrievedForAHostAlreadyInAWindowIsShownAtOnce() {
        attach()
        assertEquals(emptyList(), callback.events) // Attached with nothing to show yet.

        presentation.setPaywall(paywall)

        assertEquals(listOf(paywall), surface.presented)
        assertEquals(listOf("present:paywall"), callback.events)
    }

    @Test
    fun reattachingAfterADetachNeitherPresentsNorReportsAgain() {
        presentation.setPaywall(paywall)
        attach()

        // Compose and navigation detach and re-attach the host routinely. A
        // detach isn't acted on at all, so only the second attach reaches here.
        surface.isInWindow = false
        attach()

        assertEquals(listOf(paywall), surface.presented)
        assertEquals(listOf("present:paywall"), callback.events)
        assertEquals(emptyList(), surface.dismantled)
        assertSame(paywall, presentation.shown)
    }

    @Test
    fun aPaywallTakenByAnotherViewWhileTheHostWaitedIsAnErrorNotAPresentation() {
        presentation.setPaywall(paywall)
        surface.parents[paywall] = "a full-screen presentation"

        attach()

        assertEquals(emptyList(), surface.presented)
        assertEquals(listOf("error:The retrieved paywall is already shown in another view."), callback.events)
        assertEquals(listOf(paywall), surface.released)
        // Let go of for good: neither a later attach nor destroy() touches the other view's paywall.
        attach()
        presentation.teardown()
        assertEquals(emptyList(), surface.presented)
        assertEquals(emptyList(), surface.dismantled)
    }

    @Test
    fun destroyTakesAShownPaywallDownAndASecondDestroyDoesNothing() {
        presentation.setPaywall(paywall)
        attach()

        presentation.teardown()

        assertEquals(listOf(paywall), surface.dismantled)
        assertEquals(listOf(paywall), surface.released)
        assertNull(presentation.shown)

        presentation.teardown()

        // A second native teardown would hit a cached view the SDK may have handed out again.
        assertEquals(listOf(paywall), surface.dismantled)
        assertEquals(listOf(paywall), surface.released)
    }

    @Test
    fun destroyBeforeAttachLeavesTheNeverShownPaywallToTheSdksCache() {
        presentation.setPaywall(paywall)

        presentation.teardown()

        assertEquals(listOf(paywall), surface.released)
        assertEquals(emptyList(), surface.dismantled)

        // The host can still be attached afterwards, e.g. by a layout pass already under way.
        attach()

        assertEquals(emptyList(), surface.presented)
        assertEquals(emptyList(), callback.events)
    }

    @Test
    fun destroyWithNoPaywallDoesNothing() {
        presentation.teardown()

        assertEquals(emptyList(), surface.released)
        assertEquals(emptyList(), surface.dismantled)
    }

    @Test
    fun destroyLeavesAloneAPaywallAnotherHostNowOwns() {
        presentation.setPaywall(paywall)
        attach()
        surface.owned = false

        presentation.teardown()

        assertEquals(emptyList(), surface.dismantled)
    }

    @Test
    fun destroyLeavesAloneAPaywallThatIsNowInAnotherView() {
        presentation.setPaywall(paywall)
        attach()
        surface.parents[paywall] = "a full-screen presentation"

        presentation.teardown()

        assertEquals(emptyList(), surface.dismantled)
    }

    @Test
    fun finishedIsDeliveredWhileThePaywallIsHeld() {
        presentation.setPaywall(paywall)
        attach()

        presentation.finished(paywall, shouldDismiss = true) { PaywallResult.Purchased("premium") }

        assertEquals("finished:paywall:Purchased(productId=premium):true", callback.events.last())
    }

    @Test
    fun nothingIsDeliveredAfterDestroy() {
        presentation.setPaywall(paywall)
        attach()
        presentation.teardown()
        callback.events.clear()

        // The cached native view keeps the host's callback and can still report through it.
        presentation.finished(paywall, shouldDismiss = true) { PaywallResult.Declined }
        attach()

        assertEquals(emptyList(), callback.events)
    }

    @Test
    fun finishedFromAPaywallThisHostNeverHeldIsIgnored() {
        presentation.setPaywall(paywall)
        attach()
        callback.events.clear()

        presentation.finished(Paywall(), shouldDismiss = true) { PaywallResult.Declined }

        assertEquals(emptyList(), callback.events)
    }

    @Test
    fun aSupersededHostIsToldAndLetsGoWithoutTearingThePaywallDown() {
        presentation.setPaywall(paywall)
        attach()
        callback.events.clear()

        presentation.supersede(paywall)

        assertTrue(callback.events.single().startsWith("error:The paywall was taken over by another view"))
        assertNull(presentation.shown)

        // Its paywall is on screen in the other host now: it must not be torn
        // down from here, and what it reports is the other host's to deliver.
        presentation.teardown()
        presentation.finished(paywall, shouldDismiss = true) { PaywallResult.Declined }

        assertEquals(emptyList(), surface.dismantled)
        assertEquals(1, callback.events.size)
    }

    @Test
    fun supersedingAPaywallTheHostDoesNotHoldIsIgnored() {
        presentation.setPaywall(paywall)
        attach()
        callback.events.clear()

        presentation.supersede(Paywall())

        assertEquals(emptyList(), callback.events)
        assertSame(paywall, presentation.shown)
    }

    @Test
    fun infoFollowsThePaywallAndKeepsItsLastValueAfterDestroy() {
        assertEquals(PaywallInfo(), presentation.info)

        presentation.setPaywall(paywall)
        assertEquals("paywall", presentation.info.identifier)

        paywall.info = PaywallInfo(identifier = "updated")
        presentation.teardown()
        paywall.info = PaywallInfo(identifier = "reused by another request")

        assertEquals("updated", presentation.info.identifier)
    }

    @Test
    fun anExceptionFromTheAppsCallbackIsLoggedAndDoesNotBreakThePresentation() {
        val failure = IllegalStateException("app bug")
        callback.failWith = failure
        presentation.setPaywall(paywall)

        attach()
        presentation.finished(paywall, shouldDismiss = false) { PaywallResult.Restored }

        assertEquals(
            listOf<Pair<String, Throwable?>>(
                "PaywallViewCallback.onPresent threw an exception" to failure,
                "PaywallViewCallback.onFinished threw an exception" to failure,
            ),
            logged,
        )
        assertSame(paywall, presentation.shown)
    }
}
