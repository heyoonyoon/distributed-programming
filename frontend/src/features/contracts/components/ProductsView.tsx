import { ArrowRight, CheckCircle2, FileText, Search } from 'lucide-react'
import { formatProductType } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import type { CustomerContractsState } from '../hooks/useCustomerContracts'
import styles from '../contracts.module.css'

export function ProductsView({ state }: { state: CustomerContractsState }) {
  const {
    productType,
    keyword,
    setKeyword,
    minPremium,
    setMinPremium,
    maxPremium,
    setMaxPremium,
    products,
    selectedProduct,
    hasSearchedProducts,
    isLoading,
    error,
    success,
    loadProducts,
    changeProductType,
    selectProduct,
    submitApplication,
  } = state

  return (
    <div className={shared.splitLayout}>
      <section className={shared.panel}>
        <div className={shared.sectionTitle}>
          <Search size={18} />
          <h2>보험 상품 조회</h2>
        </div>
        <form
          className={styles.filterForm}
          onSubmit={(event) => {
            event.preventDefault()
            loadProducts()
          }}
        >
          <div className={shared.accountSwitch}>
            <button
              aria-pressed={productType === 'HEALTH'}
              className={productType === 'HEALTH' ? shared.isSelected : ''}
              type="button"
              onClick={() => changeProductType('HEALTH')}
            >
              의료보험
            </button>
            <button
              aria-pressed={productType === 'CAR'}
              className={productType === 'CAR' ? shared.isSelected : ''}
              type="button"
              onClick={() => changeProductType('CAR')}
            >
              자동차보험
            </button>
          </div>
          <label>
            검색어
            <input value={keyword} onChange={(event) => setKeyword(event.target.value)} />
          </label>
          <div className={shared.inlineFields}>
            <label>
              최소 보험료
              <input value={minPremium} onChange={(event) => setMinPremium(event.target.value)} />
            </label>
            <label>
              최대 보험료
              <input value={maxPremium} onChange={(event) => setMaxPremium(event.target.value)} />
            </label>
          </div>
          <button className={shared.secondaryButton} type="submit">
            필터 적용
          </button>
        </form>
        <div className={styles.productTabs}>
          {isLoading ? <p>상품을 불러오는 중입니다.</p> : null}
          {!isLoading && !hasSearchedProducts ? (
            <p>조건을 선택한 뒤 필터 적용을 눌러 조회하세요.</p>
          ) : null}
          {products.map((product) => (
            <button
              aria-pressed={product.id === selectedProduct?.id}
              className={product.id === selectedProduct?.id ? shared.isSelected : ''}
              key={product.id}
              type="button"
              onClick={() => selectProduct(product.id)}
            >
              {product.productName}
              <span>{product.coverageSummary} · {product.monthlyPremium.toLocaleString()}원</span>
            </button>
          ))}
          {hasSearchedProducts && products.length === 0 && !isLoading ? (
            <p>조회된 상품이 없습니다.</p>
          ) : null}
        </div>
      </section>

      <form
        className={`${shared.panel} ${shared.formPanel}`}
        onSubmit={submitApplication}
      >
        <div className={shared.sectionTitle}>
          <FileText size={18} />
          <h2>상품 상세 및 가입 신청</h2>
        </div>
        {selectedProduct ? (
          <>
            <article className={shared.detailCard}>
              <span className={shared.badge}>{formatProductType(selectedProduct.productType)}</span>
              <h3>{selectedProduct.productName}</h3>
              <p>{selectedProduct.description}</p>
              <strong>{selectedProduct.monthlyPremium.toLocaleString()}원 / 월</strong>
              <ul>
                {selectedProduct.coverageItems.map((item) => (
                  <li key={item.itemName}>
                    <CheckCircle2 size={16} />
                    {item.itemName} · 한도 {item.coverageLimit.toLocaleString()}원 · 자기부담 {item.deductible.toLocaleString()}원
                  </li>
                ))}
              </ul>
            </article>
            {selectedProduct.productType === 'HEALTH' ? (
              <>
                <label>
                  현재 병력
                  <input name="currentConditions" defaultValue="없음" />
                </label>
                <label>
                  과거 입원 이력
                  <input name="pastHospitalization" defaultValue="없음" />
                </label>
                <label>
                  복용 중인 약물
                  <input name="medications" defaultValue="없음" />
                </label>
              </>
            ) : (
              <>
                <label>
                  차량번호
                  <input name="plateNumber" defaultValue="12가3456" />
                </label>
                <label>
                  차종
                  <input name="vehicleType" defaultValue="승용차" />
                </label>
                <div className={shared.inlineFields}>
                  <label>
                    연식
                    <input name="modelYear" defaultValue="2020" />
                  </label>
                  <label>
                    운전경력
                    <input name="drivingExperienceYears" defaultValue="5" />
                  </label>
                </div>
              </>
            )}
            <button className={shared.primaryButton} type="submit">
              가입 신청
              <ArrowRight size={18} />
            </button>
          </>
        ) : (
          <p>상품을 먼저 조회하고 선택하세요.</p>
        )}
        {error ? <p className={shared.formError}>{error}</p> : null}
        {success ? <p className={shared.formSuccess}>{success}</p> : null}
      </form>
    </div>
  )
}
