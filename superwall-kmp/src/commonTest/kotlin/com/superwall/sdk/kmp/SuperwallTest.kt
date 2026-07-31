package com.superwall.sdk.kmp

import kotlin.test.Test
import kotlin.test.assertNotNull

class SuperwallTest {

    @Test
    fun superwallEntryPointExists() {
        assertNotNull(Superwall)
    }
}
