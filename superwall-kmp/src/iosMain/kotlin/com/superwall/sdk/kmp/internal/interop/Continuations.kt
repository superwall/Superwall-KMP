@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.interop

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Bridges an ObjC completion-handler API into a suspend call.
 *
 * [register] receives the completion block to hand to the bridge; the
 * coroutine resumes with the value the bridge eventually delivers. The bridge
 * contract is exactly-one invocation; the `isActive` guard degrades a
 * misbehaving double-invocation into a no-op instead of a crash, and a
 * completion firing after cancellation is likewise dropped.
 *
 * Resumption may happen on ANY queue — the SWB facade's async methods complete
 * on arbitrary queues; `suspendCancellableCoroutine` hops back to
 * the caller's dispatcher.
 */
internal suspend fun <T> awaitCompletion(register: (completion: (T) -> Unit) -> Unit): T =
    suspendCancellableCoroutine { continuation ->
        register { value ->
            if (continuation.isActive) {
                continuation.resume(value)
            }
        }
    }

/**
 * [awaitCompletion] for value-less `(() -> Void)` completions (e.g.
 * `SWBSuperwallBridge.dismiss`).
 */
internal suspend fun awaitVoidCompletion(register: (completion: () -> Unit) -> Unit) {
    suspendCancellableCoroutine { continuation ->
        register {
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }
}
