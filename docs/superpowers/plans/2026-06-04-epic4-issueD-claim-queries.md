# Epic 4 이슈 D — 보상 현황(UC03) + 이력(UC04) + 실익분석(UC11) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`).

**Goal:** 가입자가 진행 중인 보상 처리 현황, 기간별 보상 이력, 계약별 보험 처리 실익(총납입·총수령·실익률)을 조회한다. (모두 읽기 전용)

**Architecture:** `ClaimQueryService`가 가입자의 모든 계약에 걸친 `Claim`(HealthInsuranceClaim/CarAccidentReport)을 조회·집계한다. 총납입 보험료는 성공한 `Payment` 합, 총수령 보험금은 성공한 `BenefitPayment` 합으로 계산한다. 실익분석은 본인 실익만(유사가입자 비교 제외 — grill 결정), 가입 6개월 미만은 안내.

**Tech Stack:** Spring Boot 4, Java 21, JPA, Spring MVC, JUnit5 + AssertJ + MockMvc.

**입력 문서:** spec `docs/superpowers/specs/2026-06-04-epic4-claim-payout-design.md`; 용어 `CONTEXT.md`; 결정 ADR 0004(온더플라이 납입), 0007.
**선행:** 이슈 A·B·C. **브랜치 `epic4-D-claim-queries` (base: epic4-C)** 스택.
**규약:** `/claims/**` = ROLE_POLICYHOLDER, `@AuthenticationPrincipal Long userId`, 예외→HTTP 동일. 테스트 `@SpringBootTest @Transactional`/MockMvc + 한글명.
**경로 결정:** 실익분석은 spec의 `/contracts/{id}/benefit-analysis` 대신 컨트롤러 응집을 위해 **`GET /claims/benefit-analysis?contractId=&from=&to=`** 로 둔다(둘 다 POLICYHOLDER). 핸드오프에 명시.
**상태 분류:** 진행중(UC03) = `PENDING, IN_REVIEW, APPROVED, FAILED`. 종결 = `COMPLETED, REJECTED`(이력 UC04에서 노출).

---

## File Structure
- Modify: `repository/ClaimRepository.java` (가입자별/상태별 조회)
- Modify: `repository/PaymentRepository.java` (성공 납부 합계)
- Modify: `repository/BenefitPaymentRepository.java` (성공 지급 합계: 계약별·청구별)
- Create: `service/ClaimQueryService.java`
- Create: DTOs (`ClaimStatusResponse`, `ClaimProgressResponse`, `ClaimHistoryResponse`, `ClaimHistoryDetailResponse`, `BenefitAnalysisResponse`)
- Create: `web/controller/ClaimQueryController.java`
- Tests: service + controller

---

## Task 1: Repository 조회·집계 쿼리

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/repository/ClaimRepository.java`
- Modify: `backend/src/main/java/com/distribution/insurance/repository/PaymentRepository.java`
- Modify: `backend/src/main/java/com/distribution/insurance/repository/BenefitPaymentRepository.java`
- Test: `backend/src/test/java/com/distribution/insurance/repository/ClaimQueryRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.*;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ClaimQueryRepositoryTest {

    @Autowired ClaimRepository claimRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired BenefitPaymentRepository benefitPaymentRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    @Test
    void 가입자별_청구와_납입_지급_합계를_조회한다() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));

        HealthInsuranceClaim claim = claimRepository.save(new HealthInsuranceClaim(
                contract, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        paymentRepository.save(Payment.success(contract, 30000, PaymentMethod.CARD));
        paymentRepository.save(Payment.success(contract, 30000, PaymentMethod.CARD));
        benefitPaymentRepository.save(BenefitPayment.success(claim, 400000, "110-123-456789"));

        assertThat(claimRepository.findByContractPolicyholderId(ph.getId())).hasSize(1);
        assertThat(paymentRepository.sumAmountByContractIdAndStatus(contract.getId(), PaymentStatus.SUCCESS))
                .isEqualTo(60000L);
        assertThat(benefitPaymentRepository.sumPaidByContractIdAndStatus(contract.getId(), PaymentStatus.SUCCESS))
                .isEqualTo(400000L);
        assertThat(benefitPaymentRepository.sumPaidByClaimIdAndStatus(claim.getId(), PaymentStatus.SUCCESS))
                .isEqualTo(400000L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimQueryRepositoryTest'`
Expected: FAIL — 쿼리 메서드 없음.

- [ ] **Step 3: Write minimal implementation**

`ClaimRepository`에 추가:
```java
    List<Claim> findByContractPolicyholderId(Long policyholderId);
```
(import `java.util.List` 이미 있을 수 있음 — 없으면 추가.)

`PaymentRepository`에 추가:
```java
    @org.springframework.data.jpa.repository.Query(
        "select coalesce(sum(p.amount), 0) from Payment p where p.contract.id = :contractId and p.status = :status")
    long sumAmountByContractIdAndStatus(Long contractId, PaymentStatus status);
```

`BenefitPaymentRepository`에 추가:
```java
    @org.springframework.data.jpa.repository.Query(
        "select coalesce(sum(b.paidAmount), 0) from BenefitPayment b where b.claim.contract.id = :contractId and b.status = :status")
    long sumPaidByContractIdAndStatus(Long contractId, com.distribution.insurance.domain.contract.PaymentStatus status);

    @org.springframework.data.jpa.repository.Query(
        "select coalesce(sum(b.paidAmount), 0) from BenefitPayment b where b.claim.id = :claimId and b.status = :status")
    long sumPaidByClaimIdAndStatus(Long claimId, com.distribution.insurance.domain.contract.PaymentStatus status);
```

> 참고: `BenefitPayment`는 `claim`(ManyToOne)→`Claim.contract`(ManyToOne) 경로로 계약을 잇는다. JPQL 경로 `b.claim.contract.id` 사용.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimQueryRepositoryTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/repository/ClaimRepository.java \
        backend/src/main/java/com/distribution/insurance/repository/PaymentRepository.java \
        backend/src/main/java/com/distribution/insurance/repository/BenefitPaymentRepository.java \
        backend/src/test/java/com/distribution/insurance/repository/ClaimQueryRepositoryTest.java
git commit -m "Epic4-D: 청구 가입자별 조회 + 납입/지급 합계 쿼리"
```

---

## Task 2: ClaimQueryService (현황/이력/실익분석)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/ClaimQueryService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/ClaimQueryServiceTest.java`

책임 및 반환 record(서비스 내부 정의):
```java
public record ClaimSummary(Long claimId, String claimType, LocalDate claimDate, int requestAmount,
                           long paidAmount, String status) {}
public record BenefitAnalysis(long totalPaidPremium, long totalReceivedBenefit,
                              long profit, double profitRate) {}
```
- `inProgressClaims(policyholderId)`: 가입자 청구 중 상태 ∈ {PENDING, IN_REVIEW, APPROVED, FAILED} → `ClaimSummary` 목록(paidAmount=0 가능). `claimType` = `HealthInsuranceClaim`이면 "HEALTH", `CarAccidentReport`이면 "CAR"(`Hibernate.unproxy` 후 instanceof).
- `history(policyholderId, from, to)`: 가입자 청구 중 `claimDate` ∈ [from, to] → `ClaimSummary`(paidAmount = 성공 BenefitPayment 합). from/to null이면 기본 최근 1년(`to=오늘`, `from=오늘-1년`).
- `benefitAnalysis(policyholderId, contractId)`: 계약 소유 검증(타인 403). 가입 6개월 미만(`startDate > 오늘-6개월`)이면 `InvalidRequestException("분석에 필요한 데이터가 충분하지 않습니다. 6개월 이후 이용 가능합니다.")`. 아니면 totalPaidPremium=성공 Payment 합, totalReceivedBenefit=성공 BenefitPayment 합, profit=received-paid, profitRate=paid>0 ? (double)received/paid : 0.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.*;
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
class ClaimQueryServiceTest {

    @Autowired ClaimQueryService queryService;
    @Autowired ClaimRepository claimRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired BenefitPaymentRepository benefitPaymentRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Policyholder ph() {
        return userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
    }

    private InsuranceContract contract(Policyholder ph, LocalDate start) {
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        return contractRepository.save(new InsuranceContract(ph, product, 30000, start));
    }

    @Test
    void 진행중_청구만_현황에_나온다() {
        Policyholder p = ph();
        InsuranceContract c = contract(p, LocalDate.now());
        HealthInsuranceClaim pending = claimRepository.save(new HealthInsuranceClaim(
                c, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        HealthInsuranceClaim done = new HealthInsuranceClaim(
                c, 300000, "병원", "S00", LocalDate.now(), 300000, ClaimComplexity.SIMPLE);
        done.markCompleted();
        claimRepository.save(done);

        List<ClaimQueryService.ClaimSummary> inProgress = queryService.inProgressClaims(p.getId());
        assertThat(inProgress).extracting(ClaimQueryService.ClaimSummary::claimId)
                .containsExactly(pending.getId());
    }

    @Test
    void 이력은_기간내_청구를_지급액과_함께_반환한다() {
        Policyholder p = ph();
        InsuranceContract c = contract(p, LocalDate.now());
        HealthInsuranceClaim claim = claimRepository.save(new HealthInsuranceClaim(
                c, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        benefitPaymentRepository.save(BenefitPayment.success(claim, 400000, "110-123-456789"));

        List<ClaimQueryService.ClaimSummary> history = queryService.history(p.getId(),
                LocalDate.now().minusMonths(1), LocalDate.now());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).paidAmount()).isEqualTo(400000L);
    }

    @Test
    void 실익분석은_총납입_총수령_실익을_계산한다() {
        Policyholder p = ph();
        InsuranceContract c = contract(p, LocalDate.now().minusMonths(8));
        HealthInsuranceClaim claim = claimRepository.save(new HealthInsuranceClaim(
                c, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        paymentRepository.save(Payment.success(c, 30000, PaymentMethod.CARD));
        paymentRepository.save(Payment.success(c, 30000, PaymentMethod.CARD));
        benefitPaymentRepository.save(BenefitPayment.success(claim, 400000, "110-123-456789"));

        ClaimQueryService.BenefitAnalysis a = queryService.benefitAnalysis(p.getId(), c.getId());
        assertThat(a.totalPaidPremium()).isEqualTo(60000L);
        assertThat(a.totalReceivedBenefit()).isEqualTo(400000L);
        assertThat(a.profit()).isEqualTo(340000L);
    }

    @Test
    void 가입_6개월_미만_실익분석은_400성_예외() {
        Policyholder p = ph();
        InsuranceContract c = contract(p, LocalDate.now().minusMonths(2));

        assertThatThrownBy(() -> queryService.benefitAnalysis(p.getId(), c.getId()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 타인_계약_실익분석은_403성_예외() {
        Policyholder owner = ph();
        Policyholder other = ph();
        InsuranceContract c = contract(owner, LocalDate.now().minusMonths(8));

        assertThatThrownBy(() -> queryService.benefitAnalysis(other.getId(), c.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimQueryServiceTest'`
Expected: FAIL — service 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.Claim;
import com.distribution.insurance.domain.claim.ClaimStatus;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.contract.PaymentStatus;
import com.distribution.insurance.repository.BenefitPaymentRepository;
import com.distribution.insurance.repository.ClaimRepository;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.PaymentRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 보상 현황(UC03)·이력(UC04)·실익분석(UC11) 조회. 읽기 전용. */
@Service
public class ClaimQueryService {

    private static final Set<ClaimStatus> IN_PROGRESS =
            EnumSet.of(ClaimStatus.PENDING, ClaimStatus.IN_REVIEW, ClaimStatus.APPROVED, ClaimStatus.FAILED);

    private final ClaimRepository claimRepository;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final BenefitPaymentRepository benefitPaymentRepository;

    public ClaimQueryService(ClaimRepository claimRepository, ContractRepository contractRepository,
                             PaymentRepository paymentRepository, BenefitPaymentRepository benefitPaymentRepository) {
        this.claimRepository = claimRepository;
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.benefitPaymentRepository = benefitPaymentRepository;
    }

    public record ClaimSummary(Long claimId, String claimType, LocalDate claimDate, int requestAmount,
                               long paidAmount, String status) {}
    public record BenefitAnalysis(long totalPaidPremium, long totalReceivedBenefit,
                                  long profit, double profitRate) {}

    @Transactional(readOnly = true)
    public List<ClaimSummary> inProgressClaims(Long policyholderId) {
        return claimRepository.findByContractPolicyholderId(policyholderId).stream()
                .filter(c -> IN_PROGRESS.contains(c.getStatus()))
                .map(c -> toSummary(c, 0L))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimSummary> history(Long policyholderId, LocalDate from, LocalDate to) {
        LocalDate end = (to == null) ? LocalDate.now() : to;
        LocalDate start = (from == null) ? end.minusYears(1) : from;
        return claimRepository.findByContractPolicyholderId(policyholderId).stream()
                .filter(c -> !c.getClaimDate().isBefore(start) && !c.getClaimDate().isAfter(end))
                .map(c -> toSummary(c, benefitPaymentRepository.sumPaidByClaimIdAndStatus(c.getId(), PaymentStatus.SUCCESS)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BenefitAnalysis benefitAnalysis(Long policyholderId, Long contractId) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        if (!contract.getPolicyholder().getId().equals(policyholderId)) {
            throw new IllegalStateException("본인 계약만 분석할 수 있습니다.");
        }
        if (contract.getStartDate().isAfter(LocalDate.now().minusMonths(6))) {
            throw new InvalidRequestException("분석에 필요한 데이터가 충분하지 않습니다. 6개월 이후 이용 가능합니다.");
        }
        long paid = paymentRepository.sumAmountByContractIdAndStatus(contractId, PaymentStatus.SUCCESS);
        long received = benefitPaymentRepository.sumPaidByContractIdAndStatus(contractId, PaymentStatus.SUCCESS);
        double rate = paid > 0 ? (double) received / paid : 0.0;
        return new BenefitAnalysis(paid, received, received - paid, rate);
    }

    private ClaimSummary toSummary(Claim claim, long paidAmount) {
        String type = Hibernate.unproxy(claim) instanceof CarAccidentReport ? "CAR" : "HEALTH";
        return new ClaimSummary(claim.getId(), type, claim.getClaimDate(),
                claim.getRequestAmount(), paidAmount, claim.getStatus().name());
    }
}
```

> 참고: `toSummary`의 `Hibernate.unproxy(claim)`는 JOINED 상속에서 구체 타입(Health/Car) 판별을 보장한다(이슈 A/C의 product 판별과 동일 패턴).

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimQueryServiceTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/ClaimQueryService.java \
        backend/src/test/java/com/distribution/insurance/service/ClaimQueryServiceTest.java
git commit -m "Epic4-D: ClaimQueryService(현황/이력/실익분석)"
```

---

## Task 3: DTO + ClaimQueryController

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ClaimSummaryResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/BenefitAnalysisResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/ClaimQueryController.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/ClaimQueryControllerTest.java`

엔드포인트(ROLE_POLICYHOLDER):
- `GET /claims/status` — 진행중 현황 목록
- `GET /claims/history?from=&to=` — 이력(기본 최근 1년)
- `GET /claims/benefit-analysis?contractId=` — 실익분석

> 상세(`/status/{id}`, `/history/{id}`)는 본 plan에서 단일 요약으로 충분하므로 목록 응답에 핵심 필드를 포함하고, 단계별 타임라인 상세는 YAGNI로 생략(현황 목록의 status로 진행단계 표현). 핸드오프에 명시.

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.*;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimQueryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClaimRepository claimRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void 진행중_현황을_조회한다() throws Exception {
        Policyholder ph = userRepository.save(new Policyholder("홍", "q@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract c = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        claimRepository.save(new HealthInsuranceClaim(c, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        String token = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        mockMvc.perform(get("/claims/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].claimType").value("HEALTH"));
    }

    @Test
    void 실익분석을_조회한다() throws Exception {
        Policyholder ph = userRepository.save(new Policyholder("홍", "q2@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract c = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now().minusMonths(8)));
        String token = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        mockMvc.perform(get("/claims/benefit-analysis").param("contractId", String.valueOf(c.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaidPremium").exists())
                .andExpect(jsonPath("$.profit").exists());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*ClaimQueryControllerTest'`
Expected: FAIL — 엔드포인트 없음.

- [ ] **Step 3: Write minimal implementation (DTOs)**

```java
// ClaimSummaryResponse.java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.service.ClaimQueryService;

import java.time.LocalDate;

public record ClaimSummaryResponse(Long claimId, String claimType, LocalDate claimDate,
                                   int requestAmount, long paidAmount, String status) {
    public static ClaimSummaryResponse from(ClaimQueryService.ClaimSummary s) {
        return new ClaimSummaryResponse(s.claimId(), s.claimType(), s.claimDate(),
                s.requestAmount(), s.paidAmount(), s.status());
    }
}
```

```java
// BenefitAnalysisResponse.java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.service.ClaimQueryService;

public record BenefitAnalysisResponse(long totalPaidPremium, long totalReceivedBenefit,
                                      long profit, double profitRate) {
    public static BenefitAnalysisResponse from(ClaimQueryService.BenefitAnalysis a) {
        return new BenefitAnalysisResponse(a.totalPaidPremium(), a.totalReceivedBenefit(), a.profit(), a.profitRate());
    }
}
```

- [ ] **Step 4: Write minimal implementation (Controller)**

```java
// ClaimQueryController.java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.service.ClaimQueryService;
import com.distribution.insurance.web.dto.BenefitAnalysisResponse;
import com.distribution.insurance.web.dto.ClaimSummaryResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/claims")
public class ClaimQueryController {

    private final ClaimQueryService queryService;

    public ClaimQueryController(ClaimQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/status")
    public List<ClaimSummaryResponse> status(@AuthenticationPrincipal Long userId) {
        return queryService.inProgressClaims(userId).stream().map(ClaimSummaryResponse::from).toList();
    }

    @GetMapping("/history")
    public List<ClaimSummaryResponse> history(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return queryService.history(userId, from, to).stream().map(ClaimSummaryResponse::from).toList();
    }

    @GetMapping("/benefit-analysis")
    public BenefitAnalysisResponse analysis(@AuthenticationPrincipal Long userId,
                                            @RequestParam Long contractId) {
        return BenefitAnalysisResponse.from(queryService.benefitAnalysis(userId, contractId));
    }
}
```

> 주의: `ClaimController`(이슈 A/C)와 `ClaimQueryController`가 모두 `/claims` 경로를 쓰지만 서로 다른 하위 경로(`/health`,`/car-accidents` vs `/status`,`/history`,`/benefit-analysis`)라 충돌 없음. `/claims/{id}` 같은 모호 경로를 만들지 않았다.

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*ClaimQueryControllerTest'`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/dto/ClaimSummaryResponse.java \
        backend/src/main/java/com/distribution/insurance/web/dto/BenefitAnalysisResponse.java \
        backend/src/main/java/com/distribution/insurance/web/controller/ClaimQueryController.java \
        backend/src/test/java/com/distribution/insurance/web/controller/ClaimQueryControllerTest.java
git commit -m "Epic4-D: ClaimQueryController(/claims/status,history,benefit-analysis)"
```

---

## Task 4: 전체 회귀

- [ ] **Step 1:** Run: `cd backend && ./gradlew test` → BUILD SUCCESSFUL.
- [ ] **Step 2:** `git -C /Users/heeyoon/Desktop/insurance status --short` → 잔여 파일 없음.

---

## Self-Review (작성자 점검)

**Spec 커버리지(이슈 D):** UC03 진행중 현황 목록 → Task 2·3 ✓ (단계별 타임라인 상세는 YAGNI 생략, status로 표현) / UC04 이력 기간조회+지급액 → Task 2·3 ✓ / UC11 실익(총납입·총수령·실익·실익률) → Task 2·3 ✓ / 6개월 미만 안내(A1) → Task 2 ✓ / 유사가입자 비교 제외(grill 결정) ✓.
**미커버(의도적):** 현황/이력 단건 상세 타임라인(YAGNI), 유사가입자 비교(범위 제외).
**타입 일관성:** `ClaimSummary`, `BenefitAnalysis` record, `sumAmountByContractIdAndStatus`/`sumPaidBy...`, `findByContractPolicyholderId` — Task 간 동일.
**확인 필요:** `Payment.success(contract, amount, method)` 팩토리·`PaymentMethod.CARD`(기존 확인됨), `BenefitPayment.success(claim, amount, account)`(이슈 A), `InsuranceContract.getStartDate()`.

---

## Execution Handoff
브랜치 `epic4-D-claim-queries`(base: epic4-C). Subagent-Driven 실행.
