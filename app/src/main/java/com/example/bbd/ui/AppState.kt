package com.example.bbd.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.bbd.data.CurrentUser
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.Seed

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

    /** 도착 확인(입고 확정) — inbound 에서 제거 → received 맨 앞 추가. 같은 발주 중복 방지. */
    fun confirmReceive(soNumber: String) {
        val hit = inbound.firstOrNull { it.so == soNumber } ?: return
        inbound = inbound.filterNot { it.so == soNumber }
        val now = nowHHMM()
        val moved = hit.copy(status = "RECEIVED", date = Seed.DEMO_TODAY, time = now)
        if (received.none { it.so == soNumber }) received = listOf(moved) + received
    }

    /** 당겨서 새로고침 / 헤더 refresh — 시드 모드에선 타임스탬프만 갱신. */
    fun refresh() {
        refreshing = true
        lastRefresh = System.currentTimeMillis()
        refreshing = false
    }
}

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
