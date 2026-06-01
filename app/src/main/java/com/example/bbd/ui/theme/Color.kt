package com.example.bbd.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * BBD 디자인 토큰 — 핸드오프 README의 색상 표 그대로.
 * 웹 프로토타입 bbd-data.jsx 의 `T` 객체에 대응.
 */
object T {
    val bg = Color(0xFFEEF1F7)        // 앱 캔버스 (카드 뒤 배경)
    val card = Color(0xFFFFFFFF)      // 카드 표면
    val ink = Color(0xFF171C2B)       // 제목/본문 강한 텍스트
    val ink2 = Color(0xFF5D6677)      // 보조 텍스트
    val ink3 = Color(0xFF9AA3B4)      // 약한 텍스트 / placeholder
    val line = Color(0xFFE6EAF2)      // 카드 보더 / 구분선
    val lineSoft = Color(0xFFEEF1F6)  // 부드러운 내부 구분선
    val blue = Color(0xFF2F56D9)      // 프라이머리
    val blueDeep = Color(0xFF26408F)  // 출고 / 딥 액센트
    val blueSoft = Color(0xFFE9EFFF)  // 파랑 칩 / 아이콘 배경
    val blueInk = Color(0xFF2647B8)   // 파랑 칩 위 텍스트
    val red = Color(0xFFE5484D)       // 없음 / 초과 / 음수
    val redSoft = Color(0xFFFDECEC)   // 빨강 칩 배경
    val green = Color(0xFF1F9D57)     // 정상 dot
    val amber = Color(0xFFD98A1F)     // 경고 (오래된 비번, 출고 배너)

    // 컴포넌트에서 쓰는 보조 색
    val white = Color(0xFFFFFFFF)
    val outCircle = Color(0xFF1F2535)   // 출고 이동 원형 배경
    val toast = Color(0xFF1C2233)       // 토스트 배경
    val field = Color(0xFFF7F9FC)       // 입력 필드 배경(비포커스)
    val miniTile = Color(0xFFF2F5FB)    // 미니카드/설정 아이콘 타일
    val segTrack = Color(0xFFE7EBF3)    // 세그먼트 트랙
    val scrim = Color(0x73121622)       // 바텀시트 스크림 rgba(18,22,34,0.45)
    val scrimModal = Color(0x80121622)  // 모달 스크림 rgba(18,22,34,0.5)
    val thumbBorder = Color(0xFFD6DCE8)
    val thumbBorderActive = Color(0xFFB9C6EE)
    val thumbBgActive = Color(0xFFF3F6FF)
    val hotBorder = Color(0xFFC9D6F7)
    val hotBg = Color(0xFFF6F9FF)
    val disabledRed = Color(0xFFF0C2C3)  // 비활성 제출 버튼

    // 배너/블록 보조 색
    val inBannerBg = Color(0xFFF1F6FF)
    val inBannerBorder = Color(0xFFD8E4FF)
    val outBannerBg = Color(0xFFFFF7EF)
    val outBannerBorder = Color(0xFFF0DDC0)
    val redBlockBorder = Color(0xFFF6C9CA)
    val amberBlockBg = Color(0xFFFFF7EF)
    val amberBlockBorder = Color(0xFFF0DDC0)
    val amberBlockTitle = Color(0xFF9A6516)
    val redBlockTitle = Color(0xFFB5363A)
    val receivedBg = Color(0xFFEAF6EF)
    val receivedFg = Color(0xFF1F7D47)
}
