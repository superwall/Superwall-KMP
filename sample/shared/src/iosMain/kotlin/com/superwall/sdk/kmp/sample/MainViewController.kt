package com.superwall.sdk.kmp.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * The iOS entry point: the Compose [App] wrapped in a `UIViewController`.
 *
 * Called from Swift as `MainViewControllerKt.MainViewController()` (see
 * sample/iosApp/SampleApp/SampleApp.swift).
 */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
