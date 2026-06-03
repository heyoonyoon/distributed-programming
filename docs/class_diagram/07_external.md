# 외부 연동 클래스 (금융감독원)

### AccidentHistory (사고 이력)
> 금융감독원 API로부터 조회되는 외부 데이터 객체.
> 텍스트 기반 구현 시: 더미 데이터로 시뮬레이션한다.
```
[AccidentHistory]
──────────────────────────────
- ssn             : String
- accidentCount   : int
- totalPaidAmount : int
- licenseStatus   : String   // VALID, SUSPENDED, REVOKED
- fetchedAt       : Date
──────────────────────────────
+ fetch(ssn: String) : AccidentHistory   // static, 실제론 API 호출 → 텍스트 구현 시 더미 반환
+ getSummary() : AccidentSummary
```

### AccidentRecord — Composition with AccidentHistory
```
[AccidentRecord]
──────────────────────────────
- accidentDate : Date
- accidentType : String
- paidAmount   : int
```