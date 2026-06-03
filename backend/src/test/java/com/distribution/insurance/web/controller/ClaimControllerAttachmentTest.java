package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.BenefitPaymentRepository;
import com.distribution.insurance.repository.ClaimRepository;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimControllerAttachmentTest {

    @TempDir
    static Path uploadDir;

    @DynamicPropertySource
    static void uploadDirProperty(DynamicPropertyRegistry registry) {
        registry.add("insurance.upload.dir", () -> uploadDir.toString());
    }

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentRepository benefitPaymentRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void tearDown() {
        benefitPaymentRepository.deleteAll();
        claimRepository.deleteAll();
        contractRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 허용되지_않은_첨부형식이면_400() throws Exception {
        Policyholder ph = userRepository.save(new Policyholder("홍", "att@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract c = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        String token = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        MockMultipartFile bad = new MockMultipartFile("attachments", "a.txt", "text/plain", new byte[]{1});

        mockMvc.perform(multipart("/claims/health")
                        .file(bad)
                        .param("contractId", String.valueOf(c.getId()))
                        .param("hospitalName", "서울병원")
                        .param("diagnosisCode", "S00")
                        .param("treatmentDate", LocalDate.now().toString())
                        .param("requestAmount", "500000")
                        .param("receiptAmount", "500000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
