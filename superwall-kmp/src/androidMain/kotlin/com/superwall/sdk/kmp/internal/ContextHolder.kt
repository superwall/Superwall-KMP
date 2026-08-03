package com.superwall.sdk.kmp.internal

import android.app.Application
import com.superwall.sdk.kmp.SuperwallError

/**
 * Holds the [Application] captured at app start.
 *
 * Normally set by [SuperwallInitializer] via androidx.startup before any app
 * code runs. Apps that strip or disable the startup `InitializationProvider`
 * (e.g. via `tools:node="remove"`) must call
 * `Superwall.androidSetup(application)` from `Application.onCreate()` instead.
 *
 * The bridge reads [application] at configure time; if neither path ran, the
 * getter fails immediately and actionably with
 * [SuperwallError.NotInitialized] rather than letting the native SDK crash
 * later with no `Application`/`ActivityProvider`.
 */
internal object ApplicationContextHolder {
    @Volatile
    private var stored: Application? = null

    /** Whether an [Application] has been captured. */
    val isSet: Boolean
        get() = stored != null

    /** The captured [Application], or `null` when neither setup path has run. */
    val applicationOrNull: Application?
        get() = stored

    /**
     * The captured [Application].
     *
     * @throws SuperwallError.NotInitialized when neither the androidx.startup
     * initializer nor `Superwall.androidSetup` has run.
     */
    val application: Application
        get() =
            stored ?: throw SuperwallError.NotInitialized(
                "Superwall's androidx.startup initializer did not run, so no Application " +
                    "context is available. This happens when androidx.startup's " +
                    "InitializationProvider is removed or disabled in your manifest. " +
                    "Call Superwall.androidSetup(application) from Application.onCreate() " +
                    "before Superwall.configure, or re-enable the " +
                    "androidx.startup provider entry for " +
                    "com.superwall.sdk.kmp.internal.SuperwallInitializer.",
            )

    /**
     * Stores [application]. Idempotent — the first captured instance wins;
     * repeat calls (startup initializer plus a defensive `androidSetup`) are
     * no-ops.
     */
    fun set(application: Application) {
        if (stored == null) {
            stored = application
        }
    }
}
