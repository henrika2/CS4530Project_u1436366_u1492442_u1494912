plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.paintify"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.paintify"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/LICENSE.md"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runner)
    implementation(libs.androidx.navigation.testing)
    implementation(libs.androidx.navigation.common.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.0")
    implementation("com.google.android.gms:play-services-basement:18.4.0")
    androidTestImplementation("io.mockk:mockk-android:1.13.12")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines on Android (needed for viewModelScope/Dispatchers, etc.)
    implementation(libs.kotlinx.coroutines.android)


    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:<ver>")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:<ver>")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:<ver>")
    implementation("androidx.compose.material3:material3:<ver>")

    implementation("androidx.navigation:navigation-compose:2.7.7") // or your latest

    implementation("androidx.compose.material:material-icons-extended:<compose_version>")
    implementation("androidx.activity:activity-compose:1.9.3") // for Photo Picker launcher APIs
    implementation("androidx.compose.material:material-icons-extended:<compose_version>")


}
