package com.superwall.sdk.kmp.models.redemption

/**
 * Info about an expired redemption code.
 *
 * @property resent Whether the redemption email has been resent.
 * @property obfuscatedEmail The obfuscated email address that the redemption
 * email was sent to, if available.
 */
public data class ExpiredCodeInfo(
    val resent: Boolean,
    val obfuscatedEmail: String? = null,
)
