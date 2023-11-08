plugins {
    id("com.android.application")
}

android {
    androidResources {
        generateLocaleConfig = true
    }
    defaultConfig {
        resourceConfigurations += arrayOf("am", "af", "ar", "as", "az", "be", "bg", "bn", "bs", "ca", "cs", "da", "de", "el", "en-AU", "en-CA", "en-GB", "en-IN", "en-US", "en-XA", "es", "es-US", "et", "eu", "fa", "fi",
            "fr", "fr-CA", "gl", "gu", "hi", "hr", "hu", "hy", "in", "is", "it", "iw", "ja", "ka", "kk", "km", "kn", "ko", "ky", "lo", "lt", "lv", "mk", "ml", "mn", "mr", "ms", "my", "my-MM", "nb", "ne", "nl", "or", "pa", "pl",
            "pt-BR", "pt-PT", "ro", "ru", "si", "sk", "sl", "sq", "sr", "sr-Latn", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "ur", "uz", "vi", "zh-CN", "zh-HK", "zh-TW", "zu")
    }
    namespace = "com.example.braille"
    compileSdk = 33
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
    }
    defaultConfig {
        applicationId = "com.example.braille"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("net.zetetic:android-database-sqlcipher:4.4.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20231011-2.0.0")
}