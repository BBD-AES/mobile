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
    private val itemRepo: ItemRepository = ItemRepository(),
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
                val safetyBySku = itemSafetyBySku()
                UiState.Success(all.map { it.toPart(safetyBySku) })
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                UiState.Error(e.message ?: "네트워크 오류")
            }
        }

    /**
     * 단일 SKU 를 내 창고 재고에서 해석(출고 스캔 수동/스캔 입력 — 시드 카탈로그 대체).
     * stocks?warehouseCode&sku 1건 → Part(현재고를 가용으로). 미보유면 null(미등록/재고없음).
     */
    suspend fun resolvePart(warehouseCode: String, sku: String): UiState<Part?> =
        withContext(Dispatchers.IO) {
            try {
                val dto = api.stocks(warehouseCode = warehouseCode, sku = sku.trim(), size = 1).content.firstOrNull()
                UiState.Success(dto?.toPart(itemSafetyBySku()))
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

    /**
     * 지점명(tenancyName) → 창고코드 해석. /api/auth/me·/users/me 가 창고를 주지 않으므로,
     * 로그인 후 inventory 창고목록에서 이름 일치(앞뒤공백·대소문자 무시) 창고의 code·name 을 보강한다.
     * 실패/미일치 → null(호출측은 '지점 매핑 대기' 빈값 유지). 반환 = (warehouseCode, warehouseName).
     */
    suspend fun resolveWarehouseByName(tenancyName: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = tenancyName.trim()
                val wh = api.warehouses().content.firstOrNull {
                    (it.name ?: "").trim().equals(target, ignoreCase = true)
                }
                val code = wh?.code ?: ""
                if (code.isBlank()) null else code to (wh?.name ?: target)
            }.getOrNull()
        }

    /** 특정 창고·SKU 의 서버 실가용재고 1건(차감 후 잔량·부족 안내용). 실패하면 null. */
    private suspend fun serverAvailable(warehouseCode: String, sku: String): Int? =
        runCatching {
            api.stocks(warehouseCode = warehouseCode, sku = sku, size = 1).content.firstOrNull()?.availableStock
        }.getOrNull()

    private suspend fun itemSafetyBySku(): Map<String, Int> =
        when (val items = itemRepo.activeItems(size = 1000)) {
            is UiState.Success -> items.data
                .filter { !it.sku.isNullOrBlank() }
                .associate { it.sku.orEmpty() to (it.safetyStock ?: 0) }
            else -> emptyMap()
        }

    private fun StockItemDto.toPart(safetyBySku: Map<String, Int> = emptyMap()): Part {
        val cleanSku = sku ?: ""
        val itemSafety = safetyBySku[cleanSku] ?: 0
        val resolvedSafety = if (itemSafety > 0) itemSafety else safetyStock
        return Part(
            sku = cleanSku,
            name = name ?: cleanSku,
            cat = category ?: "",
            thumb = "box",
            qty = currentStock,
            unit = unit ?: "EA",
            status = StockStatus.of(currentStock, resolvedSafety),
            safety = resolvedSafety,
            wh = warehouseCode ?: "",
        )
    }

    private companion object {
        const val PAGE_SIZE = 100
    }
}
