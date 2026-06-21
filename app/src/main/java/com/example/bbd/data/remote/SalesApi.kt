package com.example.bbd.data.remote

import com.example.bbd.data.remote.dto.SalesOrderPageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * sales 서비스 REST. 게이트웨이 prefix(/sales)는 [BuildConfig.BASE_URL] 에 포함되므로
 * 여기 경로는 컨트롤러 @RequestMapping("/api/v1/sales-orders") 기준 상대경로.
 */
interface SalesApi {

    /** 보충 발주(판매주문) 목록 검색. 모든 파라미터 선택. */
    @GET("api/v1/sales-orders")
    suspend fun search(
        @Query("status") status: String? = null,
        @Query("to_warehouse_code") toWarehouseCode: String? = null,
        @Query("requested_by") requestedBy: String? = null,
        @Query("received_by") receivedBy: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): SalesOrderPageDto

    /** 도착 확인(입고 확정): IN_FULFILLMENT → RECEIVED. */
    @PATCH("api/v1/sales-orders/{soNumber}/receive")
    suspend fun receive(@Path("soNumber") soNumber: String): Response<Unit>
}
