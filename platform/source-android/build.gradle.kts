plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "studio.guitarlab.platform.source.android"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:source"))
    implementation(project(":core:model"))
    implementation(project(":core:project"))
    implementation(project(":core:codec"))
    implementation(project(":platform:codec-android"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.youtube.dl.android)
}
