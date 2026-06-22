package com.example.bbd.data.remote.dto

/**
 * 게이트웨이 `/api/auth/me` 응답 DTO — 백엔드 record `CurrentUserResult` 와 1:1
 * (Spring 기본 직렬화 = 필드명 그대로 camelCase, Gson 필드명 일치).
 *
 * GET (게이트웨이 루트)/api/auth/me  ·  Authorization: Bearer <JWT>
 *
 * 중요: 이 응답은 **신원만** 제공한다. role / branch / warehouse(지점·창고)·tenancy 는
 * 의도적으로 포함되지 않으며(각 MSA 가 UserSnapshot 으로 인가 판단), 모바일에서 시드로 날조하지 않는다.
 */
data class MeDto(
    val authenticated: Boolean = false,
    val keycloakSub: String? = null,
    val username: String? = null,
    val employeeNumber: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val position: String? = null,
    val message: String? = null,
)
