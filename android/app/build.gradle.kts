plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.threadboardlearn.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.threadboardlearn.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.5.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            // Reutiliza directamente el código del modelo, motor y DSP del mismo repositorio
            java.srcDirs(
                "src/main/java",
                "../../src/main/java"
            )
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    exclude("com/threadboardlearn/ui/**")
    exclude("com/threadboardlearn/App.java")
    exclude("com/threadboardlearn/audio/AudioPlayer.java")
    exclude("com/threadboardlearn/audio/MicrophoneCapture.java")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    exclude("com/threadboardlearn/ui/**")
    exclude("com/threadboardlearn/App.java")
    exclude("com/threadboardlearn/audio/AudioPlayer.java")
    exclude("com/threadboardlearn/audio/MicrophoneCapture.java")
}

tasks.register<Wrapper>("wrapper") {
    gradleVersion = "8.7"
    validateDistributionUrl = true
}

dependencies {
    // Procesamiento de anotaciones Lombok para las clases compartidas
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // AndroidX & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Jetpack Compose & Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.androidx.ui.tooling)
}
