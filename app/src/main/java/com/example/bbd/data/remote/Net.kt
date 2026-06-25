package com.example.bbd.data.remote

import com.example.bbd.BuildConfig
import com.example.bbd.auth.AuthManager
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit/OkHttp 단일 설정.
 *
 * - base URL = [BuildConfig.BASE_URL] (게이트웨이 + 서비스 prefix, 예: http://10.0.2.2:8080/sales/).
 *   공개 레포이므로 host 는 코드에 하드코딩하지 않고 gradle property → BuildConfig 로만 주입.
 * - 인증 토큰은 [bearer] 로 주입. 실제 OIDC(Keycloak/AppAuth) 로그인 연동은 후속 PR.
 */
object Net {

    /** 로그인(OIDC) 후 주입할 액세스 토큰. null 이면 Authorization 헤더 미부착. */
    @Volatile
    var bearer: String? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                bearer?.let { builder.header("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            }
            // 세션 중 access token 만료(401) → 동기 refresh 후 1회 재시도. 유효한 refresh token 이 있으면
            // 사용자는 끊김 없이 이어진다. 무토큰/이미 재시도함/refresh 실패면 포기(401 그대로 전파 → 화면이 처리).
            .authenticator { _, response ->
                if (bearer == null) return@authenticator null
                if (responseCount(response) >= 2) return@authenticator null // 이미 1회 재시도 → 루프 방지
                val fresh = AuthManager.blockingFreshToken() ?: return@authenticator null
                response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                    )
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            // Retrofit 은 base URL 끝에 '/' 필수 — gradle property 로 들어온 값 정규화.
            .baseUrl(BuildConfig.BASE_URL.let { if (it.endsWith("/")) it else "$it/" })
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(service: Class<T>): T = retrofit.create(service)

    /** 현재 응답까지의 누적 응답 수(priorResponse 체인) — Authenticator 재시도 1회 제한 가드용. */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}
