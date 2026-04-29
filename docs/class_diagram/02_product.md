# 보험 상품 클래스

### InsuranceProduct (abstract)
```
[InsuranceProduct] - abstract
──────────────────────────────
# productId   : String
# productName : String
# description : String
# basePremium : int
──────────────────────────────
+ calculatePremium(holder: Policyholder) : int   // 추상 메서드
+ getProductInfo() : ProductInfo
```

### CoverageItem (Composition with InsuranceProduct)
```
[CoverageItem]
──────────────────────────────
- itemId        : String
- itemName      : String
- coverageLimit : int
- deductible    : int
```

### HealthInsuranceProduct extends InsuranceProduct
```
[HealthInsuranceProduct] extends [InsuranceProduct]
──────────────────────────────
- maxHospitalizationDays : int
──────────────────────────────
+ calculatePremium(holder: Policyholder) : int   // 나이·병력 기반
```

### CarInsuranceProduct extends InsuranceProduct
```
[CarInsuranceProduct] extends [InsuranceProduct]
──────────────────────────────
- vehicleType     : String
- driverScopeType : String
──────────────────────────────
+ calculatePremium(holder: Policyholder) : int   // 차량정보·사고이력 기반
```