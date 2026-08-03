package com.superwall.sdk.kmp.sample

/**
 * The Superwall public API key this test app configures with, per platform.
 *
 * These are the PUBLIC test-app keys shared with the Flutter SDK's `test_app`
 * (see `test_app/lib/ConfigureTest.dart` in the Superwall-Flutter repo) — they
 * point at Superwall test accounts and are safe to commit:
 *   - Android: `com.superwall.superapp`
 *   - iOS: `com.superwall.Advanced`
 *
 * To run the app against your own dashboard, replace these with the Public
 * API Key from your Superwall dashboard settings (Settings → Keys), and use a
 * campaign with the placements referenced by the test screens
 * (`campaign_trigger`, `non_gated_paywall`, `gated_paywall`, `skip_audience`).
 */
internal expect val superwallApiKey: String
