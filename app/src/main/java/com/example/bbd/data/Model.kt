package com.example.bbd.data

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

/** 발주(SalesOrder) 한 라인 — 부품 단위 품목·수량. */
data class SoLine(
    val sku: String,
    val name: String,
    val qty: Int,
    val unit: String,
    val thumb: String,
)

/**
 * 판매주문(SalesOrder) 1건 — 모바일 관심 상태는 IN_FULFILLMENT(도착 대기)·RECEIVED(입고 완료).
 * 입고 확정은 주문 단위(PATCH /sales-orders/{so}/receive)라 화면 모델도 주문 단위.
 */
data class SalesOrder(
    val so: String,
    val status: String,       // SalesOrderStatus enum name
    val fromWh: String,
    val fromCode: String = "",
    val toCode: String = "WH-BR-001",
    val date: String = "",    // RECEIVED 도착 확인일(ISO date)
    val time: String = "",
    val lines: List<SoLine>,
)

/** SO 한 건의 합계(품목 수 · 총 수량 · 단일 라인이면 단위). */
data class SoTotals(val items: Int, val qty: Int, val unit: String)

fun SalesOrder.totals(): SoTotals {
    val qty = lines.sumOf { it.qty }
    val unit = if (lines.size == 1) lines.first().unit else ""
    return SoTotals(lines.size, qty, unit)
}

/** SO 상태 enum → 안내북 표기(라벨·배지 색). */
enum class SoStatusMeta(
    val key: String,
    val label: String,
    val sub: String,
) {
    IN_FULFILLMENT("IN_FULFILLMENT", "도착 대기", "출고되어 이동 중"),
    RECEIVED("RECEIVED", "입고 완료", "도착 확인 완료"),
    REQUESTED("REQUESTED", "요청됨", "본사 검토 대기"),
    SUBMITTED("SUBMITTED", "제출됨", "처리 대기"),
    BACKORDERED("BACKORDERED", "백오더", "재고 부족 대기"),
    REJECTED("REJECTED", "거절됨", "본사 반려"),
    CANCELED("CANCELED", "취소됨", "처리 취소");

    companion object {
        fun of(key: String?): SoStatusMeta? = entries.firstOrNull { it.key == key }
    }
}

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

/** 로그인한 현재 사용자(정비사 BR002 또는 점장 BR001). */
data class CurrentUser(
    val name: String,
    val role: String,
    val position: String,
    val emp: String,
    val email: String,
    val branch: String,
    val branchCode: String,
    val warehouse: String,
    val warehouseName: String,
    val pwChanged: String,
    val pwDaysAgo: Int,
)

/**
 * 게이트웨이 `/api/auth/me`(신원만) → 앱 [CurrentUser] 매핑.
 *
 * /me 는 신원만 주고 **role·지점(branch/branchCode)·창고(warehouse/warehouseName)·tenancy 는 주지 않는다**.
 *  - role: 서버가 진짜 인가를 함. 여기 값은 position 기반 best-effort UI 힌트일 뿐
 *    (position 에 "점장" 또는 "manager" 포함 시 BRANCH_MANAGER, 아니면 BRANCH_STAFF).
 *  - branch/branchCode/warehouse/warehouseName: /me 에 없음 → **빈값**. 시드로 날조하지 않는다(지점 매핑=tenancy 연동 대기).
 *  - pwChanged/pwDaysAgo: /me 에 없음 → 빈값/0(마이 화면 '마지막 변경'은 Keycloak 소관, 표시만 비움).
 */
fun com.example.bbd.data.remote.dto.MeDto.toCurrentUser(): CurrentUser {
    val pos = position ?: ""
    // best-effort UI 힌트 — 서버가 진짜 인가(UserSnapshot). position 텍스트로만 역할을 '추정'.
    val roleHint = if (pos.contains("점장") || pos.contains("manager", ignoreCase = true))
        "BRANCH_MANAGER" else "BRANCH_STAFF"
    return CurrentUser(
        name = displayName ?: (username ?: ""),
        role = roleHint,
        position = pos,
        emp = employeeNumber ?: "",
        email = email ?: "",
        // /me 에 없음 — tenancy(지점·창고) 매핑 연동 대기. 날조 금지(빈값 유지).
        branch = "",
        branchCode = "",
        warehouse = "",
        warehouseName = "",
        // /me 에 없음 — Keycloak 소관. 표시만 비움.
        pwChanged = "",
        pwDaysAgo = 0,
    )
}

/**
 * user-service `GET /user/api/v1/users/me`(권위 role + 지점명) → 앱 [CurrentUser] 매핑.
 *
 * `/api/auth/me`(신원-only, [MeDto.toCurrentUser])와 달리:
 *  - role: **서버 권위값(UserRole enum 그대로)** — position 기반 best-effort 추정 폐기.
 *  - branch: **tenancyName(실 지점명)** 직접 사용.
 *  - branchCode/warehouse/warehouseName: user 도메인에 없음 → **빈값**. 시드로 날조하지 않는다(재고=tenancy 연동 대기).
 *  - pwChanged/pwDaysAgo: 응답에 없음 → 빈값/0(Keycloak 소관, 표시만 비움).
 */
fun com.example.bbd.data.remote.dto.UserSnapshotDto.toCurrentUser(): CurrentUser =
    CurrentUser(
        name = displayName ?: (employeeNumber ?: ""),
        // 서버 권위 role(UserRole enum 문자열). 비면 안전 기본값 BRANCH_STAFF.
        role = role?.ifBlank { null } ?: "BRANCH_STAFF",
        position = position ?: "",
        emp = employeeNumber ?: "",
        email = email ?: "",
        // 권위 지점명 — tenancyName 직접 사용(빈 문자열 폴백).
        branch = tenancyName ?: "",
        // user 도메인에 없음 — 날조 금지(빈값). branchCode/창고는 tenancy 연동 대기.
        branchCode = "",
        warehouse = "",
        warehouseName = "",
        // 응답에 없음 — Keycloak 소관. 표시만 비움.
        pwChanged = "",
        pwDaysAgo = 0,
    )

/** 역할별 모바일 이용 권한(마이 > 이용 권한). can=모바일 가능 / web=웹 ERP에서 / cant=불가. */
data class RolePerms(
    val can: List<String>,
    val web: List<String>,
    val cant: List<String>,
)

data class InvSummary(val total: Int, val short: Int, val none: Int, val ok: Int)

// ────────── 날짜 표시 헬퍼 — ISO date만 보유, 표시 시점에 기준일 기준 상대 라벨 계산 ──────────
private fun isoToEpochDay(iso: String): Long? = runCatching {
    java.time.LocalDate.parse(iso).toEpochDay()
}.getOrNull()

/** 기준일(today, 기본 DEMO_TODAY) 대비 며칠 전인지. 음수=미래. */
fun daysSince(iso: String, today: String = Seed.DEMO_TODAY): Long? {
    val d = isoToEpochDay(iso) ?: return null
    val t = isoToEpochDay(today) ?: return null
    return t - d
}

/** 상대 날짜 라벨(오늘/어제/N일 전/M월 D일). */
fun relDay(iso: String, today: String = Seed.DEMO_TODAY): String {
    val n = daysSince(iso, today) ?: return ""
    return when {
        n <= 0 -> "오늘"
        n == 1L -> "어제"
        n < 7 -> "${n}일 전"
        else -> runCatching {
            val d = java.time.LocalDate.parse(iso)
            "${d.monthValue}월 ${d.dayOfMonth}일"
        }.getOrDefault(iso)
    }
}

/** 로그인 게이팅 결과. */
sealed interface LoginResult {
    /** 등록되지 않은 사번. */
    data object Unknown : LoginResult
    /** 사번은 있으나 모바일 이용 불가 (카운터/본사/관리자). */
    data class Blocked(val user: UserAccount) : LoginResult
    /** 모바일 허용 — 정비사(BRANCH_STAFF) 또는 점장(BRANCH_MANAGER). */
    data class Allowed(val user: UserAccount) : LoginResult
}
