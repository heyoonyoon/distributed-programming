import {
  AlertTriangle,
  ArrowRight,
  BadgeCheck,
  Bell,
  BriefcaseBusiness,
  CalendarDays,
  Car,
  CheckCircle2,
  ClipboardCheck,
  CreditCard,
  Download,
  FileText,
  HeartPulse,
  LogOut,
  Receipt,
  Search,
  Send,
  ShieldCheck,
  Users,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import {
  Link,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom'
import { ApiError, apiClient, decodeUserType } from './api/apiClient'
import './App.css'
import type {
  AuthSession,
  ConfirmReviewResponse,
  ContractDetail,
  ContractSummary,
  MyApplication,
  PayableContract,
  PaymentMethod,
  PendingReview,
  PolicyholderProfile,
  ProductDetail,
  ProductSummary,
  ProductType,
  ReviewApplicationDetail,
  ReviewResult,
  UnpaidContract,
} from './types'

const tokenKey = 'insurance.authToken'

const claimHistory = [
  {
    id: 'CLM-2026-0104',
    type: '의료보험 청구',
    amount: '184,000원',
    status: '지급 완료',
    date: '2026.05.18',
  },
  {
    id: 'CAR-2026-0031',
    type: '자동차사고 접수',
    amount: '심사 중',
    status: '담당자 배정',
    date: '2026.05.29',
  },
]

const employees = [
  { name: '김심사', department: '지급심사팀', load: 3 },
  { name: '오담당', department: '가입심사팀', load: 5 },
  { name: '한검토', department: '의료심사팀', load: 2 },
]

function readSession(): AuthSession | null {
  const token = window.localStorage.getItem(tokenKey)
  return token ? { token, userType: decodeUserType(token) } : null
}

function homePath(session: AuthSession) {
  return session.userType === 'EMPLOYEE' ? '/employee/reviews' : '/customer/home'
}

function formatCurrency(value: number) {
  return `${value.toLocaleString('ko-KR')}원`
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('ko-KR')
}

function LoginPage({
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

  if (session) {
    return <Navigate to={homePath(session)} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      const { token } = await apiClient.login({ email, password })
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
    <main className="login-screen">
      <section className="login-panel">
        <div className="login-copy">
          <div className="mark">
            <ShieldCheck size={28} />
          </div>
          <h1>Insurance System</h1>
          <p>고객 포털과 보험사 직원 업무 화면을 분리한 데모 환경입니다.</p>
        </div>
        <form className="login-form" onSubmit={handleSubmit}>
          <div className="account-switch">
            <button type="button" onClick={() => setEmail('hong@test.com')}>
              Policyholder
            </button>
            <button type="button" onClick={() => setEmail('staff@test.com')}>
              InsuranceEmployee
            </button>
          </div>
          <label>
            Email
            <input
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              type="email"
            />
          </label>
          <label>
            Password
            <input
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
            />
          </label>
          {error ? <p className="form-error">{error}</p> : null}
          <button className="primary-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Signing in' : 'Sign in'}
            <ArrowRight size={18} />
          </button>
        </form>
      </section>
    </main>
  )
}

function Protected({
  session,
  children,
}: {
  session: AuthSession | null
  children: ReactNode
}) {
  const location = useLocation()

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return children
}

function CustomerShell({
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
    <div className="customer-shell">
      <header className="customer-nav">
        <Link className="brand" to="/customer/home" aria-label="Customer home">
          <ShieldCheck size={22} />
          <span>Insurance</span>
        </Link>
        <nav aria-label="Customer navigation">
          <Link to="/customer/home">Home</Link>
          <Link to="/customer/contracts">Contracts</Link>
          <Link to="/customer/claims">Claims</Link>
          <Link to="/customer/profile">Profile</Link>
        </nav>
        <button className="text-button" type="button" onClick={onLogout}>
          <LogOut size={17} />
          Logout
        </button>
      </header>
      <main className="customer-content">{children}</main>
    </div>
  )
}

function EmployeeShell({
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
    <div className="employee-shell">
      <aside className="sidebar">
        <Link className="brand" to="/employee/reviews" aria-label="Review queue">
          <BriefcaseBusiness size={22} />
          <span>Employee</span>
        </Link>
        <nav className="nav-list" aria-label="Employee navigation">
          <Link to="/employee/reviews">
            <ClipboardCheck size={18} />
            Reviews
          </Link>
          <Link to="/employee/assignments">
            <Users size={18} />
            Assignments
          </Link>
        </nav>
        <div className="sidebar-footer">
          <div>
            <strong>보험사 직원</strong>
            <span>InsuranceEmployee</span>
          </div>
          <button className="icon-button" type="button" onClick={onLogout}>
            <LogOut size={18} />
            <span className="sr-only">Logout</span>
          </button>
        </div>
      </aside>
      <main className="content">{children}</main>
    </div>
  )
}

function CustomerHomePage() {
  const actions = [
    { label: '보험 상품 조회', icon: Search, path: '/customer/contracts' },
    { label: '의료보험 청구', icon: HeartPulse, path: '/customer/claims' },
    { label: '자동차사고 접수', icon: Car, path: '/customer/claims' },
    { label: '보험료 납부', icon: CreditCard, path: '/customer/contracts' },
  ]

  return (
    <section className="customer-home">
      <div className="customer-hero">
        <span className="eyebrow">Policyholder portal</span>
        <h1>고객님, 필요한 보험 업무를 바로 처리하세요.</h1>
        <p>계약 확인, 청구 접수, 보험료 납부를 고객용 화면에서 진행합니다.</p>
      </div>

      <div className="action-grid">
        {actions.map(({ label, icon: Icon, path }) => (
          <Link className="action-tile" to={path} key={label}>
            <Icon size={22} />
            <span>{label}</span>
            <ArrowRight size={18} />
          </Link>
        ))}
      </div>

      <section className="status-panel">
        <div>
          <BadgeCheck size={22} />
          <h2>현재 유효한 계약 2건</h2>
          <p>의료보험 1건, 자동차보험 1건이 정상 유지 중입니다.</p>
        </div>
        <Link className="secondary-button" to="/customer/contracts">
          계약 보기
        </Link>
      </section>
    </section>
  )
}

function CustomerContractsPage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const [productType, setProductType] = useState<ProductType>('HEALTH')
  const [keyword, setKeyword] = useState('')
  const [minPremium, setMinPremium] = useState('')
  const [maxPremium, setMaxPremium] = useState('')
  const [products, setProducts] = useState<ProductSummary[]>([])
  const [selectedProduct, setSelectedProduct] = useState<ProductDetail | null>(null)
  const [applications, setApplications] = useState<MyApplication[]>([])
  const [contracts, setContracts] = useState<ContractSummary[]>([])
  const [selectedContract, setSelectedContract] = useState<ContractDetail | null>(null)
  const [unpaidContracts, setUnpaidContracts] = useState<UnpaidContract[]>([])
  const [selectedUnpaid, setSelectedUnpaid] = useState<UnpaidContract | null>(null)
  const [payableContracts, setPayableContracts] = useState<PayableContract[]>([])
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD')
  const [paymentInfo, setPaymentInfo] = useState('1234-5678-9012-3456')
  const [autoDebitAccount, setAutoDebitAccount] = useState('110-222-333333')
  const [withdrawalDay, setWithdrawalDay] = useState('25')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isLoading, setLoading] = useState(false)
  const [isContractLoading, setContractLoading] = useState(false)
  const [isPaymentSubmitting, setPaymentSubmitting] = useState(false)
  const [isAutoDebitSubmitting, setAutoDebitSubmitting] = useState(false)
  const selectedPayable = selectedContract
    ? payableContracts.find((contract) => contract.contractId === selectedContract.contractId)
    : null

  async function loadProducts() {
    setError('')
    setLoading(true)

    try {
      const list = await apiClient.getProducts({
        type: productType,
        keyword,
        minPremium,
        maxPremium,
      })
      setProducts(list)

      if (list[0]) {
        setSelectedProduct(await apiClient.getProduct(list[0].id))
      } else {
        setSelectedProduct(null)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '상품 조회에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function loadApplications() {
    try {
      setApplications(await apiClient.getMyApplications(token))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '신청 목록 조회에 실패했습니다.')
    }
  }

  async function loadContractDetail(id: number) {
    setError('')
    setContractLoading(true)

    try {
      const [detail, unpaid] = await Promise.all([
        apiClient.getContract(token, id),
        apiClient.getUnpaidContract(token, id).catch((err) => {
          if (err instanceof ApiError && err.status === 404) {
            return null
          }
          throw err
        }),
      ])

      setSelectedContract(detail)
      setSelectedUnpaid(unpaid)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '계약 상세 조회에 실패했습니다.')
    } finally {
      setContractLoading(false)
    }
  }

  async function loadContractData() {
    setContractLoading(true)

    try {
      const [contractList, unpaidList, payableList] = await Promise.all([
        apiClient.getContracts(token),
        apiClient.getUnpaidContracts(token),
        apiClient.getPayableContracts(token),
      ])

      setContracts(contractList)
      setUnpaidContracts(unpaidList)
      setPayableContracts(payableList)

      if (contractList[0]) {
        await loadContractDetail(contractList[0].contractId)
      } else {
        setSelectedContract(null)
        setSelectedUnpaid(null)
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '계약 정보를 조회하지 못했습니다.')
    } finally {
      setContractLoading(false)
    }
  }

  useEffect(() => {
    void Promise.resolve().then(() => {
      loadProducts()
      loadApplications()
      loadContractData()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productType])

  async function selectProduct(id: number) {
    setError('')
    setSelectedProduct(await apiClient.getProduct(id))
  }

  async function submitApplication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedProduct) {
      return
    }

    const form = new FormData(event.currentTarget)
    const request =
      selectedProduct.productType === 'HEALTH'
        ? {
            productId: selectedProduct.id,
            medicalHistory: {
              currentConditions: String(form.get('currentConditions')),
              pastHospitalization: String(form.get('pastHospitalization')),
              medications: String(form.get('medications')),
            },
          }
        : {
            productId: selectedProduct.id,
            vehicleInfo: {
              plateNumber: String(form.get('plateNumber')),
              vehicleType: String(form.get('vehicleType')),
              modelYear: Number(form.get('modelYear')),
              drivingExperienceYears: Number(form.get('drivingExperienceYears')),
            },
          }

    try {
      const created = await apiClient.createApplication(token, request)
      setSuccess(`가입 신청 ${created.applicationId}번이 ${created.status} 상태로 접수되었습니다.`)
      await loadApplications()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '가입 신청에 실패했습니다.')
    }
  }

  async function cancelApplication(id: number) {
    setError('')
    setSuccess('')

    try {
      await apiClient.cancelApplication(token, id)
      setSuccess(`가입 신청 ${id}번을 취소했습니다.`)
      await loadApplications()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '신청 취소에 실패했습니다.')
    }
  }

  async function downloadContract(id: number) {
    setError('')
    setSuccess('')

    try {
      const { blob, filename } = await apiClient.downloadContract(token, id)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '계약서 다운로드에 실패했습니다.')
    }
  }

  async function submitPayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedContract) {
      return
    }

    setError('')
    setSuccess('')
    setPaymentSubmitting(true)

    try {
      const result = await apiClient.payPremium(token, selectedContract.contractId, {
        method: paymentMethod,
        paymentInfo,
      })

      if (result.status === 'FAILED') {
        setError(`결제 실패: ${result.reason ?? '승인되지 않았습니다.'}`)
      } else {
        setSuccess(`납부 ${result.paymentId}번이 완료되었습니다. (${formatCurrency(result.amount)})`)
      }

      await loadContractData()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '보험료 납부에 실패했습니다.')
    } finally {
      setPaymentSubmitting(false)
    }
  }

  async function submitAutoDebit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedContract) {
      return
    }

    setError('')
    setSuccess('')
    setAutoDebitSubmitting(true)

    try {
      await apiClient.registerAutoDebit(token, selectedContract.contractId, {
        account: autoDebitAccount,
        withdrawalDay: Number(withdrawalDay),
      })
      setSuccess('자동이체가 등록되었습니다.')
      await loadContractDetail(selectedContract.contractId)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '자동이체 등록에 실패했습니다.')
    } finally {
      setAutoDebitSubmitting(false)
    }
  }

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">UC01 / UC02 / A-2 / A-3</span>
          <h1>상품 조회 및 가입 신청</h1>
        </div>
      </div>

      <div className="split-layout">
        <section className="panel">
          <div className="section-title">
            <Search size={18} />
            <h2>보험 상품 조회</h2>
          </div>
          <form className="filter-form" onSubmit={(event) => {
            event.preventDefault()
            loadProducts()
          }}>
            <div className="account-switch">
              <button type="button" onClick={() => setProductType('HEALTH')}>
                HEALTH
              </button>
              <button type="button" onClick={() => setProductType('CAR')}>
                CAR
              </button>
            </div>
            <label>
              Keyword
              <input value={keyword} onChange={(event) => setKeyword(event.target.value)} />
            </label>
            <div className="inline-fields">
              <label>
                Min
                <input value={minPremium} onChange={(event) => setMinPremium(event.target.value)} />
              </label>
              <label>
                Max
                <input value={maxPremium} onChange={(event) => setMaxPremium(event.target.value)} />
              </label>
            </div>
            <button className="secondary-button" type="submit">
              필터 적용
            </button>
          </form>
          <div className="product-tabs">
            {isLoading ? <p>Loading products...</p> : null}
            {products.map((product) => (
              <button
                className={product.id === selectedProduct?.id ? 'is-selected' : ''}
                key={product.id}
                type="button"
                onClick={() => selectProduct(product.id)}
              >
                {product.productName}
                <span>{product.coverageSummary} · {product.monthlyPremium.toLocaleString()}원</span>
              </button>
            ))}
          </div>
        </section>

        <form className="panel form-panel" onSubmit={submitApplication}>
          <div className="section-title">
            <FileText size={18} />
            <h2>상품 상세 및 가입 신청</h2>
          </div>
          {selectedProduct ? (
            <>
              <article className="detail-card">
                <span className="badge">{selectedProduct.productType}</span>
                <h3>{selectedProduct.productName}</h3>
                <p>{selectedProduct.description}</p>
                <strong>{selectedProduct.monthlyPremium.toLocaleString()}원 / 월</strong>
                <ul>
                  {selectedProduct.coverageItems.map((item) => (
                    <li key={item.itemName}>
                      <CheckCircle2 size={16} />
                      {item.itemName} · 한도 {item.coverageLimit.toLocaleString()}원 · 자기부담 {item.deductible.toLocaleString()}원
                    </li>
                  ))}
                </ul>
              </article>
              {selectedProduct.productType === 'HEALTH' ? (
                <>
                  <label>
                    현재 병력
                    <input name="currentConditions" defaultValue="없음" />
                  </label>
                  <label>
                    과거 입원 이력
                    <input name="pastHospitalization" defaultValue="없음" />
                  </label>
                  <label>
                    복용 중인 약물
                    <input name="medications" defaultValue="없음" />
                  </label>
                </>
              ) : (
                <>
                  <label>
                    차량번호
                    <input name="plateNumber" defaultValue="12가3456" />
                  </label>
                  <label>
                    차종
                    <input name="vehicleType" defaultValue="승용차" />
                  </label>
                  <div className="inline-fields">
                    <label>
                      연식
                      <input name="modelYear" defaultValue="2020" />
                    </label>
                    <label>
                      운전경력
                      <input name="drivingExperienceYears" defaultValue="5" />
                    </label>
                  </div>
                </>
              )}
              <button className="primary-button" type="submit">
                가입 신청
                <ArrowRight size={18} />
              </button>
            </>
          ) : (
            <p>조회된 상품이 없습니다.</p>
          )}
          {error ? <p className="form-error">{error}</p> : null}
          {success ? <p className="form-success">{success}</p> : null}
        </form>
      </div>

      <section className="panel">
        <div className="section-title">
          <Receipt size={18} />
          <h2>내 가입 신청</h2>
        </div>
        <div className="customer-list compact">
          {applications.map((application) => (
            <article key={application.applicationId}>
              <FileText size={20} />
              <div>
                <strong>{application.productName}</strong>
                <span>신청번호 {application.applicationId} · {application.status}</span>
                <small>{new Date(application.appliedAt).toLocaleString()}</small>
              </div>
              {application.status === 'PENDING' ? (
                <button
                  className="secondary-button"
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

      <section className="panel">
        <div className="section-title">
          <Download size={18} />
          <h2>내 계약 및 보험료 납부</h2>
        </div>
        {isContractLoading ? <p>Loading contracts...</p> : null}
        <div className="contract-workspace">
          <div className="customer-list compact">
            {contracts.map((contract) => (
              <article key={contract.contractId}>
                <Receipt size={20} />
                <div>
                  <strong>{contract.productName}</strong>
                  <span>
                    {formatDate(contract.startDate)} - {formatDate(contract.endDate)} · 월{' '}
                    {formatCurrency(contract.monthlyPremium)}
                  </span>
                  <small>
                    {contract.productType} · {contract.status}
                  </small>
                </div>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => loadContractDetail(contract.contractId)}
                >
                  상세보기
                </button>
              </article>
            ))}
            {contracts.length === 0 ? <p>유효한 계약이 없습니다.</p> : null}
          </div>

          <div className="contract-detail-grid">
            <section className="payment-box">
              <div>
                <span className="badge">계약 상세</span>
                <h3>{selectedContract?.productName ?? '계약을 선택하세요'}</h3>
                {selectedContract ? (
                  <p>
                    계약번호 {selectedContract.contractId} · 자동이체{' '}
                    {selectedContract.paymentMethod}
                  </p>
                ) : null}
              </div>
              {selectedContract ? (
                <>
                  <ul className="coverage-list">
                    {selectedContract.coverageItems.map((item) => (
                      <li key={item.itemName}>
                        <CheckCircle2 size={16} />
                        {item.itemName} · 한도 {formatCurrency(item.coverageLimit)} · 자기부담{' '}
                        {formatCurrency(item.deductible)}
                      </li>
                    ))}
                  </ul>
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={() => downloadContract(selectedContract.contractId)}
                  >
                    계약서 다운로드
                    <Download size={16} />
                  </button>
                </>
              ) : null}
            </section>

            <section className="payment-box">
              <div>
                <span className="badge">미납/연체</span>
                <h3>
                  {selectedUnpaid
                    ? formatCurrency(selectedUnpaid.unpaidPrincipal)
                    : '연체 없음'}
                </h3>
                <p>
                  {selectedUnpaid
                    ? `기한 ${formatDate(selectedUnpaid.dueDate)} · ${selectedUnpaid.overdueDays}일 연체 · 이자 ${formatCurrency(selectedUnpaid.overdueInterest)}`
                    : '선택한 계약에 표시할 연체 내역이 없습니다.'}
                </p>
              </div>
              <div className="unpaid-strip">
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

          {selectedContract ? (
            <div className="contract-detail-grid">
              <form className="payment-box" onSubmit={submitPayment}>
                <div>
                  <span className="badge">보험료 납부</span>
                  <h3>한 회차 납부</h3>
                  <p>
                    {selectedPayable
                      ? `납부 예정 ${formatCurrency(selectedPayable.amount)}`
                      : '현재 납부할 미납 회차가 없습니다.'}
                  </p>
                </div>
                <div className="inline-fields">
                  <label>
                    Method
                    <select
                      value={paymentMethod}
                      onChange={(event) => setPaymentMethod(event.target.value as PaymentMethod)}
                    >
                      <option value="CARD">CARD</option>
                      <option value="TRANSFER">TRANSFER</option>
                      <option value="AUTO_DEBIT">AUTO_DEBIT</option>
                    </select>
                  </label>
                  <label>
                    Payment info
                    <input
                      value={paymentInfo}
                      onChange={(event) => setPaymentInfo(event.target.value)}
                    />
                  </label>
                </div>
                <button
                  className="primary-button"
                  disabled={isPaymentSubmitting}
                  type="submit"
                >
                  {isPaymentSubmitting ? 'Processing' : '납부하기'}
                  <CreditCard size={18} />
                </button>
              </form>

              <form className="payment-box" onSubmit={submitAutoDebit}>
                <div>
                  <span className="badge">자동이체</span>
                  <h3>출금 계좌 등록</h3>
                  <p>등록 후 계약 상세의 납부 방법이 AUTO_DEBIT로 표시됩니다.</p>
                </div>
                <div className="inline-fields">
                  <label>
                    Account
                    <input
                      value={autoDebitAccount}
                      onChange={(event) => setAutoDebitAccount(event.target.value)}
                    />
                  </label>
                  <label>
                    Withdrawal day
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
                  className="secondary-button"
                  disabled={isAutoDebitSubmitting}
                  type="submit"
                >
                  {isAutoDebitSubmitting ? 'Registering' : '자동이체 등록'}
                </button>
              </form>
            </div>
          ) : null}
        </div>
      </section>
    </section>
  )
}

function CustomerClaimsPage() {
  const [claimMessage, setClaimMessage] = useState('')
  const [accidentMessage, setAccidentMessage] = useState('')

  function submitClaim(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setClaimMessage('청구 정보가 저장되었고, 간단한 청구로 판별되어 지급 처리가 진행됩니다.')
  }

  function submitAccident(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAccidentMessage('자동차사고 접수 번호 CAR-2026-0042가 발급되었습니다.')
  }

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">UC03 / UC04 / UC05 / UC09</span>
          <h1>청구 및 사고 접수</h1>
        </div>
      </div>

      <div className="split-layout">
        <form className="panel form-panel" onSubmit={submitClaim}>
          <div className="section-title">
            <HeartPulse size={18} />
            <h2>의료보험 청구</h2>
          </div>
          <label>
            병원명
            <input name="hospitalName" defaultValue="서울중앙병원" />
          </label>
          <label>
            진료일
            <input name="treatmentDate" type="date" defaultValue="2026-06-01" />
          </label>
          <label>
            청구 금액
            <input name="requestAmount" defaultValue="184000" />
          </label>
          {claimMessage ? <p className="form-success">{claimMessage}</p> : null}
          <button className="primary-button" type="submit">
            청구 신청
            <Send size={18} />
          </button>
        </form>

        <form className="panel form-panel" onSubmit={submitAccident}>
          <div className="section-title">
            <Car size={18} />
            <h2>자동차사고 접수</h2>
          </div>
          <label>
            사고 장소
            <input name="accidentLocation" defaultValue="서울 강남구 역삼동" />
          </label>
          <label>
            사고 유형
            <input name="accidentType" defaultValue="쌍방" />
          </label>
          <label>
            차량 번호
            <input name="vehicleNumber" defaultValue="12가 3456" />
          </label>
          {accidentMessage ? <p className="form-success">{accidentMessage}</p> : null}
          <button className="primary-button" type="submit">
            접수하기
            <Send size={18} />
          </button>
        </form>
      </div>

      <section className="panel">
        <div className="section-title">
          <CalendarDays size={18} />
          <h2>처리 현황 및 보상 이력</h2>
        </div>
        <div className="timeline-list">
          {claimHistory.map((claim) => (
            <article key={claim.id}>
              <span>{claim.date}</span>
              <div>
                <strong>{claim.type}</strong>
                <p>{claim.id} · {claim.amount} · {claim.status}</p>
              </div>
            </article>
          ))}
        </div>
      </section>
    </section>
  )
}

function ProfilePage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const [profile, setProfile] = useState<PolicyholderProfile | null>(null)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [isSaving, setSaving] = useState(false)

  useEffect(() => {
    let isMounted = true

    async function loadProfile() {
      setError('')
      setLoading(true)

      try {
        const nextProfile = await apiClient.getMe(token)
        if (isMounted) {
          setProfile(nextProfile)
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          onUnauthorized()
          return
        }

        if (isMounted) {
          setError(err instanceof Error ? err.message : '내 정보 조회에 실패했습니다.')
        }
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    loadProfile()

    return () => {
      isMounted = false
    }
  }, [onUnauthorized, token])

  if (isLoading) {
    return <section className="page">Loading profile...</section>
  }

  if (!profile) {
    return (
      <section className="page">
        <div className="page-header">
          <div>
            <span className="eyebrow">GET /me</span>
            <h1>마이페이지</h1>
          </div>
        </div>
        <p className="form-error">{error || '내 정보를 표시할 수 없습니다.'}</p>
      </section>
    )
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setStatus('')
    setError('')
    setSaving(true)

    const form = new FormData(event.currentTarget)

    try {
      const nextProfile = await apiClient.updateProfile(token, {
        email: String(form.get('email')),
        phone: String(form.get('phone')),
        address: String(form.get('address')),
        bankAccount: String(form.get('bankAccount')),
      })

      setProfile(nextProfile)
      setStatus('개인정보가 성공적으로 변경되었습니다.')
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }

      setError(err instanceof Error ? err.message : '개인정보 수정에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">GET /me · UC06 PUT /me/profile</span>
          <h1>마이페이지</h1>
        </div>
      </div>

      <section className="status-panel">
        <div>
          <BadgeCheck size={22} />
          <h2>{profile.name}님의 내 정보</h2>
          <p>{profile.email} · {profile.phone}</p>
        </div>
      </section>

      <form className="profile-form" key={profile.email} onSubmit={handleSubmit}>
        <label>
          Name
          <input name="name" value={profile.name} disabled />
        </label>
        <label>
          Email
          <input name="email" defaultValue={profile.email} type="email" />
        </label>
        <label>
          Phone
          <input name="phone" defaultValue={profile.phone} />
        </label>
        <label>
          Address
          <input name="address" defaultValue={profile.address} />
        </label>
        <label>
          Bank account
          <input name="bankAccount" defaultValue={profile.bankAccount} />
        </label>
        {error ? <p className="form-error">{error}</p> : null}
        {status ? <p className="form-success">{status}</p> : null}
        <button className="primary-button" disabled={isSaving} type="submit">
          {isSaving ? 'Saving' : 'Save changes'}
          <BadgeCheck size={18} />
        </button>
      </form>
    </section>
  )
}

function EmployeeReviewsPage({
  token,
  onUnauthorized,
}: {
  token: string
  onUnauthorized: () => void
}) {
  const [pendingReviews, setPendingReviews] = useState<PendingReview[]>([])
  const [selectedReview, setSelectedReview] = useState<ReviewApplicationDetail | null>(null)
  const [reviewResult, setReviewResult] = useState<ReviewResult>('APPROVED')
  const [comment, setComment] = useState('이상 없음')
  const [surchargeRate, setSurchargeRate] = useState('0.2')
  const [decision, setDecision] = useState<ConfirmReviewResponse | null>(null)
  const [error, setError] = useState('')
  const metrics = useMemo(
    () => [
      { label: 'Pending applications', value: String(pendingReviews.length), icon: ClipboardCheck },
      {
        label: 'Car underwriting',
        value: String(pendingReviews.filter((review) => review.productName.includes('드라이브') || review.productName.includes('자동차')).length),
        icon: Bell,
      },
      { label: 'Ready to confirm', value: selectedReview ? '1' : '0', icon: Users },
    ],
    [pendingReviews, selectedReview],
  )

  async function loadPendingReviews() {
    try {
      const list = await apiClient.getPendingReviews(token)
      setPendingReviews(list)
      if (list[0]) {
        setSelectedReview(await apiClient.getReviewApplication(token, list[0].applicationId))
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '심사 대기 목록 조회에 실패했습니다.')
    }
  }

  useEffect(() => {
    void Promise.resolve().then(() => {
      loadPendingReviews()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function selectReview(id: number) {
    setError('')
    setDecision(null)
    try {
      setSelectedReview(await apiClient.getReviewApplication(token, id))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '심사 상세 조회에 실패했습니다.')
    }
  }

  async function confirmReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedReview) {
      return
    }

    try {
      const response = await apiClient.confirmReview(token, selectedReview.applicationId, {
        result: reviewResult,
        comment,
        ...(reviewResult === 'CONDITIONAL' ? { surchargeRate: Number(surchargeRate) } : {}),
      })
      setDecision(response)
      await loadPendingReviews()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '심사 확정에 실패했습니다.')
    }
  }

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">UC12 / UC13 / UC15</span>
          <h1>심사 대기 목록</h1>
        </div>
      </div>
      <div className="metric-grid">
        {metrics.map(({ label, value, icon: Icon }) => (
          <article className="metric-card" key={label}>
            <Icon size={20} />
            <strong>{value}</strong>
            <span>{label}</span>
          </article>
        ))}
      </div>

      <div className="split-layout">
        <section className="panel">
          <div className="section-title">
            <ClipboardCheck size={18} />
            <h2>대기 건</h2>
          </div>
          <div className="review-list">
            {pendingReviews.map((review) => (
              <button
                className={review.applicationId === selectedReview?.applicationId ? 'is-selected' : ''}
                key={review.applicationId}
                type="button"
                onClick={() => selectReview(review.applicationId)}
              >
                <strong>APP-{review.applicationId}</strong>
                <span>{review.productName} · {review.applicantName}</span>
                <small>{new Date(review.appliedAt).toLocaleString()} · {review.basePremium.toLocaleString()}원</small>
              </button>
            ))}
            {pendingReviews.length === 0 ? <p>심사 대기 건이 없습니다.</p> : null}
          </div>
        </section>

        <form className="panel" onSubmit={confirmReview}>
          <div className="section-title">
            <FileText size={18} />
            <h2>심사 상세</h2>
          </div>
          {selectedReview ? (
            <article className="detail-card">
              <span className="badge">Application {selectedReview.applicationId}</span>
              <h3>{selectedReview.applicantName}</h3>
              <p>{selectedReview.productName} · 기본 보험료 {selectedReview.basePremium.toLocaleString()}원</p>
              <p>생년월일 {selectedReview.birthDate} · 주민등록번호 {selectedReview.ssn}</p>
              {selectedReview.medicalHistory ? (
                <div className="risk-box">
                  <HeartPulse size={18} />
                  <span>
                    현재 병력 {selectedReview.medicalHistory.currentConditions} · 입원 이력 {selectedReview.medicalHistory.pastHospitalization}
                  </span>
                </div>
              ) : null}
              {selectedReview.vehicleInfo ? (
                <div className="risk-box">
                  <Car size={18} />
                  <span>
                    {selectedReview.vehicleInfo.plateNumber} · {selectedReview.vehicleInfo.vehicleType} · {selectedReview.vehicleInfo.modelYear}년식
                  </span>
                </div>
              ) : null}
              {selectedReview.accidentHistory ? (
                <div className="risk-box">
                  <AlertTriangle size={18} />
                  <span>
                    사고 {selectedReview.accidentHistory.accidentCount}건 · 지급 {selectedReview.accidentHistory.totalPaidAmount.toLocaleString()}원 · 면허 {selectedReview.accidentHistory.licenseStatus}
                  </span>
                </div>
              ) : null}
              <label>
                ReviewResult
                <select value={reviewResult} onChange={(event) => setReviewResult(event.target.value as ReviewResult)}>
                  <option value="APPROVED">APPROVED</option>
                  <option value="CONDITIONAL">CONDITIONAL</option>
                  <option value="REJECTED">REJECTED</option>
                </select>
              </label>
              {reviewResult === 'CONDITIONAL' ? (
                <label>
                  Surcharge rate
                  <input value={surchargeRate} onChange={(event) => setSurchargeRate(event.target.value)} />
                </label>
              ) : null}
              <label>
                Comment
                <input value={comment} onChange={(event) => setComment(event.target.value)} />
              </label>
              {decision ? (
                <p className="form-success">
                  심사 {decision.reviewId}번 {decision.result} · 최종 월 보험료 {decision.adjustedPremium.toLocaleString()}원
                </p>
              ) : null}
              {error ? <p className="form-error">{error}</p> : null}
              <button className="primary-button" type="submit">
                심사 확정
                <CheckCircle2 size={18} />
              </button>
            </article>
          ) : (
            <p>심사 건을 선택하세요.</p>
          )}
        </form>
      </div>
    </section>
  )
}

function EmployeeAssignmentsPage() {
  const [assigned, setAssigned] = useState('')
  const nextEmployee = employees.reduce((best, employee) =>
    employee.load < best.load ? employee : best,
  )

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">UC14</span>
          <h1>담당자 지정</h1>
        </div>
      </div>
      <section className="workflow-band">
        <div>
          <Users size={22} />
          <h2>업무량 기준 자동 배정</h2>
          <p>현재 업무량이 가장 낮은 {nextEmployee.name}에게 다음 심사 건을 배정할 수 있습니다.</p>
        </div>
        <button
          className="primary-button"
          type="button"
          onClick={() => setAssigned(`${nextEmployee.name}에게 새 심사 건이 배정되었습니다.`)}
        >
          자동 배정
          <ArrowRight size={18} />
        </button>
      </section>

      {assigned ? <p className="form-success">{assigned}</p> : null}

      <section className="panel">
        <div className="section-title">
          <Users size={18} />
          <h2>배정 가능한 직원</h2>
        </div>
        <div className="employee-grid">
          {employees.map((employee) => (
            <article key={employee.name}>
              <strong>{employee.name}</strong>
              <span>{employee.department}</span>
              <small>현재 담당 {employee.load}건</small>
              <button
                className="secondary-button"
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

function App() {
  const [session, setSession] = useState<AuthSession | null>(() => readSession())

  function handleLogout() {
    void apiClient.logout().catch(() => undefined)
    window.localStorage.removeItem(tokenKey)
    setSession(null)
  }

  function handleUnauthorized() {
    window.localStorage.removeItem(tokenKey)
    setSession(null)
  }

  return (
    <Routes>
      <Route
        path="/login"
        element={<LoginPage session={session} onLogin={setSession} />}
      />
      {session && (
      <Route
        path="/customer/*"
        element={
          <Protected session={session}>
            <CustomerShell session={session as AuthSession} onLogout={handleLogout}>
              <Routes>
                <Route path="/home" element={<CustomerHomePage />} />
                <Route
                  path="/contracts"
                  element={
                    <CustomerContractsPage
                      token={(session as AuthSession).token}
                      onUnauthorized={handleUnauthorized}
                    />
                  }
                />
                <Route path="/claims" element={<CustomerClaimsPage />} />
                <Route
                  path="/profile"
                  element={
                    <ProfilePage
                      token={(session as AuthSession).token}
                      onUnauthorized={handleUnauthorized}
                    />
                  }
                />
                <Route path="*" element={<Navigate to="/customer/home" replace />} />
              </Routes>
            </CustomerShell>
          </Protected>
        }
      />
      )}
      {session && (
      <Route
        path="/employee/*"
        element={
          <Protected session={session}>
            <EmployeeShell session={session as AuthSession} onLogout={handleLogout}>
              <Routes>
                <Route
                  path="/reviews"
                  element={
                    <EmployeeReviewsPage
                      token={(session as AuthSession).token}
                      onUnauthorized={handleUnauthorized}
                    />
                  }
                />
                <Route path="/assignments" element={<EmployeeAssignmentsPage />} />
                <Route path="*" element={<Navigate to="/employee/reviews" replace />} />
              </Routes>
            </EmployeeShell>
          </Protected>
        }
      />
      )}
      <Route
        path="*"
        element={
          session ? (
            <Navigate to={homePath(session)} replace />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />
    </Routes>
  )
}

export default App
