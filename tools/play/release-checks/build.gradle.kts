import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val credentials = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
android {
    namespace = "io.github.hatake716.rocketboy.releasechecks"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.hatake716.rocketboy.releasechecks"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1"
    }
    if (credentials.isNotEmpty()) {
        signingConfigs.create("upload") {
            storeFile = rootProject.file(credentials.getProperty("storeFile"))
            storePassword = credentials.getProperty("storePassword")
            keyAlias = credentials.getProperty("keyAlias")
            keyPassword = credentials.getProperty("keyPassword")
        }
        buildTypes.getByName("debug").signingConfig = signingConfigs.getByName("upload")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation("androidx.test:runner:1.6.2")
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
