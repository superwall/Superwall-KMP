package com.superwall.sdk.kmp.internal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A purchase observation queued, or still looking its product up, when the
 * SDK is torn down must not report to the instance configured afterwards.
 */
class ObservationSessionTest {
    @Test
    fun anObservationMadeBeforeATeardownIsNoLongerCurrent() {
        val session = ObservationSession()
        val before = session.ticket()
        assertTrue(session.isCurrent(before))

        session.end()

        assertFalse(session.isCurrent(before))
        assertTrue(session.isCurrent(session.ticket()))
    }
}
