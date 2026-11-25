plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.lduarte1311.calsyncalarm"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lduarte1311.calsyncalarm"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "2.0"
    }

    // Assinatura da release: caminho e palavra-passe vêm de variáveis de ambiente
    // (CALSYNC_KEYSTORE, CALSYNC_KEYSTORE_PASS, opcional CALSYNC_KEY_ALIAS); nada de segredos no código.
    val keystorePath = System.getenv("CALSYNC_KEYSTORE")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("CALSYNC_KEYSTORE_PASS")
                keyAlias = System.getenv("CALSYNC_KEY_ALIAS") ?: "calsyncalarm"
                keyPassword = System.getenv("CALSYNC_KEYSTORE_PASS")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.work.runtime.ktx)

    debugImplementation(libs.androidx.ui.tooling)
}
