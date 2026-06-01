package com.example.bbd.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

private val SheetShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

/** 바텀시트 — 스크림 + 아래에서 슬라이드 업. 화면 전체를 덮는 Box 안에 배치. */
@Composable
fun BoxScope.SheetHost(
    open: Boolean,
    onClose: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(open, Modifier.fillMaxSize(), enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(T.scrim).tapNoRipple(onClose))
    }
    AnimatedVisibility(
        open,
        Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(tween(300)) { it },
        exit = slideOutVertically(tween(240)) { it },
    ) {
        Column(
            Modifier.fillMaxWidth().shadow(20.dp, SheetShape, clip = false).clip(SheetShape).background(T.card)
        ) {
            // 그래버
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.width(38.dp).height(4.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFDDE2EC)))
            }
            if (title != null) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, modifier = Modifier.weight(1f), fontFamily = Pretendard)
                    IconBtn(onClose) { BbdIcon("x", 22.dp, T.ink2) }
                }
            }
            content()
        }
    }
}

/** 중앙 모달 — 스크림 + 스케일 인. */
@Composable
fun BoxScope.ModalHost(
    open: Boolean,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(open, Modifier.fillMaxSize(), enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(T.scrimModal).tapNoRipple(onClose))
    }
    AnimatedVisibility(
        open,
        Modifier.align(Alignment.Center).padding(24.dp),
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.94f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.94f),
    ) {
        Column(Modifier.fillMaxWidth().shadow(24.dp, RoundedCornerShape(20.dp), clip = false).clip(RoundedCornerShape(20.dp)).background(T.card)) {
            content()
        }
    }
}

enum class ToastKind { OK, ERR }

/** 하단 고정 토스트 (자동 사라짐은 호출부에서 LaunchedEffect 로). */
@Composable
fun BoxScope.ToastHost(msg: String, kind: ToastKind = ToastKind.OK) {
    AnimatedVisibility(
        msg.isNotBlank(),
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 92.dp),
        enter = fadeIn() + slideInVertically(tween(220)) { it / 2 },
        exit = fadeOut(),
    ) {
        Row(
            Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(13.dp), clip = false).clip(RoundedCornerShape(13.dp)).background(T.toast).padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(if (kind == ToastKind.ERR) T.red else T.green), contentAlignment = Alignment.Center) {
                BbdIcon(if (kind == ToastKind.ERR) "x" else "check", 13.dp, Color.White, sw = 2.6f)
            }
            Text(msg, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = Pretendard)
        }
    }
}
