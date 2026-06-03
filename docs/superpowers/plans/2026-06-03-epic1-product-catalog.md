# Epic 1 — 보험 상품 조회(UC01) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** UC01 보험 상품 조회 — 상품 도메인(상속) + 종류별 목록/상세 조회 + 필터(보험료 범위·키워드)를 비로그인 가능 REST API로 구현한다.

**Architecture:** Epic 0의 계층 규약(Controller→DTO→Service→Entity→Repository)을 그대로 따른다. `InsuranceProduct`(abstract)를 `SINGLE_TABLE` 상속으로 두고 `CoverageItem`은 별도 테이블(composition). 필터는 단일 JPQL + null-guard, 키워드 매칭 중복은 `SELECT DISTINCT`. 상품 조회 엔드포인트는 `permitAll`.

**Tech Stack:** Spring Boot 4 / Java 21, Spring Data JPA, Spring Security(JWT 기존), Lombok, Bean Validation, JUnit5 + Mockito + AssertJ, H2(test) / MySQL(run).

**준수 사항(필독):**
- 네이밍은 `CONTEXT.md` 용어 그대로: `InsuranceProduct`, `HealthInsuranceProduct`, `CarInsuranceProduct`, `CoverageItem`. 동의어(product 단독, 담보, coverage 등) 금지.
- `docs/adr/0001-diagram-methods-are-responsibilities.md` 준수: `calculatePremium`은 **만들지 않는다**(UC02 Epic). `getProductInfo()`는 DTO 매핑으로 대체, 엔티티가 응답 객체를 직접 생성하지 않는다.
- 클래스 다이어그램(`docs/class_diagram/02_product.md`)에 없는 도메인 필드/클래스 신설 금지. 보험료 산출 기준은 `description`에 텍스트로 녹인다(별도 컬럼 없음).
- PK는 `Long id` 자동증가(다이어그램의 String `productId`/`itemId`는 PK로 쓰지 않음).

---

## File Structure

**생성**
- `backend/src/main/java/com/distribution/insurance/domain/product/InsuranceProduct.java` — 추상 부모 엔티티
- `backend/src/main/java/com/distribution/insurance/domain/product/HealthInsuranceProduct.java`
- `backend/src/main/java/com/distribution/insurance/domain/product/CarInsuranceProduct.java`
- `backend/src/main/java/com/distribution/insurance/domain/product/CoverageItem.java`
- `backend/src/main/java/com/distribution/insurance/domain/product/ProductType.java` — 종류 enum(요청 파라미터 + 엔티티 클래스 매핑)
- `backend/src/main/java/com/distribution/insurance/repository/ProductRepository.java`
- `backend/src/main/java/com/distribution/insurance/web/dto/CoverageItemResponse.java`
- `backend/src/main/java/com/distribution/insurance/web/dto/ProductSummaryResponse.java`
- `backend/src/main/java/com/distribution/insurance/web/dto/ProductDetailResponse.java`
- `backend/src/main/java/com/distribution/insurance/service/ProductService.java`
- `backend/src/main/java/com/distribution/insurance/web/controller/ProductController.java`
- 테스트: 각 계층 대응 테스트 파일(아래 태스크에 경로 명시)

**수정**
- `backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java` — `/products/**` GET permitAll
- `backend/src/main/java/com/distribution/insurance/web/GlobalExceptionHandler.java` — 400 매핑(잘못된 type, 음수 premium)
- `backend/src/main/java/com/distribution/insurance/config/DataSeeder.java` — 상품 시드 추가(생성자에 `ProductRepository` 주입)
- `backend/src/test/java/com/distribution/insurance/config/DataSeederTest.java` — 생성자 변경 반영

---

## Task 1: 상품 도메인 엔티티 (InsuranceProduct 계층 + CoverageItem)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/product/InsuranceProduct.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/product/HealthInsuranceProduct.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/product/CarInsuranceProduct.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/product/CoverageItem.java`
- Create: `backend/src/main/java/com/distribution/insurance/repository/ProductRepository.java` (이 태스크에선 빈 `JpaRepository`만; 동적 쿼리는 Task 2)
- Test: `backend/src/test/java/com/distribution/insurance/domain/product/ProductPersistenceTest.java`

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/distribution/insurance/domain/product/ProductPersistenceTest.java`
```java
package com.distribution.insurance.domain.product;

import com.distribution.insurance.repository.ProductRepository;
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.product.ProductPersistenceTest"`
Expected: 컴파일 실패(`HealthInsuranceProduct`, `ProductRepository` 등 미정의).

- [ ] **Step 3: Write minimal implementation**

`InsuranceProduct.java`
```java
package com.distribution.insurance.domain.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "insurance_product")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "product_type")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    @Column(length = 1000)
    private String description;

    private int basePremium;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoverageItem> coverageItems = new ArrayList<>();

    protected InsuranceProduct(String productName, String description, int basePremium) {
        this.productName = productName;
        this.description = description;
        this.basePremium = basePremium;
    }

    /** 보장항목 추가 — 양방향 연관관계를 한 곳에서 일관되게 설정. */
    public void addCoverageItem(CoverageItem item) {
        coverageItems.add(item);
        item.assignProduct(this);
    }
}
```

`CoverageItem.java`
```java
package com.distribution.insurance.domain.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coverage_item")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CoverageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;
    private int coverageLimit;
    private int deductible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private InsuranceProduct product;

    public CoverageItem(String itemName, int coverageLimit, int deductible) {
        this.itemName = itemName;
        this.coverageLimit = coverageLimit;
        this.deductible = deductible;
    }

    /** InsuranceProduct.addCoverageItem에서만 호출(연관관계 주인 설정). */
    void assignProduct(InsuranceProduct product) {
        this.product = product;
    }
}
```

`HealthInsuranceProduct.java`
```java
package com.distribution.insurance.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("HEALTH")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class HealthInsuranceProduct extends InsuranceProduct {

    private int maxHospitalizationDays;

    public HealthInsuranceProduct(String productName, String description, int basePremium,
                                  int maxHospitalizationDays) {
        super(productName, description, basePremium);
        this.maxHospitalizationDays = maxHospitalizationDays;
    }
}
```

`CarInsuranceProduct.java`
```java
package com.distribution.insurance.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CAR")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CarInsuranceProduct extends InsuranceProduct {

    private String vehicleType;
    private String driverScopeType;

    public CarInsuranceProduct(String productName, String description, int basePremium,
                               String vehicleType, String driverScopeType) {
        super(productName, description, basePremium);
        this.vehicleType = vehicleType;
        this.driverScopeType = driverScopeType;
    }
}
```

`ProductRepository.java` (이 태스크에선 기본형만)
```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.product.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<InsuranceProduct, Long> {
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.domain.product.ProductPersistenceTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/product/ \
        backend/src/main/java/com/distribution/insurance/repository/ProductRepository.java \
        backend/src/test/java/com/distribution/insurance/domain/product/ProductPersistenceTest.java
git commit -m "feat(epic1): 상품 도메인 엔티티(InsuranceProduct 계층 + CoverageItem)"
```

---

## Task 2: ProductType enum + ProductRepository 동적 검색 쿼리

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/product/ProductType.java`
- Modify: `backend/src/main/java/com/distribution/insurance/repository/ProductRepository.java`
- Test: `backend/src/test/java/com/distribution/insurance/repository/ProductRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/distribution/insurance/repository/ProductRepositoryTest.java`
```java
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
                .containsExactly("건강플러스"); // 보장항목 2개 매칭돼도 1번만
    }

    @Test
    void 조건에_맞는_상품이_없으면_빈_목록() {
        List<InsuranceProduct> result = productRepository.search(
                CarInsuranceProduct.class, null, null, "존재하지않는키워드");
        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.repository.ProductRepositoryTest"`
Expected: 컴파일 실패(`search` 메서드 없음).

- [ ] **Step 3: Write minimal implementation**

`ProductType.java`
```java
package com.distribution.insurance.domain.product;

/** 요청 파라미터(type)와 엔티티 클래스를 잇는 보험 종류. */
public enum ProductType {
    HEALTH(HealthInsuranceProduct.class),
    CAR(CarInsuranceProduct.class);

    private final Class<? extends InsuranceProduct> entityClass;

    ProductType(Class<? extends InsuranceProduct> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<? extends InsuranceProduct> entityClass() {
        return entityClass;
    }
}
```

`ProductRepository.java` (메서드 추가)
```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.repository.ProductRepositoryTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/domain/product/ProductType.java \
        backend/src/main/java/com/distribution/insurance/repository/ProductRepository.java \
        backend/src/test/java/com/distribution/insurance/repository/ProductRepositoryTest.java
git commit -m "feat(epic1): ProductType enum + 동적 검색 쿼리(종류/필터/키워드 DISTINCT)"
```

---

## Task 3: 응답 DTO + ProductService

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/CoverageItemResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ProductSummaryResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ProductDetailResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/ProductService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/ProductServiceTest.java`

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/distribution/insurance/service/ProductServiceTest.java`
```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.service.ProductServiceTest"`
Expected: 컴파일 실패(DTO·`ProductService` 미정의).

- [ ] **Step 3: Write minimal implementation**

`CoverageItemResponse.java`
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.product.CoverageItem;

public record CoverageItemResponse(String itemName, int coverageLimit, int deductible) {

    public static CoverageItemResponse from(CoverageItem item) {
        return new CoverageItemResponse(
                item.getItemName(), item.getCoverageLimit(), item.getDeductible());
    }
}
```

`ProductSummaryResponse.java`
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.product.CoverageItem;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;

import java.util.stream.Collectors;

public record ProductSummaryResponse(
        Long id, String productName, String coverageSummary,
        int monthlyPremium, String productType) {

    public static ProductSummaryResponse from(InsuranceProduct product) {
        String summary = product.getCoverageItems().stream()
                .map(CoverageItem::getItemName)
                .limit(3)
                .collect(Collectors.joining(", "));
        return new ProductSummaryResponse(
                product.getId(), product.getProductName(), summary,
                product.getBasePremium(), typeOf(product));
    }

    static String typeOf(InsuranceProduct product) {
        return product instanceof HealthInsuranceProduct ? "HEALTH" : "CAR";
    }
}
```

`ProductDetailResponse.java`
```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.product.InsuranceProduct;

import java.util.List;

public record ProductDetailResponse(
        Long id, String productName, String productType, String description,
        int monthlyPremium, List<CoverageItemResponse> coverageItems) {

    public static ProductDetailResponse from(InsuranceProduct product) {
        List<CoverageItemResponse> items = product.getCoverageItems().stream()
                .map(CoverageItemResponse::from)
                .toList();
        return new ProductDetailResponse(
                product.getId(), product.getProductName(),
                ProductSummaryResponse.typeOf(product), product.getDescription(),
                product.getBasePremium(), items);
    }
}
```

`ProductService.java`
```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.product.ProductType;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.web.dto.ProductDetailResponse;
import com.distribution.insurance.web.dto.ProductSummaryResponse;
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.service.ProductServiceTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/dto/CoverageItemResponse.java \
        backend/src/main/java/com/distribution/insurance/web/dto/ProductSummaryResponse.java \
        backend/src/main/java/com/distribution/insurance/web/dto/ProductDetailResponse.java \
        backend/src/main/java/com/distribution/insurance/service/ProductService.java \
        backend/src/test/java/com/distribution/insurance/service/ProductServiceTest.java
git commit -m "feat(epic1): 상품 응답 DTO + ProductService(목록/상세)"
```

---

## Task 4: ProductController + Security permitAll + 400 매핑

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/ProductController.java`
- Modify: `backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java`
- Modify: `backend/src/main/java/com/distribution/insurance/web/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/ProductControllerTest.java`

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/distribution/insurance/web/controller/ProductControllerTest.java`
```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.CoverageItem;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.repository.ProductRepository;
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
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.web.controller.ProductControllerTest"`
Expected: 컴파일 실패 / 404·401 등(컨트롤러·permitAll 없음).

- [ ] **Step 3: Write minimal implementation**

`ProductController.java`
```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.product.ProductType;
import com.distribution.insurance.service.ProductService;
import com.distribution.insurance.web.dto.ProductDetailResponse;
import com.distribution.insurance.web.dto.ProductSummaryResponse;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public List<ProductSummaryResponse> list(
            @RequestParam ProductType type,
            @RequestParam(required = false) @Min(0) Integer minPremium,
            @RequestParam(required = false) @Min(0) Integer maxPremium,
            @RequestParam(required = false) String keyword) {
        return productService.search(type, minPremium, maxPremium, keyword);
    }

    @GetMapping("/products/{id}")
    public ProductDetailResponse detail(@PathVariable Long id) {
        return productService.detail(id);
    }
}
```

`SecurityConfig.java` — `authorizeHttpRequests` 블록에 상품 GET 공개 추가. 기존:
```java
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated())
```
변경 후(상단에 import `org.springframework.http.HttpMethod;` 추가):
```java
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                        .anyRequest().authenticated())
```

`GlobalExceptionHandler.java` — 400 매핑 2개 추가(잘못된 enum 값, 제약 위반). 상단 import 추가:
```java
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
```
클래스 안에 핸들러 추가:
```java
    /** 잘못된 요청 파라미터(없는 종류 값 등) → 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청 파라미터입니다.");
    }

    /** 제약 위반(음수 보험료 등) → 400 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.web.controller.ProductControllerTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/web/controller/ProductController.java \
        backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java \
        backend/src/main/java/com/distribution/insurance/web/GlobalExceptionHandler.java \
        backend/src/test/java/com/distribution/insurance/web/controller/ProductControllerTest.java
git commit -m "feat(epic1): ProductController + /products permitAll + 400 매핑"
```

---

## Task 5: DataSeeder 상품 시드

**Files:**
- Modify: `backend/src/main/java/com/distribution/insurance/config/DataSeeder.java`
- Modify: `backend/src/test/java/com/distribution/insurance/config/DataSeederTest.java`
- Test: `backend/src/test/java/com/distribution/insurance/config/ProductSeedTest.java`

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/distribution/insurance/config/ProductSeedTest.java`
```java
package com.distribution.insurance.config;

import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProductSeedTest {

    @Test
    void 상품이_없으면_시드한다() throws Exception {
        UserRepository userRepo = mock(UserRepository.class);
        ProductRepository productRepo = mock(ProductRepository.class);
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(productRepo.count()).thenReturn(0L);

        new DataSeeder(userRepo, new BCryptPasswordEncoder(), productRepo).run();

        verify(productRepo, atLeast(1)).save(any());
    }

    @Test
    void 상품이_이미_있으면_시드하지_않는다() throws Exception {
        UserRepository userRepo = mock(UserRepository.class);
        ProductRepository productRepo = mock(ProductRepository.class);
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(productRepo.count()).thenReturn(6L);

        new DataSeeder(userRepo, new BCryptPasswordEncoder(), productRepo).run();

        verify(productRepo, never()).save(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.config.ProductSeedTest"`
Expected: 컴파일 실패(`DataSeeder` 3-인자 생성자 없음).

- [ ] **Step 3: Write minimal implementation**

`DataSeeder.java` — `ProductRepository` 주입 + `seedProducts()` 추가. 전체 파일:
```java
package com.distribution.insurance.config;

import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.CoverageItem;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final ProductRepository productRepository;

    public DataSeeder(UserRepository userRepository, PasswordEncoder encoder,
                      ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        seedPolicyholder("hong@test.com", "홍길동", "010-1111-1111",
                "900101-1234567", LocalDate.of(1990, 1, 1), "서울시 강남구", "110-111-111111");
        seedPolicyholder("kim@test.com", "김보험", "010-2222-2222",
                "850505-2345678", LocalDate.of(1985, 5, 5), "부산시 해운대구", "220-222-222222");
        seedEmployee("staff@test.com", "이심사", "010-9999-9999", "심사팀");
        seedProducts();
    }

    private void seedPolicyholder(String email, String name, String phone,
                                  String ssn, LocalDate birthDate, String address, String bankAccount) {
        if (userRepository.findByEmail(email).isPresent()) return;
        userRepository.save(new Policyholder(
                name, email, phone, encoder.encode("1234"), ssn, birthDate, address, bankAccount));
    }

    private void seedEmployee(String email, String name, String phone, String department) {
        if (userRepository.findByEmail(email).isPresent()) return;
        userRepository.save(new InsuranceEmployee(
                name, email, phone, encoder.encode("1234"), department, 0));
    }

    private void seedProducts() {
        if (productRepository.count() > 0) return;

        HealthInsuranceProduct healthPlus = new HealthInsuranceProduct(
                "건강플러스", "암·주요질환 중심 의료보험. 기본 보험료는 30대 기준 예시이며 나이·병력에 따라 산출됩니다.",
                30000, 120);
        healthPlus.addCoverageItem(new CoverageItem("암진단비", 30_000_000, 0));
        healthPlus.addCoverageItem(new CoverageItem("암수술비", 10_000_000, 0));
        healthPlus.addCoverageItem(new CoverageItem("입원비", 5_000_000, 100_000));
        productRepository.save(healthPlus);

        HealthInsuranceProduct silson = new HealthInsuranceProduct(
                "실손기본", "통원·입원 실손 보장. 기본 보험료는 30대 기준 예시입니다.",
                12000, 60);
        silson.addCoverageItem(new CoverageItem("통원치료비", 5_000_000, 10_000));
        silson.addCoverageItem(new CoverageItem("입원치료비", 30_000_000, 200_000));
        productRepository.save(silson);

        CarInsuranceProduct safeDrive = new CarInsuranceProduct(
                "안심드라이브", "대인·대물 종합 자동차보험. 차종·운전범위·사고이력에 따라 보험료가 산출됩니다.",
                45000, "승용차", "가족한정");
        safeDrive.addCoverageItem(new CoverageItem("대인배상", 100_000_000, 0));
        safeDrive.addCoverageItem(new CoverageItem("대물배상", 50_000_000, 200_000));
        productRepository.save(safeDrive);

        CarInsuranceProduct lightCar = new CarInsuranceProduct(
                "가벼운자차", "자기차량손해 중심 보급형 자동차보험. 운전범위 누구나.",
                28000, "경차", "누구나");
        lightCar.addCoverageItem(new CoverageItem("자기차량손해", 20_000_000, 300_000));
        productRepository.save(lightCar);
    }
}
```

`DataSeederTest.java` — 기존 두 테스트의 `new DataSeeder(repo, encoder)`를 3-인자로 수정. 변경되는 부분만:
```java
import com.distribution.insurance.repository.ProductRepository;
```
그리고 각 테스트에서 생성자 호출을 아래로 교체(상품 시드가 추가 save를 일으키지 않도록 `count()`가 양수를 반환하게 스텁):
```java
        ProductRepository productRepo = org.mockito.Mockito.mock(ProductRepository.class);
        org.mockito.Mockito.when(productRepo.count()).thenReturn(99L);
        DataSeeder seeder = new DataSeeder(repo, encoder, productRepo);
```
(두 테스트 모두 사용자 `save` 횟수만 검증하므로, 상품 `count()`를 99로 두면 상품 시드는 건너뛰어 기존 검증값 3회/0회가 유지된다.)

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "com.distribution.insurance.config.ProductSeedTest" --tests "com.distribution.insurance.config.DataSeederTest"`
Expected: PASS (ProductSeedTest 2 + DataSeederTest 2).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/distribution/insurance/config/DataSeeder.java \
        backend/src/test/java/com/distribution/insurance/config/DataSeederTest.java \
        backend/src/test/java/com/distribution/insurance/config/ProductSeedTest.java
git commit -m "feat(epic1): DataSeeder 상품 시드(의료 2 + 자동차 2 + 보장항목)"
```

---

## Task 6: 전체 회귀 검증

**Files:** (없음 — 검증 전용)

- [ ] **Step 1: 전체 테스트 실행**

Run: `cd backend && ./gradlew test`
Expected: 전체 그린(Epic 0 기존 테스트 + Epic 1 신규 모두 PASS). 실패 시 해당 태스크로 돌아가 수정.

- [ ] **Step 2: 빌드 확인**

Run: `cd backend && ./gradlew build -x test`
Expected: BUILD SUCCESSFUL.

---

## Self-Review (작성자 점검 결과)

**Spec 커버리지**
- 상품 도메인(상속 SINGLE_TABLE) + CoverageItem → Task 1 ✅
- 종류별 목록 + 필터(보험료 범위·키워드) + DISTINCT → Task 2 ✅
- 비로그인 조회(permitAll) → Task 4 ✅
- 상세 조회 + 보장항목 → Task 3·4 ✅
- 결과 없음(E1) 빈 배열 → Task 2(빈 목록 테스트)·4 ✅
- 잘못된 type 400 / 없는 id 404 / 음수 premium 400 → Task 4 ✅
- 시드(종류별 2개 + 보장항목) 멱등 → Task 5 ✅
- `calculatePremium` 미생성 / `premiumBasis` 별도 필드 없음(description에 녹임) → 전 태스크에서 준수 ✅

**Placeholder 스캔:** 없음(모든 step에 실제 코드/명령/기대값 포함).

**타입 일관성:** `search(Class, Integer, Integer, String)`, `ProductType.entityClass()`, DTO record 시그니처, `ProductSummaryResponse.typeOf()` 재사용 — Task 2~5 전반 일치 확인.
