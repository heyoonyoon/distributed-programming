# 계약 흐름 클래스

### InsuranceApplication (보험 가입 신청)
> 가입 신청 ~ 심사 완료 전 단계. 심사 승인 후 InsuranceContract로 전환.
```
[InsuranceApplication]
──────────────────────────────
- applicationId  : String
- appliedAt      : Date
- status         : ApplicationStatus   // PENDING, APPROVED, REJECTED, CANCELLED
- vehicleInfo    : VehicleInfo         // 자동차보험만, nullable
- medicalHistory : MedicalHistory      // 의료보험만, nullable
──────────────────────────────
+ getDetail() : ApplicationDetail
+ cancel() : void
```

### InsuranceContract (보험 계약)
> 심사 승인 후 생성. Payment, Notice, Claim과 Composition.
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

### Payment (보험료 납부) — Composition with InsuranceContract
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

### Notice (미납 고지서) — Composition with InsuranceContract
> Timer에 의해 자동 생성.
```
[Notice]
──────────────────────────────
- noticeId             : String
- issuedAt             : Date
- dueAmount            : int
- overdueDays          : int
- isTerminationWarning : boolean   // 30일 초과 시 true
──────────────────────────────
+ send(email: String, phone: String) : boolean
```