import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.*

val versionCodeValue = 40
val versionCodeName = "4.0.0"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composePlugin)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.buildKonfig)
}

buildkonfig {
    packageName = "fr.berliat.hskwidget"

    defaultConfigs {
        buildConfigField(INT, "VERSION_CODE", "$versionCodeValue")
        buildConfigField(BOOLEAN, "DEBUG_MODE", "true")
    }
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.normalize)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.androidx.datastore)
                implementation(libs.androidx.datastore.preferences)
                api(libs.androidx.lifecycle.viewmodel)

                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.resources)
                implementation(libs.kermit)

                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)

                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)

                // ./gradlew :cameraK:publishAllPublicationsToLocalRepoRepository in Fork_CameraK
                implementation("com.kashif.cameraK_fork:camerak:0.0.12")
                implementation(libs.navigation.compose)

                implementation(project(":hsktextviews"))
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

                implementation(libs.androidx.lifecycle.service)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.lifecycle.livedata.ktx)

                implementation(libs.androidx.work.runtime.ktx)
                implementation(libs.jieba.analysis)

                implementation(libs.text.recognition.chinese)
                implementation(libs.billing.ktx)
                implementation(libs.review.ktx)
                implementation(libs.androidx.documentfile)

                implementation(libs.play.services.auth)
                implementation(libs.google.api.services.drive)

                implementation(project(":googledrivebackup"))

                // TO remove when Widgets more over to Compose.
                implementation(libs.androidx.constraintlayout)
                implementation(libs.material)
            }
            resources.srcDirs("src/commonMain/composeResources")
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        // iOS target config remains identical
        if (System.getProperty("os.name").contains("Mac")) {
            val xcf = XCFramework()
            val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())

            val xcfName = "crossPlatformKit"
            iosTargets.forEach {
                it.binaries.framework {
                    baseName = xcfName
                    xcf.add(this)
                }
            }

            iosMain {
                dependencies {
                    // Add iOS-specific dependencies here.
                }
            }
        }
    }
}

android {
    namespace = "fr.berliat.hskwidget"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.berliat.hskwidget"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeValue
        versionName = versionCodeName
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        jvmToolchain(11)
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)

    if (System.getProperty("os.name").contains("Mac")) {
        add("kspIosSimulatorArm64", libs.room.compiler)
        add("kspIosX64", libs.room.compiler)
        add("kspIosArm64", libs.room.compiler)
    }
}

// Compose resources
compose.resources {
    publicResClass = true
    packageOfResClass = "fr.berliat.hskwidget"
    generateResClass = always
}

room {
    schemaDirectory("$projectDir/schemas")
}