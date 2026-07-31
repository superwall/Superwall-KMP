package com.superwall.sdk.kmp.internal

/**
 * iOS actual for the bridge factory. Returns `IosSuperwallBridge` (a thin
 * forwarder to the `SuperwallKMPBridge` Swift facade) once it lands.
 */
internal actual fun createSuperwallBridge(): SuperwallBridge = TODO("IosSuperwallBridge lands in the next stage")
