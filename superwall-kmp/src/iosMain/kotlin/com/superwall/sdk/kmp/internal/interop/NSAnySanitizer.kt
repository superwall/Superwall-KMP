@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.interop

import kotlinx.cinterop.toKString
import platform.Foundation.NSArray
import platform.Foundation.NSDictionary
import platform.Foundation.allKeys
import platform.Foundation.NSMutableArray
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNull
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.setValue

/**
 * Conversions between the untyped `id`-shaped payloads the SWB bridge trades
 * in (`NSDictionary`/`NSArray`/`NSNumber`/`NSNull`) and the common
 * `Map<String, Any?>` model contract.
 *
 * ObjC → Kotlin (read direction): `NSNumber` values are disambiguated via
 * `objCType` — `c`/`B` encodings read as Boolean, `f`/`d` as
 * Double, everything else as Long. `NSNull` becomes Kotlin `null`. Unknown
 * object types degrade to `toString` (degrade, never crash).
 * Kotlin/Native eagerly bridges NSString/NSArray/NSDictionary crossing as
 * `id` into String/List/Map; the explicit NS-class branches below are
 * defensive belts for values that arrive unbridged.
 *
 * Kotlin → ObjC (write direction): values are converted EXPLICITLY into
 * Foundation objects (`NSNumber` via the typed initializers, containers via
 * `NSMutable*`) so the Swift-side `SWBValueSanitizer` always sees genuine
 * Foundation types regardless of Kotlin boxing behavior. `null` becomes
 * `NSNull` — the SWB facade's merge semantics treat `NSNull` values as
 * key-removal for `setUserAttributes`.
 */
internal object NSAnySanitizer {
    // ---- ObjC -> Kotlin ----------------------------------------------------

    /** Converts one `id`-shaped value into a Kotlin model value. */
    fun fromNSAny(value: Any?): Any? =
        when (value) {
            null -> null
            is NSNull -> null
            is NSNumber -> fromNSNumber(value)
            is Boolean -> value
            is Int -> value
            is Long -> value
            is Float -> value.toDouble()
            is Double -> value
            is String -> value
            is Map<*, *> -> fromMap(value)
            is List<*> -> value.map { fromNSAny(it) }
            is NSDictionary -> fromNSDictionary(value)
            is NSArray -> fromNSArray(value)
            is NSString -> value.toString()
            else -> value.toString()
        }

    /** Converts a cinterop `Map<Any?, *>` payload into `Map<String, Any?>`. */
    fun fromMap(value: Map<*, *>): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>(value.size)
        for ((key, entry) in value) {
            val stringKey = (key as? String) ?: key?.toString() ?: continue
            result[stringKey] = fromNSAny(entry)
        }
        return result
    }

    /** Nullable convenience over [fromMap]. */
    fun fromMapOrNull(value: Map<Any?, *>?): Map<String, Any?>? = value?.let { fromMap(it) }

    /**
     * `objCType`-based NSNumber disambiguation `c` (signed char,
     * also CFBoolean's encoding) and `B` (C99 bool) read as Boolean; `f`/`d`
     * as Double; all remaining integral encodings as Long.
     */
    fun fromNSNumber(value: NSNumber): Any {
        return when (value.objCType?.toKString()) {
            "c", "B" -> value.boolValue
            "f", "d" -> value.doubleValue
            else -> value.longLongValue
        }
    }

    private fun fromNSDictionary(value: NSDictionary): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        for (key in value.allKeys) {
            val stringKey = (key as? String) ?: key?.toString() ?: continue
            result[stringKey] = fromNSAny(value.objectForKey(key))
        }
        return result
    }

    private fun fromNSArray(value: NSArray): List<Any?> {
        val result = ArrayList<Any?>(value.count.toInt())
        for (index in 0uL until value.count) {
            result.add(fromNSAny(value.objectAtIndex(index)))
        }
        return result
    }

    // ---- Kotlin -> ObjC ----------------------------------------------------

    /** Converts one Kotlin value into a Foundation object (never null). */
    fun toNSAny(value: Any?): Any =
        when (value) {
            null -> NSNull()
            is NSNull -> value
            is NSNumber -> value
            is Boolean -> NSNumber(bool = value)
            is Int -> NSNumber(int = value)
            is Long -> NSNumber(longLong = value)
            is Float -> NSNumber(float = value)
            is Double -> NSNumber(double = value)
            is String -> value
            is Map<*, *> -> toNSDictionary(value)
            is List<*> -> toNSArray(value)
            is Set<*> -> toNSArray(value.toList())
            else -> value.toString()
        }

    /**
     * Converts a common `Map<String, Any?>` into the `Map<Any?, *>` shape the
     * cinterop signatures take. The top-level container stays a Kotlin map
     * (the interop boundary bridges it to `NSDictionary`); every VALUE is
     * pre-converted into a genuine Foundation object via [toNSAny] so nested
     * containers and numbers cross deterministically.
     */
    fun toNSParams(value: Map<String, Any?>): Map<Any?, *> =
        value.entries.associate { (key, entry) -> key as Any? to toNSAny(entry) }

    private fun toNSDictionary(value: Map<*, *>): NSDictionary {
        val dictionary = NSMutableDictionary()
        for ((key, entry) in value) {
            val stringKey = (key as? String) ?: key?.toString() ?: continue
            dictionary.setValue(toNSAny(entry), forKey = stringKey)
        }
        return dictionary
    }

    private fun toNSArray(value: List<*>): NSArray {
        val array = NSMutableArray()
        for (entry in value) {
            array.addObject(toNSAny(entry))
        }
        return array
    }
}
