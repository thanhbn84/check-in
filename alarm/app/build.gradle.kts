plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.alarm"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.alarm"
        minSdk = 23
        targetSdk = 34
        versionCode = 9
        versionName = "1.2.5"
    }

    signingConfigs {
        create("stableDebug") {
            storeFile = file("../.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
    }

}

