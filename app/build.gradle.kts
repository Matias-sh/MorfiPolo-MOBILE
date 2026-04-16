plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

// Función auxiliar para cargar propiedades del keystore
fun loadKeystoreProperties(): Map<String, String>? {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (!keystorePropertiesFile.exists()) {
        return null
    }
    val props = mutableMapOf<String, String>()
    keystorePropertiesFile.readLines().forEach { line ->
        if (line.isNotBlank() && !line.trimStart().startsWith("#")) {
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                props[parts[0].trim()] = parts[1].trim()
            }
        }
    }
    return if (props.isEmpty()) null else props
}

android {
    namespace = "com.cocido.morfipolo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cocido.morfipolo"
        minSdk = 28
        targetSdk = 36
        versionCode = 6
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            loadKeystoreProperties()?.let { props ->
                keyAlias = props["keyAlias"] ?: ""
                keyPassword = props["keyPassword"] ?: ""
                // El archivo está en la raíz del proyecto, no en app/
                storeFile = rootProject.file(props["storeFile"] ?: "")
                storePassword = props["storePassword"] ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
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
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.5"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    
    
    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    
    // WorkManager
    implementation(libs.work.runtime.ktx)
    
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    
    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    kapt(libs.moshi.kapt)
    
    // Gson (para serialización de notificaciones personalizadas)
    implementation("com.google.code.gson:gson:2.10.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}