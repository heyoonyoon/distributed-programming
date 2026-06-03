package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ClaimServiceComplexTest {

    @Autowired ClaimService claimService;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentReviewRepository reviewRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Long phId(String acct) {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", acct));
        return ph.getId();
    }

    private Long healthContractId(Long phId) {
        Policyholder ph = (Policyholder) userRepository.findById(phId).orElseThrow();
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        return contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now())).getId();
    }

    @Test
    void COMPLEX_청구는_심사가_생성되고_배정되고_IN_REVIEW가_된다() {
        userRepository.save(new InsuranceEmployee("직원", "e@t.com", "010", "pw", "심사팀", 0));
        Long ph = phId("110-123-456789");
        Long contractId = healthContractId(ph);

        HealthInsuranceClaim claim = claimService.fileHealthClaim(
                ph, contractId, "서울병원", "S00", LocalDate.now(), 2000000, 2000000, List.of());

        HealthInsuranceClaim reloaded = (HealthInsuranceClaim) claimRepository.findById(claim.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ClaimStatus.IN_REVIEW);
        BenefitPaymentReview review = reviewRepository.findByClaimId(claim.getId()).orElseThrow();
        assertThat(review.getAssignedStaffId()).isNotNull();
    }

    @Test
    void 직원이_없으면_COMPLEX_청구는_롤백되어_409성_예외() {
        Long ph = phId("110-123-456789");
        Long contractId = healthContractId(ph);

        assertThatThrownBy(() -> claimService.fileHealthClaim(
                ph, contractId, "서울병원", "S00", LocalDate.now(), 2000000, 2000000, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}
