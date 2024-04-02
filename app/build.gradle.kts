    plugins {
        alias(libs.plugins.androidApplication)
        alias(libs.plugins.jetbrainsKotlinAndroid)
        id("kotlin-kapt")
        id("com.google.dagger.hilt.android")
        id("com.google.gms.google-services")
    }

    android {
        namespace = "com.example.webrtc2"
        compileSdk = 34

        defaultConfig {
            applicationId = "com.example.webrtc2"
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
        buildFeatures{
            viewBinding = true
        }
    }

    dependencies {

        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.firebase.database)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        implementation("com.google.dagger:hilt-android:2.51")
        kapt("com.google.dagger:hilt-compiler:2.51")
        implementation("com.guolindev.permissionx:permissionx:1.7.1")
        implementation("com.mesibo.api:webrtc:1.0.5")
        implementation("com.google.code.gson:gson:2.10.1")
    }