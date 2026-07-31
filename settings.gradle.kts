pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "superwall-kmp"
include(":superwall-kmp")
// Compose Multiplatform sample (not published). :sample:iosApp is an
// xcodegen/Xcode project (sample/iosApp/project.yml), not a Gradle module.
include(":sample:shared")
include(":sample:androidApp")
