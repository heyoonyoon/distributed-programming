import { BadgeCheck } from 'lucide-react'
import type { FormEvent } from 'react'
import shared from '../../../styles/shared.module.css'
import { useProfile } from '../hooks/useProfile'
import styles from '../profile.module.css'

export function ProfilePage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const { profile, status, error, isLoading, isSaving, saveProfile } = useProfile(
    token,
    onUnauthorized,
  )

  if (isLoading) {
    return <section className={shared.page}>내 정보를 불러오는 중입니다.</section>
  }

  if (!profile) {
    return (
      <section className={shared.page}>
        <div className={shared.pageHeader}>
          <div>
            <span className={shared.eyebrow}>내 정보 조회</span>
            <h1>마이페이지</h1>
          </div>
        </div>
        <p className={shared.formError}>{error || '내 정보를 표시할 수 없습니다.'}</p>
      </section>
    )
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    void saveProfile({
      email: String(form.get('email')),
      phone: String(form.get('phone')),
      address: String(form.get('address')),
      bankAccount: String(form.get('bankAccount')),
    })
  }

  return (
    <section className={shared.page}>
      <div className={shared.pageHeader}>
        <div>
          <span className={shared.eyebrow}>UC06 내 정보 수정</span>
          <h1>마이페이지</h1>
        </div>
      </div>

      <section className={shared.statusPanel}>
        <div>
          <BadgeCheck size={22} />
          <h2>{profile.name}님의 내 정보</h2>
          <p>{profile.email} · {profile.phone}</p>
        </div>
      </section>

      <form className={styles.profileForm} key={profile.email} onSubmit={handleSubmit}>
        <label>
          이름
          <input name="name" value={profile.name} disabled />
        </label>
        <label>
          이메일
          <input name="email" defaultValue={profile.email} type="email" />
        </label>
        <label>
          전화번호
          <input name="phone" defaultValue={profile.phone} />
        </label>
        <label>
          주소
          <input name="address" defaultValue={profile.address} />
        </label>
        <label>
          계좌번호
          <input name="bankAccount" defaultValue={profile.bankAccount} />
        </label>
        {error ? <p className={shared.formError}>{error}</p> : null}
        {status ? <p className={shared.formSuccess}>{status}</p> : null}
        <button className={shared.primaryButton} disabled={isSaving} type="submit">
          {isSaving ? '저장 중' : '변경사항 저장'}
          <BadgeCheck size={18} />
        </button>
      </form>
    </section>
  )
}
