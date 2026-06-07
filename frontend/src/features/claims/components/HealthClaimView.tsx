import { FileText, HeartPulse, Receipt, Send } from 'lucide-react'
import { formatContractStatus, formatCurrency, formatDate } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import type { CustomerClaimsState } from '../hooks/useCustomerClaims'
import { describeHealthClaimResult } from '../utils'
import styles from '../claims.module.css'

export function HealthClaimView({ state }: { state: CustomerClaimsState }) {
  const {
    healthContracts,
    selectedHealthContractId,
    setSelectedHealthContractId,
    hospitalName,
    setHospitalName,
    diagnosisCode,
    setDiagnosisCode,
    treatmentDate,
    setTreatmentDate,
    requestAmount,
    setRequestAmount,
    receiptAmount,
    setReceiptAmount,
    healthAttachments,
    claimResult,
    claimError,
    isClaimLoading,
    isClaimSubmitting,
    changeHealthAttachments,
    submitClaim,
  } = state

  return (
    <>
      <form className={`${shared.panel} ${shared.formPanel}`} onSubmit={submitClaim}>
        <div className={shared.sectionTitle}>
          <HeartPulse size={18} />
          <h2>청구 신청</h2>
        </div>
        <label>
          계약
          <select
            disabled={isClaimLoading || healthContracts.length === 0}
            value={selectedHealthContractId}
            onChange={(event) => setSelectedHealthContractId(event.target.value)}
          >
            {healthContracts.map((contract) => (
              <option key={contract.contractId} value={contract.contractId}>
                {contract.productName} · {formatDate(contract.startDate)} · 월{' '}
                {formatCurrency(contract.monthlyPremium)}
              </option>
            ))}
            {healthContracts.length === 0 ? <option value="">선택 가능한 계약 없음</option> : null}
          </select>
        </label>
        <label>
          병원명
          <input required value={hospitalName} onChange={(event) => setHospitalName(event.target.value)} />
        </label>
        <label>
          진단코드
          <input required value={diagnosisCode} onChange={(event) => setDiagnosisCode(event.target.value)} />
        </label>
        <label>
          진료일
          <input
            required
            type="date"
            value={treatmentDate}
            onChange={(event) => setTreatmentDate(event.target.value)}
          />
        </label>
        <div className={shared.inlineFields}>
          <label>
            청구 금액
            <input
              min="1"
              required
              type="number"
              value={requestAmount}
              onChange={(event) => setRequestAmount(event.target.value)}
            />
          </label>
          <label>
            영수증 금액
            <input
              min="1"
              required
              type="number"
              value={receiptAmount}
              onChange={(event) => setReceiptAmount(event.target.value)}
            />
          </label>
        </div>
        <label>
          증빙 첨부
          <input
            accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
            multiple
            type="file"
            onChange={(event) => changeHealthAttachments(event.target.files)}
          />
        </label>
        {healthAttachments.length > 0 ? (
          <div className={styles.attachmentList}>
            {healthAttachments.map((file) => (
              <span key={`${file.name}-${file.size}`}>
                {file.name} · {(file.size / 1024 / 1024).toFixed(2)}MB
              </span>
            ))}
          </div>
        ) : null}
        {claimError ? <p className={shared.formError}>{claimError}</p> : null}
        <button
          className={shared.primaryButton}
          disabled={isClaimSubmitting || healthContracts.length === 0}
          type="submit"
        >
          {isClaimSubmitting ? '접수 중' : '청구 신청'}
          <Send size={18} />
        </button>
      </form>
      <section className={`${shared.panel} ${styles.resultPanel}`}>
        <div className={shared.sectionTitle}>
          <Receipt size={18} />
          <h2>청구 결과</h2>
        </div>
        {claimResult ? (
          <article className={`${styles.claimResult} ${claimResult.status.toLowerCase()}`}>
            <span className={shared.badge}>{formatContractStatus(claimResult.complexity)}</span>
            <div>
              <h3>청구번호 {claimResult.claimId}</h3>
              <p>{describeHealthClaimResult(claimResult)}</p>
            </div>
            <strong>{formatContractStatus(claimResult.status)}</strong>
          </article>
        ) : (
          <div className={styles.emptyResult}>
            <FileText size={28} />
            <p>청구 신청 후 결과가 표시됩니다.</p>
          </div>
        )}
        <div className={styles.claimRuleGrid}>
          <article>
            <strong>간편 청구</strong>
            <span>1,000,000원 미만 · 즉시지급</span>
          </article>
          <article>
            <strong>심사 청구</strong>
            <span>1,000,000원 이상 · 심사대기</span>
          </article>
        </div>
      </section>
    </>
  )
}
