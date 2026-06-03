package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.ClaimComplexity;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
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
class ClaimRepositoryTest {

    @Autowired ClaimRepository claimRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    @Test
    void 계약별로_청구를_조회한다() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        claimRepository.save(new HealthInsuranceClaim(contract, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));

        List<com.distribution.insurance.domain.claim.Claim> found = claimRepository.findByContractId(contract.getId());
        assertThat(found).hasSize(1);
    }
}
