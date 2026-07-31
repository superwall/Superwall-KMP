import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.superwall.sdk"
version = "0.1.0"

kotlin {
    androidLibrary {
        namespace = "com.superwall.sdk.kmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "superwall-kmp", version.toString())

    pom {
        name = "Superwall KMP"
        description = "Kotlin Multiplatform SDK for Superwall, wrapping the native Android and iOS SuperwallKit SDKs."
        inceptionYear = "2026"
        url = "https://github.com/superwall/Superwall-KMP"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "superwall"
                name = "Superwall"
                url = "https://superwall.com"
            }
        }
        scm {
            url = "https://github.com/superwall/Superwall-KMP"
            connection = "scm:git:git://github.com/superwall/Superwall-KMP.git"
            developerConnection = "scm:git:ssh://git@github.com/superwall/Superwall-KMP.git"
        }
    }
}
