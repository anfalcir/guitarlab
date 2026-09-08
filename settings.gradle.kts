pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GuitarLabStudio"
include(":app")
include(":core:model")
include(":core:project")
include(":core:codec")

include(":core:audio")
include(":platform:audio-android")
include(":platform:codec-android")
