package com.distribution.insurance.claim.repository;

import com.distribution.insurance.claim.domain.*;
import com.distribution.insurance.contract.domain.*;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import com.distribution.insurance.contract.repository.PaymentRepository;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.user.repository.UserRepository;

@SpringBootTest
@Transactional
class ClaimQueryRepositoryTest {

    @Autowired ClaimRepository claimRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired BenefitPaymentRepository benefitPaymentRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    @Test
    void 가입자별_청구와_납입_지급_합계를_조회한다() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));

        HealthInsuranceClaim claim = claimRepository.save(new HealthInsuranceClaim(
                contract, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        paymentRepository.save(Payment.success(contract, 30000, PaymentMethod.CARD));
        paymentRepository.save(Payment.success(contract, 30000, PaymentMethod.CARD));
        benefitPaymentRepository.save(BenefitPayment.success(claim, 400000, "110-123-456789"));

        assertThat(claimRepository.findByContractPolicyholderId(ph.getId())).hasSize(1);
        assertThat(paymentRepository.sumAmountByContractIdAndStatus(contract.getId(), PaymentStatus.SUCCESS))
                .isEqualTo(60000L);
        assertThat(benefitPaymentRepository.sumPaidByContractIdAndStatus(contract.getId(), PaymentStatus.SUCCESS))
                .isEqualTo(400000L);
        assertThat(benefitPaymentRepository.sumPaidByClaimIdAndStatus(claim.getId(), PaymentStatus.SUCCESS))
                .isEqualTo(400000L);
    }
}
