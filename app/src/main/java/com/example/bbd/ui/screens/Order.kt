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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.Pr
import com.example.bbd.data.PrStatus
import com.example.bbd.data.Seed
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.Nav
import com.example.bbd.ui.PartThumb
import com.example.bbd.ui.bbdCard
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
    val list = Seed.PR

    val open = list.filter { it.status.isOpen }
    val done = list.filter { it.status.isDone }
    val shown = if (tab == "open") open else done

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            // 작성은 웹 전용 → 제목을 조회 뉘앙스로.
            Header(title = "보충 발주 현황", back = true, onBack = { nav.pop() })
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp)) {
                // 지점 + 권한
                Row(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(T.blueSoft), contentAlignment = Alignment.Center) {
                        BbdIcon("pin", 19.dp, T.blue)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(Seed.USER.branch, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                        Spacer(Modifier.size(2.dp))
                        Text("본사로 보낸 보충 발주 현황", fontSize = 12.sp, color = T.ink3)
                    }
                    CodeText(Seed.USER.warehouse, size = 12.sp)
                }
                Spacer(Modifier.size(12.dp))

                // 읽기 전용 안내 — 작성/승인은 웹 ERP
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.blueSoft)
                        .border(1.dp, T.inBannerBorder, RoundedCornerShape(13.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BbdIcon("info", 18.dp, T.blue, sw = 2f)
                    Text(
                        "발주 요청·승인은 웹 ERP에서 처리합니다. 모바일에서는 현황만 확인해요.",
                        fontSize = 12.5.sp, color = T.blueInk, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp,
                    )
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
        }
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
        Header(title = "보충 발주 현황", back = true, onBack = { nav.pop() })
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
