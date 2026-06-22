package com.example.bbd.data.remote

import com.example.bbd.data.remote.dto.MeDto
import retrofit2.http.GET

/**
 * 게이트웨이 인증 REST. `/api/auth/me` 는 **게이트웨이 루트** 직속(서비스 prefix 없음).
 * BASE_URL=게이트웨이 루트라 Retrofit 경로 = `api/auth/me`.
 *
 * 인증: Authorization: Bearer <JWT> — [Net] 의 Bearer 인터셉터가 자동 부착(로그인 시 AuthManager 주입).
 */
interface AuthApi {

    /** 현재 로그인 사용자 신원(인가값 아님 — role/지점/창고는 응답에 없음). */
    @GET("api/auth/me")
    suspend fun me(): MeDto
}
