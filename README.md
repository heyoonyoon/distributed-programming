# 의료·자동차 보험 시스템 (Insurance System)

> 의료보험·자동차보험의 **상품 조회 → 가입 → 심사 → 계약 → 납부 → 청구 → 보험금 지급**까지를 다루는 보험 도메인 시스템.
> Spring Boot 백엔드 + React 프론트엔드를 **모노레포**로 구성하고, **AI 기반 개발 방법론**(DDD + Superpowers + grill-with-docs)으로 만들었다.

이 문서는 발표자료(`docs/presentation/보험발표-figma.pptx`)의 내용을 정리한 것이다.

---

## 1. 무엇을 만들었나

보험가입자(**Policyholder**)와 보험사 직원(**InsuranceEmployee**) 두 액터를 중심으로, 17개 유스케이스(UC01~UC17)를 도메인 주도 설계(DDD)로 구현했다.

| 액터 | 하는 일 |
|------|---------|
| **고객(Policyholder)** | 상품 조회 · 가입 · 납부 · 청구 |
| **직원(InsuranceEmployee)** | 가입 심사 · 보험금 지급 심사 |
| **시스템** | 계약 생성 · 미납 고지서 · 지급 자동화 |

### 두 가지 보험

- **의료보험** — 병원비 청구
  - 100만 원 **미만** → 즉시 입금
  - 100만 원 **이상** → 직원 확인 후 입금
- **자동차 사고 보험** — 사고 접수 → 직원이 맡음 → 보상액 결정 → 계좌 입금

### 핵심 업무 흐름

```
[계약]   상품 조회 → 가입 신청 → 직원 심사 → (승인 시) 계약 자동 생성
[납부]   보험료 납부 → 시스템 미납 감지 → 고지서 발송
[보상]   청구·접수 → 직원 보상 심사 → 보험금 지급
```
> 병원비는 100만 원 미만이면 즉시, 그 이상이거나 자동차 사고는 직원 심사 후 지급.

---

## 2. 도메인 모델 — 여섯 개

`사용자 · 상품 · 계약 · 청구 · 심사 · 사고이력`

최초 클래스/유스케이스 다이어그램으로 설계한 뒤, 구현하며 일부를 조정했다(설계 ↔ 코드 역설계 비교).

| 도메인 | 최초 설계와 달라진 점 |
|--------|----------------------|
| **상품** | 상품번호를 문자열 → 숫자 PK로 (DB가 새 상품마다 자동 증가) |
| **계약** | 유스케이스엔 있으나 설계엔 없던 **자동이체 정보** 추가 |
| **청구** | 첨부파일 추가, 청구 상태를 4개 → 6개로 확장(송금 결과까지 추적) |
| **심사** | 지급 심사를 의료뿐 아니라 **자동차 사고까지** 확대 |
| **사고이력** | 상세 사고기록은 심사에 안 쓰여 제외(요약값만 보존) |

### 청구 상태값 (ClaimStatus)

```java
public enum ClaimStatus {
    PENDING, IN_REVIEW, APPROVED, REJECTED,   // 설계 4개
    COMPLETED, FAILED                         // 구현에서 추가: 송금 성공·실패
}
```
> 돈을 실제로 보냈는지까지 따라가려고 상태 두 개를 더했다.

---

## 3. 아키텍처 — 클래스 하나를 4개로 나눈다

`계약` 같은 클래스 하나가 하던 일을 네 계층으로 분리했다.

| 계층 | 역할 |
|------|------|
| `controller` | 화면에서 온 요청을 받는 입구 |
| `service` | 실제 비즈니스 로직 실행 |
| `domain` (Entity) | DB 테이블과 매핑되는 핵심 객체 (데이터 + 규칙) |
| `repository` | DB에 저장·조회 |

### 최초 클래스 설계와의 관계

**최초 다이어그램의 클래스가 곧 `domain`(엔티티)이다.** 나머지 층(controller·service·repository·dto)은 이 도메인을 안전하게 쓰기 위한 껍데기.

```
최초 클래스 설계                  →   domain (Entity)        + 감싸는 층
InsuranceContract                     = 최초 클래스 그대로      controller · service
  monthlyPremium, status                                       repository · dto
  pay() / suspend()
```

### 호출 흐름 ≠ 의존 방향 (의존성 최소화)

- **호출 흐름**(실행 순서): `Controller → Service → Repository → DB`
- **의존 방향**(코드 참조): `Service · Repository · DTO` → **모두 `domain`을 향함**

`Repository`(`JpaRepository<Claim>`), `Service`(`claim.markInReview()`), `DTO`(`from(Claim claim)`) 셋 다 코드에 `Claim`이라는 이름을 써야 동작한다 = **domain에 의존**. 반대로 `domain`(`Claim`) 코드에는 다른 계층 이름이 하나도 없다.

```java
class Claim {
    int requestAmount;        // 데이터
    ClaimStatus status;
    void markInReview() {     // 규칙
        this.status = IN_REVIEW;
    }
}
// Service? Repository? DTO? → 하나도 없음
```
> **domain은 누구도 의존하지 않는다 → 핵심을 독립시켜 의존성을 최소화했다.**

### Entity ↔ DTO

DB 엔티티를 그대로 외부에 노출하지 않고, 경계(web/dto)에서 **응답 DTO로 단방향 변환**한다. (`ProductDetailResponse.from(InsuranceProduct)`)

---

## 4. 전체 흐름 한 줄기 (예: 상품 상세 조회)

```
프론트(React)                        백엔드(Spring)
화면 → hook → api    ──요청 DTO──▶   controller → service → domain(entity)
                     ◀─응답 DTO──    ↑ DTO로 변환            ↓
                                                          repository → DB
```

1. **화면** `ProductsView.tsx` 버튼이 hook의 `selectProduct` 호출
2. **API** `contractsApi.getProduct(id)` → `GET http://localhost:8080/products/2`
3. **컨트롤러** `ProductController` 가 요청을 받아 서비스로 넘김
4. **서비스 · repository** `productRepository.findById(id)` 로 DB 조회
5. **엔티티 → DTO** `ProductDetailResponse.from(entity)` 로 변환(엔티티 직접 노출 X)
6. **화면 표시** JSON 응답을 화면에 렌더

> 클릭 → 컨트롤러 → 서비스 → DB → DTO → 화면, 한 줄기로 흐른다.

---

## 5. AI 활용 — 어떻게 통제했나

이 프로젝트의 차별점은 **AI에게 맡기되, 규약과 취조로 통제**했다는 것이다.

### 도구

| 도구 | 용도 |
|------|------|
| **Claude Code (Opus 4.8)** | 설계·구현 메인 |
| **Superpowers 스킬** | 설계 → 서브에이전트로 TDD 구현 |
| **grill-with-docs 스킬** | AI 설계를 도메인 문서에 대고 한 질문씩 따져 검증 |
| **Claude Hook** | 세션마다 방법론 자동 주입 |
| **cmux** | 프론트·백엔드 두 Claude를 메시지로 연결 |
| **Codex 5.5** | 다른 모델로 교차 코드 리뷰 |
| **모노레포** | 도메인 문서를 백·프가 한 레포에서 공유 |

### 왜 모노레포?

백엔드에서 만든 **다이어그램·MD 문서를 프론트가 한 레포에서 바로 참고**하라고. (cmux로 두 Claude를 띄워, 백엔드 API를 바꾸면 프론트가 즉시 반영)

### 개발 파이프라인

```
발산              취조               계획            구현              검증
brainstorming → grill-with-docs → writing-plans → executing-plans → Codex 리뷰
```

- **TDD 강제** — Red(실패 테스트 먼저) → Green(통과할 최소 구현) → Refactor. AI가 짠 코드도 이 사이클을 강제했다.
- **Hook 자동 주입** — `.claude` SessionStart 훅으로 세션이 열릴 때마다 "이렇게 일해"를 매번 시키지 않아도 일하는 방식이 강제됐다.

### AI 통제 사례

| AI가 한 것 | 통제한 방법 |
|------------|------------------|
| 같은 사람을 Policyholder·account·user로 섞어 부름 | `CONTEXT.md`에 표준어 하나로 못박음 |
| 보험료를 '맞춤 계산'까지 만들려 함(과한 구현) | 유스케이스 범위(예시만)로 되돌림 |
| 모노레포라 `git add`를 넓게 잡아 백엔드 PR에 프론트까지 섞임 | 영역마다 worktree 분리 + hook으로 `git add .` 차단 |

---

## 6. 작업 관리 & 회고

### Epic = 스프린트 5개

GitHub **마일스톤 = Epic**, **이슈 = 작업 단위**, **이슈 브랜치 하나 = PR 하나**.

| Epic | 내용 |
|------|------|
| Epic 0 | 기반(사용자·인증) |
| Epic 1 | 상품 카탈로그 |
| Epic 2 | 가입 & 심사 |
| Epic 3 | 계약 & 수납 |
| Epic 4 | 청구 & 보험금 지급 |

### AI는 공짜가 아니다

- 한 스프린트에 약 **$39.63 · 토큰 5,100만** — 한 세션이 5시간 토큰 한도를 거의 다 소진.
- **돈 먹은 범인**: 코드 생성이 아니라 **'긴 컨텍스트 재읽기'** — 캐시 읽기만 4,580만 토큰(전체 비용의 90%).
- **해결책**: Epic 끝나면 `/clear`, 구현은 Sonnet, 리뷰는 Codex.

---

## 7. 기술 스택 & 실행

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 4, Java 21, Spring Data JPA, Spring Security(JWT), Validation, Spring MVC, Lombok |
| Frontend | React + TypeScript + Vite |
| DB | MySQL 8 (Docker) |
| 빌드 | Gradle |
| 테스트 | JUnit 5 + MockMvc |

### 실행

```bash
# 1) DB
docker compose up -d

# 2) 백엔드 (http://localhost:8080)
cd backend && ./gradlew bootRun

# 3) 프론트 (http://localhost:5173)
cd frontend && npm install && npm run dev

# 테스트
cd backend && ./gradlew test
```

### 데모 계정 (시드, 비밀번호 전부 `1234`)

| 역할 | 이메일 |
|------|--------|
| 고객 | `hong@test.com` |
| 고객 | `kim@test.com` |
| 직원 | `staff@test.com` |

---

## 8. 레포 구조 (Monorepo)

```
insurance/
├── CLAUDE.md             # AI 개발 방법론 + 프로젝트 규약
├── CONTEXT.md            # 용어집 (네이밍 단일 출처)
├── docs/
│   ├── usecases/         # UC01 ~ UC17
│   ├── class_diagram/    # 클래스 다이어그램
│   ├── adr/              # 결정 기록(ADR)
│   ├── presentation/     # 발표자료(pptx)
│   └── superpowers/      # specs / plans
├── backend/              # Spring Boot 4 / Java 21
└── frontend/             # React + Vite
```

한 레포에 백·프를 함께 두어 공용 도메인 문서(`docs/`)를 단일 출처로 공유한다.
