# Epic 3 이슈 C — 미납 고지서 발송(UC16) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 매일 자동으로 미납(연체) 계약을 찾아 미납 고지서(Notice)를 생성·발송·기록하고, 발송 실패 시 재시도하며, 30일 초과 연체는 해지예고+직원 알림을 한다.

**Architecture:** `@Scheduled` 일일 작업이 `NoticeService.issueOverdueNotices(asOf)`를 호출한다. 서비스는 ACTIVE 계약마다 `BillingCalculator`(ADR 0004 재사용)로 미납을 계산하고, 연체(overdueDays>0)면 `Notice`를 생성해 `NotificationSender`로 발송한다. 발송은 최대 3회 재시도하고 결과를 Notice에 기록한다. 스케줄 메서드에는 로직을 두지 않고 서비스에 위임해 서비스 메서드를 직접 테스트한다.

**Tech Stack:** Spring Boot 4 / Java 21, Spring Data JPA, Spring `@Scheduled`, JUnit5, Lombok.

**준수 문서:**
- spec: `docs/superpowers/specs/2026-06-03-epic3-contract-billing-design.md` (이슈 C)
- 용어: `CONTEXT.md` — `Notice`, `OverdueInterest`. 동의어(독촉장/reminder) 금지.
- 결정: ADR 0004(온더플라이 미납 계산, FIFO, 연 10%). `BillingCalculator`/`BillingStatus` 재사용.

**선행(이슈 A·B, main):** `InsuranceContract`(getStartDate/getEndDate/getMonthlyPremium/getPolicyholder/getProduct/getStatus), `ContractStatus.ACTIVE`, `BillingCalculator.compute(contract, successCount, asOf) → BillingStatus(unpaidCount/unpaidPrincipal/oldestUnpaidDueDate/overdueDays/overdueInterest, hasUnpaid()/isOverdue())`, `PaymentRepository.countByContractIdAndStatus(contractId, SUCCESS)`, `NotificationSender.send(email, phone, message)`(void, 실패 시 RuntimeException을 던지는 구현도 가능), `ContractRepository.findByPolicyholderId` 및 `findAll()`(JpaRepository 기본).

**다이어그램 대비 보강(plan 내 명시):** 다이어그램 `Notice`는 issuedAt/dueAmount/overdueDays/isTerminationWarning + `send()`만 정의한다. 발송 "기록"(UC16 5단계: 누구에게·언제·어떻게)을 위해 `dueDate`, `overdueInterest`, `sentAt`, `delivered`, `attempts` 필드를 추가한다. 발송 동작은 도메인이 인프라(NotificationSender)에 의존하지 않도록 `NoticeService`가 수행하고, `Notice`는 데이터+메시지 빌드+발송결과 기록만 담당한다(다이어그램의 `send()`는 서비스로 이동). 이는 책임 분리를 위한 구조 선택이며 되돌리기 쉬워 ADR로 남기지 않는다.

---

## File Structure

- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/Notice.java`
- Create: `backend/src/main/java/com/distribution/insurance/repository/NoticeRepository.java`
- Modify: `backend/src/main/java/com/distribution/insurance/repository/UserRepository.java` (직원 목록 조회)
- Create: `backend/src/main/java/com/distribution/insurance/service/NoticeService.java`
- Create: `backend/src/main/java/com/distribution/insurance/config/SchedulingConfig.java` (@EnableScheduling)
- Create: `backend/src/main/java/com/distribution/insurance/service/OverdueNoticeScheduler.java` (@Scheduled 래퍼)
- Tests:
  - `backend/src/test/java/com/distribution/insurance/domain/contract/NoticeTest.java`
  - `backend/src/test/java/com/distribution/insurance/service/NoticeServiceTest.java`

명령은 `backend/`에서: `cd /Users/heeyoon/Desktop/insurance/backend && ./gradlew ...`

---

## Task 1: Notice 도메인 + NoticeRepository

**Files:**
- Create: `Notice.java`, `NoticeRepository.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/contract/NoticeTest.java`

- [ ] **Step 1: 실패 테스트 작성**

`NoticeTest.java`:
```java
package com.distribution.insurance.domain.contract;

import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class NoticeTest {

    private InsuranceContract contract() {
        // 실제 Policyholder/HealthInsuranceProduct 생성자는 도메인/이슈A 테스트에서 확인해 맞춘다.
        Policyholder ph = TestFixtures.policyholder();
        return new InsuranceContract(ph, TestFixtures.healthProduct(), 30000, LocalDate.of(2026, 1, 1));
    }

    private BillingStatus overdueStatus(int overdueDays) {
        // 미납 2회차, 원금 60000, 연체이자 임의
        return new BillingStatus(2, 60000, LocalDate.of(2026, 2, 1), overdueDays, 500L);
    }

    @Test
    void 연체_30일_이하면_해지예고가_아니다() {
        Notice n = Notice.of(contract(), overdueStatus(20), LocalDate.of(2026, 2, 21));
        assertThat(n.isTerminationWarning()).isFalse();
        assertThat(n.getDueAmount()).isEqualTo(60000);
        assertThat(n.getOverdueDays()).isEqualTo(20);
        assertThat(n.getIssuedAt()).isEqualTo(LocalDate.of(2026, 2, 21));
        assertThat(n.isDelivered()).isFalse(); // 아직 발송 전
    }

    @Test
    void 연체_30일_초과면_해지예고다() {
        Notice n = Notice.of(contract(), overdueStatus(31), LocalDate.of(2026, 3, 4));
        assertThat(n.isTerminationWarning()).isTrue();
    }

    @Test
    void 발송_메시지는_미납금액과_연체이자를_담고_해지예고면_경고문구를_포함한다() {
        Notice warn = Notice.of(contract(), overdueStatus(31), LocalDate.of(2026, 3, 4));
        String msg = warn.buildMessage("실손의료");
        assertThat(msg).contains("실손의료").contains("60000").contains("500");
        assertThat(msg).contains("해지");

        Notice mild = Notice.of(contract(), overdueStatus(10), LocalDate.of(2026, 2, 11));
        assertThat(mild.buildMessage("실손의료")).doesNotContain("해지");
    }

    @Test
    void 발송_결과를_기록한다() {
        Notice n = Notice.of(contract(), overdueStatus(10), LocalDate.of(2026, 2, 11));
        n.markSent(true, 1);
        assertThat(n.isDelivered()).isTrue();
        assertThat(n.getAttempts()).isEqualTo(1);
        assertThat(n.getSentAt()).isNotNull();
    }
}
```
`TestFixtures`는 이슈 B에서 `domain/contract` 테스트에 만들어져 있다(`git show main:backend/src/test/java/com/distribution/insurance/domain/contract/TestFixtures.java`로 확인). 없거나 다르면 실제 생성자에 맞춰 보강한다.

- [ ] **Step 2: 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.NoticeTest"`
Expected: 컴파일 실패(`Notice` 없음).

- [ ] **Step 3: Notice 작성**

`Notice.java`:
```java
package com.distribution.insurance.domain.contract;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 미납 고지서(UC16). 스케줄러가 자동 생성한다.
 * 다이어그램(issuedAt/dueAmount/overdueDays/isTerminationWarning)에 발송 기록 필드를 보강했다(plan 참고).
 */
@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Notice {

    /** 연체 30일 초과 시 해지예고(UC16 A1). */
    private static final int TERMINATION_WARNING_THRESHOLD_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate issuedAt;
    private LocalDate dueDate;
    private int dueAmount;
    private int overdueDays;
    private long overdueInterest;
    private boolean terminationWarning;

    private LocalDateTime sentAt;
    private boolean delivered;
    private int attempts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private InsuranceContract contract;

    private Notice(InsuranceContract contract, BillingStatus status, LocalDate issuedAt) {
        this.contract = contract;
        this.issuedAt = issuedAt;
        this.dueDate = status.oldestUnpaidDueDate();
        this.dueAmount = status.unpaidPrincipal();
        this.overdueDays = (int) status.overdueDays();
        this.overdueInterest = status.overdueInterest();
        this.terminationWarning = status.overdueDays() > TERMINATION_WARNING_THRESHOLD_DAYS;
        this.delivered = false;
        this.attempts = 0;
    }

    /** 미납 계산 결과로 고지서를 만든다. */
    public static Notice of(InsuranceContract contract, BillingStatus status, LocalDate issuedAt) {
        return new Notice(contract, status, issuedAt);
    }

    public boolean isTerminationWarning() {
        return terminationWarning;
    }

    /** 고지서 본문(UC16 3단계: 계약명·납부기한·미납금액·연체이자·납부방법, 30일 초과 시 해지예고). */
    public String buildMessage(String productName) {
        StringBuilder sb = new StringBuilder()
                .append("[미납 보험료 안내] ").append(productName).append("\n")
                .append("납부기한: ").append(dueDate).append("\n")
                .append("미납금액: ").append(dueAmount).append("원\n")
                .append("연체이자: ").append(overdueInterest).append("원\n")
                .append("연체일수: ").append(overdueDays).append("일\n")
                .append("납부방법: 마이페이지 > 보험료 납부에서 결제하실 수 있습니다.\n");
        if (terminationWarning) {
            sb.append("※ 연체가 30일을 초과하여 계약이 해지될 수 있습니다. 조속히 납부해 주십시오.\n");
        }
        return sb.toString();
    }

    /** 발송 결과 기록(UC16 5단계). */
    public void markSent(boolean delivered, int attempts) {
        this.delivered = delivered;
        this.attempts = attempts;
        this.sentAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 4: NoticeRepository 작성**

`NoticeRepository.java`:
```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.contract.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** 같은 날 중복 고지 방지(UC16 일일 실행). */
    boolean existsByContractIdAndIssuedAt(Long contractId, LocalDate issuedAt);

    List<Notice> findByContractId(Long contractId);
}
```

- [ ] **Step 5: 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.NoticeTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/contract/Notice.java backend/src/main/java/com/distribution/insurance/repository/NoticeRepository.java backend/src/test/java/com/distribution/insurance/domain/contract/NoticeTest.java
git commit -m "feat(epic3-C): Notice 도메인 + 발송기록/해지예고 + NoticeRepository (UC16)"
```

---

## Task 2: UserRepository 직원 조회 추가

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/repository/UserRepository.java`

직원 목록은 Task 3 서비스 테스트(직원 알림)에서 검증한다.

- [ ] **Step 1: 직원 조회 메서드 추가**

`UserRepository.java`에 추가(import 포함):
```java
import com.distribution.insurance.domain.user.InsuranceEmployee;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
```
인터페이스 본문에 추가:
```java
    /** 30일 초과 연체 발생 시 별도 알림 대상(UC16 A1). */
    @Query("select e from InsuranceEmployee e")
    List<InsuranceEmployee> findAllEmployees();
```

- [ ] **Step 2: 컴파일 확인**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/repository/UserRepository.java
git commit -m "feat(epic3-C): UserRepository 직원 목록 조회 (UC16 A1)"
```

---

## Task 3: NoticeService (탐색·생성·발송·재시도·30일경고·직원알림·중복방지)

**Files:**
- Create: `NoticeService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/NoticeServiceTest.java`

- [ ] **Step 1: 실패 테스트 작성**

`NoticeServiceTest.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.contract.Notice;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
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
class NoticeServiceTest {

    @Autowired NoticeService noticeService;
    @Autowired ContractRepository contractRepository;
    @Autowired NoticeRepository noticeRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    Long contractId; Long policyholderId;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        // 실제 생성자는 이슈A ContractControllerTest/도메인 파일에서 확인해 맞춘다.
        Policyholder ph = userRepository.save(TestEntities.policyholder());
        policyholderId = ph.getId();
        HealthInsuranceProduct product = productRepository.save(TestEntities.healthProduct());
        // 4개월 전 시작 → 미납 누적·연체
        InsuranceContract c = contractRepository.save(
                new InsuranceContract(ph, product, 30000, LocalDate.now().minusMonths(4)));
        contractId = c.getId();
    }

    @AfterEach
    void tearDown() {
        noticeRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void 연체_계약에_고지서가_생성되고_발송된다() {
        int created = noticeService.issueOverdueNotices(LocalDate.now());
        assertThat(created).isEqualTo(1);
        List<Notice> notices = noticeRepository.findByContractId(contractId);
        assertThat(notices).hasSize(1);
        assertThat(notices.get(0).isDelivered()).isTrue();
        assertThat(notices.get(0).getAttempts()).isEqualTo(1);
    }

    @Test
    void 같은_날_재실행하면_중복_고지하지_않는다() {
        noticeService.issueOverdueNotices(LocalDate.now());
        int secondRun = noticeService.issueOverdueNotices(LocalDate.now());
        assertThat(secondRun).isZero();
        assertThat(noticeRepository.findByContractId(contractId)).hasSize(1);
    }

    @Test
    void 연체가_없으면_고지서가_생성되지_않는다() {
        // 오늘 시작 계약(연체 없음)만 남기고 기존 계약 제거
        contractRepository.deleteAll();
        Policyholder ph = userRepository.findById(policyholderId).orElseThrow() instanceof Policyholder p ? p : null;
        InsuranceContract fresh = contractRepository.save(
                new InsuranceContract(ph, productRepository.findAll().get(0), 30000, LocalDate.now()));
        int created = noticeService.issueOverdueNotices(LocalDate.now());
        assertThat(created).isZero();
    }

    @Test
    void 연체_30일_초과면_해지예고_고지서가_생성된다() {
        // setUp의 4개월 전 시작 계약은 첫 회차(약 120일) 연체 → 30일 초과
        noticeService.issueOverdueNotices(LocalDate.now());
        Notice n = noticeRepository.findByContractId(contractId).get(0);
        assertThat(n.isTerminationWarning()).isTrue();
    }
}
```
구현자 주의: `TestEntities` 헬퍼는 실제 Policyholder/HealthInsuranceProduct 생성자로 채운다(이슈 A/B 테스트 참고). `연체가_없으면...` 테스트의 instanceof 패턴이 어색하면 단순히 새 Policyholder를 저장해 사용하도록 정리한다 — 핵심 단언(연체 없음→0건)만 지키면 된다.

- [ ] **Step 2: 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.service.NoticeServiceTest"`
Expected: 컴파일 실패(`NoticeService` 없음).

- [ ] **Step 3: NoticeService 작성**

`NoticeService.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.*;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.NoticeRepository;
import com.distribution.insurance.repository.PaymentRepository;
import com.distribution.insurance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 미납 고지서 발송(UC16). 스케줄러가 issueOverdueNotices를 호출한다. */
@Service
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);
    private static final int MAX_SEND_ATTEMPTS = 3;   // UC16 E1: 최대 3회 재시도

    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public NoticeService(ContractRepository contractRepository,
                         PaymentRepository paymentRepository,
                         NoticeRepository noticeRepository,
                         UserRepository userRepository,
                         NotificationSender notificationSender) {
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }

    /**
     * 연체 계약을 찾아 고지서를 생성·발송·기록한다. 생성된 고지서 수를 반환한다.
     * 같은 날 이미 고지한 계약은 건너뛴다(중복 방지).
     */
    @Transactional
    public int issueOverdueNotices(LocalDate asOf) {
        int created = 0;
        for (InsuranceContract c : contractRepository.findAll()) {
            if (c.getStatus() != ContractStatus.ACTIVE) continue;
            if (noticeRepository.existsByContractIdAndIssuedAt(c.getId(), asOf)) continue;

            long success = paymentRepository.countByContractIdAndStatus(c.getId(), PaymentStatus.SUCCESS);
            BillingStatus status = BillingCalculator.compute(c, success, asOf);
            if (!status.isOverdue()) continue;

            Notice notice = Notice.of(c, status, asOf);
            dispatch(notice, c);
            noticeRepository.save(notice);
            created++;
        }
        return created;
    }

    /** 가입자에게 발송(최대 3회 재시도). 30일 초과 시 직원에게도 알린다. */
    private void dispatch(Notice notice, InsuranceContract contract) {
        String message = notice.buildMessage(contract.getProduct().getProductName());
        String email = contract.getPolicyholder().getEmail();
        String phone = contract.getPolicyholder().getPhone();

        boolean delivered = false;
        int attempt = 0;
        while (attempt < MAX_SEND_ATTEMPTS && !delivered) {
            attempt++;
            try {
                notificationSender.send(email, phone, message);
                delivered = true;
            } catch (RuntimeException e) {
                log.warn("미납 고지서 발송 실패(시도 {}/{}): contractId={}", attempt, MAX_SEND_ATTEMPTS, contract.getId());
            }
        }
        notice.markSent(delivered, attempt);

        if (!delivered) {
            // E1: 최종 실패 → 관리자 알림 + 기록
            log.error("미납 고지서 발송 최종 실패: contractId={} (관리자 확인 필요)", contract.getId());
        }
        if (notice.isTerminationWarning()) {
            // A1: 30일 초과 → 직원 별도 알림
            for (InsuranceEmployee emp : userRepository.findAllEmployees()) {
                notificationSender.send(emp.getEmail(), emp.getPhone(),
                        "[해지예고] 계약 " + contract.getId() + " 연체 30일 초과. 확인 바랍니다.");
            }
        }
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.service.NoticeServiceTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: 발송 재시도(E1) 테스트 추가**

`NoticeServiceTest.java`에 재시도 검증을 추가한다. `NotificationSender`를 실패하도록 바꿔야 하므로 `@MockitoBean`(Spring Boot 4) 또는 테스트용 스텁을 쓴다. 아래는 Mockito 사용 예이며, 프로젝트에 Mockito가 없으면 별도 `@TestConfiguration`에서 항상 예외를 던지는 `NotificationSender` 빈을 주입하는 방식으로 바꾼다.

먼저 기존 코드 패턴 확인: 프로젝트에 Mockito가 이미 있는지 `grep -r "org.mockito" backend/src/test || grep -r "mockito" backend/build.gradle*` 로 확인하고, 있으면 다음을 사용한다:
```java
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    NotificationSender notificationSender; // 클래스 필드로 선언(기존 @Autowired 제거 충돌 주의)

    @Test
    void 발송이_계속_실패하면_3회_시도_후_미발송으로_기록된다() {
        org.mockito.Mockito.doThrow(new RuntimeException("발송 실패"))
                .when(notificationSender).send(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        noticeService.issueOverdueNotices(LocalDate.now());

        Notice n = noticeRepository.findByContractId(contractId).get(0);
        assertThat(n.isDelivered()).isFalse();
        assertThat(n.getAttempts()).isEqualTo(3);
    }
```
주의: `@MockitoBean`을 쓰면 컨텍스트 전역에서 NotificationSender가 목으로 대체된다. 같은 테스트 클래스의 다른 테스트가 실제 발송(예외 없음)을 기대하면 기본 목은 아무것도 안 하므로(발송 성공으로 간주) 문제없다 — 단, `markSent(true, 1)` 단언이 있는 기존 테스트는 목이 예외를 안 던지면 그대로 성공한다. 충돌이 우려되면 이 재시도 테스트만 별도 클래스 `NoticeServiceRetryTest`로 분리한다.

Mockito가 없다면, `src/test/java/.../service/NoticeServiceRetryTest.java`를 새로 만들고 `@TestConfiguration`으로 항상 던지는 빈을 등록:
```java
@SpringBootTest
class NoticeServiceRetryTest {
    @TestConfiguration
    static class FailingSenderConfig {
        @Bean @Primary
        NotificationSender failingSender() {
            return (email, phone, message) -> { throw new RuntimeException("발송 실패"); };
        }
    }
    // ... setUp으로 연체 계약 만들고, issueOverdueNotices 호출 후
    //     notice.isDelivered()==false, attempts==3 단언
}
```
구현자는 둘 중 프로젝트에 맞는 방식을 택해 **실패하는 테스트 먼저 작성 → 통과 확인**한다.

- [ ] **Step 6: 재시도 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.service.NoticeService*"`
Expected: PASS.

- [ ] **Step 7: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/NoticeService.java backend/src/test/java/com/distribution/insurance/service/NoticeService*Test.java
git commit -m "feat(epic3-C): NoticeService 미납 탐색·발송·재시도·30일경고·중복방지 (UC16)"
```

---

## Task 4: @EnableScheduling + @Scheduled 래퍼

**Files:**
- Create: `SchedulingConfig.java`, `OverdueNoticeScheduler.java`

스케줄 시각 자체는 단위 테스트하지 않고(타이밍 의존), 빈 와이어링/컨텍스트 로딩으로 검증한다.

- [ ] **Step 1: SchedulingConfig 작성**

`SchedulingConfig.java`:
```java
package com.distribution.insurance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 활성화(UC16 일일 자동 실행). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

- [ ] **Step 2: OverdueNoticeScheduler 작성**

`OverdueNoticeScheduler.java`:
```java
package com.distribution.insurance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 매일 오전 9시 미납 고지서 발송(UC16 1단계). 로직은 NoticeService에 위임. */
@Component
public class OverdueNoticeScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueNoticeScheduler.class);

    private final NoticeService noticeService;

    public OverdueNoticeScheduler(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void run() {
        int created = noticeService.issueOverdueNotices(LocalDate.now());
        log.info("미납 고지서 발송 완료: {}건", created);
    }
}
```

- [ ] **Step 3: 컨텍스트 로딩/와이어링 확인**

기존 `ApplicationTests`(또는 `*ApplicationTests`)의 contextLoads 테스트가 있으면 그대로 전체 빌드로 검증한다. 없으면 생략 가능. 핵심은 컴파일+컨텍스트 기동.

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL(전체). 스케줄러 빈이 컨텍스트에 정상 등록되고 기존 테스트 회귀 없음.

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/config/SchedulingConfig.java backend/src/main/java/com/distribution/insurance/service/OverdueNoticeScheduler.java
git commit -m "feat(epic3-C): @Scheduled 일일 미납 고지서 스케줄러 (UC16)"
```

---

## Self-Review (작성자 체크 결과)

- **Spec 커버리지(이슈 C)**: 일일 스케줄(Task4)·미납 탐색(Task3, BillingCalculator 재사용)·Notice 생성(Task1)·발송+기록(Task3)·3회 재시도 E1(Task3 Step5)·30일 해지예고 A1(Task1 임계값 + Task3 직원알림)·중복 방지(Task1 repo + Task3) 모두 태스크 존재.
- **ADR 준수**: 미납 판정은 ADR 0004 `BillingCalculator`/`BillingStatus` 재사용(중복 계산 로직 없음). 회차 엔티티 미도입 유지.
- **타입 일관성**: `Notice.of(contract, BillingStatus, LocalDate)`, `buildMessage(String)`, `markSent(boolean,int)`, `NoticeRepository.existsByContractIdAndIssuedAt/findByContractId`, `UserRepository.findAllEmployees()`, `NoticeService.issueOverdueNotices(LocalDate)` 전 태스크 일치.
- **다이어그램 보강**: Notice에 발송기록 필드 추가 + send()를 서비스로 이동 — plan 상단에 명시(ADR 불요, 되돌리기 쉬움).
- **실행자 주의**: 도메인 생성자/JWT 시그니처는 plan 예시 — 각 테스트 작성 시 이슈 A/B 테스트와 도메인 파일로 실제값 확인. `TestFixtures`/`TestEntities` 헬퍼는 실제 생성자로 채울 것. 재시도 테스트는 프로젝트의 Mockito 유무를 먼저 확인해 `@MockitoBean` 또는 `@TestConfiguration` 실패 빈 중 택일.
- **범위 외**: 사용자 노출 API/컨트롤러 없음(UC16은 시스템 스케줄러). 계약 자동 해지(연체 누적 시 terminate)는 UC16 범위 아님 — 경고만, 실제 해지는 다루지 않는다.
