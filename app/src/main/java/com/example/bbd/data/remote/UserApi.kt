package com.example.bbd.data.remote

import com.example.bbd.data.remote.dto.UserSnapshotDto
import retrofit2.http.GET

/**
 * user-service REST. `/user/api/v1/users/me` 는 게이트웨이 `/user` prefix 로 노출.
 * BASE_URL=게이트웨이 루트라 Retrofit 경로 = `user/api/v1/users/me`(서비스 prefix `user/` 포함).
 *
 * 인증: Authorization: Bearer <JWT> — [Net] 의 Bearer 인터셉터가 자동 부착. any authenticated 접근.
 */
interface UserApi {

    /** 현재 사용자 스냅샷 — 권위 role + 지점명(tenancyName) 포함(창고는 없음). */
    @GET("user/api/v1/users/me")
    suspend fun me(): UserSnapshotDto
}
