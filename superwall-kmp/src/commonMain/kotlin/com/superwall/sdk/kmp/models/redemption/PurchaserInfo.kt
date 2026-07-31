package com.superwall.sdk.kmp.models.redemption

/**
 * Info about the purchaser behind a redeemed code.
 *
 * @property appUserId The app user ID of the purchaser.
 * @property email The email address of the purchaser, if known.
 * @property storeIdentifiers The identifiers of the store that was purchased from.
 */
public data class PurchaserInfo(
    val appUserId: String,
    val email: String? = null,
    val storeIdentifiers: StoreIdentifiers,
)
