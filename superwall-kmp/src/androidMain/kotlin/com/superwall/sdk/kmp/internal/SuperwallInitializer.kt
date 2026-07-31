package com.superwall.sdk.kmp.internal

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

/**
 * androidx.startup initializer that captures the [Application] and registers
 * [CurrentActivityTracker] before any app code runs.
 *
 * This is what lets the common `Superwall.configure(apiKey, ...)` signature
 * omit a `Context` parameter on Android. Declared in the library manifest
 * under androidx.startup's `InitializationProvider`; discovered reflectively,
 * so it is kept by `consumer-rules.pro`.
 *
 * Apps that remove the startup provider must call
 * `Superwall.androidSetup(application)` from `Application.onCreate()` instead
 * — otherwise `configure()` fails with `SuperwallError.NotInitialized`.
 */
internal class SuperwallInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val application = context.applicationContext as Application
        ApplicationContextHolder.set(application)
        CurrentActivityTracker.registerWith(application)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
