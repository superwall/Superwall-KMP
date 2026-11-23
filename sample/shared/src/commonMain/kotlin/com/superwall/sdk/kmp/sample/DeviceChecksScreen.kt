package com.superwall.sdk.kmp.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.superwall.sdk.kmp.PaywallViewCallback
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.SuperwallDelegate
import com.superwall.sdk.kmp.models.options.AdConsent
import com.superwall.sdk.kmp.models.options.AdConsentStatus
import com.superwall.sdk.kmp.models.options.LogLevel
import com.superwall.sdk.kmp.models.options.LogScope
import com.superwall.sdk.kmp.models.options.PaywallResource
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PaywallResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sends one synthetic game controller input through the platform's forwarding
 * call (`dispatchKeyEvent` / `dispatchMotionEvent` on Android,
 * `gamepadValueChanged` on iOS) and says what it sent.
 */
expect fun sendSampleGamepadInput(): String

/** Reads the platform-only members that have no common form; "n/a" where there are none. */
expect fun readPlatformOnlyMembers(): String

/**
 * The options the device checks configure with: everything the default sample
 * options leave off, so the calls below have something to act on.
 */
private fun deviceCheckOptions(testMode: Boolean): SuperwallOptions =
    sampleOptions(testMode = testMode).copy(
        // Observer mode and test mode are separate runs: on iOS `purchase`
        // returns Cancelled while purchases are being observed.
        shouldObservePurchases = !testMode,
        isGameControllerEnabled = true,
        adConsent = AdConsent(adUserData = AdConsentStatus.DENIED, adPersonalization = AdConsentStatus.GRANTED),
        // No paywall on the test dashboards asks for this id, so this only
        // shows that a configure carrying local resources goes through.
        localResources = mapOf("sample-resource" to PaywallResource.FromUri("file:///sample-resource.png")),
    )

/**
 * One button per API that needs a device to say anything: ad consent, the
 * notification calls, `getProducts`, `purchase`, observer mode, `showAlert`,
 * `teardown` and game controller forwarding. Results go to plain [Text]s so a
 * Maestro flow can assert on them (sample/maestro/devicechecks/).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceChecksScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val configurationStatus by Superwall.configurationStatusFlow.collectAsState()
    var result by remember { mutableStateOf("-") }
    var productIds by remember { mutableStateOf(emptyList<String>()) }
    val events = remember { mutableStateListOf<String>() }
    val errorLogs = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        Superwall.eventsFlow.collect { events.add(it.eventType.name) }
    }
    // The observer calls report a product they can't find through handleLog
    // only, so the screen has to listen there to tell "tracked" from "dropped".
    DisposableEffect(Unit) {
        val previous = Superwall.delegate
        Superwall.delegate = object : SuperwallDelegate {
            override fun handleLog(
                level: LogLevel,
                scope: LogScope,
                message: String?,
                info: Map<String, Any?>?,
                error: String?,
            ) {
                if (level == LogLevel.ERROR && message != null) errorLogs.add(message)
            }
        }
        onDispose { Superwall.delegate = previous }
    }

    fun attempt(label: String, block: suspend () -> String) {
        scope.launch {
            result = "$label: running"
            result = runCatching { block() }.fold({ "$label: $it" }, { "$label failed: ${it.message ?: it}" })
        }
    }

    fun configure(testMode: Boolean) {
        result = "configure: running"
        Superwall.configure(apiKey = superwallApiKey, options = deviceCheckOptions(testMode)) { outcome ->
            result = outcome.fold({ "configure: done testMode=$testMode" }, { "configure failed: ${it.message}" })
        }
    }

    // Not SampleScreen: there are more buttons than fit, and the results have
    // to stay on screen while the buttons scroll for a flow to assert on them.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Checks") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
      Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
        Text("Configuration status: $configurationStatus")
        Text("Result: $result")
        Text("Product ids: ${productIds.joinToString().ifEmpty { "none" }}")
        // Capped so a long log line can't push the buttons off a small screen;
        // the accessibility text a flow asserts on is still the whole string.
        Text(
            "Recent events: ${events.takeLast(4).joinToString().ifEmpty { "none" }}",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "Error logs: ${errorLogs.size}${errorLogs.lastOrNull()?.let { " $it" }.orEmpty()}",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        SampleButton("Configure Observer Mode") { configure(testMode = false) }
        SampleButton("Configure Test Mode") { configure(testMode = true) }
        SampleButton("Check Ad Consent") {
            attempt("adConsent") {
                val fromOptions = Superwall.adConsent
                val updated = AdConsent(adUserData = AdConsentStatus.GRANTED, adPersonalization = AdConsentStatus.DENIED)
                Superwall.setAdConsent(updated)
                val readBack = Superwall.adConsent
                "options=${fromOptions?.adUserData}/${fromOptions?.adPersonalization} " +
                    "set=${readBack?.adUserData}/${readBack?.adPersonalization}"
            }
        }
        SampleButton("Call Notification APIs") {
            attempt("notifications") {
                Superwall.setPushToken("kmp-sample-token")
                Superwall.optOutOfNotifications()
                Superwall.optInToNotifications()
                Superwall.handleNotificationOpen(mapOf("source" to "kmp-sample"))
                Superwall.cancelAllScheduledNotifications()
                "ok"
            }
        }
        // The ids come from a real paywall rather than being hard-coded: the
        // two dashboards (one per platform) sell different products.
        SampleButton("Load Product Ids") {
            attempt("productIds") {
                val paywall = Superwall.getPaywall(
                    placement = "non_gated_paywall",
                    callback = object : PaywallViewCallback {
                        override fun onFinished(paywallInfo: PaywallInfo, result: PaywallResult, shouldDismiss: Boolean) {}
                    },
                )
                productIds = paywall?.info?.productIds.orEmpty().distinct()
                paywall?.destroy()
                "loaded ${productIds.size}"
            }
        }
        SampleButton("Get Products") {
            attempt("getProducts") {
                val products = Superwall.getProducts(productIds.toSet() + "kmp_sample_unknown_product")
                val first = products.values.firstOrNull()
                "got ${products.size} of ${productIds.size}, unknown left out=${"kmp_sample_unknown_product" !in products}, " +
                    "first=${first?.productIdentifier} ${first?.localizedPrice}"
            }
        }
        SampleButton("Purchase First Product") {
            attempt("purchase") { "${Superwall.purchase(productIds.first())}" }
        }
        SampleButton("Purchase Unknown Product") {
            attempt("purchaseUnknown") { "${Superwall.purchase("kmp_sample_unknown_product")}" }
        }
        SampleButton("Observe Purchase") {
            attempt("observe") {
                val id = productIds.first()
                val before = events.size
                val errorsBefore = errorLogs.size
                // Spaced like a real purchase. Back to back, superwall-android
                // handles the failure before it has recorded the start and
                // logs "Transactions in progress is empty".
                Superwall.observePurchaseStart(id)
                delay(2000)
                Superwall.observePurchaseError(id, "kmp sample error")
                delay(2000)
                // A second attempt, ended by the user: 1 is
                // BillingResponseCode.USER_CANCELED, with no purchases.
                Superwall.observePurchaseStart(id)
                delay(2000)
                Superwall.observePurchaseResult(billingResponseCode = 1, purchases = emptyList())
                delay(2000)
                // A product the SDK can't find is only reported as an error log.
                "tracked ${events.drop(before).filter { it.startsWith("TRANSACTION") }.joinToString()}, " +
                    "errors=${errorLogs.size - errorsBefore}"
            }
        }
        SampleButton("Paywall Then Alert") {
            attempt("showAlert") {
                Superwall.register(placement = "non_gated_paywall")
                // The alert goes over a presented paywall, so wait for it.
                var waited = 0
                while (!Superwall.isPaywallPresented && waited < 20) {
                    delay(500)
                    waited++
                }
                delay(3000)
                Superwall.showAlert(
                    title = "KMP alert",
                    message = "Shown over the paywall",
                    actionTitle = "KMP action",
                    closeActionTitle = "KMP close",
                    action = { result = "showAlert: action tapped" },
                    onClose = { result = "showAlert: closed" },
                )
                "requested, paywall presented=${Superwall.isPaywallPresented}"
            }
        }
        SampleButton("Send Gamepad Input") {
            attempt("gamepad") { sendSampleGamepadInput() }
        }
        SampleButton("Read Platform Members") {
            attempt("platform") { readPlatformOnlyMembers() }
        }
        SampleButton("Teardown And Reconfigure") {
            attempt("teardown") {
                if (!Superwall.teardown()) {
                    "returned false"
                } else {
                    val afterTeardown = Superwall.isConfigured
                    Superwall.configureAndAwait(apiKey = superwallApiKey, options = deviceCheckOptions(testMode = false))
                    "returned true, configured after teardown=$afterTeardown, after reconfigure=${Superwall.isConfigured}"
                }
            }
        }
        }
      }
    }
}
