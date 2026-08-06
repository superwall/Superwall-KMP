# Maestro flows — Superwall KMP sample

UI flows for `sample/`, ported from the Flutter SDK's `test_app/maestro/`
(Superwall-Flutter repo). Same screens, same assertions.

## Running

The flows take the app id as a parameter — the sample ships under a different id
per platform, each matching the Superwall dashboard app whose API key that
platform uses:

```bash
# iOS (simulator must be booted, app installed — see ../README.md)
maestro test -e APP_ID=com.superwall.Advanced sample/maestro/flow.yaml

# Android (emulator/device connected)
maestro test -e APP_ID=com.superwall.superapp sample/maestro/flow.yaml

# whole suite
maestro test -e APP_ID=com.superwall.Advanced sample/maestro/
```

> Do **not** add an `env:` defaults block to a flow to avoid passing `-e`. An
> in-file default silently overrides `-e` on the command line, so the flow would
> always run against whichever id is hard-coded.

**With both a simulator and an emulator running, pass `--udid`** (before the
`test` subcommand) — otherwise Maestro picks one for you and you get
`Package com.superwall.Advanced is not installed` from the Android side:

```bash
maestro --udid <ios-sim-udid> test -e APP_ID=com.superwall.Advanced sample/maestro/flow.yaml
```

Maestro is the mobile UI framework from mobile.dev. Beware: `brew install --cask
maestro` installs an unrelated macOS app of the same name. The right one is:

```bash
brew install mobile-dev-inc/tap/maestro
```

If that unrelated cask is also installed, Homebrew refuses to symlink the CLI
("maestro cask is installed, skipping link") and you must invoke it by full path,
e.g. `/opt/homebrew/Cellar/maestro/<version>/bin/maestro`.

## Status

| Flow | iOS simulator | Android emulator |
|---|---|---|
| `flow.yaml` — configure + subscription status | passes | passes |
| `handler/flow.yaml` — presentation handler | passes | passes |
| `delegate/flow.yaml` — delegate callbacks | passes | passes |
| `purchasecontroller/test_mode_purchases.yaml` — simulated purchases | passes | **blocked** — see below |
| `purchasecontroller/test_pc_purchases.yaml` | blocked — needs StoreKit products | not run — needs Play Billing sandbox |
| `purchasecontroller/no_pc_purchases.yaml` | blocked — needs a purchase sandbox | not run — needs Play Billing sandbox |

On Android, test mode activates (the "Test Mode Active" sheet appears and reports
products come from the dashboard), but tapping the paywall's CONTINUE produces no
simulated-purchase drawer — 10s later the paywall is unchanged. The flow's
purchase steps therefore cannot run there.

### Buying without a store: test mode

`test_mode_purchases.yaml` is the one purchase flow that runs unattended
anywhere. It taps **"Configure with test mode"**, which configures with
`TestModeBehavior.ALWAYS`. In test mode the SDK resolves products from
Superwall's servers instead of StoreKit / Play Billing and simulates the
transaction in its own drawer, so the paywall shows real prices and can be
bought with no StoreKit configuration and no sandbox account. The flow covers
both outcomes — simulated failure (feature block must NOT run) and simulated
purchase (entitlement granted, feature block runs).

It does **not** replace `test_pc_purchases.yaml`: test mode bypasses the purchase
controller along with the store, so `TestingPurchaseController` is never invoked
during a test-mode run.

Two quirks worth knowing, both handled in the flow:

- Entering test mode raises the SDK's own **"🧪 Test Mode Active"** sheet, which
  must be dismissed with OK. Maestro matches the *whole* string, so the assertion
  has to allow for the emoji (`.*Test Mode Active.*`).
- The **simulated-purchase drawer is invisible to Maestro** — with it plainly on
  screen, `maestro hierarchy` contains none of its labels, so neither
  `assertVisible` nor `tapOn` by text works on it. Its buttons are tapped by
  position instead, and every assertion is made against the app's own UI
  afterwards.

### Why the other two purchase flows are blocked

The paywall resolves its products through StoreKit. On a bare simulator there is
no product source, so the paywall presents with an empty price
(`/ (only/month)`) and its CONTINUE button does nothing — no purchase is ever
attempted, and the flows die on the first assertion past CONTINUE. This applies
to the mock-purchase-controller flow too: the controller only takes over the
*purchase*, not product resolution.

The sample ships a StoreKit configuration for this — `sample/iosApp/Products.storekit`,
copied from the Flutter test_app, attached to the SampleApp scheme's run action
(`sample/iosApp/project.yml` → `schemes:`). But it does **not** transfer to a
Maestro run: a StoreKit configuration is applied by *Xcode* when it launches the
app, and Maestro launches through `simctl`, which has no equivalent option
(`xcrun simctl` exposes no storekit command). So the two purchase flows still
need one of:

- **Xcode**: open `SuperwallKMPSample.xcodeproj`, Run — products resolve, and the
  purchase screens can be driven by hand. The flows themselves stay unrunnable.
- **A real sandbox account** (StoreKit test session on iOS, Play sandbox on
  Android) — the only route that makes these two flows pass unattended.

## Deviations from the Flutter originals

Each is commented at its site in the YAML:

- **`appId` is parameterized** (`${APP_ID}`). The originals hard-coded
  `com.superwall.superapp` and passed a separate `${PLATFORM_ID}` to `launchApp`,
  which only ever matched Android.
- **`util/back.yaml` taps "Back" on both platforms.** The Flutter app used a real
  Navigator, so Android could pop with hardware back. The KMP sample navigates by
  in-app state (`App.kt`) with no `BackHandler`, so hardware back would exit the
  app; a "Back" TextButton is present on every non-Home screen instead. Hardware
  back is still used on Android when no "Back" is on screen (dismissing a paywall).
- **`util/dismiss_storekit_signin.yaml` is new.** On a simulator with no Apple
  Account, iOS raises a "Sign in to Apple Account" system alert shortly after
  configure that covers the app's own dialogs. It is dismissed after launch and
  after each configure. It uses an optional `tapOn` rather than a
  `runFlow: when: visible:` guard because a `when:` condition is evaluated against
  the app's view hierarchy, which does not contain system alerts — the guard
  always evaluates false while the alert is plainly on screen.
- **Deep-link return label.** `"Return to Superwallkit Flutter"` →
  `"Return to Superwall KMP Sample"` (this sample's `CFBundleDisplayName`).
- **Safari dismiss.** The originals tapped `"Done"`; iOS 26 renders an ✕ labelled
  `"Close"`. Both are attempted.
- **No `"Mock PC Test"` tap** in the purchase flows — in this app that string is
  the screen *title* that "Purchase Controller Test" navigates straight to, not an
  intermediate button.
