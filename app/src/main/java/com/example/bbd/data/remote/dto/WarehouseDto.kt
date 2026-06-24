package com.example.bbd.data.remote.dto

/**
 * inventory 창고 목록 응답. GET /inventory/api/v1/warehouses → WarehousePageDto.
 * 모바일은 로그인 후 지점명(tenancyName)→창고코드 해석에만 사용(name 매칭 → code 보강).
 */
data class WarehousePageDto(
    val content: List<WarehouseDto> = emptyList(),
    val totalPages: Int = 0,
)

data class WarehouseDto(
    val code: String? = null,
    val name: String? = null,
    val type: String? = null,
    val active: Boolean = true,
)
