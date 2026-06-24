package com.example.bbd.data.remote.dto

/**
 * 출고(지점 재고 차감) 요청 — inventory POST /stocks/outbound (StockOutboundRequest 미러).
 *
 * referenceNumber = 출고 번호(원장 참조). inventory 는 이 값으로 멱등 처리(Idempotency-Key 헤더 없음).
 * 부분 차감 없음 — 한 라인이라도 부족하면 전체 409(INSUFFICIENT_STOCK).
 */
data class StockOutboundRequest(
    val referenceNumber: String,
    val lines: List<StockOutboundLine>,
)

data class StockOutboundLine(
    val sku: String,
    val quantity: Int,
    val warehouseCode: String,
    val unitPrice: Int,
)
