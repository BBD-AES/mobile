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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.Seed
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.relDay
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.SalesOrderSummaryDto
import com.example.bbd.data.repo.SalesOrderRepository
import com.example.bbd.data.totals
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.LocalAppData
import com.example.bbd.ui.LocalMe
import com.example.bbd.ui.Nav
import com.example.bbd.ui.SoBadge
import com.example.bbd.ui.SoDetailSheet
import com.example.bbd.ui.state.EmptyState
import com.example.bbd.ui.state.RowSkeleton
import com.example.bbd.ui.state.StateGate
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────
// 이동요청 이력 — "내 입고"가 아닌 지점 전체 재고이동요청(STR)을 상태 무관 전체로.
// 요청자/입고자/취소자(책임 추적)와 상태 배지를 보여주는 운영 이력. 상태 필터로 좁힌다.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun WorklogScreen(nav: Nav, contentPad: PaddingValues = PaddingValues()) {
    if (com.example.bbd.BuildConfig.USE_API) WorklogScreenApi(nav, contentPad)
    else WorklogScreenSeed(nav, contentPad)
}

/** 상태 필터 버킷 — null=전체. 취소·반려는 '종료'로 묶음. */
private data class StatusFilter(val id: String, val label: String, val statuses: Set<String>?) {
    fun match(s: String?) = statuses == null || (s != null && s in statuses)
}

private val STATUS_FILTERS = listOf(
    StatusFilter("ALL", "전체", null),
    StatusFilter("IN_FULFILLMENT", "도착 대기", setOf("IN_FULFILLMENT")),
    StatusFilter("BACKORDERED", "백오더", setOf("BACKORDERED")),
    StatusFilter("RECEIVED", "입고 완료", setOf("RECEIVED")),
    StatusFilter("CLOSED", "취소·반려", setOf("CANCELED", "REJECTED")),
)

/** 진행 중(도착 대기·백오더)을 위로, 그 외는 최신 날짜순. */
private fun histRank(status: String?) = when (status) {
    "IN_FULFILLMENT" -> 0
    "BACKORDERED" -> 1
    else -> 2
}

private data class HistVis(val icon: String, val tint: Color, val soft: Color)

private fun histVis(status: String?): HistVis = when (status) {
    "IN_FULFILLMENT" -> HistVis("truck", T.blue, T.blueSoft)
    "RECEIVED" -> HistVis("check", T.green, T.greenSoft)
    "BACKORDERED" -> HistVis("clock", T.amber, T.amberSoft)
    "REJECTED" -> HistVis("x", T.red, T.redSoft)
    "CANCELED" -> HistVis("ban", T.ink3, T.lineSoft)
    else -> HistVis("doc", T.ink3, T.lineSoft)
}

/** 상태별 책임자(라벨, 이름) — 없으면 null. */
private fun histActor(so: SalesOrder): Pair<String, String>? = when (so.status) {
    "RECEIVED" -> so.receivedBy.takeIf { it.isNotBlank() }?.let { "입고" to it }
    "CANCELED" -> so.canceledBy.takeIf { it.isNotBlank() }?.let { "취소" to it }
    else -> so.requestedBy.takeIf { it.isNotBlank() }?.let { "요청" to it }
}

private fun apiActor(o: SalesOrderSummaryDto): Pair<String, String>? = when (o.status) {
    "RECEIVED" -> o.receivedBy?.takeIf { it.isNotBlank() }?.let { "입고" to it }
    "CANCELED" -> o.canceledBy?.takeIf { it.isNotBlank() }?.let { "취소" to it }
    else -> o.requestedBy?.takeIf { it.isNotBlank() }?.let { "요청" to it }
}

private fun apiDay(o: SalesOrderSummaryDto): String =
    (o.receivedAt ?: o.canceledAt ?: o.approvedAt ?: o.requestedAt ?: "").take(10)

// ───────────────────────── 시드 경로 ─────────────────────────

@Composable
private fun WorklogScreenSeed(nav: Nav, contentPad: PaddingValues) {
    val me = LocalMe.current
    val app = LocalAppData.current
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    var sel by remember { mutableStateOf<SalesOrder?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(550); loading = false }

    val all = remember(app.inbound, app.received) {
        (app.inbound + app.received + Seed.HISTORY_EXTRA)
            .sortedWith(compareBy<SalesOrder> { histRank(it.status) }.thenByDescending { it.date })
    }
    val bucket = STATUS_FILTERS.first { it.id == filter }
    val list = all.filter { so ->
        bucket.match(so.status) &&
            (q.isBlank() || so.so.contains(q, ignoreCase = true) || so.fromWh.contains(q) ||
                so.requestedBy.contains(q) || so.receivedBy.contains(q) || so.canceledBy.contains(q) ||
                so.lines.any { it.name.contains(q) || it.sku.contains(q, ignoreCase = true) })
    }

    Box(Modifier.fillMaxSize().padding(contentPad)) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(
                title = "이동요청 이력", back = false, right = HeaderRight.QUEUE, queueCount = nav.queueCount,
                onRefresh = { app.refresh() }, refreshing = app.refreshing, lastRefresh = app.lastRefresh,
                onBack = { nav.tab("home") }, onRight = nav.openQueue,
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            ) {
                WorklogSearch(q) { q = it }
                Spacer(Modifier.size(12.dp))
                StatusFilterRow(filter) { filter = it }
                Spacer(Modifier.size(10.dp))
                Text(
                    buildAnnotatedString {
                        append("지점 전체 · 총 ")
                        withStyle(androidx.compose.ui.text.SpanStyle(color = T.ink2, fontWeight = FontWeight.Bold)) { append("${list.size}건") }
                    },
                    fontSize = 12.5.sp, color = T.ink3Read, modifier = Modifier.padding(start = 2.dp),
                )
                Spacer(Modifier.size(14.dp))

                when {
                    loading -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { repeat(4) { RowSkeleton() } }
                    list.isEmpty() && q.isNotBlank() -> EmptyState(
                        "search", "검색 결과가 없습니다", "'$q'에 해당하는 이동요청을 찾지 못했습니다.",
                        action = { GhostAction("검색 초기화") { q = "" } },
                    )
                    list.isEmpty() && filter != "ALL" -> EmptyState(
                        "list", "해당 상태가 없습니다", "'${bucket.label}' 상태의 이동요청이 없습니다.",
                        action = { GhostAction("전체 보기") { filter = "ALL" } },
                    )
                    list.isEmpty() -> EmptyState(
                        "list", "이동요청 이력이 없습니다", "지점의 재고이동요청이 생기면 여기에 모입니다.",
                        action = { SolidScanAction { nav.scan() } },
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        list.forEach { so -> HistoryRow(so, selected = sel?.so == so.so) { sel = so } }
                    }
                }
            }
        }
        SoDetailSheet(sel, me.name, me.warehouseName, onClose = { sel = null })
    }
}

@Composable
private fun HistoryRow(so: SalesOrder, selected: Boolean, onClick: () -> Unit) {
    val vis = histVis(so.status)
    val tot = so.totals()
    val actor = histActor(so)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) T.blue else T.line, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(vis.soft), contentAlignment = Alignment.Center) {
            BbdIcon(vis.icon, 18.dp, vis.tint, sw = 2f)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CodeText(so.so, size = 13.5.sp, color = T.ink, weight = FontWeight.SemiBold)
                SoBadge(so.status)
            }
            Spacer(Modifier.size(3.dp))
            Text(
                buildString {
                    if (actor != null) append("${actor.first} ${actor.second} · ")
                    append(so.fromWh)
                },
                fontSize = 12.sp, color = T.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (so.date.isNotBlank()) {
                Text(relDay(so.date), fontSize = 11.5.sp, color = T.ink3Read)
                Spacer(Modifier.size(2.dp))
                Text(
                    "${tot.items}품목 ${tot.qty}${if (tot.unit.isNotEmpty()) " ${tot.unit}" else ""}",
                    fontFamily = Mono, fontSize = 11.sp, color = T.ink3Read,
                )
            } else {
                Text("진행 중", fontSize = 11.5.sp, color = vis.tint, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ───────────────────────── 실 API 경로 (USE_API) ─────────────────────────
// sales GET /api/v1/sales-orders?to_warehouse_code={내 창고} → 지점 전체 이동요청(상태 무관).
// 서버가 BRANCH 를 자기 지점으로 자동 스코프하고 응답에 requested/received/canceledBy 를 준다.

@Composable
private fun WorklogScreenApi(nav: Nav, contentPad: PaddingValues) {
    val me = LocalMe.current
    val app = LocalAppData.current
    val repo = remember { SalesOrderRepository() }
    var reloadKey by remember { mutableStateOf(0) }
    var filter by remember { mutableStateOf("ALL") }
    // to_warehouse_code 는 실 창고코드(me.warehouse). 비면(/me 매핑 미확정) 조회 가드.
    val state by produceState<UiState<List<SalesOrderSummaryDto>>>(UiState.Loading, reloadKey, me.warehouse) {
        value = UiState.Loading
        value = if (me.warehouse.isBlank())
            UiState.Error("지점 창고를 확인하지 못해 이동요청 이력을 불러올 수 없습니다.")
        else
            repo.branchOrders(me.warehouse)
    }
    val bucket = STATUS_FILTERS.first { it.id == filter }

    Box(Modifier.fillMaxSize().padding(contentPad)) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(
                title = "이동요청 이력", back = false, right = HeaderRight.QUEUE, queueCount = nav.queueCount,
                onRefresh = { reloadKey++ }, lastRefresh = app.lastRefresh,
                onBack = { nav.tab("home") }, onRight = nav.openQueue,
            )
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp)) {
                StatusFilterRow(filter) { filter = it }
                Spacer(Modifier.size(12.dp))
                StateGate(
                    state = state,
                    onRetry = { reloadKey++ },
                    empty = { EmptyState("list", "이동요청 이력이 없습니다", "지점의 재고이동요청이 생기면 여기에 모입니다.") },
                ) { items ->
                    val list = items.filter { bucket.match(it.status) }
                    Text("지점 이동요청 · 총 ${list.size}건", fontSize = 12.5.sp, color = T.ink3Read, modifier = Modifier.padding(start = 2.dp, bottom = 14.dp))
                    if (list.isEmpty()) {
                        Text("'${bucket.label}' 상태의 이동요청이 없습니다.", fontSize = 13.sp, color = T.ink3Read, modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { list.forEach { HistoryApiRow(it) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryApiRow(o: SalesOrderSummaryDto) {
    val status = o.status ?: ""
    val vis = histVis(status)
    val actor = apiActor(o)
    val day = apiDay(o)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card).border(1.dp, T.line, RoundedCornerShape(13.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(vis.soft), contentAlignment = Alignment.Center) {
            BbdIcon(vis.icon, 18.dp, vis.tint, sw = 2f)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CodeText(o.soNumber ?: "-", size = 13.5.sp, color = T.ink, weight = FontWeight.SemiBold)
                SoBadge(status)
            }
            Spacer(Modifier.size(3.dp))
            Text(
                buildString {
                    if (actor != null) append("${actor.first} ${actor.second} · ")
                    append(o.toWarehouseName ?: o.toWarehouseCode ?: "—")
                },
                fontSize = 12.sp, color = T.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (day.isNotBlank()) CodeText(day, size = 11.5.sp, color = T.ink3Read)
    }
}

// ───────────────────────── 공용 UI 조각 ─────────────────────────

@Composable
private fun StatusFilterRow(active: String, onPick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        STATUS_FILTERS.forEach { f -> StatusChip(f.label, active == f.id) { onPick(f.id) } }
    }
}

@Composable
private fun StatusChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) T.blueSoft else T.card)
            .border(1.5.dp, if (on) T.hotBorder else T.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) T.blueInk else T.ink2)
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
                decorationBox = { inner -> if (value.isEmpty()) Text("요청번호 · 부품 · 담당자", color = T.ink3, fontSize = 15.5.sp); inner() },
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
