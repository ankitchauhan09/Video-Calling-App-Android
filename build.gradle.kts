// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
}

buildscript{
    dependencies{
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51")
        classpath(libs.google.services)
    }
}