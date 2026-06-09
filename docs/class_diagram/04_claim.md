# 청구 / 사고 클래스

### Claim (abstract)
```
[Claim] - abstract
──────────────────────────────
# claimId       : String
# claimDate     : Date
# requestAmount : int
# status        : ClaimStatus   // PENDING, IN_REVIEW, APPROVED, REJECTED, COMPLETED, FAILED
──────────────────────────────
+ submit() : void
+ getStatus() : ClaimStatus
```
> ClaimStatus는 지급 결과(COMPLETED 송금완료 / FAILED 송금실패)까지 포함한다. (ADR 0007)
> HealthInsuranceClaim·CarAccidentReport 둘 다 BenefitPaymentReview로 진입한다. (ADR 0009)

### HealthInsuranceClaim extends Claim
> InsuranceContract와 Composition. 복잡도에 따라 BenefitPaymentReview와 연관.
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

### CarAccidentReport extends Claim
> InsuranceContract와 Composition.
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