import { CalendarDays } from 'lucide-react'
import { formatClaimType, formatContractStatus, formatCurrency, formatDate } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import type { CustomerClaimsState } from '../hooks/useCustomerClaims'
import styles from '../claims.module.css'

export function ClaimHistoryView({ state }: { state: CustomerClaimsState }) {
  const {
    historyClaims,
    historyFrom,
    setHistoryFrom,
    historyTo,
    setHistoryTo,
    hasHistorySearched,
    isQueryLoading,
    isHistoryLoading,
    loadHistory,
  } = state

  return (
    <section className={shared.panel}>
      <div className={shared.sectionTitle}>
        <CalendarDays size={18} />
        <h2>보상 이력</h2>
      </div>
      <p className={styles.sectionNote}>기간을 정한 뒤 조회 버튼을 눌러 보상이력을 불러오세요.</p>
      <form className={shared.inlineFields} onSubmit={loadHistory}>
        <label>
          시작일
          <input type="date" value={historyFrom} onChange={(event) => setHistoryFrom(event.target.value)} />
        </label>
        <label>
          종료일
          <input type="date" value={historyTo} onChange={(event) => setHistoryTo(event.target.value)} />
        </label>
        <button className={shared.secondaryButton} disabled={isHistoryLoading} type="submit">
          {isHistoryLoading ? '조회 중' : '조회'}
        </button>
      </form>
      <div className={styles.claimTable}>
        {historyClaims.length > 0 ? (
          <article className={styles.claimTableHead}>
            <span>청구</span>
            <span>접수일</span>
            <span>청구금액</span>
            <span>지급금액</span>
            <span>상태</span>
          </article>
        ) : null}
        {historyClaims.map((claim) => (
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
        {historyClaims.length === 0 && !isQueryLoading && !isHistoryLoading && hasHistorySearched ? (
          <p>조회 기간의 보상 이력이 없습니다.</p>
        ) : null}
        {!hasHistorySearched && !isHistoryLoading ? <p>조회 버튼을 누르면 보상이력이 표시됩니다.</p> : null}
      </div>
    </section>
  )
}
