package com.superwall.sdk.kmp.models.options

/**
 * Controls when the SDK enters test mode.
 */
public enum class TestModeBehavior {
    /** Test mode is automatically determined based on server configuration. */
    AUTOMATIC,

    /** Test mode is enabled only when the server enables it for the user. */
    WHEN_ENABLED_FOR_USER,

    /** Test mode is never activated, regardless of configuration. */
    NEVER,

    /** Test mode is always activated, regardless of configuration. */
    ALWAYS,
}

/**
 * The different network environments that the SDK should use.
 *
 * Only use this to set [SuperwallOptions.networkEnvironment] if told so
 * explicitly by the Superwall team.
 */
public enum class NetworkEnvironment {
    /** Default: Uses the standard latest environment. */
    RELEASE,

    /**
     * **WARNING**: Uses a release candidate environment. This is not meant
     * for a production environment.
     */
    RELEASE_CANDIDATE,

    /**
     * **WARNING**: Uses the nightly build environment. This is not meant for
     * a production environment.
     */
    DEVELOPER,
}

/**
 * The level of logging that the SDK should print to the console.
 *
 * Note: on iOS the corresponding Swift case for [NONE] is `.none` — take care
 * when reading native logs or Swift documentation, as Swift's `.none` can be
 * confused with an absent optional.
 */
public enum class LogLevel {
    /** Prints all logs from the SDK. */
    DEBUG,

    /** Prints informational, warning and error logs. */
    INFO,

    /** Prints warning and error logs. */
    WARN,

    /** Prints error logs only. */
    ERROR,

    /** Turns off all SDK logging. */
    NONE,
}

/**
 * The possible scope of logs to print to the console.
 */
public enum class LogScope {
    /** Logs from the localization manager. */
    LOCALIZATION_MANAGER,

    /** Logs from the bounce button. */
    BOUNCE_BUTTON,

    /** Logs from Core Data. */
    CORE_DATA,

    /** Logs from the configuration manager. */
    CONFIG_MANAGER,

    /** Logs from the identity manager. */
    IDENTITY_MANAGER,

    /** Logs from the debug manager. */
    DEBUG_MANAGER,

    /** Logs from the debug view controller. */
    DEBUG_VIEW_CONTROLLER,

    /** Logs from the localization view controller. */
    LOCALIZATION_VIEW_CONTROLLER,

    /** Logs from the game controller manager. */
    GAME_CONTROLLER_MANAGER,

    /** Logs about device attributes. */
    DEVICE,

    /** Logs from the network layer. */
    NETWORK,

    /** Logs about paywall events. */
    PAYWALL_EVENTS,

    /** Logs from the products manager. */
    PRODUCTS_MANAGER,

    /** Logs from the StoreKit manager. */
    STORE_KIT_MANAGER,

    /** Logs about placements. */
    PLACEMENTS,

    /** Logs about receipts. */
    RECEIPTS,

    /** Logs from the Superwall core. */
    SUPERWALL_CORE,

    /** Logs about paywall presentation. */
    PAYWALL_PRESENTATION,

    /** Logs about transactions. */
    TRANSACTIONS,

    /** Logs from the paywall view controller. */
    PAYWALL_VIEW_CONTROLLER,

    /** Logs from the cache. */
    CACHE,

    /** Logs from all scopes. */
    ALL,
}

/**
 * The configuration status of the SDK.
 */
public enum class ConfigurationStatus {
    /** The SDK has not yet finished configuring. */
    PENDING,

    /** The SDK is configured and ready to present paywalls. */
    CONFIGURED,

    /** The SDK failed to configure. */
    FAILED,
}

/**
 * Device tier classification used by [PaywallOptions.preloadDeviceOverrides].
 *
 * Tiers are evaluated from device specs (RAM, CPU, codec support).
 * Android only.
 */
public enum class DeviceTier {
    /** ≤2GB RAM, ~1.2GHz quad-core, missing modern codecs (e.g. Android Go). */
    ULTRA_LOW,

    /** 3–4GB RAM, ~1.8GHz 4–8 cores, partial codec support (entry-level). */
    LOW,

    /** 4–6GB RAM, ~2.0–2.4GHz 8 cores, full codec support. */
    MID,

    /** 6–8GB RAM, ~2.4–2.8GHz 8 cores, high-density display. */
    HIGH,

    /** ≥10GB RAM, 2.8+GHz with prime cores ≥3.5GHz. */
    ULTRA_HIGH,

    /** Device info could not be evaluated. */
    UNKNOWN,
}

/**
 * Controls which events are sent to the Superwall servers.
 */
public enum class EventTrackingBehavior {
    /** All events are tracked. This is the default. */
    ALL,

    /**
     * Only internal Superwall events are tracked; user-initiated tracking
     * calls, trigger-fire events, and user-attribute updates are suppressed.
     */
    SUPERWALL_ONLY,

    /** No events are sent to the Superwall servers. */
    NONE,
}
