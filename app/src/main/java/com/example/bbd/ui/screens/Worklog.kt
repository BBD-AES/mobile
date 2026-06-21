package com.example.bbd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.MoveType
import com.example.bbd.data.Movement
import com.example.bbd.data.Seed
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.Header
import com.example.bbd.ui.MovementRow
import com.example.bbd.ui.Nav
import com.example.bbd.ui.Screen
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.T
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.produceState
import androidx.compose.ui.text.style.TextOverflow
import com.example.bbd.BuildConfig
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.SalesOrderSummaryDto
import com.example.bbd.data.repo.SalesOrderRepository
import com.example.bbd.ui.state.EmptyState
import com.example.bbd.ui.state.ErrorState
import com.example.bbd.ui.state.LoadingRows

@Composable
fun WorklogScreen(nav: Nav, contentPad: PaddingValues = PaddingValues()) {
    if (BuildConfig.USE_API) WorklogScreenApi(nav, contentPad) else WorklogScreenMock(nav, contentPad)
}

@Composable
private fun WorklogScreenMock(nav: Nav, contentPad: PaddingValues) {
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var sel by remember { mutableStateOf<String?>(null) }
    val s = Seed.WORKLOG_SUMMARY

    val list = Seed.WORKLOG.filter { m ->
        (filter == "all" ||
            (filter == "in" && m.type == MoveType.IN) ||
            (filter == "out" && m.type == MoveType.OUT)) &&
            (q.isBlank() || m.name.contains(q) || m.sku.contains(q, ignoreCase = true))
    }
    // 날짜별 그룹 (목록 순서 유지)
    val groups = linkedMapOf<String, MutableList<Movement>>()
    list.forEach { groups.getOrPut(it.day) { mutableListOf() }.add(it) }

    // 작업이력은 탭 루트 → 헤더 백/벨 없음(벨 no-op 제거).
    Screen(contentPad = contentPad, header = { Header(title = "내 작업 이력") }) {
        SearchField(q, { q = it }, "부품명 또는 코드", showScan = false)
        Spacer(Modifier.size(12.dp))

        // 필터 칩 — 칩 카운트는 30일 전체 요약(WORKLOG_SUMMARY) 기준(디자인 핸드오프 명세).
        // 아래 목록과 "총 N건"은 현재 시드된 샘플 이동만 표시(운영에선 API가 30일치 전부 반환).
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            WChip("전체", null, s.total, filter == "all") { filter = "all" }
            WChip("입고", "arrowDn", s.inN, filter == "in") { filter = "in" }
            WChip("출고", "arrowUp", s.outN, filter == "out") { filter = "out" }
        }
        Spacer(Modifier.size(13.dp))

        // 기간 (최근 30일 고정 — 기간 직접 선택은 미구현이라 노출하지 않음)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(999.dp)).background(T.blueSoft).padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                BbdIcon("cal", 15.dp, T.blue)
                Text("최근 30일", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.blueInk)
            }
        }
        Spacer(Modifier.size(6.dp))
        Row {
            Text("${s.from} ~ ${s.to} · 총 ", fontSize = 12.sp, color = T.ink3, fontFamily = Mono)
            Text("${list.size}건", fontSize = 12.sp, color = T.ink2, fontFamily = Mono, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(14.dp))

        if (groups.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 50.dp), contentAlignment = Alignment.Center) {
                Text("검색 결과가 없습니다.", fontSize = 14.sp, color = T.ink3)
            }
        } else {
            groups.forEach { (day, items) ->
                Text(day, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = T.ink3, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { m ->
                        val key = m.sku + m.time
                        val selected = sel == key
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(T.card)
                                .border(if (selected) 1.5.dp else 1.dp, if (selected) T.blue else T.line, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp),
                        ) {
                            MovementRow(m, showDay = false, onClick = { sel = if (selected) null else key })
                        }
                    }
                }
                Spacer(Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun WChip(label: String, icon: String?, count: Int, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) T.blue else T.card)
            .border(1.5.dp, if (on) T.blue else T.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) BbdIcon(icon, 15.dp, if (on) Color.White else T.ink3, sw = 2f)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else T.ink2)
        Text("$count", fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else T.ink3)
    }
}

// ───────────────────────── 실 API 경로 (USE_API) ─────────────────────────
// sales GET /api/v1/sales-orders?received_by={emp} 로 내가 입고 확인한 발주 로드.
// 요약 응답엔 라인이 없어 주문 단위 행으로 렌더(부품 단위 추측 금지). 인증 토큰 필요.

@Composable
private fun WorklogScreenApi(nav: Nav, contentPad: PaddingValues) {
    val repo = remember { SalesOrderRepository() }
    var reloadKey by remember { mutableStateOf(0) }
    val state by produceState<UiState<List<SalesOrderSummaryDto>>>(UiState.Loading, reloadKey) {
        value = UiState.Loading
        value = repo.receivedByMe(Seed.USER.emp)
    }
    Screen(contentPad = contentPad, header = { Header(title = "내 작업 이력") }) {
        when (val s = state) {
            is UiState.Loading -> LoadingRows()
            is UiState.Error -> ErrorState(s.message, onRetry = { reloadKey++ })
            is UiState.Success -> {
                val items = s.data
                if (items.isEmpty()) {
                    EmptyState("list", "입고 확인 기록이 없습니다", "도착 발주를 입고 확인하면 여기에 표시됩니다.")
                } else {
                    Row {
                        Text("내가 입고 확인한 발주 · 총 ", fontSize = 12.sp, color = T.ink3, fontFamily = Mono)
                        Text("${items.size}건", fontSize = 12.sp, color = T.ink2, fontFamily = Mono, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.size(14.dp))
                    val groups = linkedMapOf<String, MutableList<SalesOrderSummaryDto>>()
                    items.forEach { o ->
                        val day = (o.receivedAt ?: "").take(10).ifBlank { "-" }
                        groups.getOrPut(day) { mutableListOf() }.add(o)
                    }
                    groups.forEach { (day, rows) ->
                        Text(day, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = T.ink3, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            rows.forEach { WorklogApiRow(it) }
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorklogApiRow(o: SalesOrderSummaryDto) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(T.card)
            .border(1.dp, T.line, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(T.blueSoft), contentAlignment = Alignment.Center) {
            BbdIcon("arrowDn", 18.dp, T.blue, sw = 2f)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(o.soNumber ?: "-", fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink)
                Text("입고완료", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = T.green)
            }
            Spacer(Modifier.size(3.dp))
            Text(o.toWarehouseName ?: o.toWarehouseCode ?: "—", fontSize = 12.sp, color = T.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(if (o.totalAmount == null) "—" else "%,d원".format(o.totalAmount.toLong()), fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
            val time = (o.receivedAt ?: "").drop(11).take(5)
            if (time.isNotBlank()) {
                Spacer(Modifier.size(2.dp))
                Text(time, fontFamily = Mono, fontSize = 11.sp, color = T.ink3)
            }
        }
    }
}
