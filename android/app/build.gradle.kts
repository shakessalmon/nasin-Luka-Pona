plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.shakessalmon.nasinlukapona"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shakessalmon.nasinlukapona"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    // The keyboard reads the same data and font as the web and iOS versions.
    sourceSets.getByName("main").assets.srcDirs("../../data", "../../fonts")

    // A fixed debug key (public, debug builds only), so a newer build installs over an older one.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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
}
