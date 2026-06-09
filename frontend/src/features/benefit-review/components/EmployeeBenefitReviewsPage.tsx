import { AlertTriangle, CheckCircle2, ClipboardCheck, FileText, Receipt } from 'lucide-react'
import { useMemo } from 'react'
import type { BenefitReviewResult } from '../../../lib/types'
import { formatClaimType, formatContractStatus, formatCurrency } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import { useBenefitReviews } from '../hooks/useBenefitReviews'
import styles from '../benefit.module.css'

export function EmployeeBenefitReviewsPage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const {
    reviews,
    unassigned,
    selectedReview,
    reviewResult,
    setReviewResult,
    comment,
    setComment,
    payoutAmount,
    setPayoutAmount,
    decision,
    statusMessage,
    error,
    isLoading,
    isSubmitting,
    isRetrying,
    assigningClaimId,
    canRetry,
    selectBenefitReview,
    confirmBenefitReview,
    retryPayout,
    assignToMe,
  } = useBenefitReviews(token, onUnauthorized)

  const metrics = useMemo(
    () => [
      { label: '배정 심사', value: String(reviews.length), icon: ClipboardCheck },
      {
        label: '지급 실패',
        value: String(reviews.filter((review) => review.claimStatus === 'FAILED').length),
        icon: AlertTriangle,
      },
      { label: '선택 청구', value: selectedReview ? `#${selectedReview.claimId}` : '-', icon: Receipt },
    ],
    [reviews, selectedReview],
  )

  return (
    <section className={shared.page}>
      <div className={shared.pageHeader}>
        <div>
          <span className={shared.eyebrow}>UC12 / UC14 / UC17</span>
          <h1>보험금 지급 심사</h1>
        </div>
      </div>

      <div className={shared.metricGrid}>
        {metrics.map(({ label, value, icon: Icon }) => (
          <article className={shared.metricCard} key={label}>
            <Icon size={20} />
            <strong>{value}</strong>
            <span>{label}</span>
          </article>
        ))}
      </div>

      <div className={shared.splitLayout}>
        <section className={shared.panel}>
          <div className={shared.sectionTitle}>
            <AlertTriangle size={18} />
            <h2>미배정 자동차사고</h2>
          </div>
          <p className={styles.sectionNote}>
            미배정 사고접수 건입니다. [나에게 배정]을 누르면 내 심사 큐로 들어옵니다.
          </p>
          <div className={shared.reviewList}>
            {unassigned.map((item) => (
              <article key={item.claimId} className={styles.reviewRow}>
                <div>
                  <strong>{formatClaimType(item.claimType)}-{item.claimId}</strong>
                  <span>
                    {item.accidentType ? `${item.accidentType} · ` : ''}
                    {formatCurrency(item.requestAmount)}
                  </span>
                  <small>{formatContractStatus(item.claimStatus)}</small>
                </div>
                <button
                  className={shared.secondaryButton}
                  type="button"
                  disabled={assigningClaimId === item.claimId}
                  onClick={() => assignToMe(item.claimId)}
                >
                  {assigningClaimId === item.claimId ? '배정 중' : '나에게 배정'}
                </button>
              </article>
            ))}
            {unassigned.length === 0 ? <p>미배정 건이 없습니다.</p> : null}
          </div>
        </section>

        <section className={shared.panel}>
          <div className={shared.sectionTitle}>
            <ClipboardCheck size={18} />
            <h2>배정된 심사</h2>
          </div>
          {isLoading ? <p>보험금 심사 목록을 불러오는 중입니다.</p> : null}
          <div className={shared.reviewList}>
            {reviews.map((review) => (
              <button
                aria-pressed={review.claimId === selectedReview?.claimId}
                className={review.claimId === selectedReview?.claimId ? shared.isSelected : ''}
                key={review.claimId}
                type="button"
                onClick={() => selectBenefitReview(review.claimId)}
              >
                <strong>청구-{review.claimId}</strong>
                <span>{review.hospitalName} · {formatCurrency(review.requestAmount)}</span>
                <small>{formatContractStatus(review.claimStatus)}</small>
              </button>
            ))}
            {reviews.length === 0 && !isLoading ? <p>배정된 심사 건이 없습니다.</p> : null}
          </div>
        </section>

        <form className={shared.panel} onSubmit={confirmBenefitReview}>
          <div className={shared.sectionTitle}>
            <FileText size={18} />
            <h2>심사 상세</h2>
          </div>
          {selectedReview ? (
            <article className={shared.detailCard}>
              <span className={shared.badge}>{formatContractStatus(selectedReview.claimStatus)}</span>
              <h3>청구번호 {selectedReview.claimId}</h3>
              <p>
                {selectedReview.hospitalName} · 진단코드 {selectedReview.diagnosisCode}
              </p>
              <p>
                청구금액 {formatCurrency(selectedReview.requestAmount)} · 담당자{' '}
                {selectedReview.assignedStaffId}
              </p>
              <label>
                심사 결과
                <select
                  value={reviewResult}
                  onChange={(event) => setReviewResult(event.target.value as BenefitReviewResult)}
                >
                  <option value="APPROVED">승인</option>
                  <option value="REJECTED">거절</option>
                </select>
              </label>
              {reviewResult === 'APPROVED' ? (
                <label>
                  지급금액
                  <input
                    type="number"
                    min="1"
                    value={payoutAmount}
                    onChange={(event) => setPayoutAmount(event.target.value)}
                  />
                </label>
              ) : null}
              <label>
                심사 의견
                <input value={comment} onChange={(event) => setComment(event.target.value)} />
              </label>
              <div className={styles.buttonRow}>
                <button className={shared.primaryButton} disabled={isSubmitting} type="submit">
                  {isSubmitting ? '확정 중' : '심사 확정'}
                  <CheckCircle2 size={18} />
                </button>
                <button
                  className={shared.secondaryButton}
                  disabled={isRetrying || !canRetry}
                  type="button"
                  onClick={retryPayout}
                >
                  {isRetrying ? '재시도 중' : '지급 재시도'}
                </button>
              </div>
            </article>
          ) : (
            <p>심사 건을 선택하세요.</p>
          )}
          {decision ? (
            <p className={decision.claimStatus === 'FAILED' ? shared.formError : shared.formSuccess}>
              청구 {decision.claimId}번 {formatContractStatus(decision.result)} · {statusMessage}
            </p>
          ) : null}
          {statusMessage && !decision ? <p className={shared.formSuccess}>{statusMessage}</p> : null}
          {error ? <p className={shared.formError}>{error}</p> : null}
        </form>
      </div>
    </section>
  )
}
