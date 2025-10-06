// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false

    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.crashlytics) apply false

    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.navigation) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKMP) apply false
    alias(libs.plugins.androidLint) apply false
}