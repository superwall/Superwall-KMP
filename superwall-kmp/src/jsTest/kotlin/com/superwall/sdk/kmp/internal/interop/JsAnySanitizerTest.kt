package com.superwall.sdk.kmp.internal.interop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsAnySanitizerTest {
    @Test
    fun convertsJsValuesToTheCommonContract() {
        val map = JsAnySanitizer.fromObject(js("({ s: 'x', b: true, i: 3, d: 1.5, n: null, u: undefined, a: [1, 'y'], o: { k: 2 } })"))
        assertEquals(
            mapOf(
                "s" to "x",
                "b" to true,
                "i" to 3L,
                "d" to 1.5,
                "n" to null,
                "u" to null,
                "a" to listOf(1L, "y"),
                "o" to mapOf("k" to 2L),
            ),
            map,
        )
    }

    @Test
    fun nonObjectsAreNotMaps() {
        assertNull(JsAnySanitizer.fromObject(null))
        assertNull(JsAnySanitizer.fromObject(js("[1, 2]")))
        assertNull(JsAnySanitizer.fromObject("text"))
    }

    @Test
    fun unsafeIntegersStayDoubles() {
        assertEquals(1.0e300, JsAnySanitizer.fromJs(js("1e300")))
    }

    @Test
    fun jsErrorsBecomeTheirMessage() {
        assertEquals("boom", JsAnySanitizer.fromJs(js("new Error('boom')")))
    }

    @Test
    fun commonValuesRoundTrip() {
        val original: Map<String, Any?> =
            mapOf(
                "s" to "x",
                "l" to 42L,
                "d" to 2.5,
                "b" to false,
                "n" to null,
                "list" to listOf("a", 1L),
                "nested" to mapOf("k" to "v"),
            )
        assertEquals(original, JsAnySanitizer.fromObject(JsAnySanitizer.toObject(original)))
    }

    @Test
    fun setsAndEnumsBecomeArraysAndNames() {
        val obj = JsAnySanitizer.toObject(mapOf("set" to setOf("a"), "enum" to DeprecationLevel.WARNING))
        assertEquals(mapOf("set" to listOf("a"), "enum" to "WARNING"), JsAnySanitizer.fromObject(obj))
    }
}
