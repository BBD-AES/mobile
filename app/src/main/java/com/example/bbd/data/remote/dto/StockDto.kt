package com.example.bbd.data.remote.dto

/**
 * inventory 재고 목록 응답. GET /inventory/api/v1/stocks → StockPageDto.
 *
 * StockItemDto 는 **보강된** StockListItemResponse 전제
 * (백엔드에 safetyStock·availableStock·category·unit 추가 요청 — docs/mobile-backend-gaps.md Gap 1).
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
