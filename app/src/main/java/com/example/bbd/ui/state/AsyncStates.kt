package com.example.bbd.ui.state

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

/**
 * 실 API(USE_API) 경로용 비동기 화면 상태 컴포저블. UX 감사 #8·#48.
 * 목업 경로에서는 쓰지 않는다.
 */

/** 로딩 스켈레톤 — 카드 모양 회색 박스 n개. */
@Composable
fun LoadingRows(count: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        repeat(count) {
            Box(
                Modifier.fillMaxWidth().height(78.dp)
                    .clip(RoundedCornerShape(16.dp)).background(T.line)
            )
        }
    }
}

/** 에러 + 재시도. */
@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(T.redSoft), contentAlignment = Alignment.Center) {
            BbdIcon("alert", 26.dp, T.red, sw = 2f)
        }
        Spacer(Modifier.height(14.dp))
        Text("불러오지 못했습니다", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, fontFamily = Pretendard)
        Spacer(Modifier.height(5.dp))
        Text(message, fontSize = 12.5.sp, color = T.ink3, textAlign = TextAlign.Center, fontFamily = Pretendard)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).border(1.5.dp, T.line, RoundedCornerShape(12.dp))
                    .clickable(onClick = onRetry).padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                BbdIcon("refresh", 16.dp, T.ink2, sw = 2f)
                Text("다시 시도", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink2, fontFamily = Pretendard)
            }
        }
    }
}

/** 빈 상태 — 아이콘 + 제목 + 보조문 + (선택)행동. */
@Composable
fun EmptyState(
    icon: String,
    title: String,
    sub: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(top = 46.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(T.lineSoft), contentAlignment = Alignment.Center) {
            BbdIcon(icon, 28.dp, T.ink3Read, sw = 1.7f)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, fontFamily = Pretendard)
        if (sub != null) {
            Spacer(Modifier.height(5.dp))
            Text(sub, fontSize = 13.sp, color = T.ink3Read, textAlign = TextAlign.Center, lineHeight = 20.sp, fontFamily = Pretendard)
        }
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

/** 단일 스켈레톤 행(카드 + 원형 + 2줄). */
@Composable
fun RowSkeleton() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(T.card).border(1.dp, T.line, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(T.line))
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth(0.52f).height(13.dp).clip(RoundedCornerShape(5.dp)).background(T.line))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.78f).height(11.dp).clip(RoundedCornerShape(5.dp)).background(T.lineSoft))
        }
    }
}

/**
 * API 모드 3-상태(로딩/에러/빈/정상) 게이트 — 디자인 시스템 3-상태 명세.
 * 실 화면은 UiState(Loading/Error/Success) 를 직접 분기하되 동일 컴포넌트(LoadingRows·ErrorState·EmptyState)로 렌더한다.
 */
@Composable
fun <T> StateGate(
    state: com.example.bbd.data.remote.UiState<List<T>>,
    rows: Int = 4,
    onRetry: () -> Unit,
    empty: @Composable () -> Unit,
    content: @Composable (List<T>) -> Unit,
) {
    when (state) {
        is com.example.bbd.data.remote.UiState.Loading -> LoadingRows(rows)
        is com.example.bbd.data.remote.UiState.Error -> ErrorState(state.message, onRetry)
        is com.example.bbd.data.remote.UiState.Success ->
            if (state.data.isEmpty()) empty() else content(state.data)
    }
}
