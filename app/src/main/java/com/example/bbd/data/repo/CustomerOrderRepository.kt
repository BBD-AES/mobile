package com.example.bbd.data.repo

import com.example.bbd.data.remote.CreateOrderResult
import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.SalesApi
import com.example.bbd.data.remote.dto.CreateCustomerOrderRequest
import com.example.bbd.data.remote.dto.CustomerOrderLineRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * 현장 수주(CustomerOrder) 등록 — sales 서비스.
 *
 * - customerName 은 백엔드 @NotBlank → 공백이면 [DEFAULT_CUSTOMER] 로 치환(README: 고객명 선택 입력).
 * - Idempotency-Key = 등록 요청마다 새 UUID(중복 탭은 화면에서 잠그고, 키는 1요청 1키).
 * - 비-2xx 는 예외가 아니라 [CreateOrderResult] 로 분기(401=세션만료 / 그 외=오류 / IOException=오프라인).
 */
class CustomerOrderRepository(
    private val api: SalesApi = Net.create(SalesApi::class.java),
) {

    suspend fun create(
        dealerWarehouseCode: String,
        customerName: String,
        customerContact: String?,
        note: String?,
        lines: List<CustomerOrderLineRequest>,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): CreateOrderResult = withContext(Dispatchers.IO) {
        try {
            val req = CreateCustomerOrderRequest(
                dealerWarehouseCode = dealerWarehouseCode,
                customerName = customerName.ifBlank { DEFAULT_CUSTOMER },
                customerContact = customerContact?.ifBlank { null },
                note = note?.ifBlank { null },
                lines = lines,
            )
            val resp = api.createCustomerOrder(idempotencyKey, req)
            when {
                resp.isSuccessful -> {
                    val co = resp.body()?.coNumber
                    if (!co.isNullOrBlank()) CreateOrderResult.Ok(co)
                    else CreateOrderResult.Error("수주 번호를 받지 못했습니다")
                }
                resp.code() == 401 -> CreateOrderResult.Unauthorized
                else -> CreateOrderResult.Error("수주 등록 실패 (${resp.code()})")
            }
        } catch (c: CancellationException) {
            throw c
        } catch (e: IOException) {
            CreateOrderResult.Offline
        } catch (e: Exception) {
            CreateOrderResult.Error(e.message ?: "네트워크 오류")
        }
    }

    private companion object {
        const val DEFAULT_CUSTOMER = "현장 즉시판매"
    }
}
