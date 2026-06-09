package com.distribution.insurance.contract.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class BillingCalculatorTest {

    private InsuranceContract contract(LocalDate start, int premium) {
        return new InsuranceContract(TestFixtures.policyholder(), TestFixtures.healthProduct(), premium, start);
    }

    @Test
    void 납부가_밀린_만큼_미납회차와_원금이_계산된다() {
        InsuranceContract c = contract(LocalDate.of(2026, 1, 1), 30000);
        BillingStatus s = BillingCalculator.compute(c, 1, LocalDate.of(2026, 3, 15));

        assertThat(s.unpaidCount()).isEqualTo(2);
        assertThat(s.unpaidPrincipal()).isEqualTo(60000);
        assertThat(s.oldestUnpaidDueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(s.overdueDays()).isEqualTo(42);
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
        assertThat(s.overdueDays()).isZero();
    }

    @Test
    void 월말_시작은_plusMonths_기한과_발생회차가_일치한다() {
        // start=1/31, asOf=2/28: 2회차 기한(plusMonths(1)=2/28)이 도래했으므로 미납 2회차.
        InsuranceContract c = contract(LocalDate.of(2026, 1, 31), 30000);
        BillingStatus s = BillingCalculator.compute(c, 0, LocalDate.of(2026, 2, 28));
        assertThat(s.unpaidCount()).isEqualTo(2);
        assertThat(s.oldestUnpaidDueDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    @Test
    void 발생회차는_총회차_12를_넘지_않는다() {
        InsuranceContract c = contract(LocalDate.of(2026, 1, 1), 30000);
        BillingStatus s = BillingCalculator.compute(c, 0, LocalDate.of(2028, 1, 1));
        assertThat(s.unpaidCount()).isEqualTo(12);
    }
}
