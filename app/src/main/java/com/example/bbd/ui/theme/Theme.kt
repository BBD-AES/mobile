package com.example.bbd.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private val BbdColors = lightColorScheme(
    primary = T.blue,
    onPrimary = Color.White,
    secondary = T.blueDeep,
    background = T.bg,
    onBackground = T.ink,
    surface = T.card,
    onSurface = T.ink,
    surfaceVariant = T.field,
    error = T.red,
    onError = Color.White,
    outline = T.line,
)

/** 앱 전역 테마. 항상 라이트, 기본 글꼴은 Pretendard / ink. */
@Composable
fun BbdTheme(content: @Composable () -> Unit) {
    val base = TextStyle(fontFamily = Pretendard, color = T.ink)
    MaterialTheme(colorScheme = BbdColors) {
        CompositionLocalProvider(LocalTextStyle provides base, content = content)
    }
}
