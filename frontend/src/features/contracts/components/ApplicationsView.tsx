import { FileText, Receipt } from 'lucide-react'
import { formatContractStatus } from '../../../utils/format'
import shared from '../../../styles/shared.module.css'
import type { CustomerContractsState } from '../hooks/useCustomerContracts'

export function ApplicationsView({ state }: { state: CustomerContractsState }) {
  const { applications, cancelApplication } = state

  return (
    <section className={shared.panel}>
      <div className={shared.sectionTitle}>
        <Receipt size={18} />
        <h2>내 가입 신청</h2>
      </div>
      <div className={`${shared.customerList} ${shared.compact}`}>
        {applications.map((application) => (
          <article key={application.applicationId}>
            <FileText size={20} />
            <div>
              <strong>{application.productName}</strong>
              <span>신청번호 {application.applicationId} · {formatContractStatus(application.status)}</span>
              <small>{new Date(application.appliedAt).toLocaleString()}</small>
            </div>
            {application.status === 'PENDING' ? (
              <button
                className={shared.secondaryButton}
                type="button"
                onClick={() => cancelApplication(application.applicationId)}
              >
                취소
              </button>
            ) : null}
          </article>
        ))}
        {applications.length === 0 ? <p>가입 신청 내역이 없습니다.</p> : null}
      </div>
    </section>
  )
}
