package com.superwall.sdk.kmp.sample

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Hosts [App] in the given activity.
 *
 * Lives here (rather than in :sample:androidApp) so the app module contains no
 * `@Composable` code at all: :sample:androidApp builds with AGP 9's built-in
 * Kotlin support, which doesn't take the `org.jetbrains.kotlin.plugin.compose`
 * compiler plugin — all Compose compilation stays in this module.
 */
fun ComponentActivity.setSampleAppContent() {
    setContent { App() }
}
