# 자동차사고 보험금 심사 (Car Accident Benefit Review)

작성일: 2026-06-04
브랜치: `epic4-car-accident-review`

## 배경 / 문제

데모에서 직원의 보상심사 화면(`GET /staff/benefit-reviews`)에 자동차사고 접수건이 뜨지 않는다.

현재 구현:
- 자동차사고(`CarAccidentReport`)는 `CarAccidentService.report()`에서 **접수만** 한다(저장 + 접수번호 + 알림). 심사·지급 흐름이 없다(UC09 구현 그대로).
- 보상심사 큐(`BenefitPaymentReview`)는 **COMPLEX 의료보험 청구 전용**으로, `HealthInsuranceClaim`에 하드코딩되어 있다(UC12 선행조건 = UC05 복잡 청구건).

즉 자동차사고는 설계상 보상심사 대상이 아니라서 큐에 영영 뜨지 않는다. 이건 버그가 아니라 **미정의 유스케이스**다.

## 목표 (제품 결정)

직원이 자동차사고 접수건을 **시스템 안에서** 심사하고, 승인 시 **직원이 직접 입력한 보험금**을 지급한다. 의료 복잡건과 **같은 보상심사 큐**에서 함께 처리한다.

확정된 결정(모두 잠금):
1. **큐 통합**: 자동차사고 접수 즉시 심사건 자동 생성 + 담당자 자동 배정. 기존 `/staff/benefit-reviews` 한 화면에 의료·자동차가 함께 뜬다.
2. **모델 일반화**: `BenefitPaymentReview`의 청구 참조를 `HealthInsuranceClaim` → 부모 `Claim`으로 넓힌다. 새 서브타입을 만들지 않는다.
3. **금액 입력**: 승인 시 직원이 보험금 액수를 입력한다(반려는 금액 불필요). 검증은 `> 0`만.
4. 지급은 기존 `BenefitPayoutService.pay(Claim)`을 재사용한다.

비목표(YAGNI): 보장한도 대조 검증, 사고유형↔보장항목 매핑, 자동차 전용 심사 화면/엔드포인트 분리.

## 도메인 흐름

```
[가입자] 자동차사고 접수(POST /claims/car-accidents)
   → CarAccidentReport 저장 (requestAmount=0, status=PENDING)
   → BenefitPaymentReview 생성 + 담당자 자동배정(StaffAssignmentService)
   → report.markInReview()  (status=IN_REVIEW)
   → 직원/가입자 알림 (기존 유지)

[직원] 보상심사 목록(GET /staff/benefit-reviews) → 의료·자동차 함께 표시
[직원] 상세(GET /staff/benefit-reviews/{claimId})
[직원] 확정(POST /staff/benefit-reviews/{claimId}/confirm)
   - 승인(APPROVED) + payoutAmount(>0):
        report.assessPayout(payoutAmount)  (requestAmount 세팅)
        claim.markApproved() → BenefitPayoutService.pay(claim)  (성공 COMPLETED / 실패 FAILED)
   - 반려(REJECTED): claim.markRejected() + 가입자 통보 (금액 불필요)
```

의료 복잡건의 기존 흐름·금액 산정 방식은 **그대로 둔다**. 자동차사고만 금액 입력이 필수다.

## 변경 사항 (컴포넌트별)

### 도메인
- `BenefitPaymentReview`
  - 필드 `HealthInsuranceClaim claim` → `Claim claim`. 생성자 인자도 `Claim`.
  - `@OneToOne` join은 `claim_id`(JOINED 부모 테이블 PK)로 그대로 동작.
  - discriminator `BENEFIT_PAYMENT` 유지.
- `Claim`
  - 심사 후 산정 금액을 세팅하는 도메인 메서드 추가. 예: `protected void assignAssessedAmount(int amount)` (음수/0 방어).
- `CarAccidentReport`
  - `public void assessPayout(int amount)` 추가 → `assignAssessedAmount` 호출. 금액 `> 0` 검증.

### 서비스
- `CarAccidentService.report()`
  - report 저장 후: `BenefitPaymentReview` 생성·저장 → `StaffAssignmentService.assignAutomatically(review)` → `report.markInReview()`.
  - 기존 직원·가입자 알림은 유지(중복 알림 방지를 위해 메시지 정리 가능).
  - 직원이 0명이면 기존 의료 흐름과 동일하게 `IllegalStateException`으로 롤백.
- `BenefitReviewService.confirm(staffId, claimId, result, comment, Integer payoutAmount)`
  - 청구 타입을 `instanceof CarAccidentReport`로 분기.
  - 자동차 + 승인: `payoutAmount` 필수(null·0·음수면 400) → `report.assessPayout(amount)` → `markApproved()` → `payoutService.pay(claim)`.
  - 의료 + 승인: `payoutAmount`는 무시(또는 제공 시 400)하고 기존대로.
  - 반려: 금액 무관, 기존대로 `markRejected()` + 통보.
  - `claim` 타입을 `Claim`으로 다뤄 `markApproved/markRejected/getContract().getPolicyholder()` 사용(모두 부모에 존재).
- `assignedReviews` / `requireOwned` / `detail` / `retryPayout`: 쿼리·로직 변경 없음(이미 `r.claim` 기준, 부모 타입으로 동작).

### 웹 / DTO
- `ConfirmBenefitReviewRequest`: `Integer payoutAmount` 추가(nullable; 자동차 승인 시에만 사용).
- `BenefitReviewSummaryResponse` / `BenefitReviewDetailResponse`
  - `claim.getHospitalName()`/`getDiagnosisCode()`는 의료 전용 → **타입 분기 필요**.
  - 공통 필드 + `String claimType`("HEALTH"/"CAR_ACCIDENT") 추가. 의료 전용(hospitalName, diagnosisCode)·자동차 전용(accidentType, accidentLocation, vehicleNumber, hasInjury) 필드는 해당 없으면 null.
  - 기존 필드명은 유지(프론트 하위호환).
- `StaffReviewController.confirm`: 요청의 `payoutAmount`를 서비스로 전달.

### 시드/데모
- 별도 시드 변경 없음. 직원은 기존 `staff@test.com` 하나. 자동차사고를 접수하면 그 직원에게 자동 배정되어 화면에 뜬다.

## 에러 규약
- 자동차 승인인데 `payoutAmount` 누락/≤0 → `400`(`InvalidRequestException`).
- 이미 확정된 심사 재확정 → 기존대로 `IllegalStateTransitionException`.
- 담당자 아님 → 기존대로 차단.

## 테스트 전략 (TDD)
- 도메인: `CarAccidentReport.assessPayout` 금액 검증·상태; `BenefitPaymentReview`가 `Claim`(자동차) 보유.
- 서비스: 접수 시 review 생성·자동배정·IN_REVIEW 전이; confirm 승인(금액>0 → 지급/COMPLETED), 승인 금액누락 400, 반려(금액 없이 REJECTED), 의료건 기존 동작 회귀.
- 웹: `/staff/benefit-reviews`에 자동차건 노출, confirm with payoutAmount.
- 기존 의료 심사 테스트 전부 그린 유지(회귀 금지).

## 미해결/그릴링 대상
- 용어: "보험금 지급 심사(BenefitPaymentReview)"가 자동차까지 포함 → CONTEXT.md 정의 확장 필요.
- UC12가 의료 전용으로 못박혀 있음 → 자동차 심사 흐름을 어디에 귀속시킬지 ADR로 기록.
