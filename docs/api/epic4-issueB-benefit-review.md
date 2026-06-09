# Epic 4 이슈 B — 복잡청구 심사(UC12) + 담당자 지정(UC14) API 명세

> 백엔드 담당(이 pane) 작성. 프론트(직원 화면) 연동 단일 출처.
> 브랜치 `epic4-B-benefit-review-assignment` (base: epic4-A). 머지 후 연동 권장.
> 용어 `CONTEXT.md`, 결정 `docs/adr/0001,0006,0007`.

## 인증
- 엔드포인트: `/staff/**` → `Authorization: Bearer <JWT>`, **ROLE_EMPLOYEE 전용**.
- 가입자(POLICYHOLDER) 토큰 접근 → 403. 토큰 없음 → 401.
- 현재 사용자(직원) id는 토큰에서 추출(`@AuthenticationPrincipal`).

## 청구 흐름 복습
의료보험 청구가 **COMPLEX**(청구금액 ≥ 100만)면 시스템이 자동으로 보험금 지급 심사(`BenefitPaymentReview`)를 만들고 **최소 부하 직원에게 자동 배정**한 뒤 청구를 `IN_REVIEW`로 둔다. 배정된 직원이 심사를 확정한다(승인→지급 / 반려).

---

## 1. 배정된 심사 대기 목록 (UC12 step2)
**`GET /staff/benefit-reviews`** — 로그인한 직원에게 배정된 **미확정** 건만.
```json
[ { "claimId": 12, "requestAmount": 2000000, "hospitalName": "서울병원", "claimStatus": "IN_REVIEW" } ]
```

## 2. 심사 상세 (UC12 step4)
**`GET /staff/benefit-reviews/{claimId}`**
```json
{ "claimId": 12, "requestAmount": 2000000, "hospitalName": "서울병원",
  "diagnosisCode": "S00", "claimStatus": "IN_REVIEW", "assignedStaffId": 7 }
```
- **비배정 직원이 접근 → 409** `현재 담당자가 처리 중인 건입니다.` (UC12 E2 중복접근 차단)

## 3. 심사 확정 (UC12 step7 / A1)
**`POST /staff/benefit-reviews/{claimId}/confirm`**
```json
{ "result": "APPROVED", "comment": "정상 청구" }   // 또는 "REJECTED" + 반려사유
```
응답 `200`:
```json
{ "claimId": 12, "result": "APPROVED", "claimStatus": "COMPLETED" }
```
- `APPROVED` → 보험금 즉시 지급. 송금 성공 시 `claimStatus="COMPLETED"`, 실패 시 `"FAILED"`(재시도 필요).
- `REJECTED` → `claimStatus="REJECTED"`, 지급 미수행. `comment`에 반려사유 필수 입력 권장.
- `result`는 `APPROVED`/`REJECTED`만. (`CONDITIONAL` 불가 → 400)
- **이미 확정된 건 재확정 → 409** `이미 확정된 심사입니다.`
- 비배정 직원 → 409.

## 4. 지급 재시도 (UC17 E1)
**`POST /staff/benefit-reviews/{claimId}/retry`** — 송금 실패(`FAILED`)건 재시도. 가입자 계좌 정정 후 호출.
- 배정 담당자만 가능(비배정 409). 성공 시 `COMPLETED`.

## 5. 수동 재배정 (UC14 A1)
**`POST /staff/claims/{claimId}/assign`** — body `{ "employeeId": 9 }`.
- 미확정 심사만 재배정 가능(확정건 → 409). 이전 담당자 부하 자동 회수.
- ⚠️ **범위 한계**: 현재 관리자 역할이 없어 모든 EMPLOYEE가 호출 가능(학습 프로젝트 단순화). 운영에선 관리자 전용으로 제한 필요.

---

## 6. UI 가이드
1. 직원 로그인 → '보험금 지급 심사 대기 목록'(`GET /staff/benefit-reviews`).
2. 행 클릭 → 상세(`GET .../{claimId}`). 비배정건은 409 → "다른 담당자 처리 중" 안내.
3. 승인/반려 선택 + 의견 입력 → `confirm`. 결과 `claimStatus`로 분기:
   - `COMPLETED` → "지급 완료", `FAILED` → "지급 실패, 계좌 확인 후 재시도"(retry 버튼), `REJECTED` → "반려 완료".
4. `FAILED` 건은 재시도 버튼 → `retry`.

> 가입자 화면(청구 제출)은 이슈 A 문서(`docs/api/epic4-issueA-claim-payout.md`). 보상 현황/이력/분석(UC03/04/11)은 후속 이슈 D, 자동차사고(UC09)는 이슈 C.

## 7. 작업 절차
- 본인 전용 git worktree, 한 이슈=한 브랜치=한 PR. 백엔드 `epic4-B...`가 main 머지된 뒤 연동 권장(엔드포인트 확정).
