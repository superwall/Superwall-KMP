package com.superwall.sdk.kmp.internal.mappers

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for AnySanitizer.kt: arbitrary native payload values normalize to the
 * documented commonMain value contract (String / Boolean / Long / Double /
 * List / Map / Set), degrading unknowns to `toString()` and never crashing.
 *
 * Note: the `org.json.JSONObject`/`JSONArray` branches are NOT covered here —
 * org.json is an android.jar stub on the JVM host-test classpath (its methods
 * throw at runtime without a device), so those branches need an
 * instrumented/Robolectric environment.
 */
class AnySanitizerTest {
    // ---- Scalars ----------------------------------------------------------------

    @Test
    fun scalars_passThroughOrWiden() {
        assertEquals("hello", sanitizeAny("hello"))
        assertEquals(true, sanitizeAny(true))
        assertEquals(42L, sanitizeAny(42L))
        assertEquals(42L, sanitizeAny(42))
        assertEquals(7L, sanitizeAny(7.toShort()))
        assertEquals(3L, sanitizeAny(3.toByte()))
        assertEquals(1.5, sanitizeAny(1.5))
        assertEquals(2.5, sanitizeAny(2.5f))
        assertEquals(9.99, sanitizeAny(BigDecimal("9.99")))
        assertEquals(123L, sanitizeAny(BigInteger("123")))
        assertEquals("x", sanitizeAny('x'))
        assertNull(sanitizeAny(null))
    }

    // ---- Nested containers --------------------------------------------------------

    @Test
    fun nestedMapsAndLists_sanitizeRecursively() {
        val input =
            mapOf(
                "outer" to
                    mapOf(
                        "count" to 1,
                        "items" to listOf(1, "two", 3.0f, null),
                    ),
                "flags" to listOf(true, false),
            )

        val result = sanitizeParams(input)

        assertEquals(
            mapOf(
                "outer" to
                    mapOf(
                        "count" to 1L,
                        "items" to listOf(1L, "two", 3.0, null),
                    ),
                "flags" to listOf(true, false),
            ),
            result,
        )
    }

    @Test
    fun nonStringAndNullKeys_becomeStrings() {
        val result = sanitizeParams(mapOf(1 to "one", null to "nothing"))
        assertEquals(mapOf("1" to "one", "null" to "nothing"), result)
    }

    @Test
    fun nullParams_returnNull() {
        assertNull(sanitizeParams(null))
    }

    @Test
    fun nullValues_arePreserved() {
        // Unlike the Flutter host, nulls survive: the contract is Map<String, Any?>.
        assertEquals(mapOf("a" to null), sanitizeParams(mapOf("a" to null)))
    }

    @Test
    fun setsStaySetsAndArraysBecomeLists() {
        assertEquals(setOf(1L, 2L), sanitizeAny(setOf(1, 2)))
        assertEquals(listOf("a", 1L), sanitizeAny(arrayOf<Any>("a", 1)))
    }

    // ---- Degrade cases ---------------------------------------------------------------

    private enum class SampleEnum { SOME_CASE }

    @Test
    fun enums_degradeToName() {
        assertEquals("SOME_CASE", sanitizeAny(SampleEnum.SOME_CASE))
    }

    private class Opaque {
        override fun toString(): String = "opaque-object"
    }

    @Test
    fun unknownObjects_degradeToToString() {
        assertEquals("opaque-object", sanitizeAny(Opaque()))
    }

    private class ThrowingToString {
        override fun toString(): String = throw IllegalStateException("boom")
    }

    @Test
    fun throwingToString_degradesToNullInsteadOfCrashing() {
        assertNull(sanitizeAny(ThrowingToString()))
    }

    // ---- kotlinx.serialization JsonElement --------------------------------------------

    @Test
    fun jsonElementTrees_sanitizeToPlainValues() {
        val json =
            buildJsonObject {
                put("string", "s")
                put("bool", true)
                put("long", 42L)
                put("double", 1.5)
                put("nullish", JsonNull)
                put(
                    "array",
                    buildJsonArray {
                        add(1L)
                        add("two")
                    },
                )
                put("nested", buildJsonObject { put("inner", false) })
            }

        val result = sanitizeAny(json)

        assertEquals(
            mapOf(
                "string" to "s",
                "bool" to true,
                "long" to 42L,
                "double" to 1.5,
                "nullish" to null,
                "array" to listOf(1L, "two"),
                "nested" to mapOf("inner" to false),
            ),
            result,
        )
    }

    @Test
    fun jsonPrimitives_mapByShape() {
        assertEquals("42", sanitizeAny(JsonPrimitive("42"))) // string stays string
        assertEquals(42L, sanitizeAny(JsonPrimitive(42)))
        assertEquals(1.25, sanitizeAny(JsonPrimitive(1.25)))
        assertEquals(false, sanitizeAny(JsonPrimitive(false)))
        assertNull(sanitizeAny(JsonNull))
    }
}
