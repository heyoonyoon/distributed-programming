# 보험 시스템 (Insurance System)

> 의료보험·자동차보험의 가입부터 청구·심사·지급까지를 다루는 보험 도메인 시스템.
> Spring Boot 백엔드 + React 프론트엔드 **모노레포**로 구성하며, AI 기반 개발 방법론(DDD + Superpowers + grill-with-docs)을 적용한다.

---

## 1. 프로젝트 소개

보험 상품 조회부터 가입 신청, 심사, 보험금 청구, 지급까지 **보험 업무 전체 라이프사이클**을 구현하는 프로젝트다. 보험가입자(Policyholder)와 보험사 직원(InsuranceEmployee) 두 액터를 중심으로 17개의 유스케이스를 정의하고, 도메인 주도 설계(DDD) 기반으로 모델을 잡았다.

### 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 4, Java 21, Spring Data JPA, Spring Security, Validation, Spring MVC |
| DB | MySQL (Docker Container) |
| 빌드 | Gradle (Kotlin DSL) |
| 테스트 | JUnit 5 |
| Frontend | React *(예정)* |

### 레포 구조 (Monorepo)

```
insurance/
├── CLAUDE.md             # AI 개발 방법론 + 프로젝트 규약
├── docs/                 # 공용 도메인 문서 (백엔드/프론트엔드 공유)
│   ├── usecases/         # UC01 ~ UC17 유스케이스 시나리오
│   └── class_diagram/    # 클래스 다이어그램 (00_overview ~ 07_external)
├── backend/              # Spring Boot 4 / Java 21
└── frontend/             # React (추후 생성)
```

한 레포에 백엔드와 프론트엔드를 함께 두어, 공용 도메인 문서(`docs/`)를 단일 출처(Single Source of Truth)로 공유한다.

---

## 2. 도메인 모델

### 액터

- **Policyholder (보험가입자)** — 상품 조회, 가입 신청, 보험금 청구, 사고 접수, 보험료 납부
- **InsuranceEmployee (보험사 직원)** — 가입 심사, 보험금 지급 심사, 담당자 지정

### 핵심 클래스 계층

```
User (abstract)                InsuranceProduct (abstract)
├── Policyholder               ├── HealthInsuranceProduct
└── InsuranceEmployee          └── CarInsuranceProduct

Claim (abstract)               Review (abstract)
├── HealthInsuranceClaim       ├── EnrollmentReview        (가입 심사)
└── CarAccidentReport          └── BenefitPaymentReview     (지급 심사)
```

### 주요 관계 (생명주기 기준 설계)

| 관계 | 종류 | 근거 |
|------|------|------|
| Policyholder ◇ InsuranceContract | Aggregation | 가입자 탈퇴해도 계약 이력은 법적으로 보존 |
| InsuranceProduct ◇ InsuranceContract | Aggregation | 상품 폐지되어도 기존 계약 유지 |
| InsuranceContract ◆ Payment / Notice / Claim | Composition | 유효 계약 없으면 납부·고지·청구 불가 |
| InsuranceProduct ◆ CoverageItem | Composition | 상품 삭제 시 보장 항목도 소멸 |
| EnrollmentReview → AccidentHistory | Association | 자동차보험 심사 시 금융감독원 사고 이력 조회 |

> 상세 정의: [`docs/class_diagram/`](./docs/class_diagram/), 유스케이스: [`docs/usecases/`](./docs/usecases/)

---

## 3. 아키텍처

### 계층 구조 (Layered Architecture)

```
Controller → DTO → Service → Entity(Domain) → Repository(DAO) → DB
```

각 계층은 **자신의 인접 계층하고만** 통신한다. 이 계층 규약을 어기면 도메인이 오염되고 유지보수가 어려워진다 (→ [4. AI 활용방안](#4-ai-활용방안) 참고).

### 데이터 접근 — Spring Data JPA

Spring Data JPA에서는 **Repository 인터페이스만 정의하면 런타임에 프록시 구현체가 생성되어 DAO 역할**을 수행한다. 내부적으로는 `EntityManager`를 사용하여 데이터 접근을 처리하므로, 개발자는 보일러플레이트 없이 도메인 중심으로 작업할 수 있다.

### 인프라 — Docker

로컬 환경에 MySQL을 직접 설치하는 대신 **Docker Container**로 데이터베이스를 구성하여, 환경 구축 시간을 단축하고 관리 복잡도를 줄였다.

---

## 4. AI 활용방안

이 프로젝트는 AI 에이전트(Claude Code)를 적극 활용하되, **AI가 만든 문제를 식별하고 해결하며 재발을 방지**하는 과정 자체를 방법론으로 정립했다.

### 개발 파이프라인 (DDD + Superpowers + grill-with-docs)

```
brainstorming → grill-with-docs → writing-plans → executing-plans → 검증
   (설계 spec)     (용어/결정 확정)    (TDD 계획서)    (TDD 구현)      (Codex Review)
```

| 단계 | 도구 | 산출물 |
|------|------|--------|
| 발산 | `brainstorming` | 설계 spec (`docs/superpowers/specs/`) |
| 수렴(취조) | `grill-with-docs` | 용어집 `CONTEXT.md`, 결정 기록 `docs/adr/` |
| 계획 | `writing-plans` | TDD 구조 계획서 (`docs/superpowers/plans/`) |
| 구현 | `executing-plans` + TDD | 코드 (테스트 우선) |
| 검증 | `Codex Review` | 최종 머지 승인 |

> 자세한 규약은 [`CLAUDE.md`](./CLAUDE.md) 참고.

### 테스트 주도 개발 (TDD)

구현 단계는 전적으로 **TDD**로 진행한다. 이는 AI가 생성하는 코드의 신뢰성을 보장하는 핵심 장치다 — 테스트가 먼저 실패하는 것을 직접 확인하지 않으면, 그 테스트가 올바른 것을 검증하는지 알 수 없다.

**철칙 (Iron Law): 실패하는 테스트 없이는 프로덕션 코드를 작성하지 않는다.** 코드를 테스트보다 먼저 썼다면 지우고 다시 시작한다.

**Red → Green → Refactor 사이클**

| 단계 | 내용 |
|------|------|
| 🔴 RED | 원하는 동작을 표현하는 실패 테스트를 먼저 작성 |
| ✅ Verify RED | 테스트를 돌려 **의도한 이유로 실패**하는지 확인 (필수, 생략 불가) |
| 🟢 GREEN | 테스트를 통과시키는 **최소한의** 구현만 작성 (YAGNI) |
| ✅ Verify GREEN | 테스트 통과 + 기존 테스트도 깨지지 않음 확인 |
| 🔵 REFACTOR | 테스트를 녹색으로 유지한 채 중복 제거·이름 개선 |

`writing-plans` 단계에서 만드는 계획서의 각 task가 이미 위 사이클(테스트 작성 → 실패 확인 → 최소 구현 → 통과 확인 → 커밋)로 쪼개져 있어, `executing-plans`로 계획을 실행하는 것이 곧 TDD를 수행하는 것이 된다.

**보험 도메인과 TDD의 궁합** — 보험은 만기·갱신·청구 조건 등 규칙이 핵심이라, 클래스 다이어그램의 관계(예: `Policy ↔ Claim`, 만기 지난 계약의 청구 거부)를 테스트로 먼저 박아두면 `grill-with-docs`에서 합의한 도메인 경계가 코드에서도 깨지지 않는다. 버그 수정도 항상 재현 테스트를 먼저 작성해 회귀를 방지한다.

```
# 백엔드 테스트 실행
cd backend && ./gradlew test
```

### AI가 뭘 잘못했고, 어떻게 해결했으며, 재발을 어떻게 막았는가

#### 문제 1. 도메인 오염 (관심사의 분리 위반)

- **무엇을 잘못했나**: AI가 도메인 엔티티에 프레젠테이션/영속성 관심사를 섞어 넣어, 순수해야 할 도메인이 오염됨.
- **어떻게 해결했나**: 관심사를 분리하여 도메인 엔티티는 비즈니스 규칙만 담도록 정리.
- **재발 방지**: `grill-with-docs`로 설계를 도메인 모델에 대고 취조하고, 확정된 경계를 `CONTEXT.md`(용어집)와 ADR에 박아 코드 네이밍·책임의 단일 출처로 삼음.

#### 문제 2. 계층 규약 위반

- **무엇을 잘못했나**: `Controller → DTO → Service → Entity → Repository → DB` 흐름을 건너뛰어, 계층을 우회하는 코드가 생성됨.
- **어떻게 해결했나**: 각 계층이 인접 계층하고만 통신하도록 재구성.
- **재발 방지**: 계층 규약을 `CLAUDE.md`에 명문화하고, TDD(실패 테스트 우선)로 각 계층 경계를 테스트로 고정. 마지막에 `Codex Review`로 교차 검증.

### 개발 환경 도구

- **cmux** — 멀티 인스턴스(Multi-instance) 운영. 여러 작업을 병렬로 진행.
- **Git Worktree** — 작업별로 격리된 워크스페이스를 만들어 안전하게 병렬 개발.
- **추천 워크플로우** — `Superpowers`로 빌드·코딩을 진행한 뒤, 마지막 검증 단계에 `Codex Review`를 끼워 넣어 최종 안심 마크(Merge)를 획득.

---

## 5. 데모

*(작성 예정)*

```
# 백엔드 실행
cd backend && ./gradlew bootRun

# 백엔드 테스트
cd backend && ./gradlew test
```

---

🤖 이 프로젝트는 [Claude Code](https://claude.com/claude-code)를 활용해 개발되었습니다.
