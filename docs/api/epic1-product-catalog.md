# API 명세서 — Epic 1 (보험 상품 카탈로그)

보험 시스템 백엔드 Epic 1의 REST API. 이 문서는 **현재 구현된 동작 그대로**를 기술한다.
프론트엔드(React)는 이 명세를 단일 출처로 삼아 연동한다.

- Base URL: `http://localhost:8080`
- 인증: **공개**. 상품 조회는 로그인 없이 가능(`GET /products`, `GET /products/**`는 permitAll).
- 요청/응답 본문: `application/json`
- 용어: `InsuranceProduct`(상품), `HealthInsuranceProduct`(의료), `CarInsuranceProduct`(자동차), `CoverageItem`(보장 항목) — CONTEXT.md 따름.
- 유스케이스: UC01 보험 상품을 조회한다.

---

## 1. 상품 목록 조회 (필터)

`GET /products` — 공개

쿼리 파라미터
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | `HEALTH` \| `CAR` | **필수** | 상품 종류 |
| `minPremium` | int(≥0) | 선택 | 월 보험료 하한 |
| `maxPremium` | int(≥0) | 선택 | 월 보험료 상한 |
| `keyword` | string | 선택 | 상품명/설명 키워드 부분일치 |

응답 `200 OK`
```json
[
  {
    "id": 1,
    "productName": "건강플러스",
    "coverageSummary": "암진단비, 암수술비",
    "monthlyPremium": 30000,
    "productType": "HEALTH"
  }
]
```
> `coverageSummary`: 보장 항목명 최대 3개를 `, `로 이어 붙인 요약.

오류
| 상황 | 상태 | 비고 |
|------|------|------|
| `type` 누락 또는 알 수 없는 값(예: `LIFE`) | `400 Bad Request` | enum 매핑 실패 |
| `minPremium`/`maxPremium` 음수 | `400 Bad Request` | `@Min(0)` 위반 |
| `minPremium > maxPremium` (역전된 범위) | `400 Bad Request` | "minPremium은 maxPremium보다 클 수 없습니다." |

---

## 2. 상품 상세 조회

`GET /products/{id}` — 공개

응답 `200 OK`
```json
{
  "id": 1,
  "productName": "건강플러스",
  "productType": "HEALTH",
  "description": "암 보장 포함",
  "monthlyPremium": 30000,
  "coverageItems": [
    { "itemName": "암진단비", "coverageLimit": 30000000, "deductible": 0 }
  ]
}
```

오류
| 상황 | 상태 |
|------|------|
| 없는 상품 id | `404 Not Found` |

---

## 비즈니스 로직

### 상품 도메인 모델
- `InsuranceProduct`는 추상 부모이며 JPA **단일 테이블 상속**(`product_type` discriminator)으로 매핑된다.
  - `HealthInsuranceProduct`(discriminator `HEALTH`): 추가 필드 `maxHospitalizationDays`(최대 입원 보장일).
  - `CarInsuranceProduct`(discriminator `CAR`): 추가 필드 `vehicleType`(차종), `driverScopeType`(운전자 범위).
- `CoverageItem`(보장 항목)은 상품과 `@OneToMany`(상품이 주인, cascade ALL + orphanRemoval). 필드: `itemName`, `coverageLimit`(보장 한도), `deductible`(자기부담금).
- `basePremium`(기본 월 보험료)은 정수(원 단위).

### 조회/필터 규칙
- 목록은 `type`으로 **해당 종류만** 반환한다(상속 타입 기준 조회).
- `minPremium`/`maxPremium`는 `basePremium` 범위 필터. 둘 다 주어지고 `min > max`면 의미상 모순이므로 **400으로 거부**(빈 목록을 반환하지 않는다).
- `keyword`는 상품명·설명에 대한 부분일치.

### 응답 매핑/예외 처리 주의
- 상품 종류 → 문자열(`HEALTH`/`CAR`) 매핑에서 **알 수 없는 종류는 `RuntimeException`(500)**으로 처리한다. `IllegalStateException`은 전역 핸들러가 403으로 매핑하므로 오매핑 방지를 위해 일부러 사용하지 않는다.
- 잘못된 enum 값/타입 불일치는 `MethodArgumentTypeMismatchException` → 400.

### 시드 데이터
- 애플리케이션 기동 시 `DataSeeder`가 상품·보장 항목을 트랜잭션 내에서 시드한다(개발/데모용).
