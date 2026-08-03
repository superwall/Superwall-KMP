package com.superwall.sdk.kmp.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superwall.sdk.kmp.Superwall
import kotlinx.coroutines.launch

/**
 * Port of the Flutter test_app's `HandlerTest` screen: registers gated,
 * non-gated, skip-audience, and error placements with the shared
 * [testHandler], plus a results box showing whether the feature block ran and
 * how many handler events were recorded.
 */
@Composable
fun HandlerTestScreen(onBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var featureBlockExecuted by remember { mutableStateOf(false) }
    var showEventsDialog by remember { mutableStateOf(false) }
    var errorDialog by remember { mutableStateOf<String?>(null) }

    fun snackbar(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    // The feature block executed for non-gated placements (immediately) and
    // gated ones (after purchase/restore).
    val featureBlock = {
        featureBlockExecuted = true
        snackbar("Feature block executed!")
    }

    fun registerPlacement(placement: String) {
        runCatching {
            Superwall.register(
                placement = placement,
                handler = testHandler.handler,
                feature = featureBlock,
            )
        }.onFailure { errorDialog = it.message ?: it.toString() }
    }

    SampleScreen(title = "Handler Test", onBack = onBack, snackbarHostState = snackbarHostState) {
        SampleButton("Test Non-Gated Paywall") { registerPlacement("non_gated_paywall") }
        SampleButton("Test Gated Paywall") { registerPlacement("gated_paywall") }
        SampleButton("Test Skip Audience") { registerPlacement("skip_audience") }
        SampleButton("Test Error Placement") { registerPlacement("error_placement") }
        SampleButton("Dismiss Paywall") {
            scope.launch {
                runCatching { Superwall.dismiss() }
                    .onFailure { errorDialog = it.message ?: it.toString() }
            }
        }
        SampleButton("Clear Handler Events") {
            testHandler.events.clear()
            featureBlockExecuted = false
            snackbar("Handler events cleared")
        }
        SampleButton("Show Handler Events") { showEventsDialog = true }

        Column(
            modifier = Modifier
                .padding(top = 20.dp)
                .border(BorderStroke(1.dp, Color.Gray), RoundedCornerShape(5.dp))
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Test Results", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "Feature Block Executed: ${if (featureBlockExecuted) "Yes" else "No"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Event Count: ${testHandler.events.size}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    if (showEventsDialog) {
        EventListDialog(
            title = "Handler Events",
            eventNames = testHandler.events.map { it.displayName() },
            onDismiss = { showEventsDialog = false },
        )
    }
    errorDialog?.let { message ->
        InfoDialog(title = "Error", message = message, onDismiss = { errorDialog = null })
    }
}

private fun HandlerEvent.displayName(): String = when (this) {
    is HandlerEvent.Present -> "OnPresent"
    is HandlerEvent.Dismiss -> "OnDismiss"
    is HandlerEvent.Error -> "OnError"
    is HandlerEvent.Skip -> "OnSkip"
}
