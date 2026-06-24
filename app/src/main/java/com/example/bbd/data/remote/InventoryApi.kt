package com.example.bbd.data.remote

import com.example.bbd.data.remote.dto.StockOutboundRequest
import com.example.bbd.data.remote.dto.StockPageDto
import com.example.bbd.data.remote.dto.WarehousePageDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * inventory 서비스 REST. 게이트웨이 루트(BuildConfig.BASE_URL) 기준이라 경로에 서비스 prefix(inventory/) 포함.
 * 컨트롤러: @RequestMapping("/api/v1/stocks") · 게이트웨이 prefix /inventory.
 */
interface InventoryApi {

    /** 재고 목록 검색. StockSearchCondition(warehouseCode·sku·category·belowSafety)을 query 로. */
    @GET("inventory/api/v1/stocks")
    suspend fun stocks(
        @Query("warehouseCode") warehouseCode: String? = null,
        @Query("category") category: String? = null,
        @Query("belowSafety") belowSafety: Boolean? = null,
        @Query("sku") sku: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
    ): StockPageDto

    /**
     * 출고(지점 재고 차감). 성공 204(No Content) · 부족 시 409(INSUFFICIENT_STOCK).
     * 게이트웨이가 변경 POST 에 Idempotency-Key 헤더를 강제(없으면 400 IDEM400) → referenceNumber 를 키로 전달.
     * inventory 도 referenceNumber(body)로 멱등 dedup — 재시도는 같은 referenceNumber = 같은 키. 비-2xx 분기 위해 Response 래핑.
     */
    @POST("inventory/api/v1/stocks/outbound")
    suspend fun outbound(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: StockOutboundRequest,
    ): Response<Unit>

    /** 창고 목록 — 로그인 후 지점명(tenancyName)→창고코드 해석용(이름 매칭). */
    @GET("inventory/api/v1/warehouses")
    suspend fun warehouses(
        @Query("type") type: String? = null,
        @Query("active") active: Boolean? = null,
        @Query("size") size: Int = 500,
    ): WarehousePageDto
}
