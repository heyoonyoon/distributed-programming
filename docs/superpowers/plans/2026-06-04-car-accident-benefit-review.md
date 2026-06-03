# 자동차사고 보험금 심사 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 자동차사고 접수건이 의료 복잡건과 같은 보상심사 큐(`/staff/benefit-reviews`)에 떠서, 직원이 승인 시 직접 사정한 보험금을 지급한다.

**Architecture:** 새 review 서브타입을 만들지 않고 기존 `BenefitPaymentReview`의 청구 참조를 `HealthInsuranceClaim` → 부모 `Claim`으로 일반화한다(ADR 0009). 자동차사고는 접수 즉시 review 생성·자동배정되어 `IN_REVIEW`가 되고, 승인 시 직원이 입력한 금액을 `Claim.requestAmount`에 사정 기록한 뒤 기존 `BenefitPayoutService.pay()`로 지급한다.

**Tech Stack:** Spring Boot 4, Java 21, JPA, JUnit5 + AssertJ, `@SpringBootTest @Transactional` 통합 테스트.

**입력 문서(반드시 준수):**
- spec: `docs/superpowers/specs/2026-06-04-car-accident-benefit-review-design.md`
- 용어: `CONTEXT.md` (BenefitPaymentReview/CarAccidentReport 정의는 일반화로 갱신됨)
- 결정: `docs/adr/0009-car-accident-enters-benefit-payment-review.md`

**테스트 실행:** `cd backend && ./gradlew test`

**용어 규약:** Policyholder/InsuranceEmployee/Claim/CarAccidentReport/BenefitPaymentReview 등 CONTEXT.md 표준어를 그대로 사용한다.

---

## File Structure

- `domain/claim/Claim.java` — 사정 금액 기록용 protected 메서드 추가
- `domain/claim/CarAccidentReport.java` — `assessPayout(int)` 추가
- `domain/review/BenefitPaymentReview.java` — claim 참조 타입 `Claim`으로 일반화
- `web/dto/BenefitReviewSummaryResponse.java` — claim 타입 분기(의료/자동차)
- `web/dto/BenefitReviewDetailResponse.java` — claim 타입 분기
- `web/dto/ConfirmBenefitReviewRequest.java` — `payoutAmount` 추가
- `service/CarAccidentService.java` — 접수 시 review 생성·자동배정·IN_REVIEW
- `service/BenefitReviewService.java` — `confirm`에 payoutAmount 분기(자동차 승인)
- `web/controller/StaffReviewController.java` — payoutAmount 전달

---

### Task 1: 자동차사고 사정 금액 도메인 메서드

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/domain/claim/Claim.java`
- Modify: `backend/src/main/java/com/distribution/insurance/domain/claim/CarAccidentReport.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/claim/CarAccidentReportTest.java` (기존 파일에 테스트 추가)

- [ ] **Step 1: 실패 테스트 작성** — `CarAccidentReportTest.java`에 추가

```java
@Test
void assessPayout는_사정금액을_청구금액으로_기록한다() {
    CarAccidentReport report = sampleReport();   // 기존 헬퍼 또는 아래 참고
    report.assessPayout(3_000_000);
    assertThat(report.getRequestAmount()).isEqualTo(3_000_000);
}

@Test
void assessPayout에_0이하면_400성_예외() {
    CarAccidentReport report = sampleReport();
    assertThatThrownBy(() -> report.assessPayout(0))
            .isInstanceOf(com.distribution.insurance.service.InvalidRequestException.class);
}
```

기존 `CarAccidentReportTest`에 `sampleReport()` 헬퍼가 없으면 추가:

```java
private CarAccidentReport sampleReport() {
    return new CarAccidentReport(null, java.time.LocalDate.now(), "서울", "단독", "12가3456", false, 0);
}
```

(import: `static org.assertj.core.api.Assertions.*;`)

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "*CarAccidentReportTest"`
Expected: FAIL — `assessPayout` 메서드 없음(컴파일 에러).

- [ ] **Step 3: 최소 구현**

`Claim.java` 에 protected 메서드 추가(클래스 내부, 다른 mark* 메서드 근처):

```java
/** 접수 시 금액 미정인 건(자동차사고 등)의 사정 금액을 청구금액으로 기록한다(서브클래스 전용). */
protected void recordAssessedAmount(int amount) {
    this.requestAmount = amount;
}
```

`CarAccidentReport.java` 에 public 메서드 추가:

```java
/** 담당자가 사정한 지급 보험금을 기록한다(승인 시). 금액은 0보다 커야 한다. */
public void assessPayout(int amount) {
    if (amount <= 0) {
        throw new com.distribution.insurance.service.InvalidRequestException("사정 보험금은 0보다 커야 합니다.");
    }
    recordAssessedAmount(amount);
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "*CarAccidentReportTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/claim/Claim.java \
        backend/src/main/java/com/distribution/insurance/domain/claim/CarAccidentReport.java \
        backend/src/test/java/com/distribution/insurance/domain/claim/CarAccidentReportTest.java
git commit -m "Epic4: CarAccidentReport.assessPayout — 담당자 사정 금액 기록"
```

---

### Task 2: BenefitPaymentReview를 Claim 참조로 일반화 + DTO 타입 분기

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/domain/review/BenefitPaymentReview.java`
- Modify: `backend/src/main/java/com/distribution/insurance/web/dto/BenefitReviewSummaryResponse.java`
- Modify: `backend/src/main/java/com/distribution/insurance/web/dto/BenefitReviewDetailResponse.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/dto/BenefitReviewResponseMappingTest.java` (신규)

> 주의: `BenefitPaymentReview.claim` 타입을 바꾸면 DTO 매퍼가 `getHospitalName()`을 못 찾아 컴파일이 깨진다. 본 태스크에서 엔티티+DTO를 한 번에 고쳐 빌드 그린을 유지한다.

- [ ] **Step 1: 실패 테스트 작성** — 신규 파일

```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BenefitReviewResponseMappingTest {

    private BenefitPaymentReview carReview() {
        CarAccidentReport report = new CarAccidentReport(null, LocalDate.now(), "서울", "쌍방", "12가3456", true, 2);
        return new BenefitPaymentReview(report);
    }

    @Test
    void 자동차사고_요약은_claimType이_CAR_ACCIDENT이고_hospitalName은_null() {
        var res = BenefitReviewSummaryResponse.from(carReview());
        assertThat(res.claimType()).isEqualTo("CAR_ACCIDENT");
        assertThat(res.hospitalName()).isNull();
        assertThat(res.accidentType()).isEqualTo("쌍방");
    }

    @Test
    void 자동차사고_상세는_사고필드를_담고_의료필드는_null() {
        var res = BenefitReviewDetailResponse.from(carReview());
        assertThat(res.claimType()).isEqualTo("CAR_ACCIDENT");
        assertThat(res.diagnosisCode()).isNull();
        assertThat(res.accidentLocation()).isEqualTo("서울");
        assertThat(res.vehicleNumber()).isEqualTo("12가3456");
        assertThat(res.hasInjury()).isTrue();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "*BenefitReviewResponseMappingTest"`
Expected: FAIL — `BenefitPaymentReview(CarAccidentReport)` 불가 / `claimType()` 없음(컴파일 에러).

- [ ] **Step 3: 최소 구현**

`BenefitPaymentReview.java` — import 및 필드/생성자 변경:

```java
import com.distribution.insurance.domain.claim.Claim;
// (HealthInsuranceClaim import 제거)
```

```java
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", unique = true)
    private Claim claim;

    public BenefitPaymentReview(Claim claim) {
        this.claim = claim;
    }
```

(클래스 docstring을 "복잡한 의료보험 청구와 자동차사고 접수 건의 보험금 지급 심사"로 갱신.)

`BenefitReviewSummaryResponse.java` 전체 교체:

```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.Claim;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.review.BenefitPaymentReview;

public record BenefitReviewSummaryResponse(Long claimId, int requestAmount, String claimType,
                                           String hospitalName, String accidentType, String claimStatus) {
    public static BenefitReviewSummaryResponse from(BenefitPaymentReview review) {
        Claim claim = review.getClaim();
        String hospitalName = claim instanceof HealthInsuranceClaim h ? h.getHospitalName() : null;
        String accidentType = claim instanceof CarAccidentReport c ? c.getAccidentType() : null;
        String claimType = claim instanceof CarAccidentReport ? "CAR_ACCIDENT" : "HEALTH";
        return new BenefitReviewSummaryResponse(
                claim.getId(), claim.getRequestAmount(), claimType, hospitalName, accidentType,
                claim.getStatus().name());
    }
}
```

`BenefitReviewDetailResponse.java` 전체 교체:

```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.Claim;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.review.BenefitPaymentReview;

public record BenefitReviewDetailResponse(Long claimId, int requestAmount, String claimType,
                                          String hospitalName, String diagnosisCode,
                                          String accidentType, String accidentLocation, String vehicleNumber,
                                          Boolean hasInjury, String claimStatus, Long assignedStaffId) {
    public static BenefitReviewDetailResponse from(BenefitPaymentReview review) {
        Claim claim = review.getClaim();
        HealthInsuranceClaim h = claim instanceof HealthInsuranceClaim hc ? hc : null;
        CarAccidentReport c = claim instanceof CarAccidentReport cc ? cc : null;
        return new BenefitReviewDetailResponse(
                claim.getId(), claim.getRequestAmount(), c != null ? "CAR_ACCIDENT" : "HEALTH",
                h != null ? h.getHospitalName() : null,
                h != null ? h.getDiagnosisCode() : null,
                c != null ? c.getAccidentType() : null,
                c != null ? c.getAccidentLocation() : null,
                c != null ? c.getVehicleNumber() : null,
                c != null ? c.isHasInjury() : null,
                claim.getStatus().name(), review.getAssignedStaffId());
    }
}
```

- [ ] **Step 4: 테스트 통과 확인 + 전체 컴파일**

Run: `cd backend && ./gradlew test --tests "*BenefitReviewResponseMappingTest" --tests "*BenefitReviewServiceTest" --tests "*StaffReviewControllerTest"`
Expected: PASS (기존 의료 심사 테스트 회귀 없음)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/review/BenefitPaymentReview.java \
        backend/src/main/java/com/distribution/insurance/web/dto/BenefitReviewSummaryResponse.java \
        backend/src/main/java/com/distribution/insurance/web/dto/BenefitReviewDetailResponse.java \
        backend/src/test/java/com/distribution/insurance/web/dto/BenefitReviewResponseMappingTest.java
git commit -m "Epic4: BenefitPaymentReview를 Claim 참조로 일반화 + 응답 DTO 타입 분기"
```

---

### Task 3: 자동차사고 접수 시 review 생성·자동배정·IN_REVIEW

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/service/CarAccidentService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/CarAccidentServiceTest.java`

- [ ] **Step 1: 실패 테스트 작성/수정** — `CarAccidentServiceTest.java`

기존 테스트 `접수하면_PENDING으로_저장되고_접수번호가_발급된다`를 아래로 **교체**(접수 후 상태가 IN_REVIEW로 바뀜):

```java
@Test
void 접수하면_review가_생성되고_담당자에_배정되어_IN_REVIEW가_된다() {
    InsuranceEmployee staff = userRepository.save(
            new InsuranceEmployee("직원", "e@t.com", "010", "pw", "사고팀", 0));
    Policyholder p = ph("110-123-456789");
    InsuranceContract c = carContract(p);

    CarAccidentReport report = carAccidentService.report(
            p.getId(), c.getId(), LocalDate.now(), "서울", "쌍방", "12가3456", true, 2, List.of());

    assertThat(report.getId()).isNotNull();
    assertThat(reportRepository.findById(report.getId()).orElseThrow().getStatus())
            .isEqualTo(ClaimStatus.IN_REVIEW);
    var review = reviewRepository.findByClaimId(report.getId()).orElseThrow();
    assertThat(review.getAssignedStaffId()).isEqualTo(staff.getId());
}
```

테스트 클래스 상단에 의존성 추가:

```java
@Autowired BenefitPaymentReviewRepository reviewRepository;
```
(import: `com.distribution.insurance.repository.*;` 는 이미 있음)

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "*CarAccidentServiceTest"`
Expected: FAIL — 상태가 PENDING이고 review가 없음.

- [ ] **Step 3: 최소 구현** — `CarAccidentService.java`

생성자/필드에 의존성 추가:

```java
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.repository.BenefitPaymentReviewRepository;
```

```java
    private final BenefitPaymentReviewRepository reviewRepository;
    private final StaffAssignmentService assignmentService;
```

생성자 인자에 `BenefitPaymentReviewRepository reviewRepository, StaffAssignmentService assignmentService` 추가하고 필드 대입.

`report(...)` 내 `reportRepository.save(report);` 직후에 추가:

```java
        // 자동차사고도 보상심사 큐로 진입(ADR 0009): review 생성·자동배정 후 심사중 전이
        BenefitPaymentReview review = reviewRepository.save(new BenefitPaymentReview(report));
        assignmentService.assignAutomatically(review);
        report.markInReview();
```

(기존 직원 전원 접수 알림 루프와 가입자 알림은 그대로 유지.)

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "*CarAccidentServiceTest" --tests "*CarAccidentControllerTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/CarAccidentService.java \
        backend/src/test/java/com/distribution/insurance/service/CarAccidentServiceTest.java
git commit -m "Epic4: 자동차사고 접수 시 보상심사 review 생성·자동배정·IN_REVIEW"
```

---

### Task 4: confirm에 payoutAmount 분기(자동차 승인 시 직원 사정)

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/service/BenefitReviewService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/BenefitReviewServiceTest.java`

- [ ] **Step 1: 실패 테스트 작성** — `BenefitReviewServiceTest.java`에 추가

기존 클래스에 자동차 계약/접수 헬퍼와 테스트 추가:

```java
@Autowired CarAccidentService carAccidentService;

/** 자동차사고를 접수하고 (자동배정된) report를 반환. account로 지급 성공/실패 제어. */
private CarAccidentReport carReport(String account) {
    Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
            "900101-1234567", LocalDate.of(1990, 1, 1), "주소", account));
    var product = productRepository.save(
            new com.distribution.insurance.domain.product.CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
    Long contractId = contractRepository.save(new InsuranceContract(ph, product, 50000, LocalDate.now())).getId();
    return carAccidentService.report(ph.getId(), contractId, LocalDate.now(), "서울", "쌍방", "12가3456", true, 2, List.of());
}

@Test
void 자동차사고_승인시_직원이_사정한_금액으로_지급되어_COMPLETED된다() {
    Long staff = staffId("e@t.com");
    CarAccidentReport report = carReport("110-123-456789");
    reviewService.confirm(staff, report.getId(), ReviewResult.APPROVED, "정상", 3_000_000);

    var saved = claimRepository.findById(report.getId()).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
    assertThat(saved.getRequestAmount()).isEqualTo(3_000_000);
}

@Test
void 자동차사고_승인인데_금액이_없으면_400성_예외() {
    Long staff = staffId("e@t.com");
    CarAccidentReport report = carReport("110-123-456789");
    assertThatThrownBy(() -> reviewService.confirm(staff, report.getId(), ReviewResult.APPROVED, "정상", null))
            .isInstanceOf(InvalidRequestException.class);
}

@Test
void 자동차사고_반려는_금액없이_REJECTED된다() {
    Long staff = staffId("e@t.com");
    CarAccidentReport report = carReport("110-123-456789");
    reviewService.confirm(staff, report.getId(), ReviewResult.REJECTED, "과실 불인정", null);
    assertThat(claimRepository.findById(report.getId()).orElseThrow().getStatus())
            .isEqualTo(ClaimStatus.REJECTED);
}
```

> `staffId(...)`는 첫 직원을 만들고, `carReport`의 자동배정은 유일/최소부하 직원에게 가므로 그 staff에 배정된다. `staffId`를 `carReport`보다 먼저 호출할 것.

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "*BenefitReviewServiceTest"`
Expected: FAIL — 5-인자 `confirm` 없음(컴파일 에러).

- [ ] **Step 3: 최소 구현** — `BenefitReviewService.java`

import 추가:

```java
import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.Claim;
```

기존 `confirm(Long, Long, ReviewResult, String)` 메서드를 아래 2개로 교체:

```java
@Transactional
public BenefitPaymentReview confirm(Long staffId, Long claimId, ReviewResult result, String comment, Integer payoutAmount) {
    BenefitPaymentReview review = requireOwned(staffId, claimId);
    if (review.getResult() != null) {
        throw new IllegalStateTransitionException("이미 확정된 심사입니다.");
    }
    Claim claim = review.getClaim();
    boolean carApprove = result == ReviewResult.APPROVED && claim instanceof CarAccidentReport;
    if (carApprove && (payoutAmount == null || payoutAmount <= 0)) {
        throw new InvalidRequestException("자동차사고 승인 시 지급 보험금(payoutAmount)은 0보다 커야 합니다.");
    }

    review.confirm(result, comment);
    Policyholder ph = claim.getContract().getPolicyholder();

    if (result == ReviewResult.REJECTED) {
        claim.markRejected();
        notificationSender.send(ph.getEmail(), ph.getPhone(),
                "보험금 지급 심사 결과: 반려. 사유: " + comment);
    } else {   // APPROVED
        if (claim instanceof CarAccidentReport car) {
            car.assessPayout(payoutAmount);   // 직원이 사정한 금액을 청구금액으로 기록
        }
        claim.markApproved();
        payoutService.pay(claim);   // 지급 성공 → COMPLETED, 실패 → FAILED + 직원 알림
    }
    return review;
}

/** 금액이 이미 확정된 건(의료 복잡 청구 등)의 확정. */
@Transactional
public BenefitPaymentReview confirm(Long staffId, Long claimId, ReviewResult result, String comment) {
    return confirm(staffId, claimId, result, comment, null);
}
```

> 기존 `HealthInsuranceClaim claim = review.getClaim();` 캐스팅 라인은 제거하고 위처럼 `Claim`으로 다룬다. `markApproved/markRejected/getContract()`는 모두 `Claim` 부모에 존재.

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "*BenefitReviewServiceTest"`
Expected: PASS (의료 기존 6개 + 자동차 신규 3개)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/BenefitReviewService.java \
        backend/src/test/java/com/distribution/insurance/service/BenefitReviewServiceTest.java
git commit -m "Epic4: confirm에 payoutAmount 분기 — 자동차사고 승인 시 직원 사정 금액 지급"
```

---

### Task 5: 웹 계층 연결 — 요청 DTO + 컨트롤러 + 큐 노출 확인

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/web/dto/ConfirmBenefitReviewRequest.java`
- Modify: `backend/src/main/java/com/distribution/insurance/web/controller/StaffReviewController.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/StaffReviewControllerTest.java`

- [ ] **Step 1: 실패 테스트 작성** — `StaffReviewControllerTest.java`에 추가

> 기존 테스트의 인증/요청 패턴(MockMvc, `@AuthenticationPrincipal Long staffId` 주입 방식, 자동차사고 접수 호출)을 그대로 따른다. 자동차사고를 접수한 뒤 `GET /staff/benefit-reviews`에 그 건이 `claimType=CAR_ACCIDENT`로 떠야 하고, `POST /staff/benefit-reviews/{claimId}/confirm`에 `payoutAmount`를 실어 승인하면 200이어야 한다. (구체 요청 빌더는 같은 파일의 기존 의료 심사 테스트를 복제해 자동차 흐름으로 변형.)

핵심 단언:
```java
// GET /staff/benefit-reviews → 자동차건이 목록에 claimType=CAR_ACCIDENT로 존재
// POST .../confirm body={"result":"APPROVED","comment":"정상","payoutAmount":3000000} → 200
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "*StaffReviewControllerTest"`
Expected: FAIL — `payoutAmount` 미전달로 자동차 승인이 400, 또는 요청 필드 미인식.

- [ ] **Step 3: 최소 구현**

`ConfirmBenefitReviewRequest.java` 전체 교체:

```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.ReviewResult;
import jakarta.validation.constraints.NotNull;

public record ConfirmBenefitReviewRequest(@NotNull ReviewResult result, String comment, Integer payoutAmount) {}
```

`StaffReviewController.confirm` 의 서비스 호출을 5-인자로 변경:

```java
BenefitPaymentReview review = reviewService.confirm(
        staffId, claimId, request.result(), request.comment(), request.payoutAmount());
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "*StaffReviewControllerTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/web/dto/ConfirmBenefitReviewRequest.java \
        backend/src/main/java/com/distribution/insurance/web/controller/StaffReviewController.java \
        backend/src/test/java/com/distribution/insurance/web/controller/StaffReviewControllerTest.java
git commit -m "Epic4: 보상심사 확정 요청에 payoutAmount 추가 + 컨트롤러 전달"
```

---

### Task 6: 전체 회귀 검증

- [ ] **Step 1: 전체 테스트**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL — 전 테스트 그린(특히 기존 ClaimService/ClaimQuery/CarAccident/StaffReview 회귀 없음).

- [ ] **Step 2: 그린 아니면** systematic-debugging으로 원인 추적 후 수정·재실행. 통과 전까지 완료 선언 금지.

---

## Self-Review (작성자 점검 결과)

- **spec 커버리지:** 큐 통합(Task 3), 모델 일반화(Task 2), 금액 직원입력·>0 검증(Task 1·4), 지급 재사용(Task 4), DTO 타입 분기(Task 2·5), 요청 payoutAmount(Task 5) — 모두 태스크 존재.
- **회귀 위험 명시:** 기존 `접수하면_PENDING으로_저장` 테스트는 IN_REVIEW로 교체(Task 3). 기존 4-인자 `confirm`는 오버로드로 보존(Task 4)하여 의료 심사 테스트 회귀 없음.
- **타입 일관성:** `payoutAmount`는 `Integer`(nullable)로 전 구간 통일. `assessPayout(int)` / `recordAssessedAmount(int)` / `confirm(...,Integer)` 시그니처 일치.
- **플레이스홀더:** 없음(Task 5 컨트롤러 테스트는 기존 파일 패턴 복제 지시 — 해당 코드베이스 의존이라 의도적).
