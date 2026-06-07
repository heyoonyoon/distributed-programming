# 외부 연동 클래스 (금융감독원)

### AccidentHistory (사고 이력)
> 금융감독원 API로부터 조회되는 외부 데이터 객체. 더미 데이터로 시뮬레이션한다.
> 구현상 EnrollmentReview에 내장(@Embeddable)되는 요약 값 객체로 단순화 — 개별 사고
> 기록(AccidentRecord)은 집계값(accidentCount, totalPaidAmount)으로 갈음하므로 별도 보유하지 않는다.
```
[AccidentHistory]
──────────────────────────────
- accidentCount   : int
- totalPaidAmount : int
- licenseStatus   : String   // VALID, SUSPENDED, REVOKED
- fetchedAt       : Date
──────────────────────────────
+ fetch(ssn: String) : AccidentHistory   // static, 실제론 API 호출 → 더미 반환
+ getSummary() : AccidentSummary
```