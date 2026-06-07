import { Car, Send } from 'lucide-react'
import { formatContractStatus, formatDate } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import type { CustomerClaimsState } from '../hooks/useCustomerClaims'
import styles from '../claims.module.css'

export function CarAccidentView({ state }: { state: CustomerClaimsState }) {
  const {
    carContracts,
    selectedCarContractId,
    setSelectedCarContractId,
    accidentDate,
    setAccidentDate,
    accidentLocation,
    setAccidentLocation,
    accidentType,
    setAccidentType,
    vehicleNumber,
    setVehicleNumber,
    hasInjury,
    setHasInjury,
    injuredCount,
    setInjuredCount,
    accidentAttachments,
    accidentResult,
    accidentError,
    isClaimLoading,
    isAccidentSubmitting,
    changeAccidentAttachments,
    submitAccident,
  } = state

  return (
    <form className={`${shared.panel} ${shared.formPanel}`} onSubmit={submitAccident}>
      <div className={shared.sectionTitle}>
        <Car size={18} />
        <h2>자동차사고 접수</h2>
      </div>
      <label>
        계약
        <select
          disabled={isClaimLoading || carContracts.length === 0}
          value={selectedCarContractId}
          onChange={(event) => setSelectedCarContractId(event.target.value)}
        >
          {carContracts.map((contract) => (
            <option key={contract.contractId} value={contract.contractId}>
              {contract.productName} · {formatDate(contract.startDate)}
            </option>
          ))}
          {carContracts.length === 0 ? <option value="">선택 가능한 계약 없음</option> : null}
        </select>
      </label>
      <label>
        사고 일자
        <input
          required
          type="date"
          value={accidentDate}
          onChange={(event) => setAccidentDate(event.target.value)}
        />
      </label>
      <label>
        사고 장소
        <input required value={accidentLocation} onChange={(event) => setAccidentLocation(event.target.value)} />
      </label>
      <label>
        사고 유형
        <input required value={accidentType} onChange={(event) => setAccidentType(event.target.value)} />
      </label>
      <label>
        차량 번호
        <input required value={vehicleNumber} onChange={(event) => setVehicleNumber(event.target.value)} />
      </label>
      <label className={styles.toggleRow}>
        <input
          checked={hasInjury}
          type="checkbox"
          onChange={(event) => {
            setHasInjury(event.target.checked)
            setInjuredCount(event.target.checked ? '1' : '0')
          }}
        />
        대인사고
      </label>
      {hasInjury ? (
        <label>
          부상자 수
          <input
            min="1"
            required
            type="number"
            value={injuredCount}
            onChange={(event) => setInjuredCount(event.target.value)}
          />
        </label>
      ) : null}
      <label>
        현장 사진/증빙
        <input
          accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
          multiple
          type="file"
          onChange={(event) => changeAccidentAttachments(event.target.files)}
        />
      </label>
      {accidentAttachments.length > 0 ? (
        <div className={styles.attachmentList}>
          {accidentAttachments.map((file) => (
            <span key={`${file.name}-${file.size}`}>
              {file.name} · {(file.size / 1024 / 1024).toFixed(2)}MB
            </span>
          ))}
        </div>
      ) : null}
      {accidentResult ? (
        <p className={shared.formSuccess}>
          자동차사고 접수번호 {accidentResult.reportId}번이 {formatContractStatus(accidentResult.status)} 상태로 접수되었습니다.
        </p>
      ) : null}
      {accidentError ? <p className={shared.formError}>{accidentError}</p> : null}
      <button
        className={shared.primaryButton}
        disabled={isAccidentSubmitting || carContracts.length === 0}
        type="submit"
      >
        {isAccidentSubmitting ? '접수 중' : '접수하기'}
        <Send size={18} />
      </button>
    </form>
  )
}
