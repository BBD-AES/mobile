package com.example.bbd.data.remote.dto

/**
 * sales 서비스 응답 DTO — 백엔드 record 와 1:1 (Gson 필드명 = record 컴포넌트명).
 *
 * GET /sales/api/v1/sales-orders
 *   → SalesOrderPageResponse<SalesOrderSummaryResponse> == [SalesOrderPageDto]
 */
data class SalesOrderPageDto(
    val items: List<SalesOrderSummaryDto> = emptyList(),
    val pagination: PaginationDto? = null,
)

/** 목록 item. 라인(SKU·수량)은 요약에 없음 → 상세(GET /{soNumber})에서 보강. */
data class SalesOrderSummaryDto(
    val soNumber: String? = null,
    val toWarehouseCode: String? = null,
    val toWarehouseName: String? = null,
    val status: String? = null,      // SalesOrderStatus enum name
    val priority: String? = null,    // SalesOrderPriority enum name
    val requestedBy: String? = null,
    val approvedBy: String? = null,
    val receivedBy: String? = null,
    val canceledBy: String? = null,
    val requestedAt: String? = null, // ISO LocalDateTime 문자열
    val approvedAt: String? = null,
    val receivedAt: String? = null,
    val canceledAt: String? = null,
    val totalAmount: Double? = null,
    val note: String? = null,
)

data class PaginationDto(
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)

/**
 * SO 상세(GET /sales-orders/{soNumber}) — SalesOrderDetailResponse 미러. 라인 포함.
 * 입고 확정 폼이 품목(라인)을 보여주려면 요약(라인 없음)이 아닌 상세가 필요.
 */
data class SalesOrderDetailDto(
    val soNumber: String? = null,
    val toWarehouseCode: String? = null,
    val toWarehouseName: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val requestedBy: String? = null,
    val approvedBy: String? = null,
    val receivedBy: String? = null,
    val canceledBy: String? = null,
    val requestedAt: String? = null,
    val approvedAt: String? = null,
    val receivedAt: String? = null,
    val canceledAt: String? = null,
    val totalAmount: Double? = null,
    val note: String? = null,
    val customerOrderNumber: String? = null,
    val lines: List<SalesOrderLineDto> = emptyList(),
)

data class SalesOrderLineDto(
    val lineNo: Int = 0,
    val sku: String? = null,
    val nameSnapshot: String? = null,
    val unitPriceSnapshot: Double? = null,
    val quantity: Int = 0,
    val reservedQuantity: Int = 0,
)
