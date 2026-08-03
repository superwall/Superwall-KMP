package com.superwall.sdk.kmp.models.callbacks

/**
 * Represents a custom callback request from the paywall.
 *
 * Custom callbacks allow paywalls to request arbitrary actions from the app
 * and receive results that determine which branch (onSuccess/onFailure)
 * executes in the paywall.
 *
 * @property name The name of the callback being requested.
 * @property variables Optional key-value pairs passed from the paywall.
 * Values are type-preserved (string/number/boolean).
 */
public data class CustomCallback(
    val name: String,
    val variables: Map<String, Any?>? = null,
)
