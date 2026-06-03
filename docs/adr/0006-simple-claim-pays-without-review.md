# 0006. SIMPLE 청구는 BenefitPaymentReview 없이 BenefitPayment를 직접 생성한다

클래스 다이어그램(06_payment)은 "BenefitPayment ◆── BenefitPaymentReview (composition) — 심사 없이 지급 없음"으로 정의한다. 그러나 SIMPLE로 판별된 의료보험 청구(UC05 → UC17 즉시지급)는 `BenefitPaymentReview`를 만들지 않고 `HealthInsuranceClaim`이 곧바로 `BenefitPayment`를 생성한다.

즉 BenefitPayment는 두 경로로 생성된다:
- **SIMPLE**: Claim → BenefitPayment (review 없음, `reviewId` 없이 claim 참조)
- **COMPLEX**: Claim → BenefitPaymentReview(승인) → BenefitPayment

다이어그램 주석도 이 예외를 이미 명시한다("단, 간단한 청구(SIMPLE)는 BenefitPaymentReview 없이 HealthInsuranceClaim에서 직접 생성").

## Considered Options

- **채택(SIMPLE 직접 지급)**: UC05 베이직 플로우("간단한 청구는 즉시 지급")를 그대로 표현. 직원 개입 없는 자동 흐름이 자연스럽다. 단점: BenefitPayment의 소유자가 경로에 따라 달라져(Claim 또는 Review) 모델이 비대칭.
- **모든 청구에 BenefitPaymentReview 생성**: composition 규칙을 무조건 지켜 대칭 유지. SIMPLE도 "자동 승인된 review"를 만든다. 단점: 심사하지 않은 건에 심사 엔티티가 생겨 의미가 흐려지고, "담당자 없는 review"라는 또 다른 예외가 생긴다.

되돌리기 어려움: 지급 내역(BenefitPayment)의 출처·집계가 이 분기에 의존한다. 이후 SIMPLE에도 review를 강제하려면 데이터 마이그레이션이 필요하다.
