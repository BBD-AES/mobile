package com.example.bbd.data.repo

import com.example.bbd.data.remote.Net
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.UserApi
import com.example.bbd.data.remote.dto.UserSnapshotDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 현재 사용자 스냅샷 데이터 소스 — user-service `GET /user/api/v1/users/me`.
 *
 * 게이트웨이 `/api/auth/me`(신원만, [AuthRepository])와 달리 **권위 role + 지점명(tenancyName)** 까지 제공.
 * 로그인 흐름에서 우선 호출하고, 실패(미등록/에러) 시 [AuthRepository] 신원-only 로 폴백한다.
 */
class UserRepository(
    private val api: UserApi = Net.create(UserApi::class.java),
) {

    /** 현재 사용자 스냅샷 조회. 성공/에러를 [UiState] 로. */
    suspend fun me(): UiState<UserSnapshotDto> =
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
