package com.superwall.sdk.kmp

import android.app.Application
import com.superwall.sdk.kmp.internal.ApplicationContextHolder
import com.superwall.sdk.kmp.internal.CurrentActivityTracker

/**
 * Manually initializes Superwall's Android context capture — the escape hatch
 * for apps that have removed or disabled the SDK's androidx.startup
 * initializer (e.g. via `tools:node="remove"` on the
 * `InitializationProvider`).
 *
 * Normally you never call this: the SDK's `SuperwallInitializer` runs
 * automatically at app start via androidx.startup, captures the
 * [Application], and starts tracking the current Activity so
 * [Superwall.configure] needs no `Context` parameter. If that initializer
 * cannot run, call this from `Application.onCreate()` **before**
 * [Superwall.configure]; otherwise `configure` fails with
 * [SuperwallError.NotInitialized].
 *
 * Idempotent and safe to call defensively even when the startup initializer
 * did run — repeat calls are no-ops.
 *
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Superwall.androidSetup(this)
 *         Superwall.configure(apiKey = "pk_...")
 *     }
 * }
 * ```
 *
 * @param application Your app's [Application] instance.
 */
@Suppress("UnusedReceiverParameter") // Scoped to Superwall for discoverability.
public fun Superwall.androidSetup(application: Application) {
    ApplicationContextHolder.set(application)
    CurrentActivityTracker.registerWith(application)
}
