# 0005. InsuranceContract는 심사 승인 시 같은 트랜잭션에서 자동 생성한다

`ReviewService.confirm()`에서 심사 결과가 APPROVED 또는 CONDITIONAL이면, 같은 트랜잭션 안에서 `InsuranceContract`를 즉시 생성한다. REJECTED는 계약을 만들지 않는다.

- `monthlyPremium = EnrollmentReview.adjustedPremium` (ADR 0003: 결과 분기 없이 한 필드만 읽음).
- `startDate = 승인일`, `endDate = startDate + 1년`(기본 기간, 고정값), `status = ACTIVE`.
- Epic 2의 `confirm()` 코드를 수정한다(승인 분기에 계약 생성 추가).

## Considered Options

- **채택(승인 시 자동 생성)**: "승인=계약 성립"이 자연스럽고 직원의 추가 단계가 없다. 승인됐는데 계약이 없는 중간 상태가 존재하지 않는다. 단점: 심사와 계약 책임이 한 트랜잭션에 묶이고 Epic 2 코드를 건드린다.
- **별도 진입점(ContractService.createFromApproved)**: 심사와 계약 생성을 분리, Epic 2 불변. 단점: 승인→계약 자동성이 약해지고 "승인됐으나 계약 미생성" 상태가 생겨 일관성 관리가 필요.

되돌리기 어려움: 계약 식별자·생성 시점이 이 흐름에 묶이므로, 이후 승인과 계약을 분리하려면 흐름과 데이터 의미를 바꿔야 한다.
