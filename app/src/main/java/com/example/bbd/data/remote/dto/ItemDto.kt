package com.example.bbd.data.remote.dto

/**
 * item-service `GET /api/v1/items/{sku}` 응답(품목 마스터). 게이트웨이 prefix /item.
 * 현장수주 부품 해석용 — sku/name 필수, 나머지는 표시 보조(필드명 틀려도 null-safe).
 */
data class ItemDto(
    val sku: String? = null,
    val name: String? = null,
    val category: String? = null,
    val unit: String? = null,
    val safetyStock: Int? = null,
    val unitPrice: Int? = null,
    val active: Boolean = true,
    val sourcingType: String? = null,  // BUY|MAKE
)
