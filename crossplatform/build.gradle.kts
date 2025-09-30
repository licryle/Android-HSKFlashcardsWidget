import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform") version "2.2.10"
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidLibrary {
        namespace = "fr.berliat.hskwidget.crossPlatform"
        compileSdk = 36
        minSdk = 26

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
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.resources)
                implementation(libs.cmptoast)
                implementation(libs.kermit)

                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)

                implementation(libs.normalize)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.androidx.datastore)
                implementation(libs.androidx.datastore.preferences)
                api(libs.androidx.lifecycle.viewmodel)

                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)

                implementation(project(":hsktextviews"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.androidx.room.sqlite.wrapper)

                // Anki
                implementation(libs.anki.android)
                implementation(project(":AnkiDroidAPIHelper"))

                implementation(libs.androidx.lifecycle.service) // LifecycleService
                implementation(libs.androidx.lifecycle.runtime.ktx)

                implementation(libs.jieba.analysis)

                // Background TTS - maybe review and simplify
                implementation(libs.androidx.work.runtime.ktx)
                implementation(libs.androidx.lifecycle.livedata.ktx)

                // OCR
                implementation(libs.text.recognition.chinese)

                implementation(libs.billing.ktx)
                implementation(libs.review.ktx)

                implementation(libs.androidx.documentfile)

                implementation(libs.play.services.auth)
                implementation(libs.google.api.services.drive)

                implementation(project(":googledrivebackup"))
            }
            resources.srcDirs("src/commonMain/composeResources")
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.junit.ext)
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

tasks.matching { it.name.startsWith("extract") && it.name.endsWith("Annotations") }
    .configureEach {
        mustRunAfter(tasks.matching { it.name.startsWith("ksp") })
    }

dependencies {
    // KSP support for Room Compiler.
    add("kspAndroid", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
}

// Compose resources
compose.resources {
    generateResClass = always
    publicResClass = true
}

// set schema
room {
    schemaDirectory("$projectDir/schemas")
}
