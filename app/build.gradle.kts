plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.posdemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.posdemo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.26020901"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(files("libs/urovoSdkLibs_New_v1.0.23_release.aar"))
    implementation("com.google.zxing:core:3.5.3") // For BarcodeFormat
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // For upload log
    implementation("androidx.fragment:fragment-ktx:1.6.2") // sharedViewModel for EMV
    implementation("com.hivemq:hivemq-mqtt-client:1.3.12") // For MQTT online Printer
    implementation("com.google.code.gson:gson:2.10.1") // For design of Blind PinPad
    implementation("org.java-websocket:Java-WebSocket:1.5.4") // For WebSocket Printer
}