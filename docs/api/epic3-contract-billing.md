# API 명세서 — Epic 3 (계약 & 수납)

보험 시스템 백엔드 Epic 3의 REST API. 이 문서는 **현재 구현된 동작 그대로**를 기술한다.
프론트엔드(React)는 이 명세를 단일 출처로 삼아 연동한다.

- Base URL: `http://localhost:8080`
- 인증: **JWT Bearer**. 헤더 `Authorization: Bearer <token>` 필요.
  - `/contracts/**` → **가입자(ROLE_POLICYHOLDER) 전용**
  - 권한 불일치 시 `403 Forbidden`, 토큰 없음 `401 Unauthorized`.
  - 모든 계약 접근은 **본인 계약만** 허용. 타인 계약 접근은 `403 Forbidden`, 없는 계약은 `404 Not Found`.
- 요청/응답 본문: `application/json` (계약서 다운로드만 파일 스트림)
- `@AuthenticationPrincipal`로 토큰의 사용자 id를 해석해 본인 계약을 식별한다(요청 본문에 가입자 식별자를 넣지 않는다).
- 용어: `InsuranceContract`(계약), `Payment`(납부), `Notice`(미납 고지서), `PaymentMethod`(CARD/TRANSFER/AUTO_DEBIT) — CONTEXT.md 따름.
- 유스케이스: UC08 계약 조회, UC07 미납 조회, UC10 보험료 납부, UC16 미납 고지서(시스템 스케줄러, 사용자 API 없음).

---

# A. 계약 조회 (UC08) — 가입자 전용

## A-1. 내 계약 목록

`GET /contracts`

응답 `200 OK` — 본인 계약만. 없으면 `[]`.
```json
[
  {
    "contractId": 10,
    "productName": "건강플러스",
    "productType": "HEALTH",
    "startDate": "2026-06-03",
    "endDate": "2027-06-03",
    "monthlyPremium": 30000,
    "status": "ACTIVE"
  }
]
```
- `productType`: `HEALTH` | `CAR`.
- `status`: `ACTIVE` | `SUSPENDED` | `TERMINATED`.

## A-2. 계약 상세

`GET /contracts/{id}`

응답 `200 OK`
```json
{
  "contractId": 10,
  "productName": "건강플러스",
  "productType": "HEALTH",
  "startDate": "2026-06-03",
  "endDate": "2027-06-03",
  "monthlyPremium": 30000,
  "status": "ACTIVE",
  "paymentMethod": "미등록",
  "coverageItems": [
    { "itemName": "입원비", "coverageLimit": 50000000, "deductible": 100000 }
  ]
}
```
- `paymentMethod`: 자동이체 등록 시 `"AUTO_DEBIT"`, 미등록 시 `"미등록"`.
- **수익자정보·특약사항은 도메인에 캡처되지 않아 응답에서 제외**(설계 결정, grill).

오류
| 상황 | 상태 |
|------|------|
| 없는 계약 | `404 Not Found` |
| 타인 계약 조회 | `403 Forbidden` |

## A-3. 계약서 다운로드

`GET /contracts/{id}/pdf`

응답 `200 OK` — 파일 다운로드.
- 헤더 `Content-Disposition: attachment; filename="contract-{id}.txt"`
- `Content-Type: application/octet-stream`
- 본문: 계약 정보가 담긴 **텍스트 기반 계약서**(현 단계는 PDF가 아닌 텍스트 stub). 계약번호·상품명·계약기간·월보험료·상태 포함.

오류: A-2와 동일(404/403).

---

# B. 미납 조회 (UC07) — 가입자 전용

미납·연체는 **별도 청구 엔티티 없이 온더플라이로 계산**한다(ADR 0004). 아래 비즈니스 로직 참고.

## B-1. 미납(연체) 목록

`GET /contracts/unpaid`

응답 `200 OK` — 본인 계약 중 **연체(연체일수 > 0)** 건만. 없으면 `[]`.
```json
[
  {
    "contractId": 10,
    "productName": "건강플러스",
    "dueDate": "2026-02-01",
    "unpaidPrincipal": 60000,
    "overdueDays": 42,
    "overdueInterest": 690
  }
]
```
- `dueDate`: 가장 오래된 미납 회차의 납부기한.
- `unpaidPrincipal`: 미납 회차 수 × 월보험료.
- `overdueInterest`: `round(미납원금 × 연체일수 × 0.10/365)` (연 10%).

## B-2. 미납 단건 상세

`GET /contracts/{id}/unpaid`

응답 `200 OK` — 단건 `UnpaidResponse`(B-1 항목과 동일 구조). 본인 계약만.

오류
| 상황 | 상태 |
|------|------|
| 없는 계약 | `404 Not Found` |
| 타인 계약 | `403 Forbidden` |

---

# C. 보험료 납부 (UC10) — 가입자 전용

## C-1. 납부 예정 목록

`GET /contracts/payable`

응답 `200 OK` — 본인 계약 중 **미납 회차가 1개 이상**인 건. 없으면 `[]`.
```json
[
  {
    "contractId": 10,
    "productName": "건강플러스",
    "dueDate": "2026-02-01",
    "amount": 60000
  }
]
```
- `amount`: 현재 미납 원금 총액(미납 회차 수 × 월보험료).

## C-2. 보험료 납부

`POST /contracts/{id}/payments`

요청
```json
{ "method": "CARD", "paymentInfo": "1234-5678-9012-3456" }
```
- `method` (필수): `CARD` | `TRANSFER` | `AUTO_DEBIT`.
- `paymentInfo`: 카드/계좌 번호 등 결제 정보. **저장하지 않는다**(로그·DB 미보존).
- 1회 호출 = **한 회차(월보험료) 납부**.

응답 `200 OK` — 성공
```json
{ "paymentId": 31, "status": "SUCCESS", "amount": 30000, "reason": null }
```
응답 `200 OK` — 결제 실패(E1, 게이트웨이 거절)
```json
{ "paymentId": 32, "status": "FAILED", "amount": 30000, "reason": "한도 초과 또는 잔액 부족" }
```
> 결제 실패는 클라이언트 오류가 아니므로 **HTTP 200**으로 응답하고 본문 `status=FAILED` + `reason`으로 구분한다. 실패 건도 `Payment(FAILED)`로 기록되며 **미납 회차를 줄이지 않는다**.
> Mock 게이트웨이 동작: `paymentInfo`가 `"0000"`으로 끝나면 실패, 그 외 성공(결정적). 성공 시 mock 영수증 알림 발송(로그).

오류
| 상황 | 상태 | 비고 |
|------|------|------|
| `method` 누락/허용외 값 | `400 Bad Request` | `@NotNull` / enum 매핑 |
| **납부할 미납이 없음** | `400 Bad Request` | `InvalidRequestException` |
| 없는 계약 | `404 Not Found` | |
| 타인 계약 납부 | `403 Forbidden` | |

## C-3. 자동이체 등록

`POST /contracts/{id}/auto-debit`

요청
```json
{ "account": "110-222-333333", "withdrawalDay": 25 }
```
- `account` (필수, 공백 불가): 출금 계좌.
- `withdrawalDay` (1~31): 매월 출금일.

응답 `200 OK` (본문 없음). 이후 A-2 계약 상세의 `paymentMethod`가 `"AUTO_DEBIT"`로 표기된다. 등록 안내 mock 알림 발송.

오류
| 상황 | 상태 | 비고 |
|------|------|------|
| `account` 공백 / `withdrawalDay` 1~31 벗어남 | `400 Bad Request` | Bean Validation + 도메인 검증 |
| 없는 계약 | `404 Not Found` | |
| 타인 계약 | `403 Forbidden` | |

---

# D. 미납 고지서 (UC16) — 시스템 스케줄러 (사용자 API 없음)

매일 정해진 시각(`cron 0 0 9 * * *`, 오전 9시) 자동 실행. REST 엔드포인트 없음. 동작은 비즈니스 로직 참고.

---

## 비즈니스 로직

### 도메인 모델
- **InsuranceContract**(계약): `startDate`, `endDate`(=startDate+1년, ADR 0005), `status`(ACTIVE/SUSPENDED/TERMINATED), `monthlyPremium`, `policyholder`(참조), `product`(참조), `autoDebit`(@Embeddable, nullable). 메서드 `suspend()`/`terminate()`(상태 전이 가드), `generatePdf()`, `registerAutoDebit()`, `registeredPaymentMethod()`.
- **Payment**(납부): `amount`, `paidAt`, `method`(PaymentMethod), `status`(SUCCESS/FAILED), `contract`(참조). **어느 회차에 대응하는지는 저장하지 않는다**(FIFO 충당, ADR 0004). 팩토리 `Payment.success()/failed()`, `getReceipt()`(FAILED는 영수증 불가).
- **Notice**(미납 고지서): `issuedAt`, `dueDate`, `dueAmount`, `overdueDays`, `overdueInterest`, `isTerminationWarning`, 발송기록(`sentAt`/`delivered`/`attempts`), `contract`(참조). `(contract_id, issued_at)` **유니크 제약**(같은 날 중복 고지 방지). `buildMessage()`, `markSent()`.
- **AutoDebit**(@Embeddable): `account`, `withdrawalDay`(1~31).
- **PaymentMethod**: `CARD` | `TRANSFER` | `AUTO_DEBIT`. **PaymentStatus**: `SUCCESS` | `FAILED`.

### 계약 생성 (UC08 전제, Epic 2 연계)
- `InsuranceContract`는 **가입 심사 승인 시 같은 트랜잭션에서 자동 생성**된다(ADR 0005). `ReviewService.confirm()`에서 결과가 `APPROVED`/`CONDITIONAL`이면 생성, `REJECTED`면 생성 안 함.
- `monthlyPremium = EnrollmentReview.adjustedPremium` (ADR 0003: 결과 분기 없이 한 필드만 읽음). `startDate=승인일`, `endDate=+1년`, `status=ACTIVE`.

### 온더플라이 청구 계산 (UC07/UC10 공통, ADR 0004)
청구 회차를 영속화하지 않고 계약 정보로 매번 계산한다.
- **청구 회차 규약**: 회차 k(0-based)의 납부기한 = `startDate.plusMonths(k)`. 1회차(k=0)는 계약 시작일에 도래. 총 회차 = `MONTHS.between(startDate, endDate)` = 12.
- **발생 회차 수**(asOf): `startDate.plusMonths(k)`가 asOf 이하인 회차 개수(총 회차 상한). 월말 시작(예: 1/31)도 `plusMonths`의 말일 보정과 일치하게 센다.
- **성공 납부 건수**: 해당 계약의 `Payment(SUCCESS)` 개수(`countByContractIdAndStatus`).
- **미납 회차 수** = max(0, 발생 회차 − 성공 납부). **FIFO 충당**: 성공 납부는 가장 오래된 회차부터 채운 것으로 간주하므로, 가장 오래된 미납 회차 기한 = `startDate.plusMonths(성공납부건수)`.
- **연체일수** = asOf > 기한이면 `DAYS.between(기한, asOf)`, 아니면 0.
- **미납 원금** = 미납 회차 × 월보험료. **연체이자** = `round(미납원금 × 연체일수 × 0.10/365)` (연 10% 고정).
- `BillingCalculator.compute(contract, successCount, asOf) → BillingStatus`로 계산. `hasUnpaid()`(미납회차≥1), `isOverdue()`(연체일수>0).

### 미납 조회 (UC07)
- `GET /contracts/unpaid`: 본인 계약 중 `isOverdue()`인 건만(연체분).
- `GET /contracts/{id}/unpaid`: 본인 단건.

### 납부 (UC10)
- `GET /contracts/payable`: 본인 계약 중 `hasUnpaid()`인 건(납부 예정 금액 존재).
- `POST /contracts/{id}/payments`: **미납이 없으면 400**으로 거부. 미납이 있으면 `PaymentGateway`(Mock)로 한 회차(월보험료) 결제. 성공→`Payment(SUCCESS)` 저장 + 영수증 알림, 실패→`Payment(FAILED)` 저장(예외 없이 결과 반환, 미납 불변).
- `POST /contracts/{id}/auto-debit`: 계약에 자동이체(계좌·출금일) 임베디드 저장.

### 미납 고지서 발송 (UC16)
- **일일 스케줄러**(`@Scheduled(cron="0 0 9 * * *")`)가 `NoticeService.issueOverdueNotices(asOf)`에 위임. 스케줄러 메서드에는 로직을 두지 않고 시작/완료/실패 로그만.
- **탐색**: 전체 계약 중 `status=ACTIVE`이고 `isOverdue()`인 건. ADR 0004 `BillingCalculator`를 재사용(미납 계산 중복 없음).
- **중복 방지**: 같은 날 이미 고지한 계약(`existsByContractIdAndIssuedAt`)은 건너뜀 + DB `(contract_id, issued_at)` 유니크 제약으로 동시 실행 시 중복 차단(`DataIntegrityViolationException` 시 skip).
- **발송**: `NotificationSender`로 가입자에게 발송, **최대 3회 재시도**(E1). 최종 실패 시 관리자 로그 + `delivered=false` 기록. 발송 결과(`sentAt`/`delivered`/`attempts`)를 Notice에 저장(UC16 5단계).
- **30일 초과 연체(A1)**: `isTerminationWarning=true`, 고지서 본문에 해지예고 문구, **직원 전원에게 별도 알림**(best-effort — 직원 알림 실패가 배치를 롤백하지 않음).
- **배치 안정성**: 계약 단위 트랜잭션 격리(`ContractNoticeIssuer.issueIfOverdue`가 계약별 `@Transactional`). 한 계약 실패가 전체 일일 배치를 중단·롤백하지 않는다.

### 의도적으로 제외한 범위 (YAGNI / 설계 결정)
- 청구 회차(Installment) 엔티티(ADR 0004로 미도입), 수익자·특약 필드(도메인 미캡처), 실제 PDF 렌더링(텍스트 stub), 부분 납부·회차별 상태, 결제 동시성 락(@Version/row lock), 연체 누적 시 자동 해지(UC16은 경고만), 실제 PG 연동(Mock).

### 에러 매핑 (전역, Epic 2와 동일)
| 예외 | 상태 |
|------|------|
| `IllegalArgumentException` | 404 |
| `IllegalStateException` | 403 |
| `InvalidRequestException` | 400 |
| `IllegalStateTransitionException` | 409 |
| `DataIntegrityViolationException` | 409 |
| Bean Validation(@Valid) 실패 | 400 |
