package com.example.bbd.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.Part
import com.example.bbd.ui.screens.HomeScreen
import com.example.bbd.ui.screens.InventoryScreen
import com.example.bbd.ui.screens.LoginScreen
import com.example.bbd.ui.screens.MyScreen
import com.example.bbd.ui.screens.OrderScreen
import com.example.bbd.ui.screens.ScanScreen
import com.example.bbd.ui.screens.WorklogScreen
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

/** 네비게이션 API — 프로토타입 nav 모델 대응. 스택 + 하단 탭. */
class Nav(
    val push: (String) -> Unit,
    val pushPart: (String, Part) -> Unit,
    val pop: () -> Unit,
    val tab: (String) -> Unit,
    val login: () -> Unit,
    val logout: () -> Unit,
)

private data class Route(val screen: String, val param: Part? = null)

@Composable
fun BbdApp() {
    var stack by remember { mutableStateOf(listOf(Route("login"))) }
    var scanSheet by remember { mutableStateOf(false) }
    val top = stack.last()

    val nav = Nav(
        push = { s -> stack = stack + Route(s) },
        pushPart = { s, p -> stack = stack + Route(s, p) },
        pop = { if (stack.size > 1) stack = stack.dropLast(1) },
        tab = { id -> if (id == "scan") scanSheet = true else stack = listOf(Route(id)) },
        login = { stack = listOf(Route("home")) },
        logout = { stack = listOf(Route("login")) },
    )

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            val routeKey = top.screen + "/" + stack.size
            ScreenContainer(routeKey) {
                when (top.screen) {
                    "login" -> LoginScreen(onLogin = nav.login)
                    "home" -> HomeScreen(nav)
                    "inventory" -> InventoryScreen(nav)
                    "my" -> MyScreen(nav)
                    "worklog" -> WorklogScreen(nav)
                    "order" -> OrderScreen(nav)
                    "scan-in" -> ScanScreen(nav, mode = "in", preset = top.param)
                    "scan-out" -> ScanScreen(nav, mode = "out", preset = top.param)
                    else -> HomeScreen(nav)
                }
            }
            ScanSheet(open = scanSheet, onClose = { scanSheet = false }, nav = nav)
        }
    }
}

/** 화면 진입 시 8px 페이드 업 (bbd-fade, 0.22s). 라우트 변경마다 재생. */
@Composable
private fun ScreenContainer(routeKey: String, content: @Composable () -> Unit) {
    key(routeKey) {
        val anim = remember { Animatable(0f) }
        LaunchedEffect(Unit) { anim.animateTo(1f, tween(220)) }
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = (1f - anim.value) * 8.dp.toPx() }) {
            content()
        }
    }
}

/** 스캔 탭 → 입고/출고 선택 액션 시트. 직접 네비게이션하지 않고 시트만 연다. */
@Composable
fun BoxScope.ScanSheet(open: Boolean, onClose: () -> Unit, nav: Nav) {
    SheetHost(open = open, onClose = onClose, title = "스캔") {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            ScanSheetOption("box", "입고 스캔", "IN · 도착 부품 확정", T.blue) { onClose(); nav.push("scan-in") }
            ScanSheetOption("logout", "출고 스캔", "OUT · 작업 사용 차감", T.blueDeep) { onClose(); nav.push("scan-out") }
        }
    }
}

@Composable
private fun ScanSheetOption(icon: String, title: String, sub: String, bg: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .border(1.dp, T.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
            BbdIcon(icon, 23.dp, Color.White, sw = 1.9f)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, fontFamily = Pretendard)
            Text(sub, fontSize = 12.5.sp, color = T.ink3, fontFamily = Mono)
        }
        BbdIcon("chevR", 19.dp, T.ink3)
    }
}
