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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.BuildConfig
import com.example.bbd.data.Part
import com.example.bbd.data.Seed
import com.example.bbd.data.remote.OutboundResult
import com.example.bbd.data.repo.InventoryRepository
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.LocalAppData
import com.example.bbd.ui.LocalMe
import com.example.bbd.ui.ModalHost
import com.example.bbd.ui.Nav
import com.example.bbd.ui.PartThumb
import com.example.bbd.ui.Spinner
import com.example.bbd.ui.StickyBar
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.nextCoNo
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

/** 출고 사유 — 색만이 아니라 아이콘 토큰 동반(핸드오프 §5). */
private data class OutReason(val id: String, val icon: String, val amber: Boolean = false)
private val OUT_REASONS = listOf(
    OutReason("정비 사용", "wrench"),
    OutReason("판매", "tag"),
    OutReason("폐기/손망실", "ban", amber = true),
)

/** 출고 제출 결과 — UiState 로는 409/401/오프라인을 못 가르므로 화면 전용 타입. */
private sealed interface OutUi {
    data class Ok(val coNo: String, val remaining: Int) : OutUi
    data class Conflict(val serverAvailable: Int) : OutUi
    data object Offline : OutUi
    data object Unauthorized : OutUi
    data class Err(val message: String) : OutUi
}

private enum class OutPhase { Scanning, Recognizing, Form }

/**
 * 출고 스캔 — 정비 사용/판매 출고로 지점 재고를 즉시 차감(POST /stocks/outbound).
 * 입고 스캔과 대칭, 방향만 OUT. preset(재고 상세 '이 품목 출고')이면 바로 폼으로.
 * 모든 출고는 추적용 출고 번호(원장 referenceNumber)를 갖고, 부족 시 409 차감 거부(부분 차감 없음).
 */
@Composable
fun ScanOutScreen(nav: Nav, preset: Part? = null) {
    var phase by remember { mutableStateOf(if (preset != null) OutPhase.Form else OutPhase.Scanning) }
    var part by remember { mutableStateOf(preset) }
    var unknownCode by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Part?>(null) }
    var manualOpen by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var mErr by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }

    fun toForm(resolved: Part?, raw: String?) {
        part = resolved; unknownCode = if (resolved == null) raw else null; phase = OutPhase.Form
    }
    fun beginRecognize(found: Part) { pending = found; phase = OutPhase.Recognizing }

    // 인식 확인 비트(0.9s) → 폼. 우발/오스캔 차단(§4A-2).
    if (phase == OutPhase.Recognizing) {
        androidx.compose.runtime.LaunchedEffect(pending) {
            kotlinx.coroutines.delay(900)
            toForm(pending, null)
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scanned = result.contents?.trim()?.uppercase()
        if (scanned != null) {
            val p = Seed.partBySku(scanned)
            if (p != null) beginRecognize(p) else toForm(null, scanned)
        }
    }

    if (phase == OutPhase.Form) {
        OutForm(
            nav = nav, part = part, unknownCode = unknownCode, preset = preset != null,
            onBack = { if (preset != null) nav.pop() else { part = null; unknownCode = null; phase = OutPhase.Scanning } },
            onRescan = { part = null; unknownCode = null; phase = OutPhase.Scanning },
        )
        return
    }

    val recognizing = phase == OutPhase.Recognizing
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "출고 스캔", back = true, chip = "OUT", right = HeaderRight.QUEUE, queueCount = nav.queueCount, onBack = { nav.pop() }, onRight = nav.openQueue)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)) {
                // 뷰파인더
                Box(Modifier.fillMaxWidth().height(318.dp).clip(RoundedCornerShape(18.dp)).background(ViewfinderBg)) {
                    Row(Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF5B5B)))
                        Text("SCAN · OUT", fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.sp)
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(44.dp).clickable { flashOn = !flashOn }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(if (flashOn) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            BbdIcon("flash", 17.dp, if (flashOn) Color(0xFF1A1D24) else Color.White)
                        }
                    }
                    OutViewfinder(Modifier.align(Alignment.Center), pending?.sku.takeIf { recognizing })
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (recognizing) "부품을 확인하고 있어요…" else "꺼내 쓸 부품의 바코드를 맞춰 주세요", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.82f))
                        Spacer(Modifier.size(11.dp))
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (recognizing) Color.White.copy(alpha = 0.55f) else Color.White)
                                .clickable(enabled = !recognizing) {
                                    scanLauncher.launch(ScanOptions().apply {
                                        setPrompt("꺼내 쓸 부품의 바코드를 맞춰 주세요")
                                        setBeepEnabled(true)
                                        setOrientationLocked(false)
                                    })
                                }.padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BbdIcon("scan", 19.dp, T.blue, sw = 2f)
                                Text("바코드 스캔", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                            }
                        }
                    }
                }
                Spacer(Modifier.size(18.dp))

                // 안내 카드
                Row(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 16.dp, vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(T.miniTile), contentAlignment = Alignment.Center) {
                        BbdIcon("arrowUp", 19.dp, T.ink2, sw = 2f)
                    }
                    Text("바코드를 스캔하면 꺼내 쓰는 부품을 찾아 지점 현재고에서 차감합니다. 가용재고가 부족하면 차감되지 않습니다.", fontSize = 13.sp, color = T.ink2, lineHeight = 20.sp)
                }
                Spacer(Modifier.size(12.dp))

                // 바코드 수동 입력
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp))
                        .clickable { manualOpen = true; mErr = false }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BbdIcon("edit", 17.dp, T.ink2)
                        Text("바코드 수동 입력", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink2)
                    }
                }
            }
        }

        // 수동 입력 — 프리필 없는 빈 칸. 미등록 코드면 인라인 오류(§4A-1).
        ModalHost(manualOpen, { manualOpen = false; mErr = false }) {
            Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("수동 입력", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, modifier = Modifier.weight(1f))
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { manualOpen = false; mErr = false }, contentAlignment = Alignment.Center) { BbdIcon("x", 22.dp, T.ink2) }
                }
                Text("부품코드", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink2)
                Spacer(Modifier.size(8.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).border(1.5.dp, if (mErr) T.red else T.blue, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("#", fontFamily = Mono, fontSize = 17.sp, color = T.ink3)
                    BasicTextField(
                        value = code, onValueChange = { code = it.uppercase(); mErr = false }, singleLine = true,
                        textStyle = TextStyle(fontFamily = Mono, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = T.ink),
                        cursorBrush = SolidColor(T.blue), modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (code.isEmpty()) Text("BBD-...", color = T.ink3, fontFamily = Mono, fontSize = 17.sp); inner() },
                    )
                }
                Spacer(Modifier.size(8.dp))
                if (mErr) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        BbdIcon("alert", 15.dp, T.red, sw = 2f)
                        Text("부품 마스터에서 ${code.uppercase()} 를 찾을 수 없습니다. 코드를 다시 확인하세요.", fontSize = 12.5.sp, color = T.red, lineHeight = 18.sp)
                    }
                } else {
                    Text("대소문자 구분 없음", fontSize = 12.sp, color = T.ink3, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.size(18.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if (code.isNotBlank()) T.blue else T.line).clickable(enabled = code.isNotBlank()) {
                        val found = Seed.partBySku(code.trim())
                        if (found == null) mErr = true else { manualOpen = false; mErr = false; beginRecognize(found) }
                    }.padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("확인", color = if (code.isNotBlank()) Color.White else T.ink3, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun OutViewfinder(modifier: Modifier, recognizingSku: String?) {
    Box(modifier.size(width = 184.dp, height = 184.dp), contentAlignment = Alignment.Center) {
        listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEach { a ->
            val top = a == Alignment.TopStart || a == Alignment.TopEnd
            val left = a == Alignment.TopStart || a == Alignment.BottomStart
            Box(Modifier.align(a).size(30.dp)) {
                Box(Modifier.align(if (top) Alignment.TopStart else Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.White))
                Box(Modifier.align(if (left) Alignment.TopStart else Alignment.TopEnd).width(3.dp).fillMaxHeight().background(Color.White))
            }
        }
        if (recognizingSku != null) {
            Row(Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xCC0A0C11)).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spinner(14.dp)
                Text("인식 중 · ", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(recognizingSku, fontFamily = Mono, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF4F7CFF)))
        }
    }
}

// ───────────────────────── 출고 폼 ─────────────────────────

@Composable
private fun OutForm(nav: Nav, part: Part?, unknownCode: String?, preset: Boolean, onBack: () -> Unit, onRescan: () -> Unit) {
    val me = LocalMe.current
    val app = LocalAppData.current
    val invRepo = remember { InventoryRepository() }
    val scope = rememberCoroutineScope()

    val unknown = part == null
    val available = if (part != null) app.availableOf(part.sku, part.qty) else 0
    val coNo = remember { nextCoNo() }   // 출고 번호 — 멱등 referenceNumber. 재시도 시 재사용.

    var qty by remember { mutableStateOf(1) }
    var reason by remember { mutableStateOf(OUT_REASONS.first().id) }
    var confirmOpen by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<OutUi?>(null) }

    val over = qty > available
    val canSubmit = !unknown && available > 0 && !over && qty >= 1 && !submitting

    fun submit() {
        if (submitting || part == null) return
        submitting = true; confirmOpen = false
        scope.launch {
            val r: OutUi = if (BuildConfig.USE_API) {
                when (val out = invRepo.outbound(coNo, me.warehouse, part.sku, qty)) {
                    is OutboundResult.Ok -> { app.applyOutbound(part, qty, reason, out.referenceNumber); OutUi.Ok(out.referenceNumber, out.remaining) }
                    is OutboundResult.Insufficient -> OutUi.Conflict(out.serverAvailable)
                    OutboundResult.Unauthorized -> OutUi.Unauthorized
                    OutboundResult.Offline -> OutUi.Offline
                    is OutboundResult.Error -> OutUi.Err(out.message)
                }
            } else {
                app.applyOutbound(part, qty, reason, coNo)
                OutUi.Ok(coNo, (available - qty).coerceAtLeast(0))
            }
            result = r; submitting = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "출고 스캔", back = true, chip = "OUT", onBack = onBack)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)) {
                // 품목 확인 카드
                Row(Modifier.fillMaxWidth().bbdCard().background(if (unknown) Color(0xFFF7F8FA) else T.card, RoundedCornerShape(16.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    PartThumb(part?.thumb ?: "box", size = 52.dp, active = !unknown)
                    Column(Modifier.weight(1f)) {
                        Text(part?.name ?: "미등록 부품", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (unknown) T.ink2 else T.ink)
                        Spacer(Modifier.size(4.dp))
                        CodeText(part?.sku ?: unknownCode ?: "—", size = 12.5.sp, color = if (unknown) T.ink3 else T.ink2)
                    }
                    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(T.miniTile).padding(horizontal = 9.dp, vertical = 4.dp)) {
                        Text(if (unknown) "미등록" else part!!.cat, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                    }
                }

                if (preset && !unknown) {
                    Spacer(Modifier.size(8.dp))
                    Box(Modifier.fillMaxWidth().clickable(onClick = onRescan).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BbdIcon("scan", 15.dp, T.blue)
                            Text("다른 부품 스캔으로 변경", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = T.blue)
                        }
                    }
                }
                Spacer(Modifier.size(14.dp))

                if (unknown) {
                    ErrCard("alert", red = true, title = "미등록 부품입니다", body = "스캔한 코드 ${unknownCode ?: ""} 를 부품 마스터에서 찾을 수 없습니다. 코드를 확인하거나 본사에 문의하세요.")
                } else {
                    // 가용재고 배너
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (available <= 0) T.redSoft else T.field).border(1.dp, if (available <= 0) T.redBlockBorder else T.line, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BbdIcon("box", 19.dp, if (available <= 0) T.red else T.ink2, sw = 1.9f)
                            Column {
                                Text("지점 가용재고", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = T.ink2)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("$available", fontFamily = Mono, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (available <= 0) T.red else T.ink)
                                    Text(part!!.unit, fontSize = 12.sp, color = T.ink3, modifier = Modifier.padding(start = 3.dp, bottom = 2.dp))
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("안전재고 ${part!!.safety} ${part.unit}", fontSize = 11.5.sp, color = T.ink3Read)
                            Text(me.warehouse, fontSize = 11.5.sp, color = T.ink3Read)
                        }
                    }
                    Spacer(Modifier.size(16.dp))

                    if (available <= 0) {
                        ErrCard("ban", red = true, title = "가용재고가 없습니다", body = "이 부품은 현재 지점 재고가 0 ${part!!.unit} 입니다. 출고할 수 없습니다. 입고 후 다시 시도하세요.")
                    } else {
                        // 수량 스텝퍼
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("출고 수량 ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink)
                            Text("*", fontSize = 14.sp, color = T.blue, modifier = Modifier.weight(1f))
                            Text("최대 $available ${part!!.unit}", fontSize = 12.sp, color = T.ink3, fontFamily = Mono)
                        }
                        Spacer(Modifier.size(8.dp))
                        Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, if (over) T.red else T.line, RoundedCornerShape(14.dp))) {
                            StepBtn("minus", enabled = qty > 1) { if (qty > 1) qty-- }
                            Row(Modifier.weight(1f).fillMaxHeight().background(if (over) Color(0xFFFFF7F7) else T.card), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("$qty", fontFamily = Mono, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = if (over) T.red else T.ink)
                                Text(part.unit, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink3, modifier = Modifier.padding(start = 6.dp))
                            }
                            StepBtn("plus", enabled = true) { qty++ }
                        }
                        if (over) {
                            Spacer(Modifier.size(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BbdIcon("alert", 15.dp, T.red, sw = 2f)
                                Text("가용재고를 ${qty - available} ${part.unit} 초과합니다 · 부분 출고 없음", fontSize = 13.sp, color = T.red, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.size(18.dp))

                        // 출고 사유(필수)
                        Row { Text("출고 사유 ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink); Text("*", fontSize = 14.sp, color = T.blue) }
                        Spacer(Modifier.size(9.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OUT_REASONS.forEach { r ->
                                val on = reason == r.id
                                Row(
                                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(999.dp)).background(if (on) T.blue else T.card).border(1.5.dp, if (on) T.blue else T.line, RoundedCornerShape(999.dp)).clickable { reason = r.id },
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                                ) {
                                    BbdIcon(r.icon, 15.dp, if (on) Color.White else if (r.amber) T.amber else T.ink3Read, sw = 2f)
                                    Spacer(Modifier.size(5.dp))
                                    Text(r.id, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else T.ink2, maxLines = 1)
                                }
                            }
                        }
                        Spacer(Modifier.size(18.dp))

                        // 출고 창고(내 지점 고정)
                        Text("출고 창고", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink)
                        Spacer(Modifier.size(9.dp))
                        Row(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            BbdIcon("home", 19.dp, T.ink2)
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(me.warehouseName, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink)
                                CodeText(" ${me.warehouse}", size = 12.5.sp, color = T.ink3)
                            }
                            Text("내 지점 고정", fontSize = 11.sp, color = T.ink3)
                        }
                    }
                }
            }
            StickyBar {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (canSubmit) T.blue else Color(0xFFDFE4EE)).clickable(enabled = canSubmit) { confirmOpen = true }.padding(vertical = 17.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BbdIcon("arrowUp", 20.dp, if (canSubmit) Color.White else T.ink3, sw = 2.2f)
                        Text("출고 확정", color = if (canSubmit) Color.White else T.ink3, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 확인 모달
        ModalHost(confirmOpen, { if (!submitting) confirmOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 20.dp)) {
                Text("출고 처리할까요?", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.field).border(1.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 14.dp)) {
                    OutRow("부품", part?.name ?: "—")
                    Spacer(Modifier.size(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("수량", fontSize = 12.5.sp, color = T.ink3, modifier = Modifier.weight(1f))
                        Text("−$qty ${part?.unit ?: ""}", fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = T.red)
                    }
                    Spacer(Modifier.size(8.dp))
                    OutRow("사유", reason)
                }
                Spacer(Modifier.size(16.dp))
                Text("확인 시 지점 가용재고에서 $qty ${part?.unit ?: ""} 차감됩니다.", fontSize = 12.sp, color = T.ink3, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.size(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModalGhostBtn("취소", Modifier.weight(1f), enabled = !submitting) { confirmOpen = false }
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(T.blue).clickable(enabled = !submitting) { submit() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (submitting) Spinner(16.dp)
                            Text(if (submitting) "처리 중…" else "출고 확정", color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // 결과 모달
        ModalHost(result != null, { if (result is OutUi.Ok) nav.tab("home") else result = null }) {
            when (val r = result) {
                is OutUi.Ok -> Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(T.greenSoft), contentAlignment = Alignment.Center) { BbdIcon("check", 28.dp, T.green, sw = 2.6f) }
                    Spacer(Modifier.size(14.dp))
                    Text("출고 완료", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                    Spacer(Modifier.size(6.dp))
                    Text("${part?.name ?: ""} $qty${part?.unit ?: ""} 차감 · $reason", fontSize = 13.5.sp, color = T.ink2, textAlign = TextAlign.Center)
                    Spacer(Modifier.size(14.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.field).border(1.dp, T.line, RoundedCornerShape(13.dp)).padding(vertical = 13.dp), horizontalArrangement = Arrangement.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("출고 번호", fontSize = 11.sp, color = T.ink3Read); Spacer(Modifier.size(3.dp)); CodeText(r.coNo, size = 13.5.sp, color = T.ink)
                        }
                        Box(Modifier.padding(horizontal = 18.dp).width(1.dp).height(34.dp).background(T.line))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("남은 가용재고", fontSize = 11.sp, color = T.ink3Read); Spacer(Modifier.size(3.dp))
                            Text("${r.remaining} ${part?.unit ?: ""}", fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                        }
                    }
                    Spacer(Modifier.size(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModalGhostBtn("홈으로", Modifier.weight(1f)) { nav.tab("home") }
                        ModalSolidBtn("계속 출고", Modifier.weight(1f)) { nav.scanOut() }
                    }
                }
                is OutUi.Conflict -> Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(T.redSoft), contentAlignment = Alignment.Center) { BbdIcon("ban", 26.dp, T.red, sw = 2.2f) }
                    Spacer(Modifier.size(14.dp))
                    Text("재고가 부족해 출고할 수 없습니다", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, textAlign = TextAlign.Center)
                    Spacer(Modifier.size(8.dp))
                    Text("서버 가용재고 ${r.serverAvailable} ${part?.unit ?: ""} < 요청 $qty ${part?.unit ?: ""}. 부분 차감은 없습니다.", fontSize = 13.sp, color = T.ink2, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    Spacer(Modifier.size(16.dp))
                    if (r.serverAvailable >= 1) {
                        ModalSolidBtn("가용 ${r.serverAvailable}${part?.unit ?: ""}로 수량 조정", Modifier.fillMaxWidth()) { qty = r.serverAvailable.coerceAtLeast(1); result = null }
                        Spacer(Modifier.size(9.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModalGhostBtn("취소", Modifier.weight(1f)) { result = null }
                        ModalGhostBtn("다시 시도", Modifier.weight(1f)) { result = null; submit() }
                    }
                }
                OutUi.Offline -> ResultSimple("wifiOff", T.miniTile, T.ink2, "연결 없음", "네트워크에 연결되어 있지 않습니다. 출고는 처리되지 않았습니다. 연결을 확인하고 다시 시도하세요.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModalGhostBtn("닫기", Modifier.weight(1f)) { result = null }
                        ModalSolidBtn("다시 시도", Modifier.weight(1f)) { result = null; submit() }
                    }
                }
                OutUi.Unauthorized -> ResultSimple("lock", T.amberSoft, T.amber, "세션이 만료되었습니다", "보안을 위해 다시 로그인해야 합니다. 출고는 처리되지 않았습니다.") {
                    ModalSolidBtn("다시 로그인", Modifier.fillMaxWidth()) { nav.logout() }
                }
                is OutUi.Err -> ResultSimple("alert", T.redSoft, T.red, "출고에 실패했습니다", r.message) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModalGhostBtn("닫기", Modifier.weight(1f)) { result = null }
                        ModalSolidBtn("다시 시도", Modifier.weight(1f)) { result = null; submit() }
                    }
                }
                null -> {}
            }
        }
    }
}

@Composable
private fun StepBtn(icon: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.width(64.dp).fillMaxHeight().background(T.field).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        BbdIcon(icon, 22.dp, if (enabled) T.ink else T.ink3, sw = 2.2f)
    }
}

@Composable
private fun OutRow(k: String, v: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(k, fontSize = 12.5.sp, color = T.ink3, modifier = Modifier.weight(1f))
        Text(v, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink)
    }
}

@Composable
private fun ErrCard(icon: String, red: Boolean, title: String, body: String) {
    val bg = if (red) T.redSoft else T.amberSoft
    val ic = if (red) T.red else T.amber
    val tt = if (red) T.redBlockTitle else T.amberBlockTitle
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).border(1.dp, if (red) T.redBlockBorder else T.amberBlockBorder, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BbdIcon(icon, 20.dp, ic, sw = 2f)
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = tt)
            Spacer(Modifier.size(4.dp))
            Text(body, fontSize = 12.5.sp, color = T.ink2, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun ResultSimple(icon: String, iconBg: Color, iconColor: Color, title: String, body: String, actions: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) { BbdIcon(icon, 25.dp, iconColor, sw = 2f) }
        Spacer(Modifier.size(14.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.size(6.dp))
        Text(body, fontSize = 13.sp, color = T.ink2, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.size(18.dp))
        actions()
    }
}

@Composable
private fun ModalGhostBtn(label: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp)).clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, color = T.ink2, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, fontFamily = Pretendard)
    }
}

@Composable
private fun ModalSolidBtn(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(13.dp)).background(T.blue).clickable(onClick = onClick).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Pretendard)
    }
}
