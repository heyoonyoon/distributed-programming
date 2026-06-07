import {
  ArrowRight,
  BadgeCheck,
  CalendarDays,
  Car,
  ClipboardCheck,
  CreditCard,
  HeartPulse,
  Receipt,
  Search,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import shared from '../../../styles/shared.module.css'
import { useContractSummary } from '../hooks/useContractSummary'
import styles from '../home.module.css'

const actions = [
  { label: '상품 조회 및 가입 신청', icon: Search, path: '/customer/products' },
  { label: '신청 내역', icon: Receipt, path: '/customer/applications' },
  { label: '의료보험 청구', icon: HeartPulse, path: '/customer/claims/health' },
  { label: '자동차사고 접수', icon: Car, path: '/customer/claims/car-accident' },
  { label: '보상 현황', icon: ClipboardCheck, path: '/customer/claims/status' },
  { label: '보상 이력', icon: CalendarDays, path: '/customer/claims/history' },
  { label: '보험료 납부', icon: CreditCard, path: '/customer/contracts' },
]

export function CustomerHomePage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const summary = useContractSummary(token, onUnauthorized)

  return (
    <section className={styles.customerHome}>
      <div className={styles.customerHero}>
        <span className={shared.eyebrow}>가입자 포털</span>
        <h1>고객님, 필요한 보험 업무를 바로 처리하세요.</h1>
        <p>계약 확인, 청구 접수, 보험료 납부를 고객용 화면에서 진행합니다.</p>
      </div>

      <div className={styles.actionGrid}>
        {actions.map(({ label, icon: Icon, path }) => (
          <Link className={styles.actionTile} to={path} key={label}>
            <Icon size={22} />
            <span>{label}</span>
            <ArrowRight size={18} />
          </Link>
        ))}
      </div>

      <section className={shared.statusPanel}>
        <div>
          <BadgeCheck size={22} />
          <h2>
            {summary ? `현재 유효한 계약 ${summary.total}건` : '현재 유효한 계약을 불러오는 중입니다.'}
          </h2>
          <p>
            {summary
              ? summary.total > 0
                ? `의료보험 ${summary.health}건, 자동차보험 ${summary.car}건이 정상 유지 중입니다.`
                : '현재 유효한 계약이 없습니다.'
              : '가입자 계약 현황을 계산하고 있습니다.'}
          </p>
        </div>
        <Link className={shared.secondaryButton} to="/customer/contracts">
          계약 보기
        </Link>
      </section>
    </section>
  )
}
