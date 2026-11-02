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
        // API legada do Xposed (de.robv.android.xposed:api:82)
        maven("https://api.xposed.info/") {
            content { includeGroup("de.robv.android.xposed") }
        }
    }
}
rootProject.name = "AOSP-Modern-QS"
include(":app")
