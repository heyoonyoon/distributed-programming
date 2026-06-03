# 보험(Insurance) 용어집

이 프로젝트의 단일 용어 출처(glossary). 코드·문서 네이밍은 여기 표준어를 그대로 따른다.
`_Avoid_`에 적힌 동의어는 쓰지 않는다. 구현 디테일·스펙은 적지 않는다(용어만).

## Language

### 사용자 (User)

**User**:
로그인하는 모든 사용자의 추상 부모. Policyholder와 InsuranceEmployee의 공통 상위 개념.

**Policyholder**:
보험에 가입한 고객. 시스템에 로그인해 가입·청구·납부 등을 수행하는 계약 당사자.
_Avoid_: 보험가입자, 가입자, 고객, customer, account, user, member

**InsuranceEmployee**:
보험사 직원. 가입·보험금 지급 심사를 담당하며, 특정 심사 건에 배정되면 "담당자" 역할을 한다.
_Avoid_: 직원, 담당자, staff, admin, agent

### 보험 상품 (Product)

**InsuranceProduct**:
가입 가능한 보험 상품의 추상 부모. HealthInsuranceProduct와 CarInsuranceProduct의 공통 상위 개념.
_Avoid_: 보험상품, product, 상품(단독), item

**HealthInsuranceProduct**:
의료보험 상품.
_Avoid_: 의료보험, 건강보험, medical, health(단독)

**CarInsuranceProduct**:
자동차보험 상품.
_Avoid_: 자동차보험, 차보험, auto, vehicle insurance

**CoverageItem**:
한 상품이 보장하는 개별 보장 항목(보장 한도·면책 포함). 보험 실무의 "담보"와 같은 개념.
_Avoid_: 보장항목, 담보, coverage, benefit

### 가입 & 심사 (Enrollment & Underwriting)

**InsuranceApplication**:
Policyholder가 특정 상품에 대해 제출한 가입 신청. 심사 완료 전까지의 단계이며, 승인 후 InsuranceContract로 이어진다.
_Avoid_: 가입신청, 신청서, application(단독), enrollment

**ApplicationStatus**:
가입 신청의 진행 상태. PENDING(심사 대기), APPROVED(승인), REJECTED(반려), CANCELLED(취소). 조건부 여부는 여기 두지 않고 ReviewResult가 가진다.
_Avoid_: 신청상태, status(단독)

**EnrollmentReview**:
InsuranceApplication에 대한 가입 심사. Review 추상 부모를 상속하며, 자동차보험 건은 AccidentHistory를 참조한다. 확정한 InsuranceEmployee가 담당자(reviewer)로 기록된다.
_Avoid_: 가입심사, 심사(단독), underwriting, review(단독)

**ReviewResult**:
심사 결과. APPROVED(승인), CONDITIONAL(조건부 승인 — 할증 부과), REJECTED(반려).
_Avoid_: 심사결과, result(단독)

**AccidentHistory**:
금융감독원(외부 기관)에서 조회되는 자동차 사고 이력(사고 건수·지급 총액·면허 상태). 텍스트 구현에서는 더미로 시뮬레이션한다.
_Avoid_: 사고이력, accident record(단독)

### 계약 & 수납 (Contract & Billing)

**InsuranceContract**:
가입 심사 승인 후 성립한 보험 계약. 월 보험료·계약 기간·상태를 가지며, Payment·Notice를 소유한다(composition).
_Avoid_: 계약, 보험계약, contract(단독), policy

**Payment**:
보험료 납부 1건의 기록(금액·납부수단·성공여부). 어느 회차에 대응하는지는 저장하지 않는다(FIFO 충당).
_Avoid_: 납부, 결제, 수납, billing

**Notice**:
미납 발생 시 시스템이 자동 생성·발송하는 미납 고지서. 연체 30일 초과 시 해지예고를 포함한다.
_Avoid_: 고지서, 독촉장, notice(단독), reminder

**Installment**:
계약 시작일부터 매월 발생하는 보험료 청구 회차. 별도 엔티티 없이 계약 정보로 계산해 도출하는 개념상의 단위(온더플라이).
_Avoid_: 회차, 청구분, billing cycle, invoice

**OverdueInterest**:
미납 원금에 연체일수와 일이율을 곱해 산출하는 연체이자.
_Avoid_: 연체이자, 연체료, late fee, penalty

**PaymentMethod**:
보험료 결제 수단. CARD(신용카드), TRANSFER(계좌이체), AUTO_DEBIT(자동이체).
_Avoid_: 결제수단, 납입방법, 결제방식
