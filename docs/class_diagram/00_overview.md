# 클래스 다이어그램 개요

> 이 파일만 CLAUDE.md에서 항상 로드한다.
> 상세 정의는 UC 구현 시 필요한 파일만 읽는다.

## 클래스 목록

| 파일 | 클래스 | 설명 |
|------|--------|------|
| 01_user.md | User, Policyholder, InsuranceEmployee | 사용자 계층 |
| 02_product.md | InsuranceProduct, HealthInsuranceProduct, CarInsuranceProduct, CoverageItem | 보험 상품 계층 |
| 03_contract.md | InsuranceApplication, InsuranceContract, Payment, Notice | 계약 흐름 |
| 04_claim.md | Claim, HealthInsuranceClaim, CarAccidentReport | 청구/사고 계층 |
| 05_review.md | Review, EnrollmentReview, BenefitPaymentReview | 심사 계층 |
| 06_payment.md | BenefitPayment | 보험금 지급 |
| 07_external.md | AccidentHistory, AccidentRecord | 금융감독원 연동 |

## 상속 관계

```
User (abstract)
├── Policyholder
└── InsuranceEmployee

InsuranceProduct (abstract)
├── HealthInsuranceProduct
└── CarInsuranceProduct

Claim (abstract)
├── HealthInsuranceClaim
└── CarAccidentReport

Review (abstract)
├── EnrollmentReview
└── BenefitPaymentReview
```

## 주요 관계

```
Policyholder ◇── InsuranceContract       (aggregation: 가입자 탈퇴해도 계약 보존)
InsuranceProduct ◇── InsuranceContract   (aggregation: 상품 폐지해도 계약 유지)
InsuranceApplication ◇── EnrollmentReview (aggregation: 신청서 삭제해도 심사 이력 보존)

InsuranceProduct ◆── CoverageItem        (composition: 상품 삭제 시 보장항목도 삭제)
InsuranceContract ◆── Payment            (composition)
InsuranceContract ◆── Notice             (composition)
InsuranceContract ◆── HealthInsuranceClaim (composition)
InsuranceContract ◆── CarAccidentReport  (composition)
BenefitPaymentReview ◆── BenefitPayment  (composition)
AccidentHistory ◆── AccidentRecord       (composition)

InsuranceEmployee → BenefitPaymentReview (association: 직원이 심사 담당)
EnrollmentReview → AccidentHistory       (association: 자동차보험 심사 시 조회)
HealthInsuranceClaim → BenefitPaymentReview (association: 복잡한 청구 → 심사 요청)
```

## UC별 필요 클래스 매핑 (추정임, 필요시 추가하거나 빼도 됨)

| UC | 필요 파일 |
|----|-----------|
| UC01 보험 상품 조회 | 02_product.md |
| UC02 보험 가입 요청 | 01_user.md, 02_product.md, 03_contract.md |
| UC03 보상 처리 현황 | 03_contract.md, 04_claim.md, 05_review.md |
| UC04 보상 이력 조회 | 04_claim.md, 06_payment.md |
| UC05 의료보험 청구 | 01_user.md, 03_contract.md, 04_claim.md, 05_review.md, 06_payment.md |
| UC06 개인정보 수정 | 01_user.md |
| UC07 미납 내역 조회 | 03_contract.md |
| UC08 계약 내용 조회 | 03_contract.md |
| UC09 자동차사고 접수 | 03_contract.md, 04_claim.md |
| UC10 보험료 납부 | 03_contract.md |
| UC11 보험 처리 실익 분석 | 03_contract.md, 06_payment.md |
| UC12 보험금 지급 심사 | 01_user.md, 04_claim.md, 05_review.md, 06_payment.md |
| UC13 보험 가입 심사 | 01_user.md, 03_contract.md, 05_review.md, 07_external.md |
| UC14 담당자 지정 | 01_user.md, 05_review.md |
| UC15 자동차 사고 이력 조회 | 07_external.md |
| UC16 미납 고지서 발송 | 03_contract.md |
| UC17 보험금 지급 | 06_payment.md |