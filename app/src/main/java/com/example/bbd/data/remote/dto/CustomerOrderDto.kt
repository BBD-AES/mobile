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
 * 수주 생성 응답(CustomerOrderDetailResponse) — 모바일은 추적용 coNumber·status 만 사용.
 * 나머지 필드는 무시(Gson 은 누락/잉여 필드 허용).
 */
data class CustomerOrderDetailDto(
    val coNumber: String? = null,
    val status: String? = null,
)
