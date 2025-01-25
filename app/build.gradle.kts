plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21"
    id("kotlin-parcelize")
}

android {
    namespace = "biz.pock.coursebookingapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "biz.pock.coursebookingapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        dataBinding = true
    }

    defaultConfig {
        resourceConfigurations += "de"

    }


}

// Kapt Konfiguration
kapt {
    correctErrorTypes = true
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Retrofit
    implementation (libs.retrofit2.retrofit)
    implementation (libs.converter.gson)

    // Timber Logging
    implementation(libs.timber)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Co-Routines für URL Fetch
    implementation(libs.kotlinx.coroutines.android)

    // Picasso für Bildverarbeitung
    implementation(libs.picasso)


    // Hilt
    //implementation(libs.hilt.android)
    //kapt(libs.hilt.android.compiler)

    implementation(libs.hilt.android.v2511)
    kapt(libs.hilt.android.compiler.v2511)

    // Google Gson -> JSON
    implementation(libs.gson)

    // Erzwinge eine spezifische Version von Guava und schließe listenablefuture aus
    // weil es zu Konflikten gekommen ist mit einem anderen Import, vermutlich wegen
    // dem Slider Widget
    implementation("com.google.guava:guava:31.1-android") {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    configurations.all {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // OK HTTP
    implementation(libs.logging.interceptor)

    //ThreeTen ABP
    implementation(libs.threetenabp)

    // Schöner Dialog -> AlertDialog
    implementation("com.github.tapadoo:alerter:7.2.4")

    // PDF Viewer für Rechnungen
    implementation("com.github.afreakyelf:Pdf-Viewer:2.0.4")

}