import { useEffect } from 'react'
import shared from '../../../styles/shared.module.css'
import { useCustomerClaims } from '../hooks/useCustomerClaims'
import { BenefitAnalysisView } from './BenefitAnalysisView'
import { CarAccidentView } from './CarAccidentView'
import { ClaimHistoryView } from './ClaimHistoryView'
import { ClaimStatusView } from './ClaimStatusView'
import { HealthClaimView } from './HealthClaimView'

export type CustomerClaimView = 'health' | 'car' | 'status' | 'history' | 'analysis'

const pageMeta: Record<CustomerClaimView, { eyebrow: string; title: string }> = {
  health: { eyebrow: 'UC05 / UC17', title: '의료보험 청구' },
  car: { eyebrow: 'UC09', title: '자동차사고 접수' },
  status: { eyebrow: 'UC03', title: '보상 처리 현황' },
  history: { eyebrow: 'UC04', title: '보상 이력' },
  analysis: { eyebrow: 'UC11', title: '실익 분석' },
}

export function CustomerClaimsPage({
  token,
  onUnauthorized,
  view,
}: {
  token: string
  onUnauthorized: () => void
  view: CustomerClaimView
}) {
  const state = useCustomerClaims(token, onUnauthorized)

  // 같은 CustomerClaimsPage 인스턴스가 view prop만 바뀌며 재사용되므로(리마운트 X),
  // 보상현황 탭으로 진입할 때마다 최신 데이터를 다시 불러온다.
  const { loadStatusClaims } = state
  useEffect(() => {
    if (view !== 'status') {
      return
    }
    void Promise.resolve().then(() => {
      loadStatusClaims()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view])

  return (
    <section className={shared.page}>
      <div className={shared.pageHeader}>
        <div>
          <span className={shared.eyebrow}>{pageMeta[view].eyebrow}</span>
          <h1>{pageMeta[view].title}</h1>
        </div>
      </div>

      {view === 'health' ? <HealthClaimView state={state} /> : null}
      {view === 'car' ? <CarAccidentView state={state} /> : null}
      {view === 'status' ? <ClaimStatusView state={state} /> : null}
      {view === 'history' ? <ClaimHistoryView state={state} /> : null}
      {view === 'analysis' ? <BenefitAnalysisView state={state} /> : null}
    </section>
  )
}
