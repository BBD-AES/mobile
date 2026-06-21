package com.example.bbd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.StockStatus
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

// ───────────────────────── Modifier 헬퍼 ─────────────────────────

/** 흰 카드 (보더 + 부드러운 그림자 + 라운드). */
fun Modifier.bbdCard(radius: Dp = 16.dp, shadowDp: Dp = 3.dp): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    return this
        .shadow(shadowDp, shape, clip = false, ambientColor = T.ink, spotColor = T.ink)
        .clip(shape)
        .background(T.card)
        .border(1.dp, T.line, shape)
}

fun Modifier.bottomBorder(color: Color = T.line, height: Dp = 1.dp): Modifier = drawBehind {
    val h = height.toPx()
    drawLine(color, Offset(0f, size.height - h / 2), Offset(size.width, size.height - h / 2), strokeWidth = h)
}

fun Modifier.topBorder(color: Color = T.line, height: Dp = 1.dp): Modifier = drawBehind {
    val h = height.toPx()
    drawLine(color, Offset(0f, h / 2), Offset(size.width, h / 2), strokeWidth = h)
}

// ───────────────────────── 텍스트 ─────────────────────────

/** 모노 코드 텍스트 (SKU·사번·타임스탬프). */
@Composable
fun CodeText(
    text: String,
    size: TextUnit = 13.sp,
    color: Color = T.ink3,
    weight: FontWeight = FontWeight.Medium,
) {
    Text(
        text, fontFamily = Mono, fontSize = size, color = color, fontWeight = weight,
        letterSpacing = (-0.2).sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
    )
}

// ───────────────────────── 상태/요약 칩 ─────────────────────────

@Composable
private fun DotPill(bg: Color, fg: Color, dot: Color, text: String, fontSize: TextUnit = 12.sp) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Text(text, color = fg, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1, fontFamily = Pretendard)
    }
}

/** 재고 상태 칩 — 신호색 3단: 정상=초록 / 부족=amber / 없음=빨강. (파랑은 주행동 전용) */
@Composable
fun StatusPill(status: StockStatus) {
    when (status) {
        StockStatus.SHORT -> DotPill(T.amberSoft, T.amberInk, T.amber, status.label)
        StockStatus.NONE -> DotPill(T.redSoft, T.red, T.red, status.label)
        StockStatus.OK -> DotPill(T.lineSoft, T.ink2, T.green, status.label)
    }
}

/** 임의 색 요약 칩 ("부족 2건" 등). */
@Composable
fun SummaryPill(bg: Color, fg: Color, dot: Color, text: String) = DotPill(bg, fg, dot, text)

// ───────────────────────── 헤더 ─────────────────────────

enum class HeaderRight { NONE, QUEUE, MANUAL }

@Composable
fun IconBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 도착 대기 큐 진입점 — 벨 아님(트럭 = 이동 중) + 중립 파랑 카운트 배지. amber 금지. */
@Composable
fun QueueBtn(count: Int, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        BbdIcon("truck", 23.dp, T.ink, sw = 1.9f)
        if (count > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp)
                    .size(17.dp).clip(CircleShape).background(Color.White).padding(2.dp)
                    .clip(CircleShape).background(T.blue),
                contentAlignment = Alignment.Center,
            ) {
                Text("$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Pretendard)
            }
        }
    }
}

/** "방금 / N분 전 / N시간 전" 상대 갱신 라벨. */
fun agoLabel(ms: Long): String {
    if (ms <= 0) return ""
    val s = ((System.currentTimeMillis() - ms) / 1000).coerceAtLeast(0)
    if (s < 45) return "방금"
    val m = (s / 60.0).toInt().coerceAtLeast(1)
    if (m < 60) return "${m}분 전"
    return "${(m / 60.0).toInt().coerceAtLeast(1)}시간 전"
}

@Composable
fun Header(
    title: String,
    back: Boolean = false,
    chip: String? = null,
    right: HeaderRight = HeaderRight.NONE,
    queueCount: Int = 0,
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
    lastRefresh: Long = 0L,
    onBack: () -> Unit = {},
    onRight: () -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().background(T.card).bottomBorder().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (back) {
                IconBtn(onBack) { BbdIcon("arrowL", 23.dp, T.ink, sw = 2f) }
            }
            Row(
                Modifier.weight(1f).padding(start = if (back) 2.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp, fontFamily = Pretendard)
                if (chip != null) {
                    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(T.ink).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text(chip, fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                    }
                }
            }
            if (onRefresh != null) {
                IconBtn(onRefresh) { BbdIcon("refresh", 20.dp, if (refreshing) T.blue else T.ink2) }
            }
            when (right) {
                HeaderRight.QUEUE -> QueueBtn(queueCount, onRight)
                HeaderRight.MANUAL -> Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onRight).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BbdIcon("edit", 17.dp, T.blue)
                    Text("수동 입력", color = T.blue, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = Pretendard)
                }
                HeaderRight.NONE -> {}
            }
        }
        if (onRefresh != null && lastRefresh > 0L) {
            Text(
                "마지막 갱신 ${agoLabel(lastRefresh)}",
                fontSize = 12.sp, color = T.ink3Read, fontFamily = Pretendard,
                modifier = Modifier.padding(start = if (back) 50.dp else 10.dp, top = 0.dp),
            )
        }
    }
}

// ───────────────────────── 탭바 ─────────────────────────

private data class Tab(val id: String, val label: String, val icon: String)
// 4개 루트 탭. 스캔은 탭이 아니라 중앙 FAB(App 셸). 작업이력 탭 신설.
private val TABS = listOf(
    Tab("home", "홈", "home"),
    Tab("inventory", "재고", "box"),
    Tab("worklog", "작업이력", "list"),
    Tab("my", "마이", "user"),
)

/** 루트 탭 ID(셸이 TabBar/FAB 노출 여부 판정에 사용). */
val TAB_ROUTES: Set<String> = TABS.map { it.id }.toSet()

@Composable
fun TabBar(active: String, onTab: (String) -> Unit) {
    // 목적지 4개 + 중앙 FAB 자리(스페이서). 스캔은 탭이 아니라 동작(셸 FAB).
    Row(Modifier.fillMaxWidth().background(T.card).topBorder().padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 4.dp)) {
        TABS.take(2).forEach { TabItem(it, active == it.id, Modifier.weight(1f), onTab) }
        Spacer(Modifier.width(64.dp))
        TABS.drop(2).forEach { TabItem(it, active == it.id, Modifier.weight(1f), onTab) }
    }
}

@Composable
private fun TabItem(t: Tab, on: Boolean, modifier: Modifier, onTab: (String) -> Unit) {
    val c = if (on) T.blue else T.tabInactive
    Box(modifier.clip(RoundedCornerShape(10.dp)).clickable { onTab(t.id) }, contentAlignment = Alignment.TopCenter) {
        if (on) Box(Modifier.width(22.dp).height(3.dp).clip(RoundedCornerShape(3.dp)).background(T.blue))
        Column(
            Modifier.padding(top = 6.dp, bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BbdIcon(t.icon, 23.dp, c, sw = if (on) 2.1f else 1.8f)
            Text(t.label, fontSize = 12.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, color = c, fontFamily = Pretendard)
        }
    }
}

// ───────────────────────── 화면 스캐폴드 ─────────────────────────

/** 헤더 + 스크롤 본문. 단순 스크롤 화면용. 탭바는 App 셸이 노출(여기서 그리지 않음).
 *  탭 루트는 contentPad(셸 탭바 높이)를 받아 본문이 탭바에 가리지 않게 한다. */
@Composable
fun Screen(
    bg: Color = T.bg,
    contentPad: PaddingValues = PaddingValues(),
    header: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(bg).padding(contentPad)) {
        header()
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            content = content,
        )
    }
}

/** 하단 고정 액션바 — 흰 배경 + 상단 보더(루트가 systemBars safe-area 처리). */
@Composable
fun StickyBar(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(T.card).topBorder()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        content = content,
    )
}

@Composable
fun RowScope.HSpace(w: Dp) = Spacer(Modifier.width(w))

/** 흰 스피너(버튼 위) — 무한 회전 원호. reduced-motion 정적 표시는 플랫폼 처리. */
@Composable
fun Spinner(size: Dp, color: Color = Color.White, track: Color = Color.White.copy(alpha = 0.4f)) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "angle",
    )
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val sw = 2.5.dp.toPx()
        val d = this.size.minDimension - sw
        val tl = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2)
        val arcSize = androidx.compose.ui.geometry.Size(d, d)
        drawArc(track, 0f, 360f, false, tl, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawArc(color, angle, 90f, false, tl, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}
