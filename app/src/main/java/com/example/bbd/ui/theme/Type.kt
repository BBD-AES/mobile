package com.example.bbd.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.bbd.R

/** UI 기본 글꼴 — Pretendard (한글 우선). */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

/** 코드·숫자용 글꼴 — JetBrains Mono. SKU·사번·타임스탬프·수량 표기. */
val Mono = FontFamily(
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
    Font(R.font.jetbrainsmono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
    Font(R.font.jetbrainsmono_extrabold, FontWeight.ExtraBold),
)
