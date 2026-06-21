package com.example.bbd.data.remote

import com.example.bbd.data.remote.dto.StockPageDto
import retrofit2.http.GET
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
}
