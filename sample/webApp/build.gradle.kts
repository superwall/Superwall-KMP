// =============================================================================
// sample:webApp — thin browser host for the shared Compose UI. NOT published.
//
// Like :sample:androidApp it contains no @Composable code: main() calls
// startSampleApp() from :sample:shared. The org.jetbrains.compose plugin is
// still applied because it is what unpacks Skiko's WebAssembly runtime (the
// Compose canvas renderer) into this executable's output; that plugin refuses
// to configure without the Compose compiler plugin, which in turn fails
// without compose.runtime on the classpath — hence both, for no composables.
//
// Run: ./gradlew :sample:webApp:jsBrowserDevelopmentRun
// =============================================================================
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    js(IR) {
        useEsModules()
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.mjs"
            }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":sample:shared"))
            implementation(compose.runtime)
        }
    }
}
