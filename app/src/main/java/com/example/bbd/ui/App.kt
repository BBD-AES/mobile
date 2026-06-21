package com.example.bbd.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.Seed
import com.example.bbd.ui.screens.ArrivalQueueSheet
import com.example.bbd.ui.screens.HomeScreen
import com.example.bbd.ui.screens.InventoryScreen
import com.example.bbd.ui.screens.LoginScreen
import com.example.bbd.ui.screens.MyScreen
import com.example.bbd.ui.screens.ScanScreen
import com.example.bbd.ui.screens.WorklogScreen
import com.example.bbd.ui.theme.T

/**
 * 네비게이션 API — 스택 + 하단 탭 + 전역 큐 시트.
 *  - push(scan-in, preset): 맥락 스캔(이 발주/이 부품) — FAB 로 못 하는 동작.
 *  - openQueue / openInventory(filter): 큐 시트 / 필터된 재고 딥링크.
 */
class Nav(
    val push: (String) -> Unit,
    val pushPreset: (String, Any?) -> Unit,
    val pop: () -> Unit,
    val tab: (String) -> Unit,
    val login: (String) -> Unit,
    val logout: () -> Unit,
    val scan: () -> Unit,
    val openQueue: () -> Unit,
    val openInventory: (String) -> Unit,
    val queueCount: Int,
)

private data class Route(val screen: String, val preset: Any? = null)

/** 탭바 높이(아이콘+라벨+패딩 근사). 탭 루트 본문 하단 패딩에 사용. */
private val TabBarHeight = 64.dp

@Composable
fun BbdApp() {
    val app = rememberAppData()
    var me by remember { mutableStateOf(Seed.USER) }
    var stack by remember { mutableStateOf(listOf(Route("login"))) }
    var queueOpen by remember { mutableStateOf(false) }
    var inventoryFilter by remember { mutableStateOf<String?>(null) }
    val top = stack.last()
    val isTabRoot = stack.size == 1 && top.screen in TAB_ROUTES

    val activity = LocalContext.current as? Activity
    var backToast by remember { mutableStateOf("") }
    var lastBackAt by remember { mutableStateOf(0L) }
    if (backToast.isNotBlank()) {
        LaunchedEffect(backToast, lastBackAt) {
            kotlinx.coroutines.delay(2000); backToast = ""
        }
    }

    fun openInventory(filter: String) {
        inventoryFilter = filter
        queueOpen = false
        stack = listOf(Route("inventory"))
    }

    val nav = Nav(
        push = { s -> stack = stack + Route(s) },
        pushPreset = { s, p -> stack = stack + Route(s, p) },
        pop = { if (stack.size > 1) stack = stack.dropLast(1) },
        tab = { id ->
            if (id == "inventory") inventoryFilter = null
            queueOpen = false
            stack = listOf(Route(id))
        },
        login = { emp -> me = Seed.resolveUser(emp); stack = listOf(Route("home")) },
        logout = { stack = listOf(Route("login")) },
        scan = { stack = stack + Route("scan-in") },
        openQueue = { queueOpen = true },
        openInventory = ::openInventory,
        queueCount = app.inbound.size,
    )

    CompositionLocalProvider(LocalAppData provides app, LocalMe provides me) {
        Box(Modifier.fillMaxSize().background(Color.White)) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                BackHandler(enabled = top.screen != "login" || stack.size > 1 || queueOpen) {
                    when {
                        queueOpen -> queueOpen = false
                        stack.size > 1 -> stack = stack.dropLast(1)
                        else -> {
                            val now = System.currentTimeMillis()
                            if (now - lastBackAt < 2000) activity?.finish()
                            else { lastBackAt = now; backToast = "한 번 더 누르면 종료됩니다" }
                        }
                    }
                }

                val routeKey = top.screen + "/" + stack.size
                ScreenContainer(routeKey) {
                    val contentPad = if (isTabRoot) PaddingValues(bottom = TabBarHeight) else PaddingValues()
                    when (top.screen) {
                        "login" -> LoginScreen(onLogin = nav.login)
                        "home" -> HomeScreen(nav, contentPad)
                        "inventory" -> InventoryScreen(nav, contentPad, inventoryFilter)
                        "my" -> MyScreen(nav, contentPad)
                        "worklog" -> WorklogScreen(nav, contentPad)
                        "scan-in" -> ScanScreen(nav, top.preset as? SalesOrder)
                        else -> HomeScreen(nav, contentPad)
                    }
                }

                // 탭바 셸 — 탭 루트에서만 1회 노출. 스캔은 중앙 FAB.
                if (isTabRoot) {
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        TabBar(active = top.screen, onTab = nav.tab)
                        ScanFab(Modifier.align(Alignment.TopCenter), onClick = nav.scan)
                    }
                }

                // 전역 도착 대기 큐 시트 — 헤더 트럭/홈 히어로에서 진입. 항목 탭 → 입고 확인 폼 프리셋.
                ArrivalQueueSheet(
                    open = queueOpen && top.screen != "login",
                    items = app.inbound,
                    onClose = { queueOpen = false },
                    onTap = { so -> queueOpen = false; stack = stack + Route("scan-in", so) },
                )

                ToastHost(backToast)
            }
        }
    }
}

/** 탭바 위로 돌출한 중앙 스캔 FAB → 입고 스캔(scan-in). */
@Composable
private fun ScanFab(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .graphicsLayer { translationY = -26.dp.toPx() }
            .size(58.dp)
            .shadow(10.dp, CircleShape, clip = false, ambientColor = T.blue, spotColor = T.blue)
            .clip(CircleShape)
            .background(T.blue)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BbdIcon("scan", 28.dp, Color.White, sw = 2.1f)
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
