package com.distribution.insurance.repository;

import com.distribution.insurance.domain.product.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        HealthInsuranceProduct health = new HealthInsuranceProduct(
                "건강플러스", "암 보장 포함", 30000, 120);
        health.addCoverageItem(new CoverageItem("암진단비", 30_000_000, 0));
        health.addCoverageItem(new CoverageItem("암수술비", 10_000_000, 0));
        productRepository.save(health);

        HealthInsuranceProduct basic = new HealthInsuranceProduct(
                "실손기본", "통원 보장", 12000, 60);
        basic.addCoverageItem(new CoverageItem("통원치료비", 5_000_000, 10_000));
        productRepository.save(basic);

        CarInsuranceProduct car = new CarInsuranceProduct(
                "안심드라이브", "대인대물", 45000, "승용차", "가족한정");
        car.addCoverageItem(new CoverageItem("대인배상", 100_000_000, 0));
        productRepository.save(car);
    }

    @Test
    void 종류로_조회하면_해당_종류만_반환한다() {
        List<InsuranceProduct> health = productRepository.search(
                HealthInsuranceProduct.class, null, null, null);
        assertThat(health).hasSize(2)
                .allMatch(p -> p instanceof HealthInsuranceProduct);
    }

    @Test
    void 보험료_범위로_필터링한다() {
        List<InsuranceProduct> result = productRepository.search(
                HealthInsuranceProduct.class, 20000, null, null);
        assertThat(result).extracting(InsuranceProduct::getProductName)
                .containsExactly("건강플러스");
    }

    @Test
    void 키워드가_상품명에_매칭된다() {
        List<InsuranceProduct> result = productRepository.search(
                HealthInsuranceProduct.class, null, null, "실손");
        assertThat(result).extracting(InsuranceProduct::getProductName)
                .containsExactly("실손기본");
    }

    @Test
    void 키워드가_보장항목명에_매칭되고_상품은_한번만_나온다() {
        List<InsuranceProduct> result = productRepository.search(
                HealthInsuranceProduct.class, null, null, "암");
        assertThat(result).extracting(InsuranceProduct::getProductName)
                .containsExactly("건강플러스");
    }

    @Test
    void 조건에_맞는_상품이_없으면_빈_목록() {
        List<InsuranceProduct> result = productRepository.search(
                CarInsuranceProduct.class, null, null, "존재하지않는키워드");
        assertThat(result).isEmpty();
    }
}
