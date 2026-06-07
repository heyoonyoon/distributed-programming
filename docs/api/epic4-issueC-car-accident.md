# Epic 4 이슈 C — 자동차사고 접수(UC09) API 명세

> 백엔드 담당(이 pane). 브랜치 `epic4-C-car-accident-report` (base: epic4-B). 머지 후 연동 권장.

## 인증
- `/claims/**` → `Authorization: Bearer <JWT>`, **ROLE_POLICYHOLDER 전용**. 토큰 없음 401, 타인계약 403, 없는 계약 404.

## 자동차사고 접수
**`POST /claims/car-accidents`** — `multipart/form-data`. 본인의 **유효한(ACTIVE) 자동차보험 계약**에 사고를 접수한다.

### 요청 (form fields)
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `contractId` | Long | ✅ | 자동차보험 계약 id(본인·ACTIVE·CAR) |
| `accidentDate` | String(`yyyy-MM-dd`) | ✅ | 사고 일자(미래 불가) |
| `accidentLocation` | String | ✅ | 사고 장소(공백 불가) |
| `accidentType` | String | ✅ | 사고 유형(단독/쌍방/대인 등, 공백 불가) |
| `vehicleNumber` | String | ✅ | 차량번호(공백 불가) |
| `hasInjury` | boolean | ✅ | 대인사고 여부 |
| `injuredCount` | int | ✅ | 부상자 수 |
| `attachments` | file[] | ❌ | 현장 사진 등. PDF/JPG/PNG, 개당 ≤ 10MB |

### 응답 `201 Created`
```json
{ "reportId": 31, "status": "PENDING" }
```
- `reportId` = 접수번호. 접수 직후 직원 전원·가입자에게 알림 발송. 이후 심사/지급 흐름은 현재 범위 밖(접수까지).

### 검증 규칙(대인사고 일관성, UC09 A1)
- `hasInjury=true` → `injuredCount ≥ 1` 필수. `hasInjury=false` → `injuredCount = 0`. 위반 시 400.
- 미래 사고일자/공백 필수항목/음수 부상자수 → 400.

### 에러
| 상황 | 코드 | 메시지 |
|------|------|------|
| 첨부 형식/크기(UC09 E1) | 400 | `지원하지 않는 파일 형식입니다...` / `파일 크기는 개당 10MB 이하여야 합니다.` |
| 자동차보험 계약 아님 / 비ACTIVE | 400 | `자동차보험 계약이 아닙니다.` / `유효한 계약이 아닙니다.` |
| 사고정보 유효성 위반 | 400 | (위 검증 규칙 메시지) |
| 타인 계약 | 403 | |
| 없는 계약 | 404 | `계약을 찾을 수 없습니다.` |

## UI 가이드 (UC09)
1. 메인 → '자동차사고 접수'. 사고 일시/장소/유형/차량/부상여부 입력 + 현장사진 첨부.
2. '대인사고' 체크(`hasInjury=true`) 시 부상자 수 입력란 노출(A1) — 1명 이상.
3. '접수하기' → `POST /claims/car-accidents`. 성공 시 접수번호(`reportId`) 안내 화면.

> 가입자 청구화면(의료)=이슈 A, 직원 심사화면=이슈 B, 보상 조회/분석=이슈 D. 본인 worktree·한이슈=한브랜치=한PR.
