package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimComplexity;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class StaffAssignmentServiceTest {

    @Autowired StaffAssignmentService assignmentService;
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

    private InsuranceEmployee emp(String email, int load) {
        return userRepository.save(new InsuranceEmployee("직원", email, "010", "pw", "심사팀", load));
    }

    @Test
    void 최소부하_직원에게_배정하고_부하를_올린다() {
        InsuranceEmployee busy = emp("busy@t.com", 5);
        InsuranceEmployee idle = emp("idle@t.com", 1);
        BenefitPaymentReview review = reviewRepository.save(new BenefitPaymentReview(savedComplexClaim()));

        assignmentService.assignAutomatically(review);

        assertThat(review.getAssignedStaffId()).isEqualTo(idle.getId());
        assertThat(userRepository.findById(idle.getId()).map(u -> ((InsuranceEmployee) u).getCurrentLoad()))
                .contains(2);
    }

    @Test
    void 배정가능_직원이_없으면_예외() {
        BenefitPaymentReview review = reviewRepository.save(new BenefitPaymentReview(savedComplexClaim()));
        assertThatThrownBy(() -> assignmentService.assignAutomatically(review))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 수동배정은_지정_직원으로_배정한다() {
        InsuranceEmployee a = emp("a@t.com", 0);
        InsuranceEmployee b = emp("b@t.com", 0);
        BenefitPaymentReview review = reviewRepository.save(new BenefitPaymentReview(savedComplexClaim()));

        assignmentService.assignManually(review, b.getId());

        assertThat(review.getAssignedStaffId()).isEqualTo(b.getId());
    }
}
