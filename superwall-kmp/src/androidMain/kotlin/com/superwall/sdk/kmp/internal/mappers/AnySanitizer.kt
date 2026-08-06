package com.superwall.sdk.kmp.internal.mappers

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Ports the Flutter host's `mapParamsForDart` (EventMapper.kt) to the KMP
 * boundary: normalizes arbitrary native payload maps into the documented
 * commonMain value contract (String / Boolean / Long / Double / List / Map /
 * Set). Anything unknown degrades to `toString()` — this sanitizer must never
 * crash (mapping failures degrade, never crash).
 *
 * Beyond the Flutter port it also understands `org.json` (`JSONObject` /
 * `JSONArray`) and kotlinx-serialization `JsonElement` trees, both of which
 * the native SDK hands over in places (e.g. redemption `placementParams`).
 * Unlike the Flutter host, `null` values are preserved rather than filtered —
 * the common contract is `Map<String, Any?>`.
 */
internal fun sanitizeParams(params: Map<*, *>?): Map<String, Any?>? {
    if (params == null) return null
    val result = LinkedHashMap<String, Any?>(params.size)
    for ((key, value) in params) {
        result[key?.toString() ?: "null"] = sanitizeAny(value)
    }
    return result
}

/** Recursively sanitizes a single value per the contract of [sanitizeParams]. */
internal fun sanitizeAny(value: Any?): Any? =
    try {
        when (value) {
            null -> null
            is String -> value
            is Boolean -> value
            is Long -> value
            is Int -> value.toLong()
            is Short -> value.toLong()
            is Byte -> value.toLong()
            is Double -> value
            is Float -> value.toDouble()
            is BigDecimal -> value.toDouble()
            is BigInteger -> value.toLong()
            is Char -> value.toString()
            is Map<*, *> -> sanitizeParams(value)
            is Set<*> -> value.map { sanitizeAny(it) }.toSet()
            is List<*> -> value.map { sanitizeAny(it) }
            is Array<*> -> value.map { sanitizeAny(it) }
            is JSONObject -> sanitizeJsonObject(value)
            is JSONArray -> sanitizeJsonArray(value)
            is JsonElement -> sanitizeJsonElement(value)
            is Enum<*> -> value.name
            else -> value.toString()
        }
    } catch (t: Throwable) {
        // Never crash while sanitizing — fall back to a string rendering.
        try {
            value.toString()
        } catch (t2: Throwable) {
            null
        }
    }

private fun sanitizeJsonObject(json: JSONObject): Map<String, Any?> {
    val result = LinkedHashMap<String, Any?>(json.length())
    val keys = json.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val raw = json.opt(key)
        result[key] = if (raw == null || raw == JSONObject.NULL) null else sanitizeAny(raw)
    }
    return result
}

private fun sanitizeJsonArray(json: JSONArray): List<Any?> =
    (0 until json.length()).map { index ->
        val raw = json.opt(index)
        if (raw == null || raw == JSONObject.NULL) null else sanitizeAny(raw)
    }

private fun sanitizeJsonElement(element: JsonElement): Any? =
    when (element) {
        is JsonNull -> null
        is JsonPrimitive ->
            when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.booleanOrNull
                element.longOrNull != null -> element.longOrNull
                element.doubleOrNull != null -> element.doubleOrNull
                else -> element.content
            }
        is JsonObject -> element.mapValues { (_, v) -> sanitizeJsonElement(v) }
        is JsonArray -> element.map { sanitizeJsonElement(it) }
    }
