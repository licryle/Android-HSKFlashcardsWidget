import org.gradle.api.tasks.testing.Test

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.composePlugin) apply false

    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.crashlytics) apply false

    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.navigation) apply false
    alias(libs.plugins.androidKMP) apply false
    alias(libs.plugins.androidLint) apply false
}

// Minimal logging to see test results in the console
subprojects {
    tasks.withType<Test>().configureEach {
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

// Single target to run all local tests
tasks.register("allTests") {
    group = "verification"
    description = "Run all local unit tests across all modules"
    
    subprojects.forEach { prj ->
        // Depends on the 'test' task of each module, which aggregates target-specific tests
        dependsOn(prj.tasks.matching { it.name == "test" })
    }
}
