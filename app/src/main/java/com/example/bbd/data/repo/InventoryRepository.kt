package com.example.bbd.data.repo

import com.example.bbd.data.Part
import com.example.bbd.data.StockStatus
import com.example.bbd.data.remote.InventoryApi
import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.StockItemDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
