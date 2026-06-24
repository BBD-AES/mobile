package com.example.bbd.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.BuildConfig
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.totals
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.repo.SalesOrderRepository
import com.example.bbd.ui.AppData
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.LocalAppData
import com.example.bbd.ui.LocalMe
import com.example.bbd.ui.ModalHost
import com.example.bbd.ui.Nav
import com.example.bbd.ui.PartThumb
import com.example.bbd.ui.SoBadge
import com.example.bbd.ui.Spinner
import com.example.bbd.ui.StickyBar
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.bottomBorder
import com.example.bbd.ui.topBorder
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

private val ViewfinderBg = Brush.radialGradient(
    colors = listOf(Color(0xFF2A3040), Color(0xFF14171F), Color(0xFF0D0F15)),
    radius = 900f,
)

/**
 * 입고 스캔 — 발주(주문) 단위 receive(PATCH /sales-orders/{so}/receive).
 * preset(SalesOrder) 가 있으면(큐/부품 상세에서) 바로 입고 확정 폼으로, 없으면 QR/SO 스캔.
 */
@Composable
fun ScanScreen(nav: Nav, preset: SalesOrder? = null) {
    val app = LocalAppData.current
    var chosen by remember { mutableStateOf(preset) }

    if (chosen != null) {
        ReceiveOrderForm(nav, chosen!!, onBack = { if (preset != null) nav.pop() else chosen = null })
        return
    }
    ScanOrderScreen(nav, app) { chosen = it }
}

@Composable
private fun ScanOrderScreen(nav: Nav, app: AppData, onResolved: (SalesOrder) -> Unit) {
    var manualOpen by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var mErr by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scanned = result.contents?.trim()?.uppercase()
        val so = scanned?.let { s -> app.inbound.firstOrNull { it.so.uppercase() == s } }
        if (so != null) onResolved(so)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "입고 스캔", back = true, chip = "QR", right = HeaderRight.QUEUE, queueCount = nav.queueCount, onBack = { nav.pop() }, onRight = nav.openQueue)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            ) {
                // 뷰파인더
                Box(
                    Modifier.fillMaxWidth().height(318.dp).clip(RoundedCornerShape(18.dp)).background(ViewfinderBg),
                ) {
                    // 좌상단 SCAN 라벨
                    Row(Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF5B5B)))
                        Text("SCAN · 발주 QR", fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.sp)
                    }
                    // 플래시 토글 44×44 히트(글리프 34)
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(44.dp).clickable { flashOn = !flashOn },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(if (flashOn) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            BbdIcon("flash", 17.dp, if (flashOn) Color(0xFF1A1D24) else Color.White)
                        }
                    }
                    // 코너 마커 + 스캔 라인
                    Viewfinder(Modifier.align(Alignment.Center))
                    // 안내 + 스캔 버튼
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("발주 QR 또는 박스 라벨을 맞춰 주세요", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.82f))
                        Spacer(Modifier.size(11.dp))
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
                                .clickable {
                                    scanLauncher.launch(ScanOptions().apply {
                                        setPrompt("도착 발주 QR/바코드를 맞춰 주세요")
                                        setBeepEnabled(true)
                                        setOrientationLocked(false)
                                    })
                                }.padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BbdIcon("scan", 19.dp, T.blue, sw = 2f)
                                Text("QR 스캔", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                            }
                        }
                    }
                }
                Spacer(Modifier.size(18.dp))

                // 안내 카드
                Row(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 16.dp, vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(T.blueSoft), contentAlignment = Alignment.Center) {
                        BbdIcon("truck", 19.dp, T.blue, sw = 1.9f)
                    }
                    Text("QR을 스캔하면 해당 발주(주문)를 찾아 전량 입고 확정합니다. PATCH /sales-orders/{so}/receive", fontSize = 13.sp, color = T.ink2, lineHeight = 20.sp)
                }
                Spacer(Modifier.size(12.dp))

                // 발주번호 직접 입력
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp))
                        .clickable { manualOpen = true; mErr = false }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BbdIcon("keypad", 17.dp, T.ink2)
                        Text("발주번호 직접 입력", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink2)
                    }
                }
            }
        }

        // 발주번호 입력 모달 — 즉시 인라인 검증.
        ManualSoModal(
            open = manualOpen,
            code = code,
            onCode = { code = it.uppercase(); mErr = false },
            error = mErr,
            sample = app.inbound.firstOrNull()?.so ?: "SO-2026-0061",
            onClose = { manualOpen = false; mErr = false },
            onSubmit = {
                val v = ("SO-" + code.trim().removePrefix("SO-")).uppercase()
                val so = app.inbound.firstOrNull { it.so.uppercase() == v }
                if (so == null) mErr = true else { manualOpen = false; mErr = false; onResolved(so) }
            },
        )
    }
}

@Composable
private fun Viewfinder(modifier: Modifier) {
    Box(modifier.size(width = 184.dp, height = 184.dp)) {
        // 코너 마커 4개
        listOf(
            Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd,
        ).forEach { a ->
            val top = a == Alignment.TopStart || a == Alignment.TopEnd
            val left = a == Alignment.TopStart || a == Alignment.BottomStart
            Box(Modifier.align(a).size(30.dp)) {
                // 가로 변
                Box(Modifier.align(if (top) Alignment.TopStart else Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.White))
                // 세로 변
                Box(Modifier.align(if (left) Alignment.TopStart else Alignment.TopEnd).width(3.dp).fillMaxHeight().background(Color.White))
            }
        }
        // 스캔 라인(정적 중앙 — reduced-motion 안전)
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(3.dp).background(Color(0xFF4F7CFF)))
    }
}

@Composable
private fun ManualSoModal(
    open: Boolean,
    code: String,
    onCode: (String) -> Unit,
    error: Boolean,
    sample: String,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ModalHost(open, onClose) {
            Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("발주번호 입력", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, modifier = Modifier.weight(1f))
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                        BbdIcon("x", 22.dp, T.ink2)
                    }
                }
                Text(buildString { append("SO 번호 ") }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink2)
                Spacer(Modifier.size(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).border(1.5.dp, if (error) T.red else T.blue, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("SO-", fontFamily = Mono, fontSize = 16.sp, color = T.ink3)
                    BasicTextField(
                        value = code, onValueChange = onCode, singleLine = true,
                        textStyle = TextStyle(fontFamily = Mono, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = T.ink),
                        cursorBrush = SolidColor(T.blue), modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (code.isEmpty()) Text("2026-0061", color = T.ink3, fontFamily = Mono, fontSize = 17.sp); inner() },
                    )
                }
                Spacer(Modifier.size(8.dp))
                if (error) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        BbdIcon("alert", 15.dp, T.red, sw = 2f)
                        Text("도착 대기 발주에서 찾을 수 없습니다. 번호를 확인하세요. (예: $sample)", fontSize = 12.5.sp, color = T.red, lineHeight = 18.sp)
                    }
                } else {
                    Text("SO- 접두어 자동 · 도착 대기 발주만 입고 가능", fontSize = 12.sp, color = T.ink3, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Spacer(Modifier.size(18.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if (code.isNotBlank()) T.blue else T.line).clickable(enabled = code.isNotBlank(), onClick = onSubmit).padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("발주 찾기", color = if (code.isNotBlank()) Color.White else T.ink3, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ───────────────────────── 발주 전량 입고 확정 ─────────────────────────

@Composable
private fun ReceiveOrderForm(nav: Nav, so: SalesOrder, onBack: () -> Unit) {
    val me = LocalMe.current
    val app = LocalAppData.current
    val repo = remember { SalesOrderRepository() }
    val scope = rememberCoroutineScope()
    val tot = so.totals()
    var confirmOpen by remember { mutableStateOf(false) }
    var doneOpen by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // 이 발주를 제외한 남은 도착 대기 — confirmReceive 가 inbound 를 줄이기 전 값으로 고정.
    var remaining by remember { mutableStateOf(0) }

    fun submit() {
        if (submitting) return
        submitting = true; confirmOpen = false; error = null
        val soNo = so.so
        val others = app.inbound.count { it.so != soNo }
        scope.launch {
            val r: UiState<Unit> = if (BuildConfig.USE_API) repo.receive(soNo) else UiState.Success(Unit)
            when (r) {
                is UiState.Success -> { app.confirmReceive(soNo); remaining = others; submitting = false; doneOpen = true }
                is UiState.Error -> { submitting = false; error = r.message }
                else -> submitting = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "입고 확정", back = true, chip = "발주 단위", onBack = onBack)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)) {
                // 발주 헤더 카드
                Column(Modifier.fillMaxWidth().bbdCard().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        CodeText(so.so, size = 15.sp, color = T.ink)
                        SoBadge(so.status)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.clip(RoundedCornerShape(999.dp)).background(T.greenSoft).padding(horizontal = 9.dp, vertical = 3.dp)) {
                            Text("전량 수령", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = T.greenInk)
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF7F9FC)).border(1.dp, T.line, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BbdIcon("truck", 20.dp, T.ink2)
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(so.fromWh, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.ink)
                            BbdIcon("arrowR", 14.dp, T.ink3, modifier = Modifier.padding(horizontal = 6.dp))
                            Text(me.warehouseName, fontSize = 13.5.sp, color = T.ink2)
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Row(Modifier.fillMaxWidth().topBorder(T.lineSoft).padding(top = 12.dp)) {
                        Text("합계", fontSize = 13.sp, color = T.ink2, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${tot.items}품목 · ${tot.qty}${if (tot.unit.isNotEmpty()) " ${tot.unit}" else ""}", fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                    }
                }
                Spacer(Modifier.size(14.dp))

                // 발주 품목
                Row(Modifier.fillMaxWidth()) {
                    Text("발주 품목", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink, modifier = Modifier.weight(1f))
                    Text("${tot.items}건", fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                }
                Spacer(Modifier.size(9.dp))
                Column(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 14.dp)) {
                    so.lines.forEachIndexed { i, l ->
                        Row(
                            Modifier.fillMaxWidth().then(if (i < so.lines.lastIndex) Modifier.bottomBorder(T.lineSoft) else Modifier).padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PartThumb(l.thumb, size = 42.dp)
                            Column(Modifier.weight(1f)) {
                                Text(l.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                CodeText(l.sku, size = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("+${l.qty}", fontFamily = Mono, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = T.blue)
                                Text(l.unit, fontSize = 11.sp, color = T.ink3Read, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.size(14.dp))

                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFAFBFE)).border(1.dp, T.line, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BbdIcon("info", 16.dp, T.ink3Read)
                    Text("발주 주문 단위로 입고됩니다. 라인별 부분 수령은 백엔드 미지원(전량 확정).", fontSize = 12.sp, color = T.ink2, lineHeight = 18.sp)
                }

                error?.let { e ->
                    Spacer(Modifier.size(12.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(T.redSoft).padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BbdIcon("alert", 18.dp, T.red, sw = 2f)
                        Text(e, fontSize = 13.sp, color = T.redBlockTitle, fontWeight = FontWeight.Bold)
                    }
                }
            }
            StickyBar {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(T.blue).clickable(enabled = !submitting) { confirmOpen = true }.padding(vertical = 17.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BbdIcon("check", 20.dp, Color.White, sw = 2.4f)
                        Text("발주 전량 입고 확정", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 확인 모달
        ModalHost(confirmOpen, { if (!submitting) confirmOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 20.dp)) {
                Text("발주를 전량 입고 확정할까요?", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFFF7F9FC)).border(1.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text("발주", fontSize = 12.5.sp, color = T.ink3, modifier = Modifier.weight(1f)); CodeText(so.so, size = 13.sp, color = T.ink)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("합계", fontSize = 12.5.sp, color = T.ink3, modifier = Modifier.weight(1f))
                        Text("${tot.items}품목 · ${tot.qty}${if (tot.unit.isNotEmpty()) " ${tot.unit}" else ""}", fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = T.blue)
                    }
                }
                Spacer(Modifier.size(16.dp))
                Text("확인 시 상태가 입고 완료(RECEIVED)로 바뀝니다.", fontSize = 12.sp, color = T.ink3, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.size(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModalGhost("취소", Modifier.weight(1f), enabled = !submitting) { confirmOpen = false }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(T.blue).clickable(enabled = !submitting) { submit() }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (submitting) Spinner(16.dp)
                            Text(if (submitting) "처리 중…" else "입고 확정", color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // 완료 모달 — 단일 종료(토스트+모달 이중 금지).
        ModalHost(doneOpen, { nav.tab("home") }) {
            Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(T.greenSoft), contentAlignment = Alignment.Center) {
                    BbdIcon("check", 28.dp, T.green, sw = 2.6f)
                }
                Spacer(Modifier.size(14.dp))
                Text("입고 완료", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                Spacer(Modifier.size(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CodeText(so.so, size = 12.5.sp)
                    Text(" · ${tot.items}품목 ${tot.qty}${if (tot.unit.isNotEmpty()) " ${tot.unit}" else ""} 전량 수령", fontSize = 13.5.sp, color = T.ink2)
                }
                Spacer(Modifier.size(8.dp))
                Text(if (remaining > 0) "남은 도착 대기 ${remaining}건" else "도착 대기 발주를 모두 처리했습니다.", fontSize = 12.5.sp, color = T.ink3Read)
                Spacer(Modifier.size(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModalGhost("홈으로", Modifier.weight(1f)) { nav.tab("home") }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(T.blue).clickable {
                            if (remaining > 0) nav.tab("home") else nav.tab("worklog")
                        }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (remaining > 0) "도착 대기 보기" else "작업 이력 보기", color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalGhost(label: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp)).clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = T.ink2, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, fontFamily = Pretendard)
    }
}
