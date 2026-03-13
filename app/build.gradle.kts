plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.posdemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.posdemo"
        minSdk = 30
        targetSdk = 33
        versionCode = 3
        versionName = "1.0.26022401"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.property("RELEASE_STORE_FILE") as String)
            storePassword = project.property("RELEASE_STORE_PASSWORD") as String
            keyAlias = project.property("RELEASE_KEY_ALIAS") as String
            keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("String", "MQTT_HOST", "\"39.101.193.145\"")
            buildConfigField("String", "LIST_OF_POS", "\"SQ68,SQ29M,SQ29MB,SQ29WR,SQ65A,SQ65B,\"")
            buildConfigField("String", "LIST_OF_PDA", "\"SQ53,SQ53ST,SQ66,SQ58C,SQ58CU,\"")
        }
        release {
            isMinifyEnabled = false // Minify the naming of the codes
            isShrinkResources = false // Delete all the codes that are not in use
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "MQTT_HOST", "\"39.101.193.145\"")
            buildConfigField("String", "LIST_OF_POS", "SQ68,SQ29M,SQ29MB,SQ29WR,SQ65A,SQ65B,")
            buildConfigField("String", "LIST_OF_PDA", "SQ53,SQ53ST,SQ66,SQ58C,SQ58CU,")
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
        buildConfig = true
        aidl = true
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

    implementation(files("libs/urovoSdkLibs_New_v1.0.23_release.aar")) // Urovo POS SDK
    implementation("com.google.zxing:core:3.5.3") // For BarcodeFormat
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // For HTTP
    implementation("androidx.fragment:fragment-ktx:1.6.2") // sharedViewModel for EMV
    implementation("com.hivemq:hivemq-mqtt-client:1.3.12") // For MQTT online Printer
    implementation("com.google.code.gson:gson:2.10.1") // For design of Blind PinPad
    implementation("org.java-websocket:Java-WebSocket:1.5.4") // For WebSocket Printer
    implementation("com.google.android.gms:play-services-location:21.0.1") // For google location
    implementation("com.baidu.lbsyun:BaiduMapSDK_Location:9.6.0") // For Baidu Location
    implementation("com.google.android.play:integrity:1.6.0") // For Test of Google Integrity API
    implementation("org.bouncycastle:bcprov-jdk15to18:1.78.1") // For getting Device Info
    implementation("com.google.code.gson:gson:2.10.1") // For parsing JSON

}