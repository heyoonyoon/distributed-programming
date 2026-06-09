import { BriefcaseBusiness, ClipboardCheck, LogOut, Receipt, Users } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, NavLink, Navigate } from 'react-router-dom'
import { homePath } from '../../lib/session'
import type { AuthSession } from '../../lib/types'
import shared from '../../styles/shared.module.css'
import styles from './layout.module.css'

export function EmployeeShell({
  session,
  onLogout,
  children,
}: {
  session: AuthSession
  onLogout: () => void
  children: ReactNode
}) {
  if (session.userType !== 'EMPLOYEE') {
    return <Navigate to={homePath(session)} replace />
  }

  return (
    <div className={styles.employeeShell}>
      <aside className={styles.sidebar}>
        <Link className={styles.brand} to="/employee/reviews" aria-label="심사 대기 목록">
          <BriefcaseBusiness size={22} />
          <span>직원</span>
        </Link>
        <nav className={styles.navList} aria-label="직원 메뉴">
          <NavLink to="/employee/reviews">
            <ClipboardCheck size={18} />
            가입 심사
          </NavLink>
          <NavLink to="/employee/benefit-reviews">
            <Receipt size={18} />
            보험금 심사
          </NavLink>
          <NavLink to="/employee/assignments">
            <Users size={18} />
            담당자 배정
          </NavLink>
        </nav>
        <div className={styles.sidebarFooter}>
          <div>
            <strong>보험사 직원</strong>
            <span>직원 포털</span>
          </div>
          <button className={shared.iconButton} type="button" onClick={onLogout}>
            <LogOut size={18} />
            <span className="sr-only">로그아웃</span>
          </button>
        </div>
      </aside>
      <main className={styles.content}>{children}</main>
    </div>
  )
}
