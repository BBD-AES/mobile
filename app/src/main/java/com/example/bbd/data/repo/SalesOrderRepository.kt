package com.example.bbd.data.repo

import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.SalesApi
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.SoLine
import com.example.bbd.data.remote.dto.SalesOrderDetailDto
import com.example.bbd.data.remote.dto.SalesOrderSummaryDto
import kotlinx.coroutines.CancellationException
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
        call { searchAll(status = "IN_FULFILLMENT", toWarehouseCode = warehouseCode) }

    /** 내 지점의 전체 보충 발주(상태 무관). 기간(start/end_date, yyyy-MM-dd) 선택. */
    suspend fun branchOrders(
        warehouseCode: String,
        startDate: String? = null,
        endDate: String? = null,
    ): UiState<List<SalesOrderSummaryDto>> =
        call { searchAll(toWarehouseCode = warehouseCode, startDate = startDate, endDate = endDate) }

    /** 내가 입고 확인한 발주(작업 이력 소스 후보). */
    suspend fun receivedByMe(empId: String): UiState<List<SalesOrderSummaryDto>> =
        call { searchAll(receivedBy = empId) }

    /** 도착 확인(입고 확정). 성공 여부만 반환. */
    suspend fun receive(soNumber: String): UiState<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.receive(soNumber)
                if (resp.isSuccessful) UiState.Success(Unit)
                else UiState.Error("입고 확정 실패 (${resp.code()})")
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                UiState.Error(e.message ?: "네트워크 오류")
            }
        }

    /** SO 상세 → 모바일 SalesOrder(라인 포함) 매핑. 입고 확정 폼 품목 표시(요약 DTO엔 라인 없음). */
    suspend fun detail(soNumber: String): UiState<SalesOrder> = call {
        val d = api.salesOrder(soNumber)
        val at = when (d.status) {
            "RECEIVED" -> d.receivedAt
            "CANCELED", "REJECTED" -> d.canceledAt
            "SUBMITTED", "APPROVED", "IN_FULFILLMENT", "BACKORDERED" -> d.approvedAt ?: d.requestedAt
            else -> d.requestedAt ?: d.receivedAt ?: d.approvedAt ?: d.canceledAt
        }.orEmpty()
        SalesOrder(
            so = d.soNumber ?: soNumber,
            status = d.status ?: "IN_FULFILLMENT",
            // SO 상세 DTO엔 출처 창고가 없음 — 보충 SO 의 표준 출처(본사 중앙창고)로 표시. 목적지는 폼에서 me.warehouseName.
            fromWh = "본사 중앙창고",
            toCode = d.toWarehouseCode ?: "",
            date = at.take(10),
            time = at.drop(11).take(5),
            lines = d.lines.map { l ->
                SoLine(
                    sku = l.sku ?: "",
                    name = l.nameSnapshot?.takeIf { it.isNotBlank() } ?: (l.sku ?: ""),
                    qty = l.quantity,
                    unit = "EA",   // SO 라인 DTO엔 단위 없음 — 기본 EA.
                    thumb = "box",
                )
            },
            requestedBy = d.requestedBy ?: "",
            receivedBy = d.receivedBy ?: "",
        )
    }

    /** 페이지를 끝까지 모아 전체 반환(size 100, 최대 20페이지=2000건 안전캡 — 무한루프 방지). */
    private suspend fun searchAll(
        status: String? = null,
        toWarehouseCode: String? = null,
        requestedBy: String? = null,
        receivedBy: String? = null,
        startDate: String? = null,
        endDate: String? = null,
    ): List<SalesOrderSummaryDto> {
        val all = mutableListOf<SalesOrderSummaryDto>()
        var page = 0
        while (page < MAX_PAGES) {
            val resp = api.search(
                status = status,
                toWarehouseCode = toWarehouseCode,
                requestedBy = requestedBy,
                receivedBy = receivedBy,
                startDate = startDate,
                endDate = endDate,
                page = page,
                size = PAGE_SIZE,
            )
            all += resp.items
            val totalPages = resp.pagination?.totalPages ?: 1
            page++
            if (page >= totalPages || resp.items.isEmpty()) break
        }
        return all
    }

    // 코루틴 취소(CancellationException)는 삼키지 말고 재던져 structured concurrency 보존.
    private suspend fun <T> call(block: suspend () -> T): UiState<T> =
        withContext(Dispatchers.IO) {
            try {
                UiState.Success(block())
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                UiState.Error(e.message ?: "네트워크 오류")
            }
        }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 20
    }
}
