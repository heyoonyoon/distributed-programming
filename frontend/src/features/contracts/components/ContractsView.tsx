import { AlertTriangle, CheckCircle2, CreditCard, Download, Receipt } from 'lucide-react'
import type { PaymentMethod } from '../../../lib/types'
import { formatContractStatus, formatCurrency, formatDate, formatProductType } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import type { CustomerContractsState } from '../hooks/useCustomerContracts'
import styles from '../contracts.module.css'

export function ContractsView({ state }: { state: CustomerContractsState }) {
  const {
    contracts,
    selectedContract,
    selectedUnpaid,
    unpaidContracts,
    paymentMethod,
    setPaymentMethod,
    paymentInfo,
    setPaymentInfo,
    autoDebitAccount,
    setAutoDebitAccount,
    withdrawalDay,
    setWithdrawalDay,
    isContractLoading,
    isPaymentSubmitting,
    isAutoDebitSubmitting,
    selectedPayable,
    loadContractDetail,
    downloadContract,
    submitPayment,
    submitAutoDebit,
  } = state

  return (
    <section className={shared.panel}>
      <div className={shared.sectionTitle}>
        <Download size={18} />
        <h2>내 계약 및 보험료 납부</h2>
      </div>
      {isContractLoading ? <p>계약 정보를 불러오는 중입니다.</p> : null}
      <div className={styles.contractWorkspace}>
        <div className={`${shared.customerList} ${shared.compact}`}>
          {contracts.map((contract) => (
            <article
              className={contract.contractId === selectedContract?.contractId ? shared.isSelected : ''}
              key={contract.contractId}
            >
              <Receipt size={20} />
              <div>
                <strong>{contract.productName}</strong>
                <span>
                  {formatDate(contract.startDate)} - {formatDate(contract.endDate)} · 월{' '}
                  {formatCurrency(contract.monthlyPremium)}
                </span>
                <small>
                  {formatProductType(contract.productType)} · {formatContractStatus(contract.status)}
                </small>
              </div>
              <button
                aria-pressed={contract.contractId === selectedContract?.contractId}
                className={shared.secondaryButton}
                type="button"
                onClick={() => loadContractDetail(contract.contractId)}
              >
                상세보기
              </button>
            </article>
          ))}
          {contracts.length === 0 ? <p>유효한 계약이 없습니다.</p> : null}
        </div>

        {selectedContract ? (
          <>
            <div className={styles.contractDetailGrid}>
              <section className={styles.paymentBox}>
                <div>
                  <span className={shared.badge}>계약 상세</span>
                  <h3>{selectedContract.productName}</h3>
                  <p>
                    계약번호 {selectedContract.contractId} · 납부 방법{' '}
                    {formatContractStatus(selectedContract.paymentMethod)}
                  </p>
                </div>
                <ul className={styles.coverageList}>
                  {selectedContract.coverageItems.map((item) => (
                    <li key={item.itemName}>
                      <CheckCircle2 size={16} />
                      {item.itemName} · 한도 {formatCurrency(item.coverageLimit)} · 자기부담{' '}
                      {formatCurrency(item.deductible)}
                    </li>
                  ))}
                </ul>
                <button
                  className={shared.secondaryButton}
                  type="button"
                  onClick={() => downloadContract(selectedContract.contractId)}
                >
                  계약서 다운로드
                  <Download size={16} />
                </button>
              </section>

              <section className={styles.paymentBox}>
                <div>
                  <span className={shared.badge}>미납/연체</span>
                  <h3>
                    {selectedUnpaid ? formatCurrency(selectedUnpaid.unpaidPrincipal) : '연체 없음'}
                  </h3>
                  <p>
                    {selectedUnpaid
                      ? `기한 ${formatDate(selectedUnpaid.dueDate)} · ${selectedUnpaid.overdueDays}일 연체 · 이자 ${formatCurrency(selectedUnpaid.overdueInterest)}`
                      : '선택한 계약에 표시할 연체 내역이 없습니다.'}
                  </p>
                </div>
                <div className={styles.unpaidStrip}>
                  {unpaidContracts.map((contract) => (
                    <button
                      type="button"
                      key={contract.contractId}
                      onClick={() => loadContractDetail(contract.contractId)}
                    >
                      <AlertTriangle size={16} />
                      <span>{contract.productName}</span>
                      <strong>{formatCurrency(contract.unpaidPrincipal)}</strong>
                    </button>
                  ))}
                  {unpaidContracts.length === 0 ? <span>전체 연체 내역 없음</span> : null}
                </div>
              </section>
            </div>
            <div className={styles.contractDetailGrid}>
              <form className={styles.paymentBox} onSubmit={submitPayment}>
                <div>
                  <span className={shared.badge}>보험료 납부</span>
                  <h3>한 회차 납부</h3>
                  <p>
                    {selectedPayable
                      ? `납부 예정 ${formatCurrency(selectedPayable.amount)}`
                      : '현재 납부할 미납 회차가 없습니다.'}
                  </p>
                </div>
                <div className={shared.inlineFields}>
                  <label>
                    납부 방법
                    <select
                      value={paymentMethod}
                      onChange={(event) => setPaymentMethod(event.target.value as PaymentMethod)}
                    >
                      <option value="CARD">카드</option>
                      <option value="TRANSFER">계좌이체</option>
                      <option value="AUTO_DEBIT">자동이체</option>
                    </select>
                  </label>
                  <label>
                    결제 정보
                    <input
                      value={paymentInfo}
                      onChange={(event) => setPaymentInfo(event.target.value)}
                    />
                  </label>
                </div>
                <button className={shared.primaryButton} disabled={isPaymentSubmitting} type="submit">
                  {isPaymentSubmitting ? '처리 중' : '납부하기'}
                  <CreditCard size={18} />
                </button>
              </form>

              <form className={styles.paymentBox} onSubmit={submitAutoDebit}>
                <div>
                  <span className={shared.badge}>자동이체</span>
                  <h3>출금 계좌 등록</h3>
                  <p>등록 후 계약 상세의 납부 방법이 자동이체로 표시됩니다.</p>
                </div>
                <div className={shared.inlineFields}>
                  <label>
                    계좌번호
                    <input
                      value={autoDebitAccount}
                      onChange={(event) => setAutoDebitAccount(event.target.value)}
                    />
                  </label>
                  <label>
                    출금일
                    <input
                      min="1"
                      max="31"
                      type="number"
                      value={withdrawalDay}
                      onChange={(event) => setWithdrawalDay(event.target.value)}
                    />
                  </label>
                </div>
                <button
                  className={shared.secondaryButton}
                  disabled={isAutoDebitSubmitting}
                  type="submit"
                >
                  {isAutoDebitSubmitting ? '등록 중' : '자동이체 등록'}
                </button>
              </form>
            </div>
          </>
        ) : (
          <section className={styles.paymentBox}>
            <div>
              <span className={shared.badge}>계약 선택</span>
              <h3>계약을 선택하세요</h3>
              <p>계약 목록에서 상세보기를 누르면 계약 상세, 미납/연체, 납부 기능이 표시됩니다.</p>
            </div>
          </section>
        )}
      </div>
    </section>
  )
}
