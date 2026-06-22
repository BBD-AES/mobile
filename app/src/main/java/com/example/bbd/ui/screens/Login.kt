package com.example.bbd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbd.data.LoginResult
import com.example.bbd.data.Seed
import com.example.bbd.data.toCurrentUser
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.Spinner
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.bbd.BuildConfig
import com.example.bbd.auth.AuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ErrField { EMP, PW }
private data class LoginError(val title: String, val body: String, val web: Boolean, val field: ErrField = ErrField.EMP)

@Composable
fun LoginScreen(
    onLogin: (String) -> Unit,
    onLoginAs: (com.example.bbd.data.CurrentUser) -> Unit,
) {
    // release/USE_API = 항상 OIDC. debug + 시드 = 데모 사번/비번 게이팅.
    // OIDC 성공 시 게이트웨이 /me 로 실 신원을 받아 onLoginAs 로 설정(데모는 onLogin 으로 시드 resolve).
    if (BuildConfig.USE_API || !BuildConfig.DEBUG) OidcLoginScreen(onLoginAs) else DemoLoginScreen(onLogin)
}

// ───────────────────────── 데모(시드) 로그인 ─────────────────────────

@Composable
private fun DemoLoginScreen(onLogin: (String) -> Unit) {
    var emp by remember { mutableStateOf("BR002") }
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<LoginError?>(null) }
    var busy by remember { mutableStateOf(false) }

    if (busy) LaunchedEffect(Unit) { delay(650); onLogin(emp.trim().uppercase()) }

    fun submit() {
        if (busy) return
        when (val r = Seed.gate(emp)) {
            is LoginResult.Unknown -> err = LoginError("등록되지 않은 사번입니다.", "사번을 확인해 주세요.", web = false, field = ErrField.EMP)
            is LoginResult.Blocked -> {
                val u = r.user
                val role = u.position.ifEmpty { u.role }
                err = LoginError("모바일 이용 불가 계정", "${u.name} · $role — ${u.block}", web = true, field = ErrField.EMP)
            }
            is LoginResult.Allowed -> { err = null; busy = true }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        val minH = maxHeight
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = minH).padding(horizontal = 26.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                LogoLockup()
                Spacer(Modifier.size(36.dp))

                Text("로그인", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.7).sp)
                Spacer(Modifier.size(8.dp))
                Text("사번과 비밀번호로 로그인하세요.", fontSize = 14.5.sp, color = T.ink2)
                Spacer(Modifier.size(12.dp))
                // 데모 게이팅 안내
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color(0xFFFAFBFE)).border(1.dp, T.line, RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DemoTag()
                    Text("이 사번/비밀번호 폼은 데모 게이팅용입니다. 실 환경은 Keycloak(SSO)으로 로그인합니다.", fontSize = 12.sp, color = T.ink2, lineHeight = 18.sp)
                }
                Spacer(Modifier.size(24.dp))

                FieldLabel("사번")
                LoginField(emp, { emp = it; err = null }, "BR000", mono = true, isError = err?.field == ErrField.EMP, password = false)
                Spacer(Modifier.size(14.dp))

                FieldLabel("비밀번호")
                LoginField(pw, { pw = it; err = null }, "비밀번호 입력", mono = false, isError = err?.field == ErrField.PW, password = true)
                Spacer(Modifier.size(16.dp))

                // 데모 계정 칩 — 비밀번호 없이 전환
                Text(buildString { append("데모 계정 · 비밀번호 없이 전환") }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = T.ink3Read)
                Spacer(Modifier.size(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DemoAccountChip("BR002", "정비사", "BRANCH_STAFF", emp.trim().uppercase() == "BR002", Modifier.weight(1f)) { emp = "BR002"; err = null }
                    DemoAccountChip("BR001", "점장", "BRANCH_MANAGER", emp.trim().uppercase() == "BR001", Modifier.weight(1f)) { emp = "BR001"; err = null }
                }
                Spacer(Modifier.size(if (err != null) 14.dp else 24.dp))

                err?.let { e -> LoginErrorBanner(e); Spacer(Modifier.size(22.dp)) }

                PrimaryLoginButton(if (busy) "인증 중…" else "로그인", busy) { submit() }
                Spacer(Modifier.size(16.dp))
                LoginFooter()
            }
        }
    }
}

// ───────────────────────── OIDC(Keycloak) 로그인 (USE_API/release) ─────────────────────────

@Composable
private fun OidcLoginScreen(onLoginAs: (com.example.bbd.data.CurrentUser) -> Unit) {
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val userRepo = remember { com.example.bbd.data.repo.UserRepository() }
    val authRepo = remember { com.example.bbd.data.repo.AuthRepository() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        AuthManager.handleResult(result.data) { ok, err ->
            if (!ok) {
                loading = false
                error = err ?: "로그인에 실패했어요."
                return@handleResult
            }
            // 토큰 교환 성공 → 실 신원 resolve(OIDC 엔 사번 입력이 없음).
            // 우선순위: user-service /user/api/v1/users/me(권위 role + 지점명) →
            //          실패 시 게이트웨이 /api/auth/me(신원-only, role=position best-effort)로 폴백.
            // 둘 다 실패면 에러 표시(홈 진입 막기 — 시드 날조 금지).
            scope.launch {
                when (val us = userRepo.me()) {
                    // 1순위: /users/me — 권위 role + tenancyName(지점명).
                    is com.example.bbd.data.remote.UiState.Success -> {
                        loading = false
                        onLoginAs(us.data.toCurrentUser())
                    }
                    // /users/me 실패(미등록/에러) → 게이트웨이 /api/auth/me 신원-only 폴백.
                    is com.example.bbd.data.remote.UiState.Error -> {
                        when (val st = authRepo.me()) {
                            is com.example.bbd.data.remote.UiState.Success -> {
                                val dto = st.data
                                if (dto.authenticated) {
                                    loading = false
                                    onLoginAs(dto.toCurrentUser())
                                } else {
                                    loading = false
                                    error = dto.message ?: "인증 정보를 확인하지 못했어요."
                                }
                            }
                            is com.example.bbd.data.remote.UiState.Error -> {
                                loading = false
                                error = "사용자 정보를 불러오지 못했어요. (${st.message})"
                            }
                            else -> { /* Loading 은 repo 가 반환하지 않음 */ }
                        }
                    }
                    else -> { /* Loading 은 repo 가 반환하지 않음 */ }
                }
            }
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(17.dp)).background(T.ink), contentAlignment = Alignment.Center) {
                Text("BBD", fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White, letterSpacing = (-0.6).sp)
            }
            Spacer(Modifier.size(18.dp))
            Text("BBD ERP 현장 모바일", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.7).sp)
            Spacer(Modifier.size(8.dp))
            Text("사내 계정(SSO)으로 안전하게 로그인하세요.", fontSize = 14.5.sp, color = T.ink2, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.size(30.dp))

        error?.let { e ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.redSoft)
                    .border(1.dp, T.redBlockBorder, RoundedCornerShape(13.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                BbdIcon("alert", 19.dp, T.red, sw = 2f)
                Text(e, fontSize = 13.sp, color = T.redBlockTitle, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(14.dp))
        }

        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(T.blue)
                .clickable(enabled = !loading) {
                    loading = true; error = null
                    AuthManager.beginLogin { intent ->
                        if (intent != null) launcher.launch(intent)
                        else { loading = false; error = "인증 서버에 연결하지 못했어요." }
                    }
                }.padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (loading) Spinner(17.dp) else BbdIcon("key", 20.dp, Color.White, sw = 1.9f)
                Text(if (loading) "브라우저로 이동 중…" else "Keycloak으로 로그인", color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.size(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            BbdIcon("lock", 13.dp, T.ink3Read, sw = 2f)
            Spacer(Modifier.size(6.dp))
            Text("auth.bbd.co.kr · realm: bbd", fontFamily = Mono, fontSize = 11.5.sp, color = T.ink3Read)
        }
        Spacer(Modifier.size(22.dp))
        LoginFooter()
    }
}

// ───────────────────────── 공용 ─────────────────────────

@Composable
private fun LogoLockup() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(T.ink), contentAlignment = Alignment.Center) {
            Text("BBD", fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White, letterSpacing = (-0.6).sp)
        }
        Column {
            Text("BBD ERP", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp)
            Text("현장 모바일", fontSize = 13.sp, color = T.ink3Read, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DemoTag() {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(T.amberSoft).padding(horizontal = 7.dp, vertical = 2.dp)) {
        Text("데모 전용", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = T.amberInk)
    }
}

@Composable
private fun DemoAccountChip(id: String, pos: String, role: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (on) T.blueSoft else T.field)
            .border(1.5.dp, if (on) T.hotBorder else T.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(id, fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (on) T.blueInk else T.ink)
            Text(pos, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (on) T.blueInk else T.ink2)
        }
        Spacer(Modifier.size(3.dp))
        Text(role, fontFamily = Mono, fontSize = 11.sp, color = T.ink3Read)
    }
}

@Composable
private fun LoginErrorBanner(e: LoginError) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .background(if (e.web) T.amberBlockBg else T.redSoft)
            .border(1.dp, if (e.web) T.amberBlockBorder else T.redBlockBorder, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        BbdIcon(if (e.web) "info" else "alert", 19.dp, if (e.web) T.amber else T.red, sw = 2f)
        Column {
            Text(e.title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = if (e.web) T.amberBlockTitle else T.redBlockTitle)
            Spacer(Modifier.size(3.dp))
            Text(e.body, fontSize = 12.5.sp, color = T.ink2, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun PrimaryLoginButton(label: String, busy: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.blue)
            .clickable(enabled = !busy, onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (busy) Spinner(17.dp)
            Text(label, color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LoginFooter() {
    Text(
        "현장 모바일은 정비사·지점 점장만 이용할 수 있어요.\n그 외 계정은 웹 ERP에서 이용하세요.",
        fontSize = 12.sp, color = T.ink3Read, textAlign = TextAlign.Center, lineHeight = 18.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T.ink2)
    Spacer(Modifier.size(7.dp))
}

@Composable
private fun LoginField(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    mono: Boolean,
    isError: Boolean,
    password: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val border = if (isError) T.red else if (focused) T.blue else T.line
    val bg = if (focused) Color.White else T.field
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(bg)
            .border(1.5.dp, border, RoundedCornerShape(13.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = TextStyle(fontFamily = if (mono) Mono else Pretendard, fontSize = 16.sp, color = T.ink),
            cursorBrush = SolidColor(T.blue),
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = if (mono) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                keyboardType = if (password) KeyboardType.Password else KeyboardType.Ascii,
            ),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = T.ink3, fontSize = 16.sp, fontFamily = if (mono) Mono else Pretendard)
                inner()
            },
        )
    }
}
