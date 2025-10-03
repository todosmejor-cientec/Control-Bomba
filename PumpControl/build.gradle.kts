// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.hilt) apply false// ✅ Usar el alias aquí
    alias(libs.plugins.kotlinComposeCompiler) apply false // 🔥 Agrega esta línea

    alias(libs.plugins.kotlin.kapt) apply false
}