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
import com.example.bbd.data.repo.ItemRepository
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
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
    // 상태(정상/부족/없음)와 카테고리를 독립 필터로 분리(AND 결합). 홈 딥링크 initialFilter 는 상태값이라 statusFilter 로.
    var statusFilter by remember { mutableStateOf(if (initialFilter in listOf("부족", "없음", "정상")) initialFilter!! else "all") }
    var catFilter by remember { mutableStateOf("all") }
    var sel by remember { mutableStateOf<Part?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val scanRepo = remember { InventoryRepository() }
    val scanItemRepo = remember { ItemRepository() }
    val meWh = LocalMe.current.warehouse
    // 부품 바코드 스캔 → 해석되면 상세 열기, 미해석이면 '미등록' 토스트. API=실 재고(resolvePart), 시드=카탈로그.
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val code = result.contents?.trim()?.uppercase() ?: return@rememberLauncherForActivityResult
        if (apiMode) {
            scope.launch {
                val p = (scanRepo.resolvePart(meWh, code) as? UiState.Success)?.data
                if (p != null) sel = p
                else {
                    // 내 창고 재고엔 없음 — 품목 마스터를 확인해 '미등록'(카탈로그에 없음)과 '이 창고에 재고 없음'(타 창고엔 있을 수 있음)을 구분.
                    val inMaster = (scanItemRepo.resolve(code) as? UiState.Success)?.data != null
                    val msg = if (inMaster) "이 창고에 재고가 없는 부품입니다 ($code)" else "미등록 부품입니다 ($code)"
                    android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } else {
            val p = Seed.partBySku(code)
            if (p != null) sel = p
            else android.widget.Toast.makeText(ctx, "미등록 부품: $code", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val list = parts.filter { p ->
        (q.isBlank() || p.name.contains(q) || p.sku.contains(q, ignoreCase = true)) &&
            (statusFilter == "all" || p.status.label == statusFilter) &&
            (catFilter == "all" || p.cat == catFilter)
    }.sortedBy { sevOf(it) }

    Box(Modifier.fillMaxSize().padding(contentPad)) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "재고 조회", back = false, right = HeaderRight.QUEUE, queueCount = nav.queueCount, onBack = { nav.tab("home") }, onRight = nav.openQueue)
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
                    // 부족·없음 요약 — 안전재고가 목록 DTO 에 포함돼(시드·API parity) 양 모드 동일 표시.
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        SummaryPill(T.amberSoft, T.amberInk, T.amber, "부족 ${summary.short}")
                        SummaryPill(T.redSoft, T.red, T.red, "없음 ${summary.none}")
                    }
                }
                Spacer(Modifier.size(12.dp))

                // 검색 — 조회 맥락: 스캔은 부품을 찾아 상세를 엽니다(입고 쓰기 흐름 점프 금지).
                InventorySearch(q, { q = it }) {
                    scanLauncher.launch(ScanOptions().apply { setPrompt("부품 바코드를 맞춰 주세요"); setBeepEnabled(true); setOrientationLocked(true) })
                }
                Spacer(Modifier.size(12.dp))

                // 필터 — 상태(정상/부족/없음)와 카테고리를 분리한 2단 칩. 각 줄 단일선택, 둘은 AND.
                FilterChips(statusFilter, { statusFilter = it }, catFilter, { catFilter = it }, parts, summary, categories, apiMode)
                Spacer(Modifier.size(14.dp))

                val activeLabel = listOfNotNull(
                    statusFilter.takeIf { it != "all" },
                    catFilter.takeIf { it != "all" },
                ).joinToString(" · ")
                if (activeLabel.isNotEmpty()) {
                    Text("$activeLabel · ${list.size}건 · 심각도순", fontSize = 12.sp, color = T.ink3Read, modifier = Modifier.padding(start = 2.dp, bottom = 10.dp))
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
private fun FilterChips(
    status: String, onStatus: (String) -> Unit,
    cat: String, onCat: (String) -> Unit,
    parts: List<Part>, summary: InvSummary, categories: List<String>, apiMode: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        // 상태 필터 — 안전재고 기반(정상/부족/없음). 안전재고가 목록 DTO 에 포함돼 시드·API 동일(클라 필터=곧 '안전재고 미만' 뷰).
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            FilterChip("전체", status == "all", count = summary.total) { onStatus("all") }
            FilterChip("정상", status == "정상", count = summary.ok, icon = "check", iconColor = T.green) { onStatus("정상") }
            FilterChip("부족", status == "부족", count = summary.short, icon = "alert", iconColor = T.amber) { onStatus("부족") }
            FilterChip("없음", status == "없음", count = summary.none, icon = "ban", iconColor = T.red) { onStatus("없음") }
        }
        // 카테고리 필터 — 별도 줄.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            FilterChip("전체", cat == "all", count = if (apiMode) summary.total else null) { onCat("all") }
            categories.forEach { c ->
                val n = if (apiMode) parts.count { it.cat == c } else null
                FilterChip(c, cat == c, count = n) { onCat(c) }
            }
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
    val urgent = p.status == StockStatus.NONE || p.status == StockStatus.SHORT
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
                    Text("${p.qty}", fontFamily = Mono, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = if (p.status == StockStatus.NONE) T.red else T.ink)
                    Text(" ${p.unit}", fontSize = 11.sp, color = T.ink3Read, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.size(7.dp))
                StatusPill(p.status)
            }
        }
    }
}

// ───────────────────────── 부품 상세 시트 ─────────────────────────

@Composable
private fun PartSheetContent(p: Part, nav: Nav, onClose: () -> Unit) {
    val me = LocalMe.current
    val app = LocalAppData.current
    // 이 부품이 든 도착 예정(IN_FULFILLMENT, 이동 중) 발주 — 운영자에게 '곧 들어올 양'을 보여준다.
    val incoming = app.inbound.mapNotNull { so -> so.lines.firstOrNull { it.sku == p.sku }?.let { so to it } }.take(3)

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

        // 도착 예정 (IN_FULFILLMENT 파생) — 이 부품이 든 이동 중 발주. ETA/지연 표기 금지(중립 '이동 중'만).
        if (incoming.isNotEmpty()) {
            Text("도착 예정", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2)
            Spacer(Modifier.size(2.dp))
            Column(Modifier.fillMaxWidth()) {
                incoming.forEachIndexed { i, (so, line) ->
                    Row(
                        Modifier.fillMaxWidth().then(if (i < incoming.lastIndex) Modifier.bottomBorder(T.lineSoft) else Modifier).padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(T.blueSoft), contentAlignment = Alignment.Center) {
                            BbdIcon("truck", 17.dp, T.blue, sw = 2f)
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("이동 중 · ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.ink)
                                CodeText(so.so, size = 12.5.sp)
                            }
                            Spacer(Modifier.size(1.dp))
                            Text("${so.fromWh}에서 출고", fontSize = 11.5.sp, color = T.ink3Read)
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${line.qty}", fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = T.blue)
                            Text(line.unit, fontSize = 11.sp, color = T.ink3Read, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.size(18.dp))
        }

        // (입고는 SO 전량 일괄 모델 — 부품 단위 입고 스캔 없음. 입고는 도착 대기 큐/입고 스캔 탭에서.
        //  부품 상세는 정보(재고 + 도착 예정) + '이 품목 출고'만 둔다.)
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
                Header(title = "재고 조회", back = false, right = HeaderRight.QUEUE, queueCount = nav.queueCount, onBack = { nav.tab("home") }, onRight = nav.openQueue)
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
            Header(title = "재고 조회", back = false, right = HeaderRight.QUEUE, queueCount = nav.queueCount, onBack = { nav.tab("home") }, onRight = nav.openQueue)
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

internal fun summaryOf(parts: List<Part>): InvSummary = InvSummary(
    total = parts.size,
    short = parts.count { it.status == StockStatus.SHORT },
    none = parts.count { it.status == StockStatus.NONE },
    ok = parts.count { it.status == StockStatus.OK },
)
