plugins {
    id("com.android.application")
}

android {
    namespace = "com.affixstudio.calculator.locker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.affixstudio.calculator.locker"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"



    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.airbnb.android:lottie:3.4.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")// image loader

    implementation("net.zetetic:android-database-sqlcipher:4.4.3")//secure db
    implementation ("com.github.stfalcon-studio:StfalconImageViewer:v1.0.1") // image viewer
    implementation ("com.google.android.exoplayer:exoplayer:2.19.1") // video viewer
    implementation ("com.github.JarvanMo:ExoVideoView:2.1.6") // video viewer
    implementation ("net.objecthunter:exp4j:0.4.8") // calculator


    implementation ("com.applovin:applovin-sdk:11.10.1")  // ads
    implementation ("com.applovin.mediation:facebook-adapter:6.14.0.0") // facebook ads
    implementation ("com.applovin.mediation:unityads-adapter:4.7.1.0") // unity ads
    implementation ("com.facebook.android:audience-network-sdk:6.14.0") // facebook ads
    implementation ("com.google.android.gms:play-services-ads-identifier:18.0.1")

    implementation("com.google.android.play:app-update:2.1.0")

}