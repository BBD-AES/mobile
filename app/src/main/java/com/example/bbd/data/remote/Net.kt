package com.example.bbd.data.remote

import com.example.bbd.BuildConfig
import okhttp3.OkHttpClient
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
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(service: Class<T>): T = retrofit.create(service)
}
