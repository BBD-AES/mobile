package com.example.bbd.data.repo

import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.SalesApi
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.SalesOrderSummaryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 보충 발주(SalesOrder) 데이터 소스 — sales 서비스.
 *
 * 반환은 서버 요약 DTO 그대로. 화면 모델(Pr/Movement 등)로의 매핑은 화면별로 결정해야 하며
 * (모바일 모델 ↔ 백엔드 SO 가 1:1 아님 — 다중 라인·상태 7종·출고 미지원), 후속 PR 에서 처리.
 */
class SalesOrderRepository(
    private val api: SalesApi = Net.create(SalesApi::class.java),
) {

    /** 내 지점으로 향하는 도착 대기(IN_FULFILLMENT) 발주. */
    suspend fun arrivals(warehouseCode: String): UiState<List<SalesOrderSummaryDto>> =
        call { api.search(status = "IN_FULFILLMENT", toWarehouseCode = warehouseCode).items }

    /** 내 지점의 전체 보충 발주(상태 무관). */
    suspend fun branchOrders(warehouseCode: String): UiState<List<SalesOrderSummaryDto>> =
        call { api.search(toWarehouseCode = warehouseCode).items }

    /** 내가 입고 확인한 발주(작업 이력 소스 후보). */
    suspend fun receivedByMe(empId: String): UiState<List<SalesOrderSummaryDto>> =
        call { api.search(receivedBy = empId).items }

    /** 도착 확인(입고 확정). 성공 여부만 반환. */
    suspend fun receive(soNumber: String): UiState<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { api.receive(soNumber) }.fold(
                onSuccess = {
                    if (it.isSuccessful) UiState.Success(Unit)
                    else UiState.Error("입고 확정 실패 (${it.code()})")
                },
                onFailure = { UiState.Error(it.message ?: "네트워크 오류") },
            )
        }

    private suspend fun call(
        block: suspend () -> List<SalesOrderSummaryDto>,
    ): UiState<List<SalesOrderSummaryDto>> =
        withContext(Dispatchers.IO) {
            runCatching { block() }.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "네트워크 오류") },
            )
        }
}
