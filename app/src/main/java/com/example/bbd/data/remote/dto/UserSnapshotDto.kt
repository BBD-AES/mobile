package com.example.bbd.data.remote.dto

/**
 * user-service `GET /user/api/v1/users/me` 응답 DTO — 백엔드 `UserSnapshotResponse` 와 1:1
 * (Spring 기본 직렬화 = 필드명 그대로 camelCase, Gson 필드명 일치).
 *
 * GET (게이트웨이 루트)/user/api/v1/users/me  ·  Authorization: Bearer <JWT>  ·  any authenticated
 *
 * 게이트웨이 `/api/auth/me`(신원만)와 달리 이 응답은 **권위 role + 지점명(tenancyName)** 까지 준다:
 *  - role(UserRole enum): BRANCH_STAFF / BRANCH_MANAGER / ... — 서버 권위값(position best-effort 추정 폐기).
 *  - tenancyName: 지점명(실 지점) — 앱 branch 로 직접 사용.
 *
 * 단, **warehouse(창고코드)는 user 도메인에 없음** → 여전히 빈값, 재고는 tenancy 연동 대기(무날조).
 */
data class UserSnapshotDto(
    val userId: Long? = null,
    val keycloakSub: String? = null,
    val employeeNumber: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val position: String? = null,
    val status: String? = null,
    /** UserRole enum 문자열: BRANCH_STAFF / BRANCH_MANAGER / ... — 권위 인가값. */
    val role: String? = null,
    /** HQ / BRANCH. */
    val tenancyType: String? = null,
    /** 지점명(실 지점) — 앱 branch. */
    val tenancyName: String? = null,
    val version: Long? = null,
)
