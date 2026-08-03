package com.superwall.sdk.kmp.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.superwall.sdk.misc.ActivityProvider as NativeActivityProvider
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tracks the current foreground [Activity] via
 * [Application.ActivityLifecycleCallbacks], holding it only in a
 * [WeakReference] so the tracker never leaks a destroyed Activity.
 *
 * Implements the native SDK's [ActivityProvider][NativeActivityProvider] so
 * the bridge can hand it straight to native
 * `Superwall.configure(..., activityProvider = CurrentActivityTracker, ...)` —
 * replacing the Flutter plugin's `ActivityAware` lifecycle plumbing.
 *
 * Tracking follows the native SDK's own `CurrentActivityTracker` semantics
 * (the porting spec): the reference is set on started AND resumed (so an
 * Activity mid-transition still counts), and cleared on destroyed only when
 * the destroyed Activity is the tracked one.
 *
 * Registered by [SuperwallInitializer] (androidx.startup) or by
 * `Superwall.androidSetup`; [registerWith] is idempotent so both may run.
 */
internal object CurrentActivityTracker :
    Application.ActivityLifecycleCallbacks,
    NativeActivityProvider {
    private val registered = AtomicBoolean(false)

    @Volatile
    private var currentActivity: WeakReference<Activity>? = null

    /**
     * Registers this tracker with [application]'s lifecycle callbacks.
     * Idempotent — the startup initializer and the `androidSetup` escape
     * hatch may both call it; only the first registration takes effect.
     */
    fun registerWith(application: Application) {
        if (registered.compareAndSet(false, true)) {
            application.registerActivityLifecycleCallbacks(this)
        }
    }

    override fun getCurrentActivity(): Activity? = currentActivity?.get()

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        // No-op: only started/resumed activities are candidates.
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        // No-op: keep the reference through pause so paywalls presented
        // during transitions still resolve an Activity (native SDK parity).
    }

    override fun onActivityStopped(activity: Activity) {
        // No-op: cleared on destroy; a stopped-but-alive Activity is still a
        // better presenter than none.
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {
        // No-op.
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity?.get() === activity) {
            currentActivity = null
        }
    }
}
