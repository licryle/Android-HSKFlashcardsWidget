// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false

    id("com.google.gms.google-services") version "4.3.15" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false

    id("com.google.devtools.ksp") version "2.0.20-1.0.24" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.4" apply false
    id("com.android.library") version "8.6.1" apply false
}