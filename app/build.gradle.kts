
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.smarthealthreminder"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.smarthealthreminder"
        minSdk = 24
        targetSdk = 36
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



    buildFeatures {
        viewBinding = true
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.vectordrawable)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase
    implementation(libs.firebase.auth)
    implementation("com.google.android.gms:play-services-auth:21.6.0")

    // Room Database
    val roomVersion = "2.7.0"

    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // ViewModel + Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.40")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")


    implementation("androidx.fragment:fragment-ktx:1.8.9")

    // Koin
    implementation(libs.koin.android)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Core Retrofit library
    implementation("com.squareup.retrofit2:retrofit:3.0.0")

    // Converter factory (Choose one based on your data format)
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

}