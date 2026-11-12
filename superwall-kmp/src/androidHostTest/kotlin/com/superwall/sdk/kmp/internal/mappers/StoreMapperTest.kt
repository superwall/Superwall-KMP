package com.superwall.sdk.kmp.internal.mappers

import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Tests for the date-conversion helpers in StoreMapper.kt: native
 * `java.util.Date` <-> common `kotlin.time.Instant` via epoch milliseconds
 * (no ISO-8601 string round trip).
 */
class StoreMapperTest {
    @Test
    fun dateToInstant_usesEpochMilliseconds() {
        val epochMs = 1_712_345_678_901L
        assertEquals(Instant.fromEpochMilliseconds(epochMs), Date(epochMs).toKmpInstant())
    }

    @Test
    fun instantToDate_usesEpochMilliseconds() {
        val epochMs = 1_712_345_678_901L
        assertEquals(Date(epochMs), Instant.fromEpochMilliseconds(epochMs).toNativeDate())
    }

    @Test
    fun epochZeroAndPreEpochDates_roundTrip() {
        for (epochMs in listOf(0L, -86_400_000L, 1L, 253_402_300_799_999L)) {
            val date = Date(epochMs)
            assertEquals(date, date.toKmpInstant().toNativeDate(), "round trip failed for $epochMs")
            assertEquals(epochMs, date.toKmpInstant().toEpochMilliseconds())
        }
    }

    @Test
    fun subMillisecondPrecisionIsNotInvented() {
        // Date carries millisecond precision; the Instant must carry exactly that.
        val instant = Date(999L).toKmpInstant()
        assertEquals(999_000_000, instant.nanosecondsOfSecond)
        assertEquals(0L, instant.epochSeconds)
    }
}

/** The transaction fields added for parity, against the REAL native transaction type. */
class StoreTransactionMapperTest {
    @Test
    fun stateTokenSignatureAndPaymentAreCarried() {
        val native =
            com.superwall.sdk.store.abstractions.transactions.GoogleBillingPurchaseTransaction(
                transactionDate = Date(1_000L),
                originalTransactionIdentifier = "GPA.1",
                state = com.superwall.sdk.store.abstractions.transactions.StoreTransactionState.Restored,
                storeTransactionId = "GPA.1",
                originalTransactionDate = Date(1_000L),
                webOrderLineItemID = null,
                appBundleId = null,
                subscriptionGroupId = "base",
                isUpgraded = null,
                expirationDate = null,
                offerId = null,
                revocationDate = null,
                appAccountToken = null,
                purchaseToken = "token-1",
                payment =
                    com.superwall.sdk.store.abstractions.transactions.StorePayment(
                        productIdentifier = "annual",
                        quantity = 1,
                        discountIdentifier = null,
                    ),
                signature = "sig",
            )

        val transaction = native.toKmp()

        assertEquals(com.superwall.sdk.kmp.models.store.StoreTransactionState.RESTORED, transaction.state)
        assertEquals("token-1", transaction.purchaseToken)
        assertEquals("sig", transaction.signature)
        assertEquals(com.superwall.sdk.kmp.models.store.StorePayment("annual", 1, null), transaction.payment)
        assertEquals(null, transaction.appAccountToken)
    }

    @Test
    fun everyNativeStateMaps() {
        val states =
            listOf(
                com.superwall.sdk.store.abstractions.transactions.StoreTransactionState.Purchasing,
                com.superwall.sdk.store.abstractions.transactions.StoreTransactionState.Purchased,
                com.superwall.sdk.store.abstractions.transactions.StoreTransactionState.Failed,
                com.superwall.sdk.store.abstractions.transactions.StoreTransactionState.Restored,
                com.superwall.sdk.store.abstractions.transactions.StoreTransactionState.Deferred,
            )
        assertEquals(
            com.superwall.sdk.kmp.models.store.StoreTransactionState.entries.toList(),
            states.map { it.toKmp() },
        )
    }
}
