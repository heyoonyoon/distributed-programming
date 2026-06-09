import { BadgeCheck } from 'lucide-react'
import { formatCurrency, formatProductType } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import type { CustomerClaimsState } from '../hooks/useCustomerClaims'
import styles from '../claims.module.css'

export function BenefitAnalysisView({ state }: { state: CustomerClaimsState }) {
  const {
    contracts,
    selectedAnalysisContractId,
    setSelectedAnalysisContractId,
    benefitAnalysis,
    isAnalysisLoading,
    queryError,
    loadBenefitAnalysis,
  } = state

  return (
    <section className={shared.panel}>
      <div className={shared.sectionTitle}>
        <BadgeCheck size={18} />
        <h2>실익 분석</h2>
      </div>
      <p className={styles.sectionNote}>계약 유지 6개월 이후에만 조회할 수 있습니다.</p>
      <form className={styles.analysisForm} onSubmit={loadBenefitAnalysis}>
        <label>
          계약
          <select
            value={selectedAnalysisContractId}
            onChange={(event) => setSelectedAnalysisContractId(event.target.value)}
          >
            {contracts.map((contract) => (
              <option key={contract.contractId} value={contract.contractId}>
                {contract.productName} · {formatProductType(contract.productType)}
              </option>
            ))}
            {contracts.length === 0 ? <option value="">선택 가능한 계약 없음</option> : null}
          </select>
        </label>
        <button className={shared.secondaryButton} disabled={isAnalysisLoading || contracts.length === 0} type="submit">
          {isAnalysisLoading ? '분석 중' : '분석'}
        </button>
      </form>
      {benefitAnalysis ? (
        <div className={styles.analysisGrid}>
          <article>
            <span>총납입</span>
            <strong>{formatCurrency(benefitAnalysis.totalPaidPremium)}</strong>
          </article>
          <article>
            <span>총수령</span>
            <strong>{formatCurrency(benefitAnalysis.totalReceivedBenefit)}</strong>
          </article>
          <article>
            <span>실익</span>
            <strong>{formatCurrency(benefitAnalysis.profit)}</strong>
          </article>
          <article>
            <span>실익률</span>
            <strong>{benefitAnalysis.profitRate.toFixed(2)}</strong>
          </article>
        </div>
      ) : null}
      {queryError ? <p className={shared.formError}>{queryError}</p> : null}
    </section>
  )
}
