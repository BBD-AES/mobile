package com.example.bbd.data.remote

/**
 * 쓰기 액션(출고·수주 등록) 결과 — 디자인 핸드오프의 결과 분기(성공/409/오프라인/세션만료)를
 * 그대로 타입으로 표현. UiState(Loading/Success/Error)로는 409 vs 401 vs 오프라인을 못 가르므로 별도.
 */
sealed interface OutboundResult {
    /** 204 성공 — referenceNumber=출고 번호, remaining=차감 후 서버 가용재고(재조회). */
    data class Ok(val referenceNumber: String, val remaining: Int) : OutboundResult
    /** 409 INSUFFICIENT_STOCK — serverAvailable=서버가 보고한 현재 가용재고. 부분 차감 없음. */
    data class Insufficient(val serverAvailable: Int) : OutboundResult
    /** 401 — 세션 만료, 재로그인 필요. */
    data object Unauthorized : OutboundResult
    /** 네트워크 미연결(IOException) — 차감되지 않음. */
    data object Offline : OutboundResult
    /** 기타 오류(메시지). */
    data class Error(val message: String) : OutboundResult
}

sealed interface CreateOrderResult {
    /** 201 성공 — coNumber=수주 번호. */
    data class Ok(val coNumber: String) : CreateOrderResult
    data object Unauthorized : CreateOrderResult
    data object Offline : CreateOrderResult
    data class Error(val message: String) : CreateOrderResult
}

/** 수주 확정(OPEN→CONFIRMED) 결과. */
sealed interface ConfirmOrderResult {
    data class Ok(val coNumber: String) : ConfirmOrderResult
    /** 409 CO004 — OPEN 아님(이미 확정/취소). */
    data class Conflict(val message: String) : ConfirmOrderResult
    data object Unauthorized : ConfirmOrderResult
    data object Offline : ConfirmOrderResult
    data class Error(val message: String) : ConfirmOrderResult
}

/**
 * 수주 종료(CONFIRMED→CLOSED = 지점재고 차감) 결과 — 409 를 코드로 갈라 데모 UX 분기.
 * (ProblemDetail title: CO007=재고부족, CO006=비CONFIRMED, IDEM*=멱등 재생→이미 종료=성공 간주.)
 */
sealed interface CloseOrderResult {
    /** 2xx — 종료 + 지점재고 차감 완료. */
    data class Ok(val coNumber: String) : CloseOrderResult
    /** 409 CO007 — 지점 재고 부족, 차감 0(원자적). message=서버 detail. */
    data class Insufficient(val message: String) : CloseOrderResult
    /** 409 CO006 — CONFIRMED 아님(이미 종료/취소). */
    data class NotClosable(val message: String) : CloseOrderResult
    data object Unauthorized : CloseOrderResult
    data object Offline : CloseOrderResult
    data class Error(val message: String) : CloseOrderResult
}
