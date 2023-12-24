plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.example.googlemapsdemo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.googlemapsdemo"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    //Core
    implementation(libs.core.ktx)

    //Views
    implementation(libs.bundles.views)

    //Activity
    implementation(libs.activity.ktx)

    //Lifecycle
    implementation(libs.bundles.lifecycle)

    //Hilt Android
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    //Kotlin Coroutines
    implementation(libs.bundles.coroutines)

    //Maps
    implementation(libs.bundles.maps)
}