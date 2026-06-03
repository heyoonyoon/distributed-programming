# Epic 2 · 이슈 A — 보험 가입 요청(UC02) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Policyholder가 상품을 선택해 가입을 신청하고(InsuranceApplication 생성, status=PENDING), 본인 신청 목록을 조회하고, PENDING 건을 취소할 수 있게 한다.

**Architecture:** 기존 product/user 도메인의 JPA 단일 엔티티·Lombok·서비스·컨트롤러 관례를 따른다. 개인정보는 복제하지 않고 인증 주체 Policyholder를 참조한다(ADR 0002). 종류별 추가정보(VehicleInfo/MedicalHistory)만 @Embeddable로 Application에 둔다. 알림은 로그 기반 mock으로 처리한다.

**Tech Stack:** Spring Boot, Spring Data JPA, Spring Security(JWT), Bean Validation, Lombok, JUnit5 + MockMvc + H2.

**사전 준수 (CLAUDE.md):**
- spec: `docs/superpowers/specs/2026-06-03-epic2-enrollment-underwriting-design.md`
- 용어: `CONTEXT.md` (InsuranceApplication, ApplicationStatus, VehicleInfo/MedicalHistory는 영문 그대로). Policyholder/InsuranceProduct 등 동의어 금지.
- 결정: ADR 0002(개인정보 비복제, 참조만).

**프로젝트 규약 메모:**
- API 경로에 `/api` 접두어 없음 (예: `/products`, `/me`). 본 이슈는 `/applications`.
- 역할 권한: JWT가 `ROLE_<userType>` 부여 → `ROLE_POLICYHOLDER`, `ROLE_EMPLOYEE`.
- 기존 GlobalExceptionHandler: `IllegalArgumentException`→404, `IllegalStateException`→403. 따라서 **400/409는 전용 예외 신설**.
- 테스트는 `@SpringBootTest @AutoConfigureMockMvc`, 토큰은 `tokenProvider.createToken(userId, "POLICYHOLDER")`.

---

## File Structure

- Create: `domain/application/ApplicationStatus.java` — 신청 상태 enum (PENDING/APPROVED/REJECTED/CANCELLED).
- Create: `domain/application/VehicleInfo.java` — 자동차 추가정보 @Embeddable.
- Create: `domain/application/MedicalHistory.java` — 의료 고지 @Embeddable.
- Create: `domain/application/InsuranceApplication.java` — 가입 신청 엔티티(생성 시 종류 정합성 검증, cancel()).
- Create: `repository/ApplicationRepository.java` — 신청 조회(신청자별).
- Create: `service/InvalidRequestException.java` — 400 매핑용 도메인 검증 예외.
- Create: `service/IllegalStateTransitionException.java` — 409 매핑용 상태 전이 예외.
- Create: `service/NotificationSender.java` + `service/LogNotificationSender.java` — 로그 기반 mock 알림.
- Create: `service/ApplicationService.java` — 신청 생성/조회/취소 유스케이스 조율.
- Create: `web/dto/CreateApplicationRequest.java` — 가입 신청 요청 본문.
- Create: `web/dto/ApplicationResponse.java` — 생성/단건 응답.
- Create: `web/dto/ApplicationSummaryResponse.java` — 목록 응답.
- Create: `web/controller/ApplicationController.java` — `/applications` 엔드포인트.
- Modify: `web/GlobalExceptionHandler.java` — 400/409 핸들러 추가.
- Modify: `security/SecurityConfig.java` — `/applications/**` 경로 권한.

---

## Task 1: ApplicationStatus enum

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/application/ApplicationStatus.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/application/ApplicationStatusTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @Test
    void 네_가지_상태값을_가진다() {
        assertThat(ApplicationStatus.values())
                .containsExactlyInAnyOrder(
                        ApplicationStatus.PENDING,
                        ApplicationStatus.APPROVED,
                        ApplicationStatus.REJECTED,
                        ApplicationStatus.CANCELLED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ApplicationStatusTest"`
Expected: 컴파일 실패 (ApplicationStatus 없음).

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.domain.application;

/** 가입 신청 진행 상태. 조건부 여부는 여기 두지 않는다(ADR 0003 — ReviewResult가 소유). */
public enum ApplicationStatus {
    PENDING, APPROVED, REJECTED, CANCELLED
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ApplicationStatusTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/application/ApplicationStatus.java backend/src/test/java/com/distribution/insurance/domain/application/ApplicationStatusTest.java
git commit -m "feat(epic2): ApplicationStatus enum (PENDING/APPROVED/REJECTED/CANCELLED)"
```

---

## Task 2: VehicleInfo / MedicalHistory @Embeddable

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/application/VehicleInfo.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/application/MedicalHistory.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/application/AdditionalInfoTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdditionalInfoTest {

    @Test
    void 차량정보는_네_필드를_보존한다() {
        VehicleInfo v = new VehicleInfo("12가3456", "승용차", 2020, 5);
        assertThat(v.getPlateNumber()).isEqualTo("12가3456");
        assertThat(v.getVehicleType()).isEqualTo("승용차");
        assertThat(v.getModelYear()).isEqualTo(2020);
        assertThat(v.getDrivingExperienceYears()).isEqualTo(5);
    }

    @Test
    void 의료고지는_세_필드를_보존한다() {
        MedicalHistory m = new MedicalHistory("고혈압", "2019년 입원", "혈압약");
        assertThat(m.getCurrentConditions()).isEqualTo("고혈압");
        assertThat(m.getPastHospitalization()).isEqualTo("2019년 입원");
        assertThat(m.getMedications()).isEqualTo("혈압약");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*AdditionalInfoTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

`VehicleInfo.java`:
```java
package com.distribution.insurance.domain.application;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자동차보험 가입 시 추가정보(UC02). */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class VehicleInfo {

    private String plateNumber;          // 차량번호
    private String vehicleType;          // 차종
    private int modelYear;               // 연식
    private int drivingExperienceYears;  // 운전 경력(년)

    public VehicleInfo(String plateNumber, String vehicleType, int modelYear, int drivingExperienceYears) {
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.modelYear = modelYear;
        this.drivingExperienceYears = drivingExperienceYears;
    }
}
```

`MedicalHistory.java`:
```java
package com.distribution.insurance.domain.application;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 의료보험 가입 시 고지 항목(UC02). */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MedicalHistory {

    @jakarta.persistence.Column(length = 500)
    private String currentConditions;     // 현재 병력
    @jakarta.persistence.Column(length = 500)
    private String pastHospitalization;   // 과거 입원 이력
    @jakarta.persistence.Column(length = 500)
    private String medications;           // 복용 중인 약물

    public MedicalHistory(String currentConditions, String pastHospitalization, String medications) {
        this.currentConditions = currentConditions;
        this.pastHospitalization = pastHospitalization;
        this.medications = medications;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*AdditionalInfoTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/application/VehicleInfo.java backend/src/main/java/com/distribution/insurance/domain/application/MedicalHistory.java backend/src/test/java/com/distribution/insurance/domain/application/AdditionalInfoTest.java
git commit -m "feat(epic2): VehicleInfo/MedicalHistory 임베디드 추가정보"
```

---

## Task 3: InvalidRequestException / IllegalStateTransitionException

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/InvalidRequestException.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/IllegalStateTransitionException.java`

> 테스트는 Task 6(핸들러 매핑)·Task 4(도메인 throw)에서 간접 검증한다. 이 태스크는 예외 타입만 도입한다.

- [ ] **Step 1: Write minimal implementation**

`InvalidRequestException.java`:
```java
package com.distribution.insurance.service;

/** 도메인 검증 실패(종류-추가정보 불일치, 할증 규칙 위반 등) → 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
```

`IllegalStateTransitionException.java`:
```java
package com.distribution.insurance.service;

/** 허용되지 않은 상태 전이(비PENDING 취소·재심사 등) → 409. */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/InvalidRequestException.java backend/src/main/java/com/distribution/insurance/service/IllegalStateTransitionException.java
git commit -m "feat(epic2): 400/409 매핑용 도메인 예외 추가"
```

---

## Task 4: InsuranceApplication 엔티티 (생성 시 종류 정합성, cancel)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/application/InsuranceApplication.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/application/InsuranceApplicationTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.domain.application;

import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.IllegalStateTransitionException;
import com.distribution.insurance.service.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuranceApplicationTest {

    private Policyholder applicant() {
        return new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
    }

    private CarInsuranceProduct carProduct() {
        return new CarInsuranceProduct("안심드라이브", "대인대물", 45000, "승용차", "가족한정");
    }

    private HealthInsuranceProduct healthProduct() {
        return new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
    }

    private VehicleInfo vehicle() {
        return new VehicleInfo("12가3456", "승용차", 2020, 5);
    }

    private MedicalHistory medical() {
        return new MedicalHistory("없음", "없음", "없음");
    }

    @Test
    void 생성시_상태는_PENDING이고_신청일시가_기록된다() {
        InsuranceApplication app = new InsuranceApplication(applicant(), carProduct(), vehicle(), null);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(app.getAppliedAt()).isNotNull();
    }

    @Test
    void 자동차상품은_차량정보_필수_의료고지_금지() {
        assertThatThrownBy(() -> new InsuranceApplication(applicant(), carProduct(), null, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> new InsuranceApplication(applicant(), carProduct(), vehicle(), medical()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 의료상품은_의료고지_필수_차량정보_금지() {
        assertThatThrownBy(() -> new InsuranceApplication(applicant(), healthProduct(), null, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> new InsuranceApplication(applicant(), healthProduct(), vehicle(), medical()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void PENDING_신청은_취소되어_CANCELLED가_된다() {
        InsuranceApplication app = new InsuranceApplication(applicant(), healthProduct(), null, medical());
        app.cancel();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
    }

    @Test
    void 이미_취소된_신청을_다시_취소하면_예외() {
        InsuranceApplication app = new InsuranceApplication(applicant(), healthProduct(), null, medical());
        app.cancel();
        assertThatThrownBy(app::cancel)
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*InsuranceApplicationTest"`
Expected: 컴파일 실패 (InsuranceApplication 없음).

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.domain.application;

import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.IllegalStateTransitionException;
import com.distribution.insurance.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_application")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InsuranceApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime appliedAt;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id")
    private Policyholder applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private InsuranceProduct product;

    @Embedded
    private VehicleInfo vehicleInfo;      // 자동차상품만, nullable

    @Embedded
    @AttributeOverrides({})
    private MedicalHistory medicalHistory; // 의료상품만, nullable

    public InsuranceApplication(Policyholder applicant, InsuranceProduct product,
                                VehicleInfo vehicleInfo, MedicalHistory medicalHistory) {
        validateTypeConsistency(product, vehicleInfo, medicalHistory);
        this.applicant = applicant;
        this.product = product;
        this.vehicleInfo = vehicleInfo;
        this.medicalHistory = medicalHistory;
        this.status = ApplicationStatus.PENDING;
        this.appliedAt = LocalDateTime.now();
    }

    private static void validateTypeConsistency(InsuranceProduct product,
                                                VehicleInfo vehicleInfo, MedicalHistory medicalHistory) {
        if (product instanceof CarInsuranceProduct) {
            if (vehicleInfo == null || medicalHistory != null) {
                throw new InvalidRequestException("자동차보험은 차량정보가 필수이며 의료고지는 입력할 수 없습니다.");
            }
        } else if (product instanceof HealthInsuranceProduct) {
            if (medicalHistory == null || vehicleInfo != null) {
                throw new InvalidRequestException("의료보험은 의료고지가 필수이며 차량정보는 입력할 수 없습니다.");
            }
        } else {
            throw new InvalidRequestException("지원하지 않는 상품 종류입니다.");
        }
    }

    /** PENDING 건만 취소 가능(UC02). 그 외 상태는 상태 전이 위반 → 409. */
    public void cancel() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new IllegalStateTransitionException("심사 대기 상태의 신청만 취소할 수 있습니다.");
        }
        this.status = ApplicationStatus.CANCELLED;
    }
}
```

> 주: `MedicalHistory`/`VehicleInfo`가 같은 엔티티에 임베드되며 컬럼명이 겹치지 않으므로 별도 @AttributeOverride 불필요. 위 `@AttributeOverrides({})`는 제거해도 무방하나 명시적 표시로 둔다.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*InsuranceApplicationTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/application/InsuranceApplication.java backend/src/test/java/com/distribution/insurance/domain/application/InsuranceApplicationTest.java
git commit -m "feat(epic2): InsuranceApplication 엔티티(종류 정합성 검증·cancel)"
```

---

## Task 5: ApplicationRepository (+ 영속성 테스트)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/repository/ApplicationRepository.java`
- Test: `backend/src/test/java/com/distribution/insurance/repository/ApplicationRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.application.*;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ApplicationRepositoryTest {

    @Autowired ApplicationRepository applicationRepository;
    @Autowired EntityManager em;

    @Test
    void 신청을_저장하고_신청자별로_조회한다() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        em.persist(ph);
        InsuranceProduct product = new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
        em.persist(product);

        InsuranceApplication app = new InsuranceApplication(
                ph, product, null, new MedicalHistory("없음", "없음", "없음"));
        applicationRepository.save(app);
        em.flush();
        em.clear();

        List<InsuranceApplication> found = applicationRepository.findByApplicantId(ph.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getMedicalHistory().getCurrentConditions()).isEqualTo("없음");
        assertThat(found.get(0).getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ApplicationRepositoryTest"`
Expected: 컴파일 실패 (ApplicationRepository 없음).

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.application.ApplicationStatus;
import com.distribution.insurance.domain.application.InsuranceApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<InsuranceApplication, Long> {

    List<InsuranceApplication> findByApplicantId(Long applicantId);

    List<InsuranceApplication> findByStatus(ApplicationStatus status);
}
```

> `findByStatus`는 이슈 B(심사 대기 목록)에서 사용한다. 여기서 미리 선언해 둔다.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ApplicationRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/repository/ApplicationRepository.java backend/src/test/java/com/distribution/insurance/repository/ApplicationRepositoryTest.java
git commit -m "feat(epic2): ApplicationRepository(신청자별·상태별 조회)"
```

---

## Task 6: GlobalExceptionHandler — 400/409 매핑 추가

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/web/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.web;

import com.distribution.insurance.service.IllegalStateTransitionException;
import com.distribution.insurance.service.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 도메인_검증_예외는_400() {
        var response = handler.handleInvalidRequest(new InvalidRequestException("형식 오류"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("형식 오류");
    }

    @Test
    void 상태전이_예외는_409() {
        var response = handler.handleStateTransition(new IllegalStateTransitionException("이미 처리됨"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("이미 처리됨");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*GlobalExceptionHandlerTest"`
Expected: 컴파일 실패 (handleInvalidRequest/handleStateTransition 없음).

- [ ] **Step 3: Write minimal implementation**

기존 클래스에 import와 두 핸들러를 추가한다.

import 추가:
```java
import com.distribution.insurance.service.IllegalStateTransitionException;
import com.distribution.insurance.service.InvalidRequestException;
```

`handleConstraintViolation` 메서드 뒤에 추가:
```java
    /** 도메인 검증 실패(종류-추가정보 불일치, 할증 규칙 위반 등) → 400. */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<String> handleInvalidRequest(InvalidRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    /** 허용되지 않은 상태 전이(비PENDING 취소·재심사) → 409. */
    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<String> handleStateTransition(IllegalStateTransitionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*GlobalExceptionHandlerTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/GlobalExceptionHandler.java backend/src/test/java/com/distribution/insurance/web/GlobalExceptionHandlerTest.java
git commit -m "feat(epic2): GlobalExceptionHandler 400/409 매핑 추가"
```

---

## Task 7: NotificationSender (로그 기반 mock)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/NotificationSender.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/LogNotificationSender.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/LogNotificationSenderTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LogNotificationSenderTest {

    @Test
    void 발송하면_마지막_메시지를_보관한다() {
        LogNotificationSender sender = new LogNotificationSender();
        sender.send("h@test.com", "010-1234", "접수번호 7 — 예상 처리 3일");
        assertThat(sender.lastMessage()).contains("접수번호 7");
    }
}
```

> mock이므로 부수효과 검증은 마지막 메시지 보관으로 단순화한다. 실제 발송은 로그만.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*LogNotificationSenderTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

`NotificationSender.java`:
```java
package com.distribution.insurance.service;

/** 가입 접수·심사 결과 통보(UC02/UC13). 텍스트 구현에서는 mock. */
public interface NotificationSender {
    void send(String email, String phone, String message);
}
```

`LogNotificationSender.java`:
```java
package com.distribution.insurance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 로그 기반 mock 알림(Epic 0 MockIdentityVerification 패턴). */
@Component
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);
    private String lastMessage;

    @Override
    public void send(String email, String phone, String message) {
        this.lastMessage = message;
        log.info("[알림] to email={}, phone={} : {}", email, phone, message);
    }

    /** 테스트·디버그용 마지막 발송 메시지. */
    public String lastMessage() {
        return lastMessage;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*LogNotificationSenderTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/NotificationSender.java backend/src/main/java/com/distribution/insurance/service/LogNotificationSender.java backend/src/test/java/com/distribution/insurance/service/LogNotificationSenderTest.java
git commit -m "feat(epic2): 로그 기반 mock NotificationSender"
```

---

## Task 8: ApplicationService (신청 생성/조회/취소)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/ApplicationService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/ApplicationServiceTest.java`

서비스 계약:
- `InsuranceApplication apply(Long applicantId, Long productId, VehicleInfo vehicleInfo, MedicalHistory medicalHistory)` — 신청자/상품 조회(없으면 `IllegalArgumentException`→404), 엔티티 생성(종류 정합성은 엔티티가 검증), 저장, 접수 알림 발송, 반환.
- `List<InsuranceApplication> myApplications(Long applicantId)` — 신청자별 목록.
- `void cancel(Long applicantId, Long applicationId)` — 신청 조회(없으면 404), 본인 소유 아니면 `IllegalStateException`→403, `app.cancel()` 호출(비PENDING이면 409).

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.application.*;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ApplicationRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ApplicationServiceTest {

    @Autowired ApplicationService applicationService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ApplicationRepository applicationRepository;

    private Long policyholderId() {
        Policyholder ph = userRepository.save(new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
        return ph.getId();
    }

    private Long healthProductId() {
        InsuranceProduct p = productRepository.save(
                new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120));
        return p.getId();
    }

    @Test
    void 가입신청하면_PENDING으로_저장된다() {
        Long phId = policyholderId();
        Long productId = healthProductId();

        InsuranceApplication app = applicationService.apply(
                phId, productId, null, new MedicalHistory("없음", "없음", "없음"));

        assertThat(app.getId()).isNotNull();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void 없는_상품으로_신청하면_404성_예외() {
        Long phId = policyholderId();
        assertThatThrownBy(() -> applicationService.apply(
                phId, 999999L, null, new MedicalHistory("없음", "없음", "없음")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 본인_PENDING_신청을_취소할_수_있다() {
        Long phId = policyholderId();
        Long productId = healthProductId();
        InsuranceApplication app = applicationService.apply(
                phId, productId, null, new MedicalHistory("없음", "없음", "없음"));

        applicationService.cancel(phId, app.getId());

        assertThat(applicationRepository.findById(app.getId()).get().getStatus())
                .isEqualTo(ApplicationStatus.CANCELLED);
    }

    @Test
    void 타인의_신청을_취소하면_403성_예외() {
        Long ownerId = policyholderId();
        Long productId = healthProductId();
        InsuranceApplication app = applicationService.apply(
                ownerId, productId, null, new MedicalHistory("없음", "없음", "없음"));
        Long otherId = userRepository.save(new Policyholder("타인", "x@test.com", "010", "pw",
                "910101-1234567", LocalDate.of(1991, 1, 1), "주소", "계좌")).getId();

        assertThatThrownBy(() -> applicationService.cancel(otherId, app.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ApplicationServiceTest"`
Expected: 컴파일 실패 (ApplicationService 없음).

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.application.VehicleInfo;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ApplicationRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository,
                              NotificationSender notificationSender) {
        this.applicationRepository = applicationRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public InsuranceApplication apply(Long applicantId, Long productId,
                                      VehicleInfo vehicleInfo, MedicalHistory medicalHistory) {
        Policyholder applicant = requirePolicyholder(applicantId);
        InsuranceProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        InsuranceApplication app = new InsuranceApplication(applicant, product, vehicleInfo, medicalHistory);
        applicationRepository.save(app);

        notificationSender.send(applicant.getEmail(), applicant.getPhone(),
                "가입 신청이 접수되었습니다. 접수번호 " + app.getId() + " — 예상 처리 기간 3영업일.");
        return app;
    }

    @Transactional(readOnly = true)
    public List<InsuranceApplication> myApplications(Long applicantId) {
        return applicationRepository.findByApplicantId(applicantId);
    }

    @Transactional
    public void cancel(Long applicantId, Long applicationId) {
        InsuranceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));
        if (!app.getApplicant().getId().equals(applicantId)) {
            throw new IllegalStateException("본인의 신청만 취소할 수 있습니다.");
        }
        app.cancel();
    }

    private Policyholder requirePolicyholder(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u instanceof Policyholder)
                .map(u -> (Policyholder) u)
                .orElseThrow(() -> new IllegalArgumentException("가입자를 찾을 수 없습니다."));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ApplicationServiceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/service/ApplicationService.java backend/src/test/java/com/distribution/insurance/service/ApplicationServiceTest.java
git commit -m "feat(epic2): ApplicationService(신청 생성·조회·취소)"
```

---

## Task 9: DTO (CreateApplicationRequest / ApplicationResponse / ApplicationSummaryResponse)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/CreateApplicationRequest.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ApplicationResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ApplicationSummaryResponse.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/dto/CreateApplicationRequestTest.java`

요청 DTO는 productId + 선택적 vehicle/medical 블록을 받고, 도메인 타입으로 변환하는 메서드를 제공한다. 개인정보는 받지 않는다(ADR 0002).

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.application.VehicleInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateApplicationRequestTest {

    @Test
    void 차량블록은_VehicleInfo로_변환된다() {
        var req = new CreateApplicationRequest(
                10L,
                new CreateApplicationRequest.VehicleInfoDto("12가3456", "승용차", 2020, 5),
                null);
        VehicleInfo v = req.toVehicleInfo();
        assertThat(v.getPlateNumber()).isEqualTo("12가3456");
        assertThat(req.toMedicalHistory()).isNull();
    }

    @Test
    void 의료블록은_MedicalHistory로_변환된다() {
        var req = new CreateApplicationRequest(
                10L, null,
                new CreateApplicationRequest.MedicalHistoryDto("고혈압", "없음", "혈압약"));
        MedicalHistory m = req.toMedicalHistory();
        assertThat(m.getCurrentConditions()).isEqualTo("고혈압");
        assertThat(req.toVehicleInfo()).isNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*CreateApplicationRequestTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: Write minimal implementation**

`CreateApplicationRequest.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.application.VehicleInfo;
import jakarta.validation.constraints.NotNull;

/** 가입 신청 요청. 개인정보는 인증 주체에서 읽으므로 받지 않는다(ADR 0002). */
public record CreateApplicationRequest(
        @NotNull Long productId,
        VehicleInfoDto vehicleInfo,
        MedicalHistoryDto medicalHistory) {

    public record VehicleInfoDto(String plateNumber, String vehicleType,
                                 int modelYear, int drivingExperienceYears) {}

    public record MedicalHistoryDto(String currentConditions, String pastHospitalization,
                                    String medications) {}

    public VehicleInfo toVehicleInfo() {
        if (vehicleInfo == null) return null;
        return new VehicleInfo(vehicleInfo.plateNumber(), vehicleInfo.vehicleType(),
                vehicleInfo.modelYear(), vehicleInfo.drivingExperienceYears());
    }

    public MedicalHistory toMedicalHistory() {
        if (medicalHistory == null) return null;
        return new MedicalHistory(medicalHistory.currentConditions(),
                medicalHistory.pastHospitalization(), medicalHistory.medications());
    }
}
```

`ApplicationResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.InsuranceApplication;

import java.time.LocalDateTime;

public record ApplicationResponse(Long applicationId, String status, LocalDateTime appliedAt) {
    public static ApplicationResponse from(InsuranceApplication app) {
        return new ApplicationResponse(app.getId(), app.getStatus().name(), app.getAppliedAt());
    }
}
```

`ApplicationSummaryResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.InsuranceApplication;

import java.time.LocalDateTime;

public record ApplicationSummaryResponse(
        Long applicationId, String status, LocalDateTime appliedAt,
        Long productId, String productName) {

    public static ApplicationSummaryResponse from(InsuranceApplication app) {
        return new ApplicationSummaryResponse(
                app.getId(), app.getStatus().name(), app.getAppliedAt(),
                app.getProduct().getId(), app.getProduct().getProductName());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*CreateApplicationRequestTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/dto/CreateApplicationRequest.java backend/src/main/java/com/distribution/insurance/web/dto/ApplicationResponse.java backend/src/main/java/com/distribution/insurance/web/dto/ApplicationSummaryResponse.java backend/src/test/java/com/distribution/insurance/web/dto/CreateApplicationRequestTest.java
git commit -m "feat(epic2): 가입 신청 요청/응답 DTO"
```

---

## Task 10: SecurityConfig — /applications 권한

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java`

> 권한 매핑은 Task 11의 컨트롤러 통합 테스트(403/201)에서 검증된다. 이 태스크는 경로 규칙만 추가한다.

- [ ] **Step 1: Modify SecurityConfig**

`authorizeHttpRequests` 람다의 `.anyRequest().authenticated()` **앞에** 다음 줄을 추가:
```java
                        .requestMatchers("/applications/**").hasRole("POLICYHOLDER")
```

수정 후 해당 블록:
```java
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                        .requestMatchers("/applications/**").hasRole("POLICYHOLDER")
                        .anyRequest().authenticated())
```

- [ ] **Step 2: Verify compile**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java
git commit -m "feat(epic2): /applications 경로 POLICYHOLDER 권한"
```

---

## Task 11: ApplicationController + 통합 테스트

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/ApplicationController.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/ApplicationControllerTest.java`

엔드포인트:
- `POST /applications` → 201 + ApplicationResponse
- `GET /applications/me` → 200 + List<ApplicationSummaryResponse>
- `POST /applications/{id}/cancel` → 200 (no body)

- [ ] **Step 1: Write the failing test**

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
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
class ApplicationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    Long phId; String phToken; Long healthProductId; String employeeToken;

    @BeforeEach
    void setUp() {
        Policyholder ph = userRepository.save(new Policyholder("홍길동", "h@test.com", "010", encoder.encode("pw"),
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
        phId = ph.getId();
        phToken = tokenProvider.createToken(phId, "POLICYHOLDER");

        InsuranceEmployee emp = userRepository.save(new InsuranceEmployee(
                "심사역", "e@test.com", "010", encoder.encode("pw"), "심사팀", 0));
        employeeToken = tokenProvider.createToken(emp.getId(), "EMPLOYEE");

        InsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120));
        healthProductId = product.getId();
    }

    private String medicalBody(Long productId) {
        return "{\"productId\":" + productId + ",\"medicalHistory\":"
                + "{\"currentConditions\":\"없음\",\"pastHospitalization\":\"없음\",\"medications\":\"없음\"}}";
    }

    @Test
    void 가입신청하면_201과_PENDING상태를_반환한다() throws Exception {
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + phToken)
                        .contentType("application/json")
                        .content(medicalBody(healthProductId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.applicationId").isNumber());
    }

    @Test
    void 의료상품에_차량정보를_보내면_400() throws Exception {
        String body = "{\"productId\":" + healthProductId + ",\"vehicleInfo\":"
                + "{\"plateNumber\":\"12가3456\",\"vehicleType\":\"승용차\",\"modelYear\":2020,\"drivingExperienceYears\":5}}";
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + phToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 없는_상품으로_신청하면_404() throws Exception {
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + phToken)
                        .contentType("application/json").content(medicalBody(999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 직원토큰으로_신청하면_403() throws Exception {
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType("application/json").content(medicalBody(healthProductId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 내_신청목록을_조회한다() throws Exception {
        mockMvc.perform(post("/applications")
                .header("Authorization", "Bearer " + phToken)
                .contentType("application/json").content(medicalBody(healthProductId)));

        mockMvc.perform(get("/applications/me")
                        .header("Authorization", "Bearer " + phToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("건강플러스"));
    }

    @Test
    void PENDING_신청을_취소하면_200() throws Exception {
        String response = mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + phToken)
                        .contentType("application/json").content(medicalBody(healthProductId)))
                .andReturn().getResponse().getContentAsString();
        Long appId = com.jayway.jsonpath.JsonPath.parse(response).read("$.applicationId", Long.class);

        mockMvc.perform(post("/applications/" + appId + "/cancel")
                        .header("Authorization", "Bearer " + phToken))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "*ApplicationControllerTest"`
Expected: 404/메서드 없음으로 실패.

- [ ] **Step 3: Write minimal implementation**

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.service.ApplicationService;
import com.distribution.insurance.web.dto.ApplicationResponse;
import com.distribution.insurance.web.dto.ApplicationSummaryResponse;
import com.distribution.insurance.web.dto.CreateApplicationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@AuthenticationPrincipal Long userId,
                                     @Valid @RequestBody CreateApplicationRequest request) {
        InsuranceApplication app = applicationService.apply(
                userId, request.productId(), request.toVehicleInfo(), request.toMedicalHistory());
        return ApplicationResponse.from(app);
    }

    @GetMapping("/me")
    public List<ApplicationSummaryResponse> myApplications(@AuthenticationPrincipal Long userId) {
        return applicationService.myApplications(userId).stream()
                .map(ApplicationSummaryResponse::from)
                .toList();
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        applicationService.cancel(userId, id);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "*ApplicationControllerTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/controller/ApplicationController.java backend/src/test/java/com/distribution/insurance/web/controller/ApplicationControllerTest.java
git commit -m "feat(epic2): ApplicationController(/applications 가입 신청·조회·취소)"
```

---

## Task 12: 전체 테스트 확인

- [ ] **Step 1: Run full suite**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL, 전체 그린.

- [ ] **Step 2: (실패 시)** superpowers:systematic-debugging로 원인 분석 후 수정. 추측 금지.

---

## 완료 기준 (DoD)
- UC02 베이직 플로우(신청 접수→PENDING→접수 알림) 동작.
- 종류-추가정보 불일치 400, 없는 상품 404, 직원 접근 403.
- 본인 PENDING 취소 200, 타인 취소 403, 비PENDING 취소 409(서비스/엔티티 레벨에서 보장 — 통합은 이슈 B 심사 후 상태에서 추가 검증 가능).
- 개인정보를 요청 본문으로 받지 않음(ADR 0002 준수).
- `./gradlew test` 전체 그린.
