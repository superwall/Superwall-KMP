import java.util.Properties

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlinMultiplatform) apply  false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    // Sample app only:
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// ---------------------------------------------------------------------------
// Version resolution, matching Superwall-Android's root build.gradle.kts.
//
// The version comes from the SUPERWALL_VERSION environment variable first (so
// CI can override without touching the tree), then from version.env at the repo
// root. The release workflow reads the resolved value back out through
// `:superwall-kmp:generateBuildInfo`, so bumping version.env is the only edit a
// release needs.
// ---------------------------------------------------------------------------
val superwallVersionFromEnv = System.getenv("SUPERWALL_VERSION")
val superwallVersionFromFile =
    rootDir.resolve("version.env").let { file ->
        if (!file.exists()) return@let null

        Properties().run {
            file.inputStream().use { load(it) }
            getProperty("SUPERWALL_VERSION")
        }
    }

extra["superwallVersion"] =
    superwallVersionFromEnv
        ?: superwallVersionFromFile
        ?: error("Missing SUPERWALL_VERSION. Set environment variable or provide version.env.")
