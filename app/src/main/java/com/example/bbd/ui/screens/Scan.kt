package com.example.bbd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.Header
import com.example.bbd.ui.Nav
import com.example.bbd.ui.ToastHost
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import com.example.bbd.BuildConfig
import com.example.bbd.data.remote.UiState
import com.example.bbd.data.repo.SalesOrderRepository
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * 입고 스캔 — 항상 SO(발주) 단위 receive. 부품 단위 입고는 백엔드(PATCH /sales-orders/{so}/receive)와
 * 안 맞으므로 제거. 출고 스캔은 백엔드 엔드포인트가 없어 모바일 미지원(진입점 없음).
 * USE_API=on 이면 실 receive 호출, off(데모)면 시드 스텁(성공 토스트)으로 동작.
 */
@Composable
fun ScanScreen(nav: Nav) = ScanReceiveScreen(nav)

// ───────────────────────── QR/바코드 입고 확정 (SO 단위) ─────────────────────────
// ZXing 스캔(또는 SO번호 직접 입력) → PATCH /sales-orders/{so}/receive. 사유 칩 없음(SO 단위엔 사유 개념 없음).

@Composable
private fun ScanReceiveScreen(nav: Nav) {
    val repo = remember { SalesOrderRepository() }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf("") }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { code = it.trim(); error = null }
    }
    if (toast.isNotBlank()) {
        LaunchedEffect(toast) { delay(1100); toast = ""; nav.tab("home") }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(T.bg)) {
            Header(title = "입고 스캔", back = true, chip = "IN", onBack = { nav.pop() })
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            ) {
                Text(
                    "도착한 발주 박스의 바코드/QR을 스캔하면 해당 발주가 입고 확정됩니다.",
                    fontSize = 13.sp, color = T.ink2, lineHeight = 19.sp,
                )
                Spacer(Modifier.size(16.dp))

                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(T.blue)
                        .clickable {
                            error = null
                            scanLauncher.launch(
                                ScanOptions().apply {
                                    setPrompt("도착 발주 바코드/QR을 맞춰 주세요")
                                    setBeepEnabled(true)
                                    setOrientationLocked(false)
                                },
                            )
                        }.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BbdIcon("scan", 20.dp, Color.White, sw = 2f)
                        Text("바코드 / QR 스캔", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.size(16.dp))

                Text("또는 발주번호 직접 입력", fontSize = 12.5.sp, color = T.ink3)
                Spacer(Modifier.size(7.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.card)
                        .border(1.5.dp, T.line, RoundedCornerShape(13.dp)).padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    BasicTextField(
                        value = code, onValueChange = { code = it; error = null }, singleLine = true,
                        textStyle = TextStyle(fontFamily = Mono, fontSize = 16.sp, color = T.ink),
                        cursorBrush = SolidColor(T.blue), modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (code.isEmpty()) Text("SO-2026-0000", color = T.ink3, fontFamily = Mono, fontSize = 16.sp)
                            inner()
                        },
                    )
                }

                error?.let { e ->
                    Spacer(Modifier.size(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(T.redSoft).padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BbdIcon("alert", 18.dp, T.red, sw = 2f)
                        Text(e, fontSize = 13.sp, color = T.redBlockTitle, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(Modifier.fillMaxWidth().background(T.card).padding(16.dp)) {
                val enabled = code.isNotBlank() && !submitting
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (enabled) T.blue else T.line)
                        .clickable(enabled = enabled) {
                            val so = code.trim()   // suspend 경계 전에 한 번 캡처(제출 중 입력 변경과 무관)
                            submitting = true; error = null
                            scope.launch {
                                // USE_API=off(데모)는 백엔드 호출 없이 시드 스텁(성공 토스트).
                                val r: UiState<Unit> =
                                    if (BuildConfig.USE_API) repo.receive(so) else UiState.Success(Unit)
                                when (r) {
                                    is UiState.Success -> { submitting = false; toast = "입고 확정 완료 · $so" }
                                    is UiState.Error -> { submitting = false; error = r.message }
                                    else -> submitting = false
                                }
                            }
                        }.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (submitting) "처리 중…" else "입고 확정", color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        ToastHost(toast)
    }
}
