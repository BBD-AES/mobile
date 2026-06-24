package com.example.bbd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.daysSince
import com.example.bbd.data.relDay
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.Header
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.LocalAppData
import com.example.bbd.ui.LocalMe
import com.example.bbd.ui.Nav
import com.example.bbd.ui.SoDetailSheet
import com.example.bbd.ui.SoRow
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.state.EmptyState
import com.example.bbd.ui.state.RowSkeleton
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T
import kotlinx.coroutines.delay

@Composable
fun WorklogScreen(nav: Nav, contentPad: PaddingValues = PaddingValues()) {
    if (com.example.bbd.BuildConfig.USE_API) WorklogScreenApi(nav, contentPad)
    else WorklogScreenSeed(nav, contentPad)
}

@Composable
private fun WorklogScreenSeed(nav: Nav, contentPad: PaddingValues) {
    val me = LocalMe.current
    val app = LocalAppData.current
    var q by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(90) }
    var sel by remember { mutableStateOf<SalesOrder?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(550); loading = false }

    val list = app.received.filter { so ->
        ((daysSince(so.date) ?: 0L) <= period) &&
            (q.isBlank() || so.so.contains(q, ignoreCase = true) || so.fromWh.contains(q) ||
                so.lines.any { it.name.contains(q) || it.sku.contains(q, ignoreCase = true) })
    }
    val groups = linkedMapOf<String, MutableList<SalesOrder>>()
    list.forEach { groups.getOrPut(relDay(it.date)) { mutableListOf() }.add(it) }
    val periods = listOf(30 to "최근 30일", 90 to "90일", 365 to "1년")

    Box(Modifier.fillMaxSize().padding(contentPad)) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(
                title = "내 작업 이력", back = false, right = HeaderRight.QUEUE, queueCount = nav.queueCount,
                onRefresh = { app.refresh() }, refreshing = app.refreshing, lastRefresh = app.lastRefresh,
                // 작업이력은 탭 루트라 pop() 이 무동작 → 재고/마이 탭과 동일하게 홈 탭으로.
                onBack = { nav.tab("home") }, onRight = nav.openQueue,
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            ) {
                WorklogSearch(q) { q = it }
                Spacer(Modifier.size(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    periods.forEach { (id, label) ->
                        PeriodChip(label, period == id) { period = id }
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    buildAnnotatedString {
                        append("최근 ${period}일 · 총 ")
                        withStyle(androidx.compose.ui.text.SpanStyle(color = T.ink2, fontWeight = FontWeight.Bold)) { append("${list.size}건") }
                        append(" 입고 확인")
                    },
                    fontSize = 12.5.sp, color = T.ink3Read, modifier = Modifier.padding(start = 2.dp),
                )
                Spacer(Modifier.size(14.dp))

                when {
                    loading -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { repeat(4) { RowSkeleton() } }
                    groups.isEmpty() && q.isNotBlank() -> EmptyState(
                        "search", "검색 결과가 없습니다", "'$q'에 해당하는 입고 확인 기록을 찾지 못했습니다.",
                        action = { GhostAction("검색 초기화") { q = "" } },
                    )
                    groups.isEmpty() -> EmptyState(
                        "list", "입고 확인 기록이 없습니다", "도착 대기 발주를 입고 확인하면 여기에 기록됩니다.",
                        action = { SolidScanAction { nav.scan() } },
                    )
                    else -> groups.forEach { (day, items) ->
                        Text(day, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = T.ink3Read, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items.forEach { so ->
                                Box(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(T.card)
                                        .border(if (sel?.so == so.so) 1.5.dp else 1.dp, if (sel?.so == so.so) T.blue else T.line, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp),
                                ) {
                                    SoRow(so, toShort = me.warehouseName.removeSuffix(" 창고"), onClick = { sel = so })
                                }
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                }
            }
        }

        SoDetailSheet(sel, me.name, me.warehouseName, onClose = { sel = null })
    }
}

@Composable
private fun WorklogSearch(value: String, onValue: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card)
            .border(1.5.dp, if (focused) T.blue else T.line, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BbdIcon("search", 19.dp, if (focused) T.blue else T.ink3Read)
        Box(Modifier.weight(1f)) {
            androidx.compose.foundation.text.BasicTextField(
                value = value, onValueChange = onValue, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Pretendard, fontSize = 15.5.sp, color = T.ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(T.blue),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                decorationBox = { inner -> if (value.isEmpty()) Text("발주번호 · 부품명 · 코드", color = T.ink3, fontSize = 15.5.sp); inner() },
            )
        }
        if (value.isNotEmpty()) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(T.lineSoft).clickable { onValue("") }, contentAlignment = Alignment.Center) {
                BbdIcon("x", 14.dp, T.ink2, sw = 2.2f)
            }
        }
    }
}

@Composable
private fun PeriodChip(label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) T.blueSoft else T.card)
            .border(1.5.dp, if (on) T.hotBorder else T.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (on) BbdIcon("cal", 15.dp, T.blueInk)
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = if (on) T.blueInk else T.ink2)
    }
}

@Composable
private fun GhostAction(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 11.dp),
    ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink2) }
}

@Composable
private fun SolidScanAction(onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(13.dp)).background(T.blue).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        BbdIcon("scan", 17.dp, Color.White, sw = 2f)
        Text("입고 스캔", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

// ───────────────────────── 실 API 경로 (USE_API) ─────────────────────────
// sales GET /api/v1/sales-orders?received_by={emp} → 내가 입고 확인한 발주(요약 DTO).
// 요약엔 라인이 없어 주문 단위 행으로 렌더(§3). StateGate 로딩/에러/빈 일관 적용.

@Composable
private fun WorklogScreenApi(nav: Nav, contentPad: PaddingValues) {
    val me = LocalMe.current
    val app = LocalAppData.current
    val repo = remember { com.example.bbd.data.repo.SalesOrderRepository() }
    var reloadKey by remember { mutableStateOf(0) }
    // received_by 는 실 사번(me.emp) — 시드 사번 쓰지 않음. emp 가 비면(/me 신원 미확정) 조회 가드.
    val state by androidx.compose.runtime.produceState<com.example.bbd.data.remote.UiState<List<com.example.bbd.data.remote.dto.SalesOrderSummaryDto>>>(
        com.example.bbd.data.remote.UiState.Loading, reloadKey, me.emp,
    ) {
        value = com.example.bbd.data.remote.UiState.Loading
        value = if (me.emp.isBlank())
            com.example.bbd.data.remote.UiState.Error("사용자 신원(사번)을 확인하지 못해 작업 이력을 불러올 수 없습니다.")
        else
            repo.receivedByMe(me.emp)
    }
    Box(Modifier.fillMaxSize().padding(contentPad)) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(
                title = "내 작업 이력", back = false, right = HeaderRight.QUEUE, queueCount = nav.queueCount,
                onRefresh = { reloadKey++ }, lastRefresh = app.lastRefresh,
                onBack = { nav.pop() }, onRight = nav.openQueue,
            )
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp)) {
                com.example.bbd.ui.state.StateGate(
                    state = state,
                    onRetry = { reloadKey++ },
                    empty = { EmptyState("list", "입고 확인 기록이 없습니다", "도착 대기 발주를 입고 확인하면 여기에 기록됩니다.") },
                ) { items ->
                    Text("내가 입고 확인한 발주 · 총 ${items.size}건", fontSize = 12.5.sp, color = T.ink3Read, modifier = Modifier.padding(start = 2.dp, bottom = 14.dp))
                    val groups = linkedMapOf<String, MutableList<com.example.bbd.data.remote.dto.SalesOrderSummaryDto>>()
                    items.forEach { o -> groups.getOrPut((o.receivedAt ?: "").take(10).ifBlank { "-" }) { mutableListOf() }.add(o) }
                    Column {
                        groups.forEach { (day, rows) ->
                            Text(day, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = T.ink3Read, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { rows.forEach { WorklogApiRow(it) } }
                            Spacer(Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorklogApiRow(o: com.example.bbd.data.remote.dto.SalesOrderSummaryDto) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(T.card).border(1.dp, T.line, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(T.greenSoft), contentAlignment = Alignment.Center) {
            BbdIcon("check", 17.dp, T.green, sw = 2f)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.bbd.ui.CodeText(o.soNumber ?: "-", size = 13.5.sp, color = T.ink, weight = FontWeight.SemiBold)
                Text("입고 완료", fontSize = 11.5.sp, color = T.greenInk, fontWeight = FontWeight.Bold)
                Box(Modifier.clip(RoundedCornerShape(5.dp)).background(T.lineSoft).padding(horizontal = 6.dp, vertical = 1.dp)) {
                    Text("SO", fontFamily = com.example.bbd.ui.theme.Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                }
            }
            Spacer(Modifier.size(3.dp))
            Text(o.toWarehouseName ?: o.toWarehouseCode ?: "—", fontSize = 12.5.sp, color = T.ink2, maxLines = 1)
        }
        val time = (o.receivedAt ?: "").drop(11).take(5)
        if (time.isNotBlank()) com.example.bbd.ui.CodeText(time, size = 12.sp, color = T.ink2)
    }
}
