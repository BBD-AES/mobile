package com.example.bbd.data.remote

import com.example.bbd.data.remote.dto.CreateCustomerOrderRequest
import com.example.bbd.data.remote.dto.CustomerOrderDetailDto
import com.example.bbd.data.remote.dto.CustomerOrderPageDto
import com.example.bbd.data.remote.dto.CustomerOrderStatusChangeDto
import com.example.bbd.data.remote.dto.SalesOrderDetailDto
import com.example.bbd.data.remote.dto.SalesOrderPageDto
import com.example.bbd.data.remote.dto.UpdateCustomerOrderRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * sales 서비스 REST. BASE_URL=게이트웨이 루트라 경로에 서비스 prefix(sales/) 포함.
 * 컨트롤러 @RequestMapping("/api/v1/sales-orders") · 게이트웨이 prefix /sales.
 */
interface SalesApi {

    /** 보충 발주(판매주문) 목록 검색. 모든 파라미터 선택. */
    @GET("sales/api/v1/sales-orders")
    suspend fun search(
        @Query("status") status: String? = null,
        @Query("to_warehouse_code") toWarehouseCode: String? = null,
        @Query("requested_by") requestedBy: String? = null,
        @Query("received_by") receivedBy: String? = null,
        @Query("start_date") startDate: String? = null,  // yyyy-MM-dd (SalesOrderController @DateTimeFormat)
        @Query("end_date") endDate: String? = null,       // yyyy-MM-dd
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): SalesOrderPageDto

    /** 도착 확인(입고 확정): IN_FULFILLMENT → RECEIVED. */
    @PATCH("sales/api/v1/sales-orders/{soNumber}/receive")
    suspend fun receive(@Path("soNumber") soNumber: String): Response<Unit>

    /** SO 상세(라인 포함) — 입고 확정 폼 품목 표시(요약엔 라인 없음). */
    @GET("sales/api/v1/sales-orders/{soNumber}")
    suspend fun salesOrder(@Path("soNumber") soNumber: String): SalesOrderDetailDto

    /**
     * 현장 수주 등록(CustomerOrder 생성, OPEN). @Idempotent → Idempotency-Key 헤더 필수(서비스가 멱등 처리).
     * 비-2xx 를 직접 분기하려고 Response 래핑(401/검증오류를 예외로 던지지 않음).
     */
    @POST("sales/api/v1/customer-orders")
    suspend fun createCustomerOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateCustomerOrderRequest,
    ): Response<CustomerOrderDetailDto>

    /** 현장 수주 목록 검색. 지점은 서버가 본인지점(이름축)으로 스코프. 모든 파라미터 선택. */
    @GET("sales/api/v1/customer-orders")
    suspend fun searchCustomerOrders(
        @Query("status") status: String? = null,
        @Query("dealer_warehouse_code") dealerWarehouseCode: String? = null,
        @Query("customer_name") customerName: String? = null,
        @Query("requested_by") requestedBy: String? = null,
        @Query("start_date") startDate: String? = null,  // yyyy-MM-dd
        @Query("end_date") endDate: String? = null,       // yyyy-MM-dd
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): CustomerOrderPageDto

    /** 수주 상세(라인 포함) — close 확인 모달에서 차감 품목 표시용. */
    @GET("sales/api/v1/customer-orders/{coNumber}")
    suspend fun customerOrder(@Path("coNumber") coNumber: String): CustomerOrderDetailDto

    /** 수주 수정(OPEN only): note/lines 전체 교체. */
    @PUT("sales/api/v1/customer-orders/{coNumber}")
    suspend fun updateCustomerOrder(
        @Path("coNumber") coNumber: String,
        @Body request: UpdateCustomerOrderRequest,
    ): Response<CustomerOrderDetailDto>

    /** 수주 확정(OPEN→CONFIRMED). 비-OPEN 이면 409(CO004). */
    @PATCH("sales/api/v1/customer-orders/{coNumber}/confirm")
    suspend fun confirmCustomerOrder(@Path("coNumber") coNumber: String): Response<CustomerOrderStatusChangeDto>

    /**
     * 수주 종료(CONFIRMED→CLOSED) = 지점재고 동기 차감(전 라인 전량, 부분출고 없음).
     * 부족하면 409 CO007(지점재고 부족·차감 0), 비CONFIRMED 면 409 CO006.
     * Idempotency-Key=클라 생성 UUID(요청 1건당 1키, 재시도 시 동일키) — sales 요청멱등.
     * (이중 차감 최종 보루는 inventory 가 referenceNumber=coNumber 로 dedup.)
     */
    @PATCH("sales/api/v1/customer-orders/{coNumber}/close")
    suspend fun closeCustomerOrder(
        @Path("coNumber") coNumber: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): Response<CustomerOrderStatusChangeDto>
}
