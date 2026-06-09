import { ClipboardCheck } from 'lucide-react'
import { formatClaimType, formatContractStatus, formatCurrency, formatDate } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import type { CustomerClaimsState } from '../hooks/useCustomerClaims'
import styles from '../claims.module.css'

export function ClaimStatusView({ state }: { state: CustomerClaimsState }) {
  const { statusClaims, isQueryLoading } = state

  return (
    <section className={shared.panel}>
      <div className={shared.sectionTitle}>
        <ClipboardCheck size={18} />
        <h2>보상 처리 현황</h2>
      </div>
      {isQueryLoading ? <p>보상 현황을 불러오는 중입니다.</p> : null}
      <div className={styles.claimTable}>
        {statusClaims.length > 0 ? (
          <article className={styles.claimTableHead}>
            <span>청구</span>
            <span>접수일</span>
            <span>청구금액</span>
            <span>지급금액</span>
            <span>상태</span>
          </article>
        ) : null}
        {statusClaims.map((claim) => (
          <article key={`${claim.claimType}-${claim.claimId}`}>
            <strong>{formatClaimType(claim.claimType)}-{claim.claimId}</strong>
            <span>{formatDate(claim.claimDate)}</span>
            <span>{formatCurrency(claim.requestAmount)}</span>
            <span>{formatCurrency(claim.paidAmount)}</span>
            <span className={`${styles.statusBadge} ${claim.status.toLowerCase()}`}>
              {formatContractStatus(claim.status)}
            </span>
          </article>
        ))}
        {statusClaims.length === 0 && !isQueryLoading ? <p>진행 중인 보상 건이 없습니다.</p> : null}
      </div>
    </section>
  )
}
