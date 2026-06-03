package com.distribution.insurance.repository;

import com.distribution.insurance.domain.product.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<InsuranceProduct, Long> {
}
