# Epic 4 이슈 C — 자동차사고 접수(UC09) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** 가입자가 본인의 유효한 자동차보험 계약에 사고를 접수하면, 사고 정보·증빙을 저장하고 접수번호를 발급하며 보험사 직원과 가입자에게 알림을 보낸다. (심사·지급 흐름은 범위 밖)

**Architecture:** 다이어그램(04_claim)대로 `CarAccidentReport extends Claim`을 추가한다. 청구 증빙은 이슈 A의 `AttachmentValidator`/`FileStorage`를 재사용한다. 계약은 `CarInsuranceProduct`여야 하며 ACTIVE여야 한다. 접수번호는 `Claim.id`를 사용한다. 직원 알림은 `UserRepository.findAllEmployees()` 전원에게 보낸다.

**Tech Stack:** Spring Boot 4, Java 21, JPA, Spring MVC(멀티파트), JUnit5 + AssertJ + MockMvc.

**입력 문서:** spec `docs/superpowers/specs/2026-06-04-epic4-claim-payout-design.md`; 용어 `CONTEXT.md`(CarAccidentReport, Claim, ClaimAttachment); 결정 ADR 0007.
**선행:** 이슈 A·B. **브랜치 `epic4-C-car-accident-report` (base: epic4-B)** 스택.
**규약:** `/claims/**` = ROLE_POLICYHOLDER(이미 설정됨), `@AuthenticationPrincipal Long userId`, 예외→HTTP 동일, `NotificationSender.send`, 테스트 `@SpringBootTest @Transactional`/MockMvc + 한글명.
**범위 경계:** 자동차 청구 심사/지급 없음. requestAmount 개념이 사고접수엔 약하므로 `Claim.requestAmount`는 0으로 둔다(접수 단계라 청구금액 미정).

---

## File Structure
- Create: `domain/claim/CarAccidentReport.java`
- Create: `repository/CarAccidentReportRepository.java`
- Create: `service/CarAccidentService.java`
- Create: `web/dto/CarAccidentResultResponse.java`
- Modify: `web/controller/ClaimController.java` (자동차사고 엔드포인트 추가)
- Tests: 각 단위 + 컨트롤러

---

## Task 1: CarAccidentReport 엔티티

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/claim/CarAccidentReport.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/claim/CarAccidentReportTest.java`

`Claim` 상속(JOINED). 필드: `accidentDate`(LocalDate), `accidentLocation`, `accidentType`, `vehicleNumber`, `hasInjury`(boolean), `injuredCount`(int). 첨부 `@ElementCollection List<ClaimAttachment>` + `addAttachment`. 생성자는 `requestAmount=0`으로 부모 호출(접수단계). `markFailed/markCompleted` 등은 사용 안 함(접수만, PENDING 유지).

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CarAccidentReportTest {

    private InsuranceContract carContract() {
        Policyholder ph = new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789");
        var product = new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL");
        return new InsuranceContract(ph, product, 50000, LocalDate.now());
    }

    @Test
    void 생성하면_PENDING이고_사고정보가_보존된다() {
        CarAccidentReport report = new CarAccidentReport(
                carContract(), LocalDate.now(), "서울 강남", "쌍방", "12가3456", true, 2);

        assertThat(report.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(report.getAccidentLocation()).isEqualTo("서울 강남");
        assertThat(report.getAccidentType()).isEqualTo("쌍방");
        assertThat(report.getVehicleNumber()).isEqualTo("12가3456");
        assertThat(report.isHasInjury()).isTrue();
        assertThat(report.getInjuredCount()).isEqualTo(2);
        assertThat(report.getRequestAmount()).isEqualTo(0);
    }

    @Test
    void 첨부를_추가하면_컬렉션에_쌓인다() {
        CarAccidentReport report = new CarAccidentReport(
                carContract(), LocalDate.now(), "서울", "단독", "12가3456", false, 0);
        report.addAttachment(new ClaimAttachment("p.jpg", "image/jpeg", 10L, "/p/p.jpg"));
        assertThat(report.getAttachments()).hasSize(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*CarAccidentReportTest'`
Expected: FAIL — `CarAccidentReport` 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 자동차사고 접수(UC09). 접수 단계라 청구금액 미정(requestAmount=0). 심사/지급 흐름 없음. */
@Entity
@Table(name = "car_accident_report")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CarAccidentReport extends Claim {

    private LocalDate accidentDate;
    private String accidentLocation;
    private String accidentType;
    private String vehicleNumber;
    private boolean hasInjury;
    private int injuredCount;

    @ElementCollection
    @CollectionTable(name = "car_accident_attachment", joinColumns = @JoinColumn(name = "claim_id"))
    private List<ClaimAttachment> attachments = new ArrayList<>();

    public CarAccidentReport(InsuranceContract contract, LocalDate accidentDate, String accidentLocation,
                             String accidentType, String vehicleNumber, boolean hasInjury, int injuredCount) {
        super(contract, 0);
        this.accidentDate = accidentDate;
        this.accidentLocation = accidentLocation;
        this.accidentType = accidentType;
        this.vehicleNumber = vehicleNumber;
        this.hasInjury = hasInjury;
        this.injuredCount = injuredCount;
    }

    public void addAttachment(ClaimAttachment attachment) {
        this.attachments.add(attachment);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*CarAccidentReportTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/claim/CarAccidentReport.java \
        backend/src/test/java/com/distribution/insurance/domain/claim/CarAccidentReportTest.java
git commit -m "Epic4-C: CarAccidentReport 엔티티(UC09 접수)"
```

---

## Task 2: CarAccidentReportRepository

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/repository/CarAccidentReportRepository.java`
- Test: `backend/src/test/java/com/distribution/insurance/repository/CarAccidentReportRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CarAccidentReportRepositoryTest {

    @Autowired CarAccidentReportRepository reportRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    @Test
    void 자동차사고_접수를_저장하고_조회한다() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 50000, LocalDate.now()));

        CarAccidentReport saved = reportRepository.save(new CarAccidentReport(
                contract, LocalDate.now(), "서울", "단독", "12가3456", false, 0));

        assertThat(saved.getId()).isNotNull();
        assertThat(reportRepository.findById(saved.getId())).isPresent();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*CarAccidentReportRepositoryTest'`
Expected: FAIL — repository 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarAccidentReportRepository extends JpaRepository<CarAccidentReport, Long> {
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*CarAccidentReportRepositoryTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/repository/CarAccidentReportRepository.java \
        backend/src/test/java/com/distribution/insurance/repository/CarAccidentReportRepositoryTest.java
git commit -m "Epic4-C: CarAccidentReportRepository"
```

---

## Task 3: CarAccidentService (UC09 접수)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/CarAccidentService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/CarAccidentServiceTest.java`

책임:
1. 계약 조회(없으면 IllegalArgumentException→404), 본인 소유 검증(아니면 IllegalStateException→403).
2. 상품이 `CarInsuranceProduct`인지(`Hibernate.unproxy` 후 instanceof; 아니면 InvalidRequestException→400 "자동차보험 계약이 아닙니다.").
3. 계약 ACTIVE 검증(아니면 InvalidRequestException "유효한 계약이 아닙니다.").
4. 접수 저장(첨부 메타 부착) → 접수번호 = id.
5. 직원 전원 알림(`findAllEmployees`) + 가입자 접수완료 안내.

시그니처(컨트롤러가 멀티파트→메타 변환 후 호출):
`CarAccidentReport report(Long policyholderId, Long contractId, LocalDate accidentDate, String accidentLocation, String accidentType, String vehicleNumber, boolean hasInjury, int injuredCount, List<ClaimAttachment> attachments)`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.ClaimStatus;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
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
class CarAccidentServiceTest {

    @Autowired CarAccidentService carAccidentService;
    @Autowired CarAccidentReportRepository reportRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Policyholder ph(String acct) {
        return userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", acct));
    }

    private InsuranceContract carContract(Policyholder ph) {
        var product = productRepository.save(new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
        return contractRepository.save(new InsuranceContract(ph, product, 50000, LocalDate.now()));
    }

    @Test
    void 접수하면_PENDING으로_저장되고_접수번호가_발급된다() {
        userRepository.save(new InsuranceEmployee("직원", "e@t.com", "010", "pw", "사고팀", 0));
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = carContract(p);

        CarAccidentReport report = carAccidentService.report(
                p.getId(), c.getId(), LocalDate.now(), "서울", "쌍방", "12가3456", true, 2, List.of());

        assertThat(report.getId()).isNotNull();
        assertThat(reportRepository.findById(report.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.PENDING);
    }

    @Test
    void 본인계약이_아니면_403성_예외() {
        Policyholder owner = ph("110-123-456789");
        Policyholder other = ph("220-123-456789");
        InsuranceContract c = carContract(owner);

        assertThatThrownBy(() -> carAccidentService.report(
                other.getId(), c.getId(), LocalDate.now(), "서울", "단독", "12가3456", false, 0, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 의료보험_계약에_사고접수하면_400성_예외() {
        Policyholder p = ph("110-123-456789");
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract health = contractRepository.save(new InsuranceContract(p, product, 30000, LocalDate.now()));

        assertThatThrownBy(() -> carAccidentService.report(
                p.getId(), health.getId(), LocalDate.now(), "서울", "단독", "12가3456", false, 0, List.of()))
                .isInstanceOf(InvalidRequestException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*CarAccidentServiceTest'`
Expected: FAIL — service 없음.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.ClaimAttachment;
import com.distribution.insurance.domain.contract.ContractStatus;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.CarAccidentReportRepository;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.UserRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 자동차사고 접수(UC09). 접수번호 발급 + 직원·가입자 알림. 심사/지급 없음. */
@Service
public class CarAccidentService {

    private final CarAccidentReportRepository reportRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public CarAccidentService(CarAccidentReportRepository reportRepository,
                              ContractRepository contractRepository,
                              UserRepository userRepository,
                              NotificationSender notificationSender) {
        this.reportRepository = reportRepository;
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public CarAccidentReport report(Long policyholderId, Long contractId, LocalDate accidentDate,
                                    String accidentLocation, String accidentType, String vehicleNumber,
                                    boolean hasInjury, int injuredCount, List<ClaimAttachment> attachments) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        if (!contract.getPolicyholder().getId().equals(policyholderId)) {
            throw new IllegalStateException("본인 계약에만 사고를 접수할 수 있습니다.");
        }
        if (!(Hibernate.unproxy(contract.getProduct()) instanceof CarInsuranceProduct)) {
            throw new InvalidRequestException("자동차보험 계약이 아닙니다.");
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new InvalidRequestException("유효한 계약이 아닙니다.");
        }

        CarAccidentReport report = new CarAccidentReport(
                contract, accidentDate, accidentLocation, accidentType, vehicleNumber, hasInjury, injuredCount);
        attachments.forEach(report::addAttachment);
        reportRepository.save(report);

        // 직원 전원에게 접수 알림(UC09 step6)
        for (InsuranceEmployee staff : userRepository.findAllEmployees()) {
            notificationSender.send(staff.getEmail(), staff.getPhone(),
                    "자동차사고 접수 알림. 접수번호 " + report.getId());
        }
        // 가입자 접수완료 안내(UC09 step7)
        Policyholder ph = contract.getPolicyholder();
        notificationSender.send(ph.getEmail(), ph.getPhone(),
                "사고 접수가 완료되었습니다. 접수번호 " + report.getId() + ". 담당자가 곧 연락드립니다.");
        return report;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*CarAccidentServiceTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/CarAccidentService.java \
        backend/src/test/java/com/distribution/insurance/service/CarAccidentServiceTest.java
git commit -m "Epic4-C: CarAccidentService(UC09 접수+접수번호+알림)"
```

---

## Task 4: ClaimController 자동차사고 엔드포인트 + DTO

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/CarAccidentResultResponse.java`
- Modify: `backend/src/main/java/com/distribution/insurance/web/controller/ClaimController.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/CarAccidentControllerTest.java`

`POST /claims/car-accidents` (multipart). 첨부는 이슈 A처럼 `AttachmentValidator.validate` + `FileStorage.store(userId, f)` 후 메타 리스트로 서비스 호출. 검증 실패 시 이슈 A와 동일하게 저장 파일 정리(try/catch + fileStorage.delete).

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CarAccidentControllerTest {

    @TempDir static Path uploadDir;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("insurance.upload.dir", () -> uploadDir.toString());
    }

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void 자동차사고_접수_멀티파트가_201을_반환한다() throws Exception {
        userRepository.save(new InsuranceEmployee("직원", "e@t.com", "010", "pw", "사고팀", 0));
        Policyholder ph = userRepository.save(new Policyholder("홍", "car@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
        InsuranceContract c = contractRepository.save(new InsuranceContract(ph, product, 50000, LocalDate.now()));
        String token = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        MockMultipartFile photo = new MockMultipartFile("attachments", "scene.jpg", "image/jpeg", new byte[]{1, 2});

        mockMvc.perform(multipart("/claims/car-accidents")
                        .file(photo)
                        .param("contractId", String.valueOf(c.getId()))
                        .param("accidentDate", LocalDate.now().toString())
                        .param("accidentLocation", "서울 강남")
                        .param("accidentType", "쌍방")
                        .param("vehicleNumber", "12가3456")
                        .param("hasInjury", "true")
                        .param("injuredCount", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*CarAccidentControllerTest'`
Expected: FAIL — 엔드포인트 없음(404).

- [ ] **Step 3: Write minimal implementation (DTO)**

```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.claim.CarAccidentReport;

public record CarAccidentResultResponse(Long reportId, String status) {
    public static CarAccidentResultResponse from(CarAccidentReport report) {
        return new CarAccidentResultResponse(report.getId(), report.getStatus().name());
    }
}
```

- [ ] **Step 4: Write minimal implementation (Controller method)** — `ClaimController`에 의존성 `CarAccidentService carAccidentService`를 생성자에 추가하고 메서드 추가:

```java
    @PostMapping(path = "/car-accidents", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public CarAccidentResultResponse reportCarAccident(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long contractId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate accidentDate,
            @RequestParam String accidentLocation,
            @RequestParam String accidentType,
            @RequestParam String vehicleNumber,
            @RequestParam boolean hasInjury,
            @RequestParam int injuredCount,
            @RequestParam(required = false) MultipartFile[] attachments) {

        List<ClaimAttachment> metas = new ArrayList<>();
        if (attachments != null) {
            for (MultipartFile f : attachments) {
                if (f.isEmpty()) continue;
                attachmentValidator.validate(f);
                metas.add(fileStorage.store(userId, f));
            }
        }
        try {
            CarAccidentReport report = carAccidentService.report(
                    userId, contractId, accidentDate, accidentLocation, accidentType,
                    vehicleNumber, hasInjury, injuredCount, metas);
            return CarAccidentResultResponse.from(report);
        } catch (RuntimeException e) {
            metas.forEach(m -> fileStorage.delete(m.getStoredPath()));
            throw e;
        }
    }
```

> import 추가: `CarAccidentReport`, `CarAccidentResultResponse`, `CarAccidentService`. 생성자에 `carAccidentService` 주입(기존 `claimService, attachmentValidator, fileStorage`에 추가). 의료 청구 엔드포인트의 고아파일 정리 로직과 동일 패턴(이슈 A 리뷰 반영).

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*CarAccidentControllerTest'`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/dto/CarAccidentResultResponse.java \
        backend/src/main/java/com/distribution/insurance/web/controller/ClaimController.java \
        backend/src/test/java/com/distribution/insurance/web/controller/CarAccidentControllerTest.java
git commit -m "Epic4-C: 자동차사고 접수 엔드포인트 POST /claims/car-accidents"
```

---

## Task 5: 전체 회귀

- [ ] **Step 1:** Run: `cd backend && ./gradlew test` → BUILD SUCCESSFUL.
- [ ] **Step 2:** `git -C /Users/heeyoon/Desktop/insurance status --short` → 업로드 잔여 파일 없음.

---

## Self-Review (작성자 점검)

**Spec 커버리지(이슈 C):** UC09 접수+저장 → Task 1·3 ✓ / 접수번호 발급 → Task 3 ✓ / 직원 알림 → Task 3 ✓ / 가입자 안내 → Task 3 ✓ / 대인사고(hasInjury, injuredCount) → Task 1·3 ✓ / 첨부 검증(E1) → Task 4(이슈A 재사용) ✓ / 자동차계약 검증 → Task 3 ✓.
**미커버(의도적):** 자동차 청구 심사/지급(범위 밖), 조회·분석(이슈 D).
**타입 일관성:** `report(...)` 시그니처, `CarAccidentResultResponse.from`, `fileStorage.store/delete`, `attachmentValidator.validate` — 이슈 A/C 동일.
**확인 필요:** `CarInsuranceProduct` 생성자(이슈 A에서 확인: productName, description, basePremium, vehicleType, driverScopeType), `JwtTokenProvider.createToken(Long,String)`.

---

## Execution Handoff
브랜치 `epic4-C-car-accident-report`(base: epic4-B). Subagent-Driven 실행.
