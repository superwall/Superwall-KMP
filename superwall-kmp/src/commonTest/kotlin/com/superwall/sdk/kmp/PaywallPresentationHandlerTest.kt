package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PaywallResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaywallPresentationHandlerTest {
    @Test
    fun onWillDismissIsStoredSeparatelyFromOnDismiss() {
        val handler = PaywallPresentationHandler()
        assertNull(handler.onWillDismissHandler)

        val seen = mutableListOf<String>()
        handler.onWillDismiss { _, _ -> seen += "willDismiss" }
        handler.onDismiss { _, _ -> seen += "dismiss" }

        handler.onWillDismissHandler?.invoke(PaywallInfo(), PaywallResult.Declined)
        handler.onDismissHandler?.invoke(PaywallInfo(), PaywallResult.Declined)
        assertEquals(listOf("willDismiss", "dismiss"), seen)
    }
}
