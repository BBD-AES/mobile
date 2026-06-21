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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.bbd.ui.BbdIcon
import com.example.bbd.ui.theme.Mono
import com.example.bbd.ui.theme.Pretendard
import com.example.bbd.ui.theme.T

private enum class ErrField { EMP, PW }
private data class LoginError(val title: String, val body: String, val web: Boolean, val field: ErrField = ErrField.EMP)

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var emp by remember { mutableStateOf("BR002") }
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<LoginError?>(null) }

    fun submit() {
        when (val r = Seed.gate(emp)) {
            is LoginResult.Unknown -> err = LoginError("등록되지 않은 사번입니다.", "사번을 확인해 주세요.", web = false, field = ErrField.EMP)
            is LoginResult.Blocked -> {
                val u = r.user
                val role = u.position.ifEmpty { u.role }
                err = LoginError("모바일 이용 불가 계정", "${u.name} · $role — ${u.block}", web = true, field = ErrField.EMP)
            }
            is LoginResult.Allowed -> {
                if (pw.isBlank()) {
                    err = LoginError("비밀번호를 입력해 주세요.", "비밀번호를 확인해 주세요.", web = false, field = ErrField.PW)
                    return
                }
                err = null
                onLogin()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        val minH = maxHeight
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = minH).padding(horizontal = 26.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                // 로고 락업
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(T.ink), contentAlignment = Alignment.Center) {
                        Text("BBD", fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White, letterSpacing = (-0.6).sp)
                    }
                    Column {
                        Text("BBD ERP", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.4).sp)
                        Text("현장 모바일", fontSize = 13.sp, color = T.ink3, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.size(38.dp))

                Text("로그인", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = T.ink, letterSpacing = (-0.7).sp)
                Spacer(Modifier.size(8.dp))
                Text("사번과 비밀번호로 로그인하세요.", fontSize = 14.5.sp, color = T.ink2)
                Spacer(Modifier.size(26.dp))

                FieldLabel("사번")
                LoginField(
                    value = emp, onValue = { emp = it; err = null }, placeholder = "BR000",
                    mono = true, isError = err?.field == ErrField.EMP, password = false,
                )
                Spacer(Modifier.size(14.dp))

                FieldLabel("비밀번호")
                LoginField(
                    value = pw, onValue = { pw = it; err = null }, placeholder = "비밀번호 입력",
                    mono = false, isError = err?.field == ErrField.PW, password = true,
                )
                Spacer(Modifier.size(if (err != null) 14.dp else 24.dp))

                err?.let { e ->
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
                    Spacer(Modifier.size(22.dp))
                }

                // 로그인 버튼
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(T.blue)
                        .clickable { submit() }.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("로그인", color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.size(16.dp))

                Text(
                    "현장 모바일은 정비사·지점 점장만 이용할 수 있어요.\n그 외 계정은 웹 ERP에서 이용하세요.",
                    fontSize = 12.sp, color = T.ink3, textAlign = TextAlign.Center, lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
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
                if (value.isEmpty()) {
                    Text(placeholder, color = T.ink3, fontSize = 16.sp, fontFamily = if (mono) Mono else Pretendard)
                }
                inner()
            },
        )
    }
}
