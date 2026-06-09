# Epic 4 이슈 D — 보상 현황(UC03) + 이력(UC04) + 실익분석(UC11) API 명세

> 백엔드 담당(이 pane). 브랜치 `epic4-D-claim-queries` (base: epic4-C). 머지 후 연동 권장.

## 인증
- `/claims/**` → `Authorization: Bearer <JWT>`, **ROLE_POLICYHOLDER 전용**. 본인 데이터만 조회.

## 1. 보상 처리 현황 (UC03)
**`GET /claims/status`** — 진행 중인 보상 건만(상태 ∈ PENDING/IN_REVIEW/APPROVED/FAILED).
```json
[ { "claimId": 12, "claimType": "HEALTH", "claimDate": "2026-06-04",
    "requestAmount": 2000000, "paidAmount": 0, "status": "IN_REVIEW" } ]
```
- `claimType`: `HEALTH`(의료청구) / `CAR`(자동차사고접수).
- 진행단계는 `status`로 표현: `PENDING`(접수) → `IN_REVIEW`(심사중) → `APPROVED`(승인) → 종결. `FAILED`는 지급 실패(직원 재시도 대기).
- 진행 건 없으면 `[]`.

## 2. 보상 이력 (UC04)
**`GET /claims/history?from=YYYY-MM-DD&to=YYYY-MM-DD`** — 기간 내 **종결된**(COMPLETED/REJECTED) 보상.
- `from`/`to` 생략 시 기본 최근 1년. `from > to`면 400.
```json
[ { "claimId": 8, "claimType": "HEALTH", "claimDate": "2026-03-01",
    "requestAmount": 500000, "paidAmount": 400000, "status": "COMPLETED" } ]
```
- `paidAmount` = 해당 청구의 성공 지급 합계. 이력 없으면 `[]`.

## 3. 실익 분석 (UC11)
**`GET /claims/benefit-analysis?contractId={id}`**
> ⚠️ 경로 주의: spec 초안의 `/contracts/{id}/benefit-analysis`가 아니라 **`/claims/benefit-analysis?contractId=`** 입니다(컨트롤러 응집).
```json
{ "totalPaidPremium": 60000, "totalReceivedBenefit": 400000, "profit": 340000, "profitRate": 6.67 }
```
- `profit` = 총수령 − 총납입. `profitRate` = 총수령 / 총납입(납입 0이면 0).
- **가입 6개월 미만** → 400 `분석에 필요한 데이터가 충분하지 않습니다. 6개월 이후 이용 가능합니다.`
- 타인 계약 → 403. 없는 계약 → 404.
- 유사 가입자 대비 비교는 범위에서 제외(본인 실익만).

## UI 가이드
- 현황: `GET /claims/status` → 진행 건 목록, `status` 배지로 단계 표시.
- 이력: 기간 선택(기본 1년) → `GET /claims/history` → 처리일/종류/청구액/지급액/결과 테이블.
- 실익: 계약 선택 → `GET /claims/benefit-analysis?contractId=` → 총납입/총수령/실익/실익률(그래프). 6개월 미만 안내 처리.

> 청구 제출(의료)=이슈 A, 자동차 접수=이슈 C, 직원 심사=이슈 B. 본인 worktree·한이슈=한브랜치=한PR.
