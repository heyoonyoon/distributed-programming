# Epic 4 이슈 B — 복잡청구 심사(UC12) + 담당자 지정(UC14) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** COMPLEX 의료보험 청구에 담당자를 자동(최소부하)/수동 배정하고, 배정된 담당자만 심사(승인→보험금 지급 / 반려→통보)하도록 한다. 지급 실패 시 직원이 재시도한다.

**Architecture:** `BenefitPaymentReview`(Review 상속, SINGLE_TABLE discriminator)가 COMPLEX `HealthInsuranceClaim`을 1:1로 참조하고 배정 담당자(`assignedStaffId`)를 가진다. `StaffAssignmentService`가 `currentLoad` 최소 직원을 골라 배정하고 부하를 올린다. `BenefitReviewService`가 소유권(배정자) 검증 후 심사를 확정하며, 승인 시 이슈 A의 `BenefitPayoutService.pay()`를 재사용한다(ADR 0001: 다이어그램 메서드는 책임, 조율은 서비스). 이슈 A의 `ClaimService` COMPLEX 분기를 "PENDING+안내"에서 "심사 생성+배정+IN_REVIEW"로 교체한다.

**Tech Stack:** Spring Boot 4, Java 21, Spring Data JPA, Spring MVC, MySQL, Lombok, JUnit5 + AssertJ + MockMvc.

**입력 문서(준수 필수):**
- spec: `docs/superpowers/specs/2026-06-04-epic4-claim-payout-design.md`
- 용어: `CONTEXT.md` (BenefitPaymentReview, BenefitPayment, ClaimStatus, InsuranceEmployee=담당자 역할; 동의어 금지)
- 결정: ADR 0001(메서드=책임), 0006(SIMPLE 직접지급), 0007(ClaimStatus 6값)
- 선행: 이슈 A(`epic4-A-health-claim-payout`)의 도메인·서비스. **이 브랜치는 epic4-A 위에 스택**으로 작업한다(A 미머지 상태이므로). 브랜치: `epic4-B-benefit-review-assignment` (base: epic4-A-health-claim-payout).

**기존 코드 규약(맞출 것):**
- 직원 엔드포인트: `/staff/**`, `ROLE_EMPLOYEE`(SecurityConfig에 규칙 추가). 현재 사용자 `@AuthenticationPrincipal Long userId`.
- `Review`는 SINGLE_TABLE + `@DiscriminatorColumn(review_type)`. 하위는 `@DiscriminatorValue(...)`.
- 예외→HTTP: `IllegalArgumentException`→404, `InvalidRequestException`→400, `IllegalStateException`→403, `IllegalStateTransitionException`→409.
- 알림: `NotificationSender.send(email, phone, message)`.
- 직원 조회: `UserRepository.findAllEmployees()` 존재. 테스트는 `@SpringBootTest @Transactional` + AssertJ + 한글 메서드명.

**용어 적응(diagram vs 코드):** 클래스 다이어그램은 `assignedStaffId : String // employeeId 참조`로 표기하나, 이 코드베이스의 `InsuranceEmployee`는 별도 employeeId 없이 `User.id`(Long)를 식별자로 쓴다. 따라서 `assignedStaffId`는 **Long(InsuranceEmployee.id)** 으로 둔다. 소유권 검증은 이 id로 한다.

**범위 경계:** 자동차사고(UC09)=이슈 C, 조회·분석(UC03/04/11)=이슈 D. 본 이슈는 의료 COMPLEX 청구의 배정·심사·지급·재시도까지.

---

## File Structure

신규(`domain/review/`): `BenefitPaymentReview.java`
수정(`domain/user/`): `InsuranceEmployee.java` (assignWork 메서드 추가)
수정(`domain/claim/`): `Claim.java` (markCompleted/markFailed가 FAILED에서도 재시도 허용)
신규(`repository/`): `BenefitPaymentReviewRepository.java`
신규(`service/`): `StaffAssignmentService.java`, `BenefitReviewService.java`
수정(`service/`): `ClaimService.java` (COMPLEX 분기 교체), `BenefitPayoutService.java` (retry 재사용 가능하게)
신규(`web/`): `StaffReviewController.java`, DTO들(`BenefitReviewSummaryResponse`, `BenefitReviewDetailResponse`, `ConfirmBenefitReviewRequest`, `BenefitReviewResultResponse`, `AssignStaffRequest`)
수정(`security/`): `SecurityConfig.java` (`/staff/**` → EMPLOYEE)

---

## Task 1: InsuranceEmployee.assignWork() (부하 증가)

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/domain/user/InsuranceEmployee.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/user/InsuranceEmployeeTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.user;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InsuranceEmployeeTest {

    @Test
    void 업무를_배정하면_currentLoad가_1_증가한다() {
        InsuranceEmployee e = new InsuranceEmployee("김직원", "k@t.com", "010", "pw", "심사1팀", 2);
        e.assignWork();
        assertThat(e.getCurrentLoad()).isEqualTo(3);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*InsuranceEmployeeTest'`
Expected: FAIL — `assignWork()` 없음.

- [ ] **Step 3: Write minimal implementation** (add method to InsuranceEmployee)

```java
    /** 새 심사 건 배정 시 업무량 증가(UC14). */
    public void assignWork() {
        this.currentLoad += 1;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*InsuranceEmployeeTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/user/InsuranceEmployee.java \
        backend/src/test/java/com/distribution/insurance/domain/user/InsuranceEmployeeTest.java
git commit -m "Epic4-B: InsuranceEmployee.assignWork() 부하 증가"
```

---

## Task 2: Claim 재시도 전이 허용 (FAILED → COMPLETED/FAILED)

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/domain/claim/Claim.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/claim/ClaimRetryTransitionTest.java`

배경: UC17 E1-3 — 송금 실패(FAILED) 후 직원이 재시도한다. 따라서 `markCompleted`/`markFailed`는 `FAILED`에서도 진입 가능해야 한다. 현재 가드는 `PENDING, APPROVED`만 허용하므로 `FAILED`를 추가한다.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimRetryTransitionTest {

    private HealthInsuranceClaim claim() {
        Policyholder ph = new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789");
        var product = new HealthInsuranceProduct("건강", "암", 30000, 120);
        var contract = new InsuranceContract(ph, product, 30000, LocalDate.now());
        return new HealthInsuranceClaim(contract, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);
    }

    @Test
    void 실패한_지급은_재시도로_COMPLETED될_수_있다() {
        HealthInsuranceClaim c = claim();
        c.markFailed();
        c.markCompleted();
        assertThat(c.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 실패한_지급은_재시도에서_다시_FAILED될_수_있다() {
        HealthInsuranceClaim c = claim();
        c.markFailed();
        c.markFailed();
        assertThat(c.getStatus()).isEqualTo(ClaimStatus.FAILED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimRetryTransitionTest'`
Expected: FAIL — `IllegalStateTransitionException`(현 가드가 FAILED 불허).

- [ ] **Step 3: Write minimal implementation** — `Claim.java`의 두 메서드 가드에 `FAILED` 추가:

```java
    /** 지급 완료. SIMPLE은 PENDING, COMPLEX는 APPROVED, 재시도는 FAILED에서 진입(ADR 0006/0007, UC17 E1). */
    public void markCompleted() {
        requireOneOf(ClaimStatus.PENDING, ClaimStatus.APPROVED, ClaimStatus.FAILED);
        this.status = ClaimStatus.COMPLETED;
    }

    /** 지급 실패(UC17 E1). PENDING/APPROVED 최초 시도 또는 FAILED 재시도에서 발생. */
    public void markFailed() {
        requireOneOf(ClaimStatus.PENDING, ClaimStatus.APPROVED, ClaimStatus.FAILED);
        this.status = ClaimStatus.FAILED;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimRetryTransitionTest' --tests '*HealthInsuranceClaimTest'`
Expected: PASS (기존 전이 테스트도 유지).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/claim/Claim.java \
        backend/src/test/java/com/distribution/insurance/domain/claim/ClaimRetryTransitionTest.java
git commit -m "Epic4-B: Claim 재시도 전이(FAILED→COMPLETED/FAILED) 허용(UC17 E1)"
```

---

## Task 3: BenefitPaymentReview 엔티티

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/review/BenefitPaymentReview.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/review/BenefitPaymentReviewTest.java`

`Review`(SINGLE_TABLE) 상속. `@DiscriminatorValue("BENEFIT_PAYMENT")`. 필드: `assignedStaffId`(Long), `claim`(@OneToOne HealthInsuranceClaim). 생성자는 claim만 받고 미배정 상태로 시작. `assignTo(InsuranceEmployee)`로 배정. `confirm(ReviewResult, comment)`로 결과 기록(부모 `recordResult` 사용); APPROVED/REJECTED만 허용(CONDITIONAL 금지 → InvalidRequestException).

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.review;

import com.distribution.insurance.domain.claim.ClaimComplexity;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class BenefitPaymentReviewTest {

    private HealthInsuranceClaim complexClaim() {
        Policyholder ph = new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789");
        var product = new HealthInsuranceProduct("건강", "암", 30000, 120);
        var contract = new InsuranceContract(ph, product, 30000, LocalDate.now());
        return new HealthInsuranceClaim(contract, 2000000, "병원", "S00", LocalDate.now(), 2000000, ClaimComplexity.COMPLEX);
    }

    @Test
    void 생성하면_미배정이고_claim을_참조한다() {
        HealthInsuranceClaim claim = complexClaim();
        BenefitPaymentReview review = new BenefitPaymentReview(claim);
        assertThat(review.getClaim()).isSameAs(claim);
        assertThat(review.getAssignedStaffId()).isNull();
    }

    @Test
    void 배정하면_assignedStaffId가_채워진다() {
        BenefitPaymentReview review = new BenefitPaymentReview(complexClaim());
        InsuranceEmployee staff = new InsuranceEmployee("김직원", "k@t.com", "010", "pw", "심사1팀", 0);
        // staff.getId()는 영속화 전 null일 수 있어, 테스트에선 명시 id 주입 대신 동작만 확인
        review.assignTo(5L);
        assertThat(review.getAssignedStaffId()).isEqualTo(5L);
    }

    @Test
    void 승인_확정하면_결과가_APPROVED로_기록된다() {
        BenefitPaymentReview review = new BenefitPaymentReview(complexClaim());
        review.assignTo(5L);
        review.confirm(ReviewResult.APPROVED, "정상 청구");
        assertThat(review.getResult()).isEqualTo(ReviewResult.APPROVED);
        assertThat(review.getReviewedAt()).isNotNull();
    }

    @Test
    void 반려_확정하면_결과와_사유가_기록된다() {
        BenefitPaymentReview review = new BenefitPaymentReview(complexClaim());
        review.assignTo(5L);
        review.confirm(ReviewResult.REJECTED, "서류 미비");
        assertThat(review.getResult()).isEqualTo(ReviewResult.REJECTED);
        assertThat(review.getComment()).isEqualTo("서류 미비");
    }

    @Test
    void 조건부_결과는_허용되지_않는다() {
        BenefitPaymentReview review = new BenefitPaymentReview(complexClaim());
        review.assignTo(5L);
        assertThatThrownBy(() -> review.confirm(ReviewResult.CONDITIONAL, "x"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*BenefitPaymentReviewTest'`
Expected: FAIL — `BenefitPaymentReview` 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.domain.review;

import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 복잡한 의료보험 청구의 보험금 지급 심사(UC12). 배정된 담당자만 확정한다. */
@Entity
@DiscriminatorValue("BENEFIT_PAYMENT")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BenefitPaymentReview extends Review {

    private Long assignedStaffId;   // InsuranceEmployee.id (용어 적응: diagram의 assignedStaffId)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", unique = true, nullable = false)
    private HealthInsuranceClaim claim;

    public BenefitPaymentReview(HealthInsuranceClaim claim) {
        this.claim = claim;
    }

    /** 담당자 배정(UC14). */
    public void assignTo(Long staffId) {
        this.assignedStaffId = staffId;
    }

    /** 심사 확정(UC12). 보험금 지급 심사는 승인/반려만 가능(조건부 없음). */
    public void confirm(ReviewResult result, String comment) {
        if (result == ReviewResult.CONDITIONAL) {
            throw new InvalidRequestException("보험금 지급 심사는 조건부 승인을 사용하지 않습니다.");
        }
        recordResult(result, comment);
    }
}
```

> 참고: 부모 `Review.recordResult`는 `protected`라 하위에서 호출 가능. `getResult()/getComment()/getReviewedAt()`은 부모 `@Getter`로 제공.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*BenefitPaymentReviewTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/review/BenefitPaymentReview.java \
        backend/src/test/java/com/distribution/insurance/domain/review/BenefitPaymentReviewTest.java
git commit -m "Epic4-B: BenefitPaymentReview 엔티티(Review 상속, 배정/확정)"
```

---

## Task 4: BenefitPaymentReviewRepository

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/repository/BenefitPaymentReviewRepository.java`
- Test: `backend/src/test/java/com/distribution/insurance/repository/BenefitPaymentReviewRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.ClaimComplexity;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BenefitPaymentReviewRepositoryTest {

    @Autowired BenefitPaymentReviewRepository reviewRepository;
    @Autowired ClaimRepository claimRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private HealthInsuranceClaim savedComplexClaim() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        return claimRepository.save(new HealthInsuranceClaim(contract, 2000000, "병원", "S00",
                LocalDate.now(), 2000000, ClaimComplexity.COMPLEX));
    }

    @Test
    void 담당자별로_심사건을_조회한다() {
        HealthInsuranceClaim claim = savedComplexClaim();
        BenefitPaymentReview review = new BenefitPaymentReview(claim);
        review.assignTo(7L);
        reviewRepository.save(review);

        List<BenefitPaymentReview> found = reviewRepository.findByAssignedStaffId(7L);
        assertThat(found).hasSize(1);
        assertThat(reviewRepository.findByClaimId(claim.getId())).isPresent();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*BenefitPaymentReviewRepositoryTest'`
Expected: FAIL — repository 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.review.BenefitPaymentReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BenefitPaymentReviewRepository extends JpaRepository<BenefitPaymentReview, Long> {
    List<BenefitPaymentReview> findByAssignedStaffId(Long staffId);
    Optional<BenefitPaymentReview> findByClaimId(Long claimId);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*BenefitPaymentReviewRepositoryTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/repository/BenefitPaymentReviewRepository.java \
        backend/src/test/java/com/distribution/insurance/repository/BenefitPaymentReviewRepositoryTest.java
git commit -m "Epic4-B: BenefitPaymentReviewRepository"
```

---

## Task 5: StaffAssignmentService (UC14 자동/수동 배정)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/StaffAssignmentService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/StaffAssignmentServiceTest.java`

책임:
- `assignAutomatically(BenefitPaymentReview)`: `userRepository.findAllEmployees()` 중 `currentLoad` 최소(동점 시 id 오름차순) 선정. 없으면 `IllegalStateException`("배정 가능한 담당자가 없습니다.")(UC14 E1). 선정 직원 `assignWork()`(부하++) + `review.assignTo(staff.getId())` + 담당자 알림.
- `assignManually(BenefitPaymentReview, Long employeeId)`: 지정 직원으로 배정(없으면 `IllegalArgumentException`). 부하++ + 알림.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimComplexity;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class StaffAssignmentServiceTest {

    @Autowired StaffAssignmentService assignmentService;
    @Autowired BenefitPaymentReviewRepository reviewRepository;
    @Autowired ClaimRepository claimRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private HealthInsuranceClaim savedComplexClaim() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        return claimRepository.save(new HealthInsuranceClaim(contract, 2000000, "병원", "S00",
                LocalDate.now(), 2000000, ClaimComplexity.COMPLEX));
    }

    private InsuranceEmployee emp(String email, int load) {
        return userRepository.save(new InsuranceEmployee("직원", email, "010", "pw", "심사팀", load));
    }

    @Test
    void 최소부하_직원에게_배정하고_부하를_올린다() {
        InsuranceEmployee busy = emp("busy@t.com", 5);
        InsuranceEmployee idle = emp("idle@t.com", 1);
        BenefitPaymentReview review = reviewRepository.save(new BenefitPaymentReview(savedComplexClaim()));

        assignmentService.assignAutomatically(review);

        assertThat(review.getAssignedStaffId()).isEqualTo(idle.getId());
        assertThat(userRepository.findById(idle.getId()).map(u -> ((InsuranceEmployee) u).getCurrentLoad()))
                .contains(2);
    }

    @Test
    void 배정가능_직원이_없으면_예외() {
        BenefitPaymentReview review = reviewRepository.save(new BenefitPaymentReview(savedComplexClaim()));
        assertThatThrownBy(() -> assignmentService.assignAutomatically(review))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 수동배정은_지정_직원으로_배정한다() {
        InsuranceEmployee a = emp("a@t.com", 0);
        InsuranceEmployee b = emp("b@t.com", 0);
        BenefitPaymentReview review = reviewRepository.save(new BenefitPaymentReview(savedComplexClaim()));

        assignmentService.assignManually(review, b.getId());

        assertThat(review.getAssignedStaffId()).isEqualTo(b.getId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*StaffAssignmentServiceTest'`
Expected: FAIL — service 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

/** 담당자 지정(UC14). 최소 부하 자동 배정 + 수동 배정. */
@Service
public class StaffAssignmentService {

    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public StaffAssignmentService(UserRepository userRepository, NotificationSender notificationSender) {
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }

    /** 현재 업무량 최소(동점 시 id 오름차순) 직원에게 자동 배정. */
    @Transactional
    public void assignAutomatically(BenefitPaymentReview review) {
        InsuranceEmployee staff = userRepository.findAllEmployees().stream()
                .min(Comparator.comparingInt(InsuranceEmployee::getCurrentLoad)
                        .thenComparing(InsuranceEmployee::getId))
                .orElseThrow(() -> new IllegalStateException("배정 가능한 담당자가 없습니다."));
        bind(review, staff);
    }

    /** 관리자가 지정한 직원으로 수동 배정(UC14 A1). */
    @Transactional
    public void assignManually(BenefitPaymentReview review, Long employeeId) {
        InsuranceEmployee staff = userRepository.findById(employeeId)
                .filter(u -> u instanceof InsuranceEmployee)
                .map(u -> (InsuranceEmployee) u)
                .orElseThrow(() -> new IllegalArgumentException("심사 직원을 찾을 수 없습니다."));
        bind(review, staff);
    }

    private void bind(BenefitPaymentReview review, InsuranceEmployee staff) {
        staff.assignWork();
        review.assignTo(staff.getId());
        notificationSender.send(staff.getEmail(), staff.getPhone(),
                "새 심사 건이 배정되었습니다. 청구 " + review.getClaim().getId());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*StaffAssignmentServiceTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/StaffAssignmentService.java \
        backend/src/test/java/com/distribution/insurance/service/StaffAssignmentServiceTest.java
git commit -m "Epic4-B: StaffAssignmentService(UC14 최소부하 자동/수동 배정)"
```

---

## Task 6: ClaimService COMPLEX 분기 교체 (배정+심사 생성+IN_REVIEW)

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/service/ClaimService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/ClaimServiceComplexTest.java`

배경: 이슈 A에서 COMPLEX는 PENDING + 안내만 했다. 이제 COMPLEX 청구 시 `BenefitPaymentReview`를 만들어 저장하고 `StaffAssignmentService.assignAutomatically`로 배정한 뒤 `claim.markInReview()`로 IN_REVIEW 전환한다. (배정 가능 직원이 없으면 IllegalStateException → 409; 청구 자체가 롤백된다 — 같은 트랜잭션.)

ClaimService에 `BenefitPaymentReviewRepository`, `StaffAssignmentService` 주입 추가. SIMPLE 경로는 그대로.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ClaimServiceComplexTest {

    @Autowired ClaimService claimService;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentReviewRepository reviewRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Long phId(String acct) {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", acct));
        return ph.getId();
    }

    private Long healthContractId(Long phId) {
        Policyholder ph = (Policyholder) userRepository.findById(phId).orElseThrow();
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        return contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now())).getId();
    }

    @Test
    void COMPLEX_청구는_심사가_생성되고_배정되고_IN_REVIEW가_된다() {
        userRepository.save(new InsuranceEmployee("직원", "e@t.com", "010", "pw", "심사팀", 0));
        Long ph = phId("110-123-456789");
        Long contractId = healthContractId(ph);

        HealthInsuranceClaim claim = claimService.fileHealthClaim(
                ph, contractId, "서울병원", "S00", LocalDate.now(), 2000000, 2000000, List.of());

        HealthInsuranceClaim reloaded = (HealthInsuranceClaim) claimRepository.findById(claim.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ClaimStatus.IN_REVIEW);
        BenefitPaymentReview review = reviewRepository.findByClaimId(claim.getId()).orElseThrow();
        assertThat(review.getAssignedStaffId()).isNotNull();
    }

    @Test
    void 직원이_없으면_COMPLEX_청구는_롤백되어_409성_예외() {
        Long ph = phId("110-123-456789");
        Long contractId = healthContractId(ph);

        assertThatThrownBy(() -> claimService.fileHealthClaim(
                ph, contractId, "서울병원", "S00", LocalDate.now(), 2000000, 2000000, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimServiceComplexTest'`
Expected: FAIL — 현재 COMPLEX는 PENDING으로 남고 review 미생성.

- [ ] **Step 3: Write minimal implementation** — `ClaimService` 수정:

생성자에 의존성 추가(기존 필드 유지):
```java
    private final BenefitPaymentReviewRepository reviewRepository;
    private final StaffAssignmentService staffAssignmentService;
```
생성자 파라미터·할당에 두 의존성을 추가한다(기존 `claimRepository, contractRepository, payoutService, notificationSender, complexThreshold`에 이어서).

COMPLEX 분기 교체(기존 `else { notificationSender.send(... "담당자 배정 후 심사" ) }` 부분):
```java
        if (complexity == ClaimComplexity.SIMPLE) {
            payoutService.pay(claim);
        } else {
            BenefitPaymentReview review = reviewRepository.save(new BenefitPaymentReview(claim));
            staffAssignmentService.assignAutomatically(review);   // 직원 없으면 IllegalStateException → 롤백
            claim.markInReview();
            notificationSender.send(ph.getEmail(), ph.getPhone(),
                    "청구가 접수되었습니다. 담당자 배정 후 심사를 진행합니다.");
        }
```

> import 추가: `BenefitPaymentReview`, `BenefitPaymentReviewRepository`, `StaffAssignmentService`.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimServiceComplexTest' --tests '*ClaimServiceTest'`
Expected: PASS (SIMPLE 경로 기존 테스트 유지).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/ClaimService.java \
        backend/src/test/java/com/distribution/insurance/service/ClaimServiceComplexTest.java
git commit -m "Epic4-B: ClaimService COMPLEX 분기 — 심사 생성+자동배정+IN_REVIEW"
```

---

## Task 7: BenefitReviewService (UC12 심사: 목록/상세/확정, 재시도)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/BenefitReviewService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/BenefitReviewServiceTest.java`

책임:
- `assignedReviews(Long staffId)`: 해당 담당자에게 배정된 미확정(result == null) 심사 목록.
- `detail(Long staffId, Long claimId)`: 심사 상세. 배정 담당자가 아니면 `IllegalStateException`(E2 → 여기선 403; 단 spec은 409를 권장 → IllegalStateTransitionException 사용해 409). **본 plan은 409로 통일**: 비배정 접근 시 `IllegalStateTransitionException("현재 담당자가 처리 중인 건입니다.")`.
- `confirm(Long staffId, Long claimId, ReviewResult result, String comment)`: 소유권 검증(비배정 409) → `review.confirm(result, comment)`. APPROVED면 `claim.markApproved()` 후 `payoutService.pay(claim)`(지급). REJECTED면 `claim.markRejected()` + 가입자 통보(지급 미수행).
- `retryPayout(Long claimId)`: FAILED 청구 재시도 → `payoutService.pay(claim)` 재호출(Task 2의 FAILED 전이 허용 덕에 가능).

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class BenefitReviewServiceTest {

    @Autowired BenefitReviewService reviewService;
    @Autowired ClaimService claimService;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentReviewRepository reviewRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Long staffId(String email) {
        return userRepository.save(new InsuranceEmployee("직원", email, "010", "pw", "심사팀", 0)).getId();
    }

    /** COMPLEX 청구를 만들고 (자동배정된) review를 반환. account로 지급 성공/실패 제어. */
    private HealthInsuranceClaim complexClaim(String account) {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", account));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        Long contractId = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now())).getId();
        return claimService.fileHealthClaim(ph.getId(), contractId, "서울병원", "S00",
                LocalDate.now(), 2000000, 2000000, List.of());
    }

    @Test
    void 배정된_담당자가_승인하면_지급되어_COMPLETED된다() {
        Long staff = staffId("e@t.com");
        HealthInsuranceClaim claim = complexClaim("110-123-456789");
        // 자동배정은 유일 직원(staff)에게 감
        reviewService.confirm(staff, claim.getId(), ReviewResult.APPROVED, "정상");

        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 반려하면_REJECTED이고_지급되지_않는다() {
        Long staff = staffId("e@t.com");
        HealthInsuranceClaim claim = complexClaim("110-123-456789");
        reviewService.confirm(staff, claim.getId(), ReviewResult.REJECTED, "서류 미비");

        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void 비배정_담당자가_확정하면_409성_예외() {
        Long assigned = staffId("e@t.com");
        Long other = staffId("o@t.com");   // currentLoad 동일 0 — 자동배정은 id 작은 쪽(assigned)이 먼저
        HealthInsuranceClaim claim = complexClaim("110-123-456789");

        assertThatThrownBy(() -> reviewService.confirm(other, claim.getId(), ReviewResult.APPROVED, "x"))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void 승인_후_지급실패건은_재시도로_COMPLETED된다() {
        Long staff = staffId("e@t.com");
        HealthInsuranceClaim claim = complexClaim("110-123-450000");  // 0000 → 송금 실패
        reviewService.confirm(staff, claim.getId(), ReviewResult.APPROVED, "정상");
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.FAILED);

        // 가입자 계좌가 정상으로 바뀌었다고 가정하고 재시도
        Policyholder ph = claim.getContract().getPolicyholder();
        ph.updateProfile("주소", "110-123-999999");
        reviewService.retryPayout(claim.getId());

        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.COMPLETED);
    }
}
```

> 주의: 비배정 테스트는 자동배정이 **id 오름차순 동점 처리**라 먼저 만든 `assigned`(작은 id)에게 배정됨에 의존한다. `complexClaim` 호출 시점에 직원 2명이 모두 존재해야 하므로, 테스트는 두 staffId를 claim 생성 전에 만든다(위 코드 순서 유지).

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*BenefitReviewServiceTest'`
Expected: FAIL — `BenefitReviewService` 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.BenefitPaymentReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 보험금 지급 심사(UC12). 배정 담당자만 확정. 승인 시 지급(UC17 재사용). */
@Service
public class BenefitReviewService {

    private final BenefitPaymentReviewRepository reviewRepository;
    private final BenefitPayoutService payoutService;
    private final NotificationSender notificationSender;

    public BenefitReviewService(BenefitPaymentReviewRepository reviewRepository,
                                BenefitPayoutService payoutService,
                                NotificationSender notificationSender) {
        this.reviewRepository = reviewRepository;
        this.payoutService = payoutService;
        this.notificationSender = notificationSender;
    }

    @Transactional(readOnly = true)
    public List<BenefitPaymentReview> assignedReviews(Long staffId) {
        return reviewRepository.findByAssignedStaffId(staffId).stream()
                .filter(r -> r.getResult() == null)
                .toList();
    }

    @Transactional(readOnly = true)
    public BenefitPaymentReview detail(Long staffId, Long claimId) {
        return requireOwned(staffId, claimId);
    }

    @Transactional
    public BenefitPaymentReview confirm(Long staffId, Long claimId, ReviewResult result, String comment) {
        BenefitPaymentReview review = requireOwned(staffId, claimId);
        HealthInsuranceClaim claim = review.getClaim();

        review.confirm(result, comment);
        Policyholder ph = claim.getContract().getPolicyholder();

        if (result == ReviewResult.REJECTED) {
            claim.markRejected();
            notificationSender.send(ph.getEmail(), ph.getPhone(),
                    "보험금 지급 심사 결과: 반려. 사유: " + comment);
        } else {   // APPROVED
            claim.markApproved();
            payoutService.pay(claim);   // 지급 성공 → COMPLETED, 실패 → FAILED + 직원 알림
        }
        return review;
    }

    /** 송금 실패(FAILED) 건 재시도(UC17 E1-3). */
    @Transactional
    public void retryPayout(Long claimId) {
        BenefitPaymentReview review = reviewRepository.findByClaimId(claimId)
                .orElseThrow(() -> new IllegalArgumentException("심사 건을 찾을 수 없습니다."));
        payoutService.pay(review.getClaim());
    }

    private BenefitPaymentReview requireOwned(Long staffId, Long claimId) {
        BenefitPaymentReview review = reviewRepository.findByClaimId(claimId)
                .orElseThrow(() -> new IllegalArgumentException("심사 건을 찾을 수 없습니다."));
        if (!staffId.equals(review.getAssignedStaffId())) {
            throw new IllegalStateTransitionException("현재 담당자가 처리 중인 건입니다.");
        }
        return review;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*BenefitReviewServiceTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/BenefitReviewService.java \
        backend/src/test/java/com/distribution/insurance/service/BenefitReviewServiceTest.java
git commit -m "Epic4-B: BenefitReviewService(UC12 목록/상세/확정/재시도, E2 소유권 409)"
```

---

## Task 8: StaffReviewController + DTO + SecurityConfig (/staff/**)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/BenefitReviewSummaryResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/BenefitReviewDetailResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ConfirmBenefitReviewRequest.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/BenefitReviewResultResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/AssignStaffRequest.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/StaffReviewController.java`
- Modify: `backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/StaffReviewControllerTest.java`

엔드포인트(모두 ROLE_EMPLOYEE):
- `GET /staff/benefit-reviews` — 본인 배정 미확정 목록
- `GET /staff/benefit-reviews/{claimId}` — 상세(비배정 409)
- `POST /staff/benefit-reviews/{claimId}/confirm` — `{result, comment}` 확정
- `POST /staff/claims/{claimId}/assign` — `{employeeId}` 수동 재배정(관리자/상위 직원; 본 plan은 EMPLOYEE 권한으로 단순화)
- `POST /staff/benefit-reviews/{claimId}/retry` — 지급 재시도

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import com.distribution.insurance.security.JwtTokenProvider;
import com.distribution.insurance.service.ClaimService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StaffReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClaimService claimService;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    private InsuranceEmployee emp(String email) {
        return userRepository.save(new InsuranceEmployee("직원", email, "010", "pw", "심사팀", 0));
    }

    private HealthInsuranceClaim complexClaim() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        Long contractId = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now())).getId();
        return claimService.fileHealthClaim(ph.getId(), contractId, "서울병원", "S00",
                LocalDate.now(), 2000000, 2000000, List.of());
    }

    @Test
    void 배정담당자가_승인하면_200() throws Exception {
        InsuranceEmployee staff = emp("e@t.com");
        HealthInsuranceClaim claim = complexClaim();   // 유일 직원에게 자동배정
        String token = jwtTokenProvider.createToken(staff.getId(), "EMPLOYEE");

        mockMvc.perform(post("/staff/benefit-reviews/" + claim.getId() + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"APPROVED\",\"comment\":\"정상\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 가입자_토큰으로_직원API_접근하면_403() throws Exception {
        Policyholder ph = userRepository.save(new Policyholder("홍", "p@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        String token = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        mockMvc.perform(get("/staff/benefit-reviews")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
```

> 참고: `JwtTokenProvider.createToken(Long, String)` 시그니처는 이슈 A에서 확인됨("EMPLOYEE"/"POLICYHOLDER"). 실제와 다르면 기존 ReviewControllerTest 패턴을 따른다.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*StaffReviewControllerTest'`
Expected: FAIL — 엔드포인트/보안규칙 없음.

- [ ] **Step 3: Write minimal implementation (DTOs)**

```java
// ConfirmBenefitReviewRequest.java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.ReviewResult;
import jakarta.validation.constraints.NotNull;

public record ConfirmBenefitReviewRequest(@NotNull ReviewResult result, String comment) {}
```

```java
// AssignStaffRequest.java
package com.distribution.insurance.web.dto;

import jakarta.validation.constraints.NotNull;

public record AssignStaffRequest(@NotNull Long employeeId) {}
```

```java
// BenefitReviewResultResponse.java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.BenefitPaymentReview;

public record BenefitReviewResultResponse(Long claimId, String result, String claimStatus) {
    public static BenefitReviewResultResponse from(BenefitPaymentReview review) {
        return new BenefitReviewResultResponse(
                review.getClaim().getId(),
                review.getResult() == null ? null : review.getResult().name(),
                review.getClaim().getStatus().name());
    }
}
```

```java
// BenefitReviewSummaryResponse.java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.BenefitPaymentReview;

public record BenefitReviewSummaryResponse(Long claimId, int requestAmount, String hospitalName, String claimStatus) {
    public static BenefitReviewSummaryResponse from(BenefitPaymentReview review) {
        var claim = review.getClaim();
        return new BenefitReviewSummaryResponse(
                claim.getId(), claim.getRequestAmount(), claim.getHospitalName(), claim.getStatus().name());
    }
}
```

```java
// BenefitReviewDetailResponse.java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.BenefitPaymentReview;

public record BenefitReviewDetailResponse(Long claimId, int requestAmount, String hospitalName,
                                          String diagnosisCode, String claimStatus, Long assignedStaffId) {
    public static BenefitReviewDetailResponse from(BenefitPaymentReview review) {
        var claim = review.getClaim();
        return new BenefitReviewDetailResponse(
                claim.getId(), claim.getRequestAmount(), claim.getHospitalName(),
                claim.getDiagnosisCode(), claim.getStatus().name(), review.getAssignedStaffId());
    }
}
```

- [ ] **Step 4: Write minimal implementation (Controller)**

```java
// StaffReviewController.java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.repository.BenefitPaymentReviewRepository;
import com.distribution.insurance.service.BenefitReviewService;
import com.distribution.insurance.service.StaffAssignmentService;
import com.distribution.insurance.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/staff")
public class StaffReviewController {

    private final BenefitReviewService reviewService;
    private final StaffAssignmentService assignmentService;
    private final BenefitPaymentReviewRepository reviewRepository;

    public StaffReviewController(BenefitReviewService reviewService,
                                 StaffAssignmentService assignmentService,
                                 BenefitPaymentReviewRepository reviewRepository) {
        this.reviewService = reviewService;
        this.assignmentService = assignmentService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/benefit-reviews")
    public List<BenefitReviewSummaryResponse> assigned(@AuthenticationPrincipal Long staffId) {
        return reviewService.assignedReviews(staffId).stream()
                .map(BenefitReviewSummaryResponse::from)
                .toList();
    }

    @GetMapping("/benefit-reviews/{claimId}")
    public BenefitReviewDetailResponse detail(@AuthenticationPrincipal Long staffId, @PathVariable Long claimId) {
        return BenefitReviewDetailResponse.from(reviewService.detail(staffId, claimId));
    }

    @PostMapping("/benefit-reviews/{claimId}/confirm")
    public BenefitReviewResultResponse confirm(@AuthenticationPrincipal Long staffId,
                                               @PathVariable Long claimId,
                                               @Valid @RequestBody ConfirmBenefitReviewRequest request) {
        BenefitPaymentReview review = reviewService.confirm(staffId, claimId, request.result(), request.comment());
        return BenefitReviewResultResponse.from(review);
    }

    @PostMapping("/claims/{claimId}/assign")
    public void assign(@PathVariable Long claimId, @Valid @RequestBody AssignStaffRequest request) {
        BenefitPaymentReview review = reviewRepository.findByClaimId(claimId)
                .orElseThrow(() -> new IllegalArgumentException("심사 건을 찾을 수 없습니다."));
        assignmentService.assignManually(review, request.employeeId());
    }

    @PostMapping("/benefit-reviews/{claimId}/retry")
    public void retry(@PathVariable Long claimId) {
        reviewService.retryPayout(claimId);
    }
}
```

- [ ] **Step 5: Modify SecurityConfig** — `/reviews/**` 규칙 아래에 추가:

```java
                        .requestMatchers("/staff/**").hasRole("EMPLOYEE")
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*StaffReviewControllerTest'`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/controller/StaffReviewController.java \
        backend/src/main/java/com/distribution/insurance/web/dto/BenefitReview*.java \
        backend/src/main/java/com/distribution/insurance/web/dto/ConfirmBenefitReviewRequest.java \
        backend/src/main/java/com/distribution/insurance/web/dto/AssignStaffRequest.java \
        backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java \
        backend/src/test/java/com/distribution/insurance/web/controller/StaffReviewControllerTest.java
git commit -m "Epic4-B: StaffReviewController(/staff/**) + DTO + EMPLOYEE 보안규칙"
```

---

## Task 9: 전체 회귀 + 통합 확인

- [ ] **Step 1: 전체 테스트**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL — 이슈 A·B 신규 + 기존 전부 통과.

- [ ] **Step 2: 스트레이 파일 확인**

Run: `git -C /Users/heeyoon/Desktop/insurance status --short`
Expected: 업로드 잔여 파일 없음(이슈 A SIMPLE 지급은 첨부 없을 수 있으나, 테스트 첨부는 @TempDir 격리).

- [ ] **Step 3: (필요시) 커밋** — Task 8까지 각자 커밋했으면 추가 커밋 불필요.

---

## Self-Review (작성자 점검 결과)

**Spec 커버리지(이슈 B):**
- UC14 자동배정(최소부하) → Task 5 ✓ / 수동배정(A1) → Task 5·8 ✓ / 배정직원 없음(E1) → Task 5 ✓
- UC12 배정목록 → Task 7·8 ✓ / 상세 → Task 7·8 ✓ / 승인→지급 → Task 7 ✓ / 반려(A1) → Task 7 ✓ / 중복접근(E2) → Task 7(409) ✓
- UC17 승인 지급 재사용 → Task 7(payoutService) ✓ / 송금실패 재시도(E1) → Task 2·7·8 ✓
- COMPLEX 청구 흐름 연결(PENDING→IN_REVIEW) → Task 6 ✓
- ADR 0001(메서드=책임: review.approve()→BenefitPayment를 서비스가 조율) ✓

**미커버(의도적):** 자동차사고(C), 조회·분석(D).

**타입 일관성:** `assignTo(Long)`, `assignAutomatically/assignManually(review[,id])`, `confirm(staffId, claimId, result, comment)`, `retryPayout(claimId)`, `payoutService.pay(claim)` — Task 간 동일.

**확인 필요(구현 시 대조):** `JwtTokenProvider.createToken(Long,String)` 시그니처(이슈 A 확인됨), Review SINGLE_TABLE 매핑 동작, `Policyholder.updateProfile` 존재(기존 코드 확인됨).

**E2 상태코드 결정:** spec은 409 권장. 비배정 접근을 `IllegalStateTransitionException`(→409)로 통일(403 아님). 사유: "이미 다른 담당자가 처리 중" 충돌 의미가 409에 더 부합.

---

## Execution Handoff

CLAUDE.md 규약대로 **이슈 브랜치**(`epic4-B-benefit-review-assignment`, base: epic4-A)에서 작업. 실행은 **Subagent-Driven**(태스크별 새 서브에이전트 + spec준수·코드품질 2단계 리뷰).
