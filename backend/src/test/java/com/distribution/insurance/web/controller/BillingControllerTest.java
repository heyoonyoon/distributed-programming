package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.PaymentRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BillingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    Long ownerId; String ownerToken; String otherToken; Long contractId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        Policyholder owner = userRepository.save(new Policyholder(
                "주인", "owner@test.com", "010-1111-1111", encoder.encode("pw"),
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
        ownerId = owner.getId();
        ownerToken = tokenProvider.createToken(ownerId, "POLICYHOLDER");

        Policyholder other = userRepository.save(new Policyholder(
                "타인", "other@test.com", "010-2222-2222", encoder.encode("pw"),
                "880202-2345678", LocalDate.of(1988, 2, 2), "주소2", "계좌2"));
        otherToken = tokenProvider.createToken(other.getId(), "POLICYHOLDER");

        HealthInsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("실손의료", "설명", 30000, 120));
        // 6개월 전 시작 → 미납 누적되도록
        InsuranceContract c = contractRepository.save(
                new InsuranceContract(owner, product, 30000, LocalDate.now().minusMonths(6)));
        contractId = c.getId();
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void 납부예정_목록은_미납회차가_있으면_노출된다() throws Exception {
        mockMvc.perform(get("/contracts/payable").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contractId").value(contractId))
                .andExpect(jsonPath("$[0].amount").isNumber());
    }

    @Test
    void 미납_목록은_연체분을_연체이자와_함께_노출한다() throws Exception {
        mockMvc.perform(get("/contracts/unpaid").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].overdueDays").isNumber())
                .andExpect(jsonPath("$[0].overdueInterest").isNumber());
    }

    @Test
    void 카드납부_성공() throws Exception {
        mockMvc.perform(post("/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CARD\",\"paymentInfo\":\"1234-5678-9012-3456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(30000));
    }

    @Test
    void 결제실패는_FAILED로_기록되고_사유를_반환한다() throws Exception {
        mockMvc.perform(post("/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CARD\",\"paymentInfo\":\"1234-5678-9012-0000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.reason").isNotEmpty());
    }

    private int unpaidPrincipal(String token) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/contracts/" + contractId + "/unpaid")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.unpaidPrincipal");
    }

    @Test
    void 성공_납부_후_미납회차가_하나_줄어든다() throws Exception {
        int before = unpaidPrincipal(ownerToken);

        mockMvc.perform(post("/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"TRANSFER\",\"paymentInfo\":\"110-222-333\"}"))
                .andExpect(status().isOk());

        int after = unpaidPrincipal(ownerToken);

        // FIFO로 한 회차(월 보험료 30000)만 정산되어 미납 원금이 정확히 그만큼 줄어든다.
        org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before - 30000);
    }

    @Test
    void 결제실패는_미납금액을_줄이지_않는다() throws Exception {
        int before = unpaidPrincipal(ownerToken);

        mockMvc.perform(post("/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CARD\",\"paymentInfo\":\"1234-5678-9012-0000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        int after = unpaidPrincipal(ownerToken);

        org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before);
    }

    @Test
    void 미납이_없으면_납부는_400() throws Exception {
        // 오늘 시작 계약 → 1회차만 발생. 한 번 성공 납부하면 미납이 없다.
        HealthInsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("오늘상품", "설명", 30000, 120));
        Policyholder owner = (Policyholder) userRepository.findById(ownerId).orElseThrow();
        InsuranceContract today = contractRepository.save(
                new InsuranceContract(owner, product, 30000, LocalDate.now()));
        Long todayId = today.getId();

        mockMvc.perform(post("/contracts/" + todayId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"TRANSFER\",\"paymentInfo\":\"110-222-333\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/contracts/" + todayId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"TRANSFER\",\"paymentInfo\":\"110-222-333\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 타인_토큰으로_미납조회는_403() throws Exception {
        mockMvc.perform(get("/contracts/" + contractId + "/unpaid")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 타인_토큰으로_납부는_403() throws Exception {
        mockMvc.perform(post("/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"TRANSFER\",\"paymentInfo\":\"110-222-333\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 없는_계약_미납조회는_404() throws Exception {
        mockMvc.perform(get("/contracts/999999/unpaid")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void 자동이체_등록_후_계약상세_결제수단이_AUTO_DEBIT() throws Exception {
        mockMvc.perform(post("/contracts/" + contractId + "/auto-debit")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"110-222-333333\",\"withdrawalDay\":25}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/contracts/" + contractId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("AUTO_DEBIT"));
    }
}
