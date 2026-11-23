package com.superwall.sdk.kmp.models.paywall

/** How a paywall is presented, as set in the paywall editor. */
public data class PaywallPresentationInfo(
    /** The presentation style. */
    val style: PaywallPresentationStyle,
    /** The delay, in milliseconds, before the loading view switches to the paywall. */
    val delayMillis: Long,
)

/** The style a paywall is presented in. */
public sealed interface PaywallPresentationStyle {
    /** A sheet over the app. */
    public data object Modal : PaywallPresentationStyle

    /** Slides up to cover the screen. */
    public data object Fullscreen : PaywallPresentationStyle

    /** Covers the screen without animation. */
    public data object FullscreenNoAnimation : PaywallPresentationStyle

    /** Pushed on screen, as onto a navigation stack. */
    public data object Push : PaywallPresentationStyle

    /** Slides up to cover part of the screen. */
    public data class Drawer(
        val height: Double,
        val cornerRadius: Double,
    ) : PaywallPresentationStyle

    /** Pops over the top of the app. */
    public data class Popup(
        val height: Double,
        val width: Double,
        val cornerRadius: Double,
    ) : PaywallPresentationStyle

    /** No style of its own: the one set in the dashboard applies. */
    public data object None : PaywallPresentationStyle
}

/** Whether users see a paywall's introductory offers. iOS only. */
public enum class IntroOfferEligibility {
    /** Always shown as eligible. */
    ELIGIBLE,

    /** Always shown as ineligible. */
    INELIGIBLE,

    /** Determined from the user's purchase history. */
    AUTOMATIC,
}
