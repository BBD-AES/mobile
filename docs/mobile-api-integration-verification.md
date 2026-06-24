# 모바일 API 연동 런타임 검증 (2026-06-25)

> 에뮬레이터(Pixel 10, API 모드 `BBD_USE_API=true`) → 배포 게이트웨이 `http://100.73.142.41/`(nginx + sales/inventory/user, Keycloak `bbd-keycloak.inwoohub.com`).
> 로그인: BR037(황인우, 판교지점 WH-DLR-004, 정비사) — Keycloak OIDC PKCE.
> 검증 = logcat OkHttp 로그(HTTP 코드) + 화면 렌더 + 상태변화. 각 API `구현`(코드) / `연동`(실 호출 200/2xx) 분리.

## ✅ 연동 확인 (200 + 실데이터/상태변화)
| 엔드포인트 | 결과 |
|---|---|
| OIDC 로그인 → Bearer | ✅ SSO 로그인, 토큰 게이트웨이 수락 |
| `GET /user/api/v1/users/me` | ✅ 200 — 황인우·WH-DLR-004 신원 enrich |
| `GET /inventory/api/v1/warehouses?size=500` | ✅ 200 — tenancyName→창고코드 매핑 |
| `GET /inventory/api/v1/stocks?warehouseCode=…` | ✅ 200 — 실재고(AC-000423) |
| `GET /sales/api/v1/sales-orders?to_warehouse_code=…` | ✅ 200 — 이동요청 이력 2건(요청자 BR036/BR037) |
| `GET /sales/api/v1/sales-orders?status=IN_FULFILLMENT&…` | ✅ 200 — 도착 대기(SO-2026-0007) |
| `GET /sales/api/v1/notifications` | ✅ 200 (이 유저 0건) |
| **`PATCH /sales/api/v1/sales-orders/{so}/receive`** | ✅ **200** — SO-0007→RECEIVED, arrivals 1→0 재조회 검증 |
| **`POST /inventory/api/v1/stocks/outbound`** | ✅ **200** — AC-000423 1EA 차감, 가용 1→0 (출고번호 CO-2026-7304) |
| **`POST /sales/api/v1/customer-orders`** | ✅ **201** — ACC-000043(실 카탈로그) → item 해석 200 → CO 생성(CO-2026-0011) |
| **`GET /item/api/v1/items/{sku}`** | ✅ **200** — ACC-000043 품목 마스터 해석(현장수주 부품 추가) |

→ **9개 엔드포인트(읽기 6 + 쓰기 3) 전부 실연동 확인.**

## 세션 중 수정·검증한 연동 갭 (커밋)
1. **도착 대기 큐/홈 카운트가 시드 고정** → 실 `repo.arrivals` 연동(App.kt). 라이브 도착 대기 1건 + receive 후 0 검증. (`0fb1512`)
2. **출고 부품 해석이 시드 카탈로그** → 실 재고 API(`InventoryRepository.resolvePart`, stocks?sku)로. AC-000423 출고 200 검증. (`b5dd6d8`)

## 부품 선택 시드 카탈로그 갭 — 수정 완료
- 과거: CO 선택기·출고가 **번들 시드 카탈로그**로 SKU 해석 → 실 SKU 불가, 시드 SKU는 백엔드 `resolveProduct`서 거부(CO 400).
- **수정**: 출고 = `InventoryRepository.resolvePart`(stocks?sku), CO = `ItemRepository`(item-service by-SKU). 둘 다 API 모드에서 실 SKU 해석. (`b5dd6d8`, `1867564`)
- 출고 스캔 **스캔 경로**는 시드 유지(실 바코드 필요) — 수동 입력은 실 재고 해석.
- (마이너) CO 추가 라인 표시명은 SKU fallback — `resolveProduct`가 서버서 이름 채움. 후속 캐시 가능.

## 비핵심 관찰(낮은 우선순위)
- 홈 "최근 입고 완료" 미리보기·재고 주의 위젯은 API 모드에서도 시드(실 이력은 이력 탭, 실 재고는 재고 탭에서 정상).
- 입고 완료 모달 "남은 도착 대기 N건"이 시드 카운트(실 arrivals 아님) — 표시만.
- 재고 목록 DTO는 현재고만(안전재고·가용·상태는 백엔드 Gap 1, 앱이 배너로 정직 표기).
