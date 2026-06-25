import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// API 연동 설정 — gradle property 로 주입(공개 레포라 host 하드코딩 회피).
// BASE_URL = 게이트웨이 루트(서비스 prefix sales/·inventory/ 는 각 Api 경로에 포함).
// ★ live(운영) 플레이버는 BBD_LIVE_BASE_URL(기본 운영 도메인)만 본다 — IDE Run·기본 빌드(-P 없음)가
//   ~/.gradle 의 BBD_BASE_URL(개발 Tailscale IP 등)에 오염돼 실기기에서 로그인 불가가 되는 함정을 차단.
//   live 를 굳이 dev 게이트웨이로 빌드하려면 -PBBD_LIVE_BASE_URL=http://… 로 명시 오버라이드.
// BBD_BASE_URL 은 defaultConfig(=USE_API false 인 demo/기본 빌드)용 — demo 는 네트워크 미사용이라 무해.
val bbdBaseUrl = (project.findProperty("BBD_BASE_URL") as? String) ?: "https://bbd.inwoohub.com/"
val liveBaseUrl = (project.findProperty("BBD_LIVE_BASE_URL") as? String) ?: "https://bbd.inwoohub.com/"
// USE_API 는 boolean BuildConfig 필드 → 반드시 "true"/"false" 리터럴로 정규화(임의 문자열 codegen 깨짐 방지).
val bbdUseApi = ((project.findProperty("BBD_USE_API") as? String)?.toBoolean() ?: false).toString()

// Keycloak OIDC 설정(운영팀 제공값 기본, gradle property 로 오버라이드 가능). client-secret 없음(public/PKCE).
val authIssuer = (project.findProperty("BBD_AUTH_ISSUER") as? String) ?: "https://bbd-keycloak.inwoohub.com/auth/realms/bbd"
val authClientId = (project.findProperty("BBD_AUTH_CLIENT_ID") as? String) ?: "bbd-mobile-android"
val authRedirect = (project.findProperty("BBD_AUTH_REDIRECT") as? String) ?: "com.bbd.mobile:/oauth2redirect"
val authEndSession = (project.findProperty("BBD_AUTH_END_SESSION_REDIRECT") as? String) ?: "com.bbd.mobile:/logout"
val authScopes = (project.findProperty("BBD_AUTH_SCOPES") as? String) ?: "openid profile email bbd-claims"
val authRedirectScheme = (project.findProperty("BBD_AUTH_REDIRECT_SCHEME") as? String) ?: "com.bbd.mobile"

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

        buildConfigField("String", "AUTH_ISSUER", "\"$authIssuer\"")
        buildConfigField("String", "AUTH_CLIENT_ID", "\"$authClientId\"")
        buildConfigField("String", "AUTH_REDIRECT", "\"$authRedirect\"")
        buildConfigField("String", "AUTH_END_SESSION_REDIRECT", "\"$authEndSession\"")
        buildConfigField("String", "AUTH_SCOPES", "\"$authScopes\"")
        // AppAuth RedirectUriReceiverActivity 가 이 scheme 으로 리다이렉트를 받음(redirect URI 의 scheme 과 일치).
        manifestPlaceholders["appAuthRedirectScheme"] = authRedirectScheme
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

    // 데모(시드)↔라이브(실 게이트웨이) 토글 — AS 'Build Variants' 드롭다운에서 demoDebug↔liveDebug 선택만(파일 수정 불필요).
    // demo = USE_API false(번들 시드 카탈로그). live = USE_API true + BASE_URL 운영 고정(liveBaseUrl, 기본 https://bbd.inwoohub.com/).
    flavorDimensions += "backend"
    productFlavors {
        create("demo") {
            dimension = "backend"
            buildConfigField("boolean", "USE_API", "false")
        }
        create("live") {
            dimension = "backend"
            buildConfigField("boolean", "USE_API", "true")
            // ★운영 URL 고정 — defaultConfig 의 BBD_BASE_URL(~/.gradle 오염 가능)을 덮어써 기본/IDE Run 이 항상 운영을 향하게.
            buildConfigField("String", "BASE_URL", "\"$liveBaseUrl\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time(LocalDate 등)을 minSdk 24(<26)에서 쓰기 위한 core library desugaring.
        isCoreLibraryDesugaringEnabled = true
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

    // 인증 (Keycloak OIDC · AppAuth)
    implementation(libs.appauth)

    // 바코드/QR 스캔 (ZXing)
    implementation(libs.zxing.embedded)

    // java.time 백포트 (minSdk 24 에서 LocalDate 등 사용 — coreLibraryDesugaring)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}