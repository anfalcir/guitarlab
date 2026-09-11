plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "studio.guitarlab.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "studio.guitarlab.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "0.4.0-rc2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // CI-safe signing: secrets are provided only at build time and never committed.
    val homologationKeystorePath = System.getenv("GUITARLAB_KEYSTORE_PATH")
    val homologationStorePassword = System.getenv("GUITARLAB_KEYSTORE_PASSWORD")
    val homologationKeyAlias = System.getenv("GUITARLAB_KEY_ALIAS")
    val homologationKeyPassword = System.getenv("GUITARLAB_KEY_PASSWORD")
    val hasHomologationSigning = listOf(
        homologationKeystorePath,
        homologationStorePassword,
        homologationKeyAlias,
        homologationKeyPassword,
    ).all { !it.isNullOrBlank() }

    if (hasHomologationSigning) {
        signingConfigs {
            create("homologation") {
                storeFile = file(homologationKeystorePath!!)
                storePassword = homologationStorePassword
                keyAlias = homologationKeyAlias
                keyPassword = homologationKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (hasHomologationSigning) {
                signingConfig = signingConfigs.getByName("homologation")
            }
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:audio"))
    implementation(project(":platform:audio-android"))
    implementation(project(":core:project"))
    implementation(project(":core:codec"))
    implementation(project(":platform:codec-android"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)

    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
    debugImplementation(libs.compose.ui.test.manifest)
}
