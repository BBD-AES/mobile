package com.example.bbd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.SalesOrder
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.InlineNote
import com.example.bbd.ui.LocalMe
import com.example.bbd.ui.SheetHost
import com.example.bbd.ui.SoRow
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.state.EmptyState
import com.example.bbd.ui.theme.T

/**
 * 도착 대기 큐 — 전역 바텀시트. 헤더 트럭 버튼·홈 히어로에서 진입.
 * SoRow(inbound) 재사용 · '이동 중' 중립 배지만(ETA/지연 표기 금지). 항목 탭 → 입고 확인 폼 프리셋.
 */
@Composable
fun BoxScope.ArrivalQueueSheet(
    open: Boolean,
    items: List<SalesOrder>,
    onClose: () -> Unit,
    onTap: (SalesOrder) -> Unit,
) {
    val me = LocalMe.current
    SheetHost(open = open, onClose = onClose, title = "도착 대기") {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(999.dp)).background(T.blueSoft).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                BbdIcon("truck", 13.dp, T.blue, sw = 2f)
                Text("${items.size}건", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = T.blueInk)
            }
            Text("내 창고로 이동 중인 입고", fontSize = 12.5.sp, color = T.ink3Read)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
            if (items.isEmpty()) {
                EmptyState("truck", "도착 대기 입고가 없습니다", "본사·타지점에서 출고되어 이동 중인 입고가 여기에 표시됩니다.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { so ->
                        Box(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 14.dp)) {
                            SoRow(so, inbound = true, toShort = me.warehouseName.removeSuffix(" 창고"), onClick = { onTap(so) })
                        }
                    }
                }
            }
            Spacer(Modifier.size(14.dp))
            InlineNote {
                Text(
                    "내 창고로 이동 중인 입고 목록입니다. 도착·배송 예정 시각은 현재 제공되지 않으며, 푸시 알림은 지점 알림함 연동 후 제공됩니다.",
                    fontSize = 12.5.sp, color = T.ink2, lineHeight = 19.sp,
                )
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}
