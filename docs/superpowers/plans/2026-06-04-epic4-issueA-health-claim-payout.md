# Epic 4 이슈 A — 의료보험 청구(UC05) + 보험금 즉시지급(UC17) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 가입자가 유효한 의료보험 계약에 의료보험 청구를 제출하면, 청구금액으로 SIMPLE/COMPLEX를 판별해 SIMPLE은 즉시 보험금을 지급(송금)하고 COMPLEX는 심사 대기(PENDING)로 둔다.

**Architecture:** 클래스 다이어그램(04_claim, 06_payment)대로 `Claim`(abstract) → `HealthInsuranceClaim`, `ClaimAttachment`, `BenefitPayment` 엔티티를 만든다. 외부 송금은 기존 `PaymentGateway`와 분리한 `BenefitTransferGateway` 포트(ADR 0008)로, 증빙은 `FileStorage`로 로컬 디스크에 저장한다. SIMPLE 청구는 `BenefitPaymentReview` 없이 `BenefitPayment`를 직접 생성한다(ADR 0006). `ClaimStatus`는 다이어그램 4값에 `COMPLETED`/`FAILED`를 더한 6값(ADR 0007).

**Tech Stack:** Spring Boot 4, Java 21, Spring Data JPA, Spring MVC(멀티파트), MySQL, Lombok, JUnit5 + AssertJ.

**입력 문서(준수 필수):**
- spec: `docs/superpowers/specs/2026-06-04-epic4-claim-payout-design.md`
- 용어: `CONTEXT.md` (Claim, HealthInsuranceClaim, ClaimAttachment, ClaimStatus, ClaimComplexity, BenefitPayment, BenefitTransferGateway — 동의어 금지)
- 결정: ADR 0006(SIMPLE 직접지급), 0007(ClaimStatus 확장), 0008(송금 포트 분리)

**기존 코드 규약(맞출 것):**
- 컨트롤러 경로엔 `/api` 접두사 없음(예: `ApplicationController`는 `/applications`). 본 이슈는 `/claims`.
- 현재 사용자: `@AuthenticationPrincipal Long userId`.
- 예외→HTTP: `IllegalArgumentException`→404, `InvalidRequestException`→400, `IllegalStateException`→403 (전역 `GlobalExceptionHandler`가 이미 매핑).
- 알림: `NotificationSender.send(email, phone, message)`.
- 테스트: `@SpringBootTest @Transactional`, AssertJ(`org.assertj.core.api.Assertions.*`), 한글 메서드명.

**범위 경계:** COMPLEX 청구의 담당자 자동배정·심사(UC14/UC12)는 **이슈 B**. 본 이슈에서 COMPLEX는 `PENDING` + "담당자 배정 예정" 안내까지만 처리한다. 자동차사고(UC09)는 이슈 C.

---

## File Structure

신규(`domain/claim/`):
- `ClaimStatus.java` — enum PENDING/IN_REVIEW/APPROVED/REJECTED/COMPLETED/FAILED
- `ClaimComplexity.java` — enum SIMPLE/COMPLEX
- `Claim.java` — abstract 엔티티(JOINED 상속), 공통 필드 + status 전이
- `HealthInsuranceClaim.java` — Claim 상속, 의료 필드 + complexity + 첨부 컬렉션
- `ClaimAttachment.java` — 증빙 메타(@Embeddable, ElementCollection)
- `BenefitPayment.java` — 보험금 지급 1건(엔티티). 상태는 기존 `contract.PaymentStatus` 재사용

신규(`service/`):
- `BenefitTransferGateway.java` (interface) + `MockBenefitTransferGateway.java`
- `FileStorage.java` (interface) + `LocalFileStorage.java`
- `AttachmentValidator.java` — 허용 타입/크기 검증(정적 헬퍼 or 컴포넌트)
- `BenefitPayoutService.java` — UC17
- `ClaimService.java` — UC05

신규(`repository/`): `ClaimRepository.java`, `BenefitPaymentRepository.java`
신규(`web/`): `ClaimController.java`, `web/dto/HealthClaimResultResponse.java`
설정: `application.properties`에 `insurance.claim.complex-threshold`, `insurance.upload.dir`

테스트: 위 각 단위별 테스트 + `web/controller/ClaimControllerTest.java`.

---

## Task 1: ClaimStatus / ClaimComplexity enums

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/claim/ClaimStatus.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/claim/ClaimComplexity.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/claim/ClaimEnumTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.claim;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ClaimEnumTest {

    @Test
    void ClaimStatus는_6개_값을_가진다() {
        assertThat(ClaimStatus.values())
                .containsExactly(ClaimStatus.PENDING, ClaimStatus.IN_REVIEW,
                        ClaimStatus.APPROVED, ClaimStatus.REJECTED,
                        ClaimStatus.COMPLETED, ClaimStatus.FAILED);
    }

    @Test
    void ClaimComplexity는_SIMPLE과_COMPLEX를_가진다() {
        assertThat(ClaimComplexity.values())
                .containsExactly(ClaimComplexity.SIMPLE, ClaimComplexity.COMPLEX);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimEnumTest'`
Expected: FAIL — `ClaimStatus`/`ClaimComplexity` 심볼 없음(compile error).

- [ ] **Step 3: Write minimal implementation**

```java
// ClaimStatus.java
package com.distribution.insurance.domain.claim;

/** Claim 처리 상태(ADR 0007: 다이어그램 4값 + COMPLETED/FAILED). */
public enum ClaimStatus {
    PENDING, IN_REVIEW, APPROVED, REJECTED, COMPLETED, FAILED
}
```

```java
// ClaimComplexity.java
package com.distribution.insurance.domain.claim;

/** HealthInsuranceClaim 복잡도. SIMPLE=즉시지급, COMPLEX=심사. */
public enum ClaimComplexity {
    SIMPLE, COMPLEX
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimEnumTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/claim/ClaimStatus.java \
        backend/src/main/java/com/distribution/insurance/domain/claim/ClaimComplexity.java \
        backend/src/test/java/com/distribution/insurance/domain/claim/ClaimEnumTest.java
git commit -m "Epic4-A: ClaimStatus/ClaimComplexity enum"
```

---

## Task 2: ClaimAttachment (증빙 메타, @Embeddable)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/claim/ClaimAttachment.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/claim/ClaimAttachmentTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.claim;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ClaimAttachmentTest {

    @Test
    void 첨부메타는_파일명_타입_크기_저장경로를_보존한다() {
        ClaimAttachment a = new ClaimAttachment("receipt.pdf", "application/pdf", 2048L, "/up/1/uuid_receipt.pdf");
        assertThat(a.getFilename()).isEqualTo("receipt.pdf");
        assertThat(a.getContentType()).isEqualTo("application/pdf");
        assertThat(a.getSizeBytes()).isEqualTo(2048L);
        assertThat(a.getStoredPath()).isEqualTo("/up/1/uuid_receipt.pdf");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimAttachmentTest'`
Expected: FAIL — `ClaimAttachment` 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.domain.claim;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Claim 증빙 1건의 메타(바이너리는 디스크, 엔티티는 메타만). */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ClaimAttachment {

    private String filename;
    private String contentType;
    private long sizeBytes;
    private String storedPath;

    public ClaimAttachment(String filename, String contentType, long sizeBytes, String storedPath) {
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storedPath = storedPath;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimAttachmentTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/claim/ClaimAttachment.java \
        backend/src/test/java/com/distribution/insurance/domain/claim/ClaimAttachmentTest.java
git commit -m "Epic4-A: ClaimAttachment 증빙 메타"
```

---

## Task 3: Claim(abstract) + HealthInsuranceClaim 엔티티

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/claim/Claim.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/claim/HealthInsuranceClaim.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/claim/HealthInsuranceClaimTest.java`

상태 전이 규칙(Claim에 둠): `markInReview()`(PENDING→IN_REVIEW), `markCompleted()`(→COMPLETED), `markFailed()`(→FAILED), `markRejected()`(→REJECTED), `markApproved()`(IN_REVIEW→APPROVED). 본 이슈에선 `markCompleted/markFailed`만 사용(SIMPLE 경로). 잘못된 전이는 `IllegalStateTransitionException`.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class HealthInsuranceClaimTest {

    private InsuranceContract contract() {
        Policyholder ph = new Policyholder("홍길동", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789");
        HealthInsuranceProduct product = new HealthInsuranceProduct("건강플러스", "암", 30000, 120);
        return new InsuranceContract(ph, product, 30000, LocalDate.now());
    }

    @Test
    void 생성하면_PENDING이고_청구금액이_보존된다() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 500000, "서울병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(claim.getRequestAmount()).isEqualTo(500000);
        assertThat(claim.getComplexity()).isEqualTo(ClaimComplexity.SIMPLE);
        assertThat(claim.getHospitalName()).isEqualTo("서울병원");
    }

    @Test
    void 첨부를_추가하면_컬렉션에_쌓인다() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 500000, "서울병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);
        claim.addAttachment(new ClaimAttachment("r.pdf", "application/pdf", 1L, "/p/r.pdf"));
        assertThat(claim.getAttachments()).hasSize(1);
    }

    @Test
    void 지급완료_전이는_PENDING에서_COMPLETED로_간다() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 500000, "서울병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);
        claim.markCompleted();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 이미_완료된_건을_다시_완료하면_예외() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 500000, "서울병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);
        claim.markCompleted();
        assertThatThrownBy(claim::markCompleted).isInstanceOf(IllegalStateTransitionException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*HealthInsuranceClaimTest'`
Expected: FAIL — `Claim`/`HealthInsuranceClaim` 없음.

- [ ] **Step 3: Write minimal implementation**

```java
// Claim.java
package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.service.IllegalStateTransitionException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 보상 처리 1건의 추상 부모(04_claim). InsuranceContract와 composition. */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "claim")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate claimDate;
    private int requestAmount;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private InsuranceContract contract;

    protected Claim(InsuranceContract contract, int requestAmount) {
        this.contract = contract;
        this.requestAmount = requestAmount;
        this.claimDate = LocalDate.now();
        this.status = ClaimStatus.PENDING;
    }

    public void markInReview() {
        requireStatus(ClaimStatus.PENDING);
        this.status = ClaimStatus.IN_REVIEW;
    }

    public void markApproved() {
        requireStatus(ClaimStatus.IN_REVIEW);
        this.status = ClaimStatus.APPROVED;
    }

    public void markRejected() {
        requireStatus(ClaimStatus.IN_REVIEW);
        this.status = ClaimStatus.REJECTED;
    }

    public void markCompleted() {
        if (status == ClaimStatus.COMPLETED) {
            throw new IllegalStateTransitionException("이미 지급 완료된 청구입니다.");
        }
        this.status = ClaimStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = ClaimStatus.FAILED;
    }

    private void requireStatus(ClaimStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateTransitionException(
                    "현재 상태(" + status + ")에서 허용되지 않는 전이입니다.");
        }
    }
}
```

```java
// HealthInsuranceClaim.java
package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 의료보험 청구(UC05). 청구금액으로 complexity 판별. */
@Entity
@Table(name = "health_insurance_claim")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class HealthInsuranceClaim extends Claim {

    private String hospitalName;
    private String diagnosisCode;
    private LocalDate treatmentDate;
    private int receiptAmount;

    @Enumerated(EnumType.STRING)
    private ClaimComplexity complexity;

    @ElementCollection
    @CollectionTable(name = "health_claim_attachment", joinColumns = @JoinColumn(name = "claim_id"))
    private List<ClaimAttachment> attachments = new ArrayList<>();

    public HealthInsuranceClaim(InsuranceContract contract, int requestAmount,
                                String hospitalName, String diagnosisCode, LocalDate treatmentDate,
                                int receiptAmount, ClaimComplexity complexity) {
        super(contract, requestAmount);
        this.hospitalName = hospitalName;
        this.diagnosisCode = diagnosisCode;
        this.treatmentDate = treatmentDate;
        this.receiptAmount = receiptAmount;
        this.complexity = complexity;
    }

    public void addAttachment(ClaimAttachment attachment) {
        this.attachments.add(attachment);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*HealthInsuranceClaimTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/claim/Claim.java \
        backend/src/main/java/com/distribution/insurance/domain/claim/HealthInsuranceClaim.java \
        backend/src/test/java/com/distribution/insurance/domain/claim/HealthInsuranceClaimTest.java
git commit -m "Epic4-A: Claim(abstract)+HealthInsuranceClaim 엔티티"
```

---

## Task 4: BenefitPayment 엔티티

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/claim/BenefitPayment.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/claim/BenefitPaymentTest.java`

상태는 기존 `com.distribution.insurance.domain.contract.PaymentStatus`(SUCCESS/FAILED)를 재사용한다(값 동일, 중복 enum 신설 금지 — DRY). 정적 팩토리 `success(claim, amount, bankAccount)` / `failed(claim, amount, bankAccount)`.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.contract.PaymentStatus;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BenefitPaymentTest {

    private HealthInsuranceClaim claim() {
        Policyholder ph = new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789");
        var product = new HealthInsuranceProduct("건강", "암", 30000, 120);
        var contract = new InsuranceContract(ph, product, 30000, LocalDate.now());
        return new HealthInsuranceClaim(contract, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);
    }

    @Test
    void 성공지급은_SUCCESS상태와_지급금액_계좌를_가진다() {
        BenefitPayment p = BenefitPayment.success(claim(), 500000, "110-123-456789");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(p.getPaidAmount()).isEqualTo(500000);
        assertThat(p.getBankAccount()).isEqualTo("110-123-456789");
        assertThat(p.getPaidAt()).isNotNull();
    }

    @Test
    void 실패지급은_FAILED상태다() {
        BenefitPayment p = BenefitPayment.failed(claim(), 500000, "110-123-456789");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*BenefitPaymentTest'`
Expected: FAIL — `BenefitPayment` 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 보험금 지급 1건(06_payment, UC17). SIMPLE 청구는 심사 없이 직접 생성(ADR 0006). */
@Entity
@Table(name = "benefit_payment")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BenefitPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int paidAmount;
    private LocalDateTime paidAt;
    private String bankAccount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private Claim claim;

    private BenefitPayment(Claim claim, int paidAmount, String bankAccount, PaymentStatus status) {
        this.claim = claim;
        this.paidAmount = paidAmount;
        this.bankAccount = bankAccount;
        this.status = status;
        this.paidAt = LocalDateTime.now();
    }

    public static BenefitPayment success(Claim claim, int paidAmount, String bankAccount) {
        return new BenefitPayment(claim, paidAmount, bankAccount, PaymentStatus.SUCCESS);
    }

    public static BenefitPayment failed(Claim claim, int paidAmount, String bankAccount) {
        return new BenefitPayment(claim, paidAmount, bankAccount, PaymentStatus.FAILED);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*BenefitPaymentTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/claim/BenefitPayment.java \
        backend/src/test/java/com/distribution/insurance/domain/claim/BenefitPaymentTest.java
git commit -m "Epic4-A: BenefitPayment 엔티티(기존 PaymentStatus 재사용)"
```

---

## Task 5: Repositories (ClaimRepository, BenefitPaymentRepository)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/repository/ClaimRepository.java`
- Create: `backend/src/main/java/com/distribution/insurance/repository/BenefitPaymentRepository.java`
- Test: `backend/src/test/java/com/distribution/insurance/repository/ClaimRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.ClaimComplexity;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
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
class ClaimRepositoryTest {

    @Autowired ClaimRepository claimRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    @Test
    void 계약별로_청구를_조회한다() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        claimRepository.save(new HealthInsuranceClaim(contract, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));

        List<com.distribution.insurance.domain.claim.Claim> found = claimRepository.findByContractId(contract.getId());
        assertThat(found).hasSize(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimRepositoryTest'`
Expected: FAIL — `ClaimRepository` 없음.

- [ ] **Step 3: Write minimal implementation**

```java
// ClaimRepository.java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByContractId(Long contractId);
}
```

```java
// BenefitPaymentRepository.java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.BenefitPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitPaymentRepository extends JpaRepository<BenefitPayment, Long> {
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimRepositoryTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/repository/ClaimRepository.java \
        backend/src/main/java/com/distribution/insurance/repository/BenefitPaymentRepository.java \
        backend/src/test/java/com/distribution/insurance/repository/ClaimRepositoryTest.java
git commit -m "Epic4-A: ClaimRepository/BenefitPaymentRepository"
```

---

## Task 6: BenefitTransferGateway 포트 + Mock (ADR 0008)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/BenefitTransferGateway.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/MockBenefitTransferGateway.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/MockBenefitTransferGatewayTest.java`

Mock 결정적 동작(기존 `MockPaymentGateway` 패턴): 계좌가 `null`/빈문자/`"0000"`으로 끝나면 실패, 그 외 성공.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MockBenefitTransferGatewayTest {

    private final BenefitTransferGateway gateway = new MockBenefitTransferGateway();

    @Test
    void 정상_계좌면_송금_성공() {
        BenefitTransferGateway.Result r = gateway.transfer("110-123-456789", 500000);
        assertThat(r.success()).isTrue();
    }

    @Test
    void 계좌가_0000으로_끝나면_송금_실패() {
        BenefitTransferGateway.Result r = gateway.transfer("110-123-450000", 500000);
        assertThat(r.success()).isFalse();
        assertThat(r.reason()).isNotBlank();
    }

    @Test
    void 계좌가_비면_송금_실패() {
        assertThat(gateway.transfer("", 500000).success()).isFalse();
        assertThat(gateway.transfer(null, 500000).success()).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*MockBenefitTransferGatewayTest'`
Expected: FAIL — 타입 없음.

- [ ] **Step 3: Write minimal implementation**

```java
// BenefitTransferGateway.java
package com.distribution.insurance.service;

/** 보험금을 가입자 계좌로 송금하는 외부 포트(ADR 0008). 수납용 PaymentGateway와 분리. */
public interface BenefitTransferGateway {

    Result transfer(String bankAccount, int amount);

    record Result(boolean success, String reason) {
        public static Result ok() { return new Result(true, null); }
        public static Result fail(String reason) { return new Result(false, reason); }
    }
}
```

```java
// MockBenefitTransferGateway.java
package com.distribution.insurance.service;

import org.springframework.stereotype.Component;

/**
 * 송금 시뮬레이션. 계좌가 비었거나 "0000"으로 끝나면 실패(UC17 E1), 그 외 성공.
 * 결정적 동작이라 성공/실패 경로를 테스트로 재현할 수 있다.
 */
@Component
public class MockBenefitTransferGateway implements BenefitTransferGateway {

    @Override
    public Result transfer(String bankAccount, int amount) {
        if (bankAccount == null || bankAccount.isBlank()) {
            return Result.fail("계좌 정보가 없습니다.");
        }
        if (bankAccount.endsWith("0000")) {
            return Result.fail("계좌 번호 오류 또는 은행 시스템 문제");
        }
        return Result.ok();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*MockBenefitTransferGatewayTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/BenefitTransferGateway.java \
        backend/src/main/java/com/distribution/insurance/service/MockBenefitTransferGateway.java \
        backend/src/test/java/com/distribution/insurance/service/MockBenefitTransferGatewayTest.java
git commit -m "Epic4-A: BenefitTransferGateway 포트+Mock(ADR 0008)"
```

---

## Task 7: 첨부 검증 + FileStorage(로컬 디스크)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/AttachmentValidator.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/FileStorage.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/LocalFileStorage.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/AttachmentValidatorTest.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/LocalFileStorageTest.java`

검증 규칙: 허용 contentType = `application/pdf`, `image/jpeg`, `image/png`. 위반 시 `InvalidRequestException("지원하지 않는 파일 형식입니다. (허용: PDF, JPG, PNG)")`. 크기 > 10MB(10*1024*1024) 시 `InvalidRequestException("파일 크기는 개당 10MB 이하여야 합니다.")`.

- [ ] **Step 1: Write the failing test (AttachmentValidator)**

```java
package com.distribution.insurance.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;

class AttachmentValidatorTest {

    private final AttachmentValidator validator = new AttachmentValidator();

    @Test
    void 허용타입은_통과한다() {
        MultipartFile f = new MockMultipartFile("file", "r.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThatNoException().isThrownBy(() -> validator.validate(f));
    }

    @Test
    void 허용되지_않은_타입은_예외() {
        MultipartFile f = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1});
        assertThatThrownBy(() -> validator.validate(f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("지원하지 않는 파일 형식");
    }

    @Test
    void 10MB_초과는_예외() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        MultipartFile f = new MockMultipartFile("file", "big.png", "image/png", big);
        assertThatThrownBy(() -> validator.validate(f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("10MB");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*AttachmentValidatorTest'`
Expected: FAIL — `AttachmentValidator` 없음.

- [ ] **Step 3: Write minimal implementation (AttachmentValidator)**

```java
package com.distribution.insurance.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/** 청구 증빙 검증: 허용 타입(PDF/JPG/PNG), 개당 10MB 이하(UC05 E1, UC09 E1). */
@Component
public class AttachmentValidator {

    private static final Set<String> ALLOWED = Set.of("application/pdf", "image/jpeg", "image/png");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    public void validate(MultipartFile file) {
        if (!ALLOWED.contains(file.getContentType())) {
            throw new InvalidRequestException("지원하지 않는 파일 형식입니다. (허용: PDF, JPG, PNG)");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidRequestException("파일 크기는 개당 10MB 이하여야 합니다.");
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*AttachmentValidatorTest'`
Expected: PASS

- [ ] **Step 5: Write the failing test (LocalFileStorage)**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageTest {

    @Test
    void 파일을_저장하고_메타를_반환한다(@TempDir Path tempDir) throws IOException {
        FileStorage storage = new LocalFileStorage(tempDir.toString());
        MultipartFile f = new MockMultipartFile("file", "r.pdf", "application/pdf", new byte[]{1, 2, 3});

        ClaimAttachment meta = storage.store(100L, f);

        assertThat(meta.getFilename()).isEqualTo("r.pdf");
        assertThat(meta.getContentType()).isEqualTo("application/pdf");
        assertThat(meta.getSizeBytes()).isEqualTo(3);
        assertThat(Files.exists(Path.of(meta.getStoredPath()))).isTrue();
        assertThat(meta.getStoredPath()).contains("100");
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*LocalFileStorageTest'`
Expected: FAIL — `FileStorage`/`LocalFileStorage` 없음.

- [ ] **Step 7: Write minimal implementation (FileStorage + LocalFileStorage)**

```java
// FileStorage.java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import org.springframework.web.multipart.MultipartFile;

/** 청구 증빙 바이너리를 저장하고 메타를 반환한다. */
public interface FileStorage {
    ClaimAttachment store(Long claimId, MultipartFile file);
}
```

```java
// LocalFileStorage.java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** 로컬 디스크 저장: {upload.dir}/{claimId}/{uuid}_{원본파일명}. */
@Component
public class LocalFileStorage implements FileStorage {

    private final String baseDir;

    public LocalFileStorage(@Value("${insurance.upload.dir:./uploads/claims}") String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public ClaimAttachment store(Long claimId, MultipartFile file) {
        try {
            Path dir = Path.of(baseDir, String.valueOf(claimId));
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            Path target = dir.resolve(UUID.randomUUID() + "_" + original);
            file.transferTo(target);
            return new ClaimAttachment(original, file.getContentType(), file.getSize(),
                    target.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new UncheckedIOException("증빙 저장 실패", e);
        }
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*LocalFileStorageTest'`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/AttachmentValidator.java \
        backend/src/main/java/com/distribution/insurance/service/FileStorage.java \
        backend/src/main/java/com/distribution/insurance/service/LocalFileStorage.java \
        backend/src/test/java/com/distribution/insurance/service/AttachmentValidatorTest.java \
        backend/src/test/java/com/distribution/insurance/service/LocalFileStorageTest.java
git commit -m "Epic4-A: 첨부 검증(AttachmentValidator)+로컬 저장(FileStorage)"
```

---

## Task 8: BenefitPayoutService (UC17 보험금 지급)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/BenefitPayoutService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/BenefitPayoutServiceTest.java`

책임: 청구의 계약→가입자 `bankAccount`로 `requestAmount` 송금. 성공 시 `BenefitPayment.success` 저장 + `claim.markCompleted()` + 가입자 알림. 실패 시 `BenefitPayment.failed` 저장 + `claim.markFailed()` + 담당직원 알림(가입자 아님). 예외를 던지지 않고 결과 상태로 표현(UC17 E1).

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.contract.PaymentStatus;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BenefitPayoutServiceTest {

    @Autowired BenefitPayoutService payoutService;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentRepository benefitPaymentRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private HealthInsuranceClaim savedClaim(String bankAccount) {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", bankAccount));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        return claimRepository.save(new HealthInsuranceClaim(contract, 500000, "병원", "S00",
                LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
    }

    @Test
    void 정상계좌면_송금성공_COMPLETED_지급기록_SUCCESS() {
        HealthInsuranceClaim claim = savedClaim("110-123-456789");

        BenefitPayment payment = payoutService.pay(claim);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAmount()).isEqualTo(500000);
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 계좌오류면_송금실패_FAILED_지급기록_FAILED() {
        HealthInsuranceClaim claim = savedClaim("110-123-450000");

        BenefitPayment payment = payoutService.pay(claim);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.FAILED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*BenefitPayoutServiceTest'`
Expected: FAIL — `BenefitPayoutService` 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.BenefitPayment;
import com.distribution.insurance.domain.claim.Claim;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.BenefitPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 보험금 지급(UC17). 송금 실패는 예외가 아니라 FAILED 상태로 기록(E1). */
@Service
public class BenefitPayoutService {

    private final BenefitTransferGateway transferGateway;
    private final BenefitPaymentRepository benefitPaymentRepository;
    private final NotificationSender notificationSender;

    public BenefitPayoutService(BenefitTransferGateway transferGateway,
                                BenefitPaymentRepository benefitPaymentRepository,
                                NotificationSender notificationSender) {
        this.transferGateway = transferGateway;
        this.benefitPaymentRepository = benefitPaymentRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public BenefitPayment pay(Claim claim) {
        Policyholder ph = claim.getContract().getPolicyholder();
        String account = ph.getBankAccount();
        int amount = claim.getRequestAmount();

        BenefitTransferGateway.Result result = transferGateway.transfer(account, amount);

        if (result.success()) {
            claim.markCompleted();
            BenefitPayment payment = benefitPaymentRepository.save(
                    BenefitPayment.success(claim, amount, account));
            notificationSender.send(ph.getEmail(), ph.getPhone(),
                    "보험금 " + amount + "원이 지급되었습니다.");
            return payment;
        } else {
            claim.markFailed();
            BenefitPayment payment = benefitPaymentRepository.save(
                    BenefitPayment.failed(claim, amount, account));
            // 가입자가 아닌 직원에게 실패 알림(UC17 E1-2). 직원 식별은 이슈 B의 배정 정보 사용 전이므로
            // 본 이슈에서는 운영 채널(직원용 phone/email 미상)로 보낼 수 없어 로그성 알림만 남긴다.
            notificationSender.send(null, null,
                    "[직원알림] 청구 " + claim.getId() + " 보험금 송금 실패: " + result.reason());
            return payment;
        }
    }
}
```

> 참고: `NotificationSender.send(null, null, ...)`는 `LogNotificationSender`가 마스킹에서 null을 안전 처리(`"***"`)하므로 NPE 없음. 직원 대상 알림의 정식 수신자 지정은 이슈 B(담당자 배정)에서 보강.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*BenefitPayoutServiceTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/BenefitPayoutService.java \
        backend/src/test/java/com/distribution/insurance/service/BenefitPayoutServiceTest.java
git commit -m "Epic4-A: BenefitPayoutService(UC17 송금/성공실패 상태)"
```

---

## Task 9: ClaimService (UC05 의료보험 청구 + 복잡도 판별 + SIMPLE 즉시지급)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/ClaimService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/ClaimServiceTest.java`

책임:
1. 계약 조회·소유권 검증(가입자 본인) → 아니면 `IllegalStateException`.
2. 계약 상품이 `HealthInsuranceProduct`인지 검증 → 아니면 `InvalidRequestException`("의료보험 계약이 아닙니다.").
3. 첨부 각각 `AttachmentValidator.validate` (저장은 컨트롤러에서 메타 생성 후 전달; 서비스는 메타 리스트를 받음).
4. 복잡도 판별: `requestAmount >= complexThreshold` → COMPLEX, else SIMPLE.
5. 청구 저장. SIMPLE → `BenefitPayoutService.pay()` 호출. COMPLEX → PENDING 유지 + "담당자 배정 예정" 안내.
6. 가입자 알림.

서비스 시그니처(컨트롤러가 멀티파트→메타 변환 후 호출):
`HealthInsuranceClaim fileHealthClaim(Long policyholderId, Long contractId, String hospitalName, String diagnosisCode, LocalDate treatmentDate, int requestAmount, int receiptAmount, List<ClaimAttachment> attachments)`

복잡도 임계값은 `@Value("${insurance.claim.complex-threshold:1000000}") int complexThreshold`.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
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
class ClaimServiceTest {

    @Autowired ClaimService claimService;
    @Autowired ClaimRepository claimRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Policyholder ph(String account) {
        return userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", account));
    }

    private InsuranceContract healthContract(Policyholder ph) {
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        return contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
    }

    @Test
    void 임계값_미만이면_SIMPLE이고_즉시_COMPLETED된다() {
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = healthContract(p);

        HealthInsuranceClaim claim = claimService.fileHealthClaim(
                p.getId(), c.getId(), "서울병원", "S00", LocalDate.now(), 500000, 500000, List.of());

        assertThat(claim.getComplexity()).isEqualTo(ClaimComplexity.SIMPLE);
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 임계값_이상이면_COMPLEX이고_PENDING으로_남는다() {
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = healthContract(p);

        HealthInsuranceClaim claim = claimService.fileHealthClaim(
                p.getId(), c.getId(), "서울병원", "S00", LocalDate.now(), 2000000, 2000000, List.of());

        assertThat(claim.getComplexity()).isEqualTo(ClaimComplexity.COMPLEX);
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.PENDING);
    }

    @Test
    void 본인계약이_아니면_403성_예외() {
        Policyholder owner = ph("110-123-456789");
        Policyholder other = ph("220-123-456789");
        InsuranceContract c = healthContract(owner);

        assertThatThrownBy(() -> claimService.fileHealthClaim(
                other.getId(), c.getId(), "서울병원", "S00", LocalDate.now(), 500000, 500000, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 자동차보험_계약에_의료청구하면_400성_예외() {
        Policyholder p = ph("110-123-456789");
        var product = productRepository.save(new CarInsuranceProduct("자동차", "대물", 50000, 12));
        InsuranceContract car = contractRepository.save(new InsuranceContract(p, product, 50000, LocalDate.now()));

        assertThatThrownBy(() -> claimService.fileHealthClaim(
                p.getId(), car.getId(), "서울병원", "S00", LocalDate.now(), 500000, 500000, List.of()))
                .isInstanceOf(InvalidRequestException.class);
    }
}
```

> 참고: `CarInsuranceProduct` 생성자 시그니처는 구현 시 `CarInsuranceProduct.java`에서 확인해 맞춘다(파라미터가 다르면 테스트의 생성 인자만 조정). 핵심은 "자동차 계약"을 만들어 의료청구가 거부되는지 검증하는 것.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimServiceTest'`
Expected: FAIL — `ClaimService` 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import com.distribution.insurance.domain.claim.ClaimComplexity;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ClaimRepository;
import com.distribution.insurance.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 의료보험 청구(UC05). 복잡도 판별 후 SIMPLE은 즉시지급, COMPLEX는 심사 대기. */
@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ContractRepository contractRepository;
    private final BenefitPayoutService payoutService;
    private final NotificationSender notificationSender;
    private final int complexThreshold;

    public ClaimService(ClaimRepository claimRepository,
                        ContractRepository contractRepository,
                        BenefitPayoutService payoutService,
                        NotificationSender notificationSender,
                        @Value("${insurance.claim.complex-threshold:1000000}") int complexThreshold) {
        this.claimRepository = claimRepository;
        this.contractRepository = contractRepository;
        this.payoutService = payoutService;
        this.notificationSender = notificationSender;
        this.complexThreshold = complexThreshold;
    }

    @Transactional
    public HealthInsuranceClaim fileHealthClaim(Long policyholderId, Long contractId,
                                                String hospitalName, String diagnosisCode,
                                                LocalDate treatmentDate, int requestAmount,
                                                int receiptAmount, List<ClaimAttachment> attachments) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        if (!contract.getPolicyholder().getId().equals(policyholderId)) {
            throw new IllegalStateException("본인 계약에만 청구할 수 있습니다.");
        }
        if (!(contract.getProduct() instanceof HealthInsuranceProduct)) {
            throw new InvalidRequestException("의료보험 계약이 아닙니다.");
        }

        ClaimComplexity complexity = requestAmount >= complexThreshold
                ? ClaimComplexity.COMPLEX : ClaimComplexity.SIMPLE;

        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract, requestAmount, hospitalName, diagnosisCode, treatmentDate, receiptAmount, complexity);
        attachments.forEach(claim::addAttachment);
        claimRepository.save(claim);

        Policyholder ph = contract.getPolicyholder();
        if (complexity == ClaimComplexity.SIMPLE) {
            payoutService.pay(claim);
        } else {
            notificationSender.send(ph.getEmail(), ph.getPhone(),
                    "청구가 접수되었습니다. 복잡한 청구로 담당자 배정 후 심사를 진행합니다.");
        }
        return claim;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimServiceTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/ClaimService.java \
        backend/src/test/java/com/distribution/insurance/service/ClaimServiceTest.java
git commit -m "Epic4-A: ClaimService(UC05 청구+복잡도판별+SIMPLE 즉시지급)"
```

---

## Task 10: ClaimController (멀티파트 POST /claims/health) + DTO

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/HealthClaimResultResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/ClaimController.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/ClaimControllerTest.java`

컨트롤러: 멀티파트 폼(`@RequestPart`/`@RequestParam` 메타 + `MultipartFile[] attachments`). 각 첨부 `AttachmentValidator.validate` → `FileStorage.store(임시 claimId?)`. **저장 경로 문제**: 파일 저장은 claimId가 필요하나 청구 저장 전엔 id가 없다. 해결: 컨트롤러에서 먼저 `ClaimService.fileHealthClaim(...)`를 빈 첨부로 호출하지 말고, **검증→저장은 메타 없이 먼저 청구를 만든 뒤 첨부를 붙이는** 순서가 꼬인다. → 단순화: `FileStorage.store`의 디렉터리 키를 claimId 대신 **랜덤 UUID 폴더**로 바꾸지 않고, 컨트롤러가 `userId`를 키로 저장(`store(userId, file)`)하여 메타 생성 → 서비스에 메타 리스트 전달. (claimId 하위 폴더가 필수 요건은 아님. spec의 `{claimId}` 경로는 권장일 뿐 — 본 구현은 `{userId}` 폴더로 저장하고 추적은 storedPath로 충분.)

> 결정: `FileStorage.store(Long key, MultipartFile)`의 `key`는 "폴더 구분용"이며 컨트롤러는 `userId`를 넘긴다. Task 7 테스트는 `store(100L, f)`로 이미 키-무관하게 통과한다.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void 의료보험_청구_멀티파트_요청이_201을_반환한다() throws Exception {
        Policyholder ph = userRepository.save(new Policyholder("홍", "claim@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract c = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        String token = jwtTokenProvider.createToken(ph.getId());

        MockMultipartFile file = new MockMultipartFile("attachments", "r.pdf", "application/pdf", new byte[]{1, 2});

        mockMvc.perform(multipart("/claims/health")
                        .file(file)
                        .param("contractId", String.valueOf(c.getId()))
                        .param("hospitalName", "서울병원")
                        .param("diagnosisCode", "S00")
                        .param("treatmentDate", LocalDate.now().toString())
                        .param("requestAmount", "500000")
                        .param("receiptAmount", "500000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.complexity").value("SIMPLE"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
```

> 참고: 토큰 생성 메서드명(`createToken`)·인증 헤더 방식은 구현 시 `JwtTokenProvider`/기존 `ApplicationControllerTest`에서 실제 시그니처를 확인해 맞춘다. 다르면 그쪽 패턴을 그대로 따른다.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimControllerTest'`
Expected: FAIL — `/claims/health` 핸들러 없음(404) 또는 컴파일 에러.

- [ ] **Step 3: Write minimal implementation (DTO)**

```java
// HealthClaimResultResponse.java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.claim.HealthInsuranceClaim;

public record HealthClaimResultResponse(Long claimId, String status, String complexity) {
    public static HealthClaimResultResponse from(HealthInsuranceClaim claim) {
        return new HealthClaimResultResponse(
                claim.getId(), claim.getStatus().name(), claim.getComplexity().name());
    }
}
```

- [ ] **Step 4: Write minimal implementation (Controller)**

```java
// ClaimController.java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.service.AttachmentValidator;
import com.distribution.insurance.service.ClaimService;
import com.distribution.insurance.service.FileStorage;
import com.distribution.insurance.web.dto.HealthClaimResultResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final AttachmentValidator attachmentValidator;
    private final FileStorage fileStorage;

    public ClaimController(ClaimService claimService, AttachmentValidator attachmentValidator,
                           FileStorage fileStorage) {
        this.claimService = claimService;
        this.attachmentValidator = attachmentValidator;
        this.fileStorage = fileStorage;
    }

    @PostMapping(path = "/health", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public HealthClaimResultResponse fileHealthClaim(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long contractId,
            @RequestParam String hospitalName,
            @RequestParam String diagnosisCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate treatmentDate,
            @RequestParam int requestAmount,
            @RequestParam int receiptAmount,
            @RequestParam(required = false) MultipartFile[] attachments) {

        List<ClaimAttachment> metas = new ArrayList<>();
        if (attachments != null) {
            for (MultipartFile f : attachments) {
                if (f.isEmpty()) continue;
                attachmentValidator.validate(f);
                metas.add(fileStorage.store(userId, f));
            }
        }

        HealthInsuranceClaim claim = claimService.fileHealthClaim(
                userId, contractId, hospitalName, diagnosisCode, treatmentDate,
                requestAmount, receiptAmount, metas);
        return HealthClaimResultResponse.from(claim);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimControllerTest'`
Expected: PASS

> 만약 Spring Security가 `/claims/**`를 막아 401이 나오면, 기존 `SecurityConfig`(또는 시큐리티 빈 설정 위치)에서 `/applications/**`·`/contracts/**`와 동일하게 인증 사용자 허용 규칙을 추가한다. 기존 컨트롤러와 같은 인증(가입자 토큰) 정책을 그대로 따른다.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/dto/HealthClaimResultResponse.java \
        backend/src/main/java/com/distribution/insurance/web/controller/ClaimController.java \
        backend/src/test/java/com/distribution/insurance/web/controller/ClaimControllerTest.java
git commit -m "Epic4-A: ClaimController 멀티파트 의료청구 엔드포인트"
```

---

## Task 11: 설정값 + 첨부 형식오류 통합 검증 + 전체 테스트

**Files:**
- Modify: `backend/src/main/resources/application.properties` (설정 추가)
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/ClaimControllerAttachmentTest.java`

- [ ] **Step 1: application.properties에 설정 추가**

```properties
# Epic 4 청구/지급
insurance.claim.complex-threshold=1000000
insurance.upload.dir=./uploads/claims
# 멀티파트 개당 10MB 제한(검증과 일관)
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

- [ ] **Step 2: Write the failing test (허용되지 않은 첨부 형식 → 400)**

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimControllerAttachmentTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void 허용되지_않은_첨부형식이면_400() throws Exception {
        Policyholder ph = userRepository.save(new Policyholder("홍", "att@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract c = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        String token = jwtTokenProvider.createToken(ph.getId());

        MockMultipartFile bad = new MockMultipartFile("attachments", "a.txt", "text/plain", new byte[]{1});

        mockMvc.perform(multipart("/claims/health")
                        .file(bad)
                        .param("contractId", String.valueOf(c.getId()))
                        .param("hospitalName", "서울병원")
                        .param("diagnosisCode", "S00")
                        .param("treatmentDate", LocalDate.now().toString())
                        .param("requestAmount", "500000")
                        .param("receiptAmount", "500000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 3: Run test to verify it passes (구현은 Task 7·10에서 이미 완료 — 통합 확인)**

Run: `cd backend && ./gradlew test --tests '*ClaimControllerAttachmentTest'`
Expected: PASS (이미 `AttachmentValidator`→`InvalidRequestException`→400 매핑 존재).

- [ ] **Step 4: 전체 테스트 회귀 확인**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL — 신규 + 기존 테스트 전부 통과.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/application.properties \
        backend/src/test/java/com/distribution/insurance/web/controller/ClaimControllerAttachmentTest.java
git commit -m "Epic4-A: 청구 설정값 + 첨부 형식오류 400 통합검증"
```

---

## Self-Review (작성자 점검 결과)

**Spec 커버리지(이슈 A 범위):**
- UC05 청구 입력·저장 → Task 3·9·10 ✓
- UC05 복잡도 자동 판별(임계값) → Task 9 ✓
- UC05 SIMPLE 즉시지급 → Task 8·9 ✓
- UC05 COMPLEX 분기(본 이슈는 PENDING+안내) → Task 9 ✓ (배정/심사는 이슈 B)
- UC05 E1 첨부 형식오류 → Task 7·11 ✓
- UC17 송금·성공(COMPLETED/BenefitPayment SUCCESS) → Task 8 ✓
- UC17 E1 송금실패(FAILED/직원알림) → Task 8 ✓
- ADR 0006(SIMPLE 직접지급) → Task 8(review 미생성) ✓
- ADR 0007(ClaimStatus 6값) → Task 1·3 ✓
- ADR 0008(송금 포트 분리) → Task 6 ✓
- 멀티파트 로컬 저장 → Task 7·10 ✓

**미커버(의도적, 이슈 B/C/D):** 담당자 자동배정·심사(UC12/14), 자동차사고(UC09), 조회·분석(UC03/04/11).

**타입 일관성:** `fileHealthClaim(...)` 시그니처, `pay(Claim)`, `store(Long, MultipartFile)`, `validate(MultipartFile)`, `transfer(String,int)` — Task 간 동일 사용 확인.

**확인 필요(구현 시 실제 코드와 대조):** `JwtTokenProvider.createToken` 시그니처, `CarInsuranceProduct` 생성자, Security 경로 허용 규칙. 각 Task 노트에 명시.

---

## Execution Handoff

이 plan을 실행할 때 CLAUDE.md 규약대로 **이슈 브랜치**(예: `epic4-A-health-claim-payout`)를 만들어 작업한다. 실행 방식은 **Subagent-Driven**(태스크별 새 서브에이전트 + spec준수·코드품질 2단계 리뷰)을 기본으로 한다.
