pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "ROCKETBOY"
include(":app")

// Self-contained instrumentation dependencies for testing the minified release APK.
include(":releaseChecks")
project(":releaseChecks").projectDir = file("tools/play/release-checks")
