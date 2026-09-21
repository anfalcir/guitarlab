plugins { alias(libs.plugins.android.library) }
android { namespace="studio.guitarlab.platform.separation"; compileSdk=36
 defaultConfig { minSdk=26 }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
}
dependencies {
 implementation(project(":core:separation"))
 implementation(libs.kotlinx.coroutines.android)
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
 implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
 implementation("com.google.firebase:firebase-auth")
 implementation("com.google.firebase:firebase-firestore")
 implementation("com.google.firebase:firebase-functions")
 implementation("com.google.firebase:firebase-storage")
 implementation(libs.androidx.work.runtime.ktx)
 implementation(libs.kotlinx.serialization.json)
 implementation(project(":core:project"))
 implementation(project(":core:model"))
 testImplementation(libs.junit4)
}
