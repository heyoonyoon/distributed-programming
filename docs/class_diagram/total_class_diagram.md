# 클래스 다이어그램

## 1. 클래스 관계 요약

### 1-1. Generalization (상속 ▷)
```
User
├── Policyholder
└── InsuranceEmployee

InsuranceProduct
├── HealthInsuranceProduct
└── CarInsuranceProduct

Claim
├── HealthInsuranceClaim
└── CarAccidentReport

Review
├── EnrollmentReview
└── BenefitPaymentReview
```

### 1-2. Composition (강한 포함 ◆) — 주인 삭제 시 포함 객체도 삭제

| 주인 (1) | 포함 (N) | 근거 |
|---------|---------|------|
| InsuranceProduct | CoverageItem | 상품 삭제 시 보장 항목도 의미 없음 |
| InsuranceContract | Payment | 계약 없으면 납부 이력 없음 |
| InsuranceContract | Notice | 계약 단위로 고지서 발행, 계약 삭제 시 고지서도 삭제 |
| InsuranceContract | HealthInsuranceClaim | 유효 계약 없으면 청구 불가 |
| InsuranceContract | CarAccidentReport | 유효 계약 없으면 사고 접수 불가 |
| BenefitPaymentReview | BenefitPayment | 심사 없으면 지급 객체 생성 안 됨 |

### 1-3. Aggregation (약한 포함 ◇) — 생명 주기 분리

| 상위 | 하위 | 근거 |
|------|------|------|
| Policyholder | InsuranceContract | 가입자 탈퇴 시에도 계약 이력은 법적으로 보존 |
| InsuranceProduct | InsuranceContract | 상품 폐지되어도 기존 계약 유지 |
| InsuranceApplication | EnrollmentReview | 신청서 삭제되어도 심사 이력 보존 가능 |

### 1-4. Association (연관 →) — 단방향, 최소화

| A → B | 이유 |
|-------|------|
| InsuranceEmployee → BenefitPaymentReview | 직원이 심사를 담당 |
| EnrollmentReview → AccidentHistory | 자동차보험 심사 시에만 조회 |
| Claim → BenefitPaymentReview | 복잡 의료청구(COMPLEX)·자동차사고가 지급심사로 진입 (ADR 0009) |

> 보험가입자 ↔ 보험상품 직접 연관 없음.
> InsuranceApplication → InsuranceContract 를 통해 간접 연결.

---

## 2. 클래스 상세 정의

### 2-1. User (abstract)
```
[User] - abstract
──────────────────────────────
# userId    : String
# name      : String
# email     : String
# phone     : String
# password  : String
──────────────────────────────
+ login(email: String, password: String) : boolean
+ logout() : void
+ updateContact(email: String, phone: String) : void
```

---

### 2-2. Policyholder (보험가입자) extends User
```
[Policyholder] extends [User]
──────────────────────────────
- ssn         : String
- birthDate   : Date
- address     : String
- bankAccount : String
──────────────────────────────
+ applyInsurance(productId: String) : InsuranceApplication
+ submitHealthClaim(contractId: String, claimInfo: HealthClaimInfo) : HealthInsuranceClaim
+ reportCarAccident(contractId: String, accidentInfo: AccidentInfo) : CarAccidentReport
+ payPremium(contractId: String, method: PaymentMethod) : Payment
+ getProfitAnalysis(contractId: String) : ProfitAnalysisResult
```

---

### 2-3. InsuranceEmployee (보험사 직원) extends User
```
[InsuranceEmployee] extends [User]
──────────────────────────────
- employeeId  : String
- department  : String
- currentLoad : int      // 현재 담당 심사 건 수
──────────────────────────────
+ reviewEnrollment(applicationId: String, result: ReviewResult) : EnrollmentReview
+ reviewBenefitPayment(claimId: String, result: ReviewResult) : BenefitPaymentReview
+ requestDeptConsultation(reviewId: String, reason: String) : void
```

---

### 2-4. InsuranceProduct (abstract)
```
[InsuranceProduct] - abstract
──────────────────────────────
# productId   : String
# productName : String
# description : String
# basePremium : int
──────────────────────────────
+ calculatePremium(holder: Policyholder) : int   // 추상 메서드
+ getProductInfo() : ProductInfo
```

#### CoverageItem (보장항목) — Composition with InsuranceProduct
```
[CoverageItem]
──────────────────────────────
- itemId        : String
- itemName      : String
- coverageLimit : int
- deductible    : int
```

---

### 2-5. HealthInsuranceProduct (의료보험 상품) extends InsuranceProduct
```
[HealthInsuranceProduct] extends [InsuranceProduct]
──────────────────────────────
- maxHospitalizationDays : int   // 입원 최대 보장 일수
──────────────────────────────
+ calculatePremium(holder: Policyholder) : int   // 나이·병력 기반 산출
```

---

### 2-6. CarInsuranceProduct (자동차보험 상품) extends InsuranceProduct
```
[CarInsuranceProduct] extends [InsuranceProduct]
──────────────────────────────
- vehicleType    : String   // 승용, 화물, 이륜 등
- driverScopeType: String   // 운전자 범위 (본인, 가족, 누구나)
──────────────────────────────
+ calculatePremium(holder: Policyholder) : int   // 차량 정보·사고이력 기반 산출
```

---

### 2-7. InsuranceApplication (보험가입 신청)
> 가입 신청 ~ 심사 완료 전 단계 관리. 심사 완료 후 InsuranceContract 로 전환.
```
[InsuranceApplication]
──────────────────────────────
- applicationId  : String
- appliedAt      : Date
- status         : ApplicationStatus   // PENDING, APPROVED, REJECTED, CANCELLED
- vehicleInfo    : VehicleInfo         // 자동차보험 시에만 유효, nullable
- medicalHistory : MedicalHistory      // 의료보험 시에만 유효, nullable
──────────────────────────────
+ getDetail() : ApplicationDetail
+ cancel() : void
```

---

### 2-8. InsuranceContract (보험계약)
> 심사 승인 후 생성. Payment, HealthInsuranceClaim, Notice 와 Composition.
```
[InsuranceContract]
──────────────────────────────
- contractId     : String
- startDate      : Date
- endDate        : Date
- status         : ContractStatus   // ACTIVE, SUSPENDED, TERMINATED
- monthlyPremium : int
──────────────────────────────
+ getContractDetail() : ContractDetail
+ suspend() : void
+ terminate() : void
+ generatePdf() : File
```

---

### 2-9. Payment (보험료 납부) — Composition with InsuranceContract
```
[Payment]
──────────────────────────────
- paymentId : String
- amount    : int
- paidAt    : Date
- method    : PaymentMethod    // CARD, TRANSFER, AUTO_DEBIT
- status    : PaymentStatus    // SUCCESS, FAILED
──────────────────────────────
+ process() : boolean
+ getReceipt() : Receipt
```

---

### 2-10. Notice (미납 고지서) — Composition with InsuranceContract
> Timer 에 의해 자동 생성.
```
[Notice]
──────────────────────────────
- noticeId            : String
- issuedAt            : Date
- dueAmount           : int
- overdueDays         : int
- isTerminationWarning: boolean   // 30일 초과 시 true
──────────────────────────────
+ send(email: String, phone: String) : boolean
```

---

### 2-11. Claim (abstract)
```
[Claim] - abstract
──────────────────────────────
# claimId       : String
# claimDate     : Date
# requestAmount : int
# status        : ClaimStatus   // PENDING, IN_REVIEW, APPROVED, REJECTED, COMPLETED, FAILED (ADR 0007)
──────────────────────────────
+ submit() : void
+ getStatus() : ClaimStatus
```

---

### 2-12. HealthInsuranceClaim (의료보험 청구) extends Claim
> InsuranceContract 와 Composition. 복잡도에 따라 BenefitPaymentReview 와 연관.
```
[HealthInsuranceClaim] extends [Claim]
──────────────────────────────
- hospitalName  : String
- diagnosisCode : String
- treatmentDate : Date
- receiptAmount : int
- complexity    : ClaimComplexity   // SIMPLE, COMPLEX
──────────────────────────────
+ attachDocument(file: File) : void
+ isSimpleClaim() : boolean
```

---

### 2-13. CarAccidentReport (자동차 사고 접수) extends Claim
> InsuranceContract 와 Composition.
```
[CarAccidentReport] extends [Claim]
──────────────────────────────
- accidentDate     : Date
- accidentLocation : String
- accidentType     : String   // 단독, 쌍방, 대인 등
- vehicleNumber    : String
- hasInjury        : boolean
- injuredCount     : int
──────────────────────────────
+ attachPhoto(file: File) : void
```

---

### 2-14. Review (abstract)
```
[Review] - abstract
──────────────────────────────
# reviewId   : String
# reviewedAt : Date
# result     : ReviewResult   // APPROVED, CONDITIONAL, REJECTED
# comment    : String
──────────────────────────────
+ confirm(result: ReviewResult, comment: String) : void
+ getResult() : ReviewResult
```

---


### 2-15. EnrollmentReview (보험 가입 심사) extends Review
> InsuranceApplication 심사 결과. 자동차보험인 경우 AccidentHistory 참조.
```
[EnrollmentReview] extends [Review]
──────────────────────────────
- surchargeRate   : double          // 조건부 승인 시 할증율 (0.0이면 미적용)
- adjustedPremium : int             // 할증 적용 후 최종 보험료
- accidentHistory : AccidentHistory // 자동차보험 심사 시에만 유효, nullable
──────────────────────────────
+ applySurcharge(rate: double) : int
+ fetchAccidentHistory(ssn: String) : AccidentHistory  // 자동차보험에 한해 호출
```
 
---
 
### 2-16. BenefitPaymentReview (보험금 지급 심사) extends Review
> 보험금 지급 심사. 대상은 Claim(추상) — 복잡 의료청구(COMPLEX)와 자동차사고 둘 다. (ADR 0009)
> 반드시 InsuranceEmployee 수동 배정. 승인 시 BenefitPayment 를 Composition 으로 포함.
```
[BenefitPaymentReview] extends [Review]
──────────────────────────────
- assignedStaffId : String   // InsuranceEmployee 의 employeeId 참조
──────────────────────────────
+ assignStaff(employee: InsuranceEmployee) : void
+ approve() : BenefitPayment
+ reject(reason: String) : void
```
 
---
 
### 2-17. BenefitPayment (보험금 지급)
> BenefitPaymentReview 와 Composition — 심사 없이 지급 없음.
> 단, 간단한 청구(SIMPLE)의 경우 BenefitPaymentReview 없이 HealthInsuranceClaim 에서 직접 생성.
```
[BenefitPayment]
──────────────────────────────
- paymentId   : String
- paidAmount  : int
- paidAt      : Date
- bankAccount : String
- status      : PaymentStatus   // SUCCESS, FAILED
──────────────────────────────
+ transfer() : boolean
+ notifyPolicyholder(email: String, phone: String) : void
```
 
---
 
### 2-18. AccidentHistory (사고 이력 — 금융감독원 연동)
> 금융감독원 API 로부터 조회되는 외부 데이터 객체. 더미로 시뮬레이션.
> 구현상 EnrollmentReview 에 내장(@Embeddable)되는 요약 값 객체로 단순화 — 개별 사고
> 기록은 집계값(accidentCount, totalPaidAmount)으로 갈음하며 AccidentRecord 는 두지 않는다.
```
[AccidentHistory]
──────────────────────────────
- accidentCount   : int
- totalPaidAmount : int
- licenseStatus   : String   // VALID, SUSPENDED, REVOKED
- fetchedAt       : Date
──────────────────────────────
+ fetch(ssn: String) : AccidentHistory   // static, 금융감독원 API 호출 → 더미 반환
+ getSummary() : AccidentSummary
```
 