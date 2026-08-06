// =============================================================================
// sample:androidApp — thin Android host for the shared Compose UI. NOT published.
//
// AGP 9's new DSL (`android.newDsl`, on by default) rejects the
// `org.jetbrains.kotlin.android` plugin, so this module relies on AGP 9's
// BUILT-IN Kotlin support: `com.android.application` alone compiles
// MainActivity.kt. That also means no `org.jetbrains.kotlin.plugin.compose`
// here — MainActivity contains no @Composable code; it calls
// setSampleAppContent() from :sample:shared, where all Compose compilation
// (and the Compose compiler plugin) lives.
// =============================================================================
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.superwall.sdk.kmp.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Must match the Superwall dashboard app that `superwallApiKey` (androidMain)
        // belongs to — the same platform-app the Flutter SDK's test_app uses. The
        // ported Maestro flows target this id too (`appId:` in sample/maestro/**).
        // `namespace` stays com.superwall.sdk.kmp.sample: that's the Kotlin package.
        applicationId = "com.superwall.superapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // Sample only — no shrinking, no signing config.
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":sample:shared"))
    // ComponentActivity also arrives via :sample:shared's `api` dependency;
    // declared explicitly since MainActivity extends it directly.
    implementation(libs.androidx.activity.compose)
}
