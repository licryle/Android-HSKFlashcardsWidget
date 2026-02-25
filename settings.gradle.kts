pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
include(":pinyin4kot")
include(":hsktextviews")
include(":floatlayouts")
include(":googledrivebackup")
include(":AnkiDroidAPIHelper")
include(":crossPlatform")
include(":androidApp")
