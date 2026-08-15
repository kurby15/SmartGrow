import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.smartgrow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smartgrow"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load API keys from local.properties safely
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }

        // API Key Configurations
        val geminiApiKey = properties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        val openRouterApiKey = properties.getProperty("OPENROUTER_API_KEY") ?: ""
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")

        val xaiApiKey = properties.getProperty("XAI_API_KEY") ?: ""
        buildConfigField("String", "XAI_API_KEY", "\"$xaiApiKey\"")

        val sambanovaApiKey = properties.getProperty("SAMBANOVA_API_KEY") ?: ""
        buildConfigField("String", "SAMBANOVA_API_KEY", "\"$sambanovaApiKey\"")

        // Map API Key
        val mapsApiKey = properties.getProperty("MAPS_API_KEY") ?: ""
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")

        // Injects MAPS_API_KEY into AndroidManifest.xml
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
    }

    buildFeatures {
        buildConfig = true
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Standard Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.play.services.location)
    implementation(libs.androidx.security.crypto)

    // Firebase (Using BoM for version management)
    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
    implementation("com.google.firebase:firebase-auth")      // Firebase Authentication
    implementation("com.google.firebase:firebase-firestore") // Cloud Firestore
    implementation(libs.firebase.database)                  // Realtime Database
    implementation(libs.firebase.storage)                   // Firebase Storage

    // OpenStreetMap (osmdroid)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation(libs.play.services.maps)

    // CameraX Integration
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.guava:guava:31.1-android")

    // Email
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    // Glide (Image Loading) + SVG Support for Distribution Maps
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.caverock:androidsvg-aar:1.4")

    // OkHttp + Logging
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}