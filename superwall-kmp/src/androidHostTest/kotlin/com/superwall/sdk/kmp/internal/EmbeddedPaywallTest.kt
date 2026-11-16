package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.internal.mappers.toNative
import com.superwall.sdk.kmp.models.options.PaywallOptions
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason as NativeRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason as NativePaywallSkippedReason

/** How a failed `PaywallBuilder.build()` is told apart, against the REAL native throwables. */
class EmbeddedPaywallTest {
    @Test
    fun aSkipReasonThrowableIsASkipNotAnError() {
        assertEquals(
            PaywallRetrievalFailure.Skipped(PaywallSkippedReason.PlacementNotFound),
            NativePaywallSkippedReason.PlacementNotFound().toPaywallRetrievalFailure(),
        )
        assertEquals(
            PaywallRetrievalFailure.Skipped(PaywallSkippedReason.NoAudienceMatch),
            NativePaywallSkippedReason.NoAudienceMatch().toPaywallRetrievalFailure(),
        )
    }

    @Test
    fun aRequestStatusReasonReportsItsInfoText() {
        val reason = NativeRequestStatusReason.DebuggerPresented()
        assertEquals(PaywallRetrievalFailure.Failed(reason.info), reason.toPaywallRetrievalFailure())
    }

    @Test
    fun anyOtherThrowableReportsItsMessage() {
        assertEquals(
            PaywallRetrievalFailure.Failed("no network"),
            IllegalStateException("no network").toPaywallRetrievalFailure(),
        )
    }

    @Test
    fun loadingColorResourceReachesTheNativeOptionsAndIsUnsetByDefault() {
        assertEquals(42, PaywallOptions(loadingColor = 42).toNative().loadingColor)
        assertNull(PaywallOptions().toNative().loadingColor)
    }
}
