package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.contract.PaymentStatus;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BenefitPayoutServiceTest {

    @Autowired BenefitPayoutService payoutService;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentRepository benefitPaymentRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private HealthInsuranceClaim savedClaim(String bankAccount) {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", bankAccount));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        return claimRepository.save(new HealthInsuranceClaim(contract, 500000, "병원", "S00",
                LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
    }

    @Test
    void 정상계좌면_송금성공_COMPLETED_지급기록_SUCCESS() {
        HealthInsuranceClaim claim = savedClaim("110-123-456789");

        BenefitPayment payment = payoutService.pay(claim);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAmount()).isEqualTo(500000);
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 계좌오류면_송금실패_FAILED_지급기록_FAILED() {
        HealthInsuranceClaim claim = savedClaim("110-123-450000");

        BenefitPayment payment = payoutService.pay(claim);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.FAILED);
    }
}
