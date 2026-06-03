# Epic 4 — 보상 / 청구 설계

> 도메인: 보상·청구(Claim/Payout). Epic 0~3 완료 상태에서 출발.
> 선행: `Policyholder`(bankAccount 보유), `InsuranceContract`, `InsuranceEmployee`(currentLoad 보유), 기존 `NotificationSender`·`PaymentGateway`·`FileStorage` 패턴 존재.
> 대상 유스케이스: UC03, UC04, UC05, UC09, UC11, UC12, UC14, UC17.

---

## 1. 범위 & 이슈 분할

Epic 4는 하나의 spec으로 설계하되, 구현(plan/branch/PR)은 4개 이슈로 분리한다.

| 이슈 | UC | 범위 |
|------|-----|------|
| **A** | UC05, UC17 | 의료보험 청구 + 복잡도 판별 + 간단청구 즉시지급 |
| **B** | UC12, UC14 | 복잡청구 담당자 배정 + 심사(승인→지급 / 반려) |
| **C** | UC09 | 자동차사고 접수(접수번호 발급 + 직원 알림까지) |
| **D** | UC03, UC04, UC11 | 보상 현황·이력 조회 + 실익분석 |

이슈 B는 A에 의존(복잡청구가 A에서 생성됨). C·D는 A와 병렬 가능하나 동일 도메인 패키지를 공유하므로 A 머지 후 진행 권장.

---

## 2. 도메인 모델

클래스 다이어그램(`04_claim`, `05_review`, `06_payment`)을 그대로 구현한다.

### Claim 계층 (`domain/claim/`)
- `Claim` (abstract): `claimId`, `claimDate`, `requestAmount`, `status`(ClaimStatus). `InsuranceContract`와 composition.
- `HealthInsuranceClaim extends Claim`: `hospitalName`, `diagnosisCode`, `treatmentDate`, `receiptAmount`, `complexity`(ClaimComplexity). 첨부 `ClaimAttachment` 컬렉션.
- `CarAccidentReport extends Claim`: `accidentDate`, `accidentLocation`, `accidentType`, `vehicleNumber`, `hasInjury`, `injuredCount`. 첨부 컬렉션.
- `ClaimAttachment`: `filename`, `contentType`, `sizeBytes`, `storedPath`. (바이너리는 디스크, 엔티티엔 메타+경로)

### enum
- `ClaimStatus`: `PENDING`(접수) → `IN_REVIEW`(심사중) → `APPROVED` / `REJECTED` → `COMPLETED`(지급완료) / `FAILED`(지급실패).
  UC03의 진행 흐름도(접수 완료 → 담당자 배정 → 심사 중 → 완료)에 매핑.
- `ClaimComplexity`: `SIMPLE` / `COMPLEX`.

### Review / Payment 계층 (`domain/review/`, `domain/payment/`)
- `BenefitPaymentReview extends Review`: `assignedStaffId`(InsuranceEmployee 참조), `result`(ReviewResult), `comment`.
  `approve() → BenefitPayment`, `reject(reason)`. 승인 시 `BenefitPayment` composition.
- `BenefitPayment`: `paymentId`, `paidAmount`, `paidAt`, `bankAccount`, `status`(PaymentStatus: SUCCESS/FAILED).
  간단청구(SIMPLE)는 `BenefitPaymentReview` 없이 청구 흐름에서 직접 생성(ADR 후보 → grill 단계에서 확정).

### 관계
```
InsuranceContract ◆── Claim                    (composition)
HealthInsuranceClaim → BenefitPaymentReview    (복잡청구만, association)
BenefitPaymentReview ◆── BenefitPayment        (composition; 심사 없이 지급 없음)
InsuranceEmployee → BenefitPaymentReview        (assignedStaffId)
```

### 상태 전이
```
SIMPLE 청구:  PENDING → (즉시지급) COMPLETED / FAILED
COMPLEX 청구: PENDING → (담당자배정) IN_REVIEW → APPROVED → COMPLETED / FAILED
                                                  → REJECTED
자동차사고:    PENDING (접수만; 후속 심사/지급은 Epic 4 범위 외)
```

---

## 3. 핵심 비즈니스 규칙 (결정 사항)

1. **복잡도 판별(UC05)**: 청구금액(receiptAmount/requestAmount)이 설정값 `insurance.claim.complex-threshold`(기본 1,000,000원) **이상이면 COMPLEX, 미만이면 SIMPLE**. (경계: 임계값 == COMPLEX)
2. **담당자 자동 배정(UC14)**: 활성 `InsuranceEmployee` 중 **`currentLoad` 최솟값** 직원 선정, 동점 시 employeeId 오름차순. 배정 시 `assignedStaffId` 저장 + `currentLoad++`. (근무상태 필드 부재 → currentLoad 기준만 사용)
3. **받을 계좌(UC17)**: `Policyholder.bankAccount`를 지급 계좌로 사용.
4. **실익분석(UC11)**: 본인 실익만 반환(총납입 보험료·총수령 보험금·실익금액·실익률). "유사 가입자 대비 비교"는 범위에서 제외(YAGNI). 가입 6개월 미만 시 데이터 부족 안내.
5. **자동차사고(UC09)**: 접수 + 접수번호 발급 + 직원 알림 + 가입자 안내까지. 자동차 청구 심사/지급 흐름은 범위 외.
6. **중복 심사 차단(UC12 E2)**: 배정된 `assignedStaffId` 직원만 심사 상세 진입·확정 가능. 타 직원 접근 시 409.

---

## 4. 서비스 · 외부연동 · 에러 규약

### 서비스 (`service/`)
- `ClaimService` — 의료보험 청구(UC05): 계약 유효성·타입(의료) 검증 → 첨부 검증·저장 → 복잡도 판별 → SIMPLE이면 `BenefitPayoutService` 즉시 호출, COMPLEX이면 `StaffAssignmentService` 배정 + IN_REVIEW → 가입자 알림.
- `CarAccidentService` — 자동차사고 접수(UC09): 계약 검증(자동차) → 첨부 검증·저장 → 접수번호 발급 → 직원 알림 + 가입자 접수완료 안내.
- `StaffAssignmentService` — 담당자 배정(UC14): 최소 currentLoad 직원 선정·저장·load++·담당자 알림. 배정 가능 직원 없으면 예외(E1). 수동 배정 진입점 제공.
- `BenefitReviewService` — 심사(UC12): 소유권 검증(E2) → 승인 시 `BenefitPayoutService` 호출, 반려 시 사유 저장 + 통보.
- `BenefitPayoutService` — 보험금 지급(UC17): `Policyholder.bankAccount` 확인 → `BenefitTransferGateway.transfer()` → 성공 시 `BenefitPayment`(SUCCESS) + COMPLETED + 가입자 알림 / 실패 시 FAILED + 직원 알림.
- `ClaimQueryService` — 현황(UC03)·이력(UC04)·실익분석(UC11) 조회.

### 외부연동 / 인프라 (기존 패턴 재사용·신설)
- `BenefitTransferGateway`(interface) + `MockBenefitTransferGateway` — 보험금 송금. `transfer(bankAccount, amount) → Result(success, reason)`. (`PaymentGateway`는 수납 전용이라 분리)
- `NotificationSender`(기존) — 가입자/직원 이메일·문자 알림 전부 재사용.
- `FileStorage`(interface) + `LocalFileStorage` — 멀티파트 → `insurance.upload.dir`(기본 `./uploads/claims`) 아래 `{claimId}/{uuid}_{원본파일명}` 저장.

### 에러 규약 (기존 `GlobalExceptionHandler` 패턴)
| 상황 | 응답 |
|------|------|
| 첨부 형식 오류(UC05/09 E1) | `400` "지원하지 않는 파일 형식입니다. (허용: PDF, JPG, PNG)" |
| 첨부 크기 초과(UC09 E1) | `400` "파일 크기는 개당 10MB 이하여야 합니다." |
| 무효 계약 / 타입 불일치(의료↔자동차) | `400` / `404` |
| 중복 심사 접근(UC12 E2) | `409` "현재 담당자 [ID]가 처리 중인 건입니다." |
| 배정 가능 직원 없음(UC14 E1) | `409` + 관리자 알림 |
| 송금 실패(UC17 E1) | 예외 아님 — FAILED 기록 + 직원 알림(재시도 진입점) |
| 실익분석 데이터 부족(UC11 A1, 6개월 미만) | `400` 또는 안내 응답 |

---

## 5. API 엔드포인트

기존 epic의 REST 규약(역할 기반 인증, `/api/...`)을 따른다.

### 이슈 A — 청구/지급 (가입자)
- `POST /api/claims/health` — 의료보험 청구(UC05). `multipart/form-data`: 청구 메타(contractId, hospitalName, diagnosisCode, treatmentDate, requestAmount/receiptAmount) + `attachments[]`. → claimId, status, complexity, (즉시지급 시) paymentId.

### 이슈 C — 자동차사고 (가입자)
- `POST /api/claims/car-accidents` — 자동차사고 접수(UC09). `multipart/form-data`: 사고 메타 + `attachments[]`. → 접수번호, 담당안내.

### 이슈 B — 심사/배정 (직원)
- `GET  /api/staff/benefit-reviews` — 배정된 심사 대기 목록(UC12 step2). 본인 배정 건만.
- `GET  /api/staff/benefit-reviews/{claimId}` — 심사 상세(UC12 step4). 비배정 직원 → 409(E2).
- `POST /api/staff/benefit-reviews/{claimId}/confirm` — 심사 확정(UC12 step7). body `{result: APPROVED|REJECTED, comment}`. 승인 시 지급 수행.
- `POST /api/staff/claims/{claimId}/assign` — 수동 배정(UC14 A1). body `{employeeId}`. (자동 배정은 청구 생성 시 내부 수행)
- `POST /api/staff/benefit-payments/{paymentId}/retry` — 송금 실패 재시도(UC17 E1).

### 이슈 D — 조회 (가입자)
- `GET /api/claims/status` — 보상 처리 현황 목록(UC03 step2). 진행중 건.
- `GET /api/claims/status/{claimId}` — 단계별 진행 현황 + 예상기간·담당자 연락처(UC03 step4-5).
- `GET /api/claims/history?from=&to=` — 보상 이력(UC04, 기본 최근 1년).
- `GET /api/claims/history/{claimId}` — 이력 상세(UC04 step6).
- `GET /api/contracts/{contractId}/benefit-analysis?from=&to=` — 실익분석(UC11). 총납입·총수령·실익금액·실익률. 6개월 미만 시 안내.

---

## 6. 테스트 전략 (TDD)

실패 테스트 → 최소 구현 순서. 테스트 실행: `cd backend && ./gradlew test`.

### 이슈 A
- 복잡도 판별 경계값(임계값 미만 SIMPLE / 이상 COMPLEX).
- SIMPLE → `BenefitTransferGateway` 호출 → COMPLETED + BenefitPayment(SUCCESS).
- 송금 실패(Mock fail) → FAILED + 직원 알림(예외 아님).
- 첨부 검증: 허용 외 타입 → E1, 10MB 초과 → 크기 예외. `@TempDir` 저장 검증.
- 무효/타입불일치 계약 거부.

### 이슈 B
- 최소부하 배정: 최소 currentLoad 선정, 동점 employeeId, 배정 후 load++.
- 배정 가능 직원 없음 → E1.
- 승인 → 지급 + COMPLETED / 반려 → REJECTED + 사유 저장, 지급 미수행.
- 소유권: 비배정 직원 상세·확정 접근 → 409(E2).
- 수동 배정 경로.

### 이슈 C
- 접수 → PENDING + 접수번호 + 직원 알림 + 가입자 안내.
- 대인사고(hasInjury) → 부상자 정보 저장.

### 이슈 D
- 현황: 진행중 건만, 없으면 빈 목록(A1).
- 이력: 기간 필터, 없으면 빈(A1).
- 실익분석: 총납입/총수령/실익률 계산. 6개월 미만 → 데이터 부족 안내.

---

## 7. 확정된 결정 (grill-with-docs)

CONTEXT.md에 보상&청구 용어(Claim, HealthInsuranceClaim, CarAccidentReport, ClaimStatus, ClaimComplexity, BenefitPaymentReview, BenefitPayment, ClaimAttachment, BenefitTransferGateway) 등재 완료. 아래 결정은 ADR로 확정:

- **ADR 0006** — SIMPLE 청구는 `BenefitPaymentReview` 없이 `BenefitPayment`를 직접 생성(composition 규칙의 명시적 예외, 다이어그램 주석과 일치).
- **ADR 0007** — `ClaimStatus`는 다이어그램 4값에 `COMPLETED`/`FAILED`를 더해 6값(지급 결과·현황 흐름·재시도 표현).
- **ADR 0008** — 보험금 송금은 `PaymentGateway`와 분리한 `BenefitTransferGateway` 포트.

`Claim`은 다이어그램대로 `InsuranceContract` composition을 유지한다(별도 ADR 불필요).
`BenefitPaymentReview`는 기존 `ReviewResult`를 재사용하되 `CONDITIONAL`은 쓰지 않는다.
