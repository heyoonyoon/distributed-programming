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
        Policyholder ph = new Policyholder("홍길동", "hong@test.com", "010-1111-2222", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        InsuranceProduct product = new HealthInsuranceProduct("실손의료", "설명", 30000, 120);
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

    @Test
    void generatePdf는_계약번호와_월보험료를_담은_바이트를_반환() {
        InsuranceContract c = newContract();
        byte[] pdf = c.generatePdf();
        String text = new String(pdf, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(text).contains("33000").contains("실손의료");
    }
}
