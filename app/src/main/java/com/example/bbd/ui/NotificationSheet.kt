package com.example.bbd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.relDay
import com.example.bbd.data.remote.dto.NotificationDto
import com.example.bbd.ui.state.ErrorState
import com.example.bbd.ui.state.LoadingRows
import com.example.bbd.ui.theme.T

/**
 * 지점 알림함 바텀시트 — 헤더 벨에서 진입.
 * 미읽음(굵게 + 파란 점) / 읽음(흐리게) 구분. 탭 → 읽음 처리(낙관적) + soNumber 가 도착 대기면 큐로.
 * API 모드는 로딩/에러를 명시(빈 목록과 오류를 구분 — 실패가 '새 알림 없음'으로 위장되지 않게).
 */
@Composable
fun BoxScope.NotificationSheet(
    open: Boolean,
    items: List<NotificationDto>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onMarkRead: (Long?) -> Unit,
    onOpenSo: (String) -> Unit,
    onClose: () -> Unit,
) {
    SheetHost(open, onClose, title = "알림") {
        when {
            loading -> Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)) {
                LoadingRows(3)
            }
            error != null -> ErrorState(error, onRetry, Modifier.padding(horizontal = 16.dp))
            items.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 48.dp), contentAlignment = Alignment.Center) {
                Text("새 알림이 없습니다.", fontSize = 13.sp, color = T.ink3Read)
            }
            else -> Column(
                Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            ) {
                items.forEachIndexed { i, n ->
                    NotificationRow(n, divider = i < items.lastIndex) {
                        onMarkRead(n.id)
                        n.soNumber?.takeIf { it.isNotBlank() }?.let { onOpenSo(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: NotificationDto, divider: Boolean, onClick: () -> Unit) {
    val unread = !n.read
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .then(if (divider) Modifier.bottomBorder(T.lineSoft) else Modifier)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        // 미읽음 점(파랑) — 읽음이면 투명(정렬 유지).
        Box(Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(if (unread) T.blue else Color.Transparent))
        Column(Modifier.weight(1f)) {
            Text(
                n.message ?: "-",
                fontSize = 13.5.sp,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                color = if (unread) T.ink else T.ink3Read,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val so = n.soNumber
                if (!so.isNullOrBlank()) CodeText(so, size = 11.5.sp, color = T.ink3Read)
                val dt = kstDayTime(n.createdAt)
                if (dt != null) Text("${relDay(dt.first)} · ${dt.second}", fontSize = 11.sp, color = T.ink3Read)
            }
        }
        if (!n.soNumber.isNullOrBlank()) BbdIcon("chevR", 16.dp, T.ink3Read, sw = 2f)
    }
}

/**
 * createdAt(서버=UTC ISO 'Z' / 시드=오프셋 없는 로컬형)을 KST 로 환산해 (yyyy-MM-dd, HH:mm) 반환.
 * 실 API 의 UTC 시각이 -9h 로 표시되던 문제 해소. java.time 은 coreLibraryDesugaring(minSdk24)으로 가용.
 */
private fun kstDayTime(createdAt: String?): Pair<String, String>? {
    val raw = createdAt ?: return null
    val seoul = java.time.ZoneId.of("Asia/Seoul")
    val zoned = runCatching { java.time.Instant.parse(raw).atZone(seoul) }
        .recoverCatching { java.time.LocalDateTime.parse(raw).atZone(seoul) }
        .getOrNull() ?: return null
    val day = zoned.toLocalDate().toString()
    val time = "%02d:%02d".format(zoned.hour, zoned.minute)
    return day to time
}
