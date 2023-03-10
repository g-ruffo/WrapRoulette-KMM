import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "ca.veltus.wraproulettekmm.android"
    compileSdk = 33
    defaultConfig {
        applicationId = "ca.veltus.wraproulettekmm.android"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        dataBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.0"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    // Allow references to generated code
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation(project(":shared"))

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.2")
    implementation("androidx.fragment:fragment:1.5.3")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("androidx.activity:activity-ktx:1.6.1")

    // Google Play Dependencies
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Firebase Dependencies
    implementation("com.google.firebase:firebase-firestore-ktx:24.4.1")
    implementation("com.google.firebase:firebase-auth-ktx:21.1.0")
    implementation("com.google.firebase:firebase-storage-ktx:20.1.0")
    implementation("com.firebaseui:firebase-ui-storage:8.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    implementation(platform("com.google.firebase:firebase-bom:31.1.1"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Glide Dependencies
    implementation("com.github.bumptech.glide:glide:4.14.2")
    kapt("com.github.bumptech.glide:compiler:4.14.2")

    // Lifecycle Dependencies
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Groupie Dependencies
    implementation("com.github.lisawray.groupie:groupie:2.10.1")
    implementation("com.github.lisawray.groupie:groupie-databinding:2.10.1")
    implementation("com.github.lisawray.groupie:groupie-kotlin-android-extensions:2.10.1")

    // Circle ImageView Dependency
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Unit Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.1")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("app.cash.turbine:turbine:0.12.1")
    testImplementation("org.robolectric:robolectric:4.9")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    implementation("androidx.test.ext:junit-ktx:1.1.4")
    testImplementation("com.google.dagger:hilt-android-testing:2.44")
    kaptTest("com.google.dagger:hilt-android-compiler:2.44")

    // Android Testing Dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.3.0")
    implementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.44")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.44")
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.3.0")

    // Hilt Dependencies
    implementation("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-android-compiler:2.44")
}