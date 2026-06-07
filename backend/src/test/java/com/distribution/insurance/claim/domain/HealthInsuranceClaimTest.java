package com.distribution.insurance.claim.domain;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.common.service.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class HealthInsuranceClaimTest {

    private InsuranceContract contract() {
        Policyholder ph = new Policyholder("홍길동", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789");
        HealthInsuranceProduct product = new HealthInsuranceProduct("건강플러스", "암", 30000, 120);
        return new InsuranceContract(ph, product, 30000, LocalDate.now());
    }

    @Test
    void 생성하면_PENDING이고_청구금액이_보존된다() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 500000, "서울병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(claim.getRequestAmount()).isEqualTo(500000);
        assertThat(claim.getComplexity()).isEqualTo(ClaimComplexity.SIMPLE);
        assertThat(claim.getHospitalName()).isEqualTo("서울병원");
    }

    @Test
    void 첨부를_추가하면_컬렉션에_쌓인다() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 500000, "서울병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);
        claim.addAttachment(new ClaimAttachment("r.pdf", "application/pdf", 1L, "/p/r.pdf"));
        assertThat(claim.getAttachments()).hasSize(1);
    }

    @Test
    void 지급완료_전이는_PENDING에서_COMPLETED로_간다() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 500000, "서울병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);
        claim.markCompleted();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 이미_완료된_건을_다시_완료하면_예외() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 500000, "서울병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE);
        claim.markCompleted();
        assertThatThrownBy(claim::markCompleted).isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void COMPLEX_전이체인은_PENDING_IN_REVIEW_APPROVED_COMPLETED로_간다() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 2000000, "서울병원", "S00", LocalDate.now(), 2000000, ClaimComplexity.COMPLEX);
        claim.markInReview();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.IN_REVIEW);
        claim.markApproved();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        claim.markCompleted();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void PENDING에서_바로_승인하면_예외() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 2000000, "서울병원", "S00", LocalDate.now(), 2000000, ClaimComplexity.COMPLEX);
        assertThatThrownBy(claim::markApproved).isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void 반려된_건을_지급완료하면_예외() {
        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract(), 2000000, "서울병원", "S00", LocalDate.now(), 2000000, ClaimComplexity.COMPLEX);
        claim.markInReview();
        claim.markRejected();
        assertThatThrownBy(claim::markCompleted).isInstanceOf(IllegalStateTransitionException.class);
    }
}
