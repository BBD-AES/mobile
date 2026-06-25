package com.example.bbd.data.repo

import com.example.bbd.data.remote.CloseOrderResult
import com.example.bbd.data.remote.ConfirmOrderResult
import com.example.bbd.data.remote.CreateOrderResult
import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.SalesApi
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.UpdateOrderResult
import com.example.bbd.data.remote.dto.CreateCustomerOrderRequest
import com.example.bbd.data.remote.dto.CustomerOrderDetailDto
import com.example.bbd.data.remote.dto.CustomerOrderLineRequest
import com.example.bbd.data.remote.dto.CustomerOrderSummaryDto
import com.example.bbd.data.remote.dto.UpdateCustomerOrderRequest
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.util.UUID

/**
 * 현장 수주(CustomerOrder) 데이터 소스 — sales 서비스. 생성/조회/확정/종료.
 *
 * 시연 흐름: 등록(OPEN) → 확정(CONFIRMED) → 종료(CLOSED=지점재고 차감). 부분출고 없음(전 라인 전량).
 * - customerName 은 백엔드 @NotBlank → 공백이면 [DEFAULT_CUSTOMER] 로 치환(README: 고객명 선택 입력).
 * - 멱등: 생성/종료 모두 Idempotency-Key=클라 생성 UUID(1요청 1키, 재시도는 동일키 재전송). coNumber 는 키 아님.
 * - 비-2xx 는 예외가 아니라 결과 타입으로 분기(401=세션만료 / 409=상태·재고 / IOException=오프라인).
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

    /** 내 지점 수주 전체(상태 무관, 전 페이지 수집). 기간(start/end_date) 선택. 지점은 서버가 본인지점으로 스코프. */
    suspend fun branchOrders(
        dealerWarehouseCode: String,
        startDate: String? = null,
        endDate: String? = null,
    ): UiState<List<CustomerOrderSummaryDto>> =
        call { searchAll(dealerWarehouseCode = dealerWarehouseCode, startDate = startDate, endDate = endDate) }

    /** 수주 상세(라인 포함). close 확인 모달에서 차감 품목·수량 표시용. */
    suspend fun detail(coNumber: String): UiState<CustomerOrderDetailDto> =
        call { api.customerOrder(coNumber) }

    /** 수주 수정(OPEN only). lines 는 전체 교체라 호출측에서 현재 라인 전체를 보낸다. */
    suspend fun update(
        coNumber: String,
        note: String?,
        lines: List<CustomerOrderLineRequest>,
    ): UpdateOrderResult = withContext(Dispatchers.IO) {
        try {
            val resp = api.updateCustomerOrder(
                coNumber,
                UpdateCustomerOrderRequest(note = note?.ifBlank { null }, lines = lines),
            )
            when {
                resp.isSuccessful -> UpdateOrderResult.Ok(resp.body()?.coNumber ?: coNumber)
                resp.code() == 401 -> UpdateOrderResult.Unauthorized
                resp.code() == 409 -> UpdateOrderResult.Conflict(problemMessage(resp) ?: "접수 상태에서만 수정할 수 있습니다")
                else -> UpdateOrderResult.Error("수주 수정 실패 (${resp.code()})")
            }
        } catch (c: CancellationException) {
            throw c
        } catch (e: IOException) {
            UpdateOrderResult.Offline
        } catch (e: Exception) {
            UpdateOrderResult.Error(e.message ?: "네트워크 오류")
        }
    }

    /** 확정(OPEN→CONFIRMED). 비-OPEN 이면 409(CO004). */
    suspend fun confirm(coNumber: String): ConfirmOrderResult = withContext(Dispatchers.IO) {
        try {
            val resp = api.confirmCustomerOrder(coNumber)
            when {
                resp.isSuccessful -> ConfirmOrderResult.Ok(coNumber)
                resp.code() == 401 -> ConfirmOrderResult.Unauthorized
                resp.code() == 409 -> ConfirmOrderResult.Conflict(problemMessage(resp) ?: "확정할 수 없는 상태입니다")
                else -> ConfirmOrderResult.Error("확정 실패 (${resp.code()})")
            }
        } catch (c: CancellationException) {
            throw c
        } catch (e: IOException) {
            ConfirmOrderResult.Offline
        } catch (e: Exception) {
            ConfirmOrderResult.Error(e.message ?: "네트워크 오류")
        }
    }

    /**
     * 종료(CONFIRMED→CLOSED) = 지점재고 차감. idempotencyKey 는 호출측이 '한 종료 시도당 하나'로 고정 전달
     * (재시도 시 동일키 재전송 → sales 요청멱등). 409 는 ProblemDetail title(코드)로 분기.
     */
    suspend fun close(
        coNumber: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): CloseOrderResult = withContext(Dispatchers.IO) {
        try {
            val resp = api.closeCustomerOrder(coNumber, idempotencyKey)
            when {
                resp.isSuccessful -> CloseOrderResult.Ok(coNumber)
                resp.code() == 401 -> CloseOrderResult.Unauthorized
                resp.code() == 409 -> {
                    val (code, msg) = problemDetail(resp)
                    when {
                        code == "CO007" -> CloseOrderResult.Insufficient(msg ?: "지점 재고가 부족하여 종료할 수 없습니다")
                        code == "CO006" -> CloseOrderResult.NotClosable(msg ?: "이미 종료되었거나 확정 상태가 아닙니다")
                        code?.startsWith("IDEM") == true -> CloseOrderResult.Ok(coNumber) // 멱등 재생 = 이미 종료됨
                        else -> CloseOrderResult.Error(msg ?: "종료 실패 (409)")
                    }
                }
                else -> CloseOrderResult.Error("종료 실패 (${resp.code()})")
            }
        } catch (c: CancellationException) {
            throw c
        } catch (e: IOException) {
            CloseOrderResult.Offline
        } catch (e: Exception) {
            CloseOrderResult.Error(e.message ?: "네트워크 오류")
        }
    }

    /** 페이지를 끝까지 모아 전체 반환(size 100, 최대 20페이지 안전캡). SalesOrderRepository.searchAll 동일 패턴. */
    private suspend fun searchAll(
        status: String? = null,
        dealerWarehouseCode: String? = null,
        customerName: String? = null,
        requestedBy: String? = null,
        startDate: String? = null,
        endDate: String? = null,
    ): List<CustomerOrderSummaryDto> {
        val all = mutableListOf<CustomerOrderSummaryDto>()
        var page = 0
        while (page < MAX_PAGES) {
            val resp = api.searchCustomerOrders(
                status = status,
                dealerWarehouseCode = dealerWarehouseCode,
                customerName = customerName,
                requestedBy = requestedBy,
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

    // 에러 바디 = Spring ProblemDetail(RFC7807): title=에러코드(CO006/CO007/IDEM*), detail=메시지.
    private fun problemDetail(resp: Response<*>): Pair<String?, String?> =
        runCatching {
            val raw = resp.errorBody()?.string().orEmpty()
            val pd = Gson().fromJson(raw, ProblemDetailDto::class.java)
            pd?.title to pd?.detail
        }.getOrDefault(null to null)

    private fun problemMessage(resp: Response<*>): String? = problemDetail(resp).second

    private data class ProblemDetailDto(val title: String? = null, val detail: String? = null)

    private companion object {
        const val DEFAULT_CUSTOMER = "현장 즉시판매"
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 20
    }
}
