package com.example.bbd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import com.example.bbd.data.Movement
import com.example.bbd.data.MoveType
import com.example.bbd.data.Part
import com.example.bbd.data.Seed
import com.example.bbd.data.StockStatus
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.MovementRow
import com.example.bbd.ui.Nav
import com.example.bbd.ui.PartThumb
import com.example.bbd.ui.SheetHost
import com.example.bbd.ui.StatusPill
import com.example.bbd.ui.SummaryPill
import com.example.bbd.ui.TabBar
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

@Composable
fun InventoryScreen(nav: Nav) {
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var sel by remember { mutableStateOf<Part?>(null) }
    val s = Seed.INV_SUMMARY

    val list = Seed.PARTS.filter { p ->
        (q.isBlank() || p.name.contains(q) || p.sku.contains(q, ignoreCase = true)) &&
            (filter == "all" ||
                (filter in listOf("부족", "없음", "정상") && p.status.label == filter) ||
                (filter !in listOf("부족", "없음", "정상") && p.cat == filter))
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "재고 조회", back = true, right = HeaderRight.BELL, onBack = { nav.tab("home") })
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
                        Text("${Seed.USER.branch} · ", fontSize = 13.sp, color = T.ink2, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        CodeText(Seed.USER.branchCode, size = 12.5.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        SummaryPill(T.blueSoft, T.blueInk, T.blue, "부족 ${s.short}건")
                        SummaryPill(T.redSoft, T.red, T.red, "없음 ${s.none}건")
                    }
                }
                Spacer(Modifier.size(12.dp))

                // 검색
                SearchField(q, { q = it }, "부품명 또는 코드 검색", showScan = true)
                Spacer(Modifier.size(14.dp))

                // 필터 칩
                FilterChips(filter, s) { filter = it }
                Spacer(Modifier.size(14.dp))

                // 리스트
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 46.dp), contentAlignment = Alignment.Center) {
                        Text("검색 결과가 없습니다.", fontSize = 14.sp, color = T.ink3)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        list.forEachIndexed { i, p ->
                            val hot = i == 0 && filter == "all" && q.isBlank()
                            PartRow(p, hot) { sel = p }
                        }
                    }
                }
            }
            TabBar(active = "inventory", onTab = nav.tab)
        }

        SheetHost(open = sel != null, onClose = { sel = null }) {
            sel?.let { PartSheetContent(it, nav) { sel = null } }
        }
    }
}

@Composable
fun SearchField(value: String, onValue: (String) -> Unit, placeholder: String, showScan: Boolean) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card)
            .border(1.5.dp, if (focused) T.blue else T.line, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BbdIcon("search", 19.dp, if (focused) T.blue else T.ink3)
        Box(Modifier.weight(1f)) {
            androidx.compose.foundation.text.BasicTextField(
                value = value, onValueChange = onValue, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Pretendard, fontSize = 15.5.sp, color = T.ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(T.blue),
                modifier = Modifier.fillMaxWidth().onFocusChangedCompat { focused = it },
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, color = T.ink3, fontSize = 15.5.sp)
                    inner()
                },
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(T.lineSoft).clickable { onValue("") },
                contentAlignment = Alignment.Center,
            ) { BbdIcon("x", 13.dp, T.ink2, sw = 2.2f) }
        }
        if (showScan) BbdIcon("scan", 20.dp, T.ink2)
    }
}

private fun Modifier.onFocusChangedCompat(cb: (Boolean) -> Unit): Modifier =
    this.onFocusChanged { cb(it.isFocused) }

@Composable
private fun FilterChips(filter: String, s: com.example.bbd.data.InvSummary, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Chip("전체", filter == "all", count = s.total) { onPick("all") }
        Chip("부족", filter == "부족", count = s.short, icon = "alert", iconColor = T.blue) { onPick("부족") }
        Chip("없음", filter == "없음", count = s.none, icon = "ban", iconColor = T.red) { onPick("없음") }
        Chip("정상", filter == "정상", count = s.ok, icon = "check", iconColor = T.ink3) { onPick("정상") }
        Seed.CATEGORIES.forEach { c ->
            Chip(c, filter == c) { onPick(c) }
        }
    }
}

@Composable
private fun Chip(label: String, on: Boolean, count: Int? = null, icon: String? = null, iconColor: Color = T.ink3, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (on) T.blue else T.card)
            .border(1.5.dp, if (on) T.blue else T.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) BbdIcon(icon, 14.dp, if (on) Color.White else iconColor, sw = 2f)
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else T.ink2, maxLines = 1)
        if (count != null) Text("$count", fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else T.ink3)
    }
}

@Composable
private fun PartRow(p: Part, hot: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (hot) T.hotBg else T.card)
            .border(if (hot) 1.5.dp else 1.dp, if (hot) T.hotBorder else T.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        if (hot) Box(Modifier.align(Alignment.CenterStart).padding(start = 0.dp).width(4.dp).height(34.dp).clip(RoundedCornerShape(4.dp)).background(T.blue))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            PartThumb(p.thumb, active = hot)
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(4.dp))
                CodeText(p.sku, size = 12.5.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${p.qty}", fontFamily = Mono, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = if (p.status == StockStatus.NONE) T.red else T.ink)
                    Text(" ${p.unit}", fontSize = 11.sp, color = T.ink3, fontWeight = FontWeight.SemiBold)
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
    val moves = listOf(
        Movement(MoveType.IN, "도착 입고 확정", 30, p.unit, p.sku, p.name, "5/19", "2026-05-19", "11:08"),
        Movement(MoveType.OUT, "출고 · 교환", -2, p.unit, p.sku, p.name, "어제", "2026-05-21", "17:22"),
    )
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 26.dp)) {
        // 헤더
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
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
        Spacer(Modifier.size(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("지점 현재고", p.qty, p.unit, if (p.status == StockStatus.NONE) T.red else T.ink, p.status)
            StatTile("안전재고", p.safety, p.unit, T.ink2, null)
        }
        Spacer(Modifier.size(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            BbdIcon("pin", 14.dp, T.ink3); Text(p.wh, fontSize = 12.5.sp, color = T.ink2)
        }
        Spacer(Modifier.size(18.dp))

        Text("최근 이동", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2)
        Spacer(Modifier.size(4.dp))
        moves.forEachIndexed { i, m -> MovementRow(m, divider = i < moves.lastIndex) }
        Spacer(Modifier.size(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 입고 ghost
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.blue, RoundedCornerShape(13.dp))
                    .clickable { onClose(); nav.pushPart("scan-in", p) }.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                BbdIcon("arrowDn", 18.dp, T.blue, sw = 2f); Spacer(Modifier.size(7.dp))
                Text("입고", fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = T.blue)
            }
            // 출고 solid
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(T.blue)
                    .clickable { onClose(); nav.pushPart("scan-out", p) }.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                BbdIcon("arrowUp", 18.dp, Color.White, sw = 2f); Spacer(Modifier.size(7.dp))
                Text("출고", fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun RowScope.StatTile(label: String, value: Int, unit: String, accent: Color, status: StockStatus?) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(T.field).border(1.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, color = T.ink3, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (status != null) StatusPill(status)
        }
        Spacer(Modifier.size(7.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$value", fontFamily = Mono, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(" $unit", fontSize = 13.sp, color = T.ink3, fontWeight = FontWeight.SemiBold)
        }
    }
}
