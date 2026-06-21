import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// API 연동 설정 — gradle property 로 주입(공개 레포라 host 하드코딩 회피).
// 예: ./gradlew assembleDebug -PBBD_BASE_URL="http://10.0.2.2:8080/sales/" -PBBD_USE_API=true
val bbdBaseUrl = (project.findProperty("BBD_BASE_URL") as? String) ?: "http://10.0.2.2:8080/sales/"
// USE_API 는 boolean BuildConfig 필드 → 반드시 "true"/"false" 리터럴로 정규화(임의 문자열 codegen 깨짐 방지).
val bbdUseApi = ((project.findProperty("BBD_USE_API") as? String)?.toBoolean() ?: false).toString()

android {
    namespace = "com.example.bbd"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.bbd"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"$bbdBaseUrl\"")
        buildConfigField("boolean", "USE_API", bbdUseApi)
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // 네트워크 / API 연동 (sales 데이터 레이어)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}