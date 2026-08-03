package com.superwall.sdk.kmp.internal

/**
 * iOS actual for the bridge factory: a thin forwarder to the
 * `SuperwallKMPBridge` Swift facade (plan §5).
 */
internal actual fun createSuperwallBridge(): SuperwallBridge = IosSuperwallBridge()
