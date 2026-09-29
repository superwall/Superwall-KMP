# CLAUDE.md — Superwall KMP

Kotlin Multiplatform SDK wrapping the native Superwall SDKs behind one common API.

**Read [`../.agents/AGENTS.md`](../.agents/AGENTS.md) for the full picture** —
SDK usage, architecture, and the release process. This file is the short version
plus the things that are easy to get wrong in this repo.

## Commands

```bash
./gradlew :superwall-kmp:build      # build the library
./gradlew :superwall-kmp:allTests   # common + Android host + iOS simulator tests
./gradlew :superwall-kmp:check      # lint
./gradlew :sample:androidApp:installDebug
```

Bridge (Swift) tests — **`swift test` does not work here**, it builds for macOS
where the iOS-only SuperwallKit can't compile:

```bash
cd bridge && xcodebuild test -scheme SuperwallKMPBridge \
  -workspace . -destination 'platform=iOS Simulator,name=iPhone 17'
```

## Where code goes

- Public API: **`commonMain` only.** No platform types in public signatures;
  `configure` is identical on both platforms. `explicitApi()` is on, so every
  public declaration needs explicit visibility and return type.
- Platform work goes behind the internal `SuperwallBridge` (`expect`/`actual`):
  `androidMain` wraps `superwall-android`; `iosMain` calls the `@objc` Swift
  bridge in `bridge/` through cinterop; `jsMain` wraps the
  `@superwall/paywalls-js` npm package.
- Touching `bridge/Sources/**` changes an ObjC surface Kotlin is compiled
  against — rebuild the XCFramework and expect cinterop fallout.

## Easy to get wrong

- **`sample/iosApp/*.xcodeproj` is generated and gitignored.** Edit
  `sample/iosApp/project.yml`, then `xcodegen generate`. Anything set in the
  Xcode UI (signing Team included) is lost on regeneration.
- **`Package.swift` url/checksum are machine-owned.** The release workflow
  rewrites and commits them before tagging. Never hand-edit.
- **Version lives in `version.env`**, not in any `build.gradle.kts`. It is read
  at configuration time with plain file I/O, so the configuration cache does not
  see changes to it — pass `--no-configuration-cache` when a version change must
  take effect.
- Callbacks (delegate, presentation handler, flows) are **main-thread**; keep it
  that way.
- **Bumping `superwall-paywalls-js`** means re-checking the web event and model
  surface: `jsTest`'s `EventMapperTest` pins the web `SuperwallEventMap` keys and
  `OptionsMapperTest` the `IntegrationAttribute` union — update both lists from
  Superwall-Web's `types.ts` / `events.ts`, don't just bump the number.
- Kotlin/JS uses **npm, not Yarn** (`kotlin.js.yarn=false`); the lockfile is
  `kotlin-js-store/package-lock.json`. Refresh it with
  `./gradlew kotlinUpgradePackageLock` after changing npm dependencies.
- The SDK **has no pre-configure call queue** — most members throw
  `SuperwallError.NotConfigured`. That behaviour is intentional; don't "fix" it.

## Conventions

Match the surrounding comment density — this codebase explains *why*, not what.
When a workaround exists because of an external constraint (an Apple behaviour, a
Maestro limitation, an SPM rule), say so at the site, because the next reader
will otherwise "simplify" it away.
