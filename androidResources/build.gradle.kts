plugins {
    id("com.android.library")
    kotlin("android") // needed for Kotlin support if you want optional Android code
}

android {
    namespace = "fr.berliat.hskwidget.androidResources"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // This is where you put your XML resources
    sourceSets["main"].res.srcDirs("src/main/res")
}

dependencies {
    // optional Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
}
