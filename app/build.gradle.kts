import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Assinatura de release: se existir keystore/keystore.properties (fora do git),
// usa a keystore real; caso contrário o release cai na keystore de debug — assim
// o APK release já sai instalável sem nenhuma configuração. Rode `make keystore`
// para gerar uma keystore de release dedicada.
val keystorePropsFile = rootProject.file("keystore/keystore.properties")
val temKeystore = keystorePropsFile.exists()
val keystoreProps = Properties().apply {
    if (temKeystore) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "br.com.notes"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "br.com.notes"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (temKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: remove código morto + encolhe recursos (o ganho real do release).
            // Se adicionar libs que usam reflexão/serialização, mantenha as regras
            // de keep em proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Com keystore de release usa ela; sem ela, assina com a de debug para
            // o APK já sair instalável (sideload). Troque por uma keystore real
            // (make keystore) antes de publicar de verdade.
            signingConfig = if (temKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
}

dependencies {
    // Core compartilhado + uma feature por tela.
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":feature:notepad"))
    implementation(project(":feature:blocos"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.ui.tooling)
}

// Gera uma cópia do APK debug com a versão no nome, em <raiz>/dist/.
// Cada versão vira um arquivo (e uma URL) diferente — facilita a distribuição
// e evita que o navegador do celular sirva uma versão antiga do cache.
tasks.register<Copy>("distApk") {
    dependsOn("assembleDebug")
    val version = android.defaultConfig.versionName
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "notes-$version-debug.apk" }
    doLast { println(">> APK versionado em: dist/notes-$version-debug.apk") }
}

// Idem para o release (assinado + R8). Sai em dist/ com sufixo -release, então é
// listado pelo server.sh e instalável pelo adb.sh igual ao debug.
tasks.register<Copy>("distReleaseApk") {
    dependsOn("assembleRelease")
    val version = android.defaultConfig.versionName
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "notes-$version-release.apk" }
    doLast { println(">> APK versionado em: dist/notes-$version-release.apk") }
}
