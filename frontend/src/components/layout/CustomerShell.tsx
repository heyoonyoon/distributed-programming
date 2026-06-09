import { LogOut, ShieldCheck } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, NavLink, Navigate } from 'react-router-dom'
import { homePath } from '../../lib/session'
import type { AuthSession } from '../../lib/types'
import shared from '../../styles/shared.module.css'
import styles from './layout.module.css'

export function CustomerShell({
  session,
  onLogout,
  children,
}: {
  session: AuthSession
  onLogout: () => void
  children: ReactNode
}) {
  if (session.userType !== 'POLICYHOLDER') {
    return <Navigate to={homePath(session)} replace />
  }

  return (
    <div className={styles.customerShell}>
      <header className={styles.customerNav}>
        <Link className={styles.brand} to="/customer/home" aria-label="고객 홈">
          <ShieldCheck size={22} />
          <span>보험</span>
        </Link>
        <nav aria-label="고객 메뉴">
          <NavLink to="/customer/home">홈</NavLink>
          <NavLink to="/customer/products">상품/가입</NavLink>
          <NavLink to="/customer/applications">신청 내역</NavLink>
          <NavLink to="/customer/contracts">계약/납부</NavLink>
          <NavLink to="/customer/claims/health">의료청구</NavLink>
          <NavLink to="/customer/claims/car-accident">사고접수</NavLink>
          <NavLink to="/customer/claims/status">보상현황</NavLink>
          <NavLink to="/customer/claims/history">보상이력</NavLink>
          {/* 데모 중 임시 숨김 — 라우트/페이지는 그대로, 탭만 비표시 */}
          {/* <NavLink to="/customer/claims/benefit-analysis">실익분석</NavLink> */}
          <NavLink to="/customer/profile">내 정보</NavLink>
        </nav>
        <button className={shared.textButton} type="button" onClick={onLogout}>
          <LogOut size={17} />
          로그아웃
        </button>
      </header>
      <main className={styles.customerContent}>{children}</main>
    </div>
  )
}
