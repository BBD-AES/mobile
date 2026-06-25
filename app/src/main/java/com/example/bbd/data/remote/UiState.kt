package com.example.bbd.data.remote

/**
 * 비동기 화면 상태. 목업 경로(USE_API=false)에서는 쓰지 않고,
 * 실 API 경로에서 ViewModel/produceState 가 노출하는 타입.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    /** code = HTTP 상태(있으면). 401=세션만료, 403/404=권한없음/미등록 구분용. 네트워크 오류 등은 null. */
    data class Error(val message: String, val code: Int? = null) : UiState<Nothing>
}
