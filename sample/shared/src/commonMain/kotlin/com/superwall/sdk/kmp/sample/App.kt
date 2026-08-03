package com.superwall.sdk.kmp.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus

/**
 * The Superwall public API key this demo configures with.
 *
 * This is the PUBLIC Android test-app key (`com.superwall.superapp`) shared
 * with the Flutter SDK's `test_app` (see `test_app/lib/ConfigureTest.dart` in
 * the Superwall-Flutter repo) — it points at a Superwall test account and is
 * safe to commit. To run the demo against your own dashboard, replace this
 * value with the Public API Key from your Superwall dashboard settings
 * (Settings → Keys), and use a campaign with a `campaign_trigger` placement
 * (or change the placement name in [App]).
 */
const val SUPERWALL_API_KEY: String = "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257"

/**
 * The whole demo: Configure / Identify / Register buttons, a subscription
 * status line bound to [Superwall.subscriptionStatusFlow], and a log line
 * showing the last configure [Result] plus the last action's outcome.
 *
 * Identify and Register deliberately work (and fail with
 * `SuperwallError.NotConfigured`) before Configure is tapped — the KMP SDK has
 * no pre-configure call queue, and the guard is part of what the demo shows.
 */
@Composable
fun App() {
    // Guard-exempt: collectable before configure, seeded with Unknown;
    // emissions arrive on the main thread.
    val subscriptionStatus by Superwall.subscriptionStatusFlow.collectAsState()
    var lastConfigureResult by remember { mutableStateOf<Result<Unit>?>(null) }
    var log by remember { mutableStateOf("Tap Configure to start.") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Superwall KMP Sample",
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = "Subscription status: ${subscriptionStatus.label()}",
                style = MaterialTheme.typography.bodyLarge,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log = "configure: configuring…"
                    Superwall.configure(apiKey = SUPERWALL_API_KEY) { result ->
                        lastConfigureResult = result
                        log = result.fold(
                            onSuccess = { "configure: success" },
                            onFailure = { "configure: failed — ${it.message}" },
                        )
                    }
                },
            ) {
                Text("Configure")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log = runCatching { Superwall.identify(userId = "sample_user") }.fold(
                        onSuccess = { "identify: identified as \"sample_user\"" },
                        onFailure = { "identify: ${it.message}" },
                    )
                },
            ) {
                Text("Identify (\"sample_user\")")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log = runCatching {
                        Superwall.register(placement = "campaign_trigger") {
                            // The gated feature: runs immediately when no paywall
                            // shows, or after purchase/restore when gated.
                            log = "register: feature launched for \"campaign_trigger\""
                        }
                    }.fold(
                        onSuccess = { "register: registered \"campaign_trigger\"" },
                        onFailure = { "register: ${it.message}" },
                    )
                },
            ) {
                Text("Register (\"campaign_trigger\")")
            }

            Text(
                text = "Last configure result: ${lastConfigureResult.label()}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = log,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun SubscriptionStatus.label(): String = when (this) {
    is SubscriptionStatus.Active ->
        "Active (${entitlements.size} entitlement(s): ${entitlements.joinToString { it.id }})"
    is SubscriptionStatus.Inactive -> "Inactive"
    is SubscriptionStatus.Unknown -> "Unknown"
}

private fun Result<Unit>?.label(): String = when {
    this == null -> "—"
    isSuccess -> "success"
    else -> "failure: ${exceptionOrNull()?.message}"
}
