package com.superwall.sdk.kmp

/**
 * The only exception family thrown by the SDK's public API.
 *
 * Domain outcomes never throw — they stay in the sealed result types
 * (e.g. `PurchaseResult.Failed`, `RestorationResult.Failed`, `TriggerResult.Error`).
 * Infrastructure failures throw a [SuperwallError] subtype.
 */
public sealed class SuperwallError(
    message: String?,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * Thrown when an SDK method is called before [Superwall.configure].
     *
     * Call `Superwall.configure` (or `configureAndAwait`) first, or gate calls
     * on `Superwall.isConfigured`.
     */
    public class NotConfigured : SuperwallError("Superwall.configure must be called first")

    /**
     * Thrown on Android when the SDK's automatic initializer was stripped or
     * disabled (e.g. androidx.startup removed), so no Application context is
     * available.
     *
     * @param message An actionable description of how to initialize manually.
     */
    public class NotInitialized(
        message: String,
    ) : SuperwallError(message)

    /**
     * Thrown when an API is not available on the current platform.
     *
     * @param api The name of the unavailable API.
     * @param platform The platform on which it is unavailable.
     */
    public class NotSupportedOnPlatform(
        api: String,
        platform: String,
    ) : SuperwallError("$api is not supported on $platform")

    /**
     * Thrown or delivered via the configure completion when the native SDK
     * fails to configure (e.g. an invalid API key or network failure).
     *
     * @param message A description of the configuration failure, if available.
     */
    public class ConfigurationFailed(
        message: String?,
    ) : SuperwallError(message)

    /**
     * Wraps a failure raised by the underlying native SDK.
     *
     * Android wraps `kotlin.Result` failures; iOS wraps `NSError` with
     * domain/code/localizedDescription preserved in the message.
     *
     * @param message A description of the native failure, if available.
     * @param cause The underlying native throwable, when one exists.
     */
    public class Native(
        message: String?,
        cause: Throwable? = null,
    ) : SuperwallError(message, cause)
}
