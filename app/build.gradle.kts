import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Claves del backend: van en local.properties (no se sube a git).
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun localProp(name: String) = "\"${localProps.getProperty(name, "")}\""

android {
    namespace = "com.angel.fitgains"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.angel.fitgains"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "SUPABASE_URL", localProp("supabase.url"))
        buildConfigField("String", "SUPABASE_ANON_KEY", localProp("supabase.anonKey"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coil.compose)
    implementation(libs.code.scanner)

    testImplementation(libs.junit)
}
