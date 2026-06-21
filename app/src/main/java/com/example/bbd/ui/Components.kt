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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.bbd.data.Movement
import com.example.bbd.data.MoveType
import com.example.bbd.data.Seed
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

/** 재고 상태 칩 (부족/없음/정상). */
@Composable
fun StatusPill(status: StockStatus) {
    when (status) {
        StockStatus.SHORT -> DotPill(T.blueSoft, T.blueInk, T.blue, status.label)
        StockStatus.NONE -> DotPill(T.redSoft, T.red, T.red, status.label)
        StockStatus.OK -> DotPill(T.lineSoft, T.ink2, T.green, status.label)
    }
}

/** 임의 색 요약 칩 ("부족 2건" 등). */
@Composable
fun SummaryPill(bg: Color, fg: Color, dot: Color, text: String) = DotPill(bg, fg, dot, text)

// ───────────────────────── 헤더 ─────────────────────────

enum class HeaderRight { NONE, BELL, MANUAL }

@Composable
fun IconBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun BellBtn(count: Int = Seed.NOTIF, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        BbdIcon("bell", 22.dp, T.ink)
        if (count > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 1.dp)
                    .size(17.dp).clip(CircleShape).background(Color.White).padding(2.dp)
                    .clip(CircleShape).background(T.blue),
                contentAlignment = Alignment.Center,
            ) {
                Text("$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Pretendard)
            }
        }
    }
}

@Composable
fun Header(
    title: String,
    back: Boolean = false,
    chip: String? = null,
    right: HeaderRight = HeaderRight.NONE,
    notif: Int = Seed.NOTIF,
    onBack: () -> Unit = {},
    onRight: () -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().background(T.card).bottomBorder()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 12.dp).heightIn(min = 44.dp),
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
            when (right) {
                HeaderRight.BELL -> BellBtn(notif, onRight)
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
    Row(Modifier.fillMaxWidth().background(T.card).topBorder().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp)) {
        TABS.forEach { t ->
            val on = active == t.id
            val c = if (on) T.blue else T.ink3
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { onTab(t.id) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BbdIcon(t.icon, 24.dp, c, sw = if (on) 2.1f else 1.8f)
                Text(t.label, fontSize = 11.5.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, color = c, fontFamily = Pretendard)
            }
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

// ───────────────────────── 이동(작업) 이력 행 ─────────────────────────

@Composable
fun MovementRow(
    m: Movement,
    showDay: Boolean = true,
    highlight: Boolean = false,
    divider: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val isIn = m.delta > 0
    val (circleBg, icon, iconColor) = when (m.type) {
        MoveType.OUT -> Triple(T.outCircle, "arrowUp", Color.White)
        MoveType.IN -> Triple(T.blueSoft, "arrowDn", T.blue)
    }
    var mod = Modifier.fillMaxWidth()
    if (onClick != null) mod = mod.clickable(onClick = onClick)
    if (highlight) {
        mod = mod.clip(RoundedCornerShape(13.dp)).background(T.hotBg).border(1.5.dp, T.hotBorder, RoundedCornerShape(13.dp))
    } else if (divider) {
        mod = mod.bottomBorder(T.lineSoft)
    }
    Row(
        mod.padding(horizontal = if (highlight) 14.dp else 4.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(circleBg), contentAlignment = Alignment.Center) {
            BbdIcon(icon, 18.dp, iconColor, sw = 2.1f)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(m.label, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, fontFamily = Pretendard)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        (if (isIn) "+" else "") + m.delta,
                        fontFamily = Mono, fontSize = 14.5.sp, fontWeight = FontWeight.Bold,
                        color = if (isIn) T.blue else T.ink,
                    )
                    Text(" " + m.unit, fontSize = 11.5.sp, color = T.ink3, fontWeight = FontWeight.SemiBold, fontFamily = Pretendard)
                }
            }
            Spacer(Modifier.size(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.name + " · ", fontSize = 12.5.sp, color = T.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = Pretendard)
                CodeText(m.sku, size = 12.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (showDay) {
                Text(m.day, fontSize = 11.5.sp, color = T.ink3, fontFamily = Pretendard)
                Spacer(Modifier.size(2.dp))
            }
            CodeText(m.time, size = 12.sp, color = T.ink2)
        }
    }
}

@Composable
fun RowScope.HSpace(w: Dp) = Spacer(Modifier.width(w))
