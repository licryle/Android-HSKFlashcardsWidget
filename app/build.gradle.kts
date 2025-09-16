plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin") // Safe Args plugin
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    kotlin("kapt")
}

android {
    namespace = "fr.berliat.hskwidget"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.berliat.hskwidget"
        minSdk = 26
        //noinspection EditedTargetSdkVersion
        targetSdk = 36
        versionCode = 38
        versionName = "3.8.4"
        buildConfigField("String", "APP_VERSION", "\"$versionName-$versionCode\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        dataBinding = true
        compose = true
    }
    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.material:material-ripple:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-service:2.9.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.3")
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    implementation("androidx.datastore:datastore:1.1.7")
    implementation("androidx.datastore:datastore-core:1.1.7")
    implementation("androidx.datastore:datastore-rxjava2:1.1.7")
    implementation("androidx.datastore:datastore-rxjava3:1.1.7")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.datastore:datastore-preferences-core:1.1.7")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))

    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    implementation("com.google.code.gson:gson:2.13.1")

    val roomVersion = "2.7.2"

    implementation("androidx.room:room-runtime:$roomVersion")

    ksp("androidx.room:room-compiler:$roomVersion")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$roomVersion")

    // optional - RxJava2 support for Room
    implementation("androidx.room:room-rxjava2:$roomVersion")

    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$roomVersion")

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation("androidx.room:room-guava:$roomVersion")

    // optional - Test helpers
    testImplementation("androidx.room:room-testing:$roomVersion")

    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$roomVersion")

    implementation(project(":hsktextviews"))
    implementation(project(":floatlayouts"))
    implementation(project(":googledrivebackup"))
    implementation(project(":AnkiDroidAPIHelper"))
    implementation(project(":crossPlatform"))

    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-video:${cameraxVersion}")

    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("androidx.camera:camera-extensions:${cameraxVersion}")

    // Text features
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    implementation("io.github.yrjyrj123:jieba-analysis:1.0.3")

    // Anki
    implementation("com.github.ankidroid:Anki-Android:api-v1.1.0")

    // Let's allow users to support the dev with money
    implementation("com.android.billingclient:billing:8.0.0")
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    // Let's adk the user for reviews
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")

    implementation("androidx.compose.ui:ui:1.9.1")
    implementation("androidx.compose.material3:material3:1.3.2")

    // Correctly import your crossPlatform module
    implementation(project(":crossPlatform"))

    // This is important for the app module to access the common resources
    // Make sure the version matches the one in your crossPlatform module
    implementation("network.chaintech:cmptoast:1.0.7")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.1")

    implementation(libs.kotlinx.datetime)
}

// TODO: Remove these unhappy hacks. I've search for a while and couldn't find why my resources aren't exported despite export configuration.
tasks.register<Delete>("cleanCopyCrossPlatformResources") {
    description = "Deletes the old cross-platform resources."
    delete("$rootDir/app/build/intermediates/assets/debug/mergeDebugAssets/composeResources/hskflashcardswidget.crossplatform.generated.resources")
}
val copyCrossPlatformResources = tasks.register<Copy>("copyCrossPlatformResources") {
    group = "build"
    description = "Copies my resources from CrossPlatform output to App input"

    val sourceDir = file("$rootDir/crossPlatform/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources")
    from(sourceDir)
    into("$rootDir/app/build/intermediates/assets/debug/mergeDebugAssets/composeResources/hskflashcardswidget.crossplatform.generated.resources")

    inputs.dir(sourceDir)
    dependsOn("cleanCopyCrossPlatformResources")
    dependsOn(project(":crossPlatform").tasks.named("prepareComposeResourcesTaskForCommonMain"))
    dependsOn(project(":crossPlatform").tasks.named("copyNonXmlValueResourcesForCommonMain"))
    dependsOn(project(":crossPlatform").tasks.named("convertXmlValueResourcesForCommonMain"))
}

tasks.named("preBuild").configure {
    dependsOn(":crossPlatform:convertXmlValueResourcesForCommonMain")
    dependsOn(copyCrossPlatformResources)
}


tasks.register<Delete>("cleanCopyHSKViewsResources") {
    description = "Deletes the old HSK Vews Resources resources."
    delete("$rootDir/app/build/intermediates/assets/debug/mergeDebugAssets/composeResources/hskflashcardswidget.hsktextviews.generated.resources")
}

val copyHSKViewsResources = tasks.register<Copy>("copyHSKViewsResources") {
    group = "build"
    description = "Copies my resources from CrossPlatform output to App input"

    val sourceDir = file("$rootDir/hsktextviews/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources")
    from(sourceDir)
    into("$rootDir/app/build/intermediates/assets/debug/mergeDebugAssets/composeResources/hskflashcardswidget.hsktextviews.generated.resources")

    inputs.dir(sourceDir) // <-- Gradle now knows this directory is an input
    dependsOn("cleanCopyHSKViewsResources")
    dependsOn(project(":hsktextviews").tasks.named("prepareComposeResourcesTaskForCommonMain"))
    dependsOn(project(":hsktextviews").tasks.named("copyNonXmlValueResourcesForCommonMain"))
    dependsOn(project(":hsktextviews").tasks.named("convertXmlValueResourcesForCommonMain"))
}

tasks.named("preBuild").configure {
    dependsOn(":hsktextviews:convertXmlValueResourcesForCommonMain")
    dependsOn(copyHSKViewsResources)
}