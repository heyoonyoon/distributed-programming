# Epic 3 — 계약 & 수납 설계

> 작성일: 2026-06-03
> 범위: UC08(계약조회), UC07(미납조회), UC10(납부), UC16(미납 고지서)
> 선행: Epic 2까지 `EnrollmentReview` 확정. `InsuranceContract`는 아직 미생성 — Epic 3의 출발점.

## 0. 용어
CONTEXT.md 단일 출처를 따른다. 신규 용어는 grill-with-docs에서 CONTEXT.md에 반영한다.
- `InsuranceContract`, `Payment`, `Notice` (다이어그램 03_contract 그대로)
- 신규 개념: 청구 회차(installment, 온더플라이), 연체이자(overdue interest), 자동이체(AUTO_DEBIT)

## 1. 온더플라이 청구 모델 (핵심 결정)

별도 청구/회차 엔티티를 두지 **않는다**. 다이어그램(`Payment`, `Notice`)을 유지하고 미납을 계산으로 도출한다.

- **청구 회차**: 계약 `startDate`부터 매월 같은 일자에 `monthlyPremium` 청구.
  n번째 회차 기한 = `startDate + n개월` (n=0,1,2,…, `endDate` 이내).
- **발생 회차 수**(asOf=오늘) = 기한이 오늘 이하인 회차 개수.
- **성공 납부 건수** = 해당 계약의 `Payment` 중 `status=SUCCESS` 개수.
- **미납 회차 수** = 발생 회차 수 − 성공 납부 건수 (음수면 0).
- **FIFO 충당**: 성공 납부는 가장 오래된 회차부터 채운 것으로 간주. Payment를 특정 회차에 명시 매핑하지 않는다.
- **연체일수** = 가장 오래된 미납 회차 기한 ~ 오늘.
- **미납 원금** = 미납 회차 수 × `monthlyPremium`.
- **연체이자** = 미납 원금 × 연체일수 × 일이율.
  - 일이율 = **연 10% / 365 ≈ 0.0274%** (고정값; ADR로 기록).

## 2. 이슈 분해 (3개 브랜치/PR)

### 이슈 A — 계약 생성 + 조회 (UC08)
- `InsuranceContract` 도메인: `contractId`, `startDate`, `endDate`, `status`(ACTIVE/SUSPENDED/TERMINATED), `monthlyPremium`.
  메서드: `getContractDetail()`, `suspend()`, `terminate()`, `generatePdf()`.
- **계약 자동 생성**: `ReviewService.confirm()`에서 결과가 APPROVED/CONDITIONAL이면 **같은 트랜잭션에서** `InsuranceContract` 생성.
  - `monthlyPremium = EnrollmentReview.adjustedPremium` (ADR 0003: 분기 없이 한 필드만 읽음).
  - `startDate = 승인일`, `endDate = startDate + 1년` (기본 기간 1년, 고정값).
  - `status = ACTIVE`.
  - REJECTED는 계약 미생성.
- API (Policyholder 전용, 본인 계약만):
  - `GET /contracts` — 유효 계약 목록(계약번호, 상품명, 보험종류, 계약기간, 월보험료). 없으면 빈 목록.
  - `GET /contracts/{id}` — 상세. 본인 아니면 거부.
    - **노출 항목(도메인 실재분만)**: 보장항목·보장금액(상품 CoverageItem), 월보험료, 계약기간, 상태, 결제수단(등록된 자동이체 기준, 없으면 "미등록").
    - **제외**: UC08 텍스트의 수익자정보·특약사항은 도메인에 캡처되지 않으므로 이번 범위에서 제외(YAGNI, 추후 필드 추가로 확장). grill 결정.
  - `GET /contracts/{id}/pdf` — 계약서 파일 생성·다운로드(텍스트 기반 간단 PDF).

### 이슈 B — 미납조회(UC07) + 납부(UC10)
- 온더플라이 계산 서비스(1절).
- API (Policyholder 전용, 본인 계약만):
  - `GET /contracts/unpaid` — 미납 목록(계약명, 납부기한, 미납금액, 연체일수, 연체이자). 없으면 빈 목록.
  - `GET /contracts/{id}/unpaid` — 단건 상세 + 납부방법 안내.
  - `GET /contracts/payable` — 납부 예정 목록(계약명, 납부기한, 납부금액).
  - `POST /contracts/{id}/payments` — 납부. body: 결제수단(CARD/TRANSFER/AUTO_DEBIT).
    - `MockPaymentGateway`로 처리. 성공 시 `Payment(status=SUCCESS)` 기록 + 영수증 발송(`NotificationSender`).
    - **실패 시뮬레이션**: 특정 조건(예: 카드번호/금액 플래그)에서 FAILED 반환 → `Payment(status=FAILED)` 기록 + 실패 메시지(E1).
  - `POST /contracts/{id}/auto-debit` — 자동이체 등록(출금계좌·출금일 저장). 다이어그램에 전용 엔티티 없음 → 계약에 단순 필드/임베디드로 저장.

### 이슈 C — 미납 고지서 스케줄러 (UC16)
- `Notice` 도메인: `noticeId`, `issuedAt`, `dueAmount`, `overdueDays`, `isTerminationWarning`. 메서드 `send(email, phone)`.
- `@Scheduled` 매일 지정 시각 실행:
  1. 미납 계약 탐색(1절 계산으로 미납 회차 ≥ 1).
  2. 계약별 `Notice` 생성(미납금액, 연체일수, 연체이자, 납부방법 안내).
  3. `NotificationSender`로 이메일/문자 발송, 발송 기록 저장.
  4. **A1**: 연체 30일 초과 시 `isTerminationWarning=true` + 해지예고 문구 + 직원 알림.
  - **E1(재시도 3회)**: 발송 실패 시 재시도는 로직만(로그 수준). 최종 실패 시 관리자 알림+기록.

## 3. 권한
- Policyholder 전용 엔드포인트, 본인 계약만 접근(타인 계약 403/404).
- 스케줄러는 시스템 내부 실행(인증 없음).

## 4. 테스트 전략 (TDD)
- 도메인: 청구 회차 계산, 미납 회차 도출, FIFO 충당, 연체일수/이자, adjustedPremium→monthlyPremium 매핑, 계약 상태 전이.
- 서비스: 승인→계약 자동 생성(REJECTED 미생성), 납부 성공/실패, 본인 계약 격리.
- 스케줄러: 미납 탐색, 30일 경고 분기, 발송 기록.

## 5. 확정 기본값
- 연체 일이율: 연 10% / 365.
- 계약 기본 기간: 1년.
- `ReviewService.confirm()` 수정하여 계약 자동 생성(Epic 2 코드 변경).
