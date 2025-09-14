import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform") version "2.2.10"
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

kotlin {
    androidLibrary {
        namespace = "fr.berliat.hskwidget.crossPlatform"
        compileSdk = 36
        minSdk = 24

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }


    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    val xcf = XCFramework()
    val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "crossPlatformKit"

    iosTargets.forEach {
        it.binaries.framework {
            baseName = xcfName
            xcf.add(this)
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
                implementation("org.jetbrains.compose.runtime:runtime:1.8.2")
                // Compose Foundation (layout, drawing, gestures)
                implementation("org.jetbrains.compose.foundation:foundation:1.8.2")
                // Compose Material3
                implementation("org.jetbrains.compose.material3:material3:1.8.2")
                // Compose Resources generator (for multi-platform Res)
                implementation("org.jetbrains.compose.components:components-resources:1.8.2")
                // Add KMP dependencies here
                implementation("network.chaintech:cmptoast:1.0.7")
                implementation("co.touchlab:kermit:2.0.8") //Add latest version
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:2.2.20")
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
                implementation("androidx.compose.ui:ui-tooling:1.9.1")
                implementation("androidx.compose.ui:ui-tooling-preview:1.9.1")
            }
            // This explicitly includes the common compose resources as Android assets
            resources.srcDirs("src/commonMain/composeResources")
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation("androidx.test:runner:1.7.0")
                implementation("androidx.test:core:1.7.0")
                implementation("androidx.test.ext:junit:1.3.0")
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }
}

//because the dependency on the compose library is a project dependency
compose.resources {
    generateResClass = always
    publicResClass = true
}