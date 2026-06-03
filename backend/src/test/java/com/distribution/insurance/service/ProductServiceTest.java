package com.distribution.insurance.service;

import com.distribution.insurance.domain.product.*;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.web.dto.ProductDetailResponse;
import com.distribution.insurance.web.dto.ProductSummaryResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    ProductRepository productRepository = mock(ProductRepository.class);
    ProductService productService = new ProductService(productRepository);

    private HealthInsuranceProduct sampleHealth() {
        HealthInsuranceProduct p = new HealthInsuranceProduct(
                "건강플러스", "암 보장", 30000, 120);
        p.addCoverageItem(new CoverageItem("암진단비", 30_000_000, 0));
        return p;
    }

    @Test
    void 종류와_필터로_목록을_조회하면_요약DTO로_변환한다() {
        when(productRepository.search(eq(HealthInsuranceProduct.class), any(), any(), any()))
                .thenReturn(List.of(sampleHealth()));

        List<ProductSummaryResponse> result =
                productService.search(ProductType.HEALTH, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productName()).isEqualTo("건강플러스");
        assertThat(result.get(0).monthlyPremium()).isEqualTo(30000);
        assertThat(result.get(0).productType()).isEqualTo("HEALTH");
        assertThat(result.get(0).coverageSummary()).contains("암진단비");
    }

    @Test
    void 상세조회는_보장항목까지_담은_상세DTO를_반환한다() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleHealth()));

        ProductDetailResponse detail = productService.detail(1L);

        assertThat(detail.productName()).isEqualTo("건강플러스");
        assertThat(detail.description()).isEqualTo("암 보장");
        assertThat(detail.coverageItems()).hasSize(1);
        assertThat(detail.coverageItems().get(0).itemName()).isEqualTo("암진단비");
        assertThat(detail.coverageItems().get(0).coverageLimit()).isEqualTo(30_000_000);
    }

    @Test
    void 없는_id로_상세조회하면_예외() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.detail(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
