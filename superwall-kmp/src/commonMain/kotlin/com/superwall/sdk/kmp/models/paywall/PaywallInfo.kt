package com.superwall.sdk.kmp.models.paywall

import com.superwall.sdk.kmp.models.triggers.Experiment
import kotlin.time.Instant

/**
 * Contains information about a paywall.
 *
 * All fields are nullable: values are only populated once the corresponding
 * lifecycle stage (presentation, response load, webview load, products load)
 * has occurred.
 */
public data class PaywallInfo(
    /** The identifier set for this paywall in the Superwall dashboard. */
    val identifier: String? = null,
    /** The name set for this paywall in Superwall's web dashboard. */
    val name: String? = null,
    /** The trigger experiment that caused the paywall to present. */
    val experiment: Experiment? = null,
    /** The product IDs that this paywall is displaying, in `[Primary, Secondary, Tertiary]` order. */
    val productIds: List<String>? = null,
    /** The products associated with the paywall. */
    val products: List<Product>? = null,
    /** The URL where this paywall is hosted. */
    val url: String? = null,
    /** The name of the placement that triggered this paywall. `null` if not triggered by a placement. */
    val presentedByPlacementWithName: String? = null,
    /** The Superwall internal id (for debugging) of the placement that triggered this paywall. `null` if not triggered by a placement. */
    val presentedByPlacementWithId: String? = null,
    /** When the placement triggered this paywall. `null` if not triggered by a placement. */
    val presentedByPlacementAt: Instant? = null,
    /** How the paywall was presented: `programmatically`, `identifier`, or `placement`. */
    val presentedBy: String? = null,
    /** The source function that retrieved the paywall: `implicit`, `getPaywall`, or `register`. `null` only when preloading. */
    val presentationSourceType: String? = null,
    /** When the paywall response began loading. */
    val responseLoadStartTime: Instant? = null,
    /** When the paywall response finished loading. */
    val responseLoadCompleteTime: Instant? = null,
    /** When the paywall response failed to load. */
    val responseLoadFailTime: Instant? = null,
    /** The time it took to load the paywall response, in seconds. */
    val responseLoadDuration: Double? = null,
    /** When the paywall webview began loading. */
    val webViewLoadStartTime: Instant? = null,
    /** When the paywall webview finished loading. */
    val webViewLoadCompleteTime: Instant? = null,
    /** When the paywall webview failed to load. */
    val webViewLoadFailTime: Instant? = null,
    /** The time it took to load the paywall website, in seconds. */
    val webViewLoadDuration: Double? = null,
    /** When the paywall products began loading. */
    val productsLoadStartTime: Instant? = null,
    /** When the paywall products finished loading. */
    val productsLoadCompleteTime: Instant? = null,
    /** When the paywall products failed to load. */
    val productsLoadFailTime: Instant? = null,
    /** The time it took to load the paywall products, in seconds. */
    val productsLoadDuration: Double? = null,
    /** The paywall.js version installed on the paywall website. */
    val paywalljsVersion: String? = null,
    /** Indicates whether the paywall is showing free trial content. */
    val isFreeTrialAvailable: Boolean? = null,
    /** Indicates whether the `Superwall.register` `feature` block executes or not. */
    val featureGatingBehavior: FeatureGatingBehavior? = null,
    /** Why this paywall was last closed. [PaywallCloseReason.NONE] if not yet closed. */
    val closeReason: PaywallCloseReason? = null,
    /** The local notifications associated with the paywall. */
    val localNotifications: List<LocalNotification>? = null,
    /** Requests to compute a device property associated with a placement at runtime. */
    val computedPropertyRequests: List<ComputedPropertyRequest>? = null,
    /** Surveys attached to the paywall. */
    val surveys: List<Survey>? = null,
    /**
     * The current state of the paywall as key-value pairs, allowing retrieval of
     * dynamic state information from the paywall.
     */
    val state: Map<String, Any?>? = null,
)
