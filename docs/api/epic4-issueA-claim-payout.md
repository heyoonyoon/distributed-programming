# Epic 4 이슈 A — 의료보험 청구(UC05) + 보험금 즉시지급(UC17) API 명세

> 백엔드 담당(이 pane) 작성. 프론트 연동 단일 출처.
> 브랜치 `epic4-A-health-claim-payout` 기준(아직 main 미머지일 수 있음 — 머지 후 연동 권장).
> 용어는 `CONTEXT.md`, 결정은 `docs/adr/0006~0008`을 따름.

## 인증
- 엔드포인트: `/claims/**` → `Authorization: Bearer <JWT>` 필수, **ROLE_POLICYHOLDER 전용**.
- 토큰 없음/만료 → 401, 타인 계약 접근 → 403, 없는 계약 → 404.

---

## 1. 의료보험 청구 (UC05)

**`POST /claims/health`** — `Content-Type: multipart/form-data`

가입자가 본인의 **유효한(ACTIVE) 의료보험 계약**에 의료비를 청구한다. 서버가 청구금액으로 복잡도를 판별해 **SIMPLE이면 즉시 지급**, **COMPLEX이면 심사 대기**로 둔다.

### 요청 (form fields)
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `contractId` | Long | ✅ | 의료보험 계약 id (본인·ACTIVE·HEALTH) |
| `hospitalName` | String | ✅ | 병원명 |
| `diagnosisCode` | String | ✅ | 진단코드 |
| `treatmentDate` | String(ISO date `yyyy-MM-dd`) | ✅ | 진료일 |
| `requestAmount` | int | ✅ | 청구 금액(> 0). **이 값으로 복잡도 판별** |
| `receiptAmount` | int | ✅ | 영수증 금액(> 0) |
| `attachments` | file[] | ❌ | 증빙(영수증). 0개 이상. 허용 PDF/JPG/PNG, 개당 ≤ 10MB |

### 복잡도 규칙 (UC05 5단계)
- `requestAmount >= 1,000,000`(설정값 `insurance.claim.complex-threshold`) → **COMPLEX**
- 미만 → **SIMPLE**

### 응답 `201 Created`
```json
{ "claimId": 12, "status": "COMPLETED", "complexity": "SIMPLE" }
```
- **SIMPLE**: 즉시 지급 시도됨. `status` = `COMPLETED`(송금 성공) 또는 `FAILED`(송금 실패, UC17 E1 — 가입자 계좌 오류 등).
- **COMPLEX**: `status` = `PENDING`. 화면엔 "담당자 배정 후 심사 예정" 안내. (배정·심사·지급은 **이슈 B에서 별도 API로 제공 예정** — 지금은 PENDING까지)

### `status` / `complexity` enum
- `ClaimStatus`: `PENDING`(접수) · `IN_REVIEW`(심사중) · `APPROVED`(승인) · `REJECTED`(반려) · `COMPLETED`(지급완료) · `FAILED`(지급실패)
- `ClaimComplexity`: `SIMPLE` · `COMPLEX`

### 에러
| 상황 | 코드 | 본문(메시지) |
|------|------|------|
| 첨부 형식 위반(UC05 E1) | 400 | `지원하지 않는 파일 형식입니다. (허용: PDF, JPG, PNG)` |
| 첨부 10MB 초과 | 400 | `파일 크기는 개당 10MB 이하여야 합니다.` |
| 청구금액 ≤ 0 | 400 | `청구 금액은 0보다 커야 합니다.` |
| 의료보험 계약 아님 / 비ACTIVE 계약 | 400 | `의료보험 계약이 아닙니다.` / `유효한 계약이 아닙니다.` |
| 타인 계약 | 403 | (본인 계약만 청구 가능) |
| 없는 계약 | 404 | `계약을 찾을 수 없습니다.` |

> 멀티파트 한도: 서버 `spring.servlet.multipart.max-file-size=10MB`, `max-request-size=50MB`.

---

## 2. UI 가이드 (UC05 화면 흐름)

1. 메인 → '의료보험 청구' 메뉴.
2. 입력 폼: 청구 사유/병원명/진단코드/진료일/청구금액 + 영수증 첨부(드래그/선택).
   - 클라에서도 타입(PDF/JPG/PNG)·크기(10MB) 1차 검증 권장(서버가 최종 검증).
3. '청구 신청' → `POST /claims/health` (multipart).
4. 결과 분기(응답 `complexity`/`status`):
   - `SIMPLE` + `COMPLETED` → "보험금이 지급되었습니다" 완료 화면.
   - `SIMPLE` + `FAILED` → "지급에 실패했습니다. 계좌 정보를 확인해 주세요" 안내(직원이 재시도 처리).
   - `COMPLEX` + `PENDING` → "복잡한 청구로 담당자 배정 후 심사를 진행합니다" 안내.

> **이슈 A 범위 한계**: 보상 처리 현황(UC03)·이력(UC04)·실익분석(UC11) 조회 화면, 자동차사고 접수(UC09), 직원 심사 화면(UC12)은 **후속 이슈(B/C/D)** 입니다. 지금은 "청구 제출 + 결과 표시"만 연동하세요.

---

## 3. 작업 절차 (CLAUDE.md 규약)
- **본인 전용 git worktree**에서 작업(백엔드 pane 워킹트리와 분리). 한 이슈 = 한 브랜치 = 한 PR.
- 지금까지 한 프론트 작업이 있으면 먼저 PR→머지로 main 정리 후, `origin/main` 기준 새 브랜치에서 이 청구 화면을 진행하세요.
- 이 백엔드 브랜치(`epic4-A-health-claim-payout`)가 main에 머지된 뒤 연동하는 것이 안전합니다(엔드포인트 확정).
