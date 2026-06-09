import { AlertTriangle, Bell, Car, CheckCircle2, ClipboardCheck, FileText, HeartPulse, Users } from 'lucide-react'
import { useMemo } from 'react'
import type { ReviewResult } from '../../../lib/types'
import { formatContractStatus } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import { useUnderwritingReviews } from '../hooks/useUnderwritingReviews'
import styles from '../review.module.css'

export function EmployeeReviewsPage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const {
    pendingReviews,
    selectedReview,
    reviewResult,
    setReviewResult,
    comment,
    setComment,
    surchargeRate,
    setSurchargeRate,
    decision,
    error,
    selectReview,
    confirmReview,
  } = useUnderwritingReviews(token, onUnauthorized)

  const metrics = useMemo(
    () => [
      { label: '대기 신청', value: String(pendingReviews.length), icon: ClipboardCheck },
      {
        label: '자동차 심사',
        value: String(
          pendingReviews.filter(
            (review) => review.productName.includes('드라이브') || review.productName.includes('자동차'),
          ).length,
        ),
        icon: Bell,
      },
      { label: '선택 건', value: selectedReview ? '1' : '0', icon: Users },
    ],
    [pendingReviews, selectedReview],
  )

  return (
    <section className={shared.page}>
      <div className={shared.pageHeader}>
        <div>
          <span className={shared.eyebrow}>UC12 / UC13 / UC15</span>
          <h1>심사 대기 목록</h1>
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
            <ClipboardCheck size={18} />
            <h2>대기 건</h2>
          </div>
          <div className={shared.reviewList}>
            {pendingReviews.map((review) => (
              <button
                aria-pressed={review.applicationId === selectedReview?.applicationId}
                className={review.applicationId === selectedReview?.applicationId ? shared.isSelected : ''}
                key={review.applicationId}
                type="button"
                onClick={() => selectReview(review.applicationId)}
              >
                <strong>신청-{review.applicationId}</strong>
                <span>{review.productName} · {review.applicantName}</span>
                <small>{new Date(review.appliedAt).toLocaleString()} · {review.basePremium.toLocaleString()}원</small>
              </button>
            ))}
            {pendingReviews.length === 0 ? <p>심사 대기 건이 없습니다.</p> : null}
          </div>
        </section>

        <form className={shared.panel} onSubmit={confirmReview}>
          <div className={shared.sectionTitle}>
            <FileText size={18} />
            <h2>심사 상세</h2>
          </div>
          {selectedReview ? (
            <article className={shared.detailCard}>
              <span className={shared.badge}>신청 {selectedReview.applicationId}</span>
              <h3>{selectedReview.applicantName}</h3>
              <p>{selectedReview.productName} · 기본 보험료 {selectedReview.basePremium.toLocaleString()}원</p>
              <p>생년월일 {selectedReview.birthDate} · 주민등록번호 {selectedReview.ssn}</p>
              {selectedReview.medicalHistory ? (
                <div className={styles.riskBox}>
                  <HeartPulse size={18} />
                  <span>
                    현재 병력 {selectedReview.medicalHistory.currentConditions} · 입원 이력 {selectedReview.medicalHistory.pastHospitalization}
                  </span>
                </div>
              ) : null}
              {selectedReview.vehicleInfo ? (
                <div className={styles.riskBox}>
                  <Car size={18} />
                  <span>
                    {selectedReview.vehicleInfo.plateNumber} · {selectedReview.vehicleInfo.vehicleType} · {selectedReview.vehicleInfo.modelYear}년식
                  </span>
                </div>
              ) : null}
              {selectedReview.accidentHistory ? (
                <div className={styles.riskBox}>
                  <AlertTriangle size={18} />
                  <span>
                    사고 {selectedReview.accidentHistory.accidentCount}건 · 지급 {selectedReview.accidentHistory.totalPaidAmount.toLocaleString()}원 · 면허 {selectedReview.accidentHistory.licenseStatus}
                  </span>
                </div>
              ) : null}
              <label>
                심사 결과
                <select value={reviewResult} onChange={(event) => setReviewResult(event.target.value as ReviewResult)}>
                  <option value="APPROVED">승인</option>
                  <option value="CONDITIONAL">조건부 승인</option>
                  <option value="REJECTED">거절</option>
                </select>
              </label>
              {reviewResult === 'CONDITIONAL' ? (
                <label>
                  할증률
                  <input value={surchargeRate} onChange={(event) => setSurchargeRate(event.target.value)} />
                </label>
              ) : null}
              <label>
                심사 의견
                <input value={comment} onChange={(event) => setComment(event.target.value)} />
              </label>
              {decision ? (
                <p className={shared.formSuccess}>
                  심사 {decision.reviewId}번 {formatContractStatus(decision.result)} · 최종 월 보험료 {decision.adjustedPremium.toLocaleString()}원
                </p>
              ) : null}
              {error ? <p className={shared.formError}>{error}</p> : null}
              <button className={shared.primaryButton} type="submit">
                심사 확정
                <CheckCircle2 size={18} />
              </button>
            </article>
          ) : (
            <p>심사 건을 선택하세요.</p>
          )}
        </form>
      </div>
    </section>
  )
}
