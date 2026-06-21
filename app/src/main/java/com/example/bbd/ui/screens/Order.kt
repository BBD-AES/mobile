package com.example.bbd.ui.screens

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.Pr
import com.example.bbd.data.PrStatus
import com.example.bbd.data.Seed
import com.example.bbd.data.StockStatus
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.HeaderRight
import com.example.bbd.ui.Nav
import com.example.bbd.ui.PartThumb
import com.example.bbd.ui.SheetHost
import com.example.bbd.ui.StatusPill
import com.example.bbd.ui.ToastHost
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.bottomBorder
import com.example.bbd.ui.topBorder
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T
import androidx.compose.runtime.produceState
import com.example.bbd.BuildConfig
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.SalesOrderSummaryDto
import com.example.bbd.data.repo.SalesOrderRepository
import com.example.bbd.ui.state.EmptyState
import com.example.bbd.ui.state.ErrorState
import com.example.bbd.ui.state.LoadingRows

private data class PrMeta(val bg: Color, val fg: Color, val dot: Color)

private fun prMeta(s: PrStatus): PrMeta = when (s) {
    PrStatus.REQUESTED -> PrMeta(T.lineSoft, T.ink2, T.ink3)
    PrStatus.APPROVED -> PrMeta(T.blueSoft, T.blueInk, T.blue)
    PrStatus.SHIPPED -> PrMeta(T.blueSoft, T.blueInk, T.blue)
    PrStatus.RECEIVED -> PrMeta(T.receivedBg, T.receivedFg, T.green)
    PrStatus.REJECTED -> PrMeta(T.redSoft, T.redBlockTitle, T.red)
}

@Composable
fun OrderScreen(nav: Nav) {
    if (BuildConfig.USE_API) OrderScreenApi(nav) else OrderScreenMock(nav)
}

@Composable
private fun OrderScreenMock(nav: Nav) {
    var tab by remember { mutableStateOf("open") }
    var create by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(Seed.PR) }

    val open = list.filter { it.status.isOpen }
    val done = list.filter { it.status.isDone }
    val shown = if (tab == "open") open else done

    if (toast.isNotBlank()) {
        LaunchedEffect(toast) { kotlinx.coroutines.delay(1800); toast = "" }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "보충 발주 요청", back = true, right = HeaderRight.BELL, onBack = { nav.pop() })
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp)) {
                // 지점 + 권한
                Row(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(T.blueSoft), contentAlignment = Alignment.Center) {
                        BbdIcon("pin", 19.dp, T.blue)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(Seed.USER.branch, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                        Spacer(Modifier.size(2.dp))
                        Text("본사로 보충을 요청합니다", fontSize = 12.sp, color = T.ink3)
                    }
                    CodeText(Seed.USER.warehouse, size = 12.sp)
                }
                Spacer(Modifier.size(14.dp))

                // 세그먼트
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(T.segTrack).padding(4.dp)) {
                    SegBtn("진행 중", open.size, tab == "open", Modifier.weight(1f)) { tab = "open" }
                    SegBtn("종료", done.size, tab == "done", Modifier.weight(1f)) { tab = "done" }
                }
                Spacer(Modifier.size(16.dp))

                if (shown.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 46.dp), contentAlignment = Alignment.Center) {
                        Text(if (tab == "open") "진행 중인 발주 요청이 없습니다." else "종료된 발주 요청이 없습니다.", fontSize = 14.sp, color = T.ink3)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        shown.forEach { OrderCard(it) }
                    }
                }
            }

            // 하단 작성 버튼
            Box(Modifier.fillMaxWidth().background(T.card).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(14.dp), clip = false, spotColor = T.blue, ambientColor = T.blue)
                        .clip(RoundedCornerShape(14.dp)).background(T.blue).clickable { create = true }.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    BbdIcon("plus", 21.dp, Color.White, sw = 2.4f)
                    Spacer(Modifier.size(8.dp))
                    Text("발주 요청 작성", color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        CreateSheet(open = create, onClose = { create = false }) { draft ->
            // 올해 prefix 의 기존 발주 번호 최댓값 + 1 → 연도 종속 제거 + prefix 범위로만 채번
            val prefix = "PR-${java.time.LocalDate.now().year}-"
            val maxN = list.filter { it.id.startsWith(prefix) }
                .mapNotNull { it.id.removePrefix(prefix).toIntOrNull() }
                .maxOrNull() ?: 0
            val newPr = draft.copy(id = "$prefix%04d".format(maxN + 1))
            list = listOf(newPr) + list
            create = false
            tab = "open"
            toast = "발주 요청 등록 완료 · ${newPr.qty} ${newPr.unit}"
        }
        ToastHost(toast)
    }
}

@Composable
private fun SegBtn(label: String, n: Int, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.then(if (on) Modifier.shadow(2.dp, RoundedCornerShape(9.dp), clip = false) else Modifier)
            .clip(RoundedCornerShape(9.dp)).background(if (on) T.card else Color.Transparent).clickable(onClick = onClick).padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) T.ink else T.ink2)
        Spacer(Modifier.size(6.dp))
        Text("$n", fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) T.blue else T.ink3)
    }
}

@Composable
private fun PRBadge(status: PrStatus) {
    val m = prMeta(status)
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(m.bg).padding(start = 9.dp, end = 11.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(m.dot))
        Text(status.label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = m.fg)
    }
}

@Composable
private fun OrderCard(pr: Pr) {
    val rejected = pr.status == PrStatus.REJECTED
    Column(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 15.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CodeText(pr.id, size = 12.5.sp, color = T.ink3)
            Spacer(Modifier.weight(1f))
            PRBadge(pr.status)
        }
        Spacer(Modifier.size(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            PartThumb(Seed.partBySku(pr.sku)?.thumb ?: "box", size = 46.dp, active = !rejected)
            Column(Modifier.weight(1f)) {
                Text(pr.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CodeText(pr.sku, size = 12.sp)
                    Text("· ${pr.reason}", fontSize = 11.5.sp, color = T.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${pr.qty}", fontFamily = Mono, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                Text(" ${pr.unit}", fontSize = 11.sp, color = T.ink3, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.size(12.dp))
        Row(Modifier.fillMaxWidth().topBorder(T.lineSoft).padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BbdIcon(if (rejected) "ban" else "info", 14.dp, if (rejected) T.red else T.ink3)
                Text(pr.note, fontSize = 12.5.sp, color = if (rejected) T.red else T.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.size(10.dp))
            CodeText(pr.date, size = 11.5.sp, color = T.ink3)
        }
    }
}

// ───────────────────────── 발주 요청 작성 시트 ─────────────────────────

@Composable
private fun androidx.compose.foundation.layout.BoxScope.CreateSheet(open: Boolean, onClose: () -> Unit, onSubmit: (Pr) -> Unit) {
    val recommend = Seed.PARTS.filter { it.status != StockStatus.OK }
    var sku by remember { mutableStateOf(recommend.firstOrNull()?.sku ?: Seed.PARTS.first().sku) }
    var qty by remember { mutableStateOf(20) }
    var reason by remember { mutableStateOf("안전재고 미달") }
    var pick by remember { mutableStateOf(false) }
    val part = Seed.partBySku(sku) ?: Seed.PARTS.first()
    val reasons = listOf("안전재고 미달", "무재고 보충", "수요 증가", "예방 정비")

    SheetHost(open = open, onClose = onClose, title = "발주 요청 작성") {
        Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp)) {
            Text("부품", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2)
            Spacer(Modifier.size(9.dp))
            // 선택 버튼
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, if (pick) T.blue else T.line, RoundedCornerShape(13.dp)).clickable { pick = !pick }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PartThumb(part.thumb, size = 44.dp, active = true)
                Column(Modifier.weight(1f)) {
                    Text(part.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.size(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CodeText(part.sku, size = 12.sp)
                        Text("· 현재고 ${part.qty}${part.unit}", fontSize = 11.5.sp, color = T.ink3)
                    }
                }
                BbdIcon(if (pick) "chevD" else "chevR", 18.dp, T.ink3)
            }
            if (pick) {
                Spacer(Modifier.size(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).border(1.dp, T.line, RoundedCornerShape(13.dp))) {
                    Seed.PARTS.forEachIndexed { i, p ->
                        val on = p.sku == sku
                        Row(
                            Modifier.fillMaxWidth().background(if (on) T.hotBg else T.card).then(if (i < Seed.PARTS.lastIndex) Modifier.bottomBorder(T.lineSoft) else Modifier).clickable { sku = p.sku; pick = false }.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            PartThumb(p.thumb, size = 36.dp, active = on)
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                CodeText(p.sku, size = 11.5.sp)
                            }
                            if (p.status != StockStatus.OK) StatusPill(p.status)
                            if (on) { Spacer(Modifier.size(6.dp)); BbdIcon("check", 18.dp, T.blue, sw = 2.4f) }
                        }
                    }
                }
            }
            Spacer(Modifier.size(16.dp))

            Row { Text("요청 수량 ", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2); Text("*", fontSize = 13.sp, color = T.blue, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.size(9.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(13.dp)).border(1.5.dp, T.line, RoundedCornerShape(13.dp)), verticalAlignment = Alignment.CenterVertically) {
                StepBtn("minus", 60.dp) { if (qty > 1) qty = (qty - 5).coerceAtLeast(1) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                    Text("$qty", fontFamily = Mono, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                    Text(" ${part.unit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink3)
                }
                StepBtn("plus", 60.dp) { qty += 5 }
            }
            Spacer(Modifier.size(16.dp))

            Text("사유", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2)
            Spacer(Modifier.size(9.dp))
            FlowChips(reasons, reason) { reason = it }
            Spacer(Modifier.size(22.dp))

            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.blue).clickable {
                    onSubmit(
                        Pr("", part.sku, part.name, qty, part.unit, PrStatus.REQUESTED, reason, java.time.LocalDate.now().toString(), java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), Seed.USER.name, "본사 승인 대기 중"),
                    )
                    qty = 20; reason = "안전재고 미달"
                }.padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("본사로 발주 요청", color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.size(11.dp))
            Text("요청 후 본사 승인을 기다립니다 · 승인·거절은 본사에서 처리해요", fontSize = 12.sp, color = T.ink3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ───────────────────────── 실 API 경로 (USE_API) ─────────────────────────
// sales GET /api/v1/sales-orders 로 내 지점 보충 발주 로드 → 로딩/에러/빈/목록.
// 요약 응답엔 라인이 없어 주문 단위 카드로 렌더(부품 단위 추측 금지). 작성(POST)·라인 상세·인증은 후속.

@Composable
private fun OrderScreenApi(nav: Nav) {
    val repo = remember { SalesOrderRepository() }
    var reloadKey by remember { mutableStateOf(0) }
    val state by produceState<UiState<List<SalesOrderSummaryDto>>>(UiState.Loading, reloadKey) {
        value = UiState.Loading
        value = repo.branchOrders(Seed.USER.warehouse)
    }
    Column(Modifier.fillMaxSize().background(T.bg)) {
        Header(title = "보충 발주 요청", back = true, right = HeaderRight.BELL, onBack = { nav.pop() })
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().bbdCard().padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(T.blueSoft), contentAlignment = Alignment.Center) {
                    BbdIcon("pin", 19.dp, T.blue)
                }
                Column(Modifier.weight(1f)) {
                    Text(Seed.USER.branch, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                    Spacer(Modifier.size(2.dp))
                    Text("본사로 보충을 요청합니다", fontSize = 12.sp, color = T.ink3)
                }
                CodeText(Seed.USER.warehouse, size = 12.sp)
            }
            Spacer(Modifier.size(14.dp))

            when (val s = state) {
                is UiState.Loading -> LoadingRows()
                is UiState.Error -> ErrorState(s.message, onRetry = { reloadKey++ })
                is UiState.Success ->
                    if (s.data.isEmpty()) {
                        EmptyState("list", "보충 발주가 없습니다", "본사로 보낸 보충 발주 요청이 여기에 표시됩니다.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                            s.data.forEach { OrderApiCard(it) }
                        }
                    }
            }
        }
    }
}

@Composable
private fun OrderApiCard(o: SalesOrderSummaryDto) {
    Column(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 15.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CodeText(o.soNumber ?: "-", size = 13.sp, color = T.ink)
            Spacer(Modifier.weight(1f))
            SoStatusChip(o.status)
        }
        Spacer(Modifier.size(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(o.toWarehouseName ?: o.toWarehouseCode ?: "", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!o.note.isNullOrBlank()) {
                    Spacer(Modifier.size(3.dp))
                    Text(o.note!!, fontSize = 12.sp, color = T.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.size(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(wonText(o.totalAmount), fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                Spacer(Modifier.size(2.dp))
                CodeText((o.requestedAt ?: "").take(10), size = 11.5.sp, color = T.ink3)
            }
        }
    }
}

@Composable
private fun SoStatusChip(status: String?) {
    val label: String; val bg: Color; val fg: Color; val dot: Color
    when (status) {
        "REQUESTED" -> { label = "요청"; bg = T.lineSoft; fg = T.ink2; dot = T.ink3 }
        "SUBMITTED" -> { label = "제출"; bg = T.blueSoft; fg = T.blueInk; dot = T.blue }
        "IN_FULFILLMENT" -> { label = "처리중"; bg = T.blueSoft; fg = T.blueInk; dot = T.blue }
        "BACKORDERED" -> { label = "백오더"; bg = T.amberBlockBg; fg = T.amberBlockTitle; dot = T.amber }
        "RECEIVED" -> { label = "입고완료"; bg = T.receivedBg; fg = T.receivedFg; dot = T.green }
        "REJECTED" -> { label = "반려"; bg = T.redSoft; fg = T.redBlockTitle; dot = T.red }
        "CANCELED" -> { label = "취소"; bg = T.lineSoft; fg = T.ink3; dot = T.ink3 }
        else -> { label = status ?: "-"; bg = T.lineSoft; fg = T.ink2; dot = T.ink3 }
    }
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, fontFamily = Pretendard)
    }
}

private fun wonText(v: Double?): String = if (v == null) "—" else "%,d원".format(v.toLong())
