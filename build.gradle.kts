// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.9.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false

    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.3" apply false

    id("com.google.devtools.ksp") version "2.0.20-1.0.24" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.9.0" apply false
    id("com.android.library") version "8.9.3" apply false
}