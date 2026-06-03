# Epic 2 · 이슈 B — 보험 가입 심사(UC13) + 자동차 사고이력 조회(UC15) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** InsuranceEmployee가 심사 대기(PENDING) 신청 목록을 조회하고, 상세(자동차건은 mock 사고이력 동봉)를 확인한 뒤, 승인/조건부승인(할증)/반려를 확정하여 EnrollmentReview를 저장하고 Application 상태를 전이시킨다.

**Architecture:** Review 추상 부모를 single-table 상속으로 도입(향후 Epic 4 BenefitPaymentReview 확장 대비). EnrollmentReview만 구현. 조건부승인은 ReviewResult.CONDITIONAL로만 표현하고 ApplicationStatus는 APPROVED로 전이한다(ADR 0003). adjustedPremium은 결과와 무관하게 항상 최종 보험료를 담는다(ADR 0003). 사고이력은 mock 클라이언트로 시뮬레이션한다(UC15).

**Tech Stack:** Spring Boot, Spring Data JPA, Spring Security(JWT), Bean Validation, Lombok, JUnit5 + MockMvc + H2.

**선행:** 이슈 A(가입 요청)가 머지되어 InsuranceApplication/ApplicationStatus/ApplicationRepository/NotificationSender/InvalidRequestException/IllegalStateTransitionException이 존재해야 한다.

**사전 준수 (CLAUDE.md):**
- spec: `docs/superpowers/specs/2026-06-03-epic2-enrollment-underwriting-design.md`
- 용어: `CONTEXT.md` (EnrollmentReview, ReviewResult, AccidentHistory, InsuranceEmployee). 동의어 금지.
- 결정: ADR 0003(조건부승인은 ReviewResult에만 / adjustedPremium은 항상 최종 보험료).

**프로젝트 규약 메모:**
- 경로 `/reviews`, 역할 `ROLE_EMPLOYEE`.
- 400=InvalidRequestException, 409=IllegalStateTransitionException, 404=IllegalArgumentException, 403=IllegalStateException(이슈 A에서 매핑 완료).

---

## File Structure

- Create: `domain/review/ReviewResult.java` — APPROVED/CONDITIONAL/REJECTED enum.
- Create: `domain/review/Review.java` — 심사 추상 부모(single-table).
- Create: `domain/review/AccidentHistory.java` — 사고이력 @Embeddable.
- Create: `domain/review/EnrollmentReview.java` — 가입 심사 엔티티(applySurcharge, confirm).
- Modify: `domain/application/InsuranceApplication.java` — markApproved()/markRejected() 상태 전이 메서드 추가.
- Create: `repository/ReviewRepository.java` — EnrollmentReview 저장/조회.
- Create: `service/AccidentHistoryClient.java` + `service/MockAccidentHistoryClient.java` — UC15 mock.
- Create: `service/ReviewService.java` — 대기 목록/상세/확정 조율.
- Create: `web/dto/PendingApplicationResponse.java` — 심사 대기 목록.
- Create: `web/dto/ReviewDetailResponse.java` — 심사 상세(추가정보 + 사고이력 참조).
- Create: `web/dto/ConfirmReviewRequest.java` — 심사 확정 요청.
- Create: `web/dto/ReviewResultResponse.java` — 심사 확정 응답.
- Create: `web/controller/ReviewController.java` — `/reviews` 엔드포인트.
- Modify: `security/SecurityConfig.java` — `/reviews/**` 권한.

---

## Task 1: ReviewResult enum

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/review/ReviewResult.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/review/ReviewResultTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.review;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReviewResultTest {

    @Test
    void 세_가지_결과값을_가진다() {
        assertThat(ReviewResult.values())
                .containsExactlyInAnyOrder(
                        ReviewResult.APPROVED,
                        ReviewResult.CONDITIONAL,
                        ReviewResult.REJECTED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ReviewResultTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.domain.review;

/** 가입 심사 결과. CONDITIONAL은 할증 부과 조건부 승인. */
public enum ReviewResult {
    APPROVED, CONDITIONAL, REJECTED
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ReviewResultTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/review/ReviewResult.java backend/src/test/java/com/distribution/insurance/domain/review/ReviewResultTest.java
git commit -m "feat(epic2): ReviewResult enum(APPROVED/CONDITIONAL/REJECTED)"
```

---

## Task 2: AccidentHistory @Embeddable

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/review/AccidentHistory.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/review/AccidentHistoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.review;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class AccidentHistoryTest {

    @Test
    void 사고이력_필드를_보존한다() {
        LocalDateTime now = LocalDateTime.now();
        AccidentHistory h = new AccidentHistory(2, 5_000_000, "VALID", now);
        assertThat(h.getAccidentCount()).isEqualTo(2);
        assertThat(h.getTotalPaidAmount()).isEqualTo(5_000_000);
        assertThat(h.getLicenseStatus()).isEqualTo("VALID");
        assertThat(h.getFetchedAt()).isEqualTo(now);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*AccidentHistoryTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.domain.review;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 금융감독원에서 조회되는 자동차 사고이력(UC15). 텍스트 구현에서는 mock. */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AccidentHistory {

    private int accidentCount;     // 최근 3년 사고 건수
    private int totalPaidAmount;   // 보험금 지급 총액
    private String licenseStatus;  // VALID, SUSPENDED, REVOKED
    private LocalDateTime fetchedAt;

    public AccidentHistory(int accidentCount, int totalPaidAmount,
                           String licenseStatus, LocalDateTime fetchedAt) {
        this.accidentCount = accidentCount;
        this.totalPaidAmount = totalPaidAmount;
        this.licenseStatus = licenseStatus;
        this.fetchedAt = fetchedAt;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*AccidentHistoryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/review/AccidentHistory.java backend/src/test/java/com/distribution/insurance/domain/review/AccidentHistoryTest.java
git commit -m "feat(epic2): AccidentHistory 임베디드(UC15 사고이력)"
```

---

## Task 3: InsuranceApplication 상태 전이 메서드 추가 (markApproved/markRejected)

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/domain/application/InsuranceApplication.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/application/ApplicationTransitionTest.java`

심사 확정이 호출할 상태 전이. PENDING에서만 전이 가능, 그 외는 409. CONDITIONAL 결과도 Application은 APPROVED로 전이된다(ADR 0003).

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.application;

import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTransitionTest {

    private InsuranceApplication pendingHealthApp() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        var product = new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
        return new InsuranceApplication(ph, product, null, new MedicalHistory("없음", "없음", "없음"));
    }

    @Test
    void PENDING에서_승인되면_APPROVED() {
        InsuranceApplication app = pendingHealthApp();
        app.markApproved();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    void PENDING에서_반려되면_REJECTED() {
        InsuranceApplication app = pendingHealthApp();
        app.markRejected();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void 이미_심사된_건을_다시_전이하면_예외() {
        InsuranceApplication app = pendingHealthApp();
        app.markApproved();
        assertThatThrownBy(app::markRejected)
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(app::markApproved)
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ApplicationTransitionTest"`
Expected: 컴파일 실패 (markApproved/markRejected 없음).

- [ ] **Step 3: Write minimal implementation**

`InsuranceApplication`의 `cancel()` 메서드 뒤에 추가:
```java
    /** 심사 승인 확정 시 호출(조건부승인 포함 — ADR 0003). PENDING에서만 전이. */
    public void markApproved() {
        requirePending();
        this.status = ApplicationStatus.APPROVED;
    }

    /** 심사 반려 확정 시 호출. PENDING에서만 전이. */
    public void markRejected() {
        requirePending();
        this.status = ApplicationStatus.REJECTED;
    }

    private void requirePending() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new IllegalStateTransitionException("심사 대기 상태의 신청만 심사할 수 있습니다.");
        }
    }
```

> `IllegalStateTransitionException`은 이미 import 되어 있다(cancel()에서 사용).

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ApplicationTransitionTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/application/InsuranceApplication.java backend/src/test/java/com/distribution/insurance/domain/application/ApplicationTransitionTest.java
git commit -m "feat(epic2): Application 심사 상태 전이(markApproved/markRejected)"
```

---

## Task 4: Review 추상 부모 + EnrollmentReview 엔티티

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/review/Review.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/review/EnrollmentReview.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/review/EnrollmentReviewTest.java`

EnrollmentReview 계약:
- 생성: `new EnrollmentReview(InsuranceApplication app, InsuranceEmployee reviewer)`.
- `confirm(ReviewResult result, String comment, Double surchargeRate, int basePremium)`:
  - APPROVED: surchargeRate 입력 금지(있으면 InvalidRequestException), adjustedPremium = basePremium.
  - CONDITIONAL: surchargeRate > 0 필수(아니면 InvalidRequestException), adjustedPremium = round(basePremium*(1+rate)).
  - REJECTED: surchargeRate 입력 금지, adjustedPremium 의미 없음(0).
  - result/comment/reviewedAt 설정.
- `attachAccidentHistory(AccidentHistory h)`: 자동차건 참조 정보 보관.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.review;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class EnrollmentReviewTest {

    private InsuranceApplication app() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        var product = new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
        return new InsuranceApplication(ph, product, null, new MedicalHistory("없음", "없음", "없음"));
    }

    private InsuranceEmployee reviewer() {
        return new InsuranceEmployee("심사역", "e@test.com", "010", "pw", "심사팀", 0);
    }

    @Test
    void 일반승인은_adjustedPremium이_basePremium과_같다() {
        EnrollmentReview r = new EnrollmentReview(app(), reviewer());
        r.confirm(ReviewResult.APPROVED, "이상 없음", null, 30000);
        assertThat(r.getResult()).isEqualTo(ReviewResult.APPROVED);
        assertThat(r.getAdjustedPremium()).isEqualTo(30000);
        assertThat(r.getReviewedAt()).isNotNull();
    }

    @Test
    void 조건부승인은_할증된_보험료를_계산한다() {
        EnrollmentReview r = new EnrollmentReview(app(), reviewer());
        r.confirm(ReviewResult.CONDITIONAL, "사고이력 다수", 0.2, 30000);
        assertThat(r.getSurchargeRate()).isEqualTo(0.2);
        assertThat(r.getAdjustedPremium()).isEqualTo(36000);
    }

    @Test
    void 조건부승인인데_할증율이_없으면_예외() {
        EnrollmentReview r = new EnrollmentReview(app(), reviewer());
        assertThatThrownBy(() -> r.confirm(ReviewResult.CONDITIONAL, "사유", null, 30000))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> r.confirm(ReviewResult.CONDITIONAL, "사유", 0.0, 30000))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 일반승인인데_할증율이_있으면_예외() {
        EnrollmentReview r = new EnrollmentReview(app(), reviewer());
        assertThatThrownBy(() -> r.confirm(ReviewResult.APPROVED, "사유", 0.2, 30000))
                .isInstanceOf(InvalidRequestException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*EnrollmentReviewTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

`Review.java`:
```java
package com.distribution.insurance.domain.review;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "review_type")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime reviewedAt;

    @Enumerated(EnumType.STRING)
    private ReviewResult result;

    @Column(length = 500)
    private String comment;

    protected void recordResult(ReviewResult result, String comment) {
        this.result = result;
        this.comment = comment;
        this.reviewedAt = LocalDateTime.now();
    }
}
```

`EnrollmentReview.java`:
```java
package com.distribution.insurance.domain.review;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 가입 심사(UC13). 자동차건은 AccidentHistory를 참조한다. */
@Entity
@DiscriminatorValue("ENROLLMENT")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EnrollmentReview extends Review {

    private double surchargeRate;   // 조건부승인 시에만 > 0
    private int adjustedPremium;    // 항상 최종 보험료(ADR 0003)

    @Embedded
    private AccidentHistory accidentHistory;  // 자동차건만, nullable

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private InsuranceApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private InsuranceEmployee reviewer;

    public EnrollmentReview(InsuranceApplication application, InsuranceEmployee reviewer) {
        this.application = application;
        this.reviewer = reviewer;
    }

    public void attachAccidentHistory(AccidentHistory accidentHistory) {
        this.accidentHistory = accidentHistory;
    }

    /** 심사 확정. adjustedPremium은 결과와 무관하게 최종 보험료를 담는다(ADR 0003). */
    public void confirm(ReviewResult result, String comment, Double surchargeRate, int basePremium) {
        switch (result) {
            case APPROVED -> {
                if (surchargeRate != null) {
                    throw new InvalidRequestException("일반 승인에는 할증율을 입력할 수 없습니다.");
                }
                this.surchargeRate = 0.0;
                this.adjustedPremium = basePremium;
            }
            case CONDITIONAL -> {
                if (surchargeRate == null || surchargeRate <= 0) {
                    throw new InvalidRequestException("조건부 승인은 0보다 큰 할증율이 필요합니다.");
                }
                this.surchargeRate = surchargeRate;
                this.adjustedPremium = applySurcharge(basePremium, surchargeRate);
            }
            case REJECTED -> {
                if (surchargeRate != null) {
                    throw new InvalidRequestException("반려에는 할증율을 입력할 수 없습니다.");
                }
                this.surchargeRate = 0.0;
                this.adjustedPremium = 0;
            }
        }
        recordResult(result, comment);
    }

    private static int applySurcharge(int basePremium, double rate) {
        return (int) Math.round(basePremium * (1 + rate));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*EnrollmentReviewTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/review/Review.java backend/src/main/java/com/distribution/insurance/domain/review/EnrollmentReview.java backend/src/test/java/com/distribution/insurance/domain/review/EnrollmentReviewTest.java
git commit -m "feat(epic2): Review 추상부모 + EnrollmentReview(confirm·할증 규칙)"
```

---

## Task 5: ReviewRepository (+ 영속성 테스트)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/repository/ReviewRepository.java`
- Test: `backend/src/test/java/com/distribution/insurance/repository/ReviewRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.review.EnrollmentReview;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReviewRepositoryTest {

    @Autowired ReviewRepository reviewRepository;
    @Autowired EntityManager em;

    @Test
    void 심사를_저장하고_신청별로_조회한다() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        em.persist(ph);
        InsuranceProduct product = new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
        em.persist(product);
        InsuranceApplication app = new InsuranceApplication(ph, product, null,
                new MedicalHistory("없음", "없음", "없음"));
        em.persist(app);
        InsuranceEmployee emp = new InsuranceEmployee("심사역", "e@test.com", "010", "pw", "심사팀", 0);
        em.persist(emp);

        EnrollmentReview review = new EnrollmentReview(app, emp);
        review.confirm(ReviewResult.APPROVED, "이상 없음", null, 30000);
        reviewRepository.save(review);
        em.flush();
        em.clear();

        assertThat(reviewRepository.findByApplicationId(app.getId())).isPresent();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ReviewRepositoryTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.review.EnrollmentReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<EnrollmentReview, Long> {

    Optional<EnrollmentReview> findByApplicationId(Long applicationId);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ReviewRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/repository/ReviewRepository.java backend/src/test/java/com/distribution/insurance/repository/ReviewRepositoryTest.java
git commit -m "feat(epic2): ReviewRepository(신청별 심사 조회)"
```

---

## Task 6: AccidentHistoryClient (UC15 mock)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/AccidentHistoryClient.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/MockAccidentHistoryClient.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/MockAccidentHistoryClientTest.java`

mock 규칙(결정적): ssn 해시로 더미 산출 — 사고건수 = (ssn 숫자합 % 4), 지급총액 = 사고건수 * 1,000,000, 면허상태 = 사고건수>=3 ? "SUSPENDED" : "VALID". fetchedAt = now. 같은 ssn은 항상 같은 값.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.review.AccidentHistory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockAccidentHistoryClientTest {

    private final AccidentHistoryClient client = new MockAccidentHistoryClient();

    @Test
    void 같은_주민번호는_항상_같은_결과를_준다() {
        AccidentHistory a = client.fetch("900101-1234567");
        AccidentHistory b = client.fetch("900101-1234567");
        assertThat(a.getAccidentCount()).isEqualTo(b.getAccidentCount());
        assertThat(a.getLicenseStatus()).isEqualTo(b.getLicenseStatus());
    }

    @Test
    void 사고건수는_0이상_3이하이고_지급총액과_정합() {
        AccidentHistory h = client.fetch("910101-2345678");
        assertThat(h.getAccidentCount()).isBetween(0, 3);
        assertThat(h.getTotalPaidAmount()).isEqualTo(h.getAccidentCount() * 1_000_000);
        assertThat(h.getFetchedAt()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*MockAccidentHistoryClientTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

`AccidentHistoryClient.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.review.AccidentHistory;

/** 금융감독원 사고이력 조회(UC15). 텍스트 구현에서는 mock. */
public interface AccidentHistoryClient {
    AccidentHistory fetch(String ssn);
}
```

`MockAccidentHistoryClient.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.review.AccidentHistory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 결정적 더미 사고이력. 같은 ssn은 항상 같은 값. */
@Component
public class MockAccidentHistoryClient implements AccidentHistoryClient {

    @Override
    public AccidentHistory fetch(String ssn) {
        int digitSum = 0;
        for (char c : ssn.toCharArray()) {
            if (Character.isDigit(c)) {
                digitSum += c - '0';
            }
        }
        int accidentCount = digitSum % 4;                 // 0~3
        int totalPaid = accidentCount * 1_000_000;
        String license = accidentCount >= 3 ? "SUSPENDED" : "VALID";
        return new AccidentHistory(accidentCount, totalPaid, license, LocalDateTime.now());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*MockAccidentHistoryClientTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/AccidentHistoryClient.java backend/src/main/java/com/distribution/insurance/service/MockAccidentHistoryClient.java backend/src/test/java/com/distribution/insurance/service/MockAccidentHistoryClientTest.java
git commit -m "feat(epic2): UC15 사고이력 mock 클라이언트"
```

---

## Task 7: ReviewService (대기 목록 / 상세 / 확정)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/ReviewService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/ReviewServiceTest.java`

서비스 계약:
- `List<InsuranceApplication> pendingApplications()` — status=PENDING 목록.
- `ReviewDetail detail(Long applicationId)` — 신청 조회(없으면 404). 자동차건이면 `accidentHistoryClient.fetch(applicant.getSsn())`로 사고이력 조회해 함께 반환. (반환 타입은 Task 8 DTO 대신, 여기서는 application + nullable accidentHistory를 담는 내부 record.)
- `EnrollmentReview confirm(Long reviewerId, Long applicationId, ReviewResult result, String comment, Double surchargeRate)`:
  - reviewer(InsuranceEmployee) 조회(아니면 404), 신청 조회(없으면 404).
  - 이미 심사된(비PENDING) 건이면 IllegalStateTransitionException(409) — markApproved/markRejected가 보장.
  - EnrollmentReview 생성, 자동차건이면 사고이력 attach, `confirm(...)` 호출(basePremium = product.getBasePremium()).
  - 결과에 따라 application.markApproved()(APPROVED/CONDITIONAL) 또는 markRejected()(REJECTED).
  - 저장 후 결과 통보 발송(NotificationSender).

내부 record `ReviewDetail(InsuranceApplication application, AccidentHistory accidentHistory)`를 ReviewService에 정적 중첩으로 둔다.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.application.*;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.review.EnrollmentReview;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ApplicationRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.ReviewRepository;
import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ReviewServiceTest {

    @Autowired ReviewService reviewService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ReviewRepository reviewRepository;

    private Policyholder ph() {
        return userRepository.save(new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
    }

    private InsuranceEmployee emp() {
        return userRepository.save(new InsuranceEmployee("심사역", "e@test.com", "010", "pw", "심사팀", 0));
    }

    private InsuranceApplication healthApp(Policyholder ph) {
        InsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120));
        return applicationRepository.save(
                new InsuranceApplication(ph, product, null, new MedicalHistory("없음", "없음", "없음")));
    }

    private InsuranceApplication carApp(Policyholder ph) {
        InsuranceProduct product = productRepository.save(
                new CarInsuranceProduct("안심드라이브", "대인대물", 45000, "승용차", "가족한정"));
        return applicationRepository.save(new InsuranceApplication(
                ph, product, new VehicleInfo("12가3456", "승용차", 2020, 5), null));
    }

    @Test
    void 대기목록은_PENDING만_반환한다() {
        InsuranceApplication app = healthApp(ph());
        assertThat(reviewService.pendingApplications()).extracting(InsuranceApplication::getId)
                .contains(app.getId());
    }

    @Test
    void 자동차건_상세는_사고이력을_동봉한다() {
        InsuranceApplication app = carApp(ph());
        ReviewService.ReviewDetail detail = reviewService.detail(app.getId());
        assertThat(detail.accidentHistory()).isNotNull();
    }

    @Test
    void 의료건_상세는_사고이력이_없다() {
        InsuranceApplication app = healthApp(ph());
        ReviewService.ReviewDetail detail = reviewService.detail(app.getId());
        assertThat(detail.accidentHistory()).isNull();
    }

    @Test
    void 조건부승인_확정시_Application은_APPROVED_보험료는_할증() {
        InsuranceApplication app = healthApp(ph());
        InsuranceEmployee reviewer = emp();

        EnrollmentReview review = reviewService.confirm(
                reviewer.getId(), app.getId(), ReviewResult.CONDITIONAL, "사고이력 다수", 0.2);

        assertThat(review.getAdjustedPremium()).isEqualTo(36000);
        assertThat(applicationRepository.findById(app.getId()).get().getStatus())
                .isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    void 이미_심사된_건을_다시_확정하면_409성_예외() {
        InsuranceApplication app = healthApp(ph());
        InsuranceEmployee reviewer = emp();
        reviewService.confirm(reviewer.getId(), app.getId(), ReviewResult.APPROVED, "이상 없음", null);

        assertThatThrownBy(() -> reviewService.confirm(
                reviewer.getId(), app.getId(), ReviewResult.REJECTED, "재심사", null))
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ReviewServiceTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.application.ApplicationStatus;
import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.review.AccidentHistory;
import com.distribution.insurance.domain.review.EnrollmentReview;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.repository.ApplicationRepository;
import com.distribution.insurance.repository.ReviewRepository;
import com.distribution.insurance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ApplicationRepository applicationRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AccidentHistoryClient accidentHistoryClient;
    private final NotificationSender notificationSender;

    public ReviewService(ApplicationRepository applicationRepository,
                         ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         AccidentHistoryClient accidentHistoryClient,
                         NotificationSender notificationSender) {
        this.applicationRepository = applicationRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.accidentHistoryClient = accidentHistoryClient;
        this.notificationSender = notificationSender;
    }

    /** 심사 상세 + (자동차건) 사고이력 참조 정보. */
    public record ReviewDetail(InsuranceApplication application, AccidentHistory accidentHistory) {}

    @Transactional(readOnly = true)
    public List<InsuranceApplication> pendingApplications() {
        return applicationRepository.findByStatus(ApplicationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public ReviewDetail detail(Long applicationId) {
        InsuranceApplication app = requireApplication(applicationId);
        AccidentHistory history = null;
        if (app.getProduct() instanceof CarInsuranceProduct) {
            history = accidentHistoryClient.fetch(app.getApplicant().getSsn());
        }
        return new ReviewDetail(app, history);
    }

    @Transactional
    public EnrollmentReview confirm(Long reviewerId, Long applicationId,
                                    ReviewResult result, String comment, Double surchargeRate) {
        InsuranceEmployee reviewer = requireEmployee(reviewerId);
        InsuranceApplication app = requireApplication(applicationId);

        EnrollmentReview review = new EnrollmentReview(app, reviewer);
        if (app.getProduct() instanceof CarInsuranceProduct) {
            review.attachAccidentHistory(accidentHistoryClient.fetch(app.getApplicant().getSsn()));
        }
        review.confirm(result, comment, surchargeRate, app.getProduct().getBasePremium());

        if (result == ReviewResult.REJECTED) {
            app.markRejected();
        } else {
            app.markApproved();
        }
        reviewRepository.save(review);

        notificationSender.send(app.getApplicant().getEmail(), app.getApplicant().getPhone(),
                "가입 심사가 완료되었습니다. 결과: " + result.name()
                        + (result == ReviewResult.CONDITIONAL
                           ? " (조정 보험료 " + review.getAdjustedPremium() + "원)" : ""));
        return review;
    }

    private InsuranceApplication requireApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));
    }

    private InsuranceEmployee requireEmployee(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u instanceof InsuranceEmployee)
                .map(u -> (InsuranceEmployee) u)
                .orElseThrow(() -> new IllegalArgumentException("심사 직원을 찾을 수 없습니다."));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ReviewServiceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/ReviewService.java backend/src/test/java/com/distribution/insurance/service/ReviewServiceTest.java
git commit -m "feat(epic2): ReviewService(대기목록·상세·확정, 사고이력 동봉)"
```

---

## Task 8: 심사 DTO

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/PendingApplicationResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ReviewDetailResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ConfirmReviewRequest.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ReviewResultResponse.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/dto/ReviewDtoTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.application.VehicleInfo;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.review.AccidentHistory;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewDtoTest {

    @Test
    void 상세응답은_자동차건의_사고이력을_포함한다() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        var product = new CarInsuranceProduct("안심드라이브", "대인대물", 45000, "승용차", "가족한정");
        var app = new InsuranceApplication(ph, product, new VehicleInfo("12가3456", "승용차", 2020, 5), null);
        var history = new AccidentHistory(2, 2_000_000, "VALID", LocalDateTime.now());

        ReviewDetailResponse res = ReviewDetailResponse.from(app, history);

        assertThat(res.applicantName()).isEqualTo("홍길동");
        assertThat(res.vehicleInfo().plateNumber()).isEqualTo("12가3456");
        assertThat(res.medicalHistory()).isNull();
        assertThat(res.accidentHistory().accidentCount()).isEqualTo(2);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ReviewDtoTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

`PendingApplicationResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.InsuranceApplication;

import java.time.LocalDateTime;

/** 심사 대기 목록 항목(UC13 2단계). */
public record PendingApplicationResponse(
        Long applicationId, LocalDateTime appliedAt, String applicantName,
        String productName, int basePremium) {

    public static PendingApplicationResponse from(InsuranceApplication app) {
        return new PendingApplicationResponse(
                app.getId(), app.getAppliedAt(), app.getApplicant().getName(),
                app.getProduct().getProductName(), app.getProduct().getBasePremium());
    }
}
```

`ReviewDetailResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.application.VehicleInfo;
import com.distribution.insurance.domain.review.AccidentHistory;

import java.time.LocalDate;

/** 심사 상세(UC13 4단계 + 자동차건 UC15 참조정보). */
public record ReviewDetailResponse(
        Long applicationId,
        String applicantName, LocalDate birthDate, String ssn,
        String productName, int basePremium,
        VehicleInfoView vehicleInfo, MedicalHistoryView medicalHistory,
        AccidentHistoryView accidentHistory) {

    public record VehicleInfoView(String plateNumber, String vehicleType,
                                  int modelYear, int drivingExperienceYears) {}

    public record MedicalHistoryView(String currentConditions, String pastHospitalization,
                                     String medications) {}

    public record AccidentHistoryView(int accidentCount, int totalPaidAmount, String licenseStatus) {}

    public static ReviewDetailResponse from(InsuranceApplication app, AccidentHistory accidentHistory) {
        VehicleInfo v = app.getVehicleInfo();
        MedicalHistory m = app.getMedicalHistory();
        return new ReviewDetailResponse(
                app.getId(),
                app.getApplicant().getName(), app.getApplicant().getBirthDate(), app.getApplicant().getSsn(),
                app.getProduct().getProductName(), app.getProduct().getBasePremium(),
                v == null ? null : new VehicleInfoView(v.getPlateNumber(), v.getVehicleType(),
                        v.getModelYear(), v.getDrivingExperienceYears()),
                m == null ? null : new MedicalHistoryView(m.getCurrentConditions(),
                        m.getPastHospitalization(), m.getMedications()),
                accidentHistory == null ? null : new AccidentHistoryView(
                        accidentHistory.getAccidentCount(), accidentHistory.getTotalPaidAmount(),
                        accidentHistory.getLicenseStatus()));
    }
}
```

`ConfirmReviewRequest.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.ReviewResult;
import jakarta.validation.constraints.NotNull;

/** 심사 확정 요청(UC13 7단계). surchargeRate는 조건부승인 시에만. */
public record ConfirmReviewRequest(
        @NotNull ReviewResult result,
        String comment,
        Double surchargeRate) {}
```

`ReviewResultResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.EnrollmentReview;

public record ReviewResultResponse(
        Long reviewId, String result, double surchargeRate, int adjustedPremium) {

    public static ReviewResultResponse from(EnrollmentReview review) {
        return new ReviewResultResponse(
                review.getId(), review.getResult().name(),
                review.getSurchargeRate(), review.getAdjustedPremium());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ReviewDtoTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/dto/PendingApplicationResponse.java backend/src/main/java/com/distribution/insurance/web/dto/ReviewDetailResponse.java backend/src/main/java/com/distribution/insurance/web/dto/ConfirmReviewRequest.java backend/src/main/java/com/distribution/insurance/web/dto/ReviewResultResponse.java backend/src/test/java/com/distribution/insurance/web/dto/ReviewDtoTest.java
git commit -m "feat(epic2): 심사 목록/상세/확정 DTO"
```

---

## Task 9: SecurityConfig — /reviews 권한

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java`

- [ ] **Step 1: Modify SecurityConfig**

`.requestMatchers("/applications/**").hasRole("POLICYHOLDER")` 줄 뒤에 추가:
```java
                        .requestMatchers("/reviews/**").hasRole("EMPLOYEE")
```

수정 후 블록:
```java
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                        .requestMatchers("/applications/**").hasRole("POLICYHOLDER")
                        .requestMatchers("/reviews/**").hasRole("EMPLOYEE")
                        .anyRequest().authenticated())
```

- [ ] **Step 2: Verify compile**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java
git commit -m "feat(epic2): /reviews 경로 EMPLOYEE 권한"
```

---

## Task 10: ReviewController + 통합 테스트

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/ReviewController.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/ReviewControllerTest.java`

엔드포인트:
- `GET /reviews/pending` → 200 + List<PendingApplicationResponse>
- `GET /reviews/applications/{id}` → 200 + ReviewDetailResponse
- `POST /reviews/applications/{id}/confirm` → 200 + ReviewResultResponse

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.application.VehicleInfo;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ApplicationRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    String empToken; String phToken; Long healthAppId; Long carAppId;

    @BeforeEach
    void setUp() {
        InsuranceEmployee emp = userRepository.save(new InsuranceEmployee(
                "심사역", "e@test.com", "010", encoder.encode("pw"), "심사팀", 0));
        empToken = tokenProvider.createToken(emp.getId(), "EMPLOYEE");

        Policyholder ph = userRepository.save(new Policyholder("홍길동", "h@test.com", "010", encoder.encode("pw"),
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
        phToken = tokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        InsuranceProduct health = productRepository.save(
                new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120));
        healthAppId = applicationRepository.save(new InsuranceApplication(
                ph, health, null, new MedicalHistory("없음", "없음", "없음"))).getId();

        InsuranceProduct car = productRepository.save(
                new CarInsuranceProduct("안심드라이브", "대인대물", 45000, "승용차", "가족한정"));
        carAppId = applicationRepository.save(new InsuranceApplication(
                ph, car, new VehicleInfo("12가3456", "승용차", 2020, 5), null)).getId();
    }

    @Test
    void 심사대기목록을_조회한다() throws Exception {
        mockMvc.perform(get("/reviews/pending")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 가입자토큰으로_심사목록_접근하면_403() throws Exception {
        mockMvc.perform(get("/reviews/pending")
                        .header("Authorization", "Bearer " + phToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 자동차건_상세는_사고이력을_포함한다() throws Exception {
        mockMvc.perform(get("/reviews/applications/" + carAppId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleInfo.plateNumber").value("12가3456"))
                .andExpect(jsonPath("$.accidentHistory").exists());
    }

    @Test
    void 의료건_상세는_사고이력이_null() throws Exception {
        mockMvc.perform(get("/reviews/applications/" + healthAppId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicalHistory.currentConditions").value("없음"))
                .andExpect(jsonPath("$.accidentHistory").doesNotExist());
    }

    @Test
    void 조건부승인_확정하면_200과_할증보험료() throws Exception {
        String body = "{\"result\":\"CONDITIONAL\",\"comment\":\"사고이력 다수\",\"surchargeRate\":0.2}";
        mockMvc.perform(post("/reviews/applications/" + healthAppId + "/confirm")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("CONDITIONAL"))
                .andExpect(jsonPath("$.adjustedPremium").value(36000));
    }

    @Test
    void 조건부승인인데_할증율_없으면_400() throws Exception {
        String body = "{\"result\":\"CONDITIONAL\",\"comment\":\"사유\"}";
        mockMvc.perform(post("/reviews/applications/" + healthAppId + "/confirm")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 이미_심사된_건_재확정하면_409() throws Exception {
        String approve = "{\"result\":\"APPROVED\",\"comment\":\"이상 없음\"}";
        mockMvc.perform(post("/reviews/applications/" + healthAppId + "/confirm")
                .header("Authorization", "Bearer " + empToken)
                .contentType("application/json").content(approve))
                .andExpect(status().isOk());

        mockMvc.perform(post("/reviews/applications/" + healthAppId + "/confirm")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType("application/json").content(approve))
                .andExpect(status().isConflict());
    }

    @Test
    void 없는_신청_상세는_404() throws Exception {
        mockMvc.perform(get("/reviews/applications/999999")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ReviewControllerTest"`
Expected: 404/메서드 없음으로 실패.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.review.EnrollmentReview;
import com.distribution.insurance.service.ReviewService;
import com.distribution.insurance.web.dto.ConfirmReviewRequest;
import com.distribution.insurance.web.dto.PendingApplicationResponse;
import com.distribution.insurance.web.dto.ReviewDetailResponse;
import com.distribution.insurance.web.dto.ReviewResultResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/pending")
    public List<PendingApplicationResponse> pending() {
        return reviewService.pendingApplications().stream()
                .map(PendingApplicationResponse::from)
                .toList();
    }

    @GetMapping("/applications/{id}")
    public ReviewDetailResponse detail(@PathVariable Long id) {
        ReviewService.ReviewDetail detail = reviewService.detail(id);
        return ReviewDetailResponse.from(detail.application(), detail.accidentHistory());
    }

    @PostMapping("/applications/{id}/confirm")
    public ReviewResultResponse confirm(@AuthenticationPrincipal Long reviewerId,
                                        @PathVariable Long id,
                                        @Valid @RequestBody ConfirmReviewRequest request) {
        EnrollmentReview review = reviewService.confirm(
                reviewerId, id, request.result(), request.comment(), request.surchargeRate());
        return ReviewResultResponse.from(review);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ReviewControllerTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/controller/ReviewController.java backend/src/test/java/com/distribution/insurance/web/controller/ReviewControllerTest.java
git commit -m "feat(epic2): ReviewController(/reviews 대기목록·상세·확정)"
```

---

## Task 11: 전체 테스트 확인

- [ ] **Step 1: Run full suite**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL, 전체 그린.

- [ ] **Step 2: (실패 시)** superpowers:systematic-debugging로 원인 분석 후 수정. 추측 금지.

---

## 완료 기준 (DoD)
- UC13 베이직 플로우(대기목록→상세→확정→상태전이→결과통보) 동작.
- A1 조건부승인: surchargeRate로 adjustedPremium 재계산, Application은 APPROVED(ADR 0003).
- UC15: 자동차건 상세·확정 시 mock 사고이력 동봉/저장. 의료건은 null.
- 권한: EMPLOYEE만 /reviews 접근(가입자 403).
- 에러: 없는 신청 404, 조건부 할증율 누락 400, 이미 심사된 건 재확정 409(E3 상태가드).
- 개인정보는 Policyholder 참조로 노출(ADR 0002).
- `./gradlew test` 전체 그린.
