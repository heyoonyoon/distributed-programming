# Epic 2 · 가입 & 심사 — 설계 문서

- **에픽**: Epic 2 · 가입 & 심사 (GitHub 마일스톤 #3)
- **유스케이스**: UC02(보험 가입을 요청한다), UC13(보험 가입을 심사한다), UC15(자동차 사고 이력을 조회한다)
- **작성일**: 2026-06-03
- **상태**: 승인됨

## 1. 범위와 경계

Epic 2는 가입 신청 접수부터 가입 심사 결과 확정까지를 다룬다. 다음 결정으로 범위를 고정한다.

- **이슈 분할**: 2개 이슈로 분리한다.
  - **이슈 A — UC02 가입 요청** (액터: Policyholder)
  - **이슈 B — UC13 가입 심사 + UC15 사고이력 조회** (액터: InsuranceEmployee)
  - spec은 에픽 1개, plan은 이슈별 2개로 작성한다.
- **Epic 3와의 경계**: 심사 '승인' 시 `InsuranceContract`를 **생성하지 않는다.** Epic 2는 `EnrollmentReview` 저장 + `InsuranceApplication.status` 전이까지만 책임진다. 실제 계약 생성·수납은 Epic 3 소관.
- **포함 흐름**: A1 조건부승인(할증), UC15 사고이력 조회(mock), E3 중복 심사 방지(상태 전이 가드 형태).
- **제외 흐름(YAGNI)**: A2 부서협의(협의중 상태/타부서 알림). 별도 lock UI도 도입하지 않는다.
- **알림 발송**: 실제 이메일/문자 대신 Epic 0의 `MockIdentityVerification` 패턴을 따른 로그 기반 mock `NotificationSender`로 처리한다.

## 2. 용어 (CONTEXT.md 준수)

- **Policyholder**: 보험 가입 고객. UC02의 액터. (account/user/가입자 등 동의어 금지)
- **InsuranceEmployee**: 보험사 직원. UC13의 액터. 심사 건에 배정되면 "담당자" 역할.
- **InsuranceProduct / HealthInsuranceProduct / CarInsuranceProduct**: 기존 product 도메인 그대로 참조.
- 신규 용어 후보(grill-with-docs에서 확정): `InsuranceApplication`, `EnrollmentReview`, `ApplicationStatus`, `ReviewResult`, `AccidentHistory`.

## 3. 도메인 모델

### 3.1 InsuranceApplication (이슈 A)

JPA single-table 상속이 필요 없는 단일 엔티티. 기존 product 도메인의 매핑/Lombok 관례를 따른다.

```
InsuranceApplication
- id            : Long (IDENTITY)
- appliedAt     : LocalDateTime
- status        : ApplicationStatus   // PENDING, APPROVED, REJECTED, CANCELLED
- product       : ManyToOne → InsuranceProduct
- applicant     : ManyToOne → Policyholder
- vehicleInfo   : VehicleInfo (@Embeddable, nullable — CarInsuranceProduct만)
- medicalHistory: MedicalHistory (@Embeddable, nullable — HealthInsuranceProduct만)
```

- **개인정보 비복제(ADR 0002)**: 이름·생년월일·주민번호·연락처·이메일은 Policyholder가 소유한다. Application은 `applicant`만 참조하고, 심사 화면·사고이력 조회(ssn)는 `applicant`에서 읽는다. Application 고유 데이터는 종류별 추가정보뿐이다.
- **조건부승인 비표현(ADR 0003)**: ApplicationStatus에 CONDITIONAL을 두지 않는다. 조건부도 APPROVED이며, 조건부 여부는 EnrollmentReview가 소유한다.
- `VehicleInfo`(@Embeddable): 차량번호, 차종, 연식, 운전경력.
- `MedicalHistory`(@Embeddable): 현재 병력, 과거 입원 이력, 복용 중인 약물.
- **종류 정합성 불변식**:
  - 상품이 `CarInsuranceProduct`이면 vehicleInfo 필수, medicalHistory 금지.
  - 상품이 `HealthInsuranceProduct`이면 medicalHistory 필수, vehicleInfo 금지.
  - 위반 시 도메인 예외 → 400.
- **상태 전이**:
  - 생성 시 `PENDING`.
  - `cancel()`: PENDING → CANCELLED. 그 외 상태에서 호출 시 예외 → 409.
  - 심사 확정에 의해서만 PENDING → APPROVED/CONDITIONAL/REJECTED. 비PENDING에서 재전이 거부 → 409 (E3 상태 가드).

### 3.2 Review / EnrollmentReview (이슈 B)

`Review` 추상 부모를 도입하고 single-table 상속으로 매핑(향후 Epic 4 `BenefitPaymentReview` 확장 대비). Epic 2에서는 `EnrollmentReview`만 구현한다.

```
Review (abstract, SINGLE_TABLE, @DiscriminatorColumn)
# id         : Long (IDENTITY)
# reviewedAt : LocalDateTime
# result     : ReviewResult   // APPROVED, CONDITIONAL, REJECTED
# comment    : String

EnrollmentReview extends Review   @DiscriminatorValue("ENROLLMENT")
- surchargeRate   : double               // 조건부승인 시에만 > 0
- adjustedPremium : int                  // 조건부승인 시 재계산된 보험료
- accidentHistory : AccidentHistory (@Embeddable, nullable — 자동차건만)
- application     : OneToOne → InsuranceApplication
- reviewer        : ManyToOne → InsuranceEmployee
```

- `applySurcharge(rate)`: `adjustedPremium = round(basePremium * (1 + rate))`.
- **adjustedPremium 단일 출처(ADR 0003)**: 결과와 무관하게 항상 최종 월 보험료를 담는다. APPROVED면 basePremium 그대로, CONDITIONAL이면 할증 적용액. REJECTED는 의미 없음(0/미사용). Epic 3는 이 필드만 읽는다.
- **할증 규칙**:
  - `result=CONDITIONAL`이면 surchargeRate > 0 필수, adjustedPremium = 할증 적용액.
  - `result=APPROVED`이면 surchargeRate 입력 금지(있으면 400), adjustedPremium = basePremium.
  - `result=REJECTED`이면 surchargeRate 입력 금지.
- 확정 시 `application`의 status를 동기 전이시킨다(CONDITIONAL→APPROVED, REJECTED→REJECTED).

### 3.3 AccidentHistory + AccidentHistoryClient (이슈 B, UC15)

외부 금융감독원 연동을 mock으로 시뮬레이션.

```
AccidentHistory (@Embeddable)
- accidentCount   : int
- totalPaidAmount : int
- licenseStatus   : String   // VALID, SUSPENDED, REVOKED
- fetchedAt       : LocalDateTime
```

- `AccidentHistoryClient` 인터페이스 + `MockAccidentHistoryClient` 구현: ssn 기반 더미 반환(Epic 0 `IdentityVerificationService`/`MockIdentityVerification` 패턴 동일).
- 자동차보험 심사 상세 조회 시에만 호출되어 '참조 정보'로 동봉.
- 연동 실패(E1)는 Epic 2 mock에서는 정상 더미 반환으로 단순화(재시도/직접입력 전환 UI는 범위 외).

## 4. API 엔드포인트

기존 관례: prefix `/api`, JWT 인증, 경로별 `hasRole`, `GlobalExceptionHandler` 공통 처리.

### 4.1 이슈 A — UC02 (ROLE: POLICYHOLDER)

- `POST /api/applications` — 가입 신청 생성
  - body: `productId`, `vehicleInfo` | `medicalHistory` (개인정보는 인증 주체 Policyholder에서 읽음 — ADR 0002)
  - 201 → `{ applicationId, status: "PENDING", appliedAt }`
  - mock 알림(접수번호, 예상 처리기간) 발송(로그)
- `GET /api/applications/me` — 내 신청 목록/상태 조회
- `POST /api/applications/{id}/cancel` — 본인 PENDING 건 취소 → CANCELLED

### 4.2 이슈 B — UC13 (ROLE: EMPLOYEE)

- `GET /api/reviews/pending` — 심사 대기 목록(접수일시, 가입자명, 보험 종류, 상품명, 보험료)
- `GET /api/reviews/applications/{id}` — 심사 상세(개인정보 + 종류별 추가정보 + 신청 보장 내용). 자동차건이면 mock 사고이력 참조 정보 동봉.
- `POST /api/reviews/applications/{id}/confirm` — 심사 확정
  - body: `{ result: APPROVED|CONDITIONAL|REJECTED, comment, surchargeRate? }`
  - EnrollmentReview 저장 + Application.status 동기 전이 + (CONDITIONAL 시) adjustedPremium 저장 + mock 결과 통보 발송

## 5. 검증 · 에러 규약

GlobalExceptionHandler(Epic 1) 확장.

| 상태 | 조건 |
|------|------|
| 400 | Bean Validation 실패(필수 누락·형식), 종류-추가정보 불일치, CONDITIONAL surchargeRate 규칙 위반, 역전/음수 값 |
| 403 | 타인 신청 조회/취소 시도 |
| 404 | 상품·신청 건 없음 |
| 409 | 비PENDING 건 취소, 비PENDING 건 재심사(상태 전이 위반 → `IllegalStateTransitionException`) |

- **500 오매핑 방지**: 알 수 없는 enum/상품 종류는 명시적으로 처리(Epic 1 교훈 반영).
- **검증 위치**:
  - 형식 검증(productId 필수, vehicleInfo/medicalHistory 필드 형식·필수) → DTO Bean Validation(`@Valid`). 개인정보는 body로 받지 않으므로 검증 대상 아님(ADR 0002).
  - 도메인 불변식(종류 정합성, 상태 전이, 할증 규칙) → 엔티티/도메인 서비스 내부에서 throw.

## 6. 테스트 전략 (TDD 철칙)

실패하는 테스트 없이 프로덕션 코드 금지. Red → Verify Red → Green → Verify Green → Refactor → 커밋.

1. **도메인 단위테스트**
   - `ApplicationStatus` 전이: PENDING→APPROVED/CONDITIONAL/REJECTED/CANCELLED, 비PENDING 재전이 거부.
   - 종류-추가정보 정합성(Car↔vehicleInfo, Health↔medicalHistory).
   - `EnrollmentReview.applySurcharge` 계산, CONDITIONAL surchargeRate 규칙.
2. **AccidentHistoryClient mock**: ssn별 더미 반환·요약.
3. **Repository(@DataJpaTest)**: Review 상속 매핑, @Embeddable 영속화, pending 목록 쿼리.
4. **Controller/통합(@WebMvcTest / @SpringBootTest)**: 권한(POLICYHOLDER vs EMPLOYEE), 201/400/403/404/409 경로, 자동차건 사고이력 동봉.
5. 보험 도메인 규칙은 클래스 다이어그램 관계를 테스트로 먼저 박는다.

## 7. 후속 단계

1. **grill-with-docs**로 이 spec을 도메인 모델에 대고 취조 → CONTEXT.md 용어 확정, 필요 시 ADR 기록.
2. 취조 결정 반영 후 **writing-plans**로 이슈별 plan 2개 작성(CONTEXT.md 용어 + docs/adr 결정 준수 명시).
3. **executing-plans**(subagent-driven)로 이슈 A → 이슈 B 순서 구현.
