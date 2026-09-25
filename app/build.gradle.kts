plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.miles.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.miles.wear"
        minSdk = 30
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Wear Compose
    implementation("androidx.wear.compose:compose-material3:1.0.0-alpha30")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear.compose:compose-navigation:1.4.0")
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear:wear-ongoing:1.0.0")

    // OpenStreetMap map rendering (no Google Maps SDK)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Wear Tiles & Complications
    implementation("androidx.wear.tiles:tiles:1.4.0")
    implementation("androidx.wear.protolayout:protolayout:1.2.0")
    implementation("androidx.wear.protolayout:protolayout-material:1.2.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1")

    // NOTE: no Google Play Services. Phone sync is GMS-free over the local
    // network (LanTransport: UDP discovery + TCP JSON frames).

    // Health Services
    implementation("androidx.health:health-services-client:1.1.0-alpha04")

    // Compose Foundation & UI
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Guava & Futures for TileService
    implementation("com.google.guava:guava:33.3.1-android")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
