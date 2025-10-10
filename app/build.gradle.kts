import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id ("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.compose.compiler)

}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}


android {
    namespace = "com.kotodama.tts"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kotodama.tts"
        minSdk = 24
        targetSdk = 35
        versionCode = 52
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en", "ar-rAE", "de",  "es", "fr", "in",
            "ja", "ko", "pt-rBR",
            "ru",  "tr",  "vi", "zh-rCN","zh-rSG")

        defaultConfig {
            manifestPlaceholders["REVENEUCAT_KEY"] = localProperties["REVENEUCAT_KEY"] ?: ""
        }
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
        //noinspection DataBindingWithoutKapt
        dataBinding = true
        buildConfig = true
        compose = true
    }

    bundle {
        language{
            enableSplit =false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.androidx.datastore.preferences.core.jvm)
    implementation(libs.androidx.ui.android)
    implementation(libs.ads.mobile.sdk)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")
    // Glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation ("it.xabaras.android:recyclerview-swipedecorator:1.4")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-functions")

    //reveneu cat
    implementation ("com.revenuecat.purchases:purchases:8.17.1")
    implementation ("com.revenuecat.purchases:purchases-ui:8.17.1")

    //compose view
    implementation ("androidx.compose.ui:ui-viewbinding:1.7.8")

    // Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.5.0")

    //language
    implementation ("com.google.mlkit:language-id:17.0.3")

    //facebook
    implementation ("com.facebook.android:facebook-android-sdk:17.0.1")

    //appsflyer
    implementation ("com.appsflyer:af-android-sdk:6.15.2")

    //in-app reviews
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")

    //lotie
    implementation ("com.airbnb.android:lottie:6.4.0")

    implementation ("com.android.billingclient:billing:6.1.0")

    implementation ("androidx.compose.ui:ui:1.7.8")
    implementation ("androidx.compose.material:material:1.7.8")
    implementation ("androidx.compose.ui:ui-tooling:1.7.8")
    implementation("androidx.compose.runtime:runtime:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.activity:activity-compose:1.10.1") // Bu sürümü güncelleyin
    // Compose View
    implementation ("androidx.compose.ui:ui-viewbinding:1.7.8")

    implementation("androidx.compose.foundation:foundation:1.7.8")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.8")


}