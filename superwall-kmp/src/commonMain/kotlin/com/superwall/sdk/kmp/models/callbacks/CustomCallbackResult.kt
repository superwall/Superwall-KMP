package com.superwall.sdk.kmp.models.callbacks

/**
 * The result status of a custom callback.
 */
public enum class CustomCallbackResultStatus {
    /** The callback completed successfully. */
    SUCCESS,

    /** The callback failed. */
    FAILURE,
}

/**
 * The result to return from a custom callback handler.
 *
 * The status determines which branch (onSuccess/onFailure) executes in the
 * paywall. Optional data can be returned and accessed as
 * `callbacks.<name>.data.<key>` in the paywall.
 *
 * @property status Whether the callback succeeded or failed.
 * @property data Optional key-value pairs to return to the paywall.
 */
public data class CustomCallbackResult(
    val status: CustomCallbackResultStatus,
    val data: Map<String, Any?>? = null,
) {
    public companion object {
        /** Creates a success result with optional [data] to return to the paywall. */
        public fun success(data: Map<String, Any?>? = null): CustomCallbackResult =
            CustomCallbackResult(
                status = CustomCallbackResultStatus.SUCCESS,
                data = data,
            )

        /** Creates a failure result with optional [data] to return to the paywall. */
        public fun failure(data: Map<String, Any?>? = null): CustomCallbackResult =
            CustomCallbackResult(
                status = CustomCallbackResultStatus.FAILURE,
                data = data,
            )
    }
}
