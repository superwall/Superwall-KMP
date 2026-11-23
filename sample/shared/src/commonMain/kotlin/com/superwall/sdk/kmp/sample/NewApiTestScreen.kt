package com.superwall.sdk.kmp.sample

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.superwall.sdk.kmp.EmbeddedPaywall
import com.superwall.sdk.kmp.PaywallViewCallback
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.models.options.InterfaceStyle
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import kotlinx.coroutines.launch

/**
 * Puts an [EmbeddedPaywall] on screen. The paywall itself is common; the thing
 * to show is a platform object (`paywall.view` / `paywall.viewController`), so
 * the host composable is the one piece that has to be platform code.
 */
@Composable
expect fun EmbeddedPaywallView(paywall: EmbeddedPaywall, modifier: Modifier)

/**
 * Exercises the API that has no counterpart in the Flutter test_app screens:
 * embedded paywalls, the Customer Center, the configuration-status and events
 * flows, assignments, configuration refresh and the interface style.
 *
 * Every result is written to a plain [Text] rather than a dialog, so a Maestro
 * flow can assert on it while an embedded paywall is still on screen.
 */
@Composable
fun NewApiTestScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val configurationStatus by Superwall.configurationStatusFlow.collectAsState()
    var eventCount by remember { mutableIntStateOf(0) }
    var embedded by remember { mutableStateOf<EmbeddedPaywall?>(null) }
    var embeddedStatus by remember { mutableStateOf("none") }
    var lastResult by remember { mutableStateOf("-") }

    LaunchedEffect(Unit) { Superwall.eventsFlow.collect { eventCount++ } }

    fun destroyEmbedded() {
        embedded?.destroy()
        embedded = null
    }
    // Leaving the screen has to end the presentation too: on Android taking the
    // view out of the layout is not enough (see EmbeddedPaywall.destroy).
    DisposableEffect(Unit) { onDispose { embedded?.destroy() } }

    fun attempt(label: String, block: suspend () -> String) {
        scope.launch {
            lastResult = runCatching { block() }.fold({ "$label: $it" }, { "$label failed: ${it.message ?: it}" })
        }
    }

    val callback = remember {
        object : PaywallViewCallback {
            override fun onFinished(paywallInfo: PaywallInfo, result: PaywallResult, shouldDismiss: Boolean) {
                embeddedStatus = "finished ${result::class.simpleName} shouldDismiss=$shouldDismiss"
                if (shouldDismiss) destroyEmbedded()
            }

            override fun onPresent(paywallInfo: PaywallInfo) {
                embeddedStatus = "presented"
            }

            override fun onSkip(reason: PaywallSkippedReason) {
                embeddedStatus = "skipped ${reason::class.simpleName}"
            }

            override fun onError(error: String) {
                embeddedStatus = "error $error"
            }
        }
    }

    SampleScreen(title = "New API Test", onBack = onBack) {
        Text("Configuration status: $configurationStatus")
        Text("Events seen: ${if (eventCount > 0) "some" else "none"} ($eventCount)")
        Text("Embedded paywall: $embeddedStatus")
        Text("Last result: $lastResult")

        SampleButton("Get Embedded Paywall") {
            attempt("getPaywall") {
                destroyEmbedded()
                val paywall = Superwall.getPaywall(placement = "non_gated_paywall", callback = callback)
                embedded = paywall
                if (paywall != null) {
                    embeddedStatus = "retrieved"
                    "returned ${paywall.info.name} for ${paywall.placement}"
                } else {
                    "returned null"
                }
            }
        }
        embedded?.let { paywall ->
            EmbeddedPaywallView(paywall, Modifier.fillMaxWidth().height(420.dp))
            SampleButton("Destroy Embedded Paywall") {
                destroyEmbedded()
                embeddedStatus = "destroyed"
            }
        }
        SampleButton("Present Customer Center") {
            attempt("presentCustomerCenter") {
                Superwall.presentCustomerCenter(onDismiss = { lastResult = "Customer Center dismissed" })
                "requested"
            }
        }
        SampleButton("Get Assignments") {
            attempt("getAssignments") { "ok, ${Superwall.getAssignments().size} assignments" }
        }
        SampleButton("Refresh Configuration") {
            attempt("refreshConfiguration") {
                Superwall.refreshConfiguration()
                "requested"
            }
        }
        SampleButton("Set Dark Interface Style") {
            attempt("setInterfaceStyle") {
                Superwall.setInterfaceStyle(InterfaceStyle.DARK)
                "dark"
            }
        }
    }
}
