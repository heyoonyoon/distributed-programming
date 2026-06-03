package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
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
class ClaimServiceTest {

    @Autowired ClaimService claimService;
    @Autowired ClaimRepository claimRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Policyholder ph(String account) {
        return userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", account));
    }

    private InsuranceContract healthContract(Policyholder ph) {
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        return contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
    }

    @Test
    void 임계값_미만이면_SIMPLE이고_즉시_COMPLETED된다() {
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = healthContract(p);

        HealthInsuranceClaim claim = claimService.fileHealthClaim(
                p.getId(), c.getId(), "서울병원", "S00", LocalDate.now(), 500000, 500000, List.of());

        assertThat(claim.getComplexity()).isEqualTo(ClaimComplexity.SIMPLE);
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 임계값_이상이면_COMPLEX이고_PENDING으로_남는다() {
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = healthContract(p);

        HealthInsuranceClaim claim = claimService.fileHealthClaim(
                p.getId(), c.getId(), "서울병원", "S00", LocalDate.now(), 2000000, 2000000, List.of());

        assertThat(claim.getComplexity()).isEqualTo(ClaimComplexity.COMPLEX);
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.PENDING);
    }

    @Test
    void 본인계약이_아니면_403성_예외() {
        Policyholder owner = ph("110-123-456789");
        Policyholder other = ph("220-123-456789");
        InsuranceContract c = healthContract(owner);

        assertThatThrownBy(() -> claimService.fileHealthClaim(
                other.getId(), c.getId(), "서울병원", "S00", LocalDate.now(), 500000, 500000, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 자동차보험_계약에_의료청구하면_400성_예외() {
        Policyholder p = ph("110-123-456789");
        // CarInsuranceProduct constructor: (productName, description, basePremium, vehicleType, driverScopeType)
        var product = productRepository.save(new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
        InsuranceContract car = contractRepository.save(new InsuranceContract(p, product, 50000, LocalDate.now()));

        assertThatThrownBy(() -> claimService.fileHealthClaim(
                p.getId(), car.getId(), "서울병원", "S00", LocalDate.now(), 500000, 500000, List.of()))
                .isInstanceOf(InvalidRequestException.class);
    }
}
