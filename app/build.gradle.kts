plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.chatnote"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.chatnote"
        minSdk = 29
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
}

dependencies {
    // Firebase
    // Sử dụng Firebase BOM để tự động quản lý phiên bản các thư viện Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))

    // Firebase Authentication và các thư viện Firebase khác
    implementation("com.google.firebase:firebase-auth:21.0.1")
    implementation("com.google.firebase:firebase-core:21.0.0")
    implementation("com.google.firebase:firebase-firestore:24.0.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-messaging:23.0.8")



    // ... other dependencies
    // Google Auth
    implementation("com.google.android.gms:play-services-auth:20.6.0") // Đăng nhập bằng Google
    implementation("androidx.credentials:credentials:1.0.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.0.0")
    implementation("com.google.firebase:firebase-storage:20.2.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okio:okio:3.6.0")
    implementation("com.squareup.picasso:picasso:2.8")
}