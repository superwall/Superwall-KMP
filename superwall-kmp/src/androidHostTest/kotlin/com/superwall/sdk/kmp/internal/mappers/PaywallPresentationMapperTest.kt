package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.paywall.PaywallPresentationInfo
import com.superwall.sdk.kmp.models.paywall.PaywallPresentationStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import com.superwall.sdk.models.paywall.PaywallPresentationInfo as NativePaywallPresentationInfo
import com.superwall.sdk.models.paywall.PaywallPresentationStyle as NativePaywallPresentationStyle

/** `PaywallInfo.presentation`, against the REAL superwall-android types. */
class PaywallPresentationMapperTest {
    @Test
    fun everyNativeStyleMapsWithItsDimensions() {
        val cases =
            mapOf(
                NativePaywallPresentationStyle.Modal to PaywallPresentationStyle.Modal,
                NativePaywallPresentationStyle.Fullscreen to PaywallPresentationStyle.Fullscreen,
                NativePaywallPresentationStyle.FullscreenNoAnimation to PaywallPresentationStyle.FullscreenNoAnimation,
                NativePaywallPresentationStyle.Push to PaywallPresentationStyle.Push,
                NativePaywallPresentationStyle.None to PaywallPresentationStyle.None,
                NativePaywallPresentationStyle.Drawer(height = 0.7, cornerRadius = 16.0) to
                    PaywallPresentationStyle.Drawer(height = 0.7, cornerRadius = 16.0),
                NativePaywallPresentationStyle.Popup(height = 0.5, width = 0.8, cornerRadius = 12.0) to
                    PaywallPresentationStyle.Popup(height = 0.5, width = 0.8, cornerRadius = 12.0),
            )
        for ((native, expected) in cases) {
            assertEquals(expected, native.toKmp())
        }
    }

    @Test
    fun delayIsCarriedInMilliseconds() {
        assertEquals(
            PaywallPresentationInfo(PaywallPresentationStyle.Modal, delayMillis = 300),
            NativePaywallPresentationInfo(style = NativePaywallPresentationStyle.Modal, delay = 300).toKmp(),
        )
    }
}
