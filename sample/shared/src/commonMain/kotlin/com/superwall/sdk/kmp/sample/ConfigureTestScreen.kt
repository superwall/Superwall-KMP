package com.superwall.sdk.kmp.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.models.options.PaywallOptions
import com.superwall.sdk.kmp.models.options.SuperwallOptions

/**
 * The options every configure call in this app uses: paywall preloading OFF,
 * matching the Flutter test_app's `options.paywalls.shouldPreload = false`.
 */
fun sampleOptions(): SuperwallOptions =
    SuperwallOptions(paywalls = PaywallOptions(shouldPreload = false))

/**
 * Port of the Flutter test_app's `ConfigureTest` screen.
 *
 * The Flutter screen's third button configures with a RevenueCat
 * `RCPurchaseController`; it has no port here because this sample carries no
 * RevenueCat dependency.
 *
 * Note the KMP SDK configures once per process: repeat configure calls are
 * no-ops whose completion is invoked with the first call's outcome, so
 * tapping a second button still shows the dialog but does not re-install
 * options or the purchase controller.
 */
@Composable
fun ConfigureTestScreen(onBack: () -> Unit) {
    var dialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val showOutcomeDialog: (Result<Unit>) -> Unit = { result ->
        dialog = result.fold(
            onSuccess = { "Success" to "Configuration completed" },
            onFailure = { "Error" to "Configuration failed: ${it.message}" },
        )
    }

    SampleScreen(title = "ConfigureTest", onBack = onBack) {
        SampleButton("Configure with dialog shown + PC") {
            Superwall.configure(
                apiKey = superwallApiKey,
                purchaseController = testingPurchaseController,
                options = sampleOptions(),
                completion = showOutcomeDialog,
            )
        }
        SampleButton("Configure with dialog shown + no PC") {
            Superwall.configure(
                apiKey = superwallApiKey,
                options = sampleOptions(),
                completion = showOutcomeDialog,
            )
        }
        SampleButton("Just configure") {
            Superwall.configure(apiKey = superwallApiKey, options = sampleOptions())
        }
    }

    dialog?.let { (title, message) ->
        InfoDialog(title = title, message = message, onDismiss = { dialog = null })
    }
}
