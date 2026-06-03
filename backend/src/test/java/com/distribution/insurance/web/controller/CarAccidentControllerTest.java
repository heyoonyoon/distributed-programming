package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CarAccidentControllerTest {

    @TempDir static Path uploadDir;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("insurance.upload.dir", () -> uploadDir.toString());
    }

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void 자동차사고_접수_멀티파트가_201을_반환한다() throws Exception {
        userRepository.save(new InsuranceEmployee("직원", "e@t.com", "010", "pw", "사고팀", 0));
        Policyholder ph = userRepository.save(new Policyholder("홍", "car@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
        InsuranceContract c = contractRepository.save(new InsuranceContract(ph, product, 50000, LocalDate.now()));
        String token = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        MockMultipartFile photo = new MockMultipartFile("attachments", "scene.jpg", "image/jpeg", new byte[]{1, 2});

        mockMvc.perform(multipart("/claims/car-accidents")
                        .file(photo)
                        .param("contractId", String.valueOf(c.getId()))
                        .param("accidentDate", LocalDate.now().toString())
                        .param("accidentLocation", "서울 강남")
                        .param("accidentType", "쌍방")
                        .param("vehicleNumber", "12가3456")
                        .param("hasInjury", "true")
                        .param("injuredCount", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
