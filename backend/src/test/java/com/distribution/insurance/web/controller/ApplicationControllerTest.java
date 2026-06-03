package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired com.distribution.insurance.repository.ApplicationRepository applicationRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    Long phId; String phToken; Long healthProductId; Long carProductId; String employeeToken;

    @AfterEach
    void tearDown() {
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        Policyholder ph = userRepository.save(new Policyholder("홍길동", "h@test.com", "010", encoder.encode("pw"),
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
        phId = ph.getId();
        phToken = tokenProvider.createToken(phId, "POLICYHOLDER");

        InsuranceEmployee emp = userRepository.save(new InsuranceEmployee(
                "심사역", "e@test.com", "010", encoder.encode("pw"), "심사팀", 0));
        employeeToken = tokenProvider.createToken(emp.getId(), "EMPLOYEE");

        InsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120));
        healthProductId = product.getId();

        InsuranceProduct carProduct = productRepository.save(
                new CarInsuranceProduct("자동차플러스", "차량 보장", 50000, "승용차", "가족"));
        carProductId = carProduct.getId();
    }

    private String medicalBody(Long productId) {
        return "{\"productId\":" + productId + ",\"medicalHistory\":"
                + "{\"currentConditions\":\"없음\",\"pastHospitalization\":\"없음\",\"medications\":\"없음\"}}";
    }

    @Test
    void 가입신청하면_201과_PENDING상태를_반환한다() throws Exception {
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + phToken)
                        .contentType("application/json")
                        .content(medicalBody(healthProductId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.applicationId").isNumber());
    }

    @Test
    void 의료상품에_차량정보를_보내면_400() throws Exception {
        String body = "{\"productId\":" + healthProductId + ",\"vehicleInfo\":"
                + "{\"plateNumber\":\"12가3456\",\"vehicleType\":\"승용차\",\"modelYear\":2020,\"drivingExperienceYears\":5}}";
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + phToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 없는_상품으로_신청하면_404() throws Exception {
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + phToken)
                        .contentType("application/json").content(medicalBody(999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 직원토큰으로_신청하면_403() throws Exception {
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType("application/json").content(medicalBody(healthProductId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 내_신청목록을_조회한다() throws Exception {
        mockMvc.perform(post("/applications")
                .header("Authorization", "Bearer " + phToken)
                .contentType("application/json").content(medicalBody(healthProductId)));

        mockMvc.perform(get("/applications/me")
                        .header("Authorization", "Bearer " + phToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("건강플러스"));
    }

    @Test
    void 차량정보_plateNumber_공백이면_400() throws Exception {
        // plateNumber가 blank → @NotBlank 위반 → 400
        String body = "{\"productId\":" + carProductId + ",\"vehicleInfo\":"
                + "{\"plateNumber\":\"\",\"vehicleType\":\"승용차\",\"modelYear\":2020,\"drivingExperienceYears\":5}}";
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + phToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void PENDING_신청을_취소하면_200() throws Exception {
        String response = mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + phToken)
                        .contentType("application/json").content(medicalBody(healthProductId)))
                .andReturn().getResponse().getContentAsString();
        Long appId = com.jayway.jsonpath.JsonPath.parse(response).read("$.applicationId", Long.class);

        mockMvc.perform(post("/applications/" + appId + "/cancel")
                        .header("Authorization", "Bearer " + phToken))
                .andExpect(status().isOk());
    }
}
