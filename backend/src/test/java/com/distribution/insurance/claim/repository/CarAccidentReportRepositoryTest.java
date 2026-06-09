package com.distribution.insurance.claim.repository;

import com.distribution.insurance.claim.domain.CarAccidentReport;
import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.CarInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.user.repository.UserRepository;

@SpringBootTest
@Transactional
class CarAccidentReportRepositoryTest {

    @Autowired CarAccidentReportRepository reportRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    @Test
    void 자동차사고_접수를_저장하고_조회한다() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 50000, LocalDate.now()));

        CarAccidentReport saved = reportRepository.save(new CarAccidentReport(
                contract, LocalDate.now(), "서울", "단독", "12가3456", false, 0));

        assertThat(saved.getId()).isNotNull();
        assertThat(reportRepository.findById(saved.getId())).isPresent();
    }
}
