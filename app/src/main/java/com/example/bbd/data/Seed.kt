package com.example.bbd.data

/**
 * 시드 데이터 — 웹 프로토타입 bbd-data.jsx 기준.
 *
 * 강남 1지점(WH-BR-001)의 9개 부품은 부록 실데이터 initial_stocks.csv 의
 * 해당 창고 재고(부족 2·없음 1·정상 6)와 정확히 일치하도록 큐레이션됨.
 * 운영에서는 ERP API 로 대체. 여기서는 앱 내장 시드.
 */
object Seed {

    /** 프로토타입 시드 기준의 '현재'(결정적) — 상대 날짜 라벨 계산용. 실 연동 시 LocalDate.now()로. */
    const val DEMO_TODAY = "2026-06-20"

    // 모바일 허용 계정 = 강남 1지점(같은 지점·같은 창고 WH-BR-001). 정비사 BR002 / 점장 BR001.
    val ACCOUNTS: Map<String, CurrentUser> = listOf(
        CurrentUser(
            name = "정민수", role = "BRANCH_STAFF", position = "정비사", emp = "BR002",
            email = "minsu.jeong@bbd.co.kr",
            branch = "강남 1지점", branchCode = "WH-BR-001",
            warehouse = "WH-BR-001", warehouseName = "강남 1지점 창고",
            pwChanged = "2025-12-04", pwDaysAgo = 198,
        ),
        CurrentUser(
            name = "이상철", role = "BRANCH_MANAGER", position = "점장", emp = "BR001",
            email = "sangchul.lee@bbd.co.kr",
            branch = "강남 1지점", branchCode = "WH-BR-001",
            warehouse = "WH-BR-001", warehouseName = "강남 1지점 창고",
            pwChanged = "2026-03-10", pwDaysAgo = 102,
        ),
    ).associateBy { it.emp }

    /** 기본 로그인 계정(정비사). 데모/시드 면에서 사용. */
    val USER = ACCOUNTS.getValue("BR002")

    /** 사번 → 현재 사용자 resolve (없으면 기본 정비사). */
    fun resolveUser(emp: String): CurrentUser = ACCOUNTS[emp.trim().uppercase()] ?: USER

    /** 역할별 모바일 이용 권한(마이 > 이용 권한). */
    val ROLE_PERMS: Map<String, RolePerms> = mapOf(
        "BRANCH_STAFF" to RolePerms(
            can = listOf("입고 스캔·도착 확인", "지점 재고 조회", "내 작업 이력"),
            web = emptyList(),
            cant = listOf("지점 발주 요청(점장 전용)", "재고 조정(본사 전용)"),
        ),
        "BRANCH_MANAGER" to RolePerms(
            can = listOf("입고 스캔·도착 확인", "지점 재고 조회", "내 작업 이력"),
            web = listOf("지점 발주 요청", "발주 결과 확인"),
            cant = listOf("재고 조정(본사 전용)"),
        ),
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

    /** 재고 조회 필터 카테고리 칩(시드 모드 고정 목록 — API 모드는 실 재고에서 도출). */
    val CATEGORIES = listOf("엔진/오일", "엔진/필터", "제동", "전장", "외장·기타")

    // ────────── 도착 대기 SO (IN_FULFILLMENT, to_warehouse_code=WH-BR-001) ──────────
    // GET /api/v1/sales-orders?status=IN_FULFILLMENT&to_warehouse_code=WH-BR-001
    val INBOUND: List<SalesOrder> = listOf(
        SalesOrder("SO-2026-0061", "IN_FULFILLMENT", "본사 중앙창고", "WH-HQ-001", "WH-BR-001", lines = listOf(
            SoLine("BBD-OIL-1006", "엔진오일 5W-30 1L", 40, "EA", "OIL-btl"),
            SoLine("BBD-FLT-2002", "오일필터 HD-O2", 20, "EA", "EN-fil"),
        ), requestedBy = "이상철"),
        SalesOrder("SO-2026-0058", "IN_FULFILLMENT", "본사 중앙창고", "WH-HQ-001", "WH-BR-001", lines = listOf(
            SoLine("BBD-ELE-7002", "배터리 60AH", 10, "EA", "EL-bat"),
        ), requestedBy = "정민수"),
    )

    // ────────── 내가 도착 확인한 SO (RECEIVED, received_by=BR002) — 최신순 ──────────
    val RECEIVED: List<SalesOrder> = listOf(
        SalesOrder("SO-2026-0054", "RECEIVED", "본사 중앙창고", "WH-HQ-001", date = "2026-05-22", time = "09:14", lines = listOf(
            SoLine("BBD-FLT-2001", "에어필터 HD-A1", 60, "EA", "EN-air"),
            SoLine("BBD-FLT-2002", "오일필터 HD-O2", 30, "EA", "EN-fil"),
        ), receivedBy = "정민수"),
        SalesOrder("SO-2026-0049", "RECEIVED", "본사 중앙창고", "WH-HQ-001", date = "2026-05-21", time = "17:22", lines = listOf(
            SoLine("BBD-BRK-4001", "전방 브레이크 패드", 30, "SET", "BR-pad"),
        ), receivedBy = "정민수"),
        SalesOrder("SO-2026-0045", "RECEIVED", "강남 2지점 창고", "WH-BR-002", date = "2026-05-21", time = "14:08", lines = listOf(
            SoLine("BBD-BRK-4005", "DOT4 브레이크 액 1L", 12, "EA", "BR-oil"),
        ), receivedBy = "이상철"),
        SalesOrder("SO-2026-0041", "RECEIVED", "본사 중앙창고", "WH-HQ-001", date = "2026-05-19", time = "11:08", lines = listOf(
            SoLine("BBD-EXT-8001", "와이퍼 24인치", 24, "EA", "EX-wpr"),
            SoLine("BBD-EXT-8005", "워셔액 2L", 40, "EA", "EX-wsh"),
            SoLine("BBD-BRK-4002", "후방 브레이크 패드", 10, "SET", "BR-pad"),
        ), receivedBy = "정민수"),
        SalesOrder("SO-2026-0036", "RECEIVED", "본사 중앙창고", "WH-HQ-001", date = "2026-05-18", time = "10:42", lines = listOf(
            SoLine("BBD-FLT-2002", "오일필터 HD-O2", 25, "EA", "EN-fil"),
        ), receivedBy = "정민수"),
        SalesOrder("SO-2026-0030", "RECEIVED", "본사 중앙창고", "WH-HQ-001", date = "2026-05-15", time = "15:30", lines = listOf(
            SoLine("BBD-OIL-1006", "엔진오일 5W-30 1L", 48, "EA", "OIL-btl"),
        ), receivedBy = "정민수"),
    )

    // ────────── 지점 이동요청 이력 — 진행 중/종료 상태 예시(백오더·반려·취소). ──────────
    // 도착대기(INBOUND)·입고완료(RECEIVED)와 합쳐 '지점 전체 이력'을 구성. to_warehouse_code=WH-BR-001.
    val HISTORY_EXTRA: List<SalesOrder> = listOf(
        SalesOrder("SO-2026-0063", "BACKORDERED", "본사 중앙창고", "WH-HQ-001", "WH-BR-001", date = "2026-06-19", time = "10:05", lines = listOf(
            SoLine("BBD-BRK-4001", "전방 브레이크 패드", 20, "SET", "BR-pad"),
            SoLine("BBD-FLT-2001", "에어필터 HD-A1", 30, "EA", "EN-air"),
        ), requestedBy = "이상철"),
        SalesOrder("SO-2026-0060", "REJECTED", "본사 중앙창고", "WH-HQ-001", "WH-BR-001", date = "2026-06-17", time = "16:40", lines = listOf(
            SoLine("BBD-ELE-7002", "배터리 60AH", 8, "EA", "EL-bat"),
        ), requestedBy = "정민수"),
        SalesOrder("SO-2026-0052", "CANCELED", "본사 중앙창고", "WH-HQ-001", "WH-BR-001", date = "2026-06-12", time = "11:20", lines = listOf(
            SoLine("BBD-EXT-8005", "워셔액 2L", 40, "EA", "EX-wsh"),
        ), requestedBy = "이상철", canceledBy = "이상철"),
    )

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
