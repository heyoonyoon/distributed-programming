# Epic 1 — 보험 상품 조회(UC01) 설계

- 작성일: 2026-06-03
- 범위: 보험 상품 도메인 + 상품 목록/상세 조회 + 필터 (UC01)
- 후속: 이 spec → grill-with-docs(취조) → writing-plans → 구현
- 관련 문서: `CONTEXT.md`(용어), `docs/adr/0001-diagram-methods-are-responsibilities.md`,
  `docs/class_diagram/02_product.md`, `docs/usecases/UC01.md`

---

## 1. 배경 / 목적

UC02(보험 가입)는 "UC01로 상품을 선택한 상태"를 선행조건으로 가진다. 따라서 **상품 도메인 +
조회**가 가입 흐름의 토대(Epic 1)다. Epic 0(사용자·인증) 위에 상품 카탈로그를 얹는다.

학교 발표용 데모(약 10분)가 목표이므로, 데모에서 보여줄 흐름("종류 선택 → 목록 → 상세 →
필터")이 실제로 동작하는 것을 우선한다.

## 2. 범위 (Scope)

### 포함 ✅
- 상품 도메인 계층: `InsuranceProduct`(abstract) → `HealthInsuranceProduct`, `CarInsuranceProduct`
- `CoverageItem` (composition) — 상품별 보장항목
- 종류별 상품 목록 조회 + 상품 상세 조회 (둘 다 **비로그인 가능 / public**)
- 필터: 월 보험료 범위(`minPremium`/`maxPremium`) + 보장항목 키워드(`keyword`) — UC01 A1
- `DataSeeder` 확장: 의료/자동차 각 2~3개 상품 + 상품별 보장항목 2~3개 (멱등 시드)

### 제외 ❌ (이유)
- **맞춤 보험료 계산(`calculatePremium` 실제 로직)** — UC01은 "월 보험료 **예시**"만 요구하고,
  맞춤 견적의 입력(병력·차량정보)은 UC02 가입 폼에서 처음 수집된다. 엔티티엔 **추상 메서드
  시그니처만** 유지(다이어그램 준수)하고 실제 계산은 UC02 Epic으로 미룬다.
- **상품 등록/수정(관리자 기능)** — 클래스 다이어그램·UC01 범위 밖. 데모는 시드로 대체.
- **다이어그램의 String 식별자(`productId`, `itemId`)** — PK는 `Long` 자동증가 사용(Epic 0 선례).

## 3. 결정 사항 (Key Decisions)

| 주제 | 결정 | 근거 |
|------|------|------|
| `calculatePremium` 범위 | **예시 보험료만 노출, 실제 계산은 미룸** | UC01은 "예시"만 명시. 맞춤 입력은 UC02에서 수집. 다이어그램의 추상 시그니처만 유지. |
| 필터 | **구현 (보험료 범위 + 키워드)** | UC01 A1 대안 흐름. 데모에서 필터 동작을 보여줌. |
| JPA 상속 전략 | **SINGLE_TABLE (`product_type`)** | Epic 0 User 계층과 동일 전략으로 일관성. 종류별 전용 필드 2개뿐이라 null 비용 미미, JOIN 없음. |
| CoverageItem | **별도 테이블 (composition, FK `product_id`)** | 다이어그램의 composition 관계. 상품 삭제 시 보장항목도 삭제. |
| 식별자(PK) | **`Long id` 자동증가** | Epic 0 선례. 다이어그램의 String 식별자(`productId`/`itemId`)는 PK로 쓰지 않음. |
| 인증 | **상품 조회는 permitAll (비로그인 가능)** | UC01 선행조건: "비로그인 상태에서도 조회 가능". `SecurityConfig`에 `/products/**` 공개 추가. |
| 시드 규모 | **종류별 2~3개 + 보장항목 2~3개** | 필터(보험료 범위·키워드)가 의미 있으려면 보험료·보장항목이 다양해야 함. |
| 결과 없음(E1) | **빈 배열 + 200** | 별도 에러 아님. 프론트가 "결과 없음" 메시지 표시. |

> 클래스 다이어그램은 고정(grill에서 준수 여부 취조) — 새 도메인 클래스를 만들지 않는다.

## 4. 아키텍처

### 계층 규약 (Epic 0과 동일, 엄수)
```
Controller → DTO → Service → Entity(도메인) → Repository(DAO) → DB
```
- 계층 건너뛰기 금지, 엔티티 직접 노출 금지(반드시 DTO 변환)

### 패키지 구조 (`com.distribution.insurance`)
```
domain/product     InsuranceProduct(abstract, @Inheritance SINGLE_TABLE, product_type),
                   HealthInsuranceProduct, CarInsuranceProduct  ← JPA 엔티티 + 도메인 메서드
                   CoverageItem(@Entity, 별도 테이블, FK product_id)
repository         ProductRepository (종류 + 동적 필터 조회)
service            ProductService (목록/필터/상세 조회)
web/dto            ProductSummaryResponse(목록용), ProductDetailResponse(상세용),
                   CoverageItemResponse
web/controller     ProductController (GET /products, GET /products/{id})
config             DataSeeder ← 상품 시드 추가(기존 사용자 시드와 함께 멱등)
```

### 클래스 → JPA 매핑
| UML | Spring/JPA |
|-----|-----------|
| `InsuranceProduct` (abstract) | `abstract class` + `@Inheritance(SINGLE_TABLE)` + `@DiscriminatorColumn("product_type")` |
| `HealthInsuranceProduct` | `@DiscriminatorValue("HEALTH")`, `maxHospitalizationDays` |
| `CarInsuranceProduct` | `@DiscriminatorValue("CAR")`, `vehicleType`, `driverScopeType` |
| `CoverageItem` | `@Entity` 별도 테이블, `@ManyToOne` product (FK), itemName/coverageLimit/deductible |
| `calculatePremium(holder)` | 추상 메서드 시그니처만 유지(미구현 — UC02 Epic). 호출 흐름 없음. |
| `getProductInfo()` | DTO 변환(`ProductService`/응답 매핑)으로 대체 — 엔티티가 응답 객체 직접 생성 안 함. |

상속 결과: **자바 클래스 3개(InsuranceProduct/Health/Car) → DB 테이블 1개(`insurance_product`)**,
`product_type` 디스크리미네이터로 구분. `CoverageItem`은 별도 테이블.

## 5. API

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| GET | `/products?type=HEALTH\|CAR&minPremium=&maxPremium=&keyword=` | 종류별 목록(필터 선택적) | 불필요(public) |
| GET | `/products/{id}` | 상품 상세 | 불필요(public) |

- **목록 응답** `ProductSummaryResponse`: `id`, `productName`, `coverageSummary`(보장항목명 일부 요약),
  `monthlyPremium`(= `basePremium`, 예시), `productType`
- **상세 응답** `ProductDetailResponse`: 목록 필드 + `description`, `premiumBasis`(보험료 산출 기준 문구),
  `coverageItems`: `CoverageItemResponse[]`(`itemName`, `coverageLimit`, `deductible`)
- `type` 필수. `minPremium`/`maxPremium`/`keyword`는 선택. 필터 조합은 `ProductRepository`의
  동적 쿼리(Spring Data `Specification` 또는 조건 분기 JPQL)로 처리.

## 6. 핵심 흐름 (비즈니스 로직)

### 6.1 상품 목록 조회 — `GET /products`
1. `type` 파싱(HEALTH/CAR) → 잘못된 값이면 **400**
2. `ProductService`가 필터 조건으로 `ProductRepository` 동적 조회
   - `minPremium`/`maxPremium` → `basePremium` 범위 조건
   - `keyword` → `productName` 또는 보장항목명(`CoverageItem.itemName`)에 LIKE 매칭(상품-보장항목 JOIN)
3. `ProductSummaryResponse[]`로 변환 반환
4. **결과 없음(E1)** → 빈 배열 `[]` + 200 (프론트가 "결과 없음" 표시)

### 6.2 상품 상세 조회 — `GET /products/{id}`
1. `findById` → 없으면 `IllegalArgumentException` → 전역 핸들러가 **404**
2. 상품 + 보장항목을 `ProductDetailResponse`로 변환 반환

### 6.3 에러 매핑 (Epic 0 `GlobalExceptionHandler` 재사용)
- 잘못된 `type` 값 / 음수 premium → **400**
- 존재하지 않는 상품 id → **404**

## 7. 테스트 전략 (TDD — Red → Green → Refactor)

- **ProductRepository**
  - 종류별 조회 / 보험료 범위 필터 / 키워드(상품명·보장항목명) 필터 / 복합 필터
- **ProductService**
  - 종류 목록 정상 / 필터 적용 결과 / 빈 결과(E1) / 상세 조회 / 없는 id → 예외
- **ProductController (MockMvc)**
  - GET 목록 200 + JSON, 필터 쿼리, 상세 200, 없는 id 404, 잘못된 type 400
  - **인증 없이 접근 가능(permitAll)** 확인
- **DataSeeder**
  - 상품 시드 멱등(이미 있으면 중복 삽입 안 함)

테스트 실행: `cd backend && ./gradlew test`

## 8. 시드 데이터 (데모 상품)

- 의료보험(`HealthInsuranceProduct`) 2~3개 — 보험료·보장항목 다양화(필터 시연용)
- 자동차보험(`CarInsuranceProduct`) 2~3개 — 차종/운전범위 다양화
- 각 상품에 `CoverageItem` 2~3개(보장항목명·한도·면책)
- `DataSeeder`가 앱 시작 시 "없으면 삽입" 방식으로 멱등 처리(기존 사용자 시드와 함께)

## 9. 미해결/후속 (이 Epic 밖)
- 맞춤 보험료 계산(`calculatePremium`)은 UC02(가입) Epic에서 입력 수집과 함께 구현.
- 상품 등록/수정(관리자)은 데모 범위 밖.
