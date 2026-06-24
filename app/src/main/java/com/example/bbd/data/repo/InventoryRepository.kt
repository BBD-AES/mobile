package com.example.bbd.data.repo

import com.example.bbd.data.Part
import com.example.bbd.data.StockStatus
import com.example.bbd.data.remote.InventoryApi
import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.OutboundResult
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.StockItemDto
import com.example.bbd.data.remote.dto.StockOutboundLine
import com.example.bbd.data.remote.dto.StockOutboundRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 재고(Stock) 데이터 소스 — inventory 서비스.
 * StockItemDto → 모바일 [Part] 매핑(상태=현재고/안전재고 계산). 카테고리/단위/안전재고는 보강 DTO 전제.
 */
class InventoryRepository(
    private val api: InventoryApi = Net.create(InventoryApi::class.java),
) {

    /** 내 지점의 재고 전체(전 페이지 수집, 20p 안전캡). */
    suspend fun branchStocks(warehouseCode: String): UiState<List<Part>> =
        withContext(Dispatchers.IO) {
            try {
                val all = mutableListOf<StockItemDto>()
                var page = 0
                var totalPages = 1
                // 백엔드 totalPages 로 완전 종료(매직 캡으로 무음 절단하지 않음).
                while (page < totalPages) {
                    val resp = api.stocks(warehouseCode = warehouseCode, page = page, size = PAGE_SIZE)
                    all += resp.content
                    totalPages = resp.totalPages
                    if (resp.content.isEmpty()) break
                    page++
                }
                UiState.Success(all.map { it.toPart() })
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                UiState.Error(e.message ?: "네트워크 오류")
            }
        }

    /**
     * 출고(지점 재고 차감, 단일 라인). 성공이면 차감 후 서버 가용재고를 재조회해 함께 반환,
     * 409 면 서버가 보고한 현재 가용재고를 함께 반환(README '가용 N으로 수량 조정' 안내용).
     * unitPrice 는 모바일에 가격 데이터가 없어 0(원장 평가액 N/A, 차감만 의미).
     */
    suspend fun outbound(
        referenceNumber: String,
        warehouseCode: String,
        sku: String,
        quantity: Int,
        unitPrice: Int = 0,
    ): OutboundResult = withContext(Dispatchers.IO) {
        try {
            val req = StockOutboundRequest(
                referenceNumber = referenceNumber,
                lines = listOf(StockOutboundLine(sku, quantity, warehouseCode, unitPrice)),
            )
            // 게이트웨이 멱등 강제(IDEM400 방지): referenceNumber 를 Idempotency-Key 로 — 재시도 시 동일 키.
            val resp = api.outbound(referenceNumber, req)
            when {
                resp.isSuccessful -> OutboundResult.Ok(referenceNumber, serverAvailable(warehouseCode, sku) ?: 0)
                resp.code() == 409 -> OutboundResult.Insufficient(serverAvailable(warehouseCode, sku) ?: 0)
                resp.code() == 401 -> OutboundResult.Unauthorized
                else -> OutboundResult.Error("출고 실패 (${resp.code()})")
            }
        } catch (c: CancellationException) {
            throw c
        } catch (e: IOException) {
            OutboundResult.Offline
        } catch (e: Exception) {
            OutboundResult.Error(e.message ?: "네트워크 오류")
        }
    }

    /** 특정 창고·SKU 의 서버 실가용재고 1건(차감 후 잔량·부족 안내용). 실패하면 null. */
    private suspend fun serverAvailable(warehouseCode: String, sku: String): Int? =
        runCatching {
            api.stocks(warehouseCode = warehouseCode, sku = sku, size = 1).content.firstOrNull()?.availableStock
        }.getOrNull()

    private fun StockItemDto.toPart(): Part = Part(
        sku = sku ?: "",
        name = name ?: (sku ?: ""),
        cat = category ?: "",
        thumb = "box",
        qty = currentStock,
        unit = unit ?: "EA",
        status = StockStatus.of(currentStock, safetyStock),
        safety = safetyStock,
        wh = warehouseCode ?: "",
    )

    private companion object {
        const val PAGE_SIZE = 100
    }
}
