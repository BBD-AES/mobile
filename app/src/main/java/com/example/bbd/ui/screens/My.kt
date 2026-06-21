package com.example.bbd.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.example.bbd.data.Seed
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.MovementRow
import com.example.bbd.ui.Nav
import com.example.bbd.ui.Screen
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.bottomBorder
import com.example.bbd.ui.theme.T

@Composable
fun MyScreen(nav: Nav, contentPad: PaddingValues = PaddingValues()) {
    val u = Seed.USER
    var notif by remember { mutableStateOf(true) }

    // 벨 no-op 제거 → 헤더 우측 액션 없음(마이엔 알림 진입점 불필요).
    Screen(contentPad = contentPad, header = { Header(title = "마이") }) {
        // 프로필 카드
        Row(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 18.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box {
                Box(
                    Modifier.size(62.dp).clip(CircleShape).background(T.lineSoft).border(1.dp, T.line, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(u.name.take(2), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2) }
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).size(22.dp).clip(CircleShape).background(Color.White).border(1.dp, T.line, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { BbdIcon("edit", 11.dp, T.ink2) }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(u.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp)
                    Row(
                        Modifier.clip(RoundedCornerShape(999.dp)).background(T.blueSoft).padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(T.blue))
                        Text(u.position, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = T.blueInk)
                    }
                }
                Spacer(Modifier.size(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("사번 · ", fontSize = 12.5.sp, color = T.ink2); CodeText(u.emp, size = 12.5.sp)
                    Text(" · ", fontSize = 12.5.sp, color = T.ink2); CodeText(u.role, size = 12.5.sp)
                }
                Spacer(Modifier.size(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BbdIcon("pin", 13.dp, T.ink3)
                    Text("${u.branch} · ", fontSize = 12.5.sp, color = T.ink2); CodeText(u.branchCode, size = 12.5.sp)
                }
            }
        }
        Spacer(Modifier.size(16.dp))

        // 내 작업 이력
        Column(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("내 작업 이력", fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, modifier = Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.tab("worklog") }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("전체 보기 ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.blue)
                    BbdIcon("chevR", 15.dp, T.blue)
                }
            }
            val items = Seed.WORKLOG.take(5)
            items.forEachIndexed { i, m -> MovementRow(m, divider = i < items.lastIndex) }
            Spacer(Modifier.size(4.dp))
        }
        Spacer(Modifier.size(16.dp))

        // 설정
        Column(Modifier.fillMaxWidth().bbdCard()) {
            SettingRow("lock", "비밀번호", divider = true, sub = {
                Text("마지막 변경 · ${u.pwChanged} (${u.pwDaysAgo}일 전)", fontSize = 12.5.sp, color = T.amber, fontWeight = FontWeight.SemiBold)
            }) { BbdIcon("chevR", 18.dp, T.ink3) }
            SettingRow("bell", "알림 설정", divider = true, sub = {
                Text("푸시 · 이메일", fontSize = 12.5.sp, color = T.ink3)
            }) { Toggle(notif) { notif = !notif } }
            SettingRow("info", "앱 정보", divider = false, sub = {
                Text("v0.5 · 현장 모바일", fontSize = 12.5.sp, color = T.ink3)
            }) { BbdIcon("chevR", 18.dp, T.ink3) }
        }
        Spacer(Modifier.size(18.dp))

        // 로그아웃
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(T.card).border(1.dp, T.line, RoundedCornerShape(14.dp)).clickable { nav.logout() }.padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            BbdIcon("logout", 18.dp, T.red); Spacer(Modifier.size(8.dp))
            Text("로그아웃", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = T.red)
        }
    }
}

@Composable
private fun SettingRow(icon: String, title: String, divider: Boolean, sub: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().then(if (divider) Modifier.bottomBorder(T.lineSoft) else Modifier).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(T.miniTile), contentAlignment = Alignment.Center) {
            BbdIcon(icon, 20.dp, T.ink2)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = T.ink)
            Spacer(Modifier.size(2.dp))
            sub()
        }
        right()
    }
}

@Composable
private fun Toggle(on: Boolean, onToggle: () -> Unit) {
    val knob by animateDpAsState(if (on) 24.dp else 3.dp, tween(200), label = "knob")
    Box(
        Modifier.width(50.dp).height(29.dp).clip(RoundedCornerShape(999.dp)).background(if (on) T.blue else Color(0xFFD4DAE6)).clickable { onToggle() },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (on) Text("ON", Modifier.padding(start = 9.dp), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Box(Modifier.offset(x = knob).size(23.dp).clip(CircleShape).background(Color.White))
    }
}
