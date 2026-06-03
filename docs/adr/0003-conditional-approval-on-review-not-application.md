# 조건부승인은 ReviewResult에만 두고, adjustedPremium은 항상 최종 보험료를 담는다

`ApplicationStatus`는 `PENDING, APPROVED, REJECTED, CANCELLED` 4값만 가진다. 조건부승인(할증)은 별도 상태로 두지 않고 `ReviewResult.CONDITIONAL`로만 표현한다. 즉 조건부승인된 신청도 `ApplicationStatus`는 `APPROVED`이며, 조건부 여부·할증율·조정 보험료는 `EnrollmentReview`가 소유한다.

또한 `EnrollmentReview.adjustedPremium`은 심사 결과와 무관하게 **항상 최종 월 보험료**를 담는다(일반 승인이면 상품 basePremium, 조건부면 할증 적용액). Epic 3(계약 생성)는 분기 없이 이 한 필드만 읽어 `InsuranceContract.monthlyPremium`을 만든다.

## Considered Options

- **채택**: 신청 상태는 "통과 여부"만, 심사 디테일은 Review가 소유. 책임 분리가 명확하고 Epic 3가 단일 출처를 본다.
- **ApplicationStatus에 CONDITIONAL 추가**: 가입자가 자기 상태에서 조건부임을 바로 봄. 단점: 상태와 심사 결과가 중복 표현되고, adjustedPremium을 조건부일 때만 채우면 Epic 3가 분기해야 함.

클래스 다이어그램(03_contract)은 `ApplicationStatus { PENDING, APPROVED, REJECTED }`로 정의했으나, cancel() 메서드 근거로 CANCELLED를 추가하고 CONDITIONAL은 의도적으로 제외했다.
