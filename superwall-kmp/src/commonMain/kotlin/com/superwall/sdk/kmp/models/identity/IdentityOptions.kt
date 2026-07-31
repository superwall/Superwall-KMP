package com.superwall.sdk.kmp.models.identity

/**
 * Options passed in when calling `Superwall.identify(userId, options)`.
 */
public data class IdentityOptions(
    /**
     * Determines whether the SDK should wait to restore paywall assignments
     * from the server before presenting any paywalls. Defaults to `false`.
     *
     * This should only be used in advanced use cases. By setting this to
     * `true`, it prevents paywalls from showing until after paywall
     * assignments have been restored. If you expect users of your app to
     * switch accounts or delete/reinstall a lot, you'd set this when users
     * log in to an existing account.
     */
    val restorePaywallAssignments: Boolean = false,
)
