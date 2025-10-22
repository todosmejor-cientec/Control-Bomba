plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.hilt) // ✅ Usar el alias aquí
    alias(libs.plugins.kotlinComposeCompiler)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.example.pumpcontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pumpcontrol"
        minSdk = 24
        targetSdk = 35
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}



dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.activity)
    implementation(libs.play.services.auth)
    implementation(libs.multidex)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.realtime)
    implementation(libs.firebase.config)

    implementation(libs.coil)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    //implementation(libs.androidx.compiler)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.hilt.android)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.credentials)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.commons.io)
    implementation(libs.material3)

    implementation(libs.firebase.firestore.ktx)

// Jetpack Paging para Compose + Kotlin Flow
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.ui.test.android)
    implementation(libs.espresso.core)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    kapt(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
