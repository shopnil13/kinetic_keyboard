import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Secrets stay out of git: local.properties (gitignored) first, environment second.
val localProps: Properties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use(::load)
}

fun secret(prop: String, env: String): String? =
    localProps.getProperty(prop)?.takeIf { it.isNotBlank() }
        ?: System.getenv(env)?.takeIf { it.isNotBlank() }

// P5.10: GIPHY API key — `giphy.apiKey=YOUR_KEY` in local.properties, or GIPHY_API_KEY env.
val giphyApiKey: String = secret("giphy.apiKey", "GIPHY_API_KEY") ?: ""

// P6.6: release signing. Provide all four to sign; omit them and the release build stays
// unsigned (what CI does — R8 still runs). local.properties keys / env vars:
//   release.storeFile     / RELEASE_STORE_FILE      (absolute path, or relative to app/)
//   release.storePassword / RELEASE_STORE_PASSWORD
//   release.keyAlias      / RELEASE_KEY_ALIAS
//   release.keyPassword   / RELEASE_KEY_PASSWORD
val releaseStoreFile = secret("release.storeFile", "RELEASE_STORE_FILE")
val releaseStorePassword = secret("release.storePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = secret("release.keyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = secret("release.keyPassword", "RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { it != null }

android {
    namespace = "com.kinetic.keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kinetic.keyboard"
        minSdk = 28
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GIPHY_API_KEY", "\"$giphyApiKey\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            // P6.6: minified + resource-shrunk release; keep rules live in proguard-rules.pro.
            // Signed only when the owner's keystore is configured (see `secret` above); CI and
            // keystore-less local builds still verify R8 on the unsigned artifact.
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.datastore.preferences)
    // P5.5 rev2: AndroidX emoji picker + EmojiCompat with the bundled font, so every emoji
    // renders correctly on any device with zero network (keeps the PRIVACY.md guarantee).
    implementation(libs.androidx.emoji2)
    implementation(libs.androidx.emoji2.bundled)
    implementation(libs.androidx.emoji2.emojipicker)
    // P5.10: GIF/sticker panel — Coil renders GIPHY previews (animated GIF/WebP).
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
}
