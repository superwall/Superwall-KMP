package com.superwall.sdk.kmp.sample

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import kotlinx.coroutines.launch

/**
 * Port of the Flutter test_app's `DelegateTest` screen: installs the shared
 * [testDelegate], drives paywall/subscription activity, and inspects the
 * recorded delegate events (with and without the high-volume log/analytics
 * callbacks).
 */
@Composable
fun DelegateTestScreen(onBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialogEvents by remember { mutableStateOf<List<TestDelegateEvent>?>(null) }
    var errorDialog by remember { mutableStateOf<String?>(null) }

    fun snackbar(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    // SDK calls on this screen require a configured SDK; surface the
    // NotConfigured guard in a dialog instead of crashing.
    fun guarded(block: () -> Unit) {
        runCatching(block).onFailure { errorDialog = it.message ?: it.toString() }
    }

    SampleScreen(title = "Delegate Test", onBack = onBack, snackbarHostState = snackbarHostState) {
        SampleButton("Set Test Delegate") {
            Superwall.delegate = testDelegate
            snackbar("Test delegate set")
        }
        SampleButton("Show Paywall") {
            guarded { Superwall.register(placement = "campaign_trigger") }
        }
        SampleButton("Change Subscription Status") {
            guarded {
                Superwall.subscriptionStatus = SubscriptionStatus.Active(
                    setOf(
                        Entitlement(id = "pro"),
                        Entitlement(id = "test_entitlement"),
                    ),
                )
            }
        }
        SampleButton("Clear Delegate and Change Status") {
            testDelegate.events.clear()
            Superwall.delegate = null
            guarded { Superwall.subscriptionStatus = SubscriptionStatus.Inactive }
            snackbar("Delegate cleared")
        }
        SampleButton("Clear Delegate Events") {
            testDelegate.events.clear()
            snackbar("Delegate events cleared")
        }
        SampleButton("Show Delegate Events without log") {
            dialogEvents = testDelegate.eventsWithoutLog
        }
        SampleButton("Show Delegate Events with log") {
            dialogEvents = testDelegate.events.filterIsInstance<TestDelegateEvent.HandleLog>()
        }
        SampleButton("Events without log and presentation") {
            dialogEvents = testDelegate.eventsWithoutLog.filter {
                it !is TestDelegateEvent.WillPresentPaywall &&
                    it !is TestDelegateEvent.DidPresentPaywall &&
                    it !is TestDelegateEvent.WillDismissPaywall &&
                    it !is TestDelegateEvent.DidDismissPaywall &&
                    it !is TestDelegateEvent.HandleSuperwallEvent
            }
        }
    }

    dialogEvents?.let { events ->
        EventListDialog(
            title = "Delegate Events",
            eventNames = events.map { it.displayName() },
            onDismiss = { dialogEvents = null },
        )
    }
    errorDialog?.let { message ->
        InfoDialog(title = "Error", message = message, onDismiss = { errorDialog = null })
    }
}

private fun TestDelegateEvent.displayName(): String = when (this) {
    is TestDelegateEvent.DidDismissPaywall -> "DidDismissPaywall"
    is TestDelegateEvent.DidPresentPaywall -> "DidPresentPaywall"
    is TestDelegateEvent.HandleCustomPaywallAction -> "HandleCustomPaywallAction"
    is TestDelegateEvent.HandleLog -> "HandleLog"
    is TestDelegateEvent.HandleSuperwallEvent -> "HandleSuperwallEvent"
    is TestDelegateEvent.PaywallWillOpenDeepLink -> "PaywallWillOpenDeepLink"
    is TestDelegateEvent.PaywallWillOpenURL -> "PaywallWillOpenURL"
    is TestDelegateEvent.SubscriptionStatusDidChange -> "SubscriptionStatusDidChange"
    is TestDelegateEvent.WillDismissPaywall -> "WillDismissPaywall"
    is TestDelegateEvent.WillPresentPaywall -> "WillPresentPaywall"
    is TestDelegateEvent.HandleSuperwallDeepLink -> "HandleSuperwallDeepLink"
}
