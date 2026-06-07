import { ArrowRight, Users } from 'lucide-react'
import { useState } from 'react'
import shared from '../../../styles/shared.module.css'
import styles from '../assignments.module.css'

const employees = [
  { name: '김심사', department: '지급심사팀', load: 3 },
  { name: '오담당', department: '가입심사팀', load: 5 },
  { name: '한검토', department: '의료심사팀', load: 2 },
]

export function EmployeeAssignmentsPage() {
  const [assigned, setAssigned] = useState('')
  const nextEmployee = employees.reduce((best, employee) =>
    employee.load < best.load ? employee : best,
  )

  return (
    <section className={shared.page}>
      <div className={shared.pageHeader}>
        <div>
          <span className={shared.eyebrow}>UC14</span>
          <h1>담당자 배정</h1>
        </div>
      </div>
      <section className={styles.workflowBand}>
        <div>
          <Users size={22} />
          <h2>업무량 기준 자동 배정</h2>
          <p>현재 업무량이 가장 낮은 {nextEmployee.name}에게 다음 심사 건을 배정할 수 있습니다.</p>
        </div>
        <button
          className={shared.primaryButton}
          type="button"
          onClick={() => setAssigned(`${nextEmployee.name}에게 새 심사 건이 배정되었습니다.`)}
        >
          자동 배정
          <ArrowRight size={18} />
        </button>
      </section>

      {assigned ? <p className={shared.formSuccess}>{assigned}</p> : null}

      <section className={shared.panel}>
        <div className={shared.sectionTitle}>
          <Users size={18} />
          <h2>배정 가능한 직원</h2>
        </div>
        <div className={styles.employeeGrid}>
          {employees.map((employee) => (
            <article key={employee.name}>
              <strong>{employee.name}</strong>
              <span>{employee.department}</span>
              <small>현재 담당 {employee.load}건</small>
              <button
                className={shared.secondaryButton}
                type="button"
                onClick={() => setAssigned(`${employee.name}에게 수동 배정되었습니다.`)}
              >
                수동 배정
              </button>
            </article>
          ))}
        </div>
      </section>
    </section>
  )
}
