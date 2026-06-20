import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.isFile) {
        releaseKeystorePropertiesFile.inputStream().use(::load)
    }
}
val isReleaseBuildRequested = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

if (isReleaseBuildRequested && !releaseKeystorePropertiesFile.isFile) {
    throw GradleException(
        "Release signing is not configured. Run scripts/generate-release-keystore.ps1 " +
            "to create a local keystore.properties file, then retry this task."
    )
}

fun releaseKeystoreProperty(name: String): String? =
    releaseKeystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }

android {
    namespace = "io.github.ak65477.kanjitrainer"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.ak65477.kanjitrainer"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (releaseKeystorePropertiesFile.isFile) {
            create("release") {
                storeFile = file(
                    releaseKeystoreProperty("storeFile")
                        ?: throw GradleException("Missing storeFile in keystore.properties")
                )
                storePassword = releaseKeystoreProperty("storePassword")
                    ?: throw GradleException("Missing storePassword in keystore.properties")
                keyAlias = releaseKeystoreProperty("keyAlias")
                    ?: throw GradleException("Missing keyAlias in keystore.properties")
                keyPassword = releaseKeystoreProperty("keyPassword")
                    ?: throw GradleException("Missing keyPassword in keystore.properties")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            if (releaseKeystorePropertiesFile.isFile) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:srs"))
    implementation(project(":core:seed-importer"))
    implementation(project(":feature:kanji-srs"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
