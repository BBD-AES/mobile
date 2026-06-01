package com.example.bbd.data

/** 재고 이동 유형 — 입고 / 출고 / 조정(표시 전용). */
enum class MoveType { IN, OUT, ADJ }

/** 재고 상태 — 현재고 vs 안전재고. */
enum class StockStatus(val label: String) {
    SHORT("부족"), NONE("없음"), OK("정상");

    companion object {
        /** 현재고·안전재고로 상태 판정 (0=없음, <안전=부족, 그 외 정상). */
        fun of(qty: Int, safety: Int): StockStatus =
            when {
                qty <= 0 -> NONE
                qty < safety -> SHORT
                else -> OK
            }
    }
}

/** 지점 재고 1건. */
data class Part(
    val sku: String,
    val name: String,
    val cat: String,
    val thumb: String,
    val qty: Int,
    val unit: String,
    val status: StockStatus,
    val safety: Int,
    val wh: String,
)

/** 재고 이동(작업) 이력 1건. */
data class Movement(
    val type: MoveType,
    val label: String,
    val delta: Int,
    val unit: String,
    val sku: String,
    val name: String,
    val day: String,
    val date: String,
    val time: String,
)

/** 보충 발주 상태 — 본사 결정. 모바일은 표시 전용. */
enum class PrStatus(val label: String, val sub: String, val step: Int) {
    REQUESTED("요청됨", "본사 승인 대기", 1),
    APPROVED("승인됨", "배송 준비", 2),
    SHIPPED("배송중", "도착 예정", 3),
    RECEIVED("입고완료", "처리 종료", 4),
    REJECTED("거절됨", "본사 반려", 0);

    val isOpen: Boolean get() = this == REQUESTED || this == APPROVED || this == SHIPPED
    val isDone: Boolean get() = this == RECEIVED || this == REJECTED
}

/** 보충 발주 요청 1건 (지점→본사). */
data class Pr(
    val id: String,
    val sku: String,
    val name: String,
    val qty: Int,
    val unit: String,
    val status: PrStatus,
    val reason: String,
    val date: String,
    val time: String,
    val by: String,
    val note: String,
)

/** 로그인 게이팅용 사용자 계정 (시드 users.csv). */
data class UserAccount(
    val emp: String,
    val name: String,
    val position: String,
    val role: String,
    val branch: String,
    val mobile: Boolean,
    val block: String? = null,
)

/** 로그인한 현재 사용자(정민수). */
data class CurrentUser(
    val name: String,
    val role: String,
    val position: String,
    val emp: String,
    val branch: String,
    val branchCode: String,
    val warehouse: String,
    val warehouseName: String,
    val pwChanged: String,
    val pwDaysAgo: Int,
)

data class InvSummary(val total: Int, val short: Int, val none: Int, val ok: Int)
data class WorklogSummary(val total: Int, val inN: Int, val outN: Int, val adj: Int, val from: String, val to: String)

/** 로그인 게이팅 결과. */
sealed interface LoginResult {
    /** 등록되지 않은 사번. */
    data object Unknown : LoginResult
    /** 사번은 있으나 모바일 이용 불가 (카운터/본사/관리자). */
    data class Blocked(val user: UserAccount) : LoginResult
    /** 모바일 허용 — 정비사(BRANCH_STAFF) 또는 점장(BRANCH_MANAGER). */
    data class Allowed(val user: UserAccount) : LoginResult
}
