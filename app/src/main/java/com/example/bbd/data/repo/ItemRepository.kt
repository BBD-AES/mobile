package com.example.bbd.data.repo

import com.example.bbd.data.remote.ItemApi
import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.ItemDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 품목 마스터 — item-service. 현장수주 부품 해석(by-SKU). */
class ItemRepository(
    private val api: ItemApi = Net.create(ItemApi::class.java),
) {
    /** SKU 해석. 미존재(404)=Success(null), 그 외 오류=Error. */
    suspend fun resolve(code: String): UiState<ItemDto?> =
        withContext(Dispatchers.IO) {
            try {
                val r = api.byCode(code.trim())
                when {
                    r.isSuccessful -> UiState.Success(r.body())
                    r.code() == 404 -> UiState.Success(null)
                    else -> UiState.Error("품목 조회 실패 (${r.code()})")
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                UiState.Error(e.message ?: "네트워크 오류")
            }
        }
}
