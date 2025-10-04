pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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
        maven {
            url = uri("${rootDir}/../repo")
        }
    }
}

rootProject.name = "HSKFlashcardsWidget"
include(":app")
include(":pinyin4kot")
include(":hsktextviews")
include(":floatlayouts")
include(":googledrivebackup")
include(":AnkiDroidAPIHelper")
include(":crossPlatform")
include(":androidResources")

