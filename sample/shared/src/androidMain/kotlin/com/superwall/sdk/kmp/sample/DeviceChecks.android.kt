package com.superwall.sdk.kmp.sample

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.superwall.sdk.kmp.Superwall
import com.superwall.sdk.kmp.dispatchKeyEvent
import com.superwall.sdk.kmp.dispatchMotionEvent

actual fun sendSampleGamepadInput(): String {
    Superwall.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A))
    Superwall.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A))
    val now = SystemClock.uptimeMillis()
    val motion = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0.5f, -0.5f, 0)
    // superwall-android only reads axes off joystick-sourced events.
    motion.source = InputDevice.SOURCE_JOYSTICK
    Superwall.dispatchMotionEvent(motion)
    motion.recycle()
    return "sent BUTTON_A down/up and a joystick move"
}

actual fun readPlatformOnlyMembers(): String = "n/a on Android"
