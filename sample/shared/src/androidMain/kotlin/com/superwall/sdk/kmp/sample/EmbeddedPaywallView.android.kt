package com.superwall.sdk.kmp.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.superwall.sdk.kmp.EmbeddedPaywall
import com.superwall.sdk.kmp.view

@Composable
actual fun EmbeddedPaywallView(paywall: EmbeddedPaywall, modifier: Modifier) {
    // Keyed so a newly retrieved paywall gets a new AndroidView: `factory` runs
    // once per composition slot and would otherwise keep showing the old view.
    key(paywall) {
        AndroidView(factory = { paywall.view }, modifier = modifier)
    }
}
