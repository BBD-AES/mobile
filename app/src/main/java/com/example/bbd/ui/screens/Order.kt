package com.example.bbd.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.BuildConfig
import com.example.bbd.data.Part
import com.example.bbd.data.Seed
import com.example.bbd.data.remote.CreateOrderResult
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.remote.dto.CustomerOrderLineRequest
import com.example.bbd.data.remote.dto.ItemDto
import com.example.bbd.data.repo.CustomerOrderRepository
import com.example.bbd.data.repo.ItemRepository
import kotlinx.coroutines.delay
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.CodeText
import com.example.bbd.ui.Header
import com.example.bbd.ui.IconBtn
import com.example.bbd.ui.LocalAppData
import com.example.bbd.ui.ToastHost
import com.example.bbd.ui.LocalMe
import com.example.bbd.ui.ModalHost
import com.example.bbd.ui.Nav
import com.example.bbd.ui.PartThumb
import com.example.bbd.ui.SheetHost
import com.example.bbd.ui.Spinner
import com.example.bbd.ui.StickyBar
import com.example.bbd.ui.bbdCard
import com.example.bbd.ui.nextCoNo
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

private sealed interface OrderUi {
    data class Ok(val coNo: String, val items: Int, val qty: Int, val customer: String) : OrderUi
    data object Offline : OrderUi
    data object Unauthorized : OrderUi
    data class Err(val message: String) : OrderUi
}

/**
 * 현장 수주 등록 — 현장 고객 작업/판매 건을 CustomerOrder(OPEN)로 기록 생성(POST /customer-orders).
 * 출고(물리 차감)와 별개 진입점. 본사 전송이 아니므로 정비사·점장 공통, 역할 게이팅 없음.
 * 고객명은 선택 입력(공백이면 repo 가 기본값으로 치환 — 백엔드 @NotBlank).
 */
@Composable
fun OrderCreateScreen(nav: Nav, preset: Part? = null) {
    val me = LocalMe.current
    val app = LocalAppData.current
    val repo = remember { CustomerOrderRepository() }
    val itemRepo = remember { ItemRepository() }
    val scope = rememberCoroutineScope()

    var customer by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf<List<Pair<String, Int>>>(if (preset != null) listOf(preset.sku to 1) else emptyList()) }
    var itemNames by remember { mutableStateOf<Map<String, String>>(preset?.let { mapOf(it.sku to it.name) } ?: emptyMap()) }
    var pickOpen by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<OrderUi?>(null) }
    var scanToast by remember { mutableStateOf("") }
    if (scanToast.isNotBlank()) LaunchedEffect(scanToast) { delay(2200); scanToast = "" }
    // 멱등 키 — 폼 진입당 1개(재시도/중복 제출 시 동일 키 재전송). 핸드오프 §4.3.
    val idemKey = remember { java.util.UUID.randomUUID().toString() }

    val totalQty = lines.sumOf { it.second }
    val canSubmit = lines.isNotEmpty() && !submitting

    fun addLine(sku: String, name: String? = null) {
        val cleanSku = sku.trim()
        val cleanName = name?.trim().orEmpty()
        if (cleanName.isNotBlank()) itemNames = itemNames + (cleanSku to cleanName)
        lines = if (lines.any { it.first == cleanSku }) lines.map { if (it.first == cleanSku) it.first to (it.second + 1) else it }
        else lines + (cleanSku to 1)
    }
    fun setQty(sku: String, d: Int) { lines = lines.map { if (it.first == sku) it.first to (it.second + d).coerceAtLeast(1) else it } }
    fun removeLine(sku: String) { lines = lines.filterNot { it.first == sku } }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { res ->
        val sku = res.contents?.trim()?.uppercase()
        if (sku != null) {
            if (BuildConfig.USE_API) {
                scope.launch {
                    val item = (itemRepo.resolve(sku) as? UiState.Success)?.data
                    if (item?.sku != null) addLine(item.sku, item.name)
                    else scanToast = "미등록 부품: $sku"
                }
            } else if (Seed.partBySku(sku) != null) addLine(sku) else scanToast = "미등록 부품: $sku"
        }
    }

    fun submit() {
        if (!canSubmit) return
        submitting = true
        val snapItems = lines.size
        val snapQty = totalQty
        val snapCustomer = customer.trim()
        scope.launch {
            val r: OrderUi = if (BuildConfig.USE_API) {
                when (val out = repo.create(me.warehouse, snapCustomer, null, memo.trim(), lines.map { CustomerOrderLineRequest(it.first, it.second) }, idemKey)) {
                    is CreateOrderResult.Ok -> { app.addOrder(out.coNumber, snapCustomer, snapItems, snapQty); OrderUi.Ok(out.coNumber, snapItems, snapQty, snapCustomer) }
                    CreateOrderResult.Unauthorized -> OrderUi.Unauthorized
                    CreateOrderResult.Offline -> OrderUi.Offline
                    is CreateOrderResult.Error -> OrderUi.Err(out.message)
                }
            } else {
                val coNo = nextCoNo()
                app.addOrder(coNo, snapCustomer, snapItems, snapQty)
                OrderUi.Ok(coNo, snapItems, snapQty, snapCustomer)
            }
            result = r; submitting = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "현장 수주 등록", back = true, onBack = { nav.pop() })
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)) {
                // 고객·작업 (선택)
                Row { Text("고객 · 작업 ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink); Text("(선택)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = T.ink3) }
                Spacer(Modifier.size(9.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BbdIcon("user", 19.dp, T.ink3)
                    BasicTextField(
                        value = customer, onValueChange = { customer = it }, singleLine = true,
                        textStyle = TextStyle(fontSize = 15.5.sp, color = T.ink, fontFamily = Pretendard),
                        cursorBrush = SolidColor(T.blue), modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (customer.isEmpty()) Text("고객명 또는 작업번호", color = T.ink3, fontSize = 15.5.sp, fontFamily = Pretendard); inner() },
                    )
                }
                Spacer(Modifier.size(18.dp))

                // 부품 (필수)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f)) { Text("부품 ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink); Text("*", fontSize = 14.sp, color = T.blue) }
                    if (lines.isNotEmpty()) Text("${lines.size}품목 · ${totalQty}개", fontFamily = Mono, fontSize = 12.sp, color = T.ink3Read)
                }
                Spacer(Modifier.size(9.dp))

                if (lines.isEmpty()) {
                    Column(Modifier.fillMaxWidth().bbdCard().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(T.miniTile), contentAlignment = Alignment.Center) { BbdIcon("cart", 22.dp, T.ink3, sw = 1.9f) }
                        Spacer(Modifier.size(10.dp))
                        Text("추가된 부품이 없습니다", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink2)
                        Spacer(Modifier.size(4.dp))
                        Text("이 작업에 쓰거나 판매할 부품을 스캔 또는 검색으로 추가하세요.", fontSize = 12.sp, color = T.ink3Read, textAlign = TextAlign.Center, lineHeight = 18.sp)
                    }
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        lines.forEach { (sku, q) ->
                            val p = Seed.partBySku(sku)
                            val name = itemNames[sku] ?: p?.name ?: sku
                            Row(Modifier.fillMaxWidth().bbdCard().padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PartThumb(p?.thumb ?: "box", size = 44.dp, active = true)
                                Column(Modifier.weight(1f)) {
                                    Text(name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    CodeText(sku, size = 12.sp)
                                }
                                Row(Modifier.clip(RoundedCornerShape(10.dp)).border(1.5.dp, T.line, RoundedCornerShape(10.dp)), verticalAlignment = Alignment.CenterVertically) {
                                    MiniStep("minus", q > 1) { setQty(sku, -1) }
                                    Text("$q", fontFamily = Mono, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp))
                                    MiniStep("plus", true) { setQty(sku, 1) }
                                }
                                IconBtn({ removeLine(sku) }) { BbdIcon("trash", 18.dp, T.ink3Read, sw = 1.9f) }
                            }
                        }
                    }
                }
                Spacer(Modifier.size(if (lines.isEmpty()) 12.dp else 12.dp))

                // 라인 추가 — 스캔 / 검색
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddBtn("scan", "스캔", Modifier.weight(1f)) {
                        scanLauncher.launch(ScanOptions().apply { setPrompt("추가할 부품의 바코드를 맞춰 주세요"); setBeepEnabled(true); setOrientationLocked(true) })
                    }
                    AddBtn("search", "검색으로 추가", Modifier.weight(1f)) { pickOpen = true }
                }
                Spacer(Modifier.size(18.dp))

                // 메모 (선택)
                Row { Text("메모 ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink); Text("(선택)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = T.ink3) }
                Spacer(Modifier.size(9.dp))
                Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 13.dp)) {
                    BasicTextField(
                        value = memo, onValueChange = { memo = it }, minLines = 2,
                        textStyle = TextStyle(fontSize = 14.5.sp, color = T.ink, fontFamily = Pretendard, lineHeight = 21.sp),
                        cursorBrush = SolidColor(T.blue), modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner -> if (memo.isEmpty()) Text("작업 메모 (수주 기록에 저장)", color = T.ink3, fontSize = 14.5.sp, fontFamily = Pretendard); inner() },
                    )
                }
            }

            StickyBar {
                if (lines.isEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        BbdIcon("alert", 15.dp, T.amber, sw = 2f)
                        Spacer(Modifier.size(6.dp))
                        Text("부품을 1개 이상 추가하세요", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = T.amberInk)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (canSubmit) T.blue else Color(0xFFDFE4EE)).clickable(enabled = canSubmit) { submit() }.padding(vertical = 17.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (submitting) Spinner(17.dp) else BbdIcon("doc", 19.dp, if (canSubmit) Color.White else T.ink3, sw = 2f)
                        Text(if (submitting) "등록 중…" else "수주 등록", color = if (canSubmit) Color.White else T.ink3, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 부품 검색 추가 시트
        SheetHost(pickOpen, { pickOpen = false }, title = "부품 추가", avoidIme = true, maxHeightFraction = 0.82f) {
            PartPickList(apiMode = BuildConfig.USE_API, selected = lines.map { it.first }) { sku, name -> addLine(sku, name); pickOpen = false }
        }

        // 결과 모달
        ModalHost(result != null, { if (result is OrderUi.Ok) nav.tab("home") else result = null }) {
            when (val r = result) {
                is OrderUi.Ok -> Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(T.greenSoft), contentAlignment = Alignment.Center) { BbdIcon("check", 28.dp, T.green, sw = 2.6f) }
                    Spacer(Modifier.size(14.dp))
                    Text("수주 등록 완료", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = T.ink)
                    Spacer(Modifier.size(6.dp))
                    Text("${r.items}품목 · ${r.qty}개${if (r.customer.isNotBlank()) " · ${r.customer}" else ""}", fontSize = 13.5.sp, color = T.ink2, textAlign = TextAlign.Center)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.clip(RoundedCornerShape(13.dp)).background(T.field).border(1.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 22.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("수주 번호", fontSize = 11.sp, color = T.ink3Read); Spacer(Modifier.size(3.dp)); CodeText(r.coNo, size = 15.sp, color = T.ink)
                    }
                    Spacer(Modifier.size(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostBtn("홈으로", Modifier.weight(1f)) { nav.tab("home") }
                        SolidBtn("새 수주 등록", Modifier.weight(1f)) { nav.orderNew() }
                    }
                }
                OrderUi.Offline -> ResultSimpleOrder("wifiOff", T.miniTile, T.ink2, "연결 없음", "네트워크에 연결되어 있지 않습니다. 수주는 등록되지 않았습니다. 작성 내용은 그대로 유지됩니다.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostBtn("닫기", Modifier.weight(1f)) { result = null }
                        SolidBtn("다시 시도", Modifier.weight(1f)) { result = null; submit() }
                    }
                }
                OrderUi.Unauthorized -> ResultSimpleOrder("lock", T.amberSoft, T.amber, "세션이 만료되었습니다", "보안을 위해 다시 로그인해야 합니다. 수주는 등록되지 않았습니다.") {
                    SolidBtn("다시 로그인", Modifier.fillMaxWidth()) { nav.logout() }
                }
                is OrderUi.Err -> ResultSimpleOrder("alert", T.redSoft, T.red, "수주 등록에 실패했습니다", r.message) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostBtn("닫기", Modifier.weight(1f)) { result = null }
                        SolidBtn("다시 시도", Modifier.weight(1f)) { result = null; submit() }
                    }
                }
                null -> {}
            }
        }
        ToastHost(scanToast)
    }
}

@Composable
private fun PartPickList(apiMode: Boolean, selected: List<String>, onPick: (String, String?) -> Unit) {
    var q by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val resultMaxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.34f).coerceAtMost(300.dp)
    val resultTap = remember { MutableInteractionSource() }
    // API 모드: item-service OpenSearch 자동검색(search/auto). 시드 모드: Seed.PARTS 필터.
    val itemRepo = remember { ItemRepository() }
    var apiItems by remember { mutableStateOf<List<ItemDto>>(emptyList()) }
    var apiPhase by remember { mutableStateOf(0) } // 0=idle 1=검색중 2=없음 3=오류
    if (apiMode) {
        LaunchedEffect(q) {
            val keyword = q.trim()
            apiItems = emptyList(); apiPhase = 0
            if (keyword.length >= 2) {
                apiPhase = 1
                delay(300)
                when (val r = itemRepo.autocomplete(keyword, size = 5)) {
                    is UiState.Success -> { apiItems = r.data.filter { !it.sku.isNullOrBlank() }; apiPhase = if (apiItems.isEmpty()) 2 else 0 }
                    else -> apiPhase = 3
                }
            }
        }
    }
    val seedList = if (apiMode) emptyList() else Seed.PARTS.filter { q.isBlank() || it.name.contains(q) || it.sku.contains(q, ignoreCase = true) }
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbdIcon("search", 19.dp, T.ink3)
            BasicTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                textStyle = TextStyle(fontSize = 15.5.sp, color = T.ink, fontFamily = Pretendard),
                cursorBrush = SolidColor(T.blue), modifier = Modifier.weight(1f),
                decorationBox = { inner -> if (q.isEmpty()) Text("부품명 또는 코드 검색", color = T.ink3, fontSize = 15.5.sp, fontFamily = Pretendard); inner() },
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(
            Modifier.fillMaxWidth()
                .heightIn(min = 150.dp, max = resultMaxHeight)
                .clickable(interactionSource = resultTap, indication = null) { focus.clearFocus() }
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (apiMode) {
                when {
                    apiPhase == 1 -> Text("검색 중…", fontSize = 14.sp, color = T.ink3Read, modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp), textAlign = TextAlign.Center)
                    apiItems.isNotEmpty() -> apiItems.forEach { item ->
                        val sku = item.sku.orEmpty()
                        PickRow(
                            sku = sku,
                            name = item.name ?: sku,
                            meta = listOfNotNull(item.category, item.unit).filter { it.isNotBlank() }.joinToString(" · "),
                            on = selected.contains(sku),
                            onClick = { onPick(sku, item.name) },
                        )
                    }
                    apiPhase == 2 -> Text("'${q.trim()}' 검색 결과가 없습니다.", fontSize = 14.sp, color = T.ink3Read, modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp), textAlign = TextAlign.Center)
                    apiPhase == 3 -> Text("검색 결과를 불러오지 못했습니다.", fontSize = 14.sp, color = T.red, modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp), textAlign = TextAlign.Center)
                    else -> Text("부품명이나 코드를 입력해 검색하세요.", fontSize = 13.sp, color = T.ink3Read, modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp), textAlign = TextAlign.Center)
                }
            } else {
                seedList.forEach { p ->
                    PickRow(sku = p.sku, name = p.name, meta = "${p.qty}${p.unit}", on = selected.contains(p.sku), thumb = p.thumb, onClick = { onPick(p.sku, p.name) })
                }
                if (seedList.isEmpty()) Text("검색 결과가 없습니다.", fontSize = 14.sp, color = T.ink3Read, modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PickRow(sku: String, name: String, meta: String, on: Boolean, thumb: String = "box", onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if (on) T.hotBg else T.card).border(1.dp, if (on) T.hotBorder else T.line, RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PartThumb(thumb, size = 42.dp, active = on)
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CodeText(sku, size = 12.sp)
                if (meta.isNotBlank()) Text(meta, fontFamily = Mono, fontSize = 11.5.sp, color = T.ink3Read)
            }
        }
        BbdIcon(if (on) "check" else "plus", 18.dp, if (on) T.blue else T.ink3, sw = 2.2f)
    }
}

@Composable
private fun MiniStep(icon: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).background(T.field).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        BbdIcon(icon, 16.dp, if (enabled) T.ink else T.ink3, sw = 2.2f)
    }
}

@Composable
private fun AddBtn(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(12.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        BbdIcon(icon, 17.dp, T.ink2)
        Spacer(Modifier.size(7.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T.ink2)
    }
}

@Composable
private fun ResultSimpleOrder(icon: String, iconBg: Color, iconColor: Color, title: String, body: String, actions: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) { BbdIcon(icon, 25.dp, iconColor, sw = 2f) }
        Spacer(Modifier.size(14.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.size(6.dp))
        Text(body, fontSize = 13.sp, color = T.ink2, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.size(18.dp))
        actions()
    }
}

@Composable
private fun GhostBtn(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(13.dp)).background(T.card).border(1.5.dp, T.line, RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, color = T.ink2, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, fontFamily = Pretendard)
    }
}

@Composable
private fun SolidBtn(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(13.dp)).background(T.blue).clickable(onClick = onClick).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Pretendard)
    }
}
