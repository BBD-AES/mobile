package com.example.bbd.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bbd.ui.theme.T

/**
 * 라인 아이콘 시스템 — 핸드오프 bbd-data.jsx 의 24×24 SVG 라인 아이콘을 그대로 포팅.
 * 모든 도형은 fill=none / stroke 방식 (둥근 cap·join).
 */

/** 24-좌표계 도형 프리미티브. */
private sealed interface Prim
private data class Pa(val d: String) : Prim                 // SVG path data
private data class Ci(val cx: Float, val cy: Float, val r: Float) : Prim
private data class Re(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float = 0f) : Prim

/** 24-좌표계 프리미티브들을 size(px) 캔버스에 스트로크로 그린다. */
private fun DrawScope.drawPrims(prims: List<Prim>, color: Color, sw: Float, sizePx: Float) {
    val scale = sizePx / 24f
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
        prims.forEach { p ->
            when (p) {
                is Pa -> drawPath(PathParser().parsePathString(p.d).toPath(), color, style = stroke)
                is Ci -> drawCircle(color, p.r, Offset(p.cx, p.cy), style = stroke)
                is Re -> drawRoundRect(
                    color, Offset(p.x, p.y), Size(p.w, p.h),
                    CornerRadius(p.rx, p.rx), style = stroke
                )
            }
        }
    }
}

private val ICONS: Map<String, List<Prim>> = mapOf(
    "home" to listOf(Pa("M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")),
    "scan" to listOf(
        Pa("M4 8V5.5A1.5 1.5 0 0 1 5.5 4H8"), Pa("M16 4h2.5A1.5 1.5 0 0 1 20 5.5V8"),
        Pa("M20 16v2.5a1.5 1.5 0 0 1-1.5 1.5H16"), Pa("M8 20H5.5A1.5 1.5 0 0 1 4 18.5V16"),
        Pa("M4 12h16"),
    ),
    "box" to listOf(Pa("M21 8 12 3 3 8v8l9 5 9-5z"), Pa("m3 8 9 5 9-5"), Pa("M12 13v8")),
    "user" to listOf(Ci(12f, 8f, 3.5f), Pa("M5 20c0-3.5 3.1-5.5 7-5.5s7 2 7 5.5")),
    "bell" to listOf(Pa("M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6"), Pa("M10.5 20a1.7 1.7 0 0 0 3 0")),
    "search" to listOf(Ci(11f, 11f, 7f), Pa("m20 20-3.2-3.2")),
    "chevR" to listOf(Pa("m9 6 6 6-6 6")),
    "chevD" to listOf(Pa("m6 9 6 6 6-6")),
    "arrowDn" to listOf(Pa("M12 5v14"), Pa("m6 13 6 6 6-6")),
    "arrowUp" to listOf(Pa("M12 19V5"), Pa("m6 11 6-6 6 6")),
    "arrowL" to listOf(Pa("M19 12H5"), Pa("m11 18-6-6 6-6")),
    "arrowR" to listOf(Pa("M5 12h14"), Pa("m13 6 6 6-6 6")),
    "lock" to listOf(Re(4.5f, 10.5f, 15f, 10f, 2f), Pa("M8 10.5V7a4 4 0 0 1 8 0v3.5")),
    "x" to listOf(Pa("M6 6l12 12"), Pa("M18 6 6 18")),
    "edit" to listOf(Pa("M12 20h9"), Pa("M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z")),
    "flash" to listOf(Pa("M13 2 4 14h7l-1 8 9-12h-7z")),
    "cal" to listOf(Re(4f, 5f, 16f, 16f, 2f), Pa("M4 9h16"), Pa("M8 3v4M16 3v4")),
    "pin" to listOf(Pa("M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11z"), Ci(12f, 10f, 2.5f)),
    "plus" to listOf(Pa("M12 5v14"), Pa("M5 12h14")),
    "minus" to listOf(Pa("M5 12h14")),
    "alert" to listOf(Pa("M12 3 2 20h20z"), Pa("M12 10v5"), Pa("M12 17.5h.01")),
    "check" to listOf(Pa("m4 12 5 5L20 6")),
    "list" to listOf(Pa("M8 6h12M8 12h12M8 18h12"), Pa("M4 6h.01M4 12h.01M4 18h.01")),
    "ban" to listOf(Ci(12f, 12f, 9f), Pa("m5.6 5.6 12.8 12.8")),
    "logout" to listOf(Pa("M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"), Pa("M9 12h11"), Pa("m15 8 4 4-4 4")),
    "info" to listOf(Ci(12f, 12f, 9f), Pa("M12 11v5"), Pa("M12 7.5h.01")),
    "cube" to listOf(Re(5f, 5f, 14f, 14f, 2f), Pa("M5 12h14M12 5v14")),
    "settings" to listOf(
        Ci(12f, 12f, 3f),
        Pa("M19.4 14a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V20a2 2 0 1 1-4 0v-.1A1.6 1.6 0 0 0 7 18.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1A1.6 1.6 0 0 0 5 12.6H5a2 2 0 1 1 0-4h.1A1.6 1.6 0 0 0 6.7 6L6.6 6a2 2 0 1 1 2.8-2.8l.1.1A1.6 1.6 0 0 0 12 4.6V4a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 2.7 1.1l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8z"),
    ),
    "refresh" to listOf(Pa("M21 12a9 9 0 1 1-2.6-6.4"), Pa("M21 3v5h-5")),
    "keypad" to listOf(
        Ci(6f, 6f, 1f), Ci(12f, 6f, 1f), Ci(18f, 6f, 1f),
        Ci(6f, 12f, 1f), Ci(12f, 12f, 1f), Ci(18f, 12f, 1f),
        Ci(6f, 18f, 1f), Ci(12f, 18f, 1f), Ci(18f, 18f, 1f),
    ),
    // ── 출고/현장수주 화면 신규 아이콘(핸드오프 §Design Tokens) ──
    "wrench" to listOf(Pa("M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z")),
    "tag" to listOf(Pa("M11.5 2.5H5a2 2 0 0 0-2 2v6.5a2 2 0 0 0 .6 1.4l8.5 8.5a2 2 0 0 0 2.8 0l6.5-6.5a2 2 0 0 0 0-2.8L11.5 2.5z"), Ci(7.5f, 7.5f, 1f)),
    "trash" to listOf(Pa("M3 6h18"), Pa("M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"), Pa("M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"), Pa("M10 11v6M14 11v6")),
    "cart" to listOf(Ci(9f, 20f, 1.3f), Ci(18f, 20f, 1.3f), Pa("M3 4h2l2.4 11.2a1.6 1.6 0 0 0 1.57 1.3h8.4a1.6 1.6 0 0 0 1.56-1.2L21 7.5H6")),
    "wifiOff" to listOf(Pa("M3 3 21 21"), Pa("M12 20h.01"), Pa("M9 16.5a4 4 0 0 1 4.9-.6"), Pa("M5 13a10 10 0 0 1 4-2.5"), Pa("M19.5 13a10 10 0 0 0-3-2.1")),
    "doc" to listOf(Pa("M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"), Pa("M14 3v5h5"), Pa("M9 13h6M9 17h6")),
    // ── 물류/상태 아이콘 ── (truck 누락 시 입고 행 글리프가 빈 원으로 렌더되던 결함 수정)
    "truck" to listOf(
        Pa("M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"),
        Pa("M15 18H9"),
        Pa("M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.62l-3.48-4.35A1 1 0 0 0 17.52 8H14"),
        Ci(7f, 18f, 2f), Ci(17f, 18f, 2f),
    ),
    "clock" to listOf(Ci(12f, 12f, 9f), Pa("M12 7v5l3 2")),
)

/** 라인 아이콘. */
@Composable
fun BbdIcon(name: String, size: Dp, color: Color, sw: Float = 1.8f, modifier: Modifier = Modifier) {
    val prims = ICONS[name] ?: emptyList()
    Canvas(modifier.size(size)) {
        drawPrims(prims, color, sw, this.size.width)
    }
}

// ───────────────────────── 부품 썸네일 글리프 ─────────────────────────
// 분류별 placeholder 도형 — 운영에서는 실제 부품 이미지로 교체.
private val GLYPHS: Map<String, List<Prim>> = mapOf(
    "BR-pad" to listOf(Re(8f, 9f, 8f, 6f, 1.2f), Pa("M8 12h8")),
    "BR-disc" to listOf(Ci(12f, 12f, 4.2f), Ci(12f, 12f, 1.3f)),
    "BR-oil" to listOf(Pa("M10 8h4v2l1 6H9l1-6z"), Pa("M10.5 8V6.5h3V8")),
    "BR-cal" to listOf(Re(8.5f, 9f, 7f, 6f, 1f), Pa("M15.5 11h1.5v2h-1.5")),
    "BR-hose" to listOf(Pa("M8 14c0-3 8-3 8 0"), Re(7f, 13.5f, 3f, 3f, 1f)),
    "EN-fil" to listOf(Re(8.5f, 8.5f, 7f, 7f, 1.4f), Pa("M10.5 8.5v-1.5h3v1.5")),
    "EN-air" to listOf(Re(8f, 9.5f, 8f, 5f, 1f), Pa("M9.5 9.5v5M12 9.5v5M14.5 9.5v5")),
    "IG-plug" to listOf(Pa("M11 7h2v4h-2z"), Pa("M11.5 11h1l-.5 6z")),
    "EL-bat" to listOf(Re(7.5f, 9f, 9f, 6f, 1f), Pa("M9.5 9V7.8M14.5 9V7.8"), Pa("M10.5 12h1M13 11.5v1M12.5 12h1")),
    "EX-wpr" to listOf(Pa("M7 15.5 16 8"), Pa("M7.2 15.2l1.6-1.2"), Pa("M14.08 7.98 16.44 6.13 17.80 7.86 15.43 9.71Z")),
    "EX-wsh" to listOf(Pa("M9.5 9h5l-.6 6.5h-3.8L9.5 9z"), Pa("M10.5 9V7.6h3V9"), Pa("M11 12h2")),
    "OIL-btl" to listOf(Pa("M10 8.5h4v1.2c1 .5 1.3 1.5 1.3 2.8l-.3 4H9l-.3-4c0-1.3.3-2.3 1.3-2.8z"), Pa("M10.7 8.5V7h2.6v1.5")),
    "DRV" to listOf(Ci(12f, 12f, 4f), Pa("M12 8v-1.5M12 16v1.5M8 12H6.5M16 12h1.5")),
    "box" to listOf(Re(8f, 8f, 8f, 8f, 1.2f), Pa("M8 12h8M12 8v8")),
)

/** 부품 분류별 placeholder 썸네일 (파선 박스 + 단순 글리프). */
@Composable
fun PartThumb(kind: String, size: Dp = 46.dp, active: Boolean = false) {
    val border = if (active) T.thumbBorderActive else T.thumbBorder
    val bg = if (active) T.thumbBgActive else T.field
    val glyphColor = if (active) Color(0xFF7C93DE) else Color(0xFFAAB3C5)
    val prims = GLYPHS[kind] ?: GLYPHS["box"]!!
    Box(
        Modifier
            .size(size)
            .background(bg, androidx.compose.foundation.shape.RoundedCornerShape(11.dp))
            .drawBehind {
                val s = 1.5.dp.toPx()
                drawRoundRect(
                    color = border,
                    topLeft = Offset(s / 2, s / 2),
                    size = Size(this.size.width - s, this.size.height - s),
                    cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx()),
                    style = Stroke(width = s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            drawPrims(prims, glyphColor, 1.5f, this.size.width)
        }
    }
}
