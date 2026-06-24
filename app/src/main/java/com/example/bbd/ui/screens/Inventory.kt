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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.BuildConfig
import com.example.bbd.data.InvSummary
import com.example.bbd.data.Part
import com.example.bbd.data.Seed
import com.example.bbd.data.StockStatus
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.repo.InventoryRepository
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.LocalAppData
import com.example.bbd.ui.LocalMe
import com.example.bbd.ui.Nav
import com.example.bbd.ui.PartThumb
import com.example.bbd.ui.SheetHost
import com.example.bbd.ui.StatusPill
import com.example.bbd.ui.SummaryPill
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.bottomBorder
import com.example.bbd.ui.state.ErrorState
import com.example.bbd.ui.state.LoadingRows
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

// 사용자 체감 심각도 순(없음 → 부족 → 정상).
private val SEV = mapOf("없음" to 0, "부족" to 1, "정상" to 2)
private fun sevOf(p: Part) = SEV[p.status.label] ?: 9

@Composable
fun InventoryScreen(nav: Nav, contentPad: PaddingValues = PaddingValues(), initialFilter: String? = null) {
    if (BuildConfig.USE_API) InventoryScreenApi(nav, contentPad, initialFilter)
    else InventoryBody(nav, Seed.PARTS, Seed.INV_SUMMARY, Seed.CATEGORIES, contentPad, initialFilter, apiMode = false)
}

@Composable
private fun InventoryBody(
    nav: Nav,
    parts: List<Part>,
    summary: InvSummary,
    categories: List<String>,
    contentPad: PaddingValues,
    initialFilter: String?,
    apiMode: Boolean,
) {
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(initialFilter ?: "all") } // all/부족/없음/정상/카테고리
    var sel by remember { mutableStateOf<Part?>(null) }

    val list = parts.filter { p ->
        (q.isBlank() || p.name.contains(q) || p.sku.contains(q, ignoreCase = true)) &&
            (filter == "all" ||
                (filter in listOf("부족", "없음", "정상")) && p.status.label == filter ||
                (filter !in listOf("부족", "없음", "정상")) && p.cat == filter)
    }.sortedBy { sevOf(it) }

    Box(Modifier.fillMaxSize().padding(contentPad)) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "재고 조회", back = true, right = HeaderRight.QUEUE, queueCount = nav.queueCount, onBack = { nav.tab("home") }, onRight = nav.openQueue)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            ) {
                // 지점 + 요약
                Row(
                    Modifier.fillMaxWidth().bbdCard().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BbdIcon("pin", 15.dp, T.ink3)
                        // 지점 표기는 실 사용자(LocalMe). 데모/시드 모드에선 LocalMe==Seed.USER 라 외형 불변.
                        // /me 에 지점명이 없으면(미래 API) 빈 라벨 대신 창고 코드만(시드 지점 날조 금지).
                        val meLoc = LocalMe.current
                        if (meLoc.branch.isNotBlank()) {
                            Text("${meLoc.branch} · ", fontSize = 13.sp, color = T.ink2, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                        CodeText((meLoc.branchCode.ifBlank { meLoc.warehouse }), size = 12.5.sp, color = T.ink3Read)
                    }
                    if (apiMode) {
                        Row(
                            Modifier.clip(RoundedCornerShape(999.dp)).background(T.lineSoft).padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            BbdIcon("refresh", 12.dp, T.ink3Read, sw = 2f)
                            Text("DTO 보강 대기", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            SummaryPill(T.amberSoft, T.amberInk, T.amber, "부족 ${summary.short}")
                            SummaryPill(T.redSoft, T.red, T.red, "없음 ${summary.none}")
                        }
                    }
                }
                Spacer(Modifier.size(12.dp))

                // API 모드: DTO 보강 대기 안내(Gap 1)
                if (apiMode) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.amberSoft).border(1.dp, T.amberBlockBorder, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        BbdIcon("info", 18.dp, T.amber, sw = 2f)
                        Text("재고 목록 DTO 보강 대기(Gap 1). 현재고만 표시되며 안전재고·가용재고·상태는 보강 후 표시됩니다.", fontSize = 12.5.sp, color = T.ink2, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.size(12.dp))
                }

                // 검색 — 조회 맥락: 스캔은 부품을 찾아 상세를 엽니다(입고 쓰기 흐름 점프 금지).
                InventorySearch(q, { q = it }) { sel = Seed.partBySku("BBD-OIL-1006") }
                Spacer(Modifier.size(12.dp))

                // 필터 칩 — 가로 스크롤. 선택 칩 = blueSoft/#c9d6f7/blueInk.
                FilterChips(filter, { filter = it }, parts, summary, categories, apiMode)
                Spacer(Modifier.size(14.dp))

                if (filter != "all") {
                    Text("'$filter' 필터 · ${list.size}건 · 심각도순", fontSize = 12.sp, color = T.ink3Read, modifier = Modifier.padding(start = 2.dp, bottom = 10.dp))
                }

                if (list.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 46.dp), contentAlignment = Alignment.Center) {
                        Text("검색 결과가 없습니다.", fontSize = 14.sp, color = T.ink3Read)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        list.forEach { p -> PartRow(p, apiMode) { sel = p } }
                    }
                }
            }
        }

        SheetHost(open = sel != null, onClose = { sel = null }) {
            sel?.let { PartSheetContent(it, nav) { sel = null } }
        }
    }
}

@Composable
private fun InventorySearch(value: String, onValue: (String) -> Unit, onScan: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card)
            .border(1.5.dp, if (focused) T.blue else T.line, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BbdIcon("search", 19.dp, if (focused) T.blue else T.ink3)
        Box(Modifier.weight(1f)) {
            androidx.compose.foundation.text.BasicTextField(
                value = value, onValueChange = onValue, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Pretendard, fontSize = 15.5.sp, color = T.ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(T.blue),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("부품명 또는 코드 검색", color = T.ink3, fontSize = 15.5.sp)
                    inner()
                },
            )
        }
        if (value.isNotEmpty()) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(T.lineSoft).clickable { onValue("") }, contentAlignment = Alignment.Center) {
                BbdIcon("x", 13.dp, T.ink2, sw = 2.2f)
            }
        }
        // 바코드로 부품 찾기 → 상세 열기.
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(999.dp)).background(T.blueSoft).clickable(onClick = onScan), contentAlignment = Alignment.Center) {
            BbdIcon("scan", 18.dp, T.blue)
        }
    }
}

@Composable
private fun FilterChips(filter: String, onPick: (String) -> Unit, parts: List<Part>, summary: InvSummary, categories: List<String>, apiMode: Boolean) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        // 전체/카테고리 카운트는 실데이터(parts·summary)에서 — API 모드도 시드(Seed.PARTS)가 아니라 fetch 결과 기준.
        FilterChip("전체", filter == "all", count = summary.total) { onPick("all") }
        if (!apiMode) {
            FilterChip("정상", filter == "정상", count = summary.ok, icon = "check", iconColor = T.green) { onPick("정상") }
            FilterChip("부족", filter == "부족", count = summary.short, icon = "alert", iconColor = T.amber) { onPick("부족") }
            FilterChip("없음", filter == "없음", count = summary.none, icon = "ban", iconColor = T.red) { onPick("없음") }
        }
        categories.forEach { c ->
            val n = if (apiMode) parts.count { it.cat == c } else null
            FilterChip(c, filter == c, count = n) { onPick(c) }
        }
    }
}

@Composable
private fun FilterChip(label: String, on: Boolean, count: Int? = null, icon: String? = null, iconColor: Color = T.ink3, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (on) T.blueSoft else T.card)
            .border(1.5.dp, if (on) T.hotBorder else T.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) BbdIcon(icon, 14.dp, iconColor, sw = 2f)
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = if (on) T.blueInk else T.ink2, maxLines = 1)
        if (count != null) Text("$count", fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) T.blueInk else T.ink3Read)
    }
}

@Composable
private fun PartRow(p: Part, apiMode: Boolean, onClick: () -> Unit) {
    val urgent = !apiMode && (p.status == StockStatus.NONE || p.status == StockStatus.SHORT)
    val accent = if (p.status == StockStatus.NONE) T.red else T.amber
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(T.card).border(1.dp, T.line, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp),
    ) {
        if (urgent) Box(Modifier.align(Alignment.CenterStart).width(4.dp).height(34.dp).clip(RoundedCornerShape(4.dp)).background(accent))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            PartThumb(p.thumb, active = urgent)
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CodeText(p.sku, size = 12.5.sp, color = T.ink3Read)
                    if (apiMode && p.cat.isNotBlank()) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF2F5FB)).padding(horizontal = 7.dp, vertical = 1.dp)) {
                            Text(p.cat, fontSize = 11.sp, color = T.ink3Read)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${p.qty}", fontFamily = Mono, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = if (!apiMode && p.status == StockStatus.NONE) T.red else T.ink)
                    Text(" ${p.unit}", fontSize = 11.sp, color = T.ink3Read, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.size(7.dp))
                if (apiMode) {
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(T.lineSoft).padding(horizontal = 9.dp, vertical = 3.dp)) {
                        Text("안전재고 —", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                    }
                } else {
                    StatusPill(p.status)
                }
            }
        }
    }
}

// ───────────────────────── 부품 상세 시트 ─────────────────────────

@Composable
private fun PartSheetContent(p: Part, nav: Nav, onClose: () -> Unit) {
    val me = LocalMe.current
    val app = LocalAppData.current
    val ins = Seed.receivedInForSku(p.sku, app.received).take(5)
    val hasLink = app.inbound.any { so -> so.lines.any { it.sku == p.sku } }

    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp)) {
        // 부품 헤더
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PartThumb(p.thumb, size = 54.dp, active = true)
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.3).sp)
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CodeText(p.sku)
                    Text("· ${p.cat}", fontSize = 11.5.sp, color = T.ink3)
                }
            }
        }
        Spacer(Modifier.size(12.dp))

        // 내 창고 재고
        Text("내 창고 재고", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2)
        Spacer(Modifier.size(8.dp))
        Column(Modifier.fillMaxWidth().bbdCard()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    // 실 사용자 창고(LocalMe). 데모/시드는 me==Seed.USER 라 외형 불변.
                    // /me 가 창고명을 안 주면(미래 API) 시드명 대신 이 부품의 창고코드(p.wh)로 표기(날조 금지).
                    Text(me.warehouseName.ifBlank { p.wh }, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.ink)
                    CodeText(me.warehouse.ifBlank { p.wh }, size = 11.5.sp, color = T.ink3Read)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${p.qty}", fontFamily = Mono, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (p.status == StockStatus.NONE) T.red else T.ink)
                        Text(" ${p.unit}", fontSize = 11.sp, color = T.ink3Read, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.size(3.dp))
                    Text("안전재고 ${p.safety}", fontSize = 11.sp, color = T.ink3Read)
                }
            }
            Box(Modifier.padding(start = 15.dp, end = 15.dp, bottom = 14.dp)) { StatusPill(p.status) }
        }
        Spacer(Modifier.size(14.dp))

        // 이 품목 출고 — 해당 SKU 프리필로 출고 폼 직행(§2 컨텍스트 진입).
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(14.dp))
                .clickable { onClose(); nav.pushPreset("scan-out", p) }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BbdIcon("arrowUp", 18.dp, T.blueDeep, sw = 2f)
                Text("이 품목 출고", color = T.blueDeep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.size(18.dp))

        // 최근 입고 (RECEIVED 파생)
        if (ins.isNotEmpty()) {
            Text("최근 입고", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2)
            Spacer(Modifier.size(2.dp))
            Column(Modifier.fillMaxWidth()) {
                ins.forEachIndexed { i, m ->
                    Row(
                        Modifier.fillMaxWidth().then(if (i < ins.lastIndex) Modifier.bottomBorder(T.lineSoft) else Modifier).padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(T.blueSoft), contentAlignment = Alignment.Center) {
                            BbdIcon("arrowDn", 16.dp, T.blue, sw = 2.1f)
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("도착 입고 · ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.ink)
                                CodeText(m.so, size = 12.5.sp)
                            }
                            Spacer(Modifier.size(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${com.example.bbd.data.relDay(m.date)} · ", fontSize = 11.5.sp, color = T.ink3Read)
                                CodeText(m.time, size = 11.5.sp, color = T.ink3Read)
                            }
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("+${m.qty}", fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                            Text(m.unit, fontSize = 11.sp, color = T.ink3Read, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.size(18.dp))
        }

        // 입고 스캔 — 도착 대기 발주 있으면 활성, 없으면 역할별 다음 경로(dead-end 금지).
        if (hasLink) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(T.blue)
                    .clickable {
                        onClose()
                        val so = app.inbound.first { soo -> soo.lines.any { it.sku == p.sku } }
                        nav.pushPreset("scan-in", so)
                    }.padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BbdIcon("scan", 19.dp, Color.White, sw = 2f)
                    Text("이 부품 입고 스캔", color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().bbdCard().background(Color(0xFFFAFBFE)).padding(horizontal = 16.dp, vertical = 15.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    BbdIcon("info", 19.dp, T.ink3Read)
                    Text("이 부품이 포함된 도착 대기 발주가 없어 입고 스캔을 시작할 수 없습니다.", fontSize = 12.5.sp, color = T.ink2, lineHeight = 18.sp)
                }
                Spacer(Modifier.size(13.dp))
                if (me.role == "BRANCH_MANAGER") {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp))
                            .clickable { onClose(); nav.openQueue() }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BbdIcon("doc", 17.dp, T.ink2)
                            Text("발주 요청은 웹 ERP에서", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink)
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(T.card).border(1.dp, T.line, RoundedCornerShape(11.dp)).padding(horizontal = 13.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        BbdIcon("user", 16.dp, T.ink3Read)
                        Text("발주가 필요하면 점장에게 요청하세요.", fontSize = 12.5.sp, color = T.ink2)
                    }
                }
            }
        }
    }
}

// ───────────────────────── 실 API 경로 (USE_API) ─────────────────────────

@Composable
private fun InventoryScreenApi(nav: Nav, contentPad: PaddingValues, initialFilter: String?) {
    val repo = remember { InventoryRepository() }
    // 창고 코드 = 실 사용자(LocalMe)의 warehouse. /me 는 지점·창고를 주지 않으므로 빈값일 수 있음.
    // 빈값이면 전 창고 조회(warehouseCode=null) 폴백 금지 — inventory 가 토큰 스코핑을 안 해 전 지점 재고가
    // 노출됨. 대신 '내 지점 매핑 연동 대기(tenancy)' 안내 상태로 두고 stocks 쿼리를 생략한다.
    val warehouse = LocalMe.current.warehouse
    if (warehouse.isBlank()) {
        InventoryTenancyPending(nav, contentPad)
        return
    }
    var reloadKey by remember { mutableStateOf(0) }
    val state by produceState<UiState<List<Part>>>(UiState.Loading, reloadKey, warehouse) {
        value = UiState.Loading
        value = repo.branchStocks(warehouse)
    }
    when (val st = state) {
        is UiState.Success -> InventoryBody(
            nav, st.data, summaryOf(st.data),
            st.data.map { it.cat }.filter { it.isNotBlank() }.distinct(),
            contentPad, initialFilter, apiMode = true,
        )
        else -> Box(Modifier.fillMaxSize().padding(contentPad)) {
            Column(Modifier.fillMaxSize().background(T.bg)) {
                Header(title = "재고 조회", back = true, right = HeaderRight.QUEUE, queueCount = nav.queueCount, onBack = { nav.tab("home") }, onRight = nav.openQueue)
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
                ) {
                    if (st is UiState.Loading) LoadingRows(5)
                    if (st is UiState.Error) ErrorState(st.message, onRetry = { reloadKey++ })
                }
            }
        }
    }
}

/**
 * 내 창고(warehouse) 미확정 상태 — /me 가 지점·창고(tenancy)를 주지 않음.
 * 전 창고 폴백 금지(inventory 토큰 스코핑 미적용 → 전 지점 노출)이므로 재고 쿼리를 생략하고
 * 안내 상태만 노출('DTO 보강 대기' 배너/카드 스타일 재사용, 문구만 지점-스코핑 대기로).
 */
@Composable
private fun InventoryTenancyPending(nav: Nav, contentPad: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(contentPad)) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "재고 조회", back = true, right = HeaderRight.QUEUE, queueCount = nav.queueCount, onBack = { nav.tab("home") }, onRight = nav.openQueue)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            ) {
                // 지점 미상 + 스코핑 대기 배지(시드 지점값 날조 금지 — 자리만 비움).
                Row(
                    Modifier.fillMaxWidth().bbdCard().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BbdIcon("pin", 15.dp, T.ink3)
                        Text("내 지점 미확정", fontSize = 13.sp, color = T.ink2, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Row(
                        Modifier.clip(RoundedCornerShape(999.dp)).background(T.lineSoft).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        BbdIcon("refresh", 12.dp, T.ink3Read, sw = 2f)
                        Text("지점 스코핑 대기", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                    }
                }
                Spacer(Modifier.size(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.amberSoft).border(1.dp, T.amberBlockBorder, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    BbdIcon("info", 18.dp, T.amber, sw = 2f)
                    Text(
                        "내 지점·창고(tenancy) 매핑 연동 대기. 로그인 신원(/me)에는 지점·창고가 없어 재고를 지점으로 좁힐 수 없습니다. 전 지점 노출을 막기 위해 매핑 연동 후 표시됩니다.",
                        fontSize = 12.5.sp, color = T.ink2, lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

private fun summaryOf(parts: List<Part>): InvSummary = InvSummary(
    total = parts.size,
    short = parts.count { it.status == StockStatus.SHORT },
    none = parts.count { it.status == StockStatus.NONE },
    ok = parts.count { it.status == StockStatus.OK },
)
