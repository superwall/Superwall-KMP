package com.superwall.sdk.kmp.sample

import androidx.compose.runtime.mutableStateListOf
import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason

/**
 * Port of the Flutter test_app's `HandlerEvent` sealed class: one variant per
 * [PaywallPresentationHandler] closure.
 */
sealed interface HandlerEvent {
    data class Present(val paywallInfo: PaywallInfo) : HandlerEvent
    data class Dismiss(val paywallInfo: PaywallInfo, val result: PaywallResult) : HandlerEvent
    data class Error(val message: String) : HandlerEvent
    data class Skip(val reason: PaywallSkippedReason) : HandlerEvent
}

/**
 * Port of the Flutter test_app's `TestHandler`: wires every
 * [PaywallPresentationHandler] closure to record into [events] for the
 * Handler Test screen (all closures are delivered on the main thread).
 */
class TestHandler {
    val handler = PaywallPresentationHandler()
    val events = mutableStateListOf<HandlerEvent>()

    init {
        handler.onPresent { info ->
            println("onPresent: $info")
            events += HandlerEvent.Present(info)
        }
        handler.onDismiss { info, result ->
            println("onDismiss: $info, $result")
            events += HandlerEvent.Dismiss(info, result)
        }
        handler.onError { error ->
            println("onError: $error")
            events += HandlerEvent.Error(error)
        }
        handler.onSkip { reason ->
            println("onSkip: $reason")
            events += HandlerEvent.Skip(reason)
        }
    }
}

/**
 * Shared instance so recorded events survive navigating away from and back to
 * the Handler Test screen.
 */
val testHandler: TestHandler = TestHandler()
