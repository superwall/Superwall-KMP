package com.superwall.sdk.kmp.internal

/**
 * Android actual for the bridge factory: returns [AndroidSuperwallBridge],
 * the port of the Flutter plugin's `SuperwallHost.kt` wired directly to
 * superwall-android 2.8.0.
 */
internal actual fun createSuperwallBridge(): SuperwallBridge = AndroidSuperwallBridge()
