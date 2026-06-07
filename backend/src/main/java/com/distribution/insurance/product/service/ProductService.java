package com.distribution.insurance.product.service;

import com.distribution.insurance.product.domain.ProductType;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.product.dto.ProductDetailResponse;
import com.distribution.insurance.product.dto.ProductSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> search(ProductType type, Integer minPremium,
                                               Integer maxPremium, String keyword) {
        return productRepository.search(type.entityClass(), minPremium, maxPremium, keyword)
                .stream()
                .map(ProductSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse detail(Long id) {
        return productRepository.findById(id)
                .map(ProductDetailResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    }
}
