# Superwall KMP

Kotlin Multiplatform SDK for [Superwall](https://superwall.com) — remotely configurable in-app paywall infrastructure.

This library wraps the native SuperwallKit SDKs for Android and iOS behind a single Kotlin Multiplatform API, forwarding all calls to the platform implementations.

## Coordinates

```
com.superwall.sdk:superwall-kmp
```

## Targets

- Android (`minSdk 24`)
- iOS (`iosArm64`, `iosSimulatorArm64`, `iosX64`)

## Development

```bash
# Build the library
./gradlew :superwall-kmp:build

# Run tests
./gradlew :superwall-kmp:allTests
```

## Project structure

- `superwall-kmp/src/commonMain` — shared public API (models, interfaces, `Superwall` entry point)
- `superwall-kmp/src/androidMain` — Android implementation wrapping the SuperwallKit Android SDK
- `superwall-kmp/src/iosMain` — iOS implementation wrapping the SuperwallKit iOS SDK

## License

Apache License 2.0 — see [LICENSE](LICENSE).
