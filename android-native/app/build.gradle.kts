plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.gms.google-services") apply false }
// The separate download test app is not registered with Firebase.
if (!project.hasProperty("downloadTest")) { apply(plugin = "com.google.gms.google-services") }
android {
    namespace = "online.mpscan.app"
    compileSdk = 35
    defaultConfig { applicationId = "online.mpscan.app"; minSdk = 24; targetSdk = 35; versionCode = 35; versionName = "5.0.0"; manifestPlaceholders["appLabel"] = "MP SCAN"
        if (project.hasProperty("downloadTest")) { applicationIdSuffix = ".downloadtest"; manifestPlaceholders["appLabel"] = "MP SCAN Teste" } }
    defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    buildTypes { getByName("release") { isDebuggable = false; isMinifyEnabled = false } }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.compose.material:material-icons-core")
    if (!project.hasProperty("downloadTest")) {
        implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
        implementation("com.google.firebase:firebase-analytics")
    }
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
