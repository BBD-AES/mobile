package com.example.bbd.data.repo

import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.NotificationApi
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.NotificationDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 지점 알림함 데이터 소스 — sales 서비스.
 * inbox 는 읽음·안읽음 모두(최근 100). markRead 는 best-effort(비핵심 read-model).
 */
class NotificationRepository(
    private val api: NotificationApi = Net.create(NotificationApi::class.java),
) {

    suspend fun inbox(): UiState<List<NotificationDto>> = call { api.inbox() }

    /** 읽음 처리 — 성공 여부만. 실패해도 UX 치명적 아님(낙관적 갱신 + 다음 조회 정정). */
    suspend fun markRead(id: Long): UiState<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.markRead(id)
                if (resp.isSuccessful) UiState.Success(Unit)
                else UiState.Error("읽음 처리 실패 (${resp.code()})")
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                UiState.Error(e.message ?: "네트워크 오류")
            }
        }

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
}
