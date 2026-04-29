# 보험금 지급 클래스

### BenefitPayment (보험금 지급)
> BenefitPaymentReview와 Composition — 심사 없이 지급 없음.
> 단, 간단한 청구(SIMPLE)는 BenefitPaymentReview 없이 HealthInsuranceClaim에서 직접 생성.
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