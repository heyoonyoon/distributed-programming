package com.distribution.insurance.product.controller;

import com.distribution.insurance.product.domain.CarInsuranceProduct;
import com.distribution.insurance.product.domain.CoverageItem;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;

    Long healthId;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        HealthInsuranceProduct health = new HealthInsuranceProduct(
                "건강플러스", "암 보장 포함", 30000, 120);
        health.addCoverageItem(new CoverageItem("암진단비", 30_000_000, 0));
        healthId = productRepository.save(health).getId();

        CarInsuranceProduct car = new CarInsuranceProduct(
                "안심드라이브", "대인대물", 45000, "승용차", "가족한정");
        productRepository.save(car);
    }

    @Test
    void 인증없이_종류별_목록을_조회한다() throws Exception {
        mockMvc.perform(get("/products").param("type", "HEALTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("건강플러스"))
                .andExpect(jsonPath("$[0].monthlyPremium").value(30000))
                .andExpect(jsonPath("$[0].productType").value("HEALTH"));
    }

    @Test
    void 키워드_필터로_목록을_좁힌다() throws Exception {
        mockMvc.perform(get("/products").param("type", "HEALTH").param("keyword", "암"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void 상세조회는_보장항목을_포함한다() throws Exception {
        mockMvc.perform(get("/products/" + healthId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("건강플러스"))
                .andExpect(jsonPath("$.coverageItems.length()").value(1))
                .andExpect(jsonPath("$.coverageItems[0].itemName").value("암진단비"));
    }

    @Test
    void 없는_상품_상세는_404() throws Exception {
        mockMvc.perform(get("/products/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 잘못된_종류값은_400() throws Exception {
        mockMvc.perform(get("/products").param("type", "LIFE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 음수_보험료_필터는_400() throws Exception {
        mockMvc.perform(get("/products").param("type", "HEALTH").param("minPremium", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 최소가_최대보다_큰_보험료_범위는_400() throws Exception {
        mockMvc.perform(get("/products")
                        .param("type", "HEALTH")
                        .param("minPremium", "50000")
                        .param("maxPremium", "10000"))
                .andExpect(status().isBadRequest());
    }
}
