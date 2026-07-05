plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "br.com.notes.feature.settings"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 26
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

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
}

dependencies {
    // :core:ui reexporta o BOM do Compose, o Material 3 e o :core:data (repositório).
    implementation(project(":core:ui"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose) // rememberLauncherForActivityResult (SAF)
    implementation(libs.androidx.documentfile) // acesso SAF a arquivos/pastas
    implementation(libs.androidx.work.runtime.ktx) // backup automático em segundo plano
    implementation(libs.kotlinx.coroutines.android)
}
