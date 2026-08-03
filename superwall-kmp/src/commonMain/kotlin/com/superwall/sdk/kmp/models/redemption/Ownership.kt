package com.superwall.sdk.kmp.models.redemption

/**
 * Specifies who owns a redeemed code.
 */
public sealed interface Ownership {
    /**
     * The code belongs to an identified app user.
     *
     * @property appUserId The app user ID that owns the code.
     */
    public data class AppUser(
        val appUserId: String,
    ) : Ownership

    /**
     * The code belongs to a device.
     *
     * @property deviceId The device ID that owns the code.
     */
    public data class Device(
        val deviceId: String,
    ) : Ownership
}
