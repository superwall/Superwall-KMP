package com.superwall.sdk.kmp.sample

import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.customerCenterViewController
import com.superwall.sdk.kmp.gamepadValueChanged
import com.superwall.sdk.kmp.presentedViewController
import platform.GameController.GCController
import platform.GameController.controllerWithExtendedGamepad

actual fun sendSampleGamepadInput(): String {
    // A snapshot controller: a real GCExtendedGamepad with no hardware behind
    // it, which is the only kind a simulator can have.
    val gamepad = GCController.controllerWithExtendedGamepad().extendedGamepad
        ?: return "no extended gamepad available"
    Superwall.gamepadValueChanged(gamepad, gamepad.buttonA)
    Superwall.gamepadValueChanged(gamepad, gamepad.leftThumbstick)
    return "sent buttonA and leftThumbstick"
}

actual fun readPlatformOnlyMembers(): String {
    val presented = Superwall.presentedViewController
    val customerCenter = Superwall.customerCenterViewController()
    return "presentedViewController=${if (presented == null) "null" else "set"}, " +
        "customerCenterViewController=${if (customerCenter.isViewLoaded()) "loaded" else "created"}"
}
