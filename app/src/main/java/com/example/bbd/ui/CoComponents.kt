package com.example.bbd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.remote.CloseOrderResult
import com.example.bbd.data.remote.ConfirmOrderResult
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.CustomerOrderDetailDto
import com.example.bbd.data.remote.dto.CustomerOrderLineDto
import com.example.bbd.data.remote.dto.CustomerOrderSummaryDto
import com.example.bbd.data.repo.CustomerOrderRepository
import com.example.bbd.ui.state.RowSkeleton
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.T
import kotlinx.coroutines.launch
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────
// 현장 수주(CustomerOrder) 표시·액션 조각 — 이력 [수주] 세그먼트에서 사용.
// 생명주기: OPEN(등록) → CONFIRMED(확정) → CLOSED(종료=지점재고 차감). 취소=CANCELED.
// 종료는 부분출고 없이 전 라인 전량 차감(부족하면 atomic 409, 차감 0).
// ─────────────────────────────────────────────────────────────────────────

/** CO 상태 배지(SoBadge 와 동일 톤). */
@Composable
fun CoBadge(status: String?) {
    val (label, dot, bg, fg) = when (status) {
        "OPEN" -> CoQuad("등록", T.amber, T.amberSoft, T.amberInk)
        "CONFIRMED" -> CoQuad("확정", T.blue, T.blueSoft, T.blueInk)
        "CLOSED" -> CoQuad("종료", T.green, T.greenSoft, T.greenInk)
        "CANCELED" -> CoQuad("취소", T.ink3, T.lineSoft, T.ink2)
        else -> CoQuad(status ?: "-", T.ink3, T.lineSoft, T.ink2)
    }
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private data class CoQuad(val label: String, val dot: Color, val bg: Color, val fg: Color)

/** CO 목록 행 — 수주번호·상태·고객·일자·금액. (라인 수량은 상세에서.) */
@Composable
fun CoRow(o: CustomerOrderSummaryDto, onClick: () -> Unit) {
    val day = (o.closedAt ?: o.confirmedAt ?: o.requestedAt ?: "").take(10)
    val vis = coVis(o.status)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card)
            .border(1.dp, T.line, RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(vis.second), contentAlignment = Alignment.Center) {
            BbdIcon("doc", 18.dp, vis.first, sw = 2f)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CodeText(o.coNumber ?: "-", size = 13.5.sp, color = T.ink, weight = FontWeight.SemiBold)
                CoBadge(o.status)
            }
            Spacer(Modifier.size(3.dp))
            Text(
                o.customerName?.takeIf { it.isNotBlank() } ?: "현장 판매",
                fontSize = 12.sp, color = T.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (day.isNotBlank()) CodeText(day, size = 11.5.sp, color = T.ink3Read)
            val amt = o.totalAmount
            if (amt != null && amt > 0) {
                Spacer(Modifier.size(2.dp))
                Text(won(amt), fontFamily = Mono, fontSize = 11.5.sp, color = T.ink3Read)
            }
        }
    }
}

private fun coVis(status: String?): Pair<Color, Color> = when (status) {
    "OPEN" -> T.amber to T.amberSoft
    "CONFIRMED" -> T.blue to T.blueSoft
    "CLOSED" -> T.green to T.greenSoft
    else -> T.ink3 to T.lineSoft
}

/**
 * 수주 상세 바텀시트 — 라인(차감 대상) + 상태별 액션.
 *  OPEN → [수주 확정]  ·  CONFIRMED → [종료 · 지점재고 차감](인라인 재확인)  ·  CLOSED/CANCELED → 읽기전용.
 *  onResult=토스트 보고(스크린 루트 Box 에서 ToastHost 렌더), onMutated=목록 리로드.
 */
@Composable
fun BoxScope.CoDetailSheet(
    coNumber: String?,
    repo: CustomerOrderRepository,
    onResult: (String, ToastKind) -> Unit,
    onMutated: () -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var reload by remember(coNumber) { mutableStateOf(0) }
    var busy by remember(coNumber) { mutableStateOf(false) }
    var confirming by remember(coNumber) { mutableStateOf(false) } // 종료 인라인 재확인
    // 한 종료 시도 = 1키(재시도 동일키). confirming 진입 시 mint, 실패로 빠지면 다음 시도에 새 키.
    val closeKey = remember(coNumber, confirming) { UUID.randomUUID().toString() }

    val state by produceState<UiState<CustomerOrderDetailDto>>(UiState.Loading, coNumber, reload) {
        value = UiState.Loading
        value = if (coNumber == null) UiState.Loading else repo.detail(coNumber)
    }

    SheetHost(open = coNumber != null, onClose = onClose, title = "수주 상세") {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp)) {
            when (val s = state) {
                is UiState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { repeat(3) { RowSkeleton() } }
                is UiState.Error -> Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(s.message, fontSize = 13.sp, color = T.ink3Read)
                    CoActionButton("다시 시도", "refresh", false, T.card, T.ink2, border = true) { reload++ }
                }
                is UiState.Success -> {
                    val co = s.data
                    val totalQty = co.lines.sumOf { it.quantity }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CodeText(co.coNumber ?: "-", size = 15.sp, color = T.ink)
                        CoBadge(co.status)
                    }
                    Spacer(Modifier.size(12.dp))
                    // 고객 + 메모
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFFF7F9FC))
                            .border(1.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BbdIcon("user", 18.dp, T.ink2)
                        Column(Modifier.weight(1f)) {
                            Text(co.customerName?.takeIf { it.isNotBlank() } ?: "현장 판매", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink)
                            val note = co.note
                            if (!note.isNullOrBlank()) {
                                Spacer(Modifier.size(2.dp)); Text(note, fontSize = 12.sp, color = T.ink3Read, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("차감 품목", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2, modifier = Modifier.weight(1f))
                        Text("${co.lines.size}건", fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                    }
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 14.dp)) {
                        co.lines.forEachIndexed { i, l -> CoLineRow(l, last = i == co.lines.lastIndex) }
                    }
                    Spacer(Modifier.size(12.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                        Text("총 차감 수량", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.ink2, modifier = Modifier.weight(1f))
                        Text("$totalQty", fontFamily = Mono, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                    }
                    Spacer(Modifier.size(18.dp))

                    when (co.status) {
                        "OPEN" -> CoActionButton("수주 확정", "check", busy, T.blue, Color.White) {
                            scope.launch {
                                busy = true
                                when (val r = repo.confirm(co.coNumber.orEmpty())) {
                                    is ConfirmOrderResult.Ok -> { onResult("수주 확정됨 — 종료(차감) 가능", ToastKind.OK); onMutated(); reload++ }
                                    is ConfirmOrderResult.Conflict -> onResult(r.message, ToastKind.ERR)
                                    ConfirmOrderResult.Unauthorized -> onResult("세션이 만료되었습니다. 다시 로그인해 주세요.", ToastKind.ERR)
                                    ConfirmOrderResult.Offline -> onResult("네트워크에 연결되어 있지 않습니다.", ToastKind.ERR)
                                    is ConfirmOrderResult.Error -> onResult(r.message, ToastKind.ERR)
                                }
                                busy = false
                            }
                        }

                        "CONFIRMED" -> if (!confirming) {
                            CoActionButton("종료 · 지점재고 차감", "box", busy, T.blue, Color.White) { confirming = true }
                        } else {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.amberSoft)
                                    .border(1.dp, T.amber.copy(alpha = 0.4f), RoundedCornerShape(13.dp)).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "종료하면 지점 재고에서 총 ${totalQty}개가 차감되며 되돌릴 수 없습니다. 재고가 부족하면 종료되지 않습니다.",
                                    fontSize = 13.sp, color = T.amberInk, fontWeight = FontWeight.SemiBold,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.weight(1f)) { CoActionButton("취소", null, busy, T.card, T.ink2, border = true) { confirming = false } }
                                    Box(Modifier.weight(1f)) {
                                        CoActionButton("종료", "check", busy, T.blue, Color.White) {
                                            scope.launch {
                                                busy = true
                                                when (val r = repo.close(co.coNumber.orEmpty(), closeKey)) {
                                                    is CloseOrderResult.Ok -> { onResult("종료 · 지점재고 차감 완료", ToastKind.OK); onMutated(); onClose() }
                                                    is CloseOrderResult.Insufficient -> { onResult(r.message, ToastKind.ERR); confirming = false }
                                                    is CloseOrderResult.NotClosable -> { onResult(r.message, ToastKind.ERR); onMutated(); onClose() }
                                                    CloseOrderResult.Unauthorized -> onResult("세션이 만료되었습니다. 다시 로그인해 주세요.", ToastKind.ERR)
                                                    CloseOrderResult.Offline -> onResult("네트워크에 연결되어 있지 않습니다.", ToastKind.ERR)
                                                    is CloseOrderResult.Error -> onResult(r.message, ToastKind.ERR)
                                                }
                                                busy = false
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "CLOSED" -> CoFooter("check", T.green, "종료 완료", co.closedBy, co.closedAt)
                        "CANCELED" -> CoFooter("ban", T.ink3, "취소됨", co.canceledBy, co.canceledAt)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CoLineRow(l: CustomerOrderLineDto, last: Boolean) {
    Row(
        Modifier.fillMaxWidth().then(if (!last) Modifier.bottomBorder(T.lineSoft) else Modifier).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PartThumb("box", size = 42.dp)
        Column(Modifier.weight(1f)) {
            Text(l.nameSnapshot?.takeIf { it.isNotBlank() } ?: (l.sku ?: "-"), fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            CodeText(l.sku ?: "-", size = 12.sp)
        }
        Text("−${l.quantity}", fontFamily = Mono, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = T.amberInk)
    }
}

@Composable
private fun CoFooter(icon: String, tint: Color, label: String, actor: String?, at: String?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFFF7F9FC))
            .border(1.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BbdIcon(icon, 18.dp, tint, sw = 2.2f)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink)
        Spacer(Modifier.weight(1f))
        val tail = listOfNotNull(actor?.takeIf { it.isNotBlank() }, at?.take(10)?.takeIf { it.isNotBlank() }).joinToString(" · ")
        if (tail.isNotBlank()) Text(tail, fontFamily = Mono, fontSize = 12.sp, color = T.ink3Read)
    }
}

@Composable
private fun CoActionButton(
    label: String,
    icon: String?,
    busy: Boolean,
    bg: Color,
    fg: Color,
    border: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg)
            .then(if (border) Modifier.border(1.5.dp, T.line, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(enabled = !busy, onClick = onClick).padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            Spinner(17.dp, color = fg)
            Spacer(Modifier.size(8.dp))
            Text("처리 중…", color = fg, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        } else {
            if (icon != null) { BbdIcon(icon, 18.dp, fg, sw = 2.2f); Spacer(Modifier.size(7.dp)) }
            Text(label, color = fg, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** 원화 표기(정수 절사, 천단위). 차감 원장 평가액은 표시용. */
private fun won(amount: Double): String {
    val n = amount.toLong()
    val digits = n.toString()
    val s = StringBuilder()
    for ((i, c) in digits.withIndex()) {
        if (i > 0 && (digits.length - i) % 3 == 0) s.append(',')
        s.append(c)
    }
    return "₩$s"
}
