package com.example.bbd.data.remote.dto

/**
 * 현장 수주 등록 요청 — sales POST /customer-orders (CreateCustomerOrderRequest 미러).
 * customerName 은 백엔드 @NotBlank 라 공백이면 repo 에서 기본값으로 치환해 전송한다.
 */
data class CreateCustomerOrderRequest(
    val dealerWarehouseCode: String,
    val customerName: String,
    val customerContact: String? = null,
    val note: String? = null,
    val lines: List<CustomerOrderLineRequest>,
)

data class CustomerOrderLineRequest(
    val sku: String,
    val quantity: Int,
)

/**
 * 수주 목록 페이지 — SalesOrderPageResponse<CustomerOrderSummaryResponse>.
 * 페이지 래퍼 형상은 SO 와 동일(items/pagination) → [PaginationDto] 재사용.
 */
data class CustomerOrderPageDto(
    val items: List<CustomerOrderSummaryDto> = emptyList(),
    val pagination: PaginationDto? = null,
)

/** 목록 item — CustomerOrderSummaryResponse 미러. 라인(SKU·수량)은 상세(GET /{coNumber})에서 보강. */
data class CustomerOrderSummaryDto(
    val coNumber: String? = null,
    val dealerWarehouseCode: String? = null,
    val dealerName: String? = null,
    val customerName: String? = null,
    val status: String? = null,          // CustomerOrderStatus enum name: OPEN/CONFIRMED/CLOSED/CANCELED
    val requestedBy: String? = null,
    val confirmedBy: String? = null,
    val canceledBy: String? = null,
    val closedBy: String? = null,
    val requestedAt: String? = null,     // ISO LocalDateTime 문자열
    val confirmedAt: String? = null,
    val canceledAt: String? = null,
    val closedAt: String? = null,
    val totalAmount: Double? = null,
    val note: String? = null,
)

/**
 * 수주 상세(CustomerOrderDetailResponse) — 라인 포함. 생성(POST) 응답도 이 타입.
 * 모바일은 추적용 coNumber·status + close 확인 모달용 lines 를 쓰고, 나머지는 무시(Gson 잉여/누락 허용).
 */
data class CustomerOrderDetailDto(
    val coNumber: String? = null,
    val dealerWarehouseCode: String? = null,
    val customerName: String? = null,
    val customerContact: String? = null,
    val status: String? = null,
    val requestedBy: String? = null,
    val confirmedBy: String? = null,
    val canceledBy: String? = null,
    val closedBy: String? = null,
    val requestedAt: String? = null,
    val confirmedAt: String? = null,
    val canceledAt: String? = null,
    val closedAt: String? = null,
    val totalAmount: Double? = null,
    val note: String? = null,
    val lines: List<CustomerOrderLineDto> = emptyList(),
)

/** 상세 라인 — CustomerOrderLineResponse. close 확인 모달에서 차감될 품목·수량 표시. */
data class CustomerOrderLineDto(
    val lineNo: Int = 0,
    val sku: String? = null,
    val nameSnapshot: String? = null,
    val unitPriceSnapshot: Double? = null,
    val quantity: Int = 0,
)

/** confirm/close 상태변경 응답 — CustomerOrderStatusChangeResponse. */
data class CustomerOrderStatusChangeDto(
    val coNumber: String? = null,
    val status: String? = null,
    val actor: String? = null,
    val changedAt: String? = null,
)
