package com.example.bbd.ui.screens

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.Part
import com.example.bbd.data.Seed
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.ModalHost
import com.example.bbd.ui.Nav
import com.example.bbd.ui.PartThumb
import com.example.bbd.ui.ToastHost
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.bottomBorder
import com.example.bbd.ui.topBorder
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScanScreen(nav: Nav, mode: String, preset: Part?) {
    val isOut = mode == "out"
    val title = if (isOut) "출고 스캔" else "입고 스캔"
    val chip = if (isOut) "OUT" else "IN"
    val target = remember(isOut) {
        Seed.partBySku(if (isOut) "BBD-BRK-4001" else "BBD-FLT-2001") ?: Seed.PARTS.first()
    }

    var phase by remember { mutableStateOf(if (preset != null) "form" else "scanning") }
    var part by remember { mutableStateOf(preset) }
    var manual by remember { mutableStateOf(false) }

    fun doScan(found: Part?) {
        part = found ?: target
        phase = "form"
    }

    if (phase == "form" && part != null) {
        ScanForm(nav, isOut, part!!, title, chip, onBack = {
            if (preset != null) nav.pop() else phase = "scanning"
        })
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = title, back = true, chip = chip, right = HeaderRight.MANUAL, onBack = { nav.pop() }, onRight = { manual = true })
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Viewfinder(isOut) { doScan(null) }
                Spacer(Modifier.size(18.dp))
                // 안내 카드
                Row(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 16.dp, vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(T.blueSoft), contentAlignment = Alignment.Center) {
                        BbdIcon(if (isOut) "arrowUp" else "arrowDn", 19.dp, T.blue, sw = 2f)
                    }
                    Column {
                        Text(
                            buildAnnotatedString {
                                if (isOut) {
                                    append("작업에 사용할 부품의 바코드를 스캔하면 ")
                                    withStyle(SpanStyle(color = T.ink, fontWeight = FontWeight.Bold)) { append("지점 현재고에서 차감") }
                                    append("됩니다.")
                                } else {
                                    append("도착한 부품의 바코드를 스캔하면 ")
                                    withStyle(SpanStyle(color = T.ink, fontWeight = FontWeight.Bold)) { append("지점 창고에 입고") }
                                    append("됩니다.")
                                }
                            },
                            fontSize = 13.sp, color = T.ink2, lineHeight = 20.sp,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            buildAnnotatedString {
                                append("스캔이 어려우면 우측 상단 ")
                                withStyle(SpanStyle(color = T.blue, fontWeight = FontWeight.Bold)) { append("수동 입력") }
                                append("을 이용하세요.")
                            },
                            fontSize = 13.sp, color = T.ink3, lineHeight = 20.sp,
                        )
                    }
                }
            }
        }

        ManualEntryModal(open = manual, isOut = isOut, onClose = { manual = false }) { code ->
            manual = false
            doScan(Seed.partBySku(code) ?: target)
        }
    }
}

@Composable
private fun Viewfinder(isOut: Boolean, onTap: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF2A3040), Color(0xFF14171F), Color(0xFF0D0F15)), center = Offset(0.5f, 0.3f) * 1000f, radius = 900f))
            .clickable(onClick = onTap),
    ) {
        // 상단 라벨
        Row(Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF5B5B)))
            Text("SCAN" + (if (isOut) " · OUT" else " · IN"), fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.sp)
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 13.dp, end = 14.dp).size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            BbdIcon("flash", 17.dp, Color.White)
        }

        // 중앙 프레임 240x150
        Box(Modifier.align(Alignment.Center).width(240.dp).height(150.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val bl = 30.dp.toPx(); val sw = 3.dp.toPx()
                val white = Color.White
                // 코너 브래킷
                fun corner(x: Float, y: Float, dx: Int, dy: Int) {
                    drawLine(white, Offset(x, y), Offset(x + dx * bl, y), sw, cap = StrokeCap.Round)
                    drawLine(white, Offset(x, y), Offset(x, y + dy * bl), sw, cap = StrokeCap.Round)
                }
                corner(0f, 0f, 1, 1)
                corner(w, 0f, -1, 1)
                corner(0f, h, 1, -1)
                corner(w, h, -1, -1)
                // 바코드 (세로 막대들)
                val barTop = 28.dp.toPx(); val barH = 86.dp.toPx()
                val left = 36.dp.toPx(); val right = w - 36.dp.toPx()
                val widths = listOf(3f, 2f, 4f, 2f, 3f, 5f, 2f, 3f, 2f, 4f, 3f, 2f, 5f, 2f, 3f, 2f, 4f, 2f, 3f, 3f)
                var x = left; var i = 0
                while (x < right) {
                    val bw = widths[i % widths.size].dp.toPx()
                    drawLine(Color(0xFFE9EDF5).copy(alpha = 0.92f), Offset(x, barTop), Offset(x, barTop + barH), bw)
                    x += bw + 3.dp.toPx(); i++
                }
            }
            // 스캔 라인
            val tr = rememberInfiniteTransition(label = "scan")
            val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1900, easing = EaseInOut), RepeatMode.Reverse), label = "y")
            Box(
                Modifier.fillMaxWidth().offset(y = (6 + t * 134).dp).height(3.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF4F7CFF), Color.Transparent))),
            )
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("바코드를 화면 안에 맞춰 주세요", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.size(4.dp))
            Text("화면을 탭하면 스캔됩니다 (데모)", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun ScanForm(nav: Nav, isOut: Boolean, part: Part, title: String, chip: String, onBack: () -> Unit) {
    val stock = part.qty
    var qty by remember { mutableStateOf(1) }
    var reason by remember { mutableStateOf(if (isOut) "작업 사용" else "도착 입고 확정") }
    var toast by remember { mutableStateOf("") }
    val over = isOut && qty > stock
    val reasons = if (isOut) listOf("작업 사용", "수리", "교환", "검사") else listOf("도착 입고 확정", "재입고")
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = title, back = true, chip = chip, right = HeaderRight.MANUAL, onBack = onBack, onRight = onBack)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)) {
                // 부품 카드
                Row(Modifier.fillMaxWidth().bbdCard().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    PartThumb(part.thumb, size = 52.dp, active = true)
                    Column(Modifier.weight(1f)) {
                        Text(part.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.3).sp)
                        Spacer(Modifier.size(4.dp)); CodeText(part.sku)
                    }
                    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(T.miniTile).padding(horizontal = 9.dp, vertical = 4.dp)) {
                        Text(part.cat, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = T.ink3)
                    }
                }
                Spacer(Modifier.size(14.dp))

                // 현재고 배너
                val bannerBg = if (over) T.redSoft else if (isOut) T.outBannerBg else T.inBannerBg
                val bannerBorder = if (over) T.redBlockBorder else if (isOut) T.outBannerBorder else T.inBannerBorder
                val bannerIcon = if (over) T.red else if (isOut) T.amber else T.blue
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bannerBg).border(1.dp, bannerBorder, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BbdIcon("alert", 18.dp, bannerIcon, sw = 2f)
                        Column {
                            Text("지점 현재고", fontSize = 12.sp, color = T.ink2, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$stock", fontFamily = Mono, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (over) T.red else T.ink)
                                Text(" ${part.unit}", fontSize = 12.sp, color = T.ink3)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("안전재고 ${part.safety} ${part.unit}", fontSize = 11.5.sp, color = T.ink3)
                        Text(part.wh, fontSize = 11.5.sp, color = T.ink3)
                    }
                }
                Spacer(Modifier.size(18.dp))

                // 수량 스테퍼
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f)) {
                        Text("${if (isOut) "출고" else "입고"} 수량 ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink)
                        Text("*", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.blue)
                    }
                    if (isOut) Text("최대 $stock ${part.unit}", fontSize = 12.sp, color = T.ink3, fontFamily = Mono)
                }
                Spacer(Modifier.size(8.dp))
                Row(
                    Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, if (over) T.red else T.line, RoundedCornerShape(14.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepBtn("minus", 64.dp) { if (qty > 1) qty-- }
                    Row(Modifier.weight(1f).fillMaxSize().background(if (over) Color(0xFFFFF7F7) else T.card), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("$qty", fontFamily = Mono, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = if (over) T.red else T.ink)
                        Text(" ${part.unit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink3)
                    }
                    StepBtn("plus", 64.dp) { qty = (qty + 1).coerceAtMost(999) }
                }
                if (over) {
                    Spacer(Modifier.size(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BbdIcon("alert", 15.dp, T.red, sw = 2f)
                        Text("현재고를 ${qty - stock} ${part.unit} 초과합니다", fontSize = 13.sp, color = T.red, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.size(18.dp))

                // 사유
                Text("사유", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink)
                Spacer(Modifier.size(9.dp))
                FlowChips(reasons, reason) { reason = it }
                Spacer(Modifier.size(18.dp))

                // 창고
                Text("${if (isOut) "출고" else "입고"} 창고", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink)
                Spacer(Modifier.size(9.dp))
                Row(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    BbdIcon("home", 19.dp, T.ink2)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(Seed.USER.warehouseName, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink)
                        Spacer(Modifier.size(8.dp)); CodeText(Seed.USER.warehouse, size = 12.5.sp)
                    }
                    BbdIcon("chevD", 18.dp, T.ink3)
                }
            }

            // 하단 고정 버튼
            Box(Modifier.fillMaxWidth().background(T.card).topBorder().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
                val enabled = !over && qty >= 1
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (enabled) T.blue else T.disabledRed)
                        .then(if (enabled) Modifier.clickable {
                            toast = if (isOut) "출고 등록 완료 · $qty ${part.unit} 차감" else "입고 등록 완료 · $qty ${part.unit} 추가"
                            scope.launch { delay(1200); nav.tab("home") }
                        } else Modifier)
                        .padding(vertical = 17.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    BbdIcon(if (isOut) "arrowUp" else "arrowDn", 20.dp, Color.White, sw = 2.2f)
                    Spacer(Modifier.size(8.dp))
                    Text(if (isOut) "출고 등록" else "입고 등록", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        ToastHost(toast)
    }
}

@Composable
fun StepBtn(icon: String, w: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(Modifier.width(w).fillMaxSize().background(T.field).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        BbdIcon(icon, 22.dp, T.ink, sw = 2.2f)
    }
}

@Composable
fun FlowChips(options: List<String>, selected: String, onPick: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { r ->
            val on = r == selected
            Box(
                Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) T.blue else T.card).border(1.5.dp, if (on) T.blue else T.line, RoundedCornerShape(999.dp)).clickable { onPick(r) }.padding(horizontal = 15.dp, vertical = 9.dp),
            ) {
                Text(r, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else T.ink2)
            }
        }
    }
}

@Composable
private fun ManualEntryModal(open: Boolean, isOut: Boolean, onClose: () -> Unit, onConfirm: (String) -> Unit) {
    var code by remember(open) { mutableStateOf(if (isOut) "BBD-BRK-4001" else "BBD-FLT-2001") }
    Box(Modifier.fillMaxSize()) {
        ModalHost(open = open, onClose = onClose) {
            Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("수동 입력", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, modifier = Modifier.weight(1f))
                    Box(Modifier.size(30.dp).clickable(onClick = onClose), contentAlignment = Alignment.Center) { BbdIcon("x", 22.dp, T.ink2) }
                }
                Row {
                    Text("부품코드 ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink2)
                    Text("*", fontSize = 13.sp, color = T.blue, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).border(1.5.dp, T.blue, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("#", fontFamily = Mono, fontSize = 18.sp, color = T.ink3)
                    Box(Modifier.weight(1f)) {
                        BasicTextField(
                            value = code, onValueChange = { code = it.uppercase() }, singleLine = true,
                            textStyle = TextStyle(fontFamily = Mono, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = T.ink, letterSpacing = 0.4.sp),
                            cursorBrush = SolidColor(T.blue), modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text("대소문자 구분 없음 · 하이픈 자동", fontSize = 12.sp, color = T.ink3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.size(18.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.blue).clickable { onConfirm(code) }.padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("확인", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}
