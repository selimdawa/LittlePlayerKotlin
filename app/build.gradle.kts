plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.daggerHiltAndroid)
    alias(libs.plugins.ksp.processor)
}

android {
    namespace = "com.flatcode.littleplayer"
    compileSdk  = 37

    defaultConfig {
        applicationId = "com.flatcode.littleplayer"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
    //implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference.ktx)           //Shared Preference
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //Layout
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    //Image
    implementation(libs.circleimageview)                   //Circle Image
    implementation(libs.coil)                              //Coil Image
    //Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    //Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    //Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    //Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    //Other's
    implementation(libs.androidx.media)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.airbnb.lottie)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.jakewharton.timber)
    debugImplementation(libs.squareup.leakcanary.android)

    implementation(libs.jp.wasabeef.blurry)
    implementation(libs.zhanghai.fastscroll)
    implementation(libs.androidx.media3.exoplayer)

    implementation(libs.facebook.shimmer)
    implementation(libs.bogerchan.niervisualizer)
    implementation(libs.google.flexbox)

    implementation(libs.lincollincol.amplituda)
    implementation(libs.chrisbanes.haze)
    implementation(libs.zetbaitsu.compressor)

    implementation(libs.waveformseekbar)
}