# Epic 3 이슈 A — 계약 생성 + 조회(UC08) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 심사 승인 시 `InsuranceContract`를 자동 생성하고, 가입자가 자기 계약 목록·상세·계약서 PDF를 조회할 수 있게 한다.

**Architecture:** `InsuranceContract` 엔티티를 신설하고, Epic 2의 `ReviewService.confirm()`이 승인(APPROVED/CONDITIONAL) 시 같은 트랜잭션에서 계약을 생성한다(ADR 0005). 조회는 가입자 본인 계약만 노출하며, 타인 접근은 거부한다. 계약서는 텍스트 기반 파일로 생성한다.

**Tech Stack:** Spring Boot 4 / Java 21, Spring Data JPA, Spring Security(JWT), JUnit5 + MockMvc, Lombok.

**준수 문서(반드시 따른다):**
- spec: `docs/superpowers/specs/2026-06-03-epic3-contract-billing-design.md`
- 용어: `CONTEXT.md` — `InsuranceContract`, `Payment`, `Notice`, `PaymentMethod` 등. 동의어(policy/contract 단독) 금지.
- 결정: ADR 0003(adjustedPremium은 항상 최종 보험료), ADR 0005(승인 시 자동 생성, 1년 기간).

**이슈 A 범위 노트:**
- 결제수단·자동이체·미납·납부는 **이슈 B**, 고지서는 **이슈 C**. 이 이슈에서 계약 상세의 "결제수단"은 상수 `"미등록"`으로 노출하고, 이슈 B에서 실제 값으로 대체한다.
- 수익자·특약은 도메인에 없으므로 제외(spec/ grill 결정).

---

## File Structure

- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/ContractStatus.java` — 계약 상태 enum.
- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/InsuranceContract.java` — 계약 엔티티.
- Create: `backend/src/main/java/com/distribution/insurance/repository/ContractRepository.java` — 계약 조회.
- Modify: `backend/src/main/java/com/distribution/insurance/service/ReviewService.java` — 승인 시 계약 생성.
- Create: `backend/src/main/java/com/distribution/insurance/service/ContractService.java` — 목록/상세/PDF.
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ContractSummaryResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ContractDetailResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/ContractController.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/contract/InsuranceContractTest.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/ReviewServiceContractTest.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/ContractControllerTest.java`

명령은 모두 `backend/`에서 실행: `cd backend && ./gradlew test ...`

---

## Task 1: ContractStatus enum + InsuranceContract 도메인

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/ContractStatus.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/InsuranceContract.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/contract/InsuranceContractTest.java`

- [ ] **Step 1: 실패 테스트 작성**

`InsuranceContractTest.java`:
```java
package com.distribution.insurance.domain.contract;

import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class InsuranceContractTest {

    private InsuranceContract newContract() {
        Policyholder ph = new Policyholder("hong@test.com", "pw", "홍길동",
                "900101-1234567", "010-1111-2222");
        InsuranceProduct product = new HealthInsuranceProduct("실손의료", "설명", 30000);
        return new InsuranceContract(ph, product, 33000, LocalDate.of(2026, 6, 3));
    }

    @Test
    void 생성_시_상태는_ACTIVE이고_종료일은_시작일_1년뒤() {
        InsuranceContract c = newContract();
        assertThat(c.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(c.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(c.getEndDate()).isEqualTo(LocalDate.of(2027, 6, 3));
        assertThat(c.getMonthlyPremium()).isEqualTo(33000);
    }

    @Test
    void suspend는_ACTIVE에서만_SUSPENDED로_전이() {
        InsuranceContract c = newContract();
        c.suspend();
        assertThat(c.getStatus()).isEqualTo(ContractStatus.SUSPENDED);
    }

    @Test
    void terminate된_계약은_다시_suspend할_수_없다() {
        InsuranceContract c = newContract();
        c.terminate();
        assertThat(c.getStatus()).isEqualTo(ContractStatus.TERMINATED);
        assertThatThrownBy(c::suspend).isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void generatePdf는_계약번호와_월보험료를_담은_바이트를_반환() {
        InsuranceContract c = newContract();
        byte[] pdf = c.generatePdf();
        String text = new String(pdf, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(text).contains("33000").contains("실손의료");
    }
}
```

확인: `Policyholder`·`HealthInsuranceProduct` 생성자 시그니처는 기존 코드 기준이다. 다르면 테스트의 생성자 인자를 실제 시그니처에 맞춘다(`Policyholder`는 `src/main/java/.../domain/user/Policyholder.java`, `HealthInsuranceProduct`는 `.../domain/product/HealthInsuranceProduct.java` 참고). 이 단계에서 먼저 두 파일을 열어 생성자 인자를 확인하고 테스트를 맞춘 뒤 진행한다.

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.InsuranceContractTest"`
Expected: 컴파일 실패(`ContractStatus`, `InsuranceContract` 없음).

- [ ] **Step 3: ContractStatus 작성**

`ContractStatus.java`:
```java
package com.distribution.insurance.domain.contract;

/** 계약 상태(03_contract 다이어그램). */
public enum ContractStatus {
    ACTIVE, SUSPENDED, TERMINATED
}
```

- [ ] **Step 4: InsuranceContract 작성**

`InsuranceContract.java`:
```java
package com.distribution.insurance.domain.contract;

import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.IllegalStateTransitionException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** 보험 계약(UC08). 심사 승인 시 생성된다(ADR 0005). */
@Entity
@Table(name = "insurance_contract")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InsuranceContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    private int monthlyPremium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policyholder_id")
    private Policyholder policyholder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private InsuranceProduct product;

    /** 계약 기간은 시작일 + 1년(ADR 0005). monthlyPremium은 adjustedPremium(ADR 0003). */
    public InsuranceContract(Policyholder policyholder, InsuranceProduct product,
                             int monthlyPremium, LocalDate startDate) {
        this.policyholder = policyholder;
        this.product = product;
        this.monthlyPremium = monthlyPremium;
        this.startDate = startDate;
        this.endDate = startDate.plusYears(1);
        this.status = ContractStatus.ACTIVE;
    }

    public void suspend() {
        if (this.status != ContractStatus.ACTIVE) {
            throw new IllegalStateTransitionException("정상 계약만 정지할 수 있습니다.");
        }
        this.status = ContractStatus.SUSPENDED;
    }

    public void terminate() {
        if (this.status == ContractStatus.TERMINATED) {
            throw new IllegalStateTransitionException("이미 해지된 계약입니다.");
        }
        this.status = ContractStatus.TERMINATED;
    }

    /** 텍스트 기반 계약서. UC08 6단계 '계약서 다운로드'. */
    public byte[] generatePdf() {
        String body = "보험 계약서\n"
                + "계약번호: " + id + "\n"
                + "상품명: " + product.getProductName() + "\n"
                + "계약기간: " + startDate + " ~ " + endDate + "\n"
                + "월 보험료: " + monthlyPremium + "원\n"
                + "상태: " + status + "\n";
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.InsuranceContractTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/contract backend/src/test/java/com/distribution/insurance/domain/contract
git commit -m "feat(epic3-A): InsuranceContract 도메인 + 상태전이/계약서 생성 (UC08)"
```

---

## Task 2: ContractRepository

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/repository/ContractRepository.java`

이 태스크는 인터페이스 선언만으로 Task 3·4 테스트에서 검증된다(단독 테스트 없음).

- [ ] **Step 1: 리포지토리 작성**

`ContractRepository.java`:
```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.contract.InsuranceContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<InsuranceContract, Long> {

    /** 가입자 본인 계약 목록(UC08). */
    List<InsuranceContract> findByPolicyholderId(Long policyholderId);
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/repository/ContractRepository.java
git commit -m "feat(epic3-A): ContractRepository"
```

---

## Task 3: 심사 승인 시 계약 자동 생성 (ReviewService 수정)

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/service/ReviewService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/ReviewServiceContractTest.java`

- [ ] **Step 1: 실패 테스트 작성**

`ReviewServiceContractTest.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.contract.ContractStatus;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ReviewServiceContractTest {

    @Autowired ReviewService reviewService;
    @Autowired ContractRepository contractRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    Long employeeId; Long applicationId; Long policyholderId;

    @BeforeEach
    void setUp() {
        InsuranceEmployee emp = userRepository.save(
                new InsuranceEmployee("emp@test.com", "pw", "심사원", "010-0000-0000"));
        employeeId = emp.getId();
        Policyholder ph = userRepository.save(new Policyholder(
                "ph@test.com", "pw", "홍길동", "900101-1234567", "010-1111-2222"));
        policyholderId = ph.getId();
        HealthInsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("실손의료", "설명", 30000));
        InsuranceApplication app = applicationRepository.save(
                new InsuranceApplication(ph, product, null, new MedicalHistory("없음")));
        applicationId = app.getId();
    }

    @AfterEach
    void tearDown() {
        contractRepository.deleteAll();
        applicationRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 일반승인_시_계약이_생성되고_월보험료는_상품_기본료() {
        reviewService.confirm(employeeId, applicationId, ReviewResult.APPROVED, "ok", null);

        List<InsuranceContract> contracts = contractRepository.findByPolicyholderId(policyholderId);
        assertThat(contracts).hasSize(1);
        InsuranceContract c = contracts.get(0);
        assertThat(c.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(c.getMonthlyPremium()).isEqualTo(30000);
        assertThat(c.getStartDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void 조건부승인_시_계약_월보험료는_할증_반영된_adjustedPremium() {
        reviewService.confirm(employeeId, applicationId, ReviewResult.CONDITIONAL, "할증", 0.2);

        InsuranceContract c = contractRepository.findByPolicyholderId(policyholderId).get(0);
        assertThat(c.getMonthlyPremium()).isEqualTo(36000); // 30000 * 1.2
    }

    @Test
    void 반려_시_계약은_생성되지_않는다() {
        reviewService.confirm(employeeId, applicationId, ReviewResult.REJECTED, "반려", null);
        assertThat(contractRepository.findByPolicyholderId(policyholderId)).isEmpty();
    }
}
```

확인: `InsuranceEmployee`·`Policyholder`·`HealthInsuranceProduct`·`MedicalHistory`의 생성자 시그니처는 기존 코드 기준 예시다. Step 시작 시 해당 도메인 파일과 기존 `ReviewControllerTest`/`ApplicationControllerTest`의 셋업을 열어 실제 생성자에 맞춘다.

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.service.ReviewServiceContractTest"`
Expected: FAIL (계약 0건 — 아직 생성 로직 없음).

- [ ] **Step 3: ReviewService에 계약 생성 추가**

`ReviewService.java` 수정:

(1) 필드/생성자에 `ContractRepository` 주입 추가:
```java
    private final ContractRepository contractRepository;
```
생성자 파라미터와 대입을 기존 5개 의존성 뒤에 추가한다(import `com.distribution.insurance.repository.ContractRepository`).

(2) `confirm()`의 승인 분기에서 계약 생성. 기존:
```java
        if (result == ReviewResult.REJECTED) {
            app.markRejected();
        } else {
            app.markApproved();
        }
        reviewRepository.save(review);
```
를 다음으로 교체:
```java
        if (result == ReviewResult.REJECTED) {
            app.markRejected();
        } else {
            app.markApproved();
            // ADR 0005: 승인 시 같은 트랜잭션에서 계약 생성.
            // ADR 0003: monthlyPremium은 결과 분기 없이 adjustedPremium 한 필드만 읽는다.
            contractRepository.save(new com.distribution.insurance.domain.contract.InsuranceContract(
                    app.getApplicant(), app.getProduct(),
                    review.getAdjustedPremium(), java.time.LocalDate.now()));
        }
        reviewRepository.save(review);
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.service.ReviewServiceContractTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: 회귀 확인(Epic 2 심사 테스트 깨지지 않음)**

Run: `cd backend && ./gradlew test --tests "*Review*"`
Expected: PASS (기존 심사 테스트 포함 전부 통과).

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/ReviewService.java backend/src/test/java/com/distribution/insurance/service/ReviewServiceContractTest.java
git commit -m "feat(epic3-A): 심사 승인 시 InsuranceContract 자동 생성 (ADR 0005)"
```

---

## Task 4: ContractService (목록·상세·PDF, 본인 계약 격리)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/ContractService.java`

서비스 동작은 Task 5의 컨트롤러 테스트(통합)에서 검증한다. 여기서는 서비스 클래스만 작성하고 컴파일을 확인한다.

- [ ] **Step 1: ContractService 작성**

`ContractService.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<InsuranceContract> myContracts(Long policyholderId) {
        return contractRepository.findByPolicyholderId(policyholderId);
    }

    @Transactional(readOnly = true)
    public InsuranceContract detail(Long policyholderId, Long contractId) {
        return requireOwned(policyholderId, contractId);
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long policyholderId, Long contractId) {
        return requireOwned(policyholderId, contractId).generatePdf();
    }

    /** 존재하지 않으면 404(IllegalArgumentException), 타인 계약이면 403(IllegalStateException). */
    private InsuranceContract requireOwned(Long policyholderId, Long contractId) {
        InsuranceContract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        if (!c.getPolicyholder().getId().equals(policyholderId)) {
            throw new IllegalStateException("본인의 계약만 조회할 수 있습니다.");
        }
        return c;
    }
}
```

확인: 404/403 매핑은 `GlobalExceptionHandler`의 기존 규칙(IllegalArgumentException→404, IllegalStateException→403)을 그대로 활용한다(ApplicationService.cancel과 동일 패턴).

- [ ] **Step 2: 컴파일 확인**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/ContractService.java
git commit -m "feat(epic3-A): ContractService 목록/상세/PDF + 본인 계약 격리"
```

---

## Task 5: DTO + ContractController + 통합 테스트

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ContractSummaryResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ContractDetailResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/ContractController.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/ContractControllerTest.java`

- [ ] **Step 1: 실패 테스트 작성**

`ContractControllerTest.java`:
```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ContractControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    Long ownerId; String ownerToken; String otherToken; Long contractId;

    @BeforeEach
    void setUp() {
        Policyholder owner = userRepository.save(new Policyholder(
                "owner@test.com", encoder.encode("pw"), "주인", "900101-1234567", "010-1111-1111"));
        ownerId = owner.getId();
        Policyholder other = userRepository.save(new Policyholder(
                "other@test.com", encoder.encode("pw"), "타인", "910101-1234567", "010-2222-2222"));
        ownerToken = tokenProvider.createToken(ownerId);
        otherToken = tokenProvider.createToken(other.getId());

        HealthInsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("실손의료", "설명", 30000));
        InsuranceContract c = contractRepository.save(
                new InsuranceContract(owner, product, 33000, LocalDate.of(2026, 6, 3)));
        contractId = c.getId();
    }

    @AfterEach
    void tearDown() {
        contractRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 내_계약_목록_조회() throws Exception {
        mockMvc.perform(get("/contracts").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monthlyPremium").value(33000))
                .andExpect(jsonPath("$[0].productName").value("실손의료"));
    }

    @Test
    void 계약_상세_조회() throws Exception {
        mockMvc.perform(get("/contracts/" + contractId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPremium").value(33000))
                .andExpect(jsonPath("$.paymentMethod").value("미등록"))
                .andExpect(jsonPath("$.coverageItems").isArray());
    }

    @Test
    void 타인_계약_상세는_403() throws Exception {
        mockMvc.perform(get("/contracts/" + contractId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 없는_계약은_404() throws Exception {
        mockMvc.perform(get("/contracts/999999").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void 계약서_PDF_다운로드() throws Exception {
        mockMvc.perform(get("/contracts/" + contractId + "/pdf").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }
}
```

확인: `tokenProvider.createToken(Long)`·`Policyholder`/`HealthInsuranceProduct` 생성자는 예시다. Step 시작 시 기존 `ApplicationControllerTest`/`ReviewControllerTest`의 토큰 발급·엔티티 생성 코드를 그대로 참고해 실제 시그니처에 맞춘다.

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.web.controller.ContractControllerTest"`
Expected: 컴파일 실패(DTO·컨트롤러 없음).

- [ ] **Step 3: ContractSummaryResponse 작성**

`ContractSummaryResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;

import java.time.LocalDate;

/** UC08 2단계: 계약번호, 상품명, 보험종류, 계약기간, 월보험료. */
public record ContractSummaryResponse(
        Long contractId, String productName, String productType,
        LocalDate startDate, LocalDate endDate, int monthlyPremium, String status) {

    public static ContractSummaryResponse from(InsuranceContract c) {
        return new ContractSummaryResponse(
                c.getId(), c.getProduct().getProductName(), typeOf(c.getProduct()),
                c.getStartDate(), c.getEndDate(), c.getMonthlyPremium(), c.getStatus().name());
    }

    static String typeOf(InsuranceProduct product) {
        if (product instanceof HealthInsuranceProduct) return "HEALTH";
        if (product instanceof CarInsuranceProduct) return "CAR";
        throw new RuntimeException("알 수 없는 상품 종류: " + product.getClass().getSimpleName());
    }
}
```

- [ ] **Step 4: ContractDetailResponse 작성**

`ContractDetailResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CoverageItem;

import java.time.LocalDate;
import java.util.List;

/**
 * UC08 4단계 상세. 도메인 실재분만 노출(spec/grill): 보장항목·보장금액·월보험료·기간·상태.
 * 결제수단은 이슈 A에서 "미등록" 상수. 수익자·특약은 제외.
 */
public record ContractDetailResponse(
        Long contractId, String productName, String productType,
        LocalDate startDate, LocalDate endDate, int monthlyPremium, String status,
        String paymentMethod, List<CoverageResponse> coverageItems) {

    public record CoverageResponse(String itemName, int coverageLimit, int deductible) {
        static CoverageResponse from(CoverageItem ci) {
            return new CoverageResponse(ci.getItemName(), ci.getCoverageLimit(), ci.getDeductible());
        }
    }

    public static ContractDetailResponse from(InsuranceContract c) {
        List<CoverageResponse> items = c.getProduct().getCoverageItems().stream()
                .map(CoverageResponse::from)
                .toList();
        return new ContractDetailResponse(
                c.getId(), c.getProduct().getProductName(),
                ContractSummaryResponse.typeOf(c.getProduct()),
                c.getStartDate(), c.getEndDate(), c.getMonthlyPremium(), c.getStatus().name(),
                "미등록", items);  // 이슈 B에서 실제 결제수단으로 대체
    }
}
```

- [ ] **Step 5: ContractController 작성**

`ContractController.java`:
```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.service.ContractService;
import com.distribution.insurance.web.dto.ContractDetailResponse;
import com.distribution.insurance.web.dto.ContractSummaryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public List<ContractSummaryResponse> myContracts(@AuthenticationPrincipal Long userId) {
        return contractService.myContracts(userId).stream()
                .map(ContractSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ContractDetailResponse detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        InsuranceContract c = contractService.detail(userId, id);
        return ContractDetailResponse.from(c);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        byte[] pdf = contractService.generatePdf(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract-" + id + ".txt\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(pdf);
    }
}
```

확인: `/contracts/{id}/pdf` 라우트가 `/contracts/unpaid`(이슈 B) 등 문자열 경로와 충돌하지 않도록, 이슈 B에서 고정 경로를 `{id}` 가변경로보다 먼저 선언한다. 이슈 A에는 가변경로만 있으므로 현재 충돌 없음.

- [ ] **Step 6: 보안 설정 확인 — `/contracts/**`가 인증 필요 + Policyholder 접근 가능**

`backend/src/main/java/.../config/SecurityConfig.java`(또는 보안 설정 파일)를 열어 기존 `/applications/**` 규칙을 찾는다. `/applications/**`가 authenticated()로 열려 있고 별도 role 제한이 없다면 `/contracts/**`도 같은 방식으로 추가한다. role 기반 제한이 있으면 `/applications`와 동일 정책을 `/contracts/**`에 적용한다. 변경 후 사유를 커밋 메시지에 적는다.

- [ ] **Step 7: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.web.controller.ContractControllerTest"`
Expected: PASS (5 tests).

- [ ] **Step 8: 전체 테스트 회귀 확인**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL (전부 통과).

- [ ] **Step 9: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/web backend/src/test/java/com/distribution/insurance/web/controller/ContractControllerTest.java backend/src/main/java/com/distribution/insurance/config
git commit -m "feat(epic3-A): 계약 목록/상세/PDF API + 본인 계약 격리 (UC08)"
```

---

## Self-Review (작성자 체크 결과)

- **Spec 커버리지**: UC08 목록(Task5)·상세(Task5)·PDF(Task1 generatePdf+Task5 endpoint)·계약 자동생성(Task3, ADR0005)·adjustedPremium 매핑(Task3, ADR0003)·본인 격리(Task4). 결제수단="미등록"·수익자/특약 제외는 spec 결정대로 반영.
- **이슈 A 범위 외**(미납/납부/자동이체/고지서)는 의도적으로 제외 — 이슈 B/C plan에서 다룸.
- **타입 일관성**: `ContractStatus`, `InsuranceContract(Policyholder, InsuranceProduct, int, LocalDate)`, `findByPolicyholderId`, `ContractSummaryResponse.typeOf`(DetailResponse가 재사용) 전 태스크 일치.
- **주의(실행자에게)**: 도메인 생성자 시그니처(Policyholder/Product/Employee/MedicalHistory)와 JWT 토큰 발급 API는 plan의 예시이므로, 각 테스트 작성 step에서 기존 코드(특히 `ApplicationControllerTest`, `ReviewService`)를 먼저 열어 실제 시그니처에 맞춘 뒤 진행할 것.
