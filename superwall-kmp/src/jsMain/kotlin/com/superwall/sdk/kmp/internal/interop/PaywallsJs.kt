@file:JsModule("@superwall/paywalls-js")

package com.superwall.sdk.kmp.internal.interop

import kotlin.js.Promise

// Hand-written bindings for the slice of @superwall/paywalls-js the bridge
// calls. Payload types (PaywallInfo, Entitlement, results, …) stay `dynamic`
// and are read field-by-field in the mappers: they are plain JSON-shaped
// objects, and typing them here would only duplicate the mappers.

/** `createSuperwall(opts)` — constructs an instance and starts configuring it. */
internal external fun createSuperwall(opts: dynamic): JsSuperwall

/** The `@superwall/paywalls-js` package version. */
internal external val SDK_VERSION: String

/** The SDK's read-only signal: a current value plus change subscription. */
internal external interface JsReadable<T> {
    val value: T

    /** Returns the unsubscribe function. */
    fun subscribe(listener: (T) -> Unit): () -> Unit
}

internal external interface JsSuperwall {
    /**
     * Settles when the initial configure pass finishes. A failed config
     * fetch usually still RESOLVES (status flips to `"failed"`); only an
     * internal error rejects — so callers read [configurationStatus] after.
     */
    val ready: Promise<Unit>
    val isConfigured: JsReadable<Boolean>

    /** `"pending" | "configured" | "failed"`. */
    val configurationStatus: JsReadable<String>

    val user: JsUserNamespace
    val placements: JsPlacementsNamespace
    val purchases: JsPurchasesNamespace
    val entitlements: JsEntitlementsNamespace

    /** `SubscriptionStatus` union: `{ status: "UNKNOWN" | "INACTIVE" | "ACTIVE", entitlements? }`. */
    val subscriptionStatus: JsReadable<dynamic>
    val customerInfo: JsReadable<dynamic>
    val latestPaywallInfo: JsReadable<dynamic>
    val isPaywallPresented: JsReadable<Boolean>

    /** `"debug" | "info" | "warn" | "error" | "none"`. */
    /** A DOM `EventTarget` dispatching `CustomEvent`s named after wire events. */
    val events: JsEventTarget

    val logLevel: JsReadable<String>
    val locale: JsReadable<String?>

    fun register(args: dynamic): Promise<dynamic>

    fun redeem(code: String): Promise<dynamic>

    fun setLogLevel(level: String)

    fun setLocale(locale: String?)

    fun setDelegate(delegate: dynamic)

    fun setInterfaceStyle(style: String?)

    fun reset(): Promise<Unit>

    fun dismiss(reason: String = definedExternally)
}

internal external interface JsUserNamespace {
    val id: JsReadable<String>
    val effectiveId: JsReadable<String>
    val isLoggedIn: JsReadable<Boolean>
    val attributes: JsReadable<dynamic>
    val integrationAttributes: JsReadable<dynamic>

    fun identify(
        userId: String,
        opts: dynamic = definedExternally,
    ): Promise<Unit>

    fun setAttributes(attrs: dynamic)

    fun setIntegrationAttribute(
        attr: String,
        value: String?,
    )

    fun setIntegrationAttributes(attrs: dynamic)
}

internal external interface JsPlacementsNamespace {
    fun getPresentationResult(
        placement: String,
        params: dynamic = definedExternally,
    ): Promise<dynamic>

    fun confirmAllAssignments(): Promise<Array<dynamic>>

    fun preloadAll(): Promise<Unit>

    fun preloadFor(placementNames: Array<String>): Promise<Unit>
}

internal external interface JsPurchasesNamespace {
    /** `RestorationResult` from the release after 0.3.0; `undefined` on 0.3.0. */
    fun restore(): Promise<dynamic>

    fun setSubscriptionStatus(status: dynamic)

    fun getCustomerInfo(): Promise<dynamic>
}

internal external interface JsEntitlementsNamespace {
    val active: JsReadable<Array<dynamic>>
    val inactive: JsReadable<Array<dynamic>>
    val all: JsReadable<Array<dynamic>>

    fun byProductIds(ids: Array<String>): Array<dynamic>
}

internal external interface JsEventTarget {
    fun addEventListener(
        type: String,
        listener: (dynamic) -> Unit,
    )

    fun removeEventListener(
        type: String,
        listener: (dynamic) -> Unit,
    )
}
