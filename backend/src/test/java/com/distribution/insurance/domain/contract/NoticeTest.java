package com.distribution.insurance.domain.contract;

import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class NoticeTest {

    private InsuranceContract contract() {
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
