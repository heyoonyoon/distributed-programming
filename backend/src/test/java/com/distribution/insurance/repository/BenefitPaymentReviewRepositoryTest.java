package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.ClaimComplexity;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BenefitPaymentReviewRepositoryTest {

    @Autowired BenefitPaymentReviewRepository reviewRepository;
    @Autowired ClaimRepository claimRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private HealthInsuranceClaim savedComplexClaim() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        return claimRepository.save(new HealthInsuranceClaim(contract, 2000000, "병원", "S00",
                LocalDate.now(), 2000000, ClaimComplexity.COMPLEX));
    }

    @Test
    void 담당자별로_심사건을_조회한다() {
        HealthInsuranceClaim claim = savedComplexClaim();
        BenefitPaymentReview review = new BenefitPaymentReview(claim);
        review.assignTo(7L);
        reviewRepository.save(review);

        List<BenefitPaymentReview> found = reviewRepository.findByAssignedStaffId(7L);
        assertThat(found).hasSize(1);
        assertThat(reviewRepository.findByClaimId(claim.getId())).isPresent();
    }
}
