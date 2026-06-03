package com.distribution.insurance.domain.review;

import com.distribution.insurance.domain.claim.ClaimComplexity;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class BenefitPaymentReviewTest {

    private HealthInsuranceClaim complexClaim() {
        Policyholder ph = new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789");
        var product = new HealthInsuranceProduct("건강", "암", 30000, 120);
        var contract = new InsuranceContract(ph, product, 30000, LocalDate.now());
        return new HealthInsuranceClaim(contract, 2000000, "병원", "S00", LocalDate.now(), 2000000, ClaimComplexity.COMPLEX);
    }

    @Test
    void 생성하면_미배정이고_claim을_참조한다() {
        HealthInsuranceClaim claim = complexClaim();
        BenefitPaymentReview review = new BenefitPaymentReview(claim);
        assertThat(review.getClaim()).isSameAs(claim);
        assertThat(review.getAssignedStaffId()).isNull();
    }

    @Test
    void 배정하면_assignedStaffId가_채워진다() {
        BenefitPaymentReview review = new BenefitPaymentReview(complexClaim());
        InsuranceEmployee staff = new InsuranceEmployee("김직원", "k@t.com", "010", "pw", "심사1팀", 0);
        // staff.getId()는 영속화 전 null일 수 있어, 테스트에선 명시 id 주입 대신 동작만 확인
        review.assignTo(5L);
        assertThat(review.getAssignedStaffId()).isEqualTo(5L);
    }

    @Test
    void 승인_확정하면_결과가_APPROVED로_기록된다() {
        BenefitPaymentReview review = new BenefitPaymentReview(complexClaim());
        review.assignTo(5L);
        review.confirm(ReviewResult.APPROVED, "정상 청구");
        assertThat(review.getResult()).isEqualTo(ReviewResult.APPROVED);
        assertThat(review.getReviewedAt()).isNotNull();
    }

    @Test
    void 반려_확정하면_결과와_사유가_기록된다() {
        BenefitPaymentReview review = new BenefitPaymentReview(complexClaim());
        review.assignTo(5L);
        review.confirm(ReviewResult.REJECTED, "서류 미비");
        assertThat(review.getResult()).isEqualTo(ReviewResult.REJECTED);
        assertThat(review.getComment()).isEqualTo("서류 미비");
    }

    @Test
    void 조건부_결과는_허용되지_않는다() {
        BenefitPaymentReview review = new BenefitPaymentReview(complexClaim());
        review.assignTo(5L);
        assertThatThrownBy(() -> review.confirm(ReviewResult.CONDITIONAL, "x"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
