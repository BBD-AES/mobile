package com.example.bbd.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import com.example.bbd.auth.AuthManager
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.collectAsState
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
import com.example.bbd.data.CurrentUser
import com.example.bbd.data.Part
import com.example.bbd.data.SalesOrder
import com.example.bbd.data.Seed
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.repo.SalesOrderRepository
import com.example.bbd.ui.screens.ArrivalQueueSheet
import com.example.bbd.ui.screens.HomeScreen
import com.example.bbd.ui.screens.InventoryScreen
import com.example.bbd.ui.screens.LoginScreen
import com.example.bbd.ui.screens.MyScreen
import com.example.bbd.ui.screens.OrderCreateScreen
import com.example.bbd.ui.screens.ScanOutScreen
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
    /** API 모드: 게이트웨이 /me 로 만든 실 사용자를 직접 설정 + 홈 진입(시드 resolve 우회). */
    val loginAs: (CurrentUser) -> Unit,
    val logout: () -> Unit,
    /** 세션 만료 감지(백그라운드 refresh 실패) — 브라우저 end-session 없이 로컬 정리 + 로그인 복귀. */
    val sessionExpired: () -> Unit,
    val scan: () -> Unit,
    val scanOut: () -> Unit,
    val orderNew: () -> Unit,
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

    // 세션 강제 종료(AUTH003=다른 기기 로그인 등) — 어느 화면이든 즉시 로그인으로 복귀. 사유는 로그인 화면이 안내한다.
    val sessionEnded by AuthManager.sessionEnded.collectAsState()
    LaunchedEffect(sessionEnded) {
        if (sessionEnded != null && stack.last().screen != "login") {
            AuthManager.clearLocal()
            me = Seed.USER
            queueOpen = false
            stack = listOf(Route("login"))
        }
    }

    fun openInventory(filter: String) {
        inventoryFilter = filter
        queueOpen = false
        stack = listOf(Route("inventory"))
    }

    val scope = rememberCoroutineScope()

    // 도착 대기 — API 모드는 목록 요약을 받은 뒤 상세를 조회해 라인(품목·수량)을 보강한다.
    // 요약 DTO 에 라인이 없어 그대로 그리면 큐 행이 "0품목"으로 보이기 때문이다.
    val salesRepo = remember { SalesOrderRepository() }
    var apiArrivals by remember { mutableStateOf<List<SalesOrder>>(emptyList()) }
    LaunchedEffect(me.warehouse, queueOpen) {
        if (com.example.bbd.BuildConfig.USE_API && me.warehouse.isNotBlank()) {
            (salesRepo.arrivals(me.warehouse) as? UiState.Success)?.let { res ->
                apiArrivals = res.data.map { d ->
                    val soNumber = d.soNumber ?: ""
                    val fallback = SalesOrder(
                        so = d.soNumber ?: "",
                        status = d.status ?: "IN_FULFILLMENT",
                        fromWh = "",
                        toCode = d.toWarehouseCode ?: "",
                        lines = emptyList(),
                    )
                    if (soNumber.isBlank()) fallback
                    else (salesRepo.detail(soNumber) as? UiState.Success)?.data ?: fallback
                }
            }
        }
    }
    val arrivalItems = if (com.example.bbd.BuildConfig.USE_API) apiArrivals else app.inbound

    // Keycloak end-session 복귀(com.bbd.mobile:/logout) — 결과 무관하게 Secure 토큰 전부 삭제 + 로그인 이동.
    val endSessionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        AuthManager.clearLocal()
        me = Seed.USER
        stack = listOf(Route("login"))
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
        loginAs = { user -> me = user; stack = listOf(Route("home")) },
        logout = {
            // 4) refresh_token back-channel 무효화 → 5) 브라우저 SSO 종료(id_token_hint) → 복귀 시 토큰 삭제(런처 콜백).
            // 세션 없음(시드/미로그인)이면 end-session Intent 가 null → 로컬 삭제+로그인 이동으로 폴백.
            scope.launch {
                AuthManager.revokeRefreshToken()
                val intent = AuthManager.endSessionIntent()
                if (intent != null) endSessionLauncher.launch(intent)
                else { AuthManager.clearLocal(); me = Seed.USER; stack = listOf(Route("login")) }
            }
        },
        // 세션 만료 — 토큰 이미 무효라 브라우저 end-session 불필요. 로컬 정리 + 로그인 화면(자동복원이 만료 안내).
        sessionExpired = { AuthManager.clearLocal(); me = Seed.USER; stack = listOf(Route("login")) },
        scan = { stack = stack + Route("scan-in") },
        scanOut = { stack = stack + Route("scan-out") },
        orderNew = { stack = stack + Route("order-new") },
        openQueue = { queueOpen = true },
        openInventory = ::openInventory,
        queueCount = arrivalItems.size,
    )

    CompositionLocalProvider(LocalAppData provides app, LocalMe provides me) {
        Box(Modifier.fillMaxSize().background(Color.White)) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                BackHandler(enabled = top.screen != "login" || stack.size > 1 || queueOpen) {
                    when {
                        // 1) 전역 큐 시트 열림 → 닫기
                        queueOpen -> queueOpen = false
                        // 2) 푸시된 화면(스택 깊이>1) → pop
                        stack.size > 1 -> stack = stack.dropLast(1)
                        // 3) 비-홈 탭 루트(깊이==1) → 홈 탭으로
                        top.screen in TAB_ROUTES && top.screen != "home" -> nav.tab("home")
                        // 4) 홈 탭 루트(또는 login) → 더블백 종료
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
                        "login" -> LoginScreen(onLogin = nav.login, onLoginAs = nav.loginAs)
                        "home" -> HomeScreen(nav, contentPad)
                        "inventory" -> InventoryScreen(nav, contentPad, inventoryFilter)
                        "my" -> MyScreen(nav, contentPad)
                        "worklog" -> WorklogScreen(nav, contentPad)
                        "scan-in" -> ScanScreen(nav, top.preset as? SalesOrder)
                        "scan-out" -> ScanOutScreen(nav, top.preset as? Part)
                        "order-new" -> OrderCreateScreen(nav, top.preset as? Part)
                        else -> HomeScreen(nav, contentPad)
                    }
                }

                // 탭바 셸 — 탭 루트에서만 1회 노출. 쓰기 액션은 홈 '작업' 카드(중앙 FAB 제거 §IA).
                if (isTabRoot) {
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        TabBar(active = top.screen, onTab = nav.tab)
                    }
                }

                // 전역 도착 대기 큐 시트 — 헤더 트럭/홈 히어로에서 진입. 항목 탭 → 입고 확인 폼 프리셋.
                ArrivalQueueSheet(
                    open = queueOpen && top.screen != "login",
                    items = arrivalItems,
                    apiMode = com.example.bbd.BuildConfig.USE_API,
                    onClose = { queueOpen = false },
                    onTap = { so -> queueOpen = false; stack = stack + Route("scan-in", so) },
                )

                ToastHost(backToast)
            }
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
