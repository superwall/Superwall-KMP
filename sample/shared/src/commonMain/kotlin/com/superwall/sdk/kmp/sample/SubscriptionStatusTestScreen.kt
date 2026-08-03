package com.superwall.sdk.kmp.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus

/**
 * Port of the Flutter test_app's `SubscriptionStatusTest` screen: sets the
 * subscription status to Active/Inactive/Unknown and shows the result in a
 * dialog.
 *
 * Setting the status requires a configured SDK ([Superwall.subscriptionStatus]
 * throws `SuperwallError.NotConfigured` otherwise — configure from the
 * ConfigureTest screen first); the failure lands in the same dialog.
 */
@Composable
fun SubscriptionStatusTestScreen(onBack: () -> Unit) {
    var dialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun setStatus(status: SubscriptionStatus) {
        dialog = runCatching { Superwall.subscriptionStatus = status }.fold(
            onSuccess = { "Success" to status.describe() },
            onFailure = { "Error" to "Failed to set subscription status: ${it.message}" },
        )
    }

    SampleScreen(title = "Subscription Status Test", onBack = onBack) {
        SampleButton("Set Subscription Status Active") {
            setStatus(
                SubscriptionStatus.Active(
                    setOf(
                        Entitlement(id = "pro"),
                        Entitlement(id = "test_entitlement"),
                    ),
                ),
            )
        }
        SampleButton("Set Subscription Status Inactive") {
            setStatus(SubscriptionStatus.Inactive)
        }
        SampleButton("Set Subscription Status Unknown") {
            setStatus(SubscriptionStatus.Unknown)
        }
    }

    dialog?.let { (title, message) ->
        InfoDialog(title = title, message = message, onDismiss = { dialog = null })
    }
}

private fun SubscriptionStatus.describe(): String = when (this) {
    is SubscriptionStatus.Active ->
        "Subscription status: Active - Entitlements: ${entitlements.joinToString(", ") { it.id }}"
    is SubscriptionStatus.Inactive -> "Subscription status: Inactive"
    is SubscriptionStatus.Unknown -> "Subscription status: Unknown"
}
