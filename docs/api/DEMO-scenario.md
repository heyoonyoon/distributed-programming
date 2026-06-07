# 보험 시스템 데모 시나리오 (프론트 연동/시연용)

> 백엔드 pane이 **로컬 실서버를 띄워둠**. 프론트에서 바로 붙여 테스트/시연하세요.
> 백엔드가 죽어 있으면 백엔드 pane에 알려주세요(재기동 필요).

## 0. 접속 정보
- **Base URL**: `http://localhost:8080`
- DB: MySQL(도커), 기동 시 시드 데이터 자동 생성.
- 인증: 로그인 시 받은 JWT를 `Authorization: Bearer <token>` 헤더로. 무상태(로그아웃은 프론트에서 토큰 삭제).

### 시드 계정 (비밀번호 전부 `1234`)
| 역할 | 이메일 | 비고 |
|------|--------|------|
| 가입자 | `hong@test.com` | 홍길동, 계좌 110-111-111111 |
| 가입자 | `kim@test.com` | 김보험 |
| 직원 | `staff@test.com` | 이심사(심사팀) |

### 시드 상품
- 의료: id=1 건강플러스(30,000), id=2 실손기본(12,000)
- 자동차: id=3 안심드라이브(45,000), id=4 가벼운자차(28,000)

---

## 1. 데모 플로우 (화면 → API)

### A. 로그인 (Epic 0)
- `POST /auth/login` body `{"email":"hong@test.com","password":"1234"}` → `{ "token": "..." }`
- 내 프로필: `GET /me` (Bearer) → 이름·이메일 등.

### B. 상품 둘러보기 (Epic 1) — 공개(토큰 불필요)
- `GET /products?type=HEALTH` / `GET /products?type=CAR` → 목록
- `GET /products/{id}` → 상세(보장항목 포함)
- ⚠️ `type` 파라미터 **필수**(없으면 400).

### C. 보험 가입 신청 → 직원 심사 → 계약 성립 (Epic 2·3)
1. (가입자) `POST /applications`
   - 의료: `{"productId":1,"medicalHistory":{"currentConditions":"없음","pastHospitalization":"없음","medications":"없음"}}`
   - 자동차: `{"productId":3,"vehicleInfo":{"plateNumber":"12가3456","vehicleType":"승용차","modelYear":2020,"drivingExperienceYears":5}}`
   - → 201, 응답 `applicationId`
2. (직원) `GET /reviews/pending` → 대기 목록(`applicationId`)
3. (직원) `POST /reviews/applications/{applicationId}/confirm` body `{"result":"APPROVED","comment":"정상"}` → **승인 시 계약 자동 생성**
   - 반려: `{"result":"REJECTED","comment":"사유"}`
4. (가입자) `GET /contracts` → 내 계약(`contractId`, status ACTIVE, monthlyPremium)

### D. 보험료 납부 (Epic 3)
- 미납: `GET /contracts/unpaid`, 납부대상: `GET /contracts/payable`
- 납부: `POST /contracts/{contractId}/payments` body `{"method":"CARD","paymentInfo":"1234-5678-9012-3456"}`
  - → 200 `{ "status":"SUCCESS"|"FAILED", ... }` — **실패도 HTTP 200**, 본문 status로 분기.
  - paymentInfo가 `0000`으로 끝나면 결제 실패(데모용).
- 자동이체 등록: `POST /contracts/{contractId}/auto-debit` `{"account":"...","withdrawalDay":15}`

### E. 의료보험 청구 (Epic 4-A) — multipart/form-data
- `POST /claims/health` (Bearer 가입자), form fields:
  `contractId, hospitalName, diagnosisCode, treatmentDate(yyyy-MM-dd), requestAmount, receiptAmount, attachments[](선택)`
- **SIMPLE**(requestAmount < 1,000,000) → 201 `{status:"COMPLETED", complexity:"SIMPLE"}` (즉시지급)
- **COMPLEX**(>= 1,000,000) → 201 `{status:"IN_REVIEW", complexity:"COMPLEX"}` (직원 심사로)
- 첨부 허용 PDF/JPG/PNG, 개당 10MB. 위반 → 400.

### F. 직원 보험금 심사 (Epic 4-B) — 직원 토큰
- 배정 목록: `GET /staff/benefit-reviews` → 본인 배정 미확정 건
- 상세: `GET /staff/benefit-reviews/{claimId}` (비배정 직원 → 409)
- 확정: `POST /staff/benefit-reviews/{claimId}/confirm` `{"result":"APPROVED"|"REJECTED","comment":"..."}`
  - APPROVED → 지급 시도, 응답 `claimStatus` = COMPLETED(성공)/FAILED(실패)
  - REJECTED → REJECTED
- 지급 실패 재시도: `POST /staff/benefit-reviews/{claimId}/retry`
- 수동 재배정: `POST /staff/claims/{claimId}/assign` `{"employeeId":N}`

### G. 자동차사고 접수 (Epic 4-C) — multipart/form-data
- `POST /claims/car-accidents` (가입자, **자동차 계약** 필요), form fields:
  `contractId, accidentDate, accidentLocation, accidentType, vehicleNumber, hasInjury(true/false), injuredCount, attachments[]`
- → 201 `{reportId, status:"PENDING"}`
- 대인사고 `hasInjury=true`면 `injuredCount>=1` 필수(아니면 400).

### H. 보상 조회·분석 (Epic 4-D) — 가입자 토큰
- 처리 현황(진행중): `GET /claims/status` → claimId/claimType(HEALTH·CAR)/status
- 이력(종결건): `GET /claims/history?from=YYYY-MM-DD&to=YYYY-MM-DD`(생략 시 최근 1년) → paidAmount 포함
- 실익분석: `GET /claims/benefit-analysis?contractId={id}` → 총납입/총수령/실익/실익률
  - ⚠️ **가입 6개월 미만 계약은 400** + 안내 메시지(데모 시 정상 동작). 6개월 이상 계약에서만 결과.

---

## 2. 추천 데모 순서 (한 번에 보여주기 좋은 흐름)
1. 가입자 로그인 → 상품 목록 → 의료보험 가입 신청
2. 직원 로그인 → 심사 대기 → **승인**(계약 생성)
3. 가입자 → 계약 확인 → 보험료 납부
4. 가입자 → **의료청구 50만원**(SIMPLE) → 즉시 "지급완료" 화면
5. 가입자 → **의료청구 200만원**(COMPLEX) → "심사중"
6. 직원 → 배정된 심사 목록 → **승인** → 지급완료
7. 가입자 → 보상 **현황/이력** 조회로 결과 확인
8. (자동차) 가입자 → 자동차 가입→직원 승인→**사고 접수**

## 3. 자주 헷갈리는 포인트
- 응답 id 필드명: 신청=`applicationId`, 계약=`contractId`, 청구=`claimId`, 사고=`reportId`.
- 직원 API는 `/staff/**` + 직원 토큰. 가입자 토큰으로 접근 시 401.
- 청구/사고 접수는 **multipart/form-data**(JSON 아님). 첨부 없으면 attachments 생략 가능.
- 납부 실패는 200+`status:FAILED`(에러코드 아님).
- 실익분석 6개월 미만 400은 **정상 동작**(가드). 시연하려면 오래된 계약 필요.
