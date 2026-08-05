package com.superwall.sdk.kmp.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The screens of the test app, mirroring the routes of the Flutter SDK's
 * `test_app` (`test_app/lib/main.dart` in the Superwall-Flutter repo).
 */
enum class Screen {
    Home,
    ConfigureTest,
    SubscriptionStatusTest,
    PurchaseControllerTest,
    DelegateTest,
    HandlerTest,
}

/**
 * KMP port of the Flutter test_app: a Home screen navigating to one test
 * screen per SDK area — configuration, subscription status, purchase
 * controller, delegate, and presentation handler.
 *
 * Every configure call in this app disables paywall preloading
 * (`PaywallOptions.shouldPreload = false`), matching the Flutter test_app.
 */
@Composable
fun App() {
    var screen by remember { mutableStateOf(Screen.Home) }
    val goHome = { screen = Screen.Home }

    MaterialTheme {
        when (screen) {
            Screen.Home -> HomeScreen(onNavigate = { screen = it })
            Screen.ConfigureTest -> ConfigureTestScreen(onBack = goHome)
            Screen.SubscriptionStatusTest -> SubscriptionStatusTestScreen(onBack = goHome)
            Screen.PurchaseControllerTest -> PurchaseControllerTestScreen(onBack = goHome)
            Screen.DelegateTest -> DelegateTestScreen(onBack = goHome)
            Screen.HandlerTest -> HandlerTestScreen(onBack = goHome)
        }
    }
}

@Composable
private fun HomeScreen(onNavigate: (Screen) -> Unit) {
    SampleScreen(title = "Home", onBack = null) {
        SampleButton("Configuration test") { onNavigate(Screen.ConfigureTest) }
        SampleButton("Subscription Status Test") { onNavigate(Screen.SubscriptionStatusTest) }
        SampleButton("Purchase Controller Test") { onNavigate(Screen.PurchaseControllerTest) }
        SampleButton("Delegate Test") { onNavigate(Screen.DelegateTest) }
        SampleButton("Handler Test") { onNavigate(Screen.HandlerTest) }
    }
}

// ---------------------------------------------------------------------------
// Shared UI building blocks
// ---------------------------------------------------------------------------

/**
 * Scaffold shared by every screen: top bar with title and (except on Home) a
 * Back action, an optional snackbar host, and a scrollable centered column of
 * content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScreen(
    title: String,
    onBack: (() -> Unit)?,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("Back") }
                    }
                },
            )
        },
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Composable
fun SampleButton(label: String, onClick: () -> Unit) {
    Button(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(label)
    }
}

/** The test_app's AlertDialog: a title, a message, and an OK button. */
@Composable
fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

/** The test_app's event-list dialog: "Event N: <name>" rows in a 300dp list. */
@Composable
fun EventListDialog(title: String, eventNames: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                itemsIndexed(eventNames) { index, name ->
                    Text(
                        text = "Event ${index + 1}: $name",
                        modifier = Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}
