package com.superwall.sdk.kmp.sample.minimal

import com.superwall.sdk.kmp.PaywallPresentationHandler
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.SuperwallDelegate
import com.superwall.sdk.kmp.models.events.SuperwallEventInfo
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement

// The public key of Superwall-Web's own example apps: a web platform-app
// whose campaign has the `campaign_trigger` placement. Replace it with your
// dashboard's web app key (Settings → Keys).
private const val API_KEY = "pk_ZNLGF8AlO2V50YDvC1y0c"

private val scope = MainScope()

private fun button(id: String) = document.getElementById(id) as HTMLButtonElement

private fun log(message: String) {
    val log = document.getElementById("log")!!
    log.textContent += "$message\n"
    log.scrollTop = log.scrollHeight.toDouble()
}

fun main() {
    // Guard-exempt before configure: the delegate is stored and installed at
    // configure, and the flow is pre-seeded with Unknown.
    Superwall.delegate =
        object : SuperwallDelegate {
            override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
                log("event ${eventInfo.eventType}" + (eventInfo.name?.let { " ($it)" } ?: ""))
            }
        }
    scope.launch {
        Superwall.subscriptionStatusFlow.collect {
            document.getElementById("status")!!.textContent = it.toString()
        }
    }

    button("configure").onclick = {
        button("configure").disabled = true
        Superwall.configure(apiKey = API_KEY) { result ->
            result
                .onSuccess {
                    log("configured — user ${Superwall.userId}")
                    listOf("register", "restore", "reset", "identify").forEach { id -> button(id).disabled = false }
                }.onFailure {
                    log("configure failed: ${it.message}")
                    button("configure").disabled = false
                }
        }
    }

    button("register").onclick = {
        Superwall.register(
            placement = "campaign_trigger",
            handler =
                PaywallPresentationHandler().apply {
                    onPresent { log("presented ${it.identifier}") }
                    onDismiss { info, result -> log("dismissed ${info.identifier}: $result") }
                    onSkip { log("skipped: $it") }
                    onError { log("error: $it") }
                },
        ) {
            log("feature unlocked")
        }
    }

    button("restore").onclick = {
        scope.launch { log("restore: ${Superwall.restorePurchases()}") }
    }

    button("identify").onclick = {
        val userId = (document.getElementById("userId") as HTMLInputElement).value.trim()
        if (userId.isNotEmpty()) {
            Superwall.identify(userId)
            log("identified as $userId")
        }
    }

    button("reset").onclick = {
        Superwall.reset()
        log("reset — user ${Superwall.userId}")
    }
}
