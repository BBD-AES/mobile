package com.example.bbd.data.remote.dto

/**
 * 지점 알림함 1건 — sales `GET /api/v1/notifications` 응답(엔티티 직렬화).
 * 필드명 = Notification 게터명. createdAt 은 ISO-8601 문자열(Instant 직렬화).
 * inbox 는 최근 100건(읽음·안읽음 모두) 반환 → `read` 로 미읽음/읽음 구분 표시.
 */
data class NotificationDto(
    val id: Long? = null,
    val targetRole: String? = null,   // "HQ_MANAGER" 또는 지점 창고명(이름축)
    val soNumber: String? = null,     // 연계 이동요청 번호(큐 점프 키)
    val message: String? = null,      // 화면 표시 본문(별도 title 없음)
    val read: Boolean = false,
    val createdAt: String? = null,
)
