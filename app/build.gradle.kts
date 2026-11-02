plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.aospmodernqs"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.aospmodernqs"
        minSdk = 31          // Material You / cores dinâmicas / blur de janela exigem Android 12+
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Sem minify: o LSPosed localiza as classes por nome (xposed_init) e o módulo se auto-detecta.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // compileOnly: a API do Xposed é fornecida pelo LSPosed em tempo de execução.
    compileOnly("de.robv.android.xposed:api:82")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.android.material:material:1.12.0")
}
