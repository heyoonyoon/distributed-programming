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
