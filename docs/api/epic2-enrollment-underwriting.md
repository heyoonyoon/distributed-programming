# API 명세서 — Epic 2 (가입 & 심사)

보험 시스템 백엔드 Epic 2의 REST API. 이 문서는 **현재 구현된 동작 그대로**를 기술한다.
프론트엔드(React)는 이 명세를 단일 출처로 삼아 연동한다.

- Base URL: `http://localhost:8080`
- 인증: **JWT Bearer**. 헤더 `Authorization: Bearer <token>` 필요.
  - `/applications/**` → **가입자(ROLE_POLICYHOLDER) 전용**
  - `/reviews/**` → **직원(ROLE_EMPLOYEE) 전용**
  - 권한 불일치 시 `403 Forbidden`, 토큰 없음 `401 Unauthorized`.
- 요청/응답 본문: `application/json`
- 용어: `InsuranceApplication`(가입 신청), `EnrollmentReview`(가입 심사), `ApplicationStatus`, `ReviewResult`, `AccidentHistory`(사고 이력) — CONTEXT.md 따름.
- 유스케이스: UC02 가입 요청, UC13 가입 심사, UC15 사고이력 조회.

---

# A. 가입 요청 (UC02) — 가입자 전용

## A-1. 가입 신청

`POST /applications` — 가입자 전용

요청 (의료보험 예시)
```json
{
  "productId": 1,
  "medicalHistory": {
    "currentConditions": "고혈압",
    "pastHospitalization": "2019년 입원",
    "medications": "혈압약"
  }
}
```
요청 (자동차보험 예시)
```json
{
  "productId": 2,
  "vehicleInfo": {
    "plateNumber": "12가3456",
    "vehicleType": "승용차",
    "modelYear": 2020,
    "drivingExperienceYears": 5
  }
}
```
> 개인정보(이름·주민번호·생년월일·연락처·이메일)는 **요청 본문에 넣지 않는다.** 인증된 Policyholder에서 읽는다(ADR 0002).

응답 `201 Created`
```json
{ "applicationId": 7, "status": "PENDING", "appliedAt": "2026-06-03T10:00:00" }
```
> 신청 접수 후 mock 알림 발송(로그). 메시지: 접수번호 + 예상 처리기간. 로그의 연락처는 마스킹된다.

오류
| 상황 | 상태 | 비고 |
|------|------|------|
| `productId` 누락 | `400 Bad Request` | `@NotNull` |
| 추가정보 형식 오류(차량 plateNumber/차종 공백, modelYear≤0, 운전경력 음수, 의료 항목 공백/500자 초과) | `400 Bad Request` | DTO Bean Validation |
| 종류-추가정보 불일치(자동차상품에 medicalHistory, 의료상품에 vehicleInfo, 한쪽 누락) | `400 Bad Request` | 도메인 검증(InvalidRequestException) |
| 없는 상품 | `404 Not Found` | |
| 직원 토큰으로 호출 | `403 Forbidden` | |

## A-2. 내 신청 목록 조회

`GET /applications/me` — 가입자 전용

응답 `200 OK`
```json
[
  {
    "applicationId": 7,
    "status": "PENDING",
    "appliedAt": "2026-06-03T10:00:00",
    "productId": 1,
    "productName": "건강플러스"
  }
]
```

## A-3. 신청 취소

`POST /applications/{id}/cancel` — 가입자 전용

응답 `200 OK` (본문 없음). 상태가 `CANCELLED`로 전이.

오류
| 상황 | 상태 |
|------|------|
| 없는 신청 | `404 Not Found` |
| 타인의 신청 취소 시도 | `403 Forbidden` |
| 비PENDING(이미 승인/반려/취소) 건 취소 | `409 Conflict` |

---

# B. 가입 심사 (UC13 + UC15) — 직원 전용

## B-1. 심사 대기 목록

`GET /reviews/pending` — 직원 전용

응답 `200 OK` — `status=PENDING`인 신청만.
```json
[
  {
    "applicationId": 7,
    "appliedAt": "2026-06-03T10:00:00",
    "applicantName": "홍길동",
    "productName": "건강플러스",
    "basePremium": 30000
  }
]
```

## B-2. 심사 상세

`GET /reviews/applications/{id}` — 직원 전용

응답 `200 OK` — 자동차보험 건이면 `accidentHistory`(금융감독원 mock 조회 결과)를 동봉, 의료 건이면 `accidentHistory`·`vehicleInfo`는 생략(null).
```json
{
  "applicationId": 8,
  "applicantName": "홍길동",
  "birthDate": "1990-01-01",
  "ssn": "900101-1234567",
  "productName": "안심드라이브",
  "basePremium": 45000,
  "vehicleInfo": {
    "plateNumber": "12가3456", "vehicleType": "승용차",
    "modelYear": 2020, "drivingExperienceYears": 5
  },
  "medicalHistory": null,
  "accidentHistory": {
    "accidentCount": 2, "totalPaidAmount": 2000000, "licenseStatus": "VALID"
  }
}
```
> `ssn`(주민등록번호) 원문은 UC13 4단계 요구사항에 따라 직원 심사 화면에 노출된다.

오류
| 상황 | 상태 |
|------|------|
| 없는 신청 | `404 Not Found` |
| 가입자 토큰으로 호출 | `403 Forbidden` |

## B-3. 심사 확정

`POST /reviews/applications/{id}/confirm` — 직원 전용

요청 (일반 승인)
```json
{ "result": "APPROVED", "comment": "이상 없음" }
```
요청 (조건부 승인 — 할증)
```json
{ "result": "CONDITIONAL", "comment": "사고이력 다수", "surchargeRate": 0.2 }
```
요청 (반려)
```json
{ "result": "REJECTED", "comment": "면허 정지 이력" }
```

응답 `200 OK`
```json
{ "reviewId": 5, "result": "CONDITIONAL", "surchargeRate": 0.2, "adjustedPremium": 36000 }
```

오류
| 상황 | 상태 | 비고 |
|------|------|------|
| `result` 누락 | `400 Bad Request` | `@NotNull` |
| CONDITIONAL인데 `surchargeRate` 없음/0/음수 | `400 Bad Request` | 할증 규칙 |
| APPROVED/REJECTED인데 `surchargeRate` 존재 | `400 Bad Request` | 할증 규칙 |
| 없는 신청 / 직원 아님 | `404 Not Found` | |
| 가입자 토큰으로 호출 | `403 Forbidden` | |
| 이미 심사된(비PENDING) 건 재확정 | `409 Conflict` | 상태가드 |
| 동일 신청에 심사 중복 저장(동시성) | `409 Conflict` | unique 제약 위반 매핑 |

---

## 비즈니스 로직

### 도메인 모델
- **InsuranceApplication**(가입 신청): `applicant`(Policyholder 참조), `product`(InsuranceProduct 참조), `status`, `appliedAt`, 종류별 추가정보 `vehicleInfo`/`medicalHistory`(@Embeddable, 한쪽만 nullable). 개인정보는 복제하지 않고 applicant에서 읽는다(ADR 0002).
- **ApplicationStatus**: `PENDING`(심사 대기) → `APPROVED`(승인) | `REJECTED`(반려) | `CANCELLED`(취소). **조건부승인을 위한 별도 상태는 없다**(ADR 0003).
- **Review**(추상, 단일 테이블 상속) → **EnrollmentReview**: `result`, `comment`, `reviewedAt`, `surchargeRate`, `adjustedPremium`, `accidentHistory`(자동차건만), `application`(1:1), `reviewer`(확정한 직원 = 담당자).
- **ReviewResult**: `APPROVED` | `CONDITIONAL`(할증 조건부 승인) | `REJECTED`.
- **AccidentHistory**(@Embeddable): `accidentCount`, `totalPaidAmount`, `licenseStatus`(VALID/SUSPENDED/REVOKED), `fetchedAt`.

### 가입 신청 규칙 (UC02)
- 종류 정합성: 자동차보험은 `vehicleInfo` 필수·`medicalHistory` 금지, 의료보험은 그 반대. 위반 시 400.
- 신청 시 `status=PENDING`, `appliedAt=now`. 접수 후 mock 알림 발송.
- 취소는 본인의 PENDING 건만 가능(타인 403, 비PENDING 409).

### 가입 심사 규칙 (UC13)
- 심사 대기 목록은 `PENDING` 신청만 노출.
- 상세 조회 시 **자동차보험 건이면** UC15에 따라 금융감독원 사고이력을 mock 조회해 동봉(`MockAccidentHistoryClient`: ssn 기반 결정적 더미). 의료 건은 조회하지 않는다.
- 확정 처리:
  - `result`에 따라 `EnrollmentReview.confirm()`가 보험료를 산출하고 `Application` 상태를 전이.
  - **`adjustedPremium`은 결과와 무관하게 항상 "최종 월 보험료"를 담는다**(ADR 0003): APPROVED = basePremium, CONDITIONAL = `round(basePremium × (1 + surchargeRate))`, REJECTED = 0(계약으로 이어지지 않으므로 무의미).
  - **조건부승인(CONDITIONAL)도 Application 상태는 `APPROVED`로 전이**한다(조건부 여부는 ReviewResult가 소유).
  - 반려(REJECTED)는 `REJECTED`로 전이.
  - 확정 후 가입자에게 mock 결과 통보 발송.
- Epic 3 연계: 실제 `InsuranceContract` 생성·수납은 Epic 2 범위 밖. Epic 3는 `EnrollmentReview.adjustedPremium` **한 곳만** 읽어 월 보험료를 만든다(단일 출처).

### 중복 심사 방어 (E3)
- 순차 재확정: 비PENDING 건은 상태 전이 가드가 `409 Conflict`로 거부.
- 동시 확정: `EnrollmentReview.application_id`에 **unique 제약**을 두어 신청당 심사 1건만 저장되도록 보장. 제약 위반은 `DataIntegrityViolationException` → `409 Conflict`로 매핑.

### 의도적으로 제외한 범위 (YAGNI)
- A2 부서협의(협의중 상태/타부서 알림), 낙관락(`@Version`) 기반 동시성 제어, 외부연동 실패(E1) 재시도/직접입력 전환 UI — 설계 단계에서 제외.

### 에러 매핑 (전역)
| 예외 | 상태 |
|------|------|
| `IllegalArgumentException` | 404 |
| `IllegalStateException` | 403 |
| `InvalidRequestException` | 400 |
| `IllegalStateTransitionException` | 409 |
| `DataIntegrityViolationException` | 409 |
| Bean Validation(@Valid) 실패 | 400 |
