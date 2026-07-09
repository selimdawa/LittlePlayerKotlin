plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.daggerHiltAndroid)
    alias(libs.plugins.ksp.processor)
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace = "com.flatcode.littleplayer"
    compileSdk  = 37

    defaultConfig {
        applicationId = "com.flatcode.littleplayer"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.01"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //Layout
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.google.flexbox)                    //Layout Fix Size
    //Image
    implementation(libs.coil)                              //Coil Image
    implementation(libs.airbnb.lottie)                     //Animation
    implementation(libs.androidx.palette.ktx)              //Background Color
    implementation(libs.zetbaitsu.compressor)              //Image Compressor
    //Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    //Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    //Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    //Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    //Navigation
    implementation(libs.navigation.fragment.ktx)                 //Need New Style |Swapping Fragments|
    implementation(libs.navigation.ui.ktx)
    implementation(libs.hilt.navigation.fragment)
    //Media Player
    implementation(libs.androidx.media)
    implementation(libs.androidx.media3.exoplayer)
    //Audio Waves
    implementation(libs.lincollincol.amplituda)
    implementation(libs.waveformseekbar)
    implementation(libs.multiwaveheader)
    //Other's
    implementation(libs.androidx.datastore.preferences)    //New Preference
    implementation(libs.zhanghai.fastscroll)                     //Need New Style |From| Drawable
    //Memory Leaks
    //debugImplementation(libs.squareup.leakcanary.android)
}