package com.example.bbd.data.remote.dto

/**
 * inventory 재고 목록 응답. GET /inventory/api/v1/stocks → StockPageDto.
 *
 * StockItemDto 는 StockListItemResponse(목록 DTO)와 1:1. 목록 DTO 는 단건 StockResponse 와 parity 라
 * safetyStock·availableStock·category·unit 을 이미 포함 — 'Gap 1 보강' 충족(별도 백엔드 PR 불필요).
 * Tolerant Reader: 목록 DTO 의 unitPrice·location·updatedAt 등 안 쓰는 필드는 선언 생략(Gson 자동 무시).
 */
data class StockPageDto(
    val content: List<StockItemDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)

data class StockItemDto(
    val sku: String? = null,
    val name: String? = null,
    val currentStock: Int = 0,
    val warehouseCode: String? = null,
    val safetyStock: Int = 0,
    val availableStock: Int = 0,
    val category: String? = null,
    val unit: String? = null,
)
