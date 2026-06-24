package com.example.bbd.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.Seed
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.repo.NotificationRepository
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.BellBtn
import com.example.bbd.ui.LocalAppData
import com.example.bbd.ui.LocalMe
import com.example.bbd.ui.Nav
import com.example.bbd.ui.NotificationSheet
import com.example.bbd.ui.QueueBtn
import com.example.bbd.ui.SoDetailSheet
import com.example.bbd.ui.SoRow
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.bottomBorder
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.T
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(nav: Nav, contentPad: PaddingValues = PaddingValues()) {
    val me = LocalMe.current
    val app = LocalAppData.current
    val waiting = nav.queueCount
    val recent = app.received.take(3)
    var sel by remember { mutableStateOf<SalesOrder?>(null) }
    var notifOpen by remember { mutableStateOf(false) }
    var notifLoading by remember { mutableStateOf(false) }
    var notifError by remember { mutableStateOf<String?>(null) }
    var notifReload by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val notifRepo = remember { NotificationRepository() }
    // API 모드: 지점 알림 로드(로딩/에러 추적, 재시도=notifReload). 시드 모드는 Seed.NOTIFICATIONS(빌더 주입) 유지.
    LaunchedEffect(notifReload) {
        if (com.example.bbd.BuildConfig.USE_API) {
            notifLoading = true; notifError = null
            app.loadNotifications(emptyList())
            when (val r = notifRepo.inbox()) {
                is UiState.Success -> app.loadNotifications(r.data)
                is UiState.Error -> notifError = r.message
                else -> {}
            }
            notifLoading = false
        }
    }

    Box(Modifier.fillMaxSize().padding(contentPad)) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            // 커스텀 헤더 — 이름·지점·직무 + 새로고침 + 도착 대기 트럭 dot.
            Row(
                Modifier.fillMaxWidth().background(T.card).bottomBorder().padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(me.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp)
                    Spacer(Modifier.size(3.dp))
                    // 지점·코드·직무 — /me 가 지점을 주지 않으면(실 API) 빈 항목은 빼고 표시(시드 날조 금지).
                    // 데모/시드 모드는 모든 값이 차 있어 외형 불변.
                    val sub = listOf(me.branch, me.branchCode, me.position).filter { it.isNotBlank() }.joinToString(" · ")
                    Text(sub, fontSize = 12.5.sp, color = T.ink3Read, fontFamily = Mono)
                }
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable { app.refresh() }, contentAlignment = Alignment.Center) {
                    BbdIcon("refresh", 20.dp, if (app.refreshing) T.blue else T.ink2)
                }
                BellBtn(app.unreadCount) { notifOpen = true }
                QueueBtn(waiting, nav.openQueue)
            }

            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                // 인사 — 도착 대기 1차 위계는 히어로 한 곳.
                Text(
                    buildAnnotatedString {
                        append("안녕하세요, ")
                        withStyle(SpanStyle(color = T.blue)) { append(me.name) }
                        append(" 님")
                    },
                    fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
                Spacer(Modifier.size(16.dp))

                // 도착 대기 히어로 — 큐 진입(스캔 아님). generic 스캔은 FAB 하나.
                if (waiting > 0) ArrivalHero(waiting, nav.openQueue) else ArrivalEmptyCard()
                Spacer(Modifier.size(18.dp))

                // 작업 — 입고/출고/현장수주를 홈에 직접 노출(별도 FAB 없음 §IA).
                Text("작업", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink3Read, letterSpacing = 0.5.sp)
                Spacer(Modifier.size(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkCard("arrowDn", "입고 스캔", "도착 ${waiting}건", "도착 확인", Modifier.weight(1f), nav.scan)
                    WorkCard("arrowUp", "출고 스캔", "오늘 ${app.outbounds.size}건", "사용 차감", Modifier.weight(1f), nav.scanOut)
                    WorkCard("doc", "현장 수주", "오늘 ${app.orders.size}건", "작업·판매", Modifier.weight(1f), nav.orderNew)
                }
                Spacer(Modifier.size(20.dp))

                // 재고 주의 위젯 — 필터된 재고 딥링크.
                StockWarningWidget(Seed.INV_SUMMARY.short, Seed.INV_SUMMARY.none) { target -> nav.openInventory(target) }
                Spacer(Modifier.size(20.dp))

                // 점장 발주 안내 인라인 노트.
                if (me.role == "BRANCH_MANAGER") {
                    Row(
                        Modifier.fillMaxWidth().bbdCard().background(Color(0xFFFAFBFE)).padding(horizontal = 15.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        BbdIcon("info", 18.dp, T.ink3Read)
                        Text("지점 이동요청 작성·결과는 웹 ERP에서 확인합니다. 도착·보충 알림은 상단 알림함에서.", fontSize = 12.5.sp, color = T.ink2, lineHeight = 19.sp)
                    }
                    Spacer(Modifier.size(20.dp))
                }

                // 최근 입고 완료 (미리보기 · 전체는 이동요청 이력)
                Column(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("최근 입고 완료", fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, modifier = Modifier.weight(1f))
                        Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.tab("worklog") }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("더보기 ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.blue)
                            BbdIcon("chevR", 15.dp, T.blue)
                        }
                    }
                    if (recent.isEmpty()) {
                        Text("아직 입고 완료한 이동요청이 없습니다.", fontSize = 13.sp, color = T.ink3Read, modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    } else {
                        recent.forEachIndexed { i, so ->
                            SoRow(so, toShort = me.warehouseName.removeSuffix(" 창고"), onClick = { sel = so }, divider = i < recent.lastIndex)
                        }
                    }
                    Spacer(Modifier.size(2.dp))
                }
            }
        }

        SoDetailSheet(sel, me.name, me.warehouseName, onClose = { sel = null })
        NotificationSheet(
            open = notifOpen,
            items = app.notifications,
            loading = notifLoading,
            error = notifError,
            onRetry = { notifReload++ },
            onMarkRead = { id ->
                app.markNotifRead(id)
                if (com.example.bbd.BuildConfig.USE_API && id != null) scope.launch { notifRepo.markRead(id) }
            },
            // 도착 대기(IN_FULFILLMENT, 큐에 있는) SO 만 큐로 점프 — 백오더/입고완료는 없는 큐로 보내지 않음.
            onOpenSo = { so -> if (app.inbound.any { it.so == so }) { notifOpen = false; nav.openQueue() } },
            onClose = { notifOpen = false },
        )
    }
}

@Composable
private fun WorkCard(icon: String, title: String, metaTop: String, metaBottom: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.bbdCard().clickable(onClick = onClick).padding(vertical = 15.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(T.miniTile), contentAlignment = Alignment.Center) {
            BbdIcon(icon, 22.dp, T.ink2, sw = 2f)
        }
        Spacer(Modifier.size(10.dp))
        Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, maxLines = 1)
        Spacer(Modifier.size(4.dp))
        Text(metaTop, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = T.ink3Read, maxLines = 1)
        Text(metaBottom, fontSize = 10.5.sp, color = T.ink3, maxLines = 1)
    }
}

@Composable
private fun ArrivalHero(waiting: Int, onClick: () -> Unit) {
    // AI 티(파란 그라데이션 hero) 제거 — 다른 카드와 동일한 플랫 업무 카드. 숫자만 accent 로 위계, 그림자/CTA 알약 없음.
    Row(
        Modifier.fillMaxWidth().bbdCard().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(T.blueSoft), contentAlignment = Alignment.Center) {
            BbdIcon("truck", 23.dp, T.blue, sw = 1.9f)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("도착 대기", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                Text("$waiting", fontFamily = Mono, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.blue)
                Text("건", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
            }
            Spacer(Modifier.size(3.dp))
            Text("내 창고로 이동 중 · 탭하여 도착 확인", fontSize = 12.5.sp, color = T.ink3Read, lineHeight = 18.sp)
        }
        BbdIcon("chevR", 20.dp, T.ink3Read, sw = 2f)
    }
}

@Composable
private fun ArrivalEmptyCard() {
    Row(
        Modifier.fillMaxWidth().bbdCard().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(T.lineSoft), contentAlignment = Alignment.Center) {
            BbdIcon("truck", 23.dp, T.ink3Read, sw = 1.8f)
        }
        Column(Modifier.weight(1f)) {
            Text("도착 대기 없음", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
            Spacer(Modifier.size(3.dp))
            Text("이동 중인 입고가 없습니다. 부품 도착 시 아래 스캔으로 입고하세요.", fontSize = 12.5.sp, color = T.ink3Read, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun StockWarningWidget(short: Int, none: Int, onOpen: (String) -> Unit) {
    val warn = short + none > 0
    val target = if (none > 0) "없음" else if (short > 0) "부족" else "all"
    Row(
        Modifier.fillMaxWidth().bbdCard().clickable { onOpen(target) }.padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(if (warn) T.amberSoft else T.greenSoft), contentAlignment = Alignment.Center) {
            BbdIcon(if (warn) "alert" else "check", 21.dp, if (warn) T.amber else T.green, sw = 2f)
        }
        Column(Modifier.weight(1f)) {
            Text(if (warn) "재고 주의" else "재고 양호", fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
            Spacer(Modifier.size(4.dp))
            if (warn) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (short > 0) WarnPill(T.amberSoft, T.amberInk, T.amber, "부족 $short")
                    if (none > 0) WarnPill(T.redSoft, T.red, T.red, "없음 $none")
                }
            } else {
                Text("부족·없음 항목이 없습니다. 지점 재고 보기", fontSize = 12.5.sp, color = T.ink3Read)
            }
        }
        BbdIcon("chevR", 18.dp, T.ink3Read)
    }
}

@Composable
private fun WarnPill(bg: Color, fg: Color, dot: Color, text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(dot))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}
