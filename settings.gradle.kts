pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "HSKFlashcardsWidget"
include(":app")
include(":hsktextviews")
include(":floatlayouts")
include(":googledrivebackup")
include(":pinyin4kot")
include(":AnkiDroidAPIHelper")
include(":crossPlatform")
include(":crossPlatform:androidmain")
