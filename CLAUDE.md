# BBD ERP — 현장 모바일 (Android)

현대 오토에버 4차 프로젝트 "현대 파츠(가상)" 통합 부품 ERP의 **현장 모바일 앱**. 정비사·지점 점장이 부품 입·출고 스캔, 지점 재고 조회, 본사 보충 발주, 작업 이력을 처리한다. 더 큰 웹 ERP의 모바일 동반 앱(일부 기능만 노출).

## 스택
- Jetpack Compose (Material3), 단일 `MainActivity`, 패키지 `com.example.bbd`
- Kotlin 2.2.10 · **AGP 9.2.1 (Kotlin 내장)** · Gradle 9.4.1 · Compose BOM 2026.02.01
- minSdk 24 / compileSdk 36 / Java 17
- 폰트: Pretendard(UI) + JetBrains Mono(코드·숫자) — `app/src/main/res/font` 번들
- 데이터: 기본은 앱 내장 시드(`data/Seed.kt`). **`USE_API=true` 면 실 ERP API(`data/remote`·`data/repo`)로 대체** — 연동 진행 중(게이트웨이 OIDC Bearer). 플래그 off(기본)는 여전히 시드.

## 빌드 / 실행
```bash
./gradlew :app:assembleDebug                 # APK 빌드
SDK=$HOME/Library/Android/sdk
"$SDK/emulator/emulator" @Pixel_10 &         # 에뮬레이터
"$SDK/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
"$SDK/platform-tools/adb" shell am start -n com.example.bbd/.MainActivity
```
로그인: 사번 `BR002`(정민수·정비사) + 임의 비밀번호(비어있지 않으면 통과 — 데모) 입력 후 로그인. 모바일 허용 계정은 `BR002`(정비사)·`BR001`(점장)뿐.

## 구조
```
app/src/main/java/com/example/bbd/
  MainActivity.kt            엣지투엣지 + BbdTheme + BbdApp
  ui/App.kt                  Nav(스택+탭) 라우터, 스캔 액션시트
  ui/theme/                  Color(T 토큰)·Type(폰트)·Theme
  ui/Icons.kt                28종 라인아이콘 + PartThumb (SVG→Canvas)
  ui/Components.kt           카드/헤더/탭바/Screen/StatusPill/MovementRow…
  ui/Overlays.kt             SheetHost(바텀시트)·ModalHost·ToastHost
  ui/screens/                Login·Home·Inventory·Scan·Order·My·Worklog
  data/Model.kt, Seed.kt     모델·시드·로그인 게이팅
```

## 핵심 규칙 (수정 시 주의)
- **로그인 게이팅**(`Seed.gate`): 정비사(BRANCH_STAFF)·점장(BRANCH_MANAGER)만 허용. 미등록=빨강, 차단 계정=앰버 블록. 가장 중요한 비즈니스 규칙.
- **출고 초과 가드**: 출고 수량 > 지점 현재고 면 배너·스테퍼 빨강 + 등록 버튼 비활성.
- **권한**: 보충 발주는 작성·자기지점 조회만, 승인/거절은 본사(상태 표시 전용).
- UI는 **항상 라이트** 고정. 색/치수/카피는 디자인 핸드오프 README 기준(`~/Downloads/_bbd_extract/handoff/`).

## 빌드 설정 함정
AGP 9는 Kotlin 내장. `org.jetbrains.kotlin.android`(kotlin-android) 플러그인을 **추가 적용하면 안 됨**(`kotlin` 확장 중복 등록 오류). app `plugins`에는 `android-application` + `kotlin-compose`만. Kotlin 옵션은 `android{}` 밖 최상위 `kotlin{}` 블록.
