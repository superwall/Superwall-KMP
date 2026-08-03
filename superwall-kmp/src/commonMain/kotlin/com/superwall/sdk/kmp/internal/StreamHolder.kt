package com.superwall.sdk.kmp.internal

import com.superwall.sdk.kmp.models.entitlements.CustomerInfo
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Owns the SDK's two public event streams and the internal bridge coroutine
 * scope, in common code, from module init.
 *
 * The flows exist — and are collectable — BEFORE `Superwall.configure` is
 * called (the facade's stream accessors are guard-exempt for this reason):
 * [subscriptionStatus] is seeded with [SubscriptionStatus.Unknown] and
 * [customerInfo] simply has nothing to emit yet. At configure-complete the
 * facade calls [attach], and the platform bridge lazily wires its native
 * sources into these flows (native stream collection plus an initial
 * synchronous read to close the seed gap). This is deliberately not a straight
 * port of the Flutter hosts, which only register their EventChannels during
 * configure.
 */
internal class StreamHolder {
    /**
     * The one internal `CoroutineScope` per bridge. Used to deliver SDK→app
     * callbacks on the main thread and to run app→SDK suspend callbacks
     * (`PurchaseController.*`, `onCustomCallback`); a failing child never
     * kills the scope thanks to the `SupervisorJob`.
     */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Backing state for `Superwall.subscriptionStatusFlow`. Seeded with
     * [SubscriptionStatus.Unknown] so it is collectable pre-configure; fed by
     * the [DelegateMultiplexer] and by the platform bridge's native collector
     * after [attach].
     */
    val subscriptionStatus: MutableStateFlow<SubscriptionStatus> =
        MutableStateFlow(SubscriptionStatus.Unknown)

    /**
     * Backing stream for `Superwall.customerInfoFlow`. No replay; a small
     * buffer with drop-oldest overflow keeps emission non-suspending from
     * delegate callbacks. Android emissions are pending the upstream
     * delegate-hook audit — the flow may stay empty there.
     */
    val customerInfo: MutableSharedFlow<CustomerInfo> =
        MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /** Whether [attach] has already run — repeat configures must not re-wire. */
    private var attached: Boolean = false

    /**
     * Idempotently attaches the platform bridge's native sources to the flows
     * above via [SuperwallBridge.attachStreams]. Called by the facade at
     * configure-complete, on the main thread; a second call (repeat
     * configure) is a no-op so no duplicate native collectors are created.
     */
    fun attach(bridge: SuperwallBridge) {
        if (attached) return
        attached = true
        bridge.attachStreams(this)
    }
}
