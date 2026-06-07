package com.distribution.insurance.product.domain;

import com.distribution.insurance.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductPersistenceTest {

    @Autowired
    ProductRepository productRepository;

    @Test
    void 의료보험_상품을_보장항목과_함께_저장하고_조회한다() {
        HealthInsuranceProduct product = new HealthInsuranceProduct(
                "건강플러스", "기본 보험료는 나이대 기준 예시입니다.", 30000, 120);
        product.addCoverageItem(new CoverageItem("입원비", 50_000_000, 100_000));
        product.addCoverageItem(new CoverageItem("수술비", 20_000_000, 0));

        InsuranceProduct saved = productRepository.save(product);

        InsuranceProduct found = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(found).isInstanceOf(HealthInsuranceProduct.class);
        assertThat(found.getProductName()).isEqualTo("건강플러스");
        assertThat(found.getBasePremium()).isEqualTo(30000);
        assertThat(found.getCoverageItems()).hasSize(2);
        assertThat(found.getCoverageItems().get(0).getProduct().getId()).isEqualTo(saved.getId());
    }

    @Test
    void 자동차보험_상품을_저장하고_타입이_유지된다() {
        CarInsuranceProduct product = new CarInsuranceProduct(
                "안심드라이브", "차종·운전범위에 따라 보험료가 산출됩니다.", 45000, "승용차", "가족한정");

        InsuranceProduct saved = productRepository.save(product);

        assertThat(productRepository.findById(saved.getId()).orElseThrow())
                .isInstanceOf(CarInsuranceProduct.class);
    }
}
