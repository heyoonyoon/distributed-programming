# 사용자 클래스

### User (abstract)
```
[User] - abstract
──────────────────────────────
# userId   : String
# name     : String
# email    : String
# phone    : String
# password : String
──────────────────────────────
+ login(email: String, password: String) : boolean
+ logout() : void
+ updateContact(email: String, phone: String) : void
```

### Policyholder extends User
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

### InsuranceEmployee extends User
```
[InsuranceEmployee] extends [User]
──────────────────────────────
- employeeId  : String
- department  : String
- currentLoad : int
──────────────────────────────
+ reviewEnrollment(applicationId: String, result: ReviewResult) : EnrollmentReview
+ reviewBenefitPayment(claimId: String, result: ReviewResult) : BenefitPaymentReview
+ requestDeptConsultation(reviewId: String, reason: String) : void
```