package com.superwall.sdk.kmp.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.superwall.sdk.kmp.PurchaseController
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult

/**
 * Port of the Flutter test_app's `TestingPurchaseController`: a mock
 * [PurchaseController] whose purchase/restore outcomes are toggled from the
 * Purchase Controller Test screen.
 *
 * The flags are Compose state so the toggle buttons relabel on change.
 */
class TestingPurchaseController : PurchaseController {
    /** When `true` (the default), purchases fail instead of succeeding. */
    var rejectPurchase by mutableStateOf(true)

    /** When `true` (the default), restores succeed and activate `test_entitlement`. */
    var restorePurchase by mutableStateOf(true)

    override suspend fun purchaseFromAppStore(productId: String): PurchaseResult =
        if (rejectPurchase) {
            PurchaseResult.failed("Purchase was rejected in TestingPurchaseController")
        } else {
            PurchaseResult.purchased()
        }

    override suspend fun purchaseFromGooglePlay(
        productId: String,
        basePlanId: String?,
        offerId: String?,
    ): PurchaseResult =
        if (rejectPurchase) {
            PurchaseResult.failed("Purchase was rejected in TestingPurchaseController")
        } else {
            Superwall.subscriptionStatus = SubscriptionStatus.Active(
                setOf(Entitlement(id = "test_entitlement")),
            )
            PurchaseResult.purchased()
        }

    override suspend fun restorePurchases(): RestorationResult =
        if (restorePurchase) {
            Superwall.subscriptionStatus = SubscriptionStatus.Active(
                setOf(Entitlement(id = "test_entitlement")),
            )
            RestorationResult.restored()
        } else {
            RestorationResult.failed("Restore failed in TestingPurchaseController")
        }
}

/**
 * The single controller instance shared by every screen. Superwall.configure
 * installs a purchase controller exactly once per process (repeat configure
 * calls are no-ops), so the instance whose toggles the UI flips must be the
 * same one that was installed — a per-screen instance would silently stop
 * driving purchases after leaving and re-entering the screen.
 */
val testingPurchaseController: TestingPurchaseController = TestingPurchaseController()
