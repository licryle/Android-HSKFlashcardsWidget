import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.*
import org.gradle.internal.os.OperatingSystem

var os: OperatingSystem? = OperatingSystem.current()

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composePlugin)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.skie)
    id("org.jetbrains.kotlin.native.cocoapods")
}

val appVersionCode = libs.versions.app.versionCode.get()
val appVersionName = libs.versions.app.versionName.get()

buildkonfig {
    packageName = "fr.berliat.hskwidget"
    defaultConfigs {
        buildConfigField(INT, "VERSION_CODE", appVersionCode)
        buildConfigField(STRING, "VERSION_NAME", appVersionName)
        buildConfigField(BOOLEAN, "DEBUG_MODE", "true")
    }
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            }
        }
    }

    cocoapods {
        summary = "HSK Flashcards cross-platform module"
        homepage = "https://github.com/Licryle/HSKFlashcardsWidget"
        version = appVersionName
        ios.deploymentTarget = "16.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "crossPlatform"
            isStatic = true
            export(":googledrivebackup")
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "crossPlatform"
            isStatic = true
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
                dependencies {
                    api(project(":googledrivebackup"))
                }
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
}

android {
    namespace = "fr.berliat.hskwidget"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
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

tasks.register("syncIosVersions") {
    doLast {
        val pbxprojFile = file("../iosApp/hskwidget.xcodeproj/project.pbxproj")
        if (pbxprojFile.exists()) {
            var content = pbxprojFile.readText()
            
            // Replace MARKETING_VERSION (Version Name)
            content = content.replace(Regex("MARKETING_VERSION = [^;]+;"), "MARKETING_VERSION = $appVersionName;")
            
            // Replace CURRENT_PROJECT_VERSION (Build Number / Version Code)
            content = content.replace(Regex("CURRENT_PROJECT_VERSION = [^;]+;"), "CURRENT_PROJECT_VERSION = $appVersionCode;")
            
            pbxprojFile.writeText(content)
            println("Successfully synced version $appVersionName ($appVersionCode) to Xcode project.")
        } else {
            println("Xcode project file not found at ${pbxprojFile.absolutePath}")
        }
    }
}

// Automatically sync versions whenever a framework is created
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>().configureEach {
    finalizedBy("syncIosVersions")
}
