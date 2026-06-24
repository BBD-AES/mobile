package com.example.bbd.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.bbd.data.CurrentUser
import com.example.bbd.data.Part
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.Seed
import com.example.bbd.data.remote.dto.NotificationDto

/**
 * 앱 전역 상태(셸 보유) — 디자인의 `AppData` 컨텍스트 대응.
 * 입고 확정이 큐·카운트·이력에 즉시 반영되는 단일 진실원.
 *
 * 시드 모드는 Seed.INBOUND/RECEIVED 로 채우고, 입고 확정 시 inbound→received 이동.
 * (실 API 모드의 큐/이력은 각 화면이 repo 로 직접 로드 — 여기 시드 상태와 별개.)
 */
class AppData(
    inbound: List<SalesOrder>,
    received: List<SalesOrder>,
) {
    var inbound by mutableStateOf(inbound); private set
    var received by mutableStateOf(received); private set
    var lastRefresh by mutableStateOf(System.currentTimeMillis()); private set
    var refreshing by mutableStateOf(false); private set

    // ── 현장 쓰기 액션(출고/현장수주) 세션 상태 — 홈 '오늘 N건' 카운터·결과 잔량용 ──
    var outbounds by mutableStateOf<List<OutboundRec>>(emptyList()); private set
    var orders by mutableStateOf<List<OrderRec>>(emptyList()); private set
    private var stockOverride by mutableStateOf<Map<String, Int>>(emptyMap())

    // ── 지점 알림함 — 시드 모드는 Seed.NOTIFICATIONS, API 모드는 화면이 repo 로 setNotifications. ──
    var notifications by mutableStateOf<List<NotificationDto>>(Seed.NOTIFICATIONS); private set
    val unreadCount: Int get() = notifications.count { !it.read }
    fun loadNotifications(list: List<NotificationDto>) { notifications = list }
    /** 읽음 처리(낙관적) — 해당 id 를 read=true 로. 서버 PATCH 는 화면에서 best-effort 호출. */
    fun markNotifRead(id: Long?) {
        if (id == null) return
        notifications = notifications.map { if (it.id == id) it.copy(read = true) else it }
    }

    /** 도착 확인(입고 확정) — inbound 에서 제거 → received 맨 앞 추가. 같은 발주 중복 방지. by=입고자 이름. */
    fun confirmReceive(soNumber: String, by: String = "") {
        val hit = inbound.firstOrNull { it.so == soNumber } ?: return
        inbound = inbound.filterNot { it.so == soNumber }
        val now = nowHHMM()
        val moved = hit.copy(status = "RECEIVED", date = Seed.DEMO_TODAY, time = now, receivedBy = by.ifBlank { hit.receivedBy })
        if (received.none { it.so == soNumber }) received = listOf(moved) + received
    }

    /** 당겨서 새로고침 / 헤더 refresh — 시드 모드에선 타임스탬프만 갱신. */
    fun refresh() {
        refreshing = true
        lastRefresh = System.currentTimeMillis()
        refreshing = false
    }

    /** 가용재고(시드 모드는 차감 반영, API 모드는 사전 표시용 fallback — 서버가 진실원). */
    fun availableOf(sku: String, fallback: Int): Int = stockOverride[sku] ?: fallback

    /** 출고 확정 — 세션 재고 차감 + 출고 이력 prepend(홈 '오늘 출고 N건'). */
    fun applyOutbound(part: Part, qty: Int, reason: String, coNo: String) {
        val cur = stockOverride[part.sku] ?: part.qty
        stockOverride = stockOverride + (part.sku to (cur - qty).coerceAtLeast(0))
        outbounds = listOf(OutboundRec(part.sku, part.name, qty, part.unit, reason, coNo, Seed.DEMO_TODAY, nowHHMM())) + outbounds
    }

    /** 현장 수주 등록 — 수주 이력 prepend(홈 '오늘 수주 N건'). */
    fun addOrder(coNo: String, customer: String, items: Int, qty: Int) {
        orders = listOf(OrderRec(coNo, customer, items, qty, Seed.DEMO_TODAY, nowHHMM())) + orders
    }
}

/** 출고 이력 1건(세션). */
data class OutboundRec(
    val sku: String, val name: String, val qty: Int, val unit: String,
    val reason: String, val coNo: String, val date: String, val time: String,
)

/** 현장 수주 이력 1건(세션). */
data class OrderRec(
    val no: String, val customer: String, val items: Int, val qty: Int,
    val date: String, val time: String,
)

private var coSeq: Int = (System.currentTimeMillis() % 9000L).toInt() + 1000

/** 추적용 CO 포맷 번호(출고 번호/수주 번호). 데모 세션 유일 · 프로덕션=백엔드 발급. */
fun nextCoNo(): String = "CO-2026-%04d".format((coSeq++) % 10000)

private fun nowHHMM(): String {
    val c = java.util.Calendar.getInstance()
    return "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
}

/** 전역 상태 — 셸에서 provide. */
val LocalAppData = staticCompositionLocalOf<AppData> { error("AppData not provided") }

/** 현재 로그인 사용자 — 역할 게이팅(권한·다음 경로)에 사용. */
val LocalMe = staticCompositionLocalOf<CurrentUser> { Seed.USER }

@Composable
fun rememberAppData(): AppData = remember { AppData(Seed.INBOUND, Seed.RECEIVED) }
