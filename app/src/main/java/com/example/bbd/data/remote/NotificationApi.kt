package com.example.bbd.data.remote

import com.example.bbd.data.remote.dto.NotificationDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

/**
 * 지점 알림함 REST (sales). BASE_URL=게이트웨이 루트라 경로에 서비스 prefix(sales/) 포함.
 * 컨트롤러 @RequestMapping("/api/v1/notifications") · 게이트웨이 prefix /sales.
 * 스코프(지점 버킷)는 서버가 토큰 스냅샷으로 결정 — 클라가 파라미터를 보내지 않는다.
 */
interface NotificationApi {

    /** 알림함 — 호출자 버킷의 최근 100건(읽음·안읽음 모두). */
    @GET("sales/api/v1/notifications")
    suspend fun inbox(): List<NotificationDto>

    /** 읽음 처리 — 항목 탭 시. 버킷 스코프(서버), 없거나 타 버킷이면 204 no-op. */
    @PATCH("sales/api/v1/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): Response<Unit>
}
