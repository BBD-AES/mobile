package com.example.bbd.data.repo

import com.example.bbd.data.remote.AuthApi
import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.MeDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 현재 사용자 신원 데이터 소스 — 게이트웨이 `/api/auth/me`.
 *
 * 응답은 신원만(authenticated/keycloakSub/username/employeeNumber/displayName/email/position).
 * role/지점/창고는 응답에 없으므로 화면에서 시드로 날조하지 않는다.
 */
class AuthRepository(
    private val api: AuthApi = Net.create(AuthApi::class.java),
) {

    /** 현재 사용자 신원 조회. 성공/에러를 [UiState] 로. */
    suspend fun me(): UiState<MeDto> =
        withContext(Dispatchers.IO) {
            try {
                UiState.Success(api.me())
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                UiState.Error(e.message ?: "네트워크 오류")
            }
        }
}
