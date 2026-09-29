package com.superwall.sdk.kmp.internal.interop

/**
 * Converts between plain JS values and the commonMain value contract
 * (String / Boolean / Long / Double / List / Map, `null` preserved) — the JS
 * counterpart of Android's `AnySanitizer` and iOS's `NSAnySanitizer`.
 *
 * JS has one number type, so integral numbers inside the safe-integer range
 * become [Long] and everything else [Double], matching what the native
 * sanitizers produce for the same JSON. Like them, it degrades rather than
 * throws: anything unrecognised becomes its string rendering.
 */
internal object JsAnySanitizer {
    /** A JS object to `Map<String, Any?>`; `null`/`undefined`/non-objects give `null`. */
    fun fromObject(value: dynamic): Map<String, Any?>? {
        if (value == null || jsTypeOf(value) != "object" || isArray(value)) return null
        val result = LinkedHashMap<String, Any?>()
        for (key in objectKeys(value)) {
            result[key] = fromJs(value[key])
        }
        return result
    }

    /** Recursively converts a single JS value per the contract above. */
    fun fromJs(value: dynamic): Any? =
        try {
            when {
                value == null -> null
                jsTypeOf(value) == "string" -> value as String
                jsTypeOf(value) == "boolean" -> value as Boolean
                jsTypeOf(value) == "number" -> numberFromJs(value as Double)
                jsTypeOf(value) == "bigint" -> value.toString().toLong()
                isArray(value) -> (value as Array<dynamic>).map { fromJs(it) }
                value is Throwable -> (value as Throwable).message ?: value.toString()
                jsTypeOf(value) == "object" -> fromObject(value)
                else -> value.toString()
            }
        } catch (t: Throwable) {
            try {
                value.toString()
            } catch (t2: Throwable) {
                null
            }
        }

    /** A commonMain map to a plain JS object (`null` values kept as `null`). */
    fun toObject(map: Map<String, Any?>): dynamic {
        val obj: dynamic = js("({})")
        for ((key, value) in map) {
            obj[key] = toJs(value)
        }
        return obj
    }

    /** Recursively converts a commonMain value to its plain JS form. */
    fun toJs(value: Any?): dynamic =
        when (value) {
            null -> null
            is String, is Boolean, is Double, is Int -> value
            is Long -> value.toDouble()
            is Float -> value.toDouble()
            is Short -> value.toInt()
            is Byte -> value.toInt()
            is Map<*, *> -> {
                val obj: dynamic = js("({})")
                for ((key, entry) in value) obj[key.toString()] = toJs(entry)
                obj
            }
            is Iterable<*> -> value.map { toJs(it) }.toTypedArray()
            is Array<*> -> value.map { toJs(it) }.toTypedArray()
            is Enum<*> -> value.name
            else -> value.toString()
        }

    private fun numberFromJs(number: Double): Any =
        if (number == kotlin.math.floor(number) && kotlin.math.abs(number) <= MAX_SAFE_INTEGER) {
            number.toLong()
        } else {
            number
        }

    private const val MAX_SAFE_INTEGER = 9007199254740991.0
}

internal fun isArray(value: dynamic): Boolean = js("Array").isArray(value) as Boolean

internal fun objectKeys(value: dynamic): Array<String> = js("Object").keys(value).unsafeCast<Array<String>>()

/** `value[key]` as a String, or `null` when absent / not a string. */
internal fun stringOrNull(obj: dynamic, key: String): String? {
    val raw = if (obj == null) null else obj[key]
    return if (jsTypeOf(raw) == "string") raw as String else null
}

/** `value[key]` as a Boolean, or `null` when absent / not a boolean. */
internal fun booleanOrNull(obj: dynamic, key: String): Boolean? {
    val raw = if (obj == null) null else obj[key]
    return if (jsTypeOf(raw) == "boolean") raw as Boolean else null
}

/** `value[key]` as a Double, or `null` when absent / not a number. */
internal fun numberOrNull(obj: dynamic, key: String): Double? {
    val raw = if (obj == null) null else obj[key]
    return if (jsTypeOf(raw) == "number") raw as Double else null
}

/** `value[key]` as an array (empty when absent / not an array). */
internal fun arrayOrEmpty(obj: dynamic, key: String): Array<dynamic> {
    val raw = if (obj == null) null else obj[key]
    return if (raw != null && isArray(raw)) raw.unsafeCast<Array<dynamic>>() else emptyArray()
}

/** A fresh empty JS object literal. */
internal fun jsObject(): dynamic = js("({})")
