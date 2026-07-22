plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.wren.ide"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wren.ide"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // Kotlin 1.9.22 (ver build.gradle.kts raíz) requiere Compose Compiler
        // 1.5.10 según el mapa oficial de compatibilidad de Google -- NO 1.5.8
        // (que es para Kotlin 1.9.21). Ese desfase de un patch es la causa real
        // del crash recurrente "NoSuchMethodError: KeyframesSpec$KeyframeEntity"
        // en CircularProgressIndicator: el compilador generaba código pensado
        // para una versión de Compose distinta a la que trae compose-bom.
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android & Lifecycle
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose UI
    // compose-bom 2024.01.00 tenía un bug REAL y documentado en el propio
    // material3 (reportado incluso en android/sunflower, el sample oficial
    // de Google): CircularProgressIndicator llamaba un metodo de
    // animation-core que esa vintage del BOM no incluía todavía, causando
    // NoSuchMethodError en KeyframesSpec. Se arregló upstream poco después
    // -- 2024.06.00 ya lo tiene resuelto. Esto, no la version del compilador,
    // era la causa real del crash recurrente.
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    // Ensure animation artifacts are included (fixes NoSuchMethodError from KeyframesSpec mismatch)
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Local Storage: Room & DataStore
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    // Kotlin annotation processor — required for Room to generate DAO/database
    // implementations for Kotlin sources. The previous "annotationProcessor"
    // (Java-only) silently generated nothing, which throws at runtime
    // ("cannot find implementation for WrenDatabase") the first time local
    // storage is touched.
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Networking: OkHttp & Gson
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Concurrency: Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
