# 심사 클래스

### Review (abstract)
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

### EnrollmentReview extends Review
> InsuranceApplication 심사. 자동차보험인 경우 AccidentHistory 참조.
```
[EnrollmentReview] extends [Review]
──────────────────────────────
- surchargeRate   : double
- adjustedPremium : int
- accidentHistory : AccidentHistory   // 자동차보험만, nullable
──────────────────────────────
+ applySurcharge(rate: double) : int
+ fetchAccidentHistory(ssn: String) : AccidentHistory   // 자동차보험만 호출
```

### BenefitPaymentReview extends Review
> 보험금 지급 심사. 대상은 Claim(추상) — 복잡한 의료보험 청구(COMPLEX)와 자동차사고 둘 다. (ADR 0009)
> InsuranceEmployee 배정 필수(수동 배정). 승인 시 BenefitPayment를 Composition으로 포함.
```
[BenefitPaymentReview] extends [Review]
──────────────────────────────
- assignedStaffId : String   // InsuranceEmployee.employeeId 참조
──────────────────────────────
+ assignStaff(employee: InsuranceEmployee) : void
+ approve() : BenefitPayment
+ reject(reason: String) : void
```