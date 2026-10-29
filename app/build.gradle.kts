import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Keep the upload key and its passwords out of version control.
val releaseSigningFile = rootProject.file("keystore.properties")
val releaseSigning = Properties().apply {
    if (releaseSigningFile.isFile) releaseSigningFile.inputStream().use { load(it) }
}

android {
    namespace = "io.github.hatake716.rocketboy"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.hatake716.rocketboy"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    if (releaseSigningFile.isFile) signingConfigs.create("upload") {
        storeFile = rootProject.file(requireNotNull(releaseSigning.getProperty("storeFile")))
        storePassword = requireNotNull(releaseSigning.getProperty("storePassword"))
        keyAlias = requireNotNull(releaseSigning.getProperty("keyAlias"))
        keyPassword = requireNotNull(releaseSigning.getProperty("keyPassword"))
    }
    buildTypes {
        release {
            if (releaseSigningFile.isFile) signingConfig = signingConfigs.getByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { animationsDisabled = true }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-ktx:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
