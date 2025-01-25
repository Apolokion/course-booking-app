// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.org.jetbrains.kotlin.kapt) apply false
    id("com.android.library") version "8.1.4" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false

}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }

    dependencies {
        classpath(libs.androidx.navigation.safe.args.gradle.plugin)
        classpath(libs.hilt.android.gradle.plugin)

    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.databinding:databinding-common:8.1.4")
    }
}


