package com.superwall.sdk.kmp.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus

/**
 * Port of the Flutter test_app's `PurchaseControllerTest` ("Mock PC Test")
 * screen: configure with or without the shared [testingPurchaseController],
 * then trigger the paywall and toggle the mock controller's purchase/restore
 * outcomes.
 */
@Composable
fun PurchaseControllerTestScreen(onBack: () -> Unit) {
    var isConfigured by remember { mutableStateOf(Superwall.isConfigured) }
    var dialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    val pc = testingPurchaseController

    val onConfigured: (Result<Unit>) -> Unit = { result ->
        result.fold(
            onSuccess = {
                Superwall.subscriptionStatus = SubscriptionStatus.Inactive
                isConfigured = true
            },
            onFailure = { dialog = "Error" to "Configuration failed: ${it.message}" },
        )
    }

    SampleScreen(title = "Mock PC Test", onBack = onBack) {
        SampleButton("Configure with PC") {
            Superwall.configure(
                apiKey = superwallApiKey,
                purchaseController = pc,
                options = sampleOptions(),
                completion = onConfigured,
            )
        }
        SampleButton("Configure without PC") {
            Superwall.configure(
                apiKey = superwallApiKey,
                options = sampleOptions(),
                completion = onConfigured,
            )
        }
        if (isConfigured) {
            SampleButton("Trigger Paywall") {
                Superwall.register(placement = "campaign_trigger") {
                    println("feature triggered")
                    dialog = "Feature" to "Feature triggered"
                }
            }
            SampleButton(if (pc.rejectPurchase) "Enable purchases" else "Disable purchases") {
                pc.rejectPurchase = !pc.rejectPurchase
            }
            SampleButton(if (pc.restorePurchase) "Disable restore" else "Enable restore") {
                pc.restorePurchase = !pc.restorePurchase
            }
            SampleButton("Reset status") {
                Superwall.subscriptionStatus = SubscriptionStatus.Inactive
            }
        }
    }

    dialog?.let { (title, message) ->
        InfoDialog(title = title, message = message, onDismiss = { dialog = null })
    }
}
