// =============================================================================
// sample:webMinimal — the smallest Kotlin/JS app using :superwall-kmp on the
// web: a static HTML page, a few buttons, no Compose. NOT published.
//
// It carries exactly what a consumer's web module needs (README "Web
// (Kotlin/JS)"): an ES-module js target and webpack.config.d/superwall.js.
//
// Run: ./gradlew :sample:webMinimal:jsBrowserDevelopmentRun
// =============================================================================
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js(IR) {
        // @superwall/paywalls-js, which :superwall-kmp wraps, is ESM-only.
        useEsModules()
        browser {
            commonWebpackConfig {
                outputFileName = "webMinimal.mjs"
            }
        }
        binaries.executable()
    }

    compilerOptions {
        // superwall-kmp models carry kotlin.time.Instant fields.
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":superwall-kmp"))
        }
    }
}
