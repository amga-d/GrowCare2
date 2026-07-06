import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.growCare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.growCare"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Load properties from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        // Gemini API Key from local.properties
        val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        // Weather API Key from local.properties
        val weatherApiKey = localProperties.getProperty("WEATHER_API_KEY") ?: ""
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // litertlm-android:0.13.1 was compiled with Kotlin 2.3.0 metadata.
        // This flag suppresses the version mismatch error without changing
        // how our own code is compiled — it only affects reading the library's
        // metadata at compile time.
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Hilt - Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    

    // LiteRT (modern TFLite runtime — GPU delegate, replaces org.tensorflow:tensorflow-lite)
    implementation(libs.litert.core)
    implementation(libs.litert.gpu)
    implementation(libs.litert.support)
    
    // LiteRT-LM (Gemma 4 on-device inference — replaces deprecated MediaPipe LLM)
    implementation(libs.litertlm.android)
    
    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // DataStore Preferences
    implementation(libs.datastore.preferences)
    
    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    
    // CameraX
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    
    // Coil - Image Loading
    implementation(libs.coil.compose)
    
    // Markdown Rendering
    implementation(libs.compose.markdown)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.play.services)
    
    // Google Play Services - Location
    implementation(libs.play.services.location)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Configure Java toolchain for compatibility
kotlin {
    jvmToolchain(17)
}