package com.distribution.insurance.claim.dto;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.claim.repository.BenefitPaymentRepository;
import com.distribution.insurance.claim.repository.ClaimRepository;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.user.repository.UserRepository;
import com.distribution.insurance.common.security.JwtTokenProvider;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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

    /** Fix 4: 서비스 거부(본인 계약 아님 → 403)시 업로드된 파일이 삭제된다. */
    @Test
    void 서비스_거부시_업로드된_파일이_삭제된다() throws Exception {
        // owner의 계약을 other 사용자가 청구 → 403(IllegalStateException)
        Policyholder owner = userRepository.save(new Policyholder("소유자", "owner_fix4@t.com", "010", "pw",
                "900101-1111111", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        Policyholder other = userRepository.save(new Policyholder("타인", "other_fix4@t.com", "010", "pw",
                "900101-2222222", LocalDate.of(1991, 2, 2), "주소", "220-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract c = contractRepository.save(new InsuranceContract(owner, product, 30000, LocalDate.now()));
        String otherToken = jwtTokenProvider.createToken(other.getId(), "POLICYHOLDER");

        MockMultipartFile pdf = new MockMultipartFile("attachments", "receipt.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/claims/health")
                        .file(pdf)
                        .param("contractId", String.valueOf(c.getId()))
                        .param("hospitalName", "서울병원")
                        .param("diagnosisCode", "S00")
                        .param("treatmentDate", LocalDate.now().toString())
                        .param("requestAmount", "500000")
                        .param("receiptAmount", "500000")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        // uploadDir 아래에 파일이 남아있지 않아야 한다
        long fileCount = countFiles(uploadDir);
        assertThat(fileCount).as("업로드 디렉터리에 남은 파일 없음").isEqualTo(0);
    }

    private long countFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) return 0;
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).count();
        }
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
