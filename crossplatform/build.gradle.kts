import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.*
import org.gradle.internal.os.OperatingSystem

var os: OperatingSystem? = OperatingSystem.current()

val versionCodeValue = 46
val versionCodeName = "4.0.2"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.androidLibrary) // Changed from androidApplication
    alias(libs.plugins.composePlugin)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.skie)
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
                implementation(libs.camerak)
                implementation(libs.navigation.compose)
                implementation(project(":hsktextviews"))
                implementation(project(":googledrivebackup"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.androidx.room.sqlite.wrapper)
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
                implementation(libs.androidx.constraintlayout)
                implementation(libs.material)

                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.analytics)
                implementation(libs.firebase.crashlytics)

                // To remove alongside removing PrefCompat
                implementation("androidx.preference:preference-ktx:1.2.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.turbine)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        if (os?.isMacOsX == true) {
            val iosMain by creating {
                dependsOn(commonMain)
            }
            val iosTest by creating {
                dependsOn(commonTest)
            }
            listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
                target.compilations["main"].defaultSourceSet.dependsOn(iosMain)
                target.compilations["test"].defaultSourceSet.dependsOn(iosTest)
            }
        }
    }

    if (os?.isMacOsX == true) {
        val xcf = XCFramework()
        val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())

        iosTargets.forEach { target ->
            target.binaries.framework {
                baseName = "crossPlatformKit"
                xcf.add(this)
            }
        }
    }
}

android {
    namespace = "fr.berliat.hskwidget"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        // applicationId, versionCode, versionName moved to androidApp
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
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
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    if (System.getProperty("os.name").contains("Mac")) {
        add("kspIosSimulatorArm64", libs.room.compiler)
        add("kspIosX64", libs.room.compiler)
        add("kspIosArm64", libs.room.compiler)
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "fr.berliat.hskwidget"
    generateResClass = always
}

room {
    schemaDirectory("$projectDir/schemas")
}

skie {
    build {
        produceDistributableFramework()
    }
}
