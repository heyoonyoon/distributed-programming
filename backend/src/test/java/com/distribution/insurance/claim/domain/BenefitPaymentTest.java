package com.distribution.insurance.claim.domain;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.contract.domain.PaymentStatus;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
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
