plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
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
        versionCode = 3
        versionName = "1.2"

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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    
    // Navigation
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    
    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    
    // WorkManager
    implementation(libs.work.runtime.ktx)
    
    // Fragment
    implementation(libs.fragment.ktx)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // RecyclerView
    implementation(libs.androidx.recyclerview)
    
    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
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
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}