package com.example.bbd.data.remote

/**
 * 비동기 화면 상태. 목업 경로(USE_API=false)에서는 쓰지 않고,
 * 실 API 경로에서 ViewModel/produceState 가 노출하는 타입.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
