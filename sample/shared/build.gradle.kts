import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// =============================================================================
// sample:shared — the Compose Multiplatform demo UI (plan §8, sample row).
//
// NOT published (no maven-publish plugin). It consumes :superwall-kmp exactly
// like a real app's shared module would (README "Installation"): a plain
// project dependency on Android, and a static Kotlin framework ("SampleShared")
// on iOS — static because the superwall-kmp klib is compile-only against the
// SuperwallKMPBridge; the iOS app supplies the bridge binary by linking the
// SPM package (sample/iosApp/project.yml uses the local ../../bridge source
// package until the first release publishes a real binary artifact).
// =============================================================================
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    // Compose compiler ships with Kotlin (same 2.3.10 version); the
    // org.jetbrains.compose plugin adds the multiplatform Compose artifacts.
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    androidLibrary {
        namespace = "com.superwall.sdk.kmp.sample.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    // Per the README consumer guidance: export the shared module as a STATIC
    // framework. isStatic is required — the Kotlin framework does not embed
    // the SuperwallKMPBridge binary; the app links it via SPM.
    //
    // No iosX64 here: Compose Multiplatform 1.11.x no longer publishes
    // ios_x64 (uikitx64) variants — the Intel iOS simulator was dropped — so
    // a Compose-consuming iosX64 target cannot resolve compose.runtime/
    // foundation/ui and fails Gradle sync. The published :superwall-kmp
    // module keeps iosX64 (it doesn't depend on Compose).
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "SampleShared"
            isStatic = true
        }
    }

    compilerOptions {
        // superwall-kmp models carry kotlin.time.Instant fields.
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":superwall-kmp"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }

        androidMain.dependencies {
            // `api` so :sample:androidApp sees ComponentActivity/setContent on
            // its compile classpath through the setSampleAppContent() helper.
            api(libs.androidx.activity.compose)
        }
    }
}
