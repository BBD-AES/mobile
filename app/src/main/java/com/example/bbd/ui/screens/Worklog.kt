package com.example.bbd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.MovementRow
import com.example.bbd.ui.Nav
import com.example.bbd.ui.Screen
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.T

@Composable
fun WorklogScreen(nav: Nav) {
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var sel by remember { mutableStateOf<String?>(null) }
    val s = Seed.WORKLOG_SUMMARY

    val list = Seed.WORKLOG.filter { m ->
        (filter == "all" ||
            (filter == "in" && m.type == MoveType.IN) ||
            (filter == "out" && m.type == MoveType.OUT) ||
            (filter == "adj" && m.type == MoveType.ADJ)) &&
            (q.isBlank() || m.name.contains(q) || m.sku.contains(q, ignoreCase = true))
    }
    // 날짜별 그룹 (목록 순서 유지)
    val groups = linkedMapOf<String, MutableList<Movement>>()
    list.forEach { groups.getOrPut(it.day) { mutableListOf() }.add(it) }

    Screen(header = { Header(title = "내 작업 이력", back = true, right = HeaderRight.BELL, onBack = { nav.pop() }) }) {
        SearchField(q, { q = it }, "부품명 또는 코드", showScan = false)
        Spacer(Modifier.size(12.dp))

        // 필터 칩 — 칩 카운트는 30일 전체 요약(WORKLOG_SUMMARY) 기준(디자인 핸드오프 명세).
        // 아래 목록과 "총 N건"은 현재 시드된 샘플 이동만 표시(운영에선 API가 30일치 전부 반환).
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            WChip("전체", null, s.total, filter == "all") { filter = "all" }
            WChip("입고", "arrowDn", s.inN, filter == "in") { filter = "in" }
            WChip("출고", "arrowUp", s.outN, filter == "out") { filter = "out" }
            WChip("조정", "plus", s.adj, filter == "adj") { filter = "adj" }
        }
        Spacer(Modifier.size(13.dp))

        // 기간
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(999.dp)).background(T.blueSoft).padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                BbdIcon("cal", 15.dp, T.blue)
                Text("최근 30일", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.blueInk)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { }.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BbdIcon("cal", 15.dp, T.ink3)
                Text("기간 직접 선택", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = T.ink2)
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
