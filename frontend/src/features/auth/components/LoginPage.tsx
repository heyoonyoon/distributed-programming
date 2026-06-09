import { ArrowRight, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { decodeUserType } from '../../../lib/api/httpClient'
import { homePath, tokenKey } from '../../../lib/session'
import type { AuthSession } from '../../../lib/types'
import shared from '../../../styles/shared.module.css'
import { authApi } from '../api'
import styles from '../auth.module.css'

export function LoginPage({
  session,
  onLogin,
}: {
  session: AuthSession | null
  onLogin: (session: AuthSession) => void
}) {
  const navigate = useNavigate()
  const [email, setEmail] = useState('hong@test.com')
  const [password, setPassword] = useState('1234')
  const [error, setError] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const selectedLoginType =
    email === 'staff@test.com' ? 'employee' : email === 'hong@test.com' ? 'policyholder' : null

  if (session) {
    return <Navigate to={homePath(session)} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      const { token } = await authApi.login({ email, password })
      const nextSession = { token, userType: decodeUserType(token) }
      window.localStorage.setItem(tokenKey, token)
      onLogin(nextSession)
      navigate(homePath(nextSession))
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className={styles.loginScreen}>
      <section className={styles.loginPanel}>
        <div className={styles.loginCopy}>
          <div className={styles.mark}>
            <ShieldCheck size={28} />
          </div>
          <h1>보험 업무 시스템</h1>
          <p>고객 포털과 보험사 직원 업무 화면을 분리한 데모 환경입니다.</p>
        </div>
        <form className={styles.loginForm} onSubmit={handleSubmit}>
          <div className={shared.accountSwitch}>
            <button
              aria-pressed={selectedLoginType === 'policyholder'}
              className={selectedLoginType === 'policyholder' ? shared.isSelected : ''}
              type="button"
              onClick={() => setEmail('hong@test.com')}
            >
              가입자
            </button>
            <button
              aria-pressed={selectedLoginType === 'employee'}
              className={selectedLoginType === 'employee' ? shared.isSelected : ''}
              type="button"
              onClick={() => setEmail('staff@test.com')}
            >
              직원
            </button>
          </div>
          <label>
            이메일
            <input
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              type="email"
            />
          </label>
          <label>
            비밀번호
            <input
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
            />
          </label>
          {error ? <p className={shared.formError}>{error}</p> : null}
          <button className={shared.primaryButton} disabled={isSubmitting} type="submit">
            {isSubmitting ? '로그인 중' : '로그인'}
            <ArrowRight size={18} />
          </button>
        </form>
      </section>
    </main>
  )
}
