import shared from '../../../styles/shared.module.css'
import { useCustomerContracts } from '../hooks/useCustomerContracts'
import { ApplicationsView } from './ApplicationsView'
import { ContractsView } from './ContractsView'
import { ProductsView } from './ProductsView'

export type CustomerContractView = 'products' | 'applications' | 'contracts'

const pageMeta: Record<CustomerContractView, { eyebrow: string; title: string }> = {
  products: { eyebrow: 'UC01 / UC02', title: '보험 상품 조회 및 가입 신청' },
  applications: { eyebrow: 'UC02', title: '가입 신청 내역' },
  contracts: { eyebrow: 'A-2 / A-3', title: '내 계약 및 보험료 납부' },
}

export function CustomerContractsPage({
  token,
  onUnauthorized,
  view,
}: {
  token: string
  onUnauthorized: () => void
  view: CustomerContractView
}) {
  const state = useCustomerContracts(token, onUnauthorized)

  return (
    <section className={shared.page}>
      <div className={shared.pageHeader}>
        <div>
          <span className={shared.eyebrow}>{pageMeta[view].eyebrow}</span>
          <h1>{pageMeta[view].title}</h1>
        </div>
      </div>

      {view === 'products' ? <ProductsView state={state} /> : null}
      {view === 'applications' ? <ApplicationsView state={state} /> : null}
      {view === 'contracts' ? <ContractsView state={state} /> : null}
    </section>
  )
}
