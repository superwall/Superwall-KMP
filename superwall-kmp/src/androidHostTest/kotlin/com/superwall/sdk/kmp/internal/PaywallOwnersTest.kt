package com.superwall.sdk.kmp.internal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Two embedded hosts handed the same cached paywall view. */
class PaywallOwnersTest {
    private class View

    private class Host

    @Test
    fun aSecondHostTakesTheViewFromTheFirst() {
        val owners = PaywallOwners<View, Host>()
        val view = View()
        val first = Host()
        val second = Host()

        assertNull(owners.claim(view, first))
        assertSame(first, owners.claim(view, second))

        assertFalse(owners.owns(view, first))
        assertTrue(owners.owns(view, second))
    }

    @Test
    fun theSupersededHostsReleaseLeavesTheNewOwnerAlone() {
        val owners = PaywallOwners<View, Host>()
        val view = View()
        val first = Host()
        val second = Host()
        owners.claim(view, first)
        owners.claim(view, second)

        // The first host's destroy() must not tear down what the second shows.
        assertFalse(owners.release(view, first))
        assertTrue(owners.owns(view, second))
        assertTrue(owners.release(view, second))
        assertFalse(owners.owns(view, second))
    }

    @Test
    fun aViewReleasedByItsOwnerIsClaimedFresh() {
        val owners = PaywallOwners<View, Host>()
        val view = View()
        val first = Host()
        owners.claim(view, first)
        assertTrue(owners.release(view, first))

        assertNull(owners.claim(view, Host()))
    }

    @Test
    fun distinctViewsAreTrackedSeparately() {
        val owners = PaywallOwners<View, Host>()
        val first = Host()
        val second = Host()
        owners.claim(View(), first)

        assertNull(owners.claim(View(), second))
    }
}
