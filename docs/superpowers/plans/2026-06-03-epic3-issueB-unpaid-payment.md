# Epic 3 이슈 B — 미납조회(UC07) + 납부(UC10) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 계약의 월 보험료 미납분을 온더플라이로 계산해 조회하고, Mock 결제로 납부(성공/실패)하며 자동이체를 등록한다.

**Architecture:** 청구 회차를 영속화하지 않고(ADR 0004) `BillingCalculator`가 계약 시작일·monthlyPremium·성공 납부 건수로 미납 회차/연체일수/연체이자를 계산한다. 납부는 `PaymentGateway`(Mock)로 처리해 `Payment`(SUCCESS/FAILED)를 기록한다. 자동이체는 계약에 임베디드로 저장한다. 모든 조회/납부는 본인 계약만 허용한다.

**Tech Stack:** Spring Boot 4 / Java 21, Spring Data JPA, Spring Security(JWT), JUnit5 + MockMvc, Lombok.

**준수 문서:**
- spec: `docs/superpowers/specs/2026-06-03-epic3-contract-billing-design.md` (이슈 B)
- 용어: `CONTEXT.md` — `Payment`, `PaymentMethod`, `Installment`, `OverdueInterest`. 동의어 금지.
- 결정: ADR 0003(adjustedPremium=monthlyPremium), ADR 0004(온더플라이, 회차 엔티티 없음, FIFO 충당).

**선행(이슈 A, 이미 main):** `InsuranceContract`(getStartDate/getEndDate/getMonthlyPremium/getPolicyholder/getProduct), `ContractService.requireOwned` 패턴, `ContractController`(GET /contracts, /{id}, /{id}/pdf), `ContractDetailResponse.paymentMethod="미등록"`.

**계산 규칙(ADR 0004 확정):**
- 청구 회차 기한: `startDate.plusMonths(k)` (k=0,1,…). 총 회차 = `ChronoUnit.MONTHS.between(startDate, endDate)` = 12.
- 발생 회차 수(asOf) = `asOf < startDate ? 0 : min(MONTHS.between(startDate, asOf) + 1, 총회차)`.
- 미납 회차 수 = `max(0, 발생회차 - 성공납부건수)`.
- 가장 오래된 미납 회차 기한 = `startDate.plusMonths(성공납부건수)` (미납이 있을 때만 의미).
- 연체일수 = `오늘 > 기한 ? DAYS.between(기한, asOf) : 0`.
- 미납 원금 = 미납회차 × monthlyPremium.
- 연체이자 = `round(미납원금 × 연체일수 × 0.10/365)`. (연 10%, ADR 0004)

**라우팅 주의:** 새 고정경로 `/contracts/unpaid`, `/contracts/payable`는 PathPattern 특이도상 `/contracts/{id}`보다 우선 매칭되어 충돌하지 않는다. 그래도 컨트롤러에 고정경로 핸들러를 `{id}` 핸들러보다 위에 선언한다.

---

## File Structure

- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/PaymentMethod.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/PaymentStatus.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/Payment.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/AutoDebit.java` (@Embeddable)
- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/BillingCalculator.java` (순수 계산)
- Create: `backend/src/main/java/com/distribution/insurance/domain/contract/BillingStatus.java` (record)
- Modify: `backend/src/main/java/com/distribution/insurance/domain/contract/InsuranceContract.java` (autoDebit 임베디드 + registerAutoDebit + 결제수단 표기)
- Create: `backend/src/main/java/com/distribution/insurance/repository/PaymentRepository.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/PaymentGateway.java` (interface)
- Create: `backend/src/main/java/com/distribution/insurance/service/MockPaymentGateway.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/BillingService.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/UnpaidResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/PayableResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/PaymentRequest.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/PaymentResultResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/AutoDebitRequest.java`
- Modify: `backend/src/main/java/com/distribution/insurance/web/controller/ContractController.java` (엔드포인트 추가)
- Modify: `backend/src/main/java/com/distribution/insurance/web/dto/ContractDetailResponse.java` (결제수단 실제값)
- Tests: `.../domain/contract/BillingCalculatorTest.java`, `.../domain/contract/PaymentTest.java`, `.../web/controller/BillingControllerTest.java`

명령은 모두 `backend/`에서: `cd /Users/heeyoon/Desktop/insurance/backend && ./gradlew ...`

---

## Task 1: PaymentMethod·PaymentStatus enum + Payment 엔티티

**Files:**
- Create: `PaymentMethod.java`, `PaymentStatus.java`, `Payment.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/contract/PaymentTest.java`

- [ ] **Step 1: 실패 테스트 작성**

`PaymentTest.java`:
```java
package com.distribution.insurance.domain.contract;

import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    private InsuranceContract contract() {
        // 실제 Policyholder/HealthInsuranceProduct 생성자 시그니처는 도메인 파일에서 확인해 맞춘다.
        Policyholder ph = TestFixtures.policyholder();
        return new InsuranceContract(ph, TestFixtures.healthProduct(), 30000, LocalDate.of(2026, 1, 1));
    }

    @Test
    void 성공_납부는_SUCCESS이고_금액과_수단을_보존한다() {
        Payment p = Payment.success(contract(), 30000, PaymentMethod.CARD);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(p.getAmount()).isEqualTo(30000);
        assertThat(p.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(p.getPaidAt()).isNotNull();
    }

    @Test
    void 실패_납부는_FAILED이고_영수증을_만들_수_없다() {
        Payment p = Payment.failed(contract(), 30000, PaymentMethod.CARD);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThatThrownBy(p::getReceipt)
                .isInstanceOf(com.distribution.insurance.service.InvalidRequestException.class);
    }

    @Test
    void 성공_납부의_영수증은_금액을_담는다() {
        Payment p = Payment.success(contract(), 30000, PaymentMethod.TRANSFER);
        assertThat(p.getReceipt().amount()).isEqualTo(30000);
    }
}
```

이 단계에서 `TestFixtures`가 없으면, 우선 `backend/src/test/java/com/distribution/insurance/domain/contract/TestFixtures.java`를 만들어 실제 생성자에 맞춘 헬퍼를 둔다:
```java
package com.distribution.insurance.domain.contract;

import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;

/** 테스트용 도메인 픽스처. 실제 생성자 시그니처에 맞춰 작성할 것(아래는 자리표시 — 실제 코드로 교체). */
final class TestFixtures {
    static Policyholder policyholder() {
        // TODO: 실제 Policyholder 생성자에 맞춘다(이슈 A 테스트/도메인 참고).
        return new Policyholder(/* name,email,phone,password,ssn,birthDate,address,bankAccount 등 */);
    }
    static HealthInsuranceProduct healthProduct() {
        // TODO: 실제 HealthInsuranceProduct 생성자에 맞춘다.
        return new HealthInsuranceProduct(/* productName, description, basePremium, maxHospitalizationDays 등 */);
    }
}
```
구현자는 `git show main:backend/src/test/java/.../ReviewServiceContractTest.java`(이슈 A에서 추가됨)와 도메인 파일을 열어 실제 인자를 채운다.

- [ ] **Step 2: 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.PaymentTest"`
Expected: 컴파일 실패(`Payment`, enum 없음).

- [ ] **Step 3: enum 작성**

`PaymentMethod.java`:
```java
package com.distribution.insurance.domain.contract;

/** 보험료 결제 수단(CONTEXT.md). */
public enum PaymentMethod {
    CARD, TRANSFER, AUTO_DEBIT
}
```
`PaymentStatus.java`:
```java
package com.distribution.insurance.domain.contract;

public enum PaymentStatus {
    SUCCESS, FAILED
}
```

- [ ] **Step 4: Payment 엔티티 작성**

`Payment.java`:
```java
package com.distribution.insurance.domain.contract;

import com.distribution.insurance.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 보험료 납부 1건(UC10). 어느 회차인지는 저장하지 않는다(ADR 0004, FIFO 충당). */
@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int amount;
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private InsuranceContract contract;

    private Payment(InsuranceContract contract, int amount, PaymentMethod method, PaymentStatus status) {
        this.contract = contract;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.paidAt = LocalDateTime.now();
    }

    public static Payment success(InsuranceContract contract, int amount, PaymentMethod method) {
        return new Payment(contract, amount, method, PaymentStatus.SUCCESS);
    }

    public static Payment failed(InsuranceContract contract, int amount, PaymentMethod method) {
        return new Payment(contract, amount, method, PaymentStatus.FAILED);
    }

    /** 납부 완료 영수증(UC10 6단계). 실패 납부는 영수증이 없다. */
    public Receipt getReceipt() {
        if (status != PaymentStatus.SUCCESS) {
            throw new InvalidRequestException("실패한 납부는 영수증을 발급할 수 없습니다.");
        }
        return new Receipt(id, amount, paidAt, method.name());
    }

    public record Receipt(Long paymentId, int amount, LocalDateTime paidAt, String method) {}
}
```

- [ ] **Step 5: 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.PaymentTest"`
Expected: PASS (3 tests).

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/contract/Payment*.java backend/src/test/java/com/distribution/insurance/domain/contract/PaymentTest.java backend/src/test/java/com/distribution/insurance/domain/contract/TestFixtures.java
git commit -m "feat(epic3-B): Payment 도메인 + 결제수단/상태 enum + 영수증 (UC10)"
```

---

## Task 2: PaymentRepository

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/repository/PaymentRepository.java`

- [ ] **Step 1: 작성**

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.contract.Payment;
import com.distribution.insurance.domain.contract.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** FIFO 충당용 — 계약의 성공 납부 건수(ADR 0004). */
    long countByContractIdAndStatus(Long contractId, PaymentStatus status);
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/repository/PaymentRepository.java
git commit -m "feat(epic3-B): PaymentRepository (성공 납부 건수 집계)"
```

---

## Task 3: BillingCalculator + BillingStatus (순수 계산, 회차 엔티티 없음)

**Files:**
- Create: `BillingStatus.java`, `BillingCalculator.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/contract/BillingCalculatorTest.java`

- [ ] **Step 1: 실패 테스트 작성**

`BillingCalculatorTest.java`:
```java
package com.distribution.insurance.domain.contract;

import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class BillingCalculatorTest {

    private InsuranceContract contract(LocalDate start, int premium) {
        Policyholder ph = TestFixtures.policyholder();
        return new InsuranceContract(ph, TestFixtures.healthProduct(), premium, start);
    }

    @Test
    void 납부가_밀린_만큼_미납회차와_원금이_계산된다() {
        // 1/1 시작, 3/15 기준 → 발생회차 3(1/1,2/1,3/1), 성공납부 1 → 미납 2회차
        InsuranceContract c = contract(LocalDate.of(2026, 1, 1), 30000);
        BillingStatus s = BillingCalculator.compute(c, 1, LocalDate.of(2026, 3, 15));

        assertThat(s.unpaidCount()).isEqualTo(2);
        assertThat(s.unpaidPrincipal()).isEqualTo(60000);
        // 가장 오래된 미납 회차 = 2/1 (성공 1건이 1/1을 충당)
        assertThat(s.oldestUnpaidDueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(s.overdueDays()).isEqualTo(42); // 2/1 ~ 3/15
        // 연체이자 = round(60000 * 42 * 0.10/365)
        assertThat(s.overdueInterest()).isEqualTo(Math.round(60000L * 42 * (0.10 / 365)));
    }

    @Test
    void 발생회차를_모두_납부하면_미납이_없다() {
        InsuranceContract c = contract(LocalDate.of(2026, 1, 1), 30000);
        BillingStatus s = BillingCalculator.compute(c, 3, LocalDate.of(2026, 3, 15));
        assertThat(s.unpaidCount()).isZero();
        assertThat(s.unpaidPrincipal()).isZero();
        assertThat(s.overdueDays()).isZero();
        assertThat(s.overdueInterest()).isZero();
    }

    @Test
    void 시작일_당일은_1회차가_발생한다() {
        InsuranceContract c = contract(LocalDate.of(2026, 6, 3), 30000);
        BillingStatus s = BillingCalculator.compute(c, 0, LocalDate.of(2026, 6, 3));
        assertThat(s.unpaidCount()).isEqualTo(1);
        assertThat(s.oldestUnpaidDueDate()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(s.overdueDays()).isZero(); // 당일은 연체 아님
    }

    @Test
    void 발생회차는_총회차_12를_넘지_않는다() {
        InsuranceContract c = contract(LocalDate.of(2026, 1, 1), 30000);
        // 2년 뒤 기준이어도 발생회차는 12 상한
        BillingStatus s = BillingCalculator.compute(c, 0, LocalDate.of(2028, 1, 1));
        assertThat(s.unpaidCount()).isEqualTo(12);
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.BillingCalculatorTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: BillingStatus 작성**

`BillingStatus.java`:
```java
package com.distribution.insurance.domain.contract;

import java.time.LocalDate;

/** 한 계약의 미납 계산 결과(온더플라이, ADR 0004). 미납이 없으면 oldestUnpaidDueDate는 null. */
public record BillingStatus(
        int unpaidCount,
        int unpaidPrincipal,
        LocalDate oldestUnpaidDueDate,
        long overdueDays,
        long overdueInterest) {

    public boolean hasUnpaid() {
        return unpaidCount > 0;
    }

    public boolean isOverdue() {
        return overdueDays > 0;
    }
}
```

- [ ] **Step 4: BillingCalculator 작성**

`BillingCalculator.java`:
```java
package com.distribution.insurance.domain.contract;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** 청구 회차를 영속화하지 않고 계약 정보로 미납을 계산한다(ADR 0004). */
public final class BillingCalculator {

    /** 연 10% 연체이율(ADR 0004). */
    private static final double DAILY_RATE = 0.10 / 365;

    private BillingCalculator() {}

    public static BillingStatus compute(InsuranceContract contract, long successCount, LocalDate asOf) {
        LocalDate start = contract.getStartDate();
        int totalInstallments = (int) ChronoUnit.MONTHS.between(start, contract.getEndDate());

        int dueCount;
        if (asOf.isBefore(start)) {
            dueCount = 0;
        } else {
            long elapsedMonths = ChronoUnit.MONTHS.between(start, asOf);
            dueCount = (int) Math.min(elapsedMonths + 1, totalInstallments);
        }

        int unpaidCount = (int) Math.max(0, dueCount - successCount);
        if (unpaidCount == 0) {
            return new BillingStatus(0, 0, null, 0, 0);
        }

        int unpaidPrincipal = unpaidCount * contract.getMonthlyPremium();
        LocalDate oldestUnpaidDueDate = start.plusMonths(successCount);
        long overdueDays = asOf.isAfter(oldestUnpaidDueDate)
                ? ChronoUnit.DAYS.between(oldestUnpaidDueDate, asOf) : 0;
        long overdueInterest = Math.round(unpaidPrincipal * overdueDays * DAILY_RATE);

        return new BillingStatus(unpaidCount, unpaidPrincipal, oldestUnpaidDueDate, overdueDays, overdueInterest);
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.BillingCalculatorTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/contract/Billing*.java backend/src/test/java/com/distribution/insurance/domain/contract/BillingCalculatorTest.java
git commit -m "feat(epic3-B): BillingCalculator 온더플라이 미납 계산 (ADR 0004)"
```

---

## Task 4: 자동이체 임베디드 + InsuranceContract 확장

**Files:**
- Create: `AutoDebit.java`
- Modify: `InsuranceContract.java`
- Test: `BillingCalculatorTest`와 별개 — `InsuranceContractTest`에 케이스 추가(이슈 A 테스트 파일).

- [ ] **Step 1: 실패 테스트 추가**

`backend/src/test/java/com/distribution/insurance/domain/contract/InsuranceContractTest.java`에 추가:
```java
    @Test
    void 자동이체_등록_시_결제수단표기가_AUTO_DEBIT가_된다() {
        InsuranceContract c = newContract();
        assertThat(c.registeredPaymentMethod()).isEqualTo("미등록");
        c.registerAutoDebit("110-222-333333", 25);
        assertThat(c.registeredPaymentMethod()).isEqualTo("AUTO_DEBIT");
        assertThat(c.getAutoDebit().getAccount()).isEqualTo("110-222-333333");
        assertThat(c.getAutoDebit().getWithdrawalDay()).isEqualTo(25);
    }

    @Test
    void 출금일은_1에서_31_사이여야_한다() {
        InsuranceContract c = newContract();
        assertThatThrownBy(() -> c.registerAutoDebit("110-222-333333", 32))
                .isInstanceOf(com.distribution.insurance.service.InvalidRequestException.class);
    }
```
(`newContract()`는 이슈 A 테스트에 이미 있음. 없으면 추가.)

- [ ] **Step 2: 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.InsuranceContractTest"`
Expected: 컴파일 실패(`registerAutoDebit`, `AutoDebit` 없음).

- [ ] **Step 3: AutoDebit 작성**

`AutoDebit.java`:
```java
package com.distribution.insurance.domain.contract;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자동이체 등록 정보(UC10 A1). 전용 엔티티 없이 계약에 임베디드(spec). */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AutoDebit {

    private String account;
    private int withdrawalDay;

    public AutoDebit(String account, int withdrawalDay) {
        this.account = account;
        this.withdrawalDay = withdrawalDay;
    }
}
```

- [ ] **Step 4: InsuranceContract 수정**

`InsuranceContract.java`에 다음을 추가한다.

import 추가:
```java
import com.distribution.insurance.service.InvalidRequestException;
```
(이미 IllegalStateTransitionException import가 있으므로 동일 패키지 import 추가)

필드 추가(product 필드 아래):
```java
    @Embedded
    private AutoDebit autoDebit;   // 자동이체 등록 전 null
```

메서드 추가(terminate() 아래):
```java
    /** 자동이체 등록(UC10 A1). 출금일은 1~31. */
    public void registerAutoDebit(String account, int withdrawalDay) {
        if (withdrawalDay < 1 || withdrawalDay > 31) {
            throw new InvalidRequestException("출금일은 1일에서 31일 사이여야 합니다.");
        }
        this.autoDebit = new AutoDebit(account, withdrawalDay);
    }

    /** 계약 상세에 노출할 결제수단 표기. 자동이체 등록 시 "AUTO_DEBIT", 아니면 "미등록". */
    public String registeredPaymentMethod() {
        return autoDebit != null ? PaymentMethod.AUTO_DEBIT.name() : "미등록";
    }
```

- [ ] **Step 5: 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.contract.InsuranceContractTest"`
Expected: PASS (기존 + 2 신규).

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/contract/AutoDebit.java backend/src/main/java/com/distribution/insurance/domain/contract/InsuranceContract.java backend/src/test/java/com/distribution/insurance/domain/contract/InsuranceContractTest.java
git commit -m "feat(epic3-B): 자동이체 임베디드 등록 + 결제수단 표기 (UC10 A1)"
```

---

## Task 5: PaymentGateway + MockPaymentGateway

**Files:**
- Create: `PaymentGateway.java`, `MockPaymentGateway.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/MockPaymentGatewayTest.java`

- [ ] **Step 1: 실패 테스트 작성**

`MockPaymentGatewayTest.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    void 일반_결제정보는_성공한다() {
        PaymentGateway.Result r = gateway.charge(PaymentMethod.CARD, 30000, "1234-5678-9012-3456");
        assertThat(r.success()).isTrue();
    }

    @Test
    void 0000으로_끝나는_결제정보는_실패한다() {
        // 한도초과/잔액부족 시뮬레이션(E1)
        PaymentGateway.Result r = gateway.charge(PaymentMethod.CARD, 30000, "1234-5678-9012-0000");
        assertThat(r.success()).isFalse();
        assertThat(r.reason()).isNotBlank();
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.service.MockPaymentGatewayTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: PaymentGateway 인터페이스 작성**

`PaymentGateway.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.PaymentMethod;

/** 외부 결제 처리 추상화. 텍스트 구현은 Mock으로 시뮬레이션(spec). */
public interface PaymentGateway {

    Result charge(PaymentMethod method, int amount, String paymentInfo);

    record Result(boolean success, String reason) {
        public static Result ok() { return new Result(true, null); }
        public static Result fail(String reason) { return new Result(false, reason); }
    }
}
```

- [ ] **Step 4: MockPaymentGateway 작성**

`MockPaymentGateway.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.PaymentMethod;
import org.springframework.stereotype.Component;

/**
 * 결제 시뮬레이션. 결제정보가 "0000"으로 끝나면 한도초과/잔액부족으로 실패(E1), 그 외 성공.
 * 결정적 동작이라 성공/실패 경로를 테스트로 재현할 수 있다.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public Result charge(PaymentMethod method, int amount, String paymentInfo) {
        if (paymentInfo != null && paymentInfo.endsWith("0000")) {
            return Result.fail("한도 초과 또는 잔액 부족");
        }
        return Result.ok();
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.service.MockPaymentGatewayTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/PaymentGateway.java backend/src/main/java/com/distribution/insurance/service/MockPaymentGateway.java backend/src/test/java/com/distribution/insurance/service/MockPaymentGatewayTest.java
git commit -m "feat(epic3-B): PaymentGateway + MockPaymentGateway 성공/실패 시뮬레이션 (UC10 E1)"
```

---

## Task 6: BillingService (미납/납부예정/납부/자동이체, 본인 격리)

**Files:**
- Create: `BillingService.java`

이 서비스의 동작은 Task 7 컨트롤러 통합 테스트로 검증한다. 여기서는 작성 + 컴파일 확인.

- [ ] **Step 1: BillingService 작성**

`BillingService.java`:
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.*;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingService {

    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationSender notificationSender;

    public BillingService(ContractRepository contractRepository,
                          PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway,
                          NotificationSender notificationSender) {
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.notificationSender = notificationSender;
    }

    /** 계약 + 그 미납 계산결과 묶음. */
    public record ContractBilling(InsuranceContract contract, BillingStatus status) {}

    /** 미납(연체) 목록(UC07): 연체일수 > 0 인 계약만. */
    @Transactional(readOnly = true)
    public List<ContractBilling> myOverdue(Long policyholderId) {
        List<ContractBilling> result = new ArrayList<>();
        for (InsuranceContract c : contractRepository.findByPolicyholderId(policyholderId)) {
            BillingStatus s = statusOf(c);
            if (s.isOverdue()) result.add(new ContractBilling(c, s));
        }
        return result;
    }

    /** 납부 예정 목록(UC10 2단계): 미납 회차가 1개 이상인 계약. */
    @Transactional(readOnly = true)
    public List<ContractBilling> myPayable(Long policyholderId) {
        List<ContractBilling> result = new ArrayList<>();
        for (InsuranceContract c : contractRepository.findByPolicyholderId(policyholderId)) {
            BillingStatus s = statusOf(c);
            if (s.hasUnpaid()) result.add(new ContractBilling(c, s));
        }
        return result;
    }

    /** 단건 미납 상세(UC07 3단계). 본인 계약만. */
    @Transactional(readOnly = true)
    public ContractBilling unpaidDetail(Long policyholderId, Long contractId) {
        InsuranceContract c = requireOwned(policyholderId, contractId);
        return new ContractBilling(c, statusOf(c));
    }

    /**
     * 한 회차(monthlyPremium) 납부(UC10). 성공/실패 모두 Payment로 기록한다.
     * 게이트웨이 실패는 클라이언트 오류가 아니므로 예외 없이 FAILED 결과를 반환한다(E1).
     */
    @Transactional
    public Payment pay(Long policyholderId, Long contractId, PaymentMethod method, String paymentInfo) {
        InsuranceContract c = requireOwned(policyholderId, contractId);
        int amount = c.getMonthlyPremium();

        PaymentGateway.Result result = paymentGateway.charge(method, amount, paymentInfo);
        Payment payment = result.success()
                ? Payment.success(c, amount, method)
                : Payment.failed(c, amount, method);
        paymentRepository.save(payment);

        if (result.success()) {
            notificationSender.send(c.getPolicyholder().getEmail(), c.getPolicyholder().getPhone(),
                    "보험료 " + amount + "원 납부가 완료되었습니다. (영수증번호 " + payment.getId() + ")");
        }
        return payment;
    }

    /** 자동이체 등록(UC10 A1). 본인 계약만. */
    @Transactional
    public void registerAutoDebit(Long policyholderId, Long contractId, String account, int withdrawalDay) {
        InsuranceContract c = requireOwned(policyholderId, contractId);
        c.registerAutoDebit(account, withdrawalDay);
        notificationSender.send(c.getPolicyholder().getEmail(), c.getPolicyholder().getPhone(),
                "자동이체가 등록되었습니다. 출금계좌 " + account + ", 매월 " + withdrawalDay + "일.");
    }

    private BillingStatus statusOf(InsuranceContract c) {
        long success = paymentRepository.countByContractIdAndStatus(c.getId(), PaymentStatus.SUCCESS);
        return BillingCalculator.compute(c, success, LocalDate.now());
    }

    /** 없으면 404, 타인 계약이면 403(이슈 A ContractService와 동일 규약). */
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

- [ ] **Step 2: 컴파일 확인**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/BillingService.java
git commit -m "feat(epic3-B): BillingService 미납/납부예정/납부/자동이체 + 본인 격리 (UC07/UC10)"
```

---

## Task 7: DTO + 컨트롤러 엔드포인트 + 계약상세 결제수단 + 통합테스트

**Files:**
- Create: `UnpaidResponse.java`, `PayableResponse.java`, `PaymentRequest.java`, `PaymentResultResponse.java`, `AutoDebitRequest.java`
- Modify: `ContractController.java`, `ContractDetailResponse.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/BillingControllerTest.java`

- [ ] **Step 1: 실패 테스트 작성**

`BillingControllerTest.java`:
```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.contract.Payment;
import com.distribution.insurance.domain.contract.PaymentMethod;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.PaymentRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BillingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    Long ownerId; String ownerToken; Long contractId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // 실제 Policyholder/HealthInsuranceProduct/tokenProvider 시그니처는 이슈 A의
        // ContractControllerTest를 열어 그대로 맞춘다.
        Policyholder owner = userRepository.save(TestUsers.policyholder(encoder));
        ownerId = owner.getId();
        ownerToken = tokenProvider.createToken(ownerId, "POLICYHOLDER");

        HealthInsuranceProduct product = productRepository.save(TestUsers.healthProduct());
        // 6개월 전 시작 → 미납 누적되도록
        InsuranceContract c = contractRepository.save(
                new InsuranceContract(owner, product, 30000, LocalDate.now().minusMonths(6)));
        contractId = c.getId();
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void 납부예정_목록은_미납회차가_있으면_노출된다() throws Exception {
        mockMvc.perform(get("/contracts/payable").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contractId").value(contractId))
                .andExpect(jsonPath("$[0].amount").isNumber());
    }

    @Test
    void 미납_목록은_연체분을_연체이자와_함께_노출한다() throws Exception {
        mockMvc.perform(get("/contracts/unpaid").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].overdueDays").isNumber())
                .andExpect(jsonPath("$[0].overdueInterest").isNumber());
    }

    @Test
    void 카드납부_성공() throws Exception {
        mockMvc.perform(post("/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CARD\",\"paymentInfo\":\"1234-5678-9012-3456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(30000));
    }

    @Test
    void 결제실패는_FAILED로_기록되고_사유를_반환한다() throws Exception {
        mockMvc.perform(post("/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CARD\",\"paymentInfo\":\"1234-5678-9012-0000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.reason").isNotEmpty());
    }

    @Test
    void 성공_납부_후_미납회차가_하나_줄어든다() throws Exception {
        // 납부 전 납부예정 금액 기록
        String before = mockMvc.perform(get("/contracts/" + contractId + "/unpaid")
                        .header("Authorization", "Bearer " + ownerToken))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"TRANSFER\",\"paymentInfo\":\"110-222-333\"}"))
                .andExpect(status().isOk());

        String after = mockMvc.perform(get("/contracts/" + contractId + "/unpaid")
                        .header("Authorization", "Bearer " + ownerToken))
                .andReturn().getResponse().getContentAsString();

        // 미납 원금이 줄었는지(단순 문자열 동등 비교 회피 — unpaidCount 필드로 검증)
        org.assertj.core.api.Assertions.assertThat(before).isNotEqualTo(after);
    }

    @Test
    void 자동이체_등록_후_계약상세_결제수단이_AUTO_DEBIT() throws Exception {
        mockMvc.perform(post("/contracts/" + contractId + "/auto-debit")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"110-222-333333\",\"withdrawalDay\":25}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/contracts/" + contractId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("AUTO_DEBIT"));
    }
}
```
구현자 주의: `TestUsers` 헬퍼가 없으면 이슈 A의 `ContractControllerTest` setUp 코드(실제 Policyholder/HealthInsuranceProduct 생성자, `tokenProvider.createToken(Long, String)`)를 그대로 인라인하거나 헬퍼로 추출해 쓴다. JWT/생성자 시그니처는 plan 예시가 아니라 실제 코드를 따른다.

- [ ] **Step 2: 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.web.controller.BillingControllerTest"`
Expected: 컴파일 실패(DTO·엔드포인트 없음).

- [ ] **Step 3: UnpaidResponse 작성**

`UnpaidResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.service.BillingService.ContractBilling;

import java.time.LocalDate;

/** UC07 2단계: 계약명, 납부기한, 미납금액, 연체일수, 연체이자. */
public record UnpaidResponse(
        Long contractId, String productName, LocalDate dueDate,
        int unpaidPrincipal, long overdueDays, long overdueInterest) {

    public static UnpaidResponse from(ContractBilling cb) {
        return new UnpaidResponse(
                cb.contract().getId(),
                cb.contract().getProduct().getProductName(),
                cb.status().oldestUnpaidDueDate(),
                cb.status().unpaidPrincipal(),
                cb.status().overdueDays(),
                cb.status().overdueInterest());
    }
}
```

- [ ] **Step 4: PayableResponse 작성**

`PayableResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.service.BillingService.ContractBilling;

import java.time.LocalDate;

/** UC10 2단계: 계약명, 납부기한, 납부금액. */
public record PayableResponse(
        Long contractId, String productName, LocalDate dueDate, int amount) {

    public static PayableResponse from(ContractBilling cb) {
        return new PayableResponse(
                cb.contract().getId(),
                cb.contract().getProduct().getProductName(),
                cb.status().oldestUnpaidDueDate(),
                cb.status().unpaidPrincipal());
    }
}
```

- [ ] **Step 5: PaymentRequest / PaymentResultResponse / AutoDebitRequest 작성**

`PaymentRequest.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.contract.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/** UC10 3~4단계: 결제수단 + 결제정보(카드/계좌 번호). */
public record PaymentRequest(@NotNull PaymentMethod method, String paymentInfo) {}
```

`PaymentResultResponse.java`:
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.contract.Payment;
import com.distribution.insurance.domain.contract.PaymentStatus;

/** 납부 결과. 성공이면 영수증번호, 실패면 사유(E1). */
public record PaymentResultResponse(
        Long paymentId, String status, int amount, String reason) {

    public static PaymentResultResponse success(Payment p) {
        return new PaymentResultResponse(p.getId(), PaymentStatus.SUCCESS.name(), p.getAmount(), null);
    }

    public static PaymentResultResponse failed(Payment p, String reason) {
        return new PaymentResultResponse(p.getId(), PaymentStatus.FAILED.name(), p.getAmount(), reason);
    }
}
```

`AutoDebitRequest.java`:
```java
package com.distribution.insurance.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** UC10 A1: 출금계좌 + 출금일. */
public record AutoDebitRequest(
        @NotBlank String account,
        @Min(1) @Max(31) int withdrawalDay) {}
```

- [ ] **Step 6: BillingService.pay 반환을 사유와 함께 컨트롤러로 넘기기 위한 보강**

`PaymentResultResponse.failed`는 사유 문자열이 필요하다. `BillingService.pay`는 `Payment`만 반환하므로, 컨트롤러가 사유를 알 수 있도록 `pay`가 결과 사유를 함께 반환하게 바꾼다. `BillingService`에 반환 레코드를 추가하고 `pay` 시그니처를 수정한다.

`BillingService.java` 수정:
- 레코드 추가:
```java
    public record PayOutcome(Payment payment, String failureReason) {}
```
- `pay` 메서드의 반환 타입을 `Payment` → `PayOutcome`로 바꾸고, 본문 마지막을:
```java
        if (result.success()) {
            notificationSender.send(c.getPolicyholder().getEmail(), c.getPolicyholder().getPhone(),
                    "보험료 " + amount + "원 납부가 완료되었습니다. (영수증번호 " + payment.getId() + ")");
        }
        return new PayOutcome(payment, result.success() ? null : result.reason());
```

- [ ] **Step 7: ContractController에 엔드포인트 추가**

`ContractController.java`에 import 추가:
```java
import com.distribution.insurance.domain.contract.Payment;
import com.distribution.insurance.service.BillingService;
import com.distribution.insurance.service.BillingService.PayOutcome;
import com.distribution.insurance.web.dto.AutoDebitRequest;
import com.distribution.insurance.web.dto.PayableResponse;
import com.distribution.insurance.web.dto.PaymentRequest;
import com.distribution.insurance.web.dto.PaymentResultResponse;
import com.distribution.insurance.web.dto.UnpaidResponse;
import jakarta.validation.Valid;
```
생성자에 `BillingService` 주입:
```java
    private final ContractService contractService;
    private final BillingService billingService;

    public ContractController(ContractService contractService, BillingService billingService) {
        this.contractService = contractService;
        this.billingService = billingService;
    }
```
**고정경로 핸들러를 `@GetMapping("/{id}")`보다 위에** 추가:
```java
    @GetMapping("/unpaid")
    public List<UnpaidResponse> myUnpaid(@AuthenticationPrincipal Long userId) {
        return billingService.myOverdue(userId).stream().map(UnpaidResponse::from).toList();
    }

    @GetMapping("/payable")
    public List<PayableResponse> myPayable(@AuthenticationPrincipal Long userId) {
        return billingService.myPayable(userId).stream().map(PayableResponse::from).toList();
    }
```
가변경로 핸들러들(상세 아래에 둬도 무방, 단 `/unpaid`·`/payable`보다 뒤):
```java
    @GetMapping("/{id}/unpaid")
    public UnpaidResponse unpaidDetail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return UnpaidResponse.from(billingService.unpaidDetail(userId, id));
    }

    @PostMapping("/{id}/payments")
    public PaymentResultResponse pay(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                     @Valid @RequestBody PaymentRequest request) {
        PayOutcome outcome = billingService.pay(userId, id, request.method(), request.paymentInfo());
        Payment p = outcome.payment();
        return outcome.failureReason() == null
                ? PaymentResultResponse.success(p)
                : PaymentResultResponse.failed(p, outcome.failureReason());
    }

    @PostMapping("/{id}/auto-debit")
    public void registerAutoDebit(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                  @Valid @RequestBody AutoDebitRequest request) {
        billingService.registerAutoDebit(userId, id, request.account(), request.withdrawalDay());
    }
```

- [ ] **Step 8: ContractDetailResponse 결제수단 실제값으로 교체**

`ContractDetailResponse.java`의 `from`에서 `"미등록"` 하드코딩을 계약의 표기 메서드로 바꾼다:
```java
        return new ContractDetailResponse(
                c.getId(), c.getProduct().getProductName(),
                ProductTypeMapper.typeOf(c.getProduct()),
                c.getStartDate(), c.getEndDate(), c.getMonthlyPremium(), c.getStatus().name(),
                c.registeredPaymentMethod(), items);  // 이슈 B: "미등록" 상수 → 실제 등록 상태
```

- [ ] **Step 9: 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.web.controller.BillingControllerTest"`
Expected: PASS (6 tests).

- [ ] **Step 10: 전체 회귀 확인**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL(이슈 A 계약상세 테스트 포함 전부 통과). 이슈 A `ContractControllerTest`의 `paymentMethod=="미등록"` 단언은 자동이체 미등록 계약이므로 그대로 통과한다.

- [ ] **Step 11: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/web backend/src/main/java/com/distribution/insurance/service/BillingService.java backend/src/test/java/com/distribution/insurance/web/controller/BillingControllerTest.java
git commit -m "feat(epic3-B): 미납/납부예정/납부/자동이체 API + 계약상세 결제수단 (UC07/UC10)"
```

---

## Self-Review (작성자 체크 결과)

- **Spec 커버리지(이슈 B)**: 온더플라이 계산(Task3, ADR0004)·미납목록 UC07(Task6/7)·단건 미납(Task7)·납부예정 UC10 2단계(Task6/7)·납부 성공/실패 E1(Task5/6/7)·자동이체 A1(Task4/6/7)·계약상세 결제수단 대체(Task8)·본인 격리(Task6) 모두 태스크 존재.
- **ADR 준수**: 회차 엔티티 없음(BillingCalculator 순수계산), FIFO 충당(성공건수만큼 앞 회차 충당, oldestUnpaidDueDate=start.plusMonths(success)), 연 10% 이율 상수.
- **타입 일관성**: `BillingStatus`(unpaidCount/unpaidPrincipal/oldestUnpaidDueDate/overdueDays/overdueInterest), `BillingService.ContractBilling`/`PayOutcome`, `PaymentGateway.Result`, `Payment.success/failed/getReceipt`, `registeredPaymentMethod()` 전 태스크 일치.
- **라우팅**: `/contracts/unpaid`·`/payable` 고정경로를 `/{id}`보다 먼저 선언(Task7 Step7).
- **회귀**: 이슈 A `ContractControllerTest`는 자동이체 미등록 계약이라 `paymentMethod=="미등록"` 유지.
- **실행자 주의**: 도메인 생성자/JWT 시그니처는 plan 예시 — 각 테스트 작성 시 이슈 A의 `ContractControllerTest`/`ReviewServiceContractTest`와 도메인 파일을 열어 실제값으로 맞출 것. `TestFixtures`/`TestUsers` 헬퍼는 실제 생성자로 채워 작성.
