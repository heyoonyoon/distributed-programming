package com.distribution.insurance.repository;

import com.distribution.insurance.domain.product.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<InsuranceProduct, Long> {

    /**
     * 종류(필수) + 보험료 범위·키워드(선택)로 상품을 조회한다.
     * 선택 파라미터는 null이면 무시(null-guard). 키워드는 상품명 또는 보장항목명에 매칭.
     * 보장항목 JOIN으로 한 상품이 여러 번 매칭될 수 있어 DISTINCT로 상품 단위 1회만 반환.
     */
    @Query("""
            SELECT DISTINCT p FROM InsuranceProduct p
            LEFT JOIN p.coverageItems c
            WHERE TYPE(p) = :type
              AND (:minPremium IS NULL OR p.basePremium >= :minPremium)
              AND (:maxPremium IS NULL OR p.basePremium <= :maxPremium)
              AND (:keyword IS NULL
                   OR p.productName LIKE CONCAT('%', :keyword, '%')
                   OR c.itemName LIKE CONCAT('%', :keyword, '%'))
            """)
    List<InsuranceProduct> search(@Param("type") Class<? extends InsuranceProduct> type,
                                  @Param("minPremium") Integer minPremium,
                                  @Param("maxPremium") Integer maxPremium,
                                  @Param("keyword") String keyword);
}
