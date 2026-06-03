# 0007. ClaimStatus는 다이어그램의 4값에 지급 결과(COMPLETED/FAILED)를 더한다

클래스 다이어그램(04_claim)의 `ClaimStatus`는 `PENDING, IN_REVIEW, APPROVED, REJECTED` 4값이다. Epic 4는 여기에 `COMPLETED`(지급완료)와 `FAILED`(지급실패)를 추가해 6값으로 둔다.

이유: UC17(보험금 지급)은 송금 성공 시 "처리 완료", 실패 시 "지급 실패" 상태를 명시적으로 요구한다(UC17 후행조건·E1). APPROVED는 "심사 통과"이지 "지급 끝"이 아니므로, 송금 결과를 별도 상태로 표현하지 않으면 UC03(보상 처리 현황: 접수→배정→심사→완료) 흐름도와 UC17 재시도(FAILED→재송금)를 표현할 수 없다.

상태 전이:
```
SIMPLE 청구:  PENDING → COMPLETED / FAILED
COMPLEX 청구: PENDING → IN_REVIEW → APPROVED → COMPLETED / FAILED
                                   → REJECTED
자동차사고:    PENDING (접수만)
```

## Considered Options

- **채택(COMPLETED/FAILED 추가)**: 지급 결과가 상태로 드러나 현황·이력·재시도를 모두 표현. ADR 0003이 ApplicationStatus에 CANCELLED를 더한 선례와 동일하게 다이어그램을 책임 기준으로 보강.
- **APPROVED를 종착 상태로 사용**: 다이어그램 4값 유지. 지급 성공·실패는 BenefitPayment.status로만 표현. 단점: Claim만 보면 지급 여부를 알 수 없어 현황 화면이 BenefitPayment를 항상 조인해야 하고, 송금 실패 재시도 대상을 Claim 상태로 거를 수 없다.

되돌리기 어려움: 현황/이력 조회와 재시도 로직이 이 상태값에 의존하므로, 이후 값을 줄이면 질의·전이 로직을 다시 설계해야 한다.
