package com.superwall.sdk.kmp.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * Renders [App] into the page body.
 *
 * Lives here (rather than in :sample:webApp) for the same reason as Android's
 * setSampleAppContent(): all `@Composable` code and the Compose compiler stay
 * in this module; the host only calls this.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun startSampleApp() {
    ComposeViewport(document.body!!) { App() }
}
