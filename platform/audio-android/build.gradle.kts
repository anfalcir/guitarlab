plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "studio.guitarlab.platform.audio.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:audio"))
    implementation(libs.kotlinx.coroutines.android)
}
