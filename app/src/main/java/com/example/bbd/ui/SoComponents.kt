package com.example.bbd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxScope
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.SoStatusMeta
import com.example.bbd.data.relDay
import com.example.bbd.data.totals
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

/** SO 상태 배지(실제 sales enum → 안내북 표기). */
@Composable
fun SoBadge(status: String) {
    val (label, dot, bg, fg) = when (status) {
        "IN_FULFILLMENT" -> Quad("도착 대기", T.blue, T.blueSoft, T.blueInk)
        "RECEIVED" -> Quad("입고 완료", T.green, T.greenSoft, T.greenInk)
        "BACKORDERED" -> Quad("백오더", T.amber, T.amberSoft, T.amberInk)
        "REJECTED" -> Quad("거절됨", T.red, T.redSoft, T.redBlockTitle)
        "REQUESTED" -> Quad("요청됨", T.ink3, T.lineSoft, T.ink2)
        "SUBMITTED" -> Quad("제출됨", T.ink3, T.lineSoft, T.ink2)
        "CANCELED" -> Quad("취소됨", T.ink3, T.lineSoft, T.ink2)
        else -> Quad(SoStatusMeta.of(status)?.label ?: status, T.ink3, T.lineSoft, T.ink2)
    }
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Pretendard)
    }
}

private data class Quad(val label: String, val dot: Color, val bg: Color, val fg: Color)

/**
 * SO 행 — 입고 완료 / 도착 대기 공용. 탭 가능 시 button 시멘틱(clickable).
 * 시드 모드: 리치(부품명 head + 품목·수량). API 모드: 주문 단위(지점 경로 + 합계, 부품명 없음).
 */
@Composable
fun SoRow(
    so: SalesOrder,
    inbound: Boolean = false,
    apiMode: Boolean = false,
    toShort: String = "내 창고",
    onClick: (() -> Unit)? = null,
    divider: Boolean = false,
) {
    val tot = so.totals()
    // API 모드 요약 SO 는 lines 가 비어있을 수 있음 → first() 크래시 방지(없으면 SO번호로 대체).
    val head = (so.lines.firstOrNull()?.name ?: so.so) + (if (tot.items > 1) " 외 ${tot.items - 1}" else "")
    var mod = Modifier.fillMaxWidth()
    if (onClick != null) mod = mod.clickable(onClick = onClick)
    if (divider) mod = mod.bottomBorder(T.lineSoft)
    Row(
        mod.padding(horizontal = 4.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(if (inbound) T.blueSoft else T.greenSoft),
            contentAlignment = Alignment.Center,
        ) { BbdIcon(if (inbound) "truck" else "check", if (inbound) 19.dp else 17.dp, if (inbound) T.blue else T.green, sw = 2f) }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CodeText(so.so, size = 13.5.sp, color = T.ink, weight = FontWeight.SemiBold)
                if (inbound) {
                    // '이동 중' 중립 배지만 — ETA/지연 표기 금지.
                    Row(
                        Modifier.clip(RoundedCornerShape(999.dp)).background(T.blueSoft).padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(T.blue))
                        Text("이동 중", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = T.blueInk)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BbdIcon("check", 12.dp, T.green, sw = 2.4f)
                        Text("입고 완료", fontSize = 11.5.sp, color = T.greenInk, fontWeight = FontWeight.Bold)
                    }
                }
                if (apiMode) {
                    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(T.lineSoft).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text("SO", fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                    }
                }
            }
            Spacer(Modifier.size(3.dp))
            if (apiMode) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        "${so.fromWh} → $toShort",
                        fontSize = 12.5.sp, color = T.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF2F5FB)).padding(horizontal = 7.dp, vertical = 1.dp)) {
                        Text("${tot.items}품목 ${tot.qty}${if (tot.unit.isNotEmpty()) " ${tot.unit}" else ""}", fontFamily = Mono, fontSize = 11.5.sp, color = T.ink3Read)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${so.fromWh} · $head · ",
                        fontSize = 12.5.sp, color = T.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text("${tot.items}품목 ${tot.qty}${if (tot.unit.isNotEmpty()) " ${tot.unit}" else ""}", fontFamily = Mono, fontSize = 12.sp, color = T.ink3Read)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (inbound) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("입고 확인 ", fontSize = 12.5.sp, color = T.blue, fontWeight = FontWeight.Bold)
                    BbdIcon("chevR", 14.dp, T.blue)
                }
            } else {
                Text(relDay(so.date), fontSize = 12.sp, color = T.ink3Read)
                Spacer(Modifier.size(2.dp))
                Text(so.time, fontFamily = Mono, fontSize = 12.sp, color = T.ink2)
            }
        }
    }
}

/** 공용 인라인 노트(카드 톤 · info 아이콘 + 본문). */
@Composable
fun InlineNote(icon: String = "info", content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFAFBFE)).border(1.dp, T.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        BbdIcon(icon, 18.dp, T.ink3Read)
        Box(Modifier.weight(1f)) { content() }
    }
}

/** 재고이동요청 상세 바텀시트 — 상태 무관 SO 라인/경로/합계. */
@Composable
fun BoxScope.SoDetailSheet(so: SalesOrder?, meName: String, meWarehouseName: String, onClose: () -> Unit) {
    SheetHost(open = so != null, onClose = onClose, title = "재고이동요청 상세") {
        if (so != null) {
            val tot = so.totals()
            val confirmedAt = listOf(relDay(so.date).takeIf { so.date.isNotBlank() }, so.time.takeIf { so.time.isNotBlank() })
                .filterNotNull()
                .joinToString(" ")
            val actor = so.actorForStatus()
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CodeText(so.so, size = 15.sp, color = T.ink)
                    SoBadge(so.status)
                }
                Spacer(Modifier.size(14.dp))
                // 경로
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFFF7F9FC)).border(1.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BbdIcon("truck", 20.dp, T.ink2)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(so.fromWh, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.ink)
                        BbdIcon("arrowR", 14.dp, T.ink3, modifier = Modifier.padding(horizontal = 6.dp))
                        Text(meWarehouseName, fontSize = 13.5.sp, color = T.ink2)
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    buildString {
                        append(SoStatusMeta.of(so.status)?.label ?: so.status)
                        if (confirmedAt.isNotBlank()) append(" · $confirmedAt")
                        if (actor != null) append(" · ${actor.second}")
                    },
                    fontFamily = Mono, fontSize = 12.5.sp, color = T.ink3Read,
                )
                Spacer(Modifier.size(18.dp))

                Column(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 14.dp, vertical = 12.dp)) {
                    DetailInfoRow("상태", SoStatusMeta.of(so.status)?.label ?: so.status)
                    actor?.let { DetailInfoRow(it.first, it.second) }
                    if (confirmedAt.isNotBlank()) DetailInfoRow("처리시각", confirmedAt)
                    if (so.customerOrderNumber.isNotBlank()) DetailInfoRow("연계 수주", so.customerOrderNumber, mono = true)
                    if (so.note.isNotBlank()) DetailInfoRow("메모", so.note)
                }
                Spacer(Modifier.size(18.dp))

                Row(Modifier.fillMaxWidth()) {
                    Text("품목", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = T.ink2, modifier = Modifier.weight(1f))
                    Text("${tot.items}건", fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                }
                Spacer(Modifier.size(8.dp))
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
                Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                    Text("총 수량", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = T.ink2, modifier = Modifier.weight(1f))
                    Text("${tot.qty}${if (tot.unit.isNotEmpty()) " ${tot.unit}" else ""}", fontFamily = Mono, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                }
            }
        }
    }
}

private fun SalesOrder.actorForStatus(): Pair<String, String>? =
    when (status) {
        "RECEIVED" -> receivedBy.takeIf { it.isNotBlank() }?.let { "입고자" to it }
        "CANCELED", "REJECTED" -> canceledBy.takeIf { it.isNotBlank() }?.let { "처리자" to it }
        "IN_FULFILLMENT", "BACKORDERED" -> approvedBy.takeIf { it.isNotBlank() }?.let { "승인자" to it }
        "SUBMITTED", "REQUESTED" -> requestedBy.takeIf { it.isNotBlank() }?.let { "요청자" to it }
        else -> listOf(approvedBy, receivedBy, requestedBy, canceledBy).firstOrNull { it.isNotBlank() }?.let { "처리자" to it }
    }

@Composable
private fun DetailInfoRow(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = T.ink3Read, modifier = Modifier.width(72.dp))
        if (mono) CodeText(value, size = 12.5.sp, color = T.ink)
        else Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = T.ink, modifier = Modifier.weight(1f))
    }
}
