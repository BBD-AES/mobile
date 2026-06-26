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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.Seed
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.LocalMe
import com.example.bbd.ui.ModalHost
import com.example.bbd.ui.Nav
import com.example.bbd.ui.SoDetailSheet
import com.example.bbd.ui.ToastHost
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.bottomBorder
import com.example.bbd.ui.topBorder
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

@Composable
fun MyScreen(nav: Nav, contentPad: PaddingValues = PaddingValues()) {
    val me = LocalMe.current
    val perm = Seed.ROLE_PERMS[me.role] ?: Seed.ROLE_PERMS.getValue("BRANCH_STAFF")
    var sel by remember { mutableStateOf<SalesOrder?>(null) }
    var confirmOut by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf("") }
    if (toast.isNotBlank()) LaunchedEffect(toast) { kotlinx.coroutines.delay(2200); toast = "" }

    Box(Modifier.fillMaxSize().padding(contentPad)) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "마이", right = HeaderRight.QUEUE, queueCount = nav.queueCount, onRight = nav.openQueue)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp)) {
                // 프로필
                Column(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 18.dp, vertical = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(62.dp).clip(CircleShape).background(T.lineSoft).border(1.dp, T.line, CircleShape), contentAlignment = Alignment.Center) {
                            Text(me.name.take(2), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2)
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(me.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp)
                                Row(Modifier.clip(RoundedCornerShape(999.dp)).background(T.blueSoft).padding(horizontal = 10.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(T.blue))
                                    Text(me.position, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = T.blueInk)
                                }
                            }
                            Spacer(Modifier.size(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("직무 ${me.position} · 사번 ", fontSize = 12.5.sp, color = T.ink2)
                                CodeText(me.emp, size = 12.5.sp, color = T.ink3Read)
                            }
                            Spacer(Modifier.size(3.dp))
                            CodeText(me.email, size = 12.5.sp, color = T.ink3Read)
                        }
                    }
                    Spacer(Modifier.size(14.dp))
                    Row(Modifier.fillMaxWidth().topBorder(T.lineSoft).padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BbdIcon("pin", 14.dp, T.ink3Read)
                        // 지점명(tenancyName) + 창고코드(로그인 시 inventory 이름매칭으로 해석, PR#18).
                        if (me.branch.isNotBlank()) Text(me.branch, fontSize = 12.5.sp, color = T.ink2)
                        if (me.branchCode.isNotBlank()) {
                            Spacer(Modifier.size(6.dp)); CodeText(me.branchCode, size = 12.5.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("정보 수정은 관리자 문의", fontSize = 12.sp, color = T.ink3Read)
                    }
                }
                Spacer(Modifier.size(16.dp))

                // 이용 권한
                Column(Modifier.fillMaxWidth().bbdCard().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("이용 권한", fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(T.blueSoft).padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text(me.role, fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = T.blueInk)
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    perm.can.forEach { PermRow("check", T.green, T.greenSoft, it, T.ink) }
                    if (perm.web.isNotEmpty()) {
                        Spacer(Modifier.size(4.dp))
                        Box(Modifier.fillMaxWidth().bottomBorder(T.lineSoft))
                        Spacer(Modifier.size(11.dp))
                        Text("웹 ERP에서 가능", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = T.ink3Read, modifier = Modifier.padding(bottom = 9.dp))
                        perm.web.forEach { WebPermRow(it) }
                    }
                    if (perm.cant.isNotEmpty()) {
                        Spacer(Modifier.size(4.dp))
                        Box(Modifier.fillMaxWidth().bottomBorder(T.lineSoft))
                        Spacer(Modifier.size(11.dp))
                        perm.cant.forEach { PermRow("ban", T.ink3Read, T.lineSoft, it, T.ink3Read) }
                    }
                }
                Spacer(Modifier.size(16.dp))

                // (작업 이력은 별도 '작업이력' 탭으로 분리 — 마이에서 제거)

                // 로그아웃
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(T.card).border(1.dp, T.line, RoundedCornerShape(14.dp)).clickable { confirmOut = true }.padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("로그아웃", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = T.red)
                }
            }
        }

        SoDetailSheet(sel, me.name, me.warehouseName, onClose = { sel = null })

        // 로그아웃 확인 모달
        ModalHost(confirmOut, { confirmOut = false }) {
            Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(50.dp).clip(CircleShape).background(T.redSoft), contentAlignment = Alignment.Center) { BbdIcon("logout", 24.dp, T.red, sw = 2f) }
                Spacer(Modifier.size(14.dp))
                Text("로그아웃 할까요?", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                Spacer(Modifier.size(6.dp))
                Text("다시 사용하려면 사번으로 로그인해야 합니다.", fontSize = 13.5.sp, color = T.ink2)
                Spacer(Modifier.size(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostBtn("취소", Modifier.weight(1f)) { confirmOut = false }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(T.red).clickable { confirmOut = false; nav.logout() }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("로그아웃", color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }

        ToastHost(toast)
    }
}

@Composable
private fun PermRow(icon: String, iconColor: Color, iconBg: Color, label: String, textColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) { BbdIcon(icon, 12.dp, iconColor, sw = 2.6f) }
        Text(label, fontSize = 13.5.sp, color = textColor)
    }
}

@Composable
private fun WebPermRow(label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFF2F5FB)), contentAlignment = Alignment.Center) { BbdIcon("doc", 12.dp, T.ink3Read, sw = 2f) }
        Text(label, fontSize = 13.5.sp, color = T.ink2, modifier = Modifier.weight(1f))
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(T.lineSoft).padding(horizontal = 7.dp, vertical = 2.dp)) {
            Text("웹", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink3Read)
        }
    }
}

@Composable
private fun GhostBtn(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, color = T.ink2, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, fontFamily = Pretendard)
    }
}
