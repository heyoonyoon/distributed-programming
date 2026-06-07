package com.distribution.insurance.claim.service;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.distribution.insurance.claim.repository.ClaimRepository;
import com.distribution.insurance.review.repository.BenefitPaymentReviewRepository;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.user.repository.UserRepository;

/**
 * COMPLEX 청구 실패 시 롤백 검증 — @Transactional 없이 실제 롤백 증명 (FIX B).
 * 같은 tx 안에서 count하면 신뢰할 수 없으므로 별도 클래스로 분리.
 */
@SpringBootTest
class ClaimServiceRollbackTest {

    @Autowired ClaimService claimService;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentReviewRepository reviewRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Policyholder savedPh;
    private HealthInsuranceProduct savedProduct;
    private InsuranceContract savedContract;

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        claimRepository.deleteAll();
        if (savedContract != null) contractRepository.deleteById(savedContract.getId());
        if (savedProduct != null) productRepository.deleteById(savedProduct.getId());
        if (savedPh != null) userRepository.deleteById(savedPh.getId());
    }

    @Test
    void 직원이_없으면_COMPLEX_청구는_DB에_저장되지_않는다() {
        savedPh = userRepository.save(new Policyholder("홍", "rb" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        savedProduct = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        savedContract = contractRepository.save(new InsuranceContract(savedPh, savedProduct, 30000, LocalDate.now()));

        long before = claimRepository.count();

        assertThatThrownBy(() -> claimService.fileHealthClaim(
                savedPh.getId(), savedContract.getId(), "서울병원", "S00",
                LocalDate.now(), 2000000, 2000000, List.of()))
                .isInstanceOf(IllegalStateException.class);

        long after = claimRepository.count();
        assertThat(after).isEqualTo(before);
    }
}
