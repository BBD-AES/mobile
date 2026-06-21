package com.example.bbd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.Seed
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.BellBtn
import com.example.bbd.ui.MovementRow
import com.example.bbd.ui.Nav
import com.example.bbd.ui.TabBar
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.bottomBorder
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

@Composable
fun HomeScreen(nav: Nav) {
    val u = Seed.USER
    Column(Modifier.fillMaxSize().background(T.bg)) {
        // 커스텀 헤더
        Row(
            Modifier.fillMaxWidth().background(T.card).bottomBorder().padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(u.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp)
                Spacer(Modifier.size(3.dp))
                Text("${u.branch} · ${u.branchCode}", fontSize = 12.5.sp, color = T.ink3, fontFamily = Mono)
            }
            BellBtn(Seed.NOTIF) {}
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // 인사 카드
            Column(Modifier.fillMaxWidth().bbdCard().padding(20.dp)) {
                Text(
                    buildAnnotatedString {
                        append("안녕하세요, ")
                        withStyle(SpanStyle(color = T.blue)) { append(u.name) }
                        append(" 님")
                    },
                    fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp,
                )
                Spacer(Modifier.size(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("오늘 처리할 항목", fontSize = 14.sp, color = T.ink2)
                    Row(
                        Modifier.clip(RoundedCornerShape(999.dp)).background(T.blueSoft).padding(horizontal = 11.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(T.blue))
                        Text("도착 대기 2건", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.blueInk)
                    }
                }
            }
            Spacer(Modifier.size(16.dp))

            // 액션 카드 2개
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(Modifier.weight(1f), T.blue, "box", "입고 스캔", "IN · 도착 확정") { nav.push("scan-in") }
                ActionCard(Modifier.weight(1f), T.blueDeep, "logout", "출고 스캔", "OUT · 작업 사용") { nav.push("scan-out") }
            }
            Spacer(Modifier.size(12.dp))

            // 보조 카드 3개
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniCard(Modifier.weight(1f), "search", "재고 조회", "부품 검색") { nav.tab("inventory") }
                MiniCard(Modifier.weight(1f), "cube", "보충 발주", "본사 요청") { nav.push("order") }
                MiniCard(Modifier.weight(1f), "list", "작업 이력", "최근 30일") { nav.push("worklog") }
            }
            Spacer(Modifier.size(20.dp))

            // 최근 활동
            Column(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("최근 활동", fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, modifier = Modifier.weight(1f))
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.push("worklog") }.padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("더보기 ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.blue)
                        BbdIcon("chevR", 15.dp, T.blue)
                    }
                }
                val recent = Seed.RECENT
                recent.forEachIndexed { i, m ->
                    MovementRow(m, divider = i < recent.lastIndex)
                }
                Spacer(Modifier.size(2.dp))
            }
        }

        TabBar(active = "home", onTab = nav.tab)
    }
}

@Composable
private fun ActionCard(modifier: Modifier, bg: Color, icon: String, title: String, sub: String, onClick: () -> Unit) {
    Box(
        modifier
            .shadow(8.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = T.blueDeep, spotColor = T.blueDeep)
            .clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp),
    ) {
        Box(Modifier.align(Alignment.TopEnd)) { BbdIcon("chevR", 18.dp, Color.White.copy(alpha = 0.9f), sw = 2.2f) }
        Column {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                BbdIcon(icon, 22.dp, Color.White, sw = 1.9f)
            }
            Spacer(Modifier.size(30.dp))
            Text(title, fontSize = 17.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = (-0.4).sp)
            Spacer(Modifier.size(4.dp))
            Text(sub, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.78f), fontFamily = Mono)
        }
    }
}

@Composable
private fun MiniCard(modifier: Modifier, icon: String, title: String, sub: String, onClick: () -> Unit) {
    Column(
        modifier.bbdCard().clickable(onClick = onClick).padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 16.dp),
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(T.miniTile), contentAlignment = Alignment.Center) {
            BbdIcon(icon, 20.dp, T.ink2)
        }
        Spacer(Modifier.size(16.dp))
        Text(title, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp)
        Spacer(Modifier.size(3.dp))
        Text(sub, fontSize = 12.sp, color = T.ink3)
    }
}
