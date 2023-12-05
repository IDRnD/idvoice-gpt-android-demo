import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("org.jlleitschuh.gradle.ktlint")
}

android {

    namespace = "net.idrnd.idvoicegpt"
    compileSdk = 33

    defaultConfig {
        applicationId = "net.idrnd.idvoicegpt"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        gradleLocalProperties(rootDir).getProperty("OPEN_AI_KEY")?.apply {
            buildConfigField("String", "OPEN_AI_KEY", this)
        }
        gradleLocalProperties(rootDir).getProperty("ID_VOICE_LICENSE")?.apply {
            buildConfigField("String", "ID_VOICE_LICENSE", this)
        }
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

    flavorDimensions += "version"

    productFlavors {
        create("idrnd") {
            dimension = "version"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures.buildConfig = true

    packaging {
        resources.excludes.add("META-INF/*")
    }

    configurations {
        all {
            /**
             * Lint found fatal errors while assembling a release target
             * Error: commons-logging defines classes that conflict with classes now provided by Android.
             * Error: httpclient defines classes that conflict with classes now provided by Android.
             *
             *  Explanation for issues of type "DuplicatePlatformClasses":
             *    There are a number of libraries that duplicate not just functionality of
             *    the Android platform but using the exact same class names as the ones
             *    provided in Android -- for example the apache http classes. This can lead
             *    to unexpected crashes.
             **/
            exclude(group = "org.apache.httpcomponents", module = "httpclient")
        }
    }
}

dependencies {

    // IDVoice SDK.
    implementation(files("libs/voicesdk-aar-full-release.aar"))

    // Google Cloud Speech to text.
    implementation("com.google.api.grpc:grpc-google-cloud-speech-v1:1.23.0")

    // GRPC.
    implementation("io.grpc:grpc-okhttp:1.38.1")

    // OAuth2 for Google API.
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    // OpenAI.
    implementation("com.aallam.openai:openai-client:3.5.1")
    implementation("io.ktor:ktor-client-android:2.3.5")

    // UI.
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")

    // Tests.
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
