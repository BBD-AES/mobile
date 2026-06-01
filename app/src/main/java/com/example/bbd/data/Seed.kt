package com.example.bbd.data

/**
 * 시드 데이터 — 웹 프로토타입 bbd-data.jsx 기준.
 *
 * 강남 1지점(WH-BR-001)의 9개 부품은 부록 실데이터 initial_stocks.csv 의
 * 해당 창고 재고(부족 2·없음 1·정상 6)와 정확히 일치하도록 큐레이션됨.
 * 운영에서는 ERP API 로 대체. 여기서는 앱 내장 시드.
 */
object Seed {

    val USER = CurrentUser(
        name = "정민수", role = "BRANCH_STAFF", position = "정비사", emp = "BR002",
        branch = "강남 1지점", branchCode = "WH-BR-001",
        warehouse = "WH-BR-001", warehouseName = "강남 1지점",
        pwChanged = "2025-12-04", pwDaysAgo = 170,
    )

    // 모바일 로그인 게이팅 테이블.
    // 허용: BRANCH_STAFF '정비사' + BRANCH_MANAGER(자기 지점). 그 외 웹 전용.
    val USERS: Map<String, UserAccount> = listOf(
        UserAccount("BR002", "정민수", "정비사", "BRANCH_STAFF", "강남 1지점", mobile = true),
        UserAccount("BR001", "이상철", "점장", "BRANCH_MANAGER", "강남 1지점", mobile = true),
        UserAccount("BR003", "한미영", "주임", "BRANCH_STAFF", "강남 1지점", mobile = false, block = "카운터 직무는 웹에서 이용하세요."),
        UserAccount("HQ001", "김영희", "부장", "HQ_MANAGER", "본사", mobile = false, block = "본사 계정은 웹에서 이용하세요."),
        UserAccount("HQ002", "박철수", "과장", "HQ_STAFF", "본사", mobile = false, block = "본사 계정은 웹에서 이용하세요."),
        UserAccount("ADMIN", "관리자", "", "ADMIN", "본사", mobile = false, block = "관리자 계정은 웹에서 이용하세요."),
    ).associateBy { it.emp }

    private const val WH = "강남 1지점 · WH-BR-001"

    val PARTS: List<Part> = listOf(
        Part("BBD-BRK-4001", "전방 브레이크 패드", "제동", "BR-pad", 8, "SET", StockStatus.SHORT, 10, WH),
        Part("BBD-ELE-7002", "배터리 60AH", "전장", "EL-bat", 5, "EA", StockStatus.SHORT, 8, WH),
        Part("BBD-OIL-1006", "엔진오일 5W-30 1L", "엔진/오일", "OIL-btl", 0, "EA", StockStatus.NONE, 20, WH),
        Part("BBD-FLT-2002", "오일필터 HD-O2", "엔진/필터", "EN-fil", 45, "EA", StockStatus.OK, 25, WH),
        Part("BBD-FLT-2001", "에어필터 HD-A1", "엔진/필터", "EN-air", 28, "EA", StockStatus.OK, 20, WH),
        Part("BBD-BRK-4002", "후방 브레이크 패드", "제동", "BR-pad", 12, "SET", StockStatus.OK, 10, WH),
        Part("BBD-BRK-4005", "DOT4 브레이크 액 1L", "제동", "BR-oil", 22, "EA", StockStatus.OK, 15, WH),
        Part("BBD-EXT-8001", "와이퍼 24인치", "외장·기타", "EX-wpr", 18, "EA", StockStatus.OK, 15, WH),
        Part("BBD-EXT-8005", "워셔액 2L", "외장·기타", "EX-wsh", 38, "EA", StockStatus.OK, 20, WH),
    )

    val INV_SUMMARY = InvSummary(total = 9, short = 2, none = 1, ok = 6)

    /** 재고 조회 필터 카테고리 칩 순서. */
    val CATEGORIES = listOf("엔진/오일", "엔진/필터", "점화", "제동", "동력전달", "현가·조향", "전장", "외장·기타")

    val WORKLOG: List<Movement> = listOf(
        Movement(MoveType.OUT, "출고 · 수리", -4, "EA", "BBD-FLT-2002", "오일필터 HD-O2", "오늘", "2026-05-22", "14:35"),
        Movement(MoveType.IN, "도착 입고 확정", 60, "EA", "BBD-FLT-2001", "에어필터 HD-A1", "오늘", "2026-05-22", "09:14"),
        Movement(MoveType.OUT, "출고 · 교환", -2, "SET", "BBD-BRK-4001", "전방 브레이크 패드", "어제", "2026-05-21", "17:22"),
        Movement(MoveType.OUT, "출고 · 검사", -1, "SET", "BBD-IGN-3002", "스파크플러그 이리듐", "어제", "2026-05-21", "14:08"),
        Movement(MoveType.IN, "도착 입고 확정", 30, "SET", "BBD-BRK-4001", "전방 브레이크 패드", "5월 19일", "2026-05-19", "11:08"),
        Movement(MoveType.ADJ, "재고 조정 · 실사", 1, "EA", "BBD-BRK-4005", "DOT4 브레이크 액 1L", "5월 18일", "2026-05-18", "10:42"),
    )

    val WORKLOG_SUMMARY = WorklogSummary(total = 23, inN = 9, outN = 12, adj = 2, from = "2026-04-22", to = "2026-05-22")

    val RECENT: List<Movement> get() = WORKLOG.take(3)

    val PR: List<Pr> = listOf(
        Pr("PR-2026-0042", "BBD-OIL-1006", "엔진오일 5W-30 1L", 40, "EA", PrStatus.APPROVED, "무재고 보충", "2026-05-22", "09:40", "정민수", "본사 승인 완료 · 배송 준비"),
        Pr("PR-2026-0039", "BBD-ELE-7002", "배터리 60AH", 10, "EA", PrStatus.REQUESTED, "안전재고 미달", "2026-05-21", "16:05", "정민수", "본사 승인 대기 중"),
        Pr("PR-2026-0035", "BBD-BRK-4001", "전방 브레이크 패드", 20, "SET", PrStatus.RECEIVED, "안전재고 미달", "2026-05-19", "10:12", "정민수", "입고 완료 · 5/19"),
        Pr("PR-2026-0031", "BBD-FLT-2001", "에어필터 HD-A1", 30, "EA", PrStatus.SHIPPED, "수요 증가", "2026-05-18", "11:33", "정민수", "배송 중 · 도착 예정 5/24"),
        Pr("PR-2026-0028", "BBD-OIL-1006", "엔진오일 5W-30 1L", 24, "EA", PrStatus.REJECTED, "수요 증가", "2026-05-15", "14:50", "정민수", "본사 반려 · 직전 발주분 소진 후 재요청"),
    )

    /** 알림 배지 수. */
    const val NOTIF = 3

    fun partBySku(sku: String): Part? =
        PARTS.find { it.sku.equals(sku, ignoreCase = true) }

    /**
     * 로그인 게이팅 — 가장 중요한 비즈니스 규칙.
     * 미등록 사번 / 모바일 불가 계정 / 허용 의 세 갈래.
     */
    fun gate(empId: String): LoginResult {
        val u = USERS[empId.trim().uppercase()] ?: return LoginResult.Unknown
        return if (u.mobile) LoginResult.Allowed(u) else LoginResult.Blocked(u)
    }
}
