package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimRetryTransitionTest {

    private HealthInsuranceClaim claim() {
        Policyholder ph = new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789");
        var product = new HealthInsuranceProduct("건강", "암", 30000, 120);
        var contract = new InsuranceContract(ph, product, 30000, LocalDate.now());
        return new HealthInsuranceClaim(contract, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);
    }

    @Test
    void 실패한_지급은_재시도로_COMPLETED될_수_있다() {
        HealthInsuranceClaim c = claim();
        c.markFailed();
        c.markCompleted();
        assertThat(c.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 실패한_지급은_재시도에서_다시_FAILED될_수_있다() {
        HealthInsuranceClaim c = claim();
        c.markFailed();
        c.markFailed();
        assertThat(c.getStatus()).isEqualTo(ClaimStatus.FAILED);
    }
}
